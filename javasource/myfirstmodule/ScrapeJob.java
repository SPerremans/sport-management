package myfirstmodule;

import com.mendix.core.Core;
import com.mendix.systemwideinterfaces.core.*;
import com.mendix.thirdparty.org.json.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Runs tools/scraper from the import page: one job at a time, one league at a time. Callers pass DevelopmentSamples.authorize first, so this only runs for local administrators. */
final class ScrapeJob {
    private static final String M = "MyFirstModule.";
    /** Per league; the script's own limits and its pacing stay in force. */
    private static final String MAX_REQUESTS = "2000";
    private static final long SEARCH_MINUTES = 5;
    private static final ZoneId BRUSSELS = ZoneId.of("Europe/Brussels");
    private static volatile Thread worker;
    private static volatile Process process;
    private static volatile boolean stopRequested;
    private static volatile String status = "";
    private ScrapeJob() {}

    private record League(String id, String name) {}

    /** The season that is running now; seasons start on 1 July, as in the script. */
    static String currentSeason(LocalDate today) {
        int start = today.getMonthValue() >= 7 ? today.getYear() : today.getYear() - 1;
        return start + "-" + (start + 1);
    }

    static boolean validFilter(String sport, String country, String season) {
        return sport != null && sport.matches("[a-z]+") && country != null && country.matches("[A-Z]{3}") && season != null && season.matches("[0-9]{4}-[0-9]{4}");
    }

    private static String text(IContext ctx, IMendixObject form, String attribute, String fallback) {
        String value = form.getValue(ctx, attribute);
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static Path script() { return ScrapedRunImport.scrapesRoot().getParent().resolve("toernooi_scraper.py"); }

    private static List<String> command(String... arguments) {
        List<String> command = new ArrayList<>(List.of("python", "-X", "utf8", script().toString()));
        command.addAll(Arrays.asList(arguments));
        return command;
    }

    /** Fills the league rows from the site, or from the script's cache of the search pages. */
    static void findLeagues(IContext ctx, IMendixObject form) throws Exception {
        String sport = text(ctx, form, "FilterSport", "badminton"), country = text(ctx, form, "FilterCountry", "BEL").toUpperCase(), season = text(ctx, form, "FilterSeason", currentSeason(LocalDate.now(BRUSSELS))), name = text(ctx, form, "FilterName", "");
        form.setValue(ctx, "FilterSport", sport); form.setValue(ctx, "FilterCountry", country); form.setValue(ctx, "FilterSeason", season);
        form.setValue(ctx, "SelectedLeagueRows", "");
        for (IMendixObject row : Core.retrieveByPath(ctx, form, M + "ScrapedLeagueRow_Form")) Core.delete(ctx, row);
        if (!validFilter(sport, country, season)) { form.setValue(ctx, "LeagueMessage", "Use a sport such as badminton, a country such as BEL and a season such as 2026-2027."); return; }
        if (worker != null && worker.isAlive()) { form.setValue(ctx, "LeagueMessage", "A scrape is running. Search again when it has finished."); return; }
        Path output = Files.createTempFile("scrape-leagues", ".json");
        try {
            Process search = new ProcessBuilder(command("leagues", "--json", "--sport", sport, "--country", country, "--season", season)).redirectErrorStream(true).redirectOutput(output.toFile()).start();
            if (!search.waitFor(SEARCH_MINUTES, TimeUnit.MINUTES)) { search.destroyForcibly(); form.setValue(ctx, "LeagueMessage", "The league search took too long and was stopped."); return; }
            String raw = Files.readString(output, StandardCharsets.UTF_8);
            if (search.exitValue() != 0) { form.setValue(ctx, "LeagueMessage", "The league search failed: " + tail(raw)); return; }
            JSONObject result = new JSONObject(raw);
            JSONArray leagues = result.getJSONArray("leagues");
            int shown = 0;
            String lowered = name.toLowerCase();
            for (int i = 0; i < leagues.length(); i++) {
                JSONObject league = leagues.getJSONObject(i);
                if (!league.optString("name").toLowerCase().contains(lowered)) continue;
                IMendixObject row = Core.instantiate(ctx, M + "ScrapedLeagueRow");
                row.setValue(ctx, "LeagueId", league.getString("id"));
                row.setValue(ctx, "LeagueName", league.optString("name"));
                row.setValue(ctx, "Organizer", league.optString("organizer"));
                row.setValue(ctx, "StartsOn", league.optString("startsOn"));
                row.setValue(ctx, M + "ScrapedLeagueRow_Form", form.getId());
                shown++;
            }
            form.setValue(ctx, "LeagueMessage", shown + " leagues found for " + season + ". " + result.optInt("youthLeftOut") + " youth leagues left out.");
        } catch (java.io.IOException e) {
            form.setValue(ctx, "LeagueMessage", "Python could not be started: " + e.getMessage());
        } finally {
            Files.deleteIfExists(output);
        }
    }

    /** Starts the background job for the selected rows. Each league gets its own timestamped version folder from the script. */
    static synchronized void start(IContext ctx, IMendixObject form) throws Exception {
        if (worker != null && worker.isAlive()) throw new IllegalStateException("A scrape is already running.");
        String sport = text(ctx, form, "FilterSport", ""), country = text(ctx, form, "FilterCountry", ""), season = text(ctx, form, "FilterSeason", "");
        if (!validFilter(sport, country, season)) throw new IllegalArgumentException("Search for leagues first.");
        List<League> leagues = new ArrayList<>();
        String chosen = text(ctx, form, "SelectedLeagueRows", "");
        for (String id : chosen.isBlank() ? new String[0] : chosen.split(",")) {
            IMendixObject row = Core.retrieveId(ctx, Core.createMendixIdentifier(Long.parseLong(id.trim())));
            if (row == null || !row.getType().equals(M + "ScrapedLeagueRow")) throw new IllegalArgumentException("Search for leagues again.");
            leagues.add(new League(row.getValue(ctx, "LeagueId"), row.getValue(ctx, "LeagueName")));
        }
        boolean rankings = Boolean.TRUE.equals(form.getValue(ctx, "IncludeRankings"));
        if (leagues.isEmpty() && !rankings) throw new IllegalArgumentException("Select at least one league, or include the rankings.");
        Files.createDirectories(ScrapedRunImport.scrapesRoot());
        stopRequested = false;
        status = "Starting.";
        worker = new Thread(() -> runAll(leagues, rankings, sport, country, season), "scrape-job");
        worker.setDaemon(true);
        worker.start();
    }

    private static void runAll(List<League> leagues, boolean rankings, String sport, String country, String season) {
        int total = leagues.size() + (rankings ? 1 : 0), done = 0;
        Path log = ScrapedRunImport.scrapesRoot().resolve("_job.log");
        try {
            Files.deleteIfExists(log);
            for (League league : leagues) {
                status = "Scraping " + (done + 1) + " of " + total + ": " + league.name() + ". Started " + now() + ".";
                if (!runOne(log, league.name(), done, total, "run", "--sport", sport, "--country", country, "--season", season, "--league-id", league.id(), "--include", "clubs,players,matches", "--max-requests", MAX_REQUESTS)) return;
                done++;
            }
            if (rankings) {
                status = "Scraping " + (done + 1) + " of " + total + ": rankings. Started " + now() + ".";
                if (!runOne(log, "the rankings", done, total, "run", "--sport", sport, "--country", country, "--include", "rankings", "--max-requests", MAX_REQUESTS)) return;
                done++;
            }
            status = "Finished " + now() + ": " + done + " of " + total + " captured. Refresh to see the new versions.";
        } catch (Exception e) {
            status = "Stopped after " + done + " of " + total + ": " + e.getMessage();
            Core.getLogger("ScrapeJob").error("The scrape job failed.", e);
        } finally {
            process = null;
        }
    }

    /** False stops the queue: a refusal or an early stop must not be followed by more requests. */
    private static boolean runOne(Path log, String label, int done, int total, String... arguments) throws Exception {
        if (stopRequested) { status = "Stopped by you after " + done + " of " + total + "."; return false; }
        process = new ProcessBuilder(command(arguments)).redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.appendTo(log.toFile())).start();
        int exit = process.waitFor();
        if (stopRequested) { status = "Stopped by you during " + label + " after " + done + " of " + total + ". It left no complete version."; return false; }
        if (exit != 0) { status = "Stopped at " + label + " after " + done + " of " + total + ": " + tail(Files.readString(log, StandardCharsets.UTF_8)); return false; }
        return true;
    }

    static void stop() {
        stopRequested = true;
        Process running = process;
        if (running != null) running.destroy();
    }

    /** The job lives in this runtime only; after a restart the page shows no job. */
    static void describe(IContext ctx, IMendixObject form) {
        form.setValue(ctx, "JobStatus", status);
        form.setValue(ctx, "JobRunning", worker != null && worker.isAlive());
    }

    private static String now() { return LocalTime.now(BRUSSELS).withNano(0).withSecond(0).toString(); }

    private static String tail(String raw) {
        String trimmed = raw.trim();
        return trimmed.substring(Math.max(0, trimmed.length() - 300));
    }
}
