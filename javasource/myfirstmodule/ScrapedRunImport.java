package myfirstmodule;

import com.mendix.core.Core;
import com.mendix.systemwideinterfaces.core.*;
import com.mendix.thirdparty.org.json.*;
import java.math.BigDecimal;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import java.util.stream.Stream;

/** Lists, previews and imports the timestamped output of tools/scraper as owned sample data. Local administrators only; reads local files and sends no requests. */
final class ScrapedRunImport {
    static final String FEATURE = "features:scraped-run-import-v1";
    static final String EXAMPLE_FOLDER = "example-badminton-bel-fictional";
    static final String EXAMPLE_RESOURCE = "scrape-example-run";
    private static final String PREFIX = "DEMO-SCRAPE:";
    private static final String M = "MyFirstModule.";
    /** Relative to the scrapes folder: one level for the league or rankings, one for the run. */
    private static final String FOLDER = "[a-z0-9]+(-[a-z0-9]+)*/[a-z][a-z0-9]*-[a-z]{3}-[0-9]{8}-[0-9]{6}(-partial)?";
    private ScrapedRunImport() {}

    /** One run folder: its parsed manifest and whether every file still has the hash the scraper wrote. */
    static final class Run {
        final String folder; final boolean example; final Path dir; final JSONObject manifest; final String marker; final List<String> problems = new ArrayList<>();
        Run(String folder, boolean example, Path dir) throws Exception {
            this.folder = folder; this.example = example; this.dir = dir;
            byte[] raw = Files.readAllBytes(dir.resolve("manifest.json"));
            manifest = new JSONObject(new String(raw, java.nio.charset.StandardCharsets.UTF_8));
            marker = FEATURE + ":" + sha256(raw);
            if (manifest.optInt("schemaVersion") != 1) problems.add("Unsupported schema version " + manifest.opt("schemaVersion") + ".");
            JSONObject files = manifest.optJSONObject("files");
            if (files == null) { problems.add("The manifest lists no files."); return; }
            Set<String> names = new TreeSet<>();
            for (Iterator<String> keys = files.keys(); keys.hasNext();) names.add(keys.next());
            for (String name : names) {
                Path file = dir.resolve(name).normalize();
                if (!file.getParent().equals(dir.normalize())) problems.add(name + " is outside the run folder.");
                else if (!Files.isRegularFile(file)) problems.add(name + " is missing.");
                else if (!sha256(Files.readAllBytes(file)).equals(files.getJSONObject(name).optString("sha256"))) problems.add(name + " does not match its SHA-256 in the manifest.");
            }
        }
        boolean verified() { return problems.isEmpty(); }
        boolean partial() { return !"complete".equals(manifest.optString("status")); }
        JSONObject filters() { JSONObject f = manifest.optJSONObject("filters"); return f == null ? new JSONObject() : f; }
        JSONObject counts() { JSONObject c = manifest.optJSONObject("counts"); return c == null ? new JSONObject() : c; }
        JSONArray rows(String name) throws Exception {
            Path file = dir.resolve(name + ".json");
            return Files.isRegularFile(file) ? new JSONArray(Files.readString(file)) : new JSONArray();
        }
        String scope() {
            JSONObject f = filters();
            List<String> parts = new ArrayList<>();
            for (String key : Arrays.asList("sport", "country", "season", "league", "division", "since", "until")) if (!f.isNull(key) && !f.optString(key).isBlank()) parts.add(f.optString(key));
            return String.join(" · ", parts);
        }
        String statusText() { return !verified() ? "Failed checks" : partial() ? "Partial, stopped early" : "Complete"; }
    }

    static boolean validFolder(String folder) { return folder != null && folder.matches(FOLDER); }

    static final String OTHER_VERSION = "Another version of this league is imported. To use this version, remove the sample data, generate it again and then import this version.";

    /** Each league or the rankings folder holds its versions; only one of them can be in the sample batch. */
    static boolean otherVersionImported(String summary, Run run) {
        return !run.example && !summary.contains(run.marker) && summary.contains("Scraper run " + run.folder.substring(0, run.folder.indexOf('/') + 1));
    }

    static String sha256(byte[] raw) throws Exception {
        StringBuilder hex = new StringBuilder();
        for (byte b : MessageDigest.getInstance("SHA-256").digest(raw)) hex.append(String.format("%02x", b));
        return hex.toString();
    }

    /** Every run under the scrapes folder, newest first. The cache and anything not named by the scraper are ignored. */
    static List<String> folders(Path root) throws Exception {
        List<String> names = new ArrayList<>();
        if (!Files.isDirectory(root)) return names;
        List<Path> groups;
        try (Stream<Path> children = Files.list(root)) { groups = children.filter(p -> Files.isDirectory(p, LinkOption.NOFOLLOW_LINKS)).toList(); }
        for (Path group : groups) {
            try (Stream<Path> children = Files.list(group)) {
                children.filter(p -> Files.isDirectory(p, LinkOption.NOFOLLOW_LINKS) && Files.isRegularFile(p.resolve("manifest.json"))).map(p -> group.getFileName() + "/" + p.getFileName()).filter(ScrapedRunImport::validFolder).forEach(names::add);
            }
        }
        names.sort(Comparator.comparing((String name) -> name.replaceAll("^.*-([0-9]{8}-[0-9]{6})(-partial)?$", "$1")).reversed());
        return names;
    }

    // The base path is the deployment folder of the project in the local runtime; the scraper writes next to its own script.
    static Path scrapesRoot() { return Core.getConfiguration().getBasePath().toPath().toAbsolutePath().getParent().resolve("tools/scraper/scrapes"); }

    static Run example() throws Exception { return new Run(EXAMPLE_FOLDER, true, Core.getConfiguration().getResourcesPath().toPath().resolve(EXAMPLE_RESOURCE)); }

    private static Run open(String folder) throws Exception {
        if (EXAMPLE_FOLDER.equals(folder)) return example();
        if (!validFolder(folder)) throw new IllegalArgumentException("This is not a scraper run folder.");
        Path dir = scrapesRoot().resolve(folder).normalize();
        if (!Files.isDirectory(dir, LinkOption.NOFOLLOW_LINKS) || !Files.isRegularFile(dir.resolve("manifest.json"))) throw new IllegalArgumentException("The run folder " + folder + " is no longer there.");
        return new Run(folder, false, dir);
    }

    // ------------------------------------------------------------------ page commands

    /** Open|form|, Select|form|row ids, Import|form|. */
    static void dispatch(IContext context, String command) throws Exception {
        DevelopmentSamples.authorize(context);
        String[] p = command.split("\\|", -1);
        if (p.length != 3 || !p[1].matches("[0-9]+")) throw new IllegalArgumentException("Invalid import command.");
        IContext ctx = context.createSudoClone();
        IMendixObject form = Core.retrieveId(ctx, Core.createMendixIdentifier(Long.parseLong(p[1])));
        if (form == null || !form.getType().equals(M + "ScrapedRunForm")) throw new IllegalArgumentException("Reopen the import page.");
        switch (p[0]) {
            case "Open": open(ctx, context, form); return;
            case "Select": select(ctx, context, form, p[2]); return;
            case "Import": importSelected(ctx, context, form); return;
            case "FindLeagues": ScrapeJob.findLeagues(ctx, form); return;
            case "SelectLeagues": form.setValue(ctx, "SelectedLeagueRows", p[2]); return;
            case "StartScrape": ScrapeJob.start(ctx, form); return;
            case "StopScrape": ScrapeJob.stop(); return;
            default: throw new IllegalArgumentException("Invalid import command.");
        }
    }

    private static void open(IContext ctx, IContext caller, IMendixObject form) throws Exception {
        String summary = DevelopmentSamples.batchSummary();
        List<String> names = folders(scrapesRoot());
        names.add(EXAMPLE_FOLDER);
        for (String name : names) {
            IMendixObject row = Core.instantiate(ctx, M + "ScrapedRunRow");
            row.setValue(ctx, "Folder", name);
            row.setValue(ctx, M + "ScrapedRunRow_Form", form.getId());
            try {
                Run run = open(name);
                JSONObject c = run.counts();
                row.setValue(ctx, "RunStatus", run.statusText());
                row.setValue(ctx, "RunSource", run.example ? "Bundled fictional example" : "Scraper");
                row.setValue(ctx, "FinishedAt", run.manifest.optString("finishedAt"));
                row.setValue(ctx, "RunScope", run.scope());
                row.setValue(ctx, "LeagueCount", c.optInt("leagues"));
                row.setValue(ctx, "ClubCount", c.optInt("clubs"));
                row.setValue(ctx, "TeamCount", c.optInt("teams"));
                row.setValue(ctx, "PlayerCount", c.optInt("players"));
                row.setValue(ctx, "RankingCount", c.optInt("rankings"));
                row.setValue(ctx, "MatchCount", c.optInt("matches"));
                row.setValue(ctx, "GameCount", c.optInt("games"));
                row.setValue(ctx, "IsImported", summary.contains(run.marker));
            } catch (Exception e) {
                row.setValue(ctx, "RunStatus", "Unreadable");
                row.setValue(ctx, "RunScope", e.getMessage());
            }
        }
        form.setValue(ctx, "Message", names.size() == 1 ? "No scraper runs found in tools/scraper/scrapes yet. The bundled fictional example is listed so you can try the import." : "");
        clearPreview(ctx, form);
        ScrapeJob.describe(ctx, form);
    }

    private static void clearPreview(IContext ctx, IMendixObject form) {
        for (String a : Arrays.asList("SelectedFolder", "PreviewTitle", "PreviewCounts", "PreviewChecks", "PreviewWarnings")) form.setValue(ctx, a, "");
        form.setValue(ctx, "CanImport", false);
        form.setValue(ctx, "HasPreview", false);
    }

    private static void select(IContext ctx, IContext caller, IMendixObject form, String ids) throws Exception {
        clearPreview(ctx, form);
        form.setValue(ctx, "Message", "");
        String[] chosen = ids.isBlank() ? new String[0] : ids.split(",");
        if (chosen.length != 1) { if (chosen.length > 1) form.setValue(ctx, "Message", "Select one run to preview or import."); return; }
        IMendixObject row = Core.retrieveId(ctx, Core.createMendixIdentifier(Long.parseLong(chosen[0].trim())));
        if (row == null || !row.getType().equals(M + "ScrapedRunRow")) throw new IllegalArgumentException("Reopen the import page.");
        preview(ctx, form, open((String)row.getValue(ctx, "Folder")));
    }

    private static void preview(IContext ctx, IMendixObject form, Run run) throws Exception {
        boolean imported = DevelopmentSamples.batchSummary().contains(run.marker);
        JSONObject c = run.counts();
        form.setValue(ctx, "SelectedFolder", run.folder);
        form.setValue(ctx, "HasPreview", true);
        form.setValue(ctx, "PreviewTitle", run.folder + " · " + run.statusText());
        form.setValue(ctx, "PreviewCounts", c.optInt("leagues") + " leagues · " + c.optInt("clubs") + " clubs · " + c.optInt("teams") + " teams · " + c.optInt("players") + " players · " + c.optInt("rankings") + " ranking entries · " + c.optInt("matches") + " matches · " + c.optInt("games") + " games");
        form.setValue(ctx, "PreviewChecks", run.verified() ? "All files match the SHA-256 in the manifest." : String.join(" ", run.problems));
        List<String> notes = new ArrayList<>();
        if (run.partial()) notes.add("Partial run: " + run.manifest.optString("stopReason"));
        JSONArray warnings = run.manifest.optJSONArray("warnings");
        for (int i = 0; warnings != null && i < warnings.length(); i++) notes.add(warnings.optString(i));
        form.setValue(ctx, "PreviewWarnings", notes.isEmpty() ? "No warnings." : String.join("\n", notes));
        boolean otherVersion = otherVersionImported(DevelopmentSamples.batchSummary(), run);
        form.setValue(ctx, "CanImport", run.verified() && !imported && !otherVersion);
        if (imported) form.setValue(ctx, "Message", "This run is already imported. Importing it again adds nothing.");
        else if (otherVersion) form.setValue(ctx, "Message", OTHER_VERSION);
    }

    private static void importSelected(IContext ctx, IContext caller, IMendixObject form) throws Exception {
        Run run = open((String)form.getValue(ctx, "SelectedFolder"));
        if (!run.verified()) throw new IllegalStateException("This run failed its checks and cannot be imported. " + String.join(" ", run.problems));
        String outcome = DevelopmentSamples.importScrapedRun(caller, run);
        preview(ctx, form, run);
        form.setValue(ctx, "Message", outcome);
        for (IMendixObject row : Core.retrieveByPath(ctx, form, M + "ScrapedRunRow_Form")) if (run.folder.equals(row.getValue(ctx, "Folder"))) row.setValue(ctx, "IsImported", true);
    }

    // ------------------------------------------------------------------ sample records

    /** Appends a verified run through the seed; every record joins the private ownership manifest. Existing records are never matched by name or rewritten. Returns a summary. */
    static String populate(IContext ctx, DevelopmentSamples.Seed s, Run run) throws Exception {
        if (!run.verified()) throw new IllegalStateException("The run failed its checks.");
        if (!"badminton".equals(run.filters().optString("sport"))) throw new IllegalStateException("Only badminton runs can be imported for now.");
        String season = run.filters().isNull("season") ? "" : run.filters().optString("season");
        String capture = run.manifest.optString("finishedAt") + " " + run.folder;
        Map<String,JSONObject> leagues = new LinkedHashMap<>();
        for (Object item : array(run.rows("leagues"))) {
            JSONObject league = (JSONObject)item;
            if (league.isNull("startsOn") || league.isNull("endsOn")) throw new IllegalStateException("League " + league.optString("name") + " has no published dates. Nothing was imported.");
            leagues.put(league.getString("id"), league);
        }
        Map<String,IMendixObject> federations = new HashMap<>(), clubsByName = new HashMap<>(), clubs = new HashMap<>(), teams = new HashMap<>();
        Set<String> affiliations = new HashSet<>();
        for (JSONObject league : leagues.values()) {
            String organizer = league.optString("organizer");
            if (!federations.containsKey(organizer)) federations.put(organizer, federation(s, organizer, "Scraper run " + capture + "; " + league.optString("url")));
        }
        for (Object item : array(run.rows("clubs"))) {
            JSONObject c = (JSONObject)item; JSONObject league = leagues.get(c.getString("leagueId"));
            if (league == null) continue;
            String name = c.getString("name"), key = name.trim().toLowerCase(Locale.ROOT);
            IMendixObject club = clubsByName.get(key);
            if (club == null) {
                club = s.make("Club", "Name", "[DEMO] " + name, "Description", "Public club name; local development copy from scraper run " + run.folder + ". No real accounts or contact details are imported.",
                    "IsPublished", true, "LocationVerified", false, "RequiresApproval", true, "RequiresPayment", false);
                clubsByName.put(key, club);
            }
            clubs.put(c.getString("key"), club);
            IMendixObject federation = federations.get(league.optString("organizer"));
            if (affiliations.add(s.id(federation) + ":" + s.id(club))) {
                IMendixObject affiliation = s.make("FederationAffiliation", "ClubName", club.getValue(ctx, "Name"), "StartsOn", day(league.optString("startsOn")), "EndsOn", day(league.optString("endsOn")), "IsActive", true, "Status", "Approved");
                s.link(affiliation, "FederationAffiliation_Club", club); s.link(affiliation, "FederationAffiliation_Federation", federation);
            }
        }
        for (Object item : array(run.rows("teams"))) {
            JSONObject t = (JSONObject)item; IMendixObject club = clubs.get(t.getString("leagueId") + ":" + t.getString("clubId"));
            if (club == null) continue;
            IMendixObject team = s.make("CompetitionTeam", "Name", t.getString("name"), "ClubName", club.getValue(ctx, "Name"), "SeasonName", season, "IsActive", true);
            s.link(team, "CompetitionTeam_Club", club);
            teams.put(t.getString("key"), team);
        }
        // League, then division, then draw: a draw becomes a poule, as in the frozen public capture.
        Map<String,Map<String,Map<String,List<JSONObject>>>> tree = new LinkedHashMap<>();
        int skipped = 0, fixtures = 0, sheets = 0;
        for (Object item : array(run.rows("matches"))) {
            JSONObject match = (JSONObject)item;
            if (!leagues.containsKey(match.getString("leagueId")) || !teams.containsKey(match.getString("leagueId") + ":" + match.optString("homeTeamId")) || !teams.containsKey(match.getString("leagueId") + ":" + match.optString("awayTeamId"))) { skipped++; continue; }
            String draw = match.getString("division"); int cut = draw.lastIndexOf(" – ");
            tree.computeIfAbsent(match.getString("leagueId"), k -> new LinkedHashMap<>()).computeIfAbsent(cut > 0 ? draw.substring(0, cut) : draw, k -> new LinkedHashMap<>()).computeIfAbsent(match.getString("drawId"), k -> new ArrayList<>()).add(match);
        }
        for (Map.Entry<String,Map<String,Map<String,List<JSONObject>>>> byLeague : tree.entrySet()) {
            JSONObject league = leagues.get(byLeague.getKey());
            boolean national = league.optString("organizer").contains("Royal Belgian");
            IMendixObject federation = federations.get(league.optString("organizer"));
            Map<String,IMendixObject> formats = new HashMap<>();
            JSONObject source = new JSONObject().put("sport", "Badminton").put("level", national ? "National" : "Regional").put("startsOn", league.optString("startsOn")).put("endsOn", league.optString("endsOn"));
            for (Map.Entry<String,Map<String,List<JSONObject>>> division : byLeague.getValue().entrySet()) {
                String label = division.getKey(), category = label.startsWith("Heren") ? "Men" : label.startsWith("Dames") ? "Women" : "Mixed";
                IMendixObject format = formats.get(category);
                if (format == null) {
                    format = PublicCompetitionSamples.format(ctx, s, federation, source, category);
                    format.setValue(ctx, "Name", shortText("[DEMO] Badminton " + category + " " + league.optString("name")));
                    Core.commitWithoutEvents(ctx, format);
                    formats.put(category, format);
                }
                int maxTeams = 0;
                for (List<JSONObject> matches : division.getValue().values()) maxTeams = Math.max(maxTeams, teamIds(matches).size());
                IMendixObject competition = s.make("CompetitionLeague", "Name", "[DEMO] " + league.optString("name") + " / " + label,
                    "ResultSettingsConfigured", true, "UseGameBreakdown", true, "PointsPerGame", national,
                    "WinPoints", BigDecimal.valueOf(national ? 1 : 2), "DrawPoints", national ? BigDecimal.ZERO : BigDecimal.ONE, "LossPoints", BigDecimal.ZERO,
                    "SeasonName", season, "StartsOn", day(league.optString("startsOn")), "EndsOn", day(league.optString("endsOn")),
                    "RequestedPoules", division.getValue().size(), "MaxTeamsPerPoule", maxTeams, "DurationMinutes", 180L,
                    "Format", national ? "SingleRoundRobin" : "DoubleRoundRobin", "Stage", "Published",
                    "Info", "Scraper run " + capture + ". " + league.optString("url") + " Published pairings, dates and results are preserved. Unknown dates and times stay empty. Only teams with a captured match are in a poule. Duration and review settings are editable demo defaults.");
                s.link(competition, "CompetitionLeague_Federation", federation); s.link(competition, "CompetitionLeague_MatchFormat", format);
                int pouleNumber = 0;
                for (Map.Entry<String,List<JSONObject>> draw : division.getValue().entrySet()) {
                    List<String> teamKeys = teamIds(draw.getValue());
                    IMendixObject poule = s.make("CompetitionPoule", "Name", draw.getValue().get(0).getString("division"), "Number", ++pouleNumber, "TeamCount", teamKeys.size());
                    s.link(poule, "CompetitionPoule_League", competition);
                    Map<String,IMendixObject> entries = new HashMap<>(); int order = 0;
                    for (String teamId : teamKeys) {
                        IMendixObject team = teams.get(byLeague.getKey() + ":" + teamId);
                        IMendixObject club = Core.retrieveId(ctx, team.getValue(ctx, "MyFirstModule.CompetitionTeam_Club"));
                        IMendixObject entry = s.make("LeagueEntry", "TeamName", team.getValue(ctx, "Name"), "ClubName", club.getValue(ctx, "Name"), "ClubKey", s.id(club),
                            "EntryKey", PREFIX + s.marker + ":" + byLeague.getKey() + ":" + draw.getKey() + ":" + teamId, "Order", ++order);
                        s.link(entry, "LeagueEntry_Team", team); s.link(entry, "LeagueEntry_Club", club); s.link(entry, "LeagueEntry_League", competition); s.link(entry, "LeagueEntry_Poule", poule);
                        entries.put(teamId, entry);
                    }
                    Map<String,Integer> legs = new HashMap<>();
                    for (JSONObject match : draw.getValue()) {
                        String homeId = match.getString("homeTeamId"), awayId = match.getString("awayTeamId");
                        IMendixObject home = entries.get(homeId), away = entries.get(awayId);
                        IMendixObject hc = Core.retrieveId(ctx, home.getValue(ctx, "MyFirstModule.LeagueEntry_Club")), ac = Core.retrieveId(ctx, away.getValue(ctx, "MyFirstModule.LeagueEntry_Club"));
                        String pair = homeId.compareTo(awayId) < 0 ? homeId + ":" + awayId : awayId + ":" + homeId;
                        IMendixObject fixture = s.make("CompetitionFixture", "HomeName", home.getValue(ctx, "TeamName"), "AwayName", away.getValue(ctx, "TeamName"),
                            "HomeKey", s.id(home), "AwayKey", s.id(away), "HomeClubKey", s.id(hc), "AwayClubKey", s.id(ac), "VenueName", match.optString("venue"),
                            "StartsAt", instant(match.isNull("startsAt") ? "" : match.optString("startsAt")), "Status", "Published", "Version", 1L, "Leg", legs.merge(pair, 1, Integer::sum),
                            "PairKey", PREFIX + s.marker + ":" + match.getString("key"),
                            "Issue", shortText("Scraped " + run.manifest.optString("finishedAt") + " | " + (match.isNull("date") ? "" : match.optString("date")) + " | " + match.optString("round") + " | " + match.optString("sourceScore") + " | " + match.optString("sourceStatus")));
                        s.link(fixture, "CompetitionFixture_League", competition); s.link(fixture, "CompetitionFixture_Poule", poule);
                        s.link(fixture, "CompetitionFixture_Home", home); s.link(fixture, "CompetitionFixture_Away", away);
                        s.link(fixture, "CompetitionFixture_HomeClub", hc); s.link(fixture, "CompetitionFixture_AwayClub", ac);
                        fixtures++;
                        if (match.getJSONArray("games").length() > 0) { PublicCompetitionSamples.result(ctx, s, fixture, format, sheet(match), "Badminton"); sheets++; }
                    }
                }
            }
        }
        int[] players = players(ctx, s, run, teams);
        int[] rankings = rankings(ctx, s, run);
        return fixtures + " fixtures, " + sheets + " match sheets, " + players[0] + " roster players, " + rankings[0] + " rankings"
            + (skipped > 0 ? "; " + skipped + " matches left out because their team is not in the run" : "")
            + (players[1] > 0 ? "; " + players[1] + " players left out because the run links them to no team" : "")
            + (rankings[1] > 0 ? "; " + rankings[1] + " ranking entries left out for an unknown category or level" : "");
    }

    private static IMendixObject federation(DevelopmentSamples.Seed s, String name, String description) throws Exception {
        return s.make("Federation", "Name", "[DEMO] " + name, "Sport", "Badminton", "IsActive", true, "Currency", "EUR", "CensusMonth", 1L, "CensusDay", 1L, "BillingMethod", "ACTIVE_AT_CENSUS", "Description", description);
    }

    private static List<String> teamIds(List<JSONObject> matches) {
        Set<String> ids = new LinkedHashSet<>();
        for (JSONObject match : matches) { ids.add(match.getString("homeTeamId")); ids.add(match.getString("awayTeamId")); }
        return new ArrayList<>(ids);
    }

    /** The run's sheet in the shape of the frozen capture: lineup names carry the level printed on the sheet. */
    static JSONObject sheet(JSONObject match) {
        JSONObject copy = new JSONObject(match.toString());
        JSONArray games = new JSONArray();
        for (Object item : array(match.getJSONArray("games"))) {
            JSONObject game = new JSONObject(item.toString());
            for (String side : Arrays.asList("home", "away")) {
                JSONArray names = new JSONArray();
                for (Object raw : array(game.getJSONArray(side))) {
                    JSONObject player = (JSONObject)raw;
                    names.put(player.getString("name") + (player.isNull("level") ? "" : " (" + player.get("level") + ")"));
                }
                game.put(side, names);
            }
            games.put(game);
        }
        return copy.put("games", games);
    }

    /** Display-only roster players, without membership or account, like the frozen capture. */
    private static int[] players(IContext ctx, DevelopmentSamples.Seed s, Run run, Map<String,IMendixObject> teams) throws Exception {
        int created = 0, left = 0;
        for (Object item : array(run.rows("players"))) {
            JSONObject player = (JSONObject)item; JSONArray memberships = player.optJSONArray("teams");
            boolean linked = false;
            for (int i = 0; memberships != null && i < memberships.length(); i++) {
                IMendixObject team = teams.get(memberships.getJSONObject(i).getString("key"));
                if (team == null) continue;
                linked = true;
                String name = player.getString("name"), key = "public:" + team.getId().toLong() + ":" + name;
                if (PublicCompetitionSamples.first(ctx, "//MyFirstModule.CompetitionPlayer[MemberKey=$k]", key) != null) continue;
                IMendixObject roster = s.make("CompetitionPlayer", "MemberName", name, "MemberKey", key, "PersonKey", key);
                s.link(roster, "CompetitionPlayer_Team", team);
                created++;
            }
            if (!linked) left++;
        }
        return new int[]{created, left};
    }

    /** Published categories on a scheme of its own federation, so they never mix with a league's calculated ranking. */
    private static int[] rankings(IContext ctx, DevelopmentSamples.Seed s, Run run) throws Exception {
        JSONArray rows = run.rows("rankings");
        if (rows.length() == 0) return new int[]{0, 0};
        Map<String,String> codes = Map.of("HE", "MS", "DE", "WS", "HD", "MD", "DD", "WD", "GD", "XD");
        String week = rows.getJSONObject(0).optString("week");
        IMendixObject federation = federation(s, "Published ranking " + run.filters().optString("country") + " week " + week, "Published individual ranking from scraper run " + run.folder + ". Levels and points as published; not calculated here.");
        RankingService service = RankingService.forSamples(ctx, s.owned);
        IMendixObject scheme = service.createScheme(federation, "[DEMO] Published badminton ranking week " + week);
        int created = 0, left = 0;
        for (Object item : array(rows)) {
            JSONObject entry = (JSONObject)item;
            String code = codes.get(entry.optString("category").split("/")[0].trim().toUpperCase(Locale.ROOT));
            if (code == null || entry.isNull("level")) { left++; continue; }
            String note = "Published rank " + (entry.isNull("rank") ? "unknown" : entry.get("rank")) + ", " + (entry.isNull("points") ? "no" : entry.get("points")) + " points, " + entry.optString("club") + ", week " + entry.optString("week") + ".";
            if (service.importPublishedRanking(scheme, code, entry.getString("name"), entry.optString("club"), entry.getInt("level"), note)) created++; else left++;
        }
        return new int[]{created, left};
    }

    /** Coverage of one imported run inside the importing transaction: everything is owned and no login identity appears. */
    static void verify(IContext ctx, Set<Long> owned, String marker) throws Exception {
        List<IMendixObject> fixtures = Core.createXPathQuery("//MyFirstModule.CompetitionFixture[starts-with(PairKey,'" + PREFIX + marker + ":')]").execute(ctx);
        for (IMendixObject fixture : fixtures) {
            if (!owned.contains(fixture.getId().toLong())) throw new IllegalStateException("A scraped fixture is absent from the ownership manifest.");
            for (String side : Arrays.asList("Home", "Away")) {
                IMendixObject club = Core.retrieveId(ctx, fixture.getValue(ctx, "MyFirstModule.CompetitionFixture_" + side + "Club"));
                if (!owned.contains(club.getId().toLong())) throw new IllegalStateException("A scraped fixture points to a club outside the ownership manifest.");
                Collection<?> admins = club.getValue(ctx, "MyFirstModule.Club_Admins");
                if (admins != null && !admins.isEmpty()) throw new IllegalStateException("A scraped club received an administrator account.");
            }
        }
    }

    private static Date day(String value) { return value == null || value.isBlank() ? null : Date.from(LocalDate.parse(value).atStartOfDay(ZoneOffset.UTC).toInstant()); }
    private static Date instant(String value) { return value == null || value.isBlank() ? null : Date.from(OffsetDateTime.parse(value).toInstant()); }
    private static String shortText(String value) { return value.length() > 200 ? value.substring(0, 200) : value; }
    private static List<Object> array(JSONArray value) { List<Object> list = new ArrayList<>(); for (int i = 0; i < value.length(); i++) list.add(value.get(i)); return list; }
}
