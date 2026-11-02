package myfirstmodule;

import com.mendix.core.Core;
import com.mendix.systemwideinterfaces.core.*;
import com.mendix.thirdparty.org.json.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

/**
 * The design workspace of a ranking scheme: a working configuration next to imported game data, a replay of those games and scenarios to compare.
 * Nothing here changes player rankings until Apply. Data sets and scenarios are persistent without client access; the page sees rows of the non-persistent RankingForm.
 * Called by RankingService, which already checked that the user manages the federation.
 */
final class RankingWorkbench {
    private static final String M = "MyFirstModule.";
    static final int MAX_YEARS = 3, MAX_PLAYER_ROWS = 400;
    /** key, group, label, help; shown only for the methods in the last column (empty: every method). */
    private static final String[][] SETTINGS = {
        {"windowWeeks", "Counting", "Window (weeks)", "Only games in this period count.", ""},
        {"maxMatches", "Counting", "Most recent games counted", "Older games in the window are ignored.", "AveragePoints"},
        {"minDivisor", "Counting", "Divide by at least (games)", "Stops a player with few games from getting a high average.", "AveragePoints"},
        {"bestCount", "Counting", "Best results counted", "Only the best results in the window are added up.", "BestResults"},
        {"ignoreHigherLosses", "Counting", "Ignore losses against a directly higher level (yes/no)", "Such a loss then does not lower the average.", "AveragePoints"},
        {"lossEqual", "Counting", "Loss against the same level costs", "One win is worth 1.", "WinBalance"},
        {"lossOneBelow", "Counting", "Loss one level lower costs", "One win is worth 1.", "WinBalance"},
        {"lossTwoBelow", "Counting", "Loss two or more levels lower costs", "One win is worth 1.", "WinBalance"},
        {"eloK", "Rating", "K factor", "How far one game moves the rating. FIDE uses 20, German table tennis 16.", "EloRating"},
        {"eloKNew", "Rating", "K factor for new players", "New players find their level faster with a higher value.", "EloRating"},
        {"eloNewGames", "Rating", "New player until this many games", "", "EloRating"},
        {"eloScale", "Rating", "Rating scale", "A gap of this size means 10 to 1 expected wins. FIDE uses 400, German table tennis 150.", "EloRating"},
        {"eloStart", "Rating", "Start rating of the lowest level", "", "EloRating"},
        {"eloStep", "Rating", "Rating per level at the start", "", "EloRating"},
        {"maxUp", "Movement", "Most levels up per review (0 for no limit)", "", ""},
        {"maxDown", "Movement", "Most levels down per review (0 for no limit)", "", ""},
        {"maxSpread", "Movement", "Largest level difference between disciplines (empty for no limit)", "", ""},
        {"inactiveMinMatches", "Inactivity", "Games needed in the window to count as active", "Fewer games: no demotion for results, and no place in a pyramid.", ""},
        {"inactiveWeeks", "Inactivity", "Inactive for this many weeks before a drop", "", ""},
        {"inactiveDrop", "Inactivity", "Levels lost when inactive (0 for none)", "", ""},
        {"countWalkovers", "Data", "Count walkovers (yes/no)", "A walkover is marked in the score of the imported data.", ""},
        {"startFromPublished", "Data", "Start players at the level printed on their first sheet (yes/no)", "No means every player starts at the lowest level.", ""},
        {"disciplines", "Data", "Disciplines to rank (empty for all)", "Codes from the data, separated by commas.", ""}};

    private final RankingService service; private final IContext ctx; private final String actor;

    RankingWorkbench(RankingService service, IContext ctx, String actor) { this.service = service; this.ctx = ctx; this.actor = actor; }

    // ------------------------------------------------------------------ configuration as JSON

    /** The scheme's own values as a configuration: existing schemes have no configuration yet and use the average-points method. */
    JSONObject liveConfig(IMendixObject s) throws Exception {
        String stored = service.text(s, "ConfigJson");
        JSONObject c = stored.isEmpty() ? new JSONObject() : new JSONObject(stored);
        if (!c.has("method")) { RankingEngine.Config d = RankingEngine.preset("BadmintonAverage", null); writeEngine(c, d); c.remove("levels"); }
        c.put("frequency", service.text(s, "Frequency").isEmpty() ? c.optString("frequency", "FirstMondayMonthly") : service.text(s, "Frequency"));
        c.put("windowWeeks", service.num(s, "WindowWeeks")); c.put("maxMatches", service.num(s, "MaxMatches")); c.put("minDivisor", service.num(s, "MinDivisor")); c.put("maxSpread", service.num(s, "MaxSpread"));
        c.put("inactiveWeeks", service.num(s, "InactiveWeeks")); c.put("inactiveMinMatches", service.num(s, "InactiveMinMatches")); c.put("inactiveDrop", service.num(s, "InactiveDrop")); c.put("ignoreHigherLosses", service.bool(s, "IgnoreHigherLosses"));
        JSONArray levels = new JSONArray();
        for (IMendixObject l : service.levels(s, false)) levels.put(new JSONObject().put("name", service.levelName(l)).put("points", service.dec(l, "WinPoints")).put("promote", service.dec(l, "PromoteAt")).put("demote", service.dec(l, "DemoteBelow")).put("share", service.dec(l, "SharePercent")));
        c.put("levels", levels);
        JSONObject bonus = new JSONObject();
        for (IMendixObject d : service.children("RankingDiscipline", "RankingDiscipline_Scheme", s)) if (service.num(d, "BonusThreshold") > 0) bonus.put(service.text(d, "Code"), service.num(d, "BonusThreshold"));
        c.put("bonus", bonus);
        return c;
    }

    static void writeEngine(JSONObject c, RankingEngine.Config e) throws Exception {
        RankingRules.Params p = e.params;
        c.put("method", e.method).put("placement", e.placement).put("frequency", e.frequency).put("windowWeeks", p.windowWeeks()).put("maxMatches", p.maxMatches()).put("minDivisor", p.minDivisor()).put("maxSpread", p.maxSpread())
            .put("inactiveWeeks", p.inactiveWeeks()).put("inactiveMinMatches", p.inactiveMinMatches()).put("inactiveDrop", p.inactiveDrop()).put("ignoreHigherLosses", p.ignoreHigherLosses())
            .put("maxUp", e.maxUp).put("maxDown", e.maxDown).put("bestCount", e.bestCount).put("eloK", e.eloK).put("eloKNew", e.eloKNew).put("eloNewGames", e.eloNewGames).put("eloScale", e.eloScale).put("eloStart", e.eloStart).put("eloStep", e.eloStep)
            .put("lossEqual", e.lossEqual).put("lossOneBelow", e.lossOneBelow).put("lossTwoBelow", e.lossTwoBelow).put("countWalkovers", e.countWalkovers).put("startFromPublished", e.startFromPublished).put("disciplines", String.join(",", new TreeSet<>(e.disciplines)));
        JSONObject bonus = new JSONObject(); for (Map.Entry<String, Integer> b : e.bonusThresholds.entrySet()) bonus.put(b.getKey(), b.getValue()); c.put("bonus", bonus);
        JSONArray levels = new JSONArray();
        for (int l = 1; l <= e.levels(); l++) levels.put(new JSONObject().put("name", e.name(l)).put("points", e.table.winPoints()[l]).put("promote", e.table.promoteAt()[l]).put("demote", e.table.demoteBelow()[l]).put("share", e.shares == null ? 0 : e.shares[l]));
        c.put("levels", levels);
    }

    static RankingEngine.Config readEngine(JSONObject c) throws Exception {
        RankingEngine.Config e = new RankingEngine.Config(); JSONArray levels = c.getJSONArray("levels"); int n = levels.length();
        double[] win = new double[n + 1], up = new double[n + 1], down = new double[n + 1]; e.shares = new double[n + 1]; e.names = new String[n + 1];
        for (int i = 0; i < n; i++) { JSONObject l = levels.getJSONObject(i); e.names[i + 1] = l.optString("name", String.valueOf(i + 1)); win[i + 1] = l.optDouble("points", 0.0); up[i + 1] = l.optDouble("promote", 0.0); down[i + 1] = l.optDouble("demote", 0.0); e.shares[i + 1] = l.optDouble("share", 0.0); }
        e.table = new RankingRules.Table(n, win, up, down);
        e.params = new RankingRules.Params(n, c.optInt("windowWeeks", 52), c.optInt("maxMatches", 20), c.optInt("minDivisor", 1), c.optInt("maxSpread", -1), c.optInt("inactiveWeeks", 104), c.optInt("inactiveMinMatches", 1), c.optInt("inactiveDrop", 0), c.optBoolean("ignoreHigherLosses", false));
        e.method = c.optString("method", RankingEngine.AVERAGE); e.placement = c.optString("placement", RankingEngine.THRESHOLDS); e.frequency = c.optString("frequency", "FirstMondayMonthly");
        e.maxUp = c.optInt("maxUp", 1); e.maxDown = c.optInt("maxDown", 1); e.bestCount = c.optInt("bestCount", 6); e.eloK = c.optDouble("eloK", 20.0); e.eloKNew = c.optDouble("eloKNew", 40.0); e.eloNewGames = c.optInt("eloNewGames", 30);
        e.eloScale = c.optDouble("eloScale", 400.0); e.eloStart = c.optDouble("eloStart", 1000.0); e.eloStep = c.optDouble("eloStep", 100.0); e.lossEqual = c.optDouble("lossEqual", 1.0); e.lossOneBelow = c.optDouble("lossOneBelow", 2.0); e.lossTwoBelow = c.optDouble("lossTwoBelow", 5.0);
        e.countWalkovers = c.optBoolean("countWalkovers", false); e.startFromPublished = c.optBoolean("startFromPublished", true);
        for (String d : c.optString("disciplines", "").split(",")) if (!d.isBlank()) e.disciplines.add(d.trim().toUpperCase(Locale.ROOT));
        JSONObject bonus = c.optJSONObject("bonus"); String[] codes = bonus == null ? null : JSONObject.getNames(bonus); if (codes != null) for (String k : codes) e.bonusThresholds.put(k, bonus.getInt(k));
        return e;
    }

    // ------------------------------------------------------------------ scenarios

    private List<IMendixObject> scenarios(IMendixObject s) throws Exception { return service.children("RankingScenario", "RankingScenario_Scheme", s); }

    /** The working scenario holds the design in progress; it is created from the scheme's own values the first time. */
    IMendixObject working(IMendixObject s) throws Exception {
        for (IMendixObject o : scenarios(s)) if (service.bool(o, "IsWorking")) return o;
        IMendixObject o = service.make("RankingScenario", "Name", "Working design", "IsWorking", true, "IsBaseline", false, "CreatedBy", actor, "CreatedOn", new Date(), "ConfigJson", liveConfig(s).toString(), "ResultJson", "", "Summary", "", "DatasetName", "");
        service.link(o, "RankingScenario_Scheme", s);
        return o;
    }

    private IMendixObject baseline(IMendixObject s) throws Exception { for (IMendixObject o : scenarios(s)) if (service.bool(o, "IsBaseline") && !service.bool(o, "IsWorking")) return o; return null; }

    private JSONObject configOf(IMendixObject scenario) throws Exception { return new JSONObject(service.text(scenario, "ConfigJson")); }

    private void store(IMendixObject working, JSONObject config, boolean keepResult) throws Exception {
        working.setValue(ctx, "ConfigJson", config.toString());
        if (!keepResult) { working.setValue(ctx, "ResultJson", ""); working.setValue(ctx, "Summary", ""); }
        service.commit(working);
    }

    // ------------------------------------------------------------------ commands

    void open(IMendixObject s) throws Exception {
        IMendixObject next = Core.instantiate(ctx, M + "RankingForm");
        next.setValue(ctx, "SchemeKey", service.id(s)); next.setValue(ctx, "Mode", "Lab"); next.setValue(ctx, "CanManage", true); next.setValue(ctx, "HasScheme", true);
        working(s); refresh(next, s);
        service.flags(next, service.state(next));
        Core.microflowCall(M + "ACT_Ranking_ShowWorkbench").withParam("Form", next).execute(ctx);
    }

    void command(String op, IMendixObject form, IMendixObject state, String ids) throws Exception {
        IMendixObject s = service.requireScheme(service.text(form, "SchemeKey")); IMendixObject w = working(s); JSONObject c = configOf(w); String message = null;
        switch (op) {
            case "Preset": {
                Object chosen = form.getValue(ctx, "LabPreset"); RankingService.require(chosen != null, "Choose a starting point first.");
                JSONObject next = new JSONObject(); writeEngine(next, RankingEngine.preset(chosen.toString(), null)); next.put("dataset", c.optString("dataset", "")); store(w, next, false);
                message = "Starting point loaded: " + RankingEngine.presetLabel(chosen.toString()) + ". Every value stays editable and only takes effect when you apply the design."; break; }
            case "Method": { Object chosen = form.getValue(ctx, "LabMethod"); if (chosen != null) { c.put("method", chosen.toString()); store(w, c, false); } break; }
            case "Placement": { Object chosen = form.getValue(ctx, "LabPlacement"); if (chosen != null) { c.put("placement", chosen.toString()); store(w, c, false); } break; }
            case "Frequency": { Object chosen = form.getValue(ctx, "Frequency"); if (chosen != null) { c.put("frequency", chosen.toString()); store(w, c, false); } break; }
            case "Cell": cell(ids, c, w); form.setValue(ctx, "Schedule", "The design changed: run the simulation again. The results below are of the previous design."); form.setValue(ctx, "Warnings", ""); service.flags(form, state); return;
            case "LevelAdd": addLevel(c); store(w, c, false); message = "A lowest level was added. Name it and set its values."; break;
            case "LevelRemove": { removeLevel(c, service.selected(form, "LabLevel")); store(w, c, false); message = "The level was removed from the design."; break; }
            case "Run": { message = run(s, w, c); break; }
            case "Find": break;
            case "Import": form.setValue(ctx, "ImportText", ""); form.setValue(ctx, "Value1", ""); form.setValue(ctx, "Mode", "LabImport"); form.setValue(ctx, "Title", "Import games"); Core.microflowCall(M + "ACT_Ranking_ShowImport").withParam("Form", form).execute(ctx); return;
            case "FromMatches": { message = fromMatches(s, c, w); break; }
            case "DatasetUse": { IMendixObject d = service.load(service.requireKey(form, "Dataset", "Ds:")); c.put("dataset", service.id(d)); store(w, c, false); message = "The simulation now uses " + service.text(d, "Name") + "."; break; }
            case "DatasetDelete": {
                IMendixObject d = service.load(service.requireKey(form, "Dataset", "Ds:"));
                if (service.id(d).equals(c.optString("dataset"))) { c.put("dataset", ""); store(w, c, false); }
                Core.delete(ctx, d); message = "The game data was removed. Scenarios keep their results."; break; }
            case "SaveAs": service.openEdit(form, "LabSave", "Save scenario", new String[][]{{"Scenario name", "Scenario " + (scenarios(s).size())}}); return;
            case "ScenarioLoad": { IMendixObject o = service.load(service.requireKey(form, "Scenario", "Sc:")); w.setValue(ctx, "ResultJson", service.text(o, "ResultJson")); w.setValue(ctx, "Summary", service.text(o, "Summary")); w.setValue(ctx, "DatasetName", service.text(o, "DatasetName")); store(w, configOf(o), true); message = "Scenario " + service.text(o, "Name") + " is now the working design."; break; }
            case "ScenarioBaseline": {
                IMendixObject o = service.load(service.requireKey(form, "Scenario", "Sc:"));
                for (IMendixObject other : scenarios(s)) if (service.bool(other, "IsBaseline")) { other.setValue(ctx, "IsBaseline", false); service.commit(other); }
                o.setValue(ctx, "IsBaseline", true); service.commit(o); message = "Results are now compared with " + service.text(o, "Name") + "."; break; }
            case "ScenarioDelete": { IMendixObject o = service.load(service.requireKey(form, "Scenario", "Sc:")); Core.delete(ctx, o); message = "The scenario was deleted."; break; }
            case "Apply": {
                RankingService.require(!service.text(w, "ResultJson").isEmpty(), "Run the simulation first, so you can see what this design does.");
                service.openEdit(form, "LabApply", "Apply this design to the ranking", new String[][]{{"Reason, kept in the ranking's note", ""}}); return; }
            case "Player": { detail(form, s, c, ids); service.flags(form, state); return; }
            default: throw new IllegalArgumentException("Unknown design command.");
        }
        refresh(form, s); if (message != null) form.setValue(ctx, "Message", message); service.flags(form, state);
    }

    /** Dialog answers of the workspace. */
    void save(IMendixObject form, String mode, String[] v) throws Exception {
        IMendixObject s = service.requireScheme(service.text(form, "SchemeKey")); IMendixObject w = working(s); JSONObject c = configOf(w); String message;
        switch (mode) {
            case "LabImport": message = importText(s, c, w, v[1], service.text(form, "ImportText")); break;
            case "LabSave": {
                RankingService.require(!v[1].isEmpty(), "Enter a name for the scenario.");
                RankingService.require(!service.text(w, "ResultJson").isEmpty(), "Run the simulation first: a scenario keeps its result for comparing.");
                IMendixObject o = service.make("RankingScenario", "Name", v[1], "IsWorking", false, "IsBaseline", baseline(s) == null, "CreatedBy", actor, "CreatedOn", new Date(), "RanOn", w.getValue(ctx, "RanOn"), "DatasetName", service.text(w, "DatasetName"), "ConfigJson", service.text(w, "ConfigJson"), "ResultJson", service.text(w, "ResultJson"), "Summary", service.text(w, "Summary"));
                service.link(o, "RankingScenario_Scheme", s); message = "Scenario " + v[1] + " saved."; break; }
            case "LabApply": message = apply(s, w, c, v[1]); break;
            default: throw new IllegalArgumentException("Nothing to save.");
        }
        form.setValue(ctx, "Mode", "Lab"); refresh(form, s); form.setValue(ctx, "Message", message);
    }

    private void cell(String ids, JSONObject c, IMendixObject w) throws Exception {
        IMendixObject row = Core.retrieveId(ctx, Core.createMendixIdentifier(Long.parseLong(ids.split(",")[0].trim())));
        RankingService.require(row != null && row.getType().equals(M + "RankingRow"), "The row is no longer available. Refresh the page.");
        String key = service.text(row, "RecordKey");
        try {
            if (key.startsWith("Set:")) setting(c, key.substring(4), service.text(row, "C3").trim());
            else if (key.startsWith("Lv:")) {
                JSONArray levels = c.getJSONArray("levels"); int i = Integer.parseInt(key.substring(3)); JSONObject l = levels.getJSONObject(i);
                String name = service.text(row, "C2").trim(); RankingService.require(!name.isEmpty(), "A level needs a name.");
                for (int k = 0; k < levels.length(); k++) RankingService.require(k == i || !levels.getJSONObject(k).optString("name").equalsIgnoreCase(name), "The level name " + name + " is already used.");
                l.put("name", name); l.put("points", RankingService.number(service.text(row, "C3"), "points")); l.put("promote", RankingService.number(service.text(row, "C4"), "promotion score")); l.put("demote", RankingService.number(service.text(row, "C5"), "demotion score")); l.put("share", RankingService.number(service.text(row, "C6"), "share"));
            }
        } catch (RuntimeException invalid) { refreshRow(row, c, key); throw invalid; }
        store(w, c, false);
    }

    private void refreshRow(IMendixObject row, JSONObject c, String key) throws Exception {
        if (key.startsWith("Set:")) row.setValue(ctx, "C3", settingValue(c, key.substring(4)));
        else if (key.startsWith("Lv:")) { JSONObject l = c.getJSONArray("levels").getJSONObject(Integer.parseInt(key.substring(3))); row.setValue(ctx, "C2", l.optString("name")); row.setValue(ctx, "C3", RankingService.plain(l.optDouble("points", 0.0))); row.setValue(ctx, "C4", RankingService.plain(l.optDouble("promote", 0.0))); row.setValue(ctx, "C5", RankingService.plain(l.optDouble("demote", 0.0))); row.setValue(ctx, "C6", RankingService.plain(l.optDouble("share", 0.0))); }
    }

    private static void setting(JSONObject c, String key, String raw) throws Exception {
        switch (key) {
            case "ignoreHigherLosses": case "countWalkovers": case "startFromPublished": {
                String yn = raw.toLowerCase(Locale.ROOT); RankingService.require(yn.equals("yes") || yn.equals("no"), "Answer yes or no."); c.put(key, yn.equals("yes")); return; }
            case "disciplines": c.put(key, raw.toUpperCase(Locale.ROOT)); return;
            case "maxSpread": if (raw.isEmpty()) { c.put(key, -1); return; } break;
            default:
        }
        double value = RankingService.number(raw, "value"); RankingService.require(value >= 0, "Enter a value of 0 or more.");
        RankingService.require(!Set.of("windowWeeks", "maxMatches", "minDivisor", "bestCount", "eloScale").contains(key) || value >= 1, "This value must be at least 1.");
        if (key.equals("inactiveWeeks")) RankingService.require(value >= c.optInt("windowWeeks", 1), "The inactivity period cannot be shorter than the window.");
        c.put(key, value == Math.rint(value) ? (Object)(int)value : (Object)value);
    }

    private static String settingValue(JSONObject c, String key) throws Exception {
        if (!c.has(key)) return "";
        Object v = c.get(key);
        if (v instanceof Boolean) return (Boolean)v ? "yes" : "no";
        if (key.equals("maxSpread") && c.optInt(key) < 0) return "";
        return v instanceof Number ? RankingService.plain(((Number)v).doubleValue()) : String.valueOf(v);
    }

    private static void addLevel(JSONObject c) throws Exception {
        JSONArray levels = c.getJSONArray("levels"); RankingService.require(levels.length() < 30, "A ranking can have at most 30 levels.");
        JSONObject last = levels.getJSONObject(levels.length() - 1);
        levels.put(new JSONObject().put("name", "Level " + (levels.length() + 1)).put("points", Math.max(1, Math.round(last.optDouble("points", 1.0) * 0.69))).put("promote", last.optDouble("demote", 0.0)).put("demote", 0.0).put("share", last.optDouble("share", 0.0)));
    }

    private static void removeLevel(JSONObject c, String key) throws Exception {
        JSONArray levels = c.getJSONArray("levels"); RankingService.require(levels.length() > 2, "A ranking needs at least two levels.");
        RankingService.require(key != null && key.startsWith("Lv:"), "Select one level first."); levels.remove(Integer.parseInt(key.substring(3)));
    }

    // ------------------------------------------------------------------ data sets

    /** Same shape as the sample resource: a table of names and rows of date, discipline, winners, losers, winner levels, loser levels, score, flag, source, event. */
    static JSONObject datasetJson(List<RankingEngine.Game> games) throws Exception {
        Map<String, Integer> names = new LinkedHashMap<>(); JSONArray rows = new JSONArray();
        for (RankingEngine.Game g : games) {
            JSONArray w = new JSONArray(), l = new JSONArray(), wl = new JSONArray(), ll = new JSONArray();
            for (String n : g.winners()) { names.putIfAbsent(n, names.size()); w.put(names.get(n)); }
            for (String n : g.losers()) { names.putIfAbsent(n, names.size()); l.put(names.get(n)); }
            for (Integer v : g.winnerLevels()) wl.put(v == null ? JSONObject.NULL : v);
            for (Integer v : g.loserLevels()) ll.put(v == null ? JSONObject.NULL : v);
            rows.put(new JSONArray().put(g.date().toString()).put(g.discipline()).put(w).put(l).put(wl).put(ll).put("").put(g.flag()).put(0).put(g.event() == null ? "" : g.event()));
        }
        JSONArray players = new JSONArray(); for (String n : names.keySet()) players.put(n);
        return new JSONObject().put("players", players).put("games", rows);
    }

    static List<RankingEngine.Game> readGames(JSONObject data) throws Exception {
        JSONArray players = data.getJSONArray("players"), rows = data.getJSONArray("games"); List<RankingEngine.Game> games = new ArrayList<>(rows.length());
        for (int i = 0; i < rows.length(); i++) {
            JSONArray r = rows.getJSONArray(i);
            games.add(new RankingEngine.Game(LocalDate.parse(r.getString(0)), r.getString(1), names(players, r.getJSONArray(2)), names(players, r.getJSONArray(3)), levels(r.getJSONArray(4)), levels(r.getJSONArray(5)), r.getString(7), r.optString(9, "")));
        }
        return games;
    }

    private static List<String> names(JSONArray players, JSONArray codes) throws Exception { List<String> out = new ArrayList<>(codes.length()); for (int i = 0; i < codes.length(); i++) out.add(players.getString(codes.getInt(i))); return out; }
    private static List<Integer> levels(JSONArray values) throws Exception { List<Integer> out = new ArrayList<>(values.length()); for (int i = 0; i < values.length(); i++) out.add(values.isNull(i) ? null : Integer.valueOf(values.getInt(i))); return out; }

    /** Stores games as a data set: at most the latest three years are kept. */
    IMendixObject createDataset(IMendixObject s, String name, String sport, String note, List<RankingEngine.Game> all) throws Exception {
        RankingService.require(!all.isEmpty(), "There are no games to store.");
        LocalDate latest = all.stream().map(RankingEngine.Game::date).max(Comparator.naturalOrder()).get(), from = latest.minusYears(MAX_YEARS);
        List<RankingEngine.Game> games = new ArrayList<>(); for (RankingEngine.Game g : all) if (g.date().isAfter(from)) games.add(g);
        games.sort(Comparator.comparing(RankingEngine.Game::date));
        Set<String> people = new HashSet<>(), disciplines = new TreeSet<>(); boolean printed = false;
        for (RankingEngine.Game g : games) {
            disciplines.add(g.discipline()); people.addAll(g.winners()); people.addAll(g.losers());
            printed |= g.winnerLevels().stream().anyMatch(Objects::nonNull) || g.loserLevels().stream().anyMatch(Objects::nonNull);
        }
        IMendixObject d = service.make("RankingDataset", "Name", name, "Sport", sport, "SourceNote", note, "GameCount", games.size(), "PlayerCount", people.size(), "FirstOn", RankingService.utc(games.get(0).date()), "LastOn", RankingService.utc(games.get(games.size() - 1).date()),
            "HasLevels", printed, "Disciplines", String.join(", ", disciplines), "GamesJson", datasetJson(games).toString());
        service.link(d, "RankingDataset_Scheme", s);
        return d;
    }

    private String importText(IMendixObject s, JSONObject c, IMendixObject w, String name, String text) throws Exception {
        RankingService.require(text != null && !text.isBlank(), "Paste the game lines first.");
        List<String> errors = new ArrayList<>(); List<RankingEngine.Game> games = RankingEngine.parseLines(text, errors);
        RankingService.require(!games.isEmpty(), "No game could be read." + (errors.isEmpty() ? "" : " " + errors.get(0)));
        int total = games.size();
        IMendixObject d = createDataset(s, name.isEmpty() ? "Imported games " + LocalDate.now(ZoneOffset.UTC) : name, service.text(s, "Sport"), "Imported by " + actor + " on " + LocalDate.now(ZoneOffset.UTC), games);
        c.put("dataset", service.id(d)); store(w, c, false);
        int kept = (int)service.num(d, "GameCount");
        return kept + " games imported" + (kept < total ? " (older than " + MAX_YEARS + " years: " + (total - kept) + " left out)" : "") + (errors.isEmpty() ? "." : ". " + errors.size() + " line(s) could not be read. First: " + errors.get(0));
    }

    private String fromMatches(IMendixObject s, JSONObject c, IMendixObject w) throws Exception {
        List<RankingEngine.Game> games = service.gamesOf(s);
        RankingService.require(!games.isEmpty(), "This federation has no played games with named players yet. Import games instead.");
        IMendixObject d = createDataset(s, "Match results of this federation, " + LocalDate.now(ZoneOffset.UTC), service.text(s, "Sport"), "Copied from the registered match results by " + actor, games);
        c.put("dataset", service.id(d)); store(w, c, false);
        return service.num(d, "GameCount") + " games copied from the match results.";
    }

    // ------------------------------------------------------------------ simulation

    private String run(IMendixObject s, IMendixObject w, JSONObject c) throws Exception {
        String key = c.optString("dataset", ""); RankingService.require(!key.isEmpty(), "Choose the game data first: select a row under Game data and press Use.");
        IMendixObject dataset = service.load("Ds:" + key);
        RankingEngine.Result r = RankingEngine.replay(readGames(new JSONObject(service.text(dataset, "GamesJson"))), readEngine(c));
        JSONObject json = resultJson(r, readEngine(c));
        w.setValue(ctx, "ResultJson", json.toString()); w.setValue(ctx, "Summary", json.getString("headline")); w.setValue(ctx, "RanOn", new Date()); w.setValue(ctx, "DatasetName", service.text(dataset, "Name"));
        service.commit(w);
        return "Simulation finished: " + json.getString("headline") + " Nothing was changed for players.";
    }

    static JSONObject resultJson(RankingEngine.Result r, RankingEngine.Config c) throws Exception {
        JSONObject j = new JSONObject();
        j.put("headline", r.players + " players, " + r.games + " games, " + r.reviews + " reviews: " + r.promoted + " promotions, " + r.demoted + " demotions.");
        JSONObject m = new JSONObject();
        m.put("games", (double)r.games).put("players", (double)r.players).put("reviews", (double)r.reviews).put("promoted", (double)r.promoted).put("demoted", (double)r.demoted).put("favourite", r.favouriteRate()).put("brier", r.brierScore());
        m.put("equal", r.games == 0 ? 0 : 100.0 * r.equalGames / r.games).put("churn", r.movesPerPlayerYear()).put("flip", r.flipRate()).put("exact", r.exactRate()).put("within", r.withinOneRate()).put("distance", r.meanDistance());
        j.put("metrics", m); j.put("period", r.first == null ? "" : r.first + " to " + r.last); j.put("skipped", r.skippedWalkovers);
        JSONArray warnings = new JSONArray(); for (String w : r.warnings) warnings.put(w); j.put("warnings", warnings);
        JSONArray levels = new JSONArray();
        for (int l = 1; l <= c.levels(); l++) levels.put(new JSONArray().put(c.name(l)).put(r.startCount[l]).put(r.endCount[l]).put(r.players == 0 ? 0 : Math.round(1000.0 * r.endCount[l] / r.players) / 10.0).put(c.shares == null ? 0 : c.shares[l]));
        j.put("levels", levels);
        JSONArray reviews = new JSONArray();
        for (RankingEngine.Review v : r.timeline) reviews.put(new JSONArray().put(v.date().toString()).put(v.active()).put(v.promoted()).put(v.demoted()).put(Math.round(v.favouriteRate() * 10) / 10.0));
        j.put("reviews", reviews);
        JSONArray gaps = new JSONArray();
        for (Map.Entry<Integer, int[]> g : r.gaps.entrySet()) gaps.put(new JSONArray().put(g.getKey() / 2.0).put(g.getValue()[1]).put(Math.round(1000.0 * g.getValue()[0] / g.getValue()[1]) / 10.0));
        j.put("gaps", gaps);
        JSONArray players = new JSONArray();
        for (RankingEngine.Player p : r.table) players.put(new JSONArray().put(p.name).put(p.discipline).put(p.startLevel).put(p.level).put(p.moves).put(p.games).put(Math.round(p.score * 10) / 10.0).put(p.lastPublished == null ? JSONObject.NULL : p.lastPublished).put(p.path.toString()));
        j.put("players", players);
        return j;
    }

    private void detail(IMendixObject form, IMendixObject s, JSONObject c, String ids) throws Exception {
        String key = ids;
        if (key.matches("[0-9, ]+")) { IMendixObject row = Core.retrieveId(ctx, Core.createMendixIdentifier(Long.parseLong(ids.split(",")[0].trim()))); key = row == null ? "" : service.text(row, "RecordKey"); }
        if (!key.startsWith("LP:")) key = service.requireKey(form, "LabPlayer", "LP:");
        String[] part = key.substring(3).split("\\|", 2); String datasetKey = c.optString("dataset", ""); RankingService.require(!datasetKey.isEmpty(), "Choose the game data first.");
        RankingEngine.Config e = readEngine(c);
        RankingEngine.Result r = RankingEngine.replay(readGames(new JSONObject(service.text(service.load("Ds:" + datasetKey), "GamesJson"))), e);
        for (IMendixObject old : Core.retrieveByPath(ctx, form, M + "RankingRow_Form")) if (service.text(old, "Kind").equals("LabDetail")) Core.delete(ctx, old);
        for (RankingEngine.Player p : r.table) {
            if (!p.discipline.equals(part[0]) || !RankingRules.nameKey(p.name).equals(part[1])) continue;
            String summary = p.name + " (" + p.discipline + "): " + p.path + ". " + p.reason; form.setValue(ctx, "Message", summary);
            List<RankingRules.Match> matches = new ArrayList<>(p.matches); matches.sort(Comparator.comparing(RankingRules.Match::date).reversed());
            for (RankingRules.Match m : matches) service.row(form, "LabDetail", "", m.date().toString(), m.won() ? "Won" : "Lost", m.label(), e.name((int)Math.round(m.opponentLevel())), e.name((int)Math.round(m.ownLevel())), m.won() ? RankingService.plain(e.table.points(m.opponentLevel())) : "0");
        }
    }

    // ------------------------------------------------------------------ development samples

    /** One saved scenario of a sample: a starting point replayed on the data set. Names the levels of the scheme when given. */
    IMendixObject sampleScenario(IMendixObject s, IMendixObject dataset, String name, String preset, String[] names, boolean baseline) throws Exception {
        RankingEngine.Config e = RankingEngine.preset(preset, names); JSONObject c = new JSONObject(); writeEngine(c, e); c.put("dataset", service.id(dataset));
        JSONObject result = resultJson(RankingEngine.replay(readGames(new JSONObject(service.text(dataset, "GamesJson"))), e), e);
        IMendixObject o = service.make("RankingScenario", "Name", name, "IsWorking", false, "IsBaseline", baseline, "CreatedBy", actor, "CreatedOn", new Date(), "RanOn", new Date(), "DatasetName", service.text(dataset, "Name"), "ConfigJson", c.toString(), "ResultJson", result.toString(), "Summary", result.getString("headline"));
        service.link(o, "RankingScenario_Scheme", s);
        return o;
    }

    /** The working design of a sample starts as a copy of a saved scenario, with its result. */
    void useAsWorking(IMendixObject s, IMendixObject scenario) throws Exception {
        IMendixObject w = working(s);
        w.setValue(ctx, "ConfigJson", service.text(scenario, "ConfigJson")); w.setValue(ctx, "ResultJson", service.text(scenario, "ResultJson")); w.setValue(ctx, "Summary", service.text(scenario, "Summary")); w.setValue(ctx, "DatasetName", service.text(scenario, "DatasetName")); w.setValue(ctx, "RanOn", new Date());
        service.commit(w);
    }

    // ------------------------------------------------------------------ apply

    /** Makes the design the scheme's real configuration. A scheme without players starts from the levels of the simulation. */
    private String apply(IMendixObject s, IMendixObject w, JSONObject c, String reason) throws Exception {
        RankingEngine.Config e = readEngine(c); RankingRules.Params p = e.params; int count = e.levels();
        s.setValue(ctx, "WindowWeeks", (long)p.windowWeeks()); s.setValue(ctx, "MaxMatches", (long)p.maxMatches()); s.setValue(ctx, "MinDivisor", (long)p.minDivisor()); s.setValue(ctx, "MaxSpread", (long)p.maxSpread());
        s.setValue(ctx, "InactiveWeeks", (long)p.inactiveWeeks()); s.setValue(ctx, "InactiveMinMatches", (long)p.inactiveMinMatches()); s.setValue(ctx, "InactiveDrop", (long)p.inactiveDrop()); s.setValue(ctx, "IgnoreHigherLosses", p.ignoreHigherLosses());
        s.setValue(ctx, "Frequency", e.frequency); s.setValue(ctx, "ConfigJson", c.toString()); s.setValue(ctx, "LevelCount", (long)count); s.setValue(ctx, "Version", service.num(s, "Version") + 1);
        if (!reason.isBlank()) s.setValue(ctx, "Note", reason.trim());
        service.commit(s);
        service.replaceLevels(s, e);
        int seeded = service.children("PlayerRanking", "PlayerRanking_Scheme", s).isEmpty() ? seedFromResult(s, w) : 0;
        int moved = service.clampPlayers(s, count);
        service.clearSimulation(s);
        return "The design is now the ranking's configuration." + (moved > 0 ? " " + moved + " player(s) moved up from a removed level." : "") + (seeded > 0 ? " " + seeded + " player ranking(s) were created from the simulation." : "") + " Existing levels of players stay until the next review.";
    }

    /** Player rankings from the end of the simulation; an existing discipline is reused, a code from the data gets its own discipline. */
    int seedFromResult(IMendixObject s, IMendixObject w) throws Exception {
        JSONArray players = new JSONObject(service.text(w, "ResultJson")).getJSONArray("players"); int seeded = 0; LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Map<String, IMendixObject> disciplines = new HashMap<>();
        for (IMendixObject d : service.children("RankingDiscipline", "RankingDiscipline_Scheme", s)) disciplines.put(service.text(d, "Code"), d);
        for (int i = 0; i < players.length(); i++) {
            JSONArray p = players.getJSONArray(i); String code = p.getString(1); IMendixObject d = disciplines.get(code);
            if (d == null) d = disciplines.get(RankingService.sampleCode(code));
            if (d == null) { d = service.make("RankingDiscipline", "Code", code, "Name", code, "Kind", code, "Gender", "", "BonusThreshold", 0L, "Sequence", (long)disciplines.size() + 1, "SlotKey", "Open", "Players", 1L, "Active", true); service.link(d, "RankingDiscipline_Scheme", s); disciplines.put(code, d); }
            IMendixObject pr = service.make("PlayerRanking", "PlayerKey", RankingRules.nameKey(p.getString(0)), "PlayerName", p.getString(0), "Level", p.getLong(3), "Average", BigDecimal.valueOf(p.getDouble(6)), "Matches", p.getLong(5), "BonusCounter", 0L, "LastActiveOn", RankingService.utc(today), "Version", 1L);
            service.link(pr, "PlayerRanking_Scheme", s); service.link(pr, "PlayerRanking_Discipline", d);
            service.recordChange(s, pr, null, "Seed", "Applied", 0, (int)p.getLong(3), p.getDouble(6), (int)p.getLong(5), today, "Level at the end of the simulation on " + service.text(w, "DatasetName") + ".", ""); seeded++;
        }
        return seeded;
    }

    // ------------------------------------------------------------------ rows

    void refresh(IMendixObject form, IMendixObject s) throws Exception {
        service.clear(form); form.setValue(ctx, "Selected", ""); form.setValue(ctx, "Message", "");
        IMendixObject w = working(s); JSONObject c = configOf(w);
        form.setValue(ctx, "Title", "Design: " + service.text(s, "Name"));
        form.setValue(ctx, "Info", "Change the design on the left and press Run simulation: the games on the right are replayed in date order. Players keep their levels until you apply the design.");
        form.setValue(ctx, "LabMethod", c.optString("method", RankingEngine.AVERAGE)); form.setValue(ctx, "LabPlacement", c.optString("placement", RankingEngine.THRESHOLDS)); form.setValue(ctx, "Frequency", c.optString("frequency", "FirstMondayMonthly"));
        String method = c.optString("method", RankingEngine.AVERAGE);
        for (String[] def : SETTINGS) if (def[4].isEmpty() || def[4].equals(method)) service.row(form, "Setting", "Set:" + def[0], def[1], def[2], settingValue(c, def[0]), def[3]);
        JSONArray levels = c.getJSONArray("levels");
        for (int i = 0; i < levels.length(); i++) {
            JSONObject l = levels.getJSONObject(i);
            service.row(form, "LabLevel", "Lv:" + i, String.valueOf(i + 1), l.optString("name"), RankingService.plain(l.optDouble("points", 0.0)), RankingService.plain(l.optDouble("promote", 0.0)), RankingService.plain(l.optDouble("demote", 0.0)), RankingService.plain(l.optDouble("share", 0.0)));
        }
        String used = c.optString("dataset", "");
        List<IMendixObject> sets = service.children("RankingDataset", "RankingDataset_Scheme", s);
        sets.sort(Comparator.comparing(d -> service.text(d, "Name")));
        form.setValue(ctx, "LabDataset", "");
        for (IMendixObject d : sets) {
            if (service.id(d).equals(used)) form.setValue(ctx, "LabDataset", service.text(d, "Name"));
            service.row(form, "Dataset", "Ds:" + service.id(d), service.id(d).equals(used) ? "In use" : "", service.text(d, "Name"), String.valueOf(service.num(d, "GameCount")), String.valueOf(service.num(d, "PlayerCount")), service.day(d, "FirstOn") + " to " + service.day(d, "LastOn"), service.text(d, "Disciplines"), service.bool(d, "HasLevels") ? "Yes" : "No", service.text(d, "SourceNote"));
        }
        IMendixObject base = baseline(s); JSONObject baseResult = base == null || service.text(base, "ResultJson").isEmpty() ? null : new JSONObject(service.text(base, "ResultJson"));
        form.setValue(ctx, "LabBaseline", base == null ? "" : service.text(base, "Name"));
        List<IMendixObject> saved = scenarios(s); saved.sort(Comparator.comparing(o -> service.text(o, "Name")));
        for (IMendixObject o : saved) {
            if (service.bool(o, "IsWorking")) continue;
            JSONObject cfg = configOf(o);
            service.row(form, "Scenario", "Sc:" + service.id(o), service.bool(o, "IsBaseline") ? "Baseline" : "", service.text(o, "Name"), cfg.optString("method"), String.valueOf(cfg.getJSONArray("levels").length()), service.text(o, "DatasetName"), service.text(o, "Summary"));
        }
        String result = service.text(w, "ResultJson");
        form.setValue(ctx, "Warnings", ""); form.setValue(ctx, "Schedule", result.isEmpty() ? "No simulation yet for this design." : service.text(w, "Summary") + " Data: " + service.text(w, "DatasetName") + ".");
        if (!result.isEmpty()) resultRows(form, new JSONObject(result), baseResult, service.text(form, "Value10").trim().toLowerCase(Locale.ROOT));
    }

    private static final String[][] METRICS = {
        {"favourite", "Better-levelled side won (%)", "Higher is better: the levels predict results. Equal levels are left out."},
        {"brier", "Prediction error (Brier)", "Lower is better. 0.25 is a coin flip."},
        {"equal", "Games between equal levels (%)", "High means the levels are too coarse to separate players."},
        {"churn", "Level changes per player per year", "Lower is calmer. Very low can also mean levels do not respond."},
        {"flip", "Moves undoing a recent move (%)", "High means players yo-yo between two levels."},
        {"exact", "Same as the level printed on the sheet (%)", "Only with levels on the sheets. Shows how close this comes to the current official levels."},
        {"within", "Within one level of the sheet (%)", "Only with levels on the sheets."},
        {"distance", "Average distance from the sheet (levels)", "Only with levels on the sheets. Lower is closer."},
        {"promoted", "Promotions", ""}, {"demoted", "Demotions", ""}, {"players", "Players", ""}, {"games", "Games replayed", ""}, {"reviews", "Reviews", ""}};

    private void resultRows(IMendixObject form, JSONObject r, JSONObject base, String search) throws Exception {
        StringBuilder warnings = new StringBuilder(); JSONArray list = r.getJSONArray("warnings");
        for (int i = 0; i < list.length(); i++) warnings.append(i > 0 ? "\n" : "").append("• ").append(list.getString(i));
        form.setValue(ctx, "Warnings", warnings.toString());
        JSONObject m = r.getJSONObject("metrics"), bm = base == null ? null : base.getJSONObject("metrics");
        for (String[] def : METRICS) {
            double v = m.optDouble(def[0], -1.0); if (v < 0 && def[0].matches("exact|within|distance")) continue;
            String shown = RankingRules.fmt(v), other = bm == null ? "" : bm.optDouble(def[0], -1.0) < 0 ? "" : RankingRules.fmt(bm.getDouble(def[0]));
            if (def[0].equals("brier")) { shown = String.format(Locale.ROOT, "%.3f", v); other = bm == null ? "" : String.format(Locale.ROOT, "%.3f", bm.optDouble(def[0], 0.0)); }
            String difference = other.isEmpty() ? "" : String.format(Locale.ROOT, "%+.3f", v - bm.getDouble(def[0]));
            service.row(form, "LabSummary", "", def[1], shown, other, difference, def[2]);
        }
        JSONArray levels = r.getJSONArray("levels");
        for (int i = 0; i < levels.length(); i++) { JSONArray l = levels.getJSONArray(i); int bar = (int)Math.round(l.getDouble(3) / 2.5);
            service.row(form, "LabDistribution", "", l.getString(0), String.valueOf(l.getInt(1)), String.valueOf(l.getInt(2)), RankingService.plain(l.getDouble(3)) + " %", l.getDouble(4) > 0 ? RankingService.plain(l.getDouble(4)) + " %" : "", "█".repeat(Math.max(0, Math.min(40, bar)))); }
        JSONArray reviews = r.getJSONArray("reviews");
        for (int i = reviews.length() - 1; i >= 0; i--) { JSONArray v = reviews.getJSONArray(i); service.row(form, "LabReview", "", v.getString(0), String.valueOf(v.getInt(1)), String.valueOf(v.getInt(2)), String.valueOf(v.getInt(3)), RankingService.plain(v.getDouble(4)) + " %"); }
        JSONArray gaps = r.getJSONArray("gaps");
        for (int i = 0; i < gaps.length(); i++) { JSONArray g = gaps.getJSONArray(i); service.row(form, "LabGap", "", RankingService.plain(g.getDouble(0)), String.valueOf(g.getInt(1)), RankingService.plain(g.getDouble(2)) + " %"); }
        // players: first those who differ from the baseline, then the ones who moved most
        Map<String, Integer> baseLevel = new HashMap<>();
        if (base != null) { JSONArray bp = base.getJSONArray("players"); for (int i = 0; i < bp.length(); i++) { JSONArray p = bp.getJSONArray(i); baseLevel.put(p.getString(1) + "|" + RankingRules.nameKey(p.getString(0)), p.getInt(3)); } }
        JSONArray players = r.getJSONArray("players"), names = r.getJSONArray("levels"); List<JSONArray> rows = new ArrayList<>();
        for (int i = 0; i < players.length(); i++) { JSONArray p = players.getJSONArray(i); if (search.isEmpty() || p.getString(0).toLowerCase(Locale.ROOT).contains(search)) rows.add(p); }
        rows.sort(Comparator.comparingInt((JSONArray p) -> -Math.abs(baseLevel.getOrDefault(key(p), level(p)) - level(p))).thenComparingInt(p -> -p.optInt(4)).thenComparing(p -> p.optString(0)));
        int shown = 0;
        for (JSONArray p : rows) {
            if (shown++ >= MAX_PLAYER_ROWS) break;
            Integer b = baseLevel.get(key(p));
            service.row(form, "LabPlayer", "LP:" + p.getString(1) + "|" + RankingRules.nameKey(p.getString(0)), p.getString(0), p.getString(1), name(names, p.getInt(2)), name(names, p.getInt(3)), b == null ? "" : name(names, b), String.valueOf(p.getInt(4)), String.valueOf(p.getInt(5)), RankingService.plain(p.getDouble(6)), p.isNull(7) ? "" : String.valueOf(p.getInt(7)), p.getString(8));
        }
        form.setValue(ctx, "Comment", rows.size() > MAX_PLAYER_ROWS ? "Showing " + MAX_PLAYER_ROWS + " of " + rows.size() + " players: first those who differ from the baseline, then those who moved most. Search by name to find others." : rows.size() + " players.");
    }

    private static String key(JSONArray p) { return p.optString(1) + "|" + RankingRules.nameKey(p.optString(0)); }
    private static int level(JSONArray p) { return p.optInt(3); }
    private static String name(JSONArray levels, int level) throws Exception { return level >= 1 && level <= levels.length() ? levels.getJSONArray(level - 1).getString(0) : String.valueOf(level); }
}
