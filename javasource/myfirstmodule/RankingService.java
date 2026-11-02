package myfirstmodule;

import com.mendix.core.Core;
import com.mendix.systemwideinterfaces.core.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Individual rankings: schemes (one per federation and sport, only where a federation defines one), levels and points that are edited in place as
 * draft "test values" (simulated, then approved or discarded), configurable disciplines, a review schedule with a log, previews of the upcoming
 * review with promoted/demoted/staying players, per-player rankings with a change history, forced changes and the calculation behind each average.
 * All persistent ranking types have no client access; pages only see the non-persistent RankingForm/RankingRow/RankingState.
 * Every command rechecks permission here: platform administrators and federation administrators with Manage permission.
 */
public final class RankingService {
    private static final String M = "MyFirstModule.";
    static final String FEATURE = "features:individual-rankings-v4";
    static final String FEATURE_V3 = "features:individual-rankings-v3";
    static final String FEATURE_V2 = "features:individual-rankings-v2";
    static final String FEATURE_V1 = "features:individual-rankings-v1";
    static final String CURRENT = "Current values", TEST = "Test values";
    static final String[] TYPES = {"MyFirstModule.RankingScheme", "MyFirstModule.RankingDiscipline", "MyFirstModule.RankingLevel", "MyFirstModule.RankingMoment", "MyFirstModule.PlayerRanking", "MyFirstModule.RankingChange", "MyFirstModule.RankingDataset", "MyFirstModule.RankingScenario"};
    /** code, name, kind, gender, bonus threshold, match slot, players per side (published thresholds: men singles 12, doubles 24, mixed 24; women singles 8, doubles 16) */
    static final String[][] DISCIPLINES = {{"MS", "Men's singles", "Singles", "Men", "12", "Men", "1"}, {"WS", "Women's singles", "Singles", "Women", "8", "Women", "1"},
        {"MD", "Men's doubles", "Doubles", "Men", "24", "Men", "2"}, {"WD", "Women's doubles", "Doubles", "Women", "16", "Women", "2"},
        {"XD", "Mixed doubles", "Mixed", "Mixed (gender is not on the match sheets)", "24", "Mixed", "2"}};
    private static final String SAMPLE_NOTE = "Points of a win by opponent level are published by Badminton Vlaanderen. The promotion and demotion thresholds, "
        + "the way doubles points are derived from the two opponents, what counts towards the bonus counter and the reading of 'losses against a directly higher level' "
        + "are NOT published here: they are editable sample defaults.";
    private static java.util.concurrent.ScheduledExecutorService scheduler;

    private final IContext ctx, user;
    private final String actor;
    private final boolean admin;
    private final List<IMendixObject> created;

    private RankingService(IContext ctx, IContext user, String actor, List<IMendixObject> created) {
        this.ctx = ctx; this.user = user; this.actor = actor; this.created = created;
        this.admin = user == null || (user.getSession() != null && user.getSession().getUserRolesNames().contains("Administrator"));
    }

    /** Service for development samples: system context, no user, every object created is reported to {@code created}. */
    static RankingService forSamples(IContext ctx, List<IMendixObject> created) { return new RankingService(ctx, null, "Development sample", created); }

    // ------------------------------------------------------------------ scheduled reviews

    /** Starts the once-a-minute check for due automatic reviews. Called from the private startup action. */
    static synchronized void startScheduler() {
        if (scheduler != null) return;
        scheduler = java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> { Thread t = new Thread(r, "ranking-reviews"); t.setDaemon(true); return t; });
        scheduler.scheduleWithFixedDelay(() -> {
            try { runDue(); } catch (Exception e) { Core.getLogger("Ranking").error("Scheduled ranking review failed: " + e, e); }
        }, 90, 60, TimeUnit.SECONDS);
    }

    /** Applies every automatic review that is due (at most three catch-up reviews per scheme per check), always with the current values. */
    static void runDue() throws Exception {
        IContext ctx = Core.createSystemContext(); ctx.startTransaction();
        try {
            synchronized (RankingService.class) {
                RankingService service = new RankingService(ctx, null, "Scheduled review", null);
                LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
                for (IMendixObject scheme : service.query("//MyFirstModule.RankingScheme[Active=true][AutoRun=true][StartAt!=empty]")) {
                    for (int guard = 0; guard < 3; guard++) {
                        LocalDateTime next = service.nextReviewOf(scheme);
                        if (next.isAfter(now)) break;
                        service.review(scheme, next, true, "Scheduled", false);
                    }
                }
            }
            ctx.endTransaction();
        } catch (Exception e) { ctx.rollbackTransaction(); throw e; }
    }

    // ------------------------------------------------------------------ entry point

    public static void dispatch(IContext context, String command) throws Exception {
        if (context.getSession() == null || context.getSession().isSystemSession()) throw new SecurityException("Sign in to manage rankings.");
        String[] p = command.split("\\|", -1);
        if (p.length != 3 || !p[1].matches("[0-9]+")) throw new IllegalArgumentException("Invalid ranking command.");
        IContext user = context.getSession().createContext();
        IContext ctx = context.createSudoClone();
        IMendixObject form = Core.retrieveId(ctx, Core.createMendixIdentifier(Long.parseLong(p[1])));
        if (form == null || !form.getType().equals(M + "RankingForm")) throw new IllegalArgumentException("Reopen the rankings page.");
        synchronized (RankingService.class) {
            new RankingService(ctx, user, user.getSession().getUserName(), null).run(p[0], form, p[2]);
        }
    }

    private void run(String op, IMendixObject form, String ids) throws Exception {
        IMendixObject state = state(form);
        form.setValue(ctx, "Message", "");
        if (op.startsWith("Inspect.")) { inspect(form, state, op.substring(8), ids); return; }
        if (op.startsWith("Lab.")) { new RankingWorkbench(this, ctx, actor).command(op.substring(4), form, state, ids); return; }
        String scheme = text(form, "SchemeKey");
        switch (op) {
            case "Open": clear(form); form.setValue(ctx, "Title", "Individual rankings"); form.setValue(ctx, "SchemeKey", ""); refresh(form, state); return;
            case "Refresh": refresh(form, state); return;
            case "Cell.Save": if (text(form, "Mode").startsWith("Lab")) new RankingWorkbench(this, ctx, actor).command("Cell", form, state, ids); else saveCell(form, state, ids); return;
            case "Scheme.Create": {
                IMendixObject fed = federationOf(selected(form, "Scheme"));
                require(fed != null && manage(fed), "Select a federation without a ranking scheme.");
                require(schemes(fed).isEmpty(), "This federation already has a ranking scheme.");
                openEdit(form, "SchemeCreate", "Create ranking scheme", new String[][]{{"Name", text(fed, "Name") + " individual ranking"}, {"Level names, highest first and separated by commas (empty: the names of the starting point)", ""}});
                form.setValue(ctx, "LabPreset", null);
                form.setValue(ctx, "FederationKey", id(fed)); return; }
            case "Scheme.View": openScheme(selected(form, "Scheme")); return;
            case "Row.Open": {
                IMendixObject row = Core.retrieveId(ctx, Core.createMendixIdentifier(Long.parseLong(ids.split(",")[0].trim())));
                require(row != null && row.getType().equals(M + "RankingRow"), "The row is no longer available. Refresh the page.");
                String key = text(row, "RecordKey");
                if (key.startsWith("S:")) openScheme(key);
                else if (key.startsWith("U:") || key.startsWith("M:")) { requireScheme(scheme); openReview(form, key); }
                else if (key.startsWith("P:")) { requireScheme(scheme); openPlayer(form, load(key)); }
                else if (key.startsWith("LP:")) new RankingWorkbench(this, ctx, actor).command("Player", form, state(form), key);
                return; }
            case "Scheme.Edit": new RankingWorkbench(this, ctx, actor).open(scheme(form, "Scheme")); return;
            case "Scheme.Design": new RankingWorkbench(this, ctx, actor).open(requireScheme(scheme)); return;
            case "Scheme.Delete": {
                IMendixObject s = scheme(form, "Scheme");
                if (!query("//MyFirstModule.RankingChange[MyFirstModule.RankingChange_Scheme='" + s.getId().toLong() + "'][State='Applied'][Kind!='Seed']").isEmpty())
                    throw new IllegalStateException("This scheme has applied ranking changes. Its history is kept; deactivate it instead of deleting it.");
                deleteScheme(s); refresh(form, state);
                form.setValue(ctx, "Message", "The ranking scheme and its seed rankings were deleted."); return; }
            case "Level.Create": { IMendixObject s = requireScheme(scheme); appendLevel(s); refresh(form, state); form.setValue(ctx, "Message", "A lowest level was added to the test values. Fill in its values, simulate, then approve."); return; }
            case "Level.Delete": { IMendixObject s = requireScheme(scheme); removeLowestLevel(s); refresh(form, state); form.setValue(ctx, "Message", "The lowest level was removed from the test values. Players on it move up when you approve."); return; }
            case "Level.Approve": { IMendixObject s = requireScheme(scheme); int moved = approveDraft(s); refresh(form, state); form.setValue(ctx, "Message", "Test values approved: they are now the real values." + (moved > 0 ? " " + moved + " player(s) moved up from a removed level." : "")); return; }
            case "Level.Discard": { IMendixObject s = requireScheme(scheme); discardDraft(s); refresh(form, state); form.setValue(ctx, "Message", "Test values discarded. The current values stay in use."); return; }
            case "Schedule.Edit": {
                IMendixObject s = requireScheme(scheme);
                LocalDateTime start = s.getValue(ctx, "StartAt") == null ? RankingRules.nextFirstMondays(LocalDate.now(ZoneOffset.UTC), 1).get(0).atTime(9, 0) : at(s, "StartAt");
                openEdit(form, "Schedule", "Review schedule", new String[][]{{"First review: date (yyyy-mm-dd)", start.toLocalDate().toString()}, {"First review: hour (hh:mm, UTC)", String.format("%02d:%02d", start.getHour(), start.getMinute())},
                    {"Run reviews automatically (yes/no)", bool(s, "AutoRun") ? "yes" : "no"}});
                String frequency = text(s, "Frequency"); form.setValue(ctx, "Frequency", frequency.isEmpty() ? "FirstMondayMonthly" : frequency); return; }
            case "Review.Simulate": case "Review.SimulateTest": {
                boolean test = op.equals("Review.SimulateTest"); IMendixObject s = requireScheme(scheme); LocalDateTime next = nextReviewOf(s);
                require(!test || hasDraft(s), "There are no test values to simulate. Change a value on the Levels tab first.");
                IMendixObject mo = review(s, next, false, "Simulation", test); refresh(form, state);
                form.setValue(ctx, "Message", "Simulation of the review of " + fmt(next) + " with " + (test ? "test values" : "current values") + ": " + num(mo, "Promoted") + " promoted, " + num(mo, "Demoted") + " demoted. Nothing was applied. Select it and press View to see the players."); return; }
            case "Review.Run": {
                IMendixObject s = requireScheme(scheme); IMendixObject mo = review(s, LocalDateTime.now(ZoneOffset.UTC), true, "Manual", false); refresh(form, state);
                form.setValue(ctx, "Message", "Review applied with the current values: " + num(mo, "Promoted") + " promoted, " + num(mo, "Demoted") + " demoted."); return; }
            case "Review.View": requireScheme(scheme); openReview(form, requireKeyOf(form, "Review", "U:", "M:")); return;
            case "Outcome.Scoring": { requireScheme(scheme); openPlayer(form, load(requireKey(form, "Outcome", "P:"))); return; }
            case "Review.More": { requireScheme(scheme); form.setValue(ctx, "LogLimit", Math.max(5, num(form, "LogLimit")) + 10); refresh(form, state); return; }
            case "Review.Filter": {
                requireScheme(scheme);
                openEdit(form, "LogFilter", "Filter the review log", new String[][]{{"From date (yyyy-mm-dd, empty for no limit)", text(form, "LogFrom")}, {"Until date (yyyy-mm-dd, empty for no limit)", text(form, "LogTo")},
                    {"Number of latest reviews to show without a date range", String.valueOf(Math.max(5, num(form, "LogLimit")))}}); return; }
            case "Discipline.Create": requireScheme(scheme);
                openEdit(form, "DisciplineCreate", "Create discipline", new String[][]{{"Code (up to 6 letters)", ""}, {"Name", ""}, {"Match slot on the sheets (Men, Women, Mixed or Open)", "Men"},
                    {"Players per side (1 or 2)", "1"}, {"Bonus counter threshold (0 for none)", "0"}}); return;
            case "Discipline.Edit": {
                IMendixObject d = load(requireKey(form, "Discipline", "D:")); requireScheme(scheme);
                openEdit(form, "DisciplineEdit", "Edit discipline " + text(d, "Code"), new String[][]{{"Code (up to 6 letters)", text(d, "Code")}, {"Name", text(d, "Name")}, {"Match slot on the sheets (Men, Women, Mixed or Open)", text(d, "SlotKey")},
                    {"Players per side (1 or 2)", String.valueOf(num(d, "Players"))}, {"Bonus counter threshold (0 for none)", String.valueOf(num(d, "BonusThreshold"))}}, "D:" + id(d)); return; }
            case "Discipline.Deactivate": case "Discipline.Activate": {
                requireScheme(scheme); boolean on = op.equals("Discipline.Activate"); int n = 0; IMendixObject s = load("S:" + scheme);
                for (String key : selectedAll(form, "Discipline")) {
                    IMendixObject d = load(key); if (on) requireFreeSlot(s, text(d, "SlotKey"), num(d, "Players"), d);
                    d.setValue(ctx, "Active", on); commit(d); n++;
                }
                refresh(form, state); form.setValue(ctx, "Message", n + " discipline(s) " + (on ? "activated" : "deactivated") + (on ? "." : ". Rankings and history are kept; matches of a deactivated discipline are no longer counted.")); return; }
            case "Player.Seed": { IMendixObject s = requireScheme(scheme); int n = seedPlayers(s); refresh(form, state); form.setValue(ctx, "Message", n + " player ranking(s) added from the match results."); return; }
            case "Player.View": { IMendixObject pr = load(requireKey(form, "Player", "P:")); requireScheme(scheme); openPlayer(form, pr); return; }
            case "Player.Force": {
                IMendixObject pr = load(requireKey(form, "Player", "P:")); requireScheme(scheme);
                openEdit(form, "Force", "Force a level change: " + text(pr, "PlayerName") + " (" + ref(pr, "PlayerRanking_Discipline").getValue(ctx, "Name") + ", now " + levelNames(requireScheme(scheme)).getOrDefault((int)num(pr, "Level"), String.valueOf(num(pr, "Level"))) + ")",
                    new String[][]{{"New level", levelNames(requireScheme(scheme)).getOrDefault((int)num(pr, "Level"), String.valueOf(num(pr, "Level")))}}, "P:" + id(pr));
                form.setValue(ctx, "Comment", ""); return; }
            case "Change.Clear": { IMendixObject s = requireScheme(scheme); int n = clearSimulation(s); refresh(form, state); form.setValue(ctx, "Message", n + " simulated change(s) cleared."); return; }
            case "Save": save(form, state); return;
            default: throw new IllegalArgumentException("Unknown ranking command.");
        }
    }

    /** Opens a scheme on its own page: the list page stays as it is. */
    private void openScheme(String key) throws Exception {
        require(key != null && key.startsWith("S:"), "Select a ranking scheme to open.");
        IMendixObject s = load(key); require(manage(ref(s, "RankingScheme_Federation")), "You cannot manage this federation's rankings.");
        IMendixObject next = Core.instantiate(ctx, M + "RankingForm");
        next.setValue(ctx, "SchemeKey", id(s)); next.setValue(ctx, "Title", text(s, "Name")); next.setValue(ctx, "LogLimit", 5L);
        refresh(next, state(next));
        Core.microflowCall(M + "ACT_Ranking_ShowScheme").withParam("Form", next).execute(ctx);
    }

    /** The player page. When it is opened from a review it calculates as of that review, with its values. */
    private void openPlayer(IMendixObject form, IMendixObject pr) throws Exception {
        IMendixObject next = Core.instantiate(ctx, M + "RankingForm");
        next.setValue(ctx, "SchemeKey", text(form, "SchemeKey")); next.setValue(ctx, "PlayerKey", text(pr, "PlayerKey")); next.setValue(ctx, "Title", text(pr, "PlayerName"));
        next.setValue(ctx, "ReviewKey", text(form, "ReviewKey"));
        history(next); Core.microflowCall(M + "ACT_Ranking_ShowHistory").withParam("Form", next).execute(ctx);
    }

    // ------------------------------------------------------------------ dialogs

    void openEdit(IMendixObject form, String mode, String title, String[][] fields) throws Exception { openEdit(form, mode, title, fields, ""); }

    /** The target record key is kept on the form: the grid reselects after a refresh, so the selection cannot be trusted when Save runs. */
    private void openEdit(IMendixObject form, String mode, String title, String[][] fields, String target) throws Exception {
        form.setValue(ctx, "Mode", mode); form.setValue(ctx, "Title", title); form.setValue(ctx, "PlayerKey", target);
        for (int i = 1; i <= 10; i++) {
            form.setValue(ctx, "Label" + i, i <= fields.length ? fields[i - 1][0] : "");
            form.setValue(ctx, "Value" + i, i <= fields.length ? fields[i - 1][1] : "");
        }
        Core.microflowCall(M + "ACT_Ranking_ShowEdit").withParam("Form", form).execute(ctx);
    }

    private void save(IMendixObject form, IMendixObject state) throws Exception {
        String mode = text(form, "Mode");
        String[] v = new String[11];
        for (int i = 1; i <= 10; i++) v[i] = text(form, "Value" + i).trim();
        String schemeKey = text(form, "SchemeKey");
        switch (mode) {
            case "SchemeCreate": {
                IMendixObject fed = Core.retrieveId(ctx, Core.createMendixIdentifier(Long.parseLong(text(form, "FederationKey"))));
                require(manage(fed), "You cannot manage this federation's rankings.");
                Object preset = form.getValue(ctx, "LabPreset"); require(preset != null, "Choose a starting point.");
                String[] names = RankingEngine.parseNames(v[2]); require(names.length == 0 || names.length >= 2 && names.length <= 30, "A ranking needs between 2 and 30 levels.");
                IMendixObject s = createScheme(fed, v[1], preset.toString(), names);
                new RankingWorkbench(this, ctx, actor).open(s); form.setValue(ctx, "Message", "Ranking scheme created."); break; }
            case "LabImport": case "LabSave": case "LabApply": new RankingWorkbench(this, ctx, actor).save(form, mode, v); flags(form, state); return;
            case "Schedule": {
                IMendixObject s = requireScheme(schemeKey);
                LocalDate date = date(v[1]); LocalTime time = time(v[2]); String yn = v[3].toLowerCase(Locale.ROOT);
                require(yn.equals("yes") || yn.equals("no"), "Answer yes or no for running the reviews automatically.");
                Object chosen = form.getValue(ctx, "Frequency"); String frequency = chosen == null ? "" : chosen.toString();
                require(!RankingRules.frequencyLabel(frequency).equals("not set"), "Choose how often the ranking is reviewed.");
                s.setValue(ctx, "StartAt", utc(date.atTime(time))); s.setValue(ctx, "Frequency", frequency); s.setValue(ctx, "AutoRun", yn.equals("yes"));
                s.setValue(ctx, "Version", num(s, "Version") + 1); commit(s); clearSimulation(s);
                form.setValue(ctx, "Message", "Schedule saved. Next review: " + fmt(nextReviewOf(s)) + ". Earlier simulations were cleared."); break; }
            case "LogFilter": {
                requireScheme(schemeKey);
                if (!v[1].isEmpty()) date(v[1]); if (!v[2].isEmpty()) date(v[2]);
                form.setValue(ctx, "LogFrom", v[1]); form.setValue(ctx, "LogTo", v[2]);
                form.setValue(ctx, "LogLimit", v[3].isEmpty() ? 5L : Math.max(1L, (long)number(v[3], "number of reviews"))); break; }
            case "DisciplineCreate": case "DisciplineEdit": {
                IMendixObject s = requireScheme(schemeKey); IMendixObject d = mode.equals("DisciplineEdit") ? load(text(form, "PlayerKey")) : null;
                String code = v[1].toUpperCase(Locale.ROOT), slot = v[3];
                require(code.matches("[A-Z0-9]{1,6}"), "The code is 1 to 6 letters or digits.");
                require(!v[2].isEmpty(), "Enter a name.");
                require(Set.of("Men", "Women", "Mixed", "Open").contains(slot), "The match slot is Men, Women, Mixed or Open, as on the match sheets.");
                long players = (long)number(v[4], "players per side"); require(players == 1 || players == 2, "Players per side is 1 or 2.");
                long bonus = (long)number(v[5], "bonus threshold"); require(bonus >= 0, "The bonus threshold cannot be negative.");
                for (IMendixObject other : children("RankingDiscipline", "RankingDiscipline_Scheme", s)) require(d != null && other.getId().equals(d.getId()) || !text(other, "Code").equals(code), "There is already a discipline with code " + code + ".");
                if (d == null || bool(d, "Active")) requireFreeSlot(s, slot, players, d);
                if (d == null) { d = make("RankingDiscipline", "Sequence", (long)children("RankingDiscipline", "RankingDiscipline_Scheme", s).size() + 1, "Active", true); link(d, "RankingDiscipline_Scheme", s); }
                d.setValue(ctx, "Code", code); d.setValue(ctx, "Name", v[2]); d.setValue(ctx, "SlotKey", slot); d.setValue(ctx, "Players", players); d.setValue(ctx, "BonusThreshold", bonus);
                d.setValue(ctx, "Kind", players == 1 ? "Singles" : slot.equals("Mixed") ? "Mixed" : "Doubles"); d.setValue(ctx, "Gender", slot); commit(d);
                form.setValue(ctx, "Message", "Discipline " + code + " saved. Use Initialise players to add the players of this discipline."); break; }
            case "Force": {
                IMendixObject pr = load(text(form, "PlayerKey")); IMendixObject s = requireScheme(schemeKey);
                String comment = text(form, "Comment").trim(); int level = levelFromText(s, v[1]);
                forceChange(s, pr, level, comment); form.setValue(ctx, "Message", "Level changed and written to the history with your comment."); break; }
            default: throw new IllegalArgumentException("Nothing to save.");
        }
        refresh(form, state);
    }

    private void requireFreeSlot(IMendixObject scheme, String slot, long players, IMendixObject except) throws Exception {
        for (IMendixObject other : children("RankingDiscipline", "RankingDiscipline_Scheme", scheme))
            if (bool(other, "Active") && (except == null || !other.getId().equals(except.getId())) && text(other, "SlotKey").equals(slot) && num(other, "Players") == players)
                throw new IllegalArgumentException("Discipline " + text(other, "Code") + " already ranks " + slot + " matches with " + players + " player(s) per side.");
    }

    private void applySchemeValues(IMendixObject s, String[] v, boolean create) throws Exception {
        if (!v[1].isEmpty()) s.setValue(ctx, "Name", v[1]);
        long levels = create ? (long)number(v[2], "levels") : num(s, "LevelCount");
        require(levels >= 2 && levels <= 30, "A scheme needs between 2 and 30 levels.");
        long window = (long)number(v[3], "window"), max = (long)number(v[4], "matches counted"), divisor = (long)number(v[5], "divisor"), spread = (long)number(v[6], "level difference"),
            inactiveWeeks = (long)number(v[7], "inactivity period"), inactiveMin = (long)number(v[8], "matches"), drop = (long)number(v[9], "levels lost");
        require(window >= 1 && max >= 1 && divisor >= 1 && spread >= 0 && inactiveWeeks >= window && inactiveMin >= 0 && drop >= 0, "Check the numbers: the inactivity period cannot be shorter than the window.");
        s.setValue(ctx, "WindowWeeks", window); s.setValue(ctx, "MaxMatches", max); s.setValue(ctx, "MinDivisor", divisor); s.setValue(ctx, "MaxSpread", spread);
        s.setValue(ctx, "InactiveWeeks", inactiveWeeks); s.setValue(ctx, "InactiveMinMatches", inactiveMin); s.setValue(ctx, "InactiveDrop", drop);
        String yn = v[10].toLowerCase(Locale.ROOT); require(yn.equals("yes") || yn.equals("no"), "Answer yes or no for the higher-level losses rule.");
        s.setValue(ctx, "IgnoreHigherLosses", yn.equals("yes")); s.setValue(ctx, "Version", num(s, "Version") + 1);
        commit(s);
        if (create && levels != num(s, "LevelCount")) rebuildLevels(s, (int)levels);
    }

    // ------------------------------------------------------------------ schemes, disciplines

    /** Creates the scheme from a starting point with default disciplines and a schedule that is not automatic; fails when the federation already has one. */
    IMendixObject createScheme(IMendixObject federation, String name) throws Exception { return createScheme(federation, name, "BadmintonAverage", null); }

    IMendixObject createScheme(IMendixObject federation, String name, String preset, String[] names) throws Exception {
        require(manage(federation), "You cannot manage this federation's rankings.");
        require(schemes(federation).isEmpty(), "This federation already has a ranking scheme.");
        RankingEngine.Config e = RankingEngine.preset(preset, names); RankingRules.Params p = e.params;
        com.mendix.thirdparty.org.json.JSONObject config = new com.mendix.thirdparty.org.json.JSONObject(); RankingWorkbench.writeEngine(config, e);
        IMendixObject s = make("RankingScheme", "Name", name == null || name.isBlank() ? text(federation, "Name") + " individual ranking" : name, "Sport", text(federation, "Sport"),
            "LevelCount", (long)p.levels(), "WindowWeeks", (long)p.windowWeeks(), "MaxMatches", (long)p.maxMatches(), "MinDivisor", (long)p.minDivisor(), "MaxSpread", (long)p.maxSpread(),
            "InactiveWeeks", (long)p.inactiveWeeks(), "InactiveMinMatches", (long)p.inactiveMinMatches(), "InactiveDrop", (long)p.inactiveDrop(), "IgnoreHigherLosses", p.ignoreHigherLosses(),
            "Active", true, "Note", preset.equals("BadmintonAverage") ? SAMPLE_NOTE : "Starting point: " + RankingEngine.presetLabel(preset) + ". Inspired by that system; every number is an editable sample value.", "Version", 1L,
            "StartAt", utc(RankingRules.nextFirstMondays(LocalDate.now(ZoneOffset.UTC), 1).get(0).atTime(9, 0)), "Frequency", e.frequency, "AutoRun", false, "ConfigJson", config.toString());
        link(s, "RankingScheme_Federation", federation);
        int seq = 0;
        for (String[] d : DISCIPLINES) link(make("RankingDiscipline", "Code", d[0], "Name", d[1], "Kind", d[2], "Gender", d[3], "BonusThreshold", preset.equals("BadmintonAverage") ? Long.parseLong(d[4]) : 0L, "Sequence", (long)++seq,
            "SlotKey", d[5], "Players", Long.parseLong(d[6]), "Active", true), "RankingDiscipline_Scheme", s);
        replaceLevels(s, e);
        return s;
    }

    /** Replaces the current level rows with the levels of a configuration. */
    void replaceLevels(IMendixObject s, RankingEngine.Config e) throws Exception {
        for (IMendixObject old : levels(s, false)) Core.delete(ctx, old);
        for (IMendixObject old : levels(s, true)) Core.delete(ctx, old);
        for (int level = 1; level <= e.levels(); level++)
            link(make("RankingLevel", "Level", (long)level, "LevelName", e.name(level), "WinPoints", bd(e.table.winPoints()[level]), "PromoteAt", bd(e.table.promoteAt()[level]), "DemoteBelow", bd(e.table.demoteBelow()[level]),
                "SharePercent", bd(e.shares == null ? 0 : e.shares[level]), "PointsPublished", false, "IsDraft", false), "RankingLevel_Scheme", s);
        s.setValue(ctx, "LevelCount", (long)e.levels()); commit(s);
    }

    /** Players on a level that no longer exists move to the new lowest level, with a history entry. */
    int clampPlayers(IMendixObject s, int count) throws Exception {
        int moved = 0;
        for (IMendixObject pr : children("PlayerRanking", "PlayerRanking_Scheme", s)) {
            if (num(pr, "Level") <= count) continue;
            recordChange(s, pr, null, "Levels", "Applied", (int)num(pr, "Level"), count, dec(pr, "Average"), (int)num(pr, "Matches"), LocalDate.now(ZoneOffset.UTC), "Level " + num(pr, "Level") + " was removed from the scheme.", "");
            pr.setValue(ctx, "Level", (long)count); commit(pr); moved++;
        }
        return moved;
    }

    /** The display name of a level; older rows without a name show their position. */
    String levelName(IMendixObject level) { String name = text(level, "LevelName"); return name.isBlank() ? String.valueOf(num(level, "Level")) : name; }

    Map<Integer, String> levelNames(IMendixObject s) throws Exception {
        Map<Integer, String> names = new HashMap<>();
        for (IMendixObject l : levels(s, false)) names.put((int)num(l, "Level"), levelName(l));
        return names;
    }

    private String nameOf(Map<Integer, String> names, long level) { return names.getOrDefault((int)level, String.valueOf(level)); }

    /** A level typed by a user: its name, or its position when no level has that name. */
    private int levelFromText(IMendixObject s, String raw) throws Exception {
        for (Map.Entry<Integer, String> e : levelNames(s).entrySet()) if (e.getValue().equalsIgnoreCase(raw.trim())) return e.getKey();
        try { return Integer.parseInt(raw.trim()); } catch (NumberFormatException e) { throw new IllegalArgumentException("There is no level called " + raw + "."); }
    }

    /** The federation's registered games in the form of imported data: the winners' views of one game are joined by date, discipline and opponents. */
    List<RankingEngine.Game> gamesOf(IMendixObject s) throws Exception {
        Map<String, RankingEngine.Game> games = new LinkedHashMap<>();
        for (History h : gather(s).values()) for (Entry e : h.entries) {
            if (!e.won) continue;
            List<String> opponents = new ArrayList<>(e.opponents); Collections.sort(opponents);
            String key = e.date + "|" + h.discipline + "|" + String.join("&", opponents); int repeat = 0;
            while (games.containsKey(key + "#" + repeat) && games.get(key + "#" + repeat).winners().size() >= e.opponents.size()) repeat++;
            RankingEngine.Game known = games.get(key + "#" + repeat);
            List<String> winners = new ArrayList<>(known == null ? List.of() : known.winners()); winners.add(h.name);
            List<Integer> winLevels = new ArrayList<>(known == null ? new ArrayList<Integer>() : known.winnerLevels()); winLevels.add(e.own);
            games.put(key + "#" + repeat, new RankingEngine.Game(e.date, h.discipline, winners, new ArrayList<>(e.opponentNames), winLevels, new ArrayList<>(e.opponentBrackets), "", ""));
        }
        return new ArrayList<>(games.values());
    }

    /** A discipline code of the sample data (HE, DE, HD, DD, GD) as the code of the default disciplines. */
    static String sampleCode(String code) { switch (code) { case "HE": return "MS"; case "DE": return "WS"; case "HD": return "MD"; case "DD": return "WD"; case "GD": return "XD"; default: return code; } }

    /**
     * Brings a scheme of an earlier feature version up to date: schedule fields, discipline match mapping, live-level flags and removal of the planned
     * moments (never of applied ones). Only empty values are filled, edited values are kept.
     */
    int upgradeScheme(IMendixObject s) throws Exception {
        int changed = 0;
        if (s.getValue(ctx, "StartAt") == null) {
            s.setValue(ctx, "StartAt", utc(RankingRules.nextFirstMondays(LocalDate.now(ZoneOffset.UTC), 1).get(0).atTime(9, 0))); s.setValue(ctx, "Frequency", "FirstMondayMonthly"); s.setValue(ctx, "AutoRun", false); commit(s); changed++;
        }
        for (IMendixObject d : children("RankingDiscipline", "RankingDiscipline_Scheme", s)) {
            if (!text(d, "SlotKey").isEmpty()) continue;
            for (String[] def : DISCIPLINES) if (def[0].equals(text(d, "Code"))) { d.setValue(ctx, "SlotKey", def[5]); d.setValue(ctx, "Players", Long.parseLong(def[6])); d.setValue(ctx, "Active", true); commit(d); changed++; }
        }
        for (IMendixObject l : query("//MyFirstModule.RankingLevel[MyFirstModule.RankingLevel_Scheme='" + s.getId().toLong() + "'][IsDraft=empty]")) { l.setValue(ctx, "IsDraft", false); commit(l); changed++; }
        for (IMendixObject mo : children("RankingMoment", "RankingMoment_Scheme", s)) {
            if (text(mo, "Basis").isEmpty()) { mo.setValue(ctx, "Basis", CURRENT); commit(mo); }
            if (!"Planned".equals(text(mo, "Status"))) continue;
            for (IMendixObject c : query("//MyFirstModule.RankingChange[MyFirstModule.RankingChange_Moment='" + mo.getId().toLong() + "'][State='Simulated']")) Core.delete(ctx, c);
            Core.delete(ctx, mo); changed++;
        }
        return changed;
    }

    /**
     * Sample "test values": a draft copy of the levels with 20 % lower promotion and demotion averages, and a simulation of the next review with them.
     * Skipped when the scheme already has test values (edited ones are never replaced). Returns the number of simulated level changes, or -1 when skipped.
     */
    int createSampleTestValues(IMendixObject s) throws Exception {
        if (hasDraft(s)) return -1;
        ensureDraft(s);
        for (IMendixObject d : levels(s, true)) { d.setValue(ctx, "PromoteAt", bd(dec(d, "PromoteAt") * 0.8)); d.setValue(ctx, "DemoteBelow", bd(dec(d, "DemoteBelow") * 0.8)); commit(d); }
        IMendixObject mo = review(s, nextReviewOf(s), false, "Simulation", true);
        return (int)(num(mo, "Promoted") + num(mo, "Demoted"));
    }

    // ------------------------------------------------------------------ levels: current and test values

    List<IMendixObject> levels(IMendixObject s, boolean draft) throws Exception {
        String flag = draft ? "[IsDraft=true]" : "[IsDraft!=true]";
        List<IMendixObject> list = query("//MyFirstModule.RankingLevel[MyFirstModule.RankingLevel_Scheme='" + s.getId().toLong() + "']" + flag);
        list.sort(Comparator.comparingLong(l -> num(l, "Level"))); return list;
    }

    private boolean hasDraft(IMendixObject s) throws Exception { return !levels(s, true).isEmpty(); }

    /** Test values start as a copy of the current values. */
    private List<IMendixObject> ensureDraft(IMendixObject s) throws Exception {
        List<IMendixObject> draft = levels(s, true);
        if (!draft.isEmpty()) return draft;
        for (IMendixObject l : levels(s, false))
            link(make("RankingLevel", "Level", num(l, "Level"), "WinPoints", l.getValue(ctx, "WinPoints"), "PromoteAt", l.getValue(ctx, "PromoteAt"), "DemoteBelow", l.getValue(ctx, "DemoteBelow"), "PointsPublished", bool(l, "PointsPublished"), "IsDraft", true, "LevelName", text(l, "LevelName"), "SharePercent", l.getValue(ctx, "SharePercent")), "RankingLevel_Scheme", s);
        return levels(s, true);
    }

    private void rebuildLevels(IMendixObject s, int levels) throws Exception {
        for (IMendixObject old : levels(s, false)) Core.delete(ctx, old);
        RankingRules.Table t = RankingRules.defaultTable(levels);
        for (int level = 1; level <= levels; level++)
            link(make("RankingLevel", "Level", (long)level, "WinPoints", BigDecimal.valueOf(t.winPoints()[level]), "PromoteAt", BigDecimal.valueOf(t.promoteAt()[level]),
                "DemoteBelow", BigDecimal.valueOf(t.demoteBelow()[level]), "PointsPublished", level <= 12, "IsDraft", false), "RankingLevel_Scheme", s);
        s.setValue(ctx, "LevelCount", (long)levels); commit(s);
    }

    /** Adds a lowest level to the test values. */
    private void appendLevel(IMendixObject s) throws Exception {
        List<IMendixObject> draft = ensureDraft(s); long next = draft.size() + 1; require(next <= 30, "A scheme can have at most 30 levels.");
        IMendixObject last = draft.get(draft.size() - 1);
        double win = Math.max(1, Math.round(dec(last, "WinPoints") * 0.69));
        last.setValue(ctx, "DemoteBelow", BigDecimal.valueOf(Math.round(dec(last, "WinPoints") * 0.30))); commit(last);
        link(make("RankingLevel", "Level", next, "WinPoints", BigDecimal.valueOf(win), "PromoteAt", BigDecimal.valueOf(Math.round(dec(last, "WinPoints") * 0.55)), "DemoteBelow", BigDecimal.ZERO, "PointsPublished", false, "IsDraft", true), "RankingLevel_Scheme", s);
    }

    /** Removes the lowest level from the test values; players on it move up only when the test values are approved. */
    private void removeLowestLevel(IMendixObject s) throws Exception {
        List<IMendixObject> draft = ensureDraft(s); require(draft.size() > 2, "A scheme needs at least two levels.");
        IMendixObject last = draft.get(draft.size() - 1); Core.delete(ctx, last);
        IMendixObject now = draft.get(draft.size() - 2); now.setValue(ctx, "DemoteBelow", BigDecimal.ZERO); commit(now);
    }

    /** One edited cell of the levels grid: the row's values are written to the test values (created on the first edit). */
    private void saveCell(IMendixObject form, IMendixObject state, String ids) throws Exception {
        IMendixObject s = requireScheme(text(form, "SchemeKey"));
        IMendixObject row = Core.retrieveId(ctx, Core.createMendixIdentifier(Long.parseLong(ids.split(",")[0].trim())));
        require(row != null && row.getType().equals(M + "RankingRow") && text(row, "Kind").equals("Level"), "The row is no longer available. Refresh the page.");
        int number = (int)number(text(row, "C1"), "level");
        String name = text(row, "C7").trim(); require(!name.isEmpty(), "A level needs a name.");
        for (Map.Entry<Integer, String> other : levelNames(s).entrySet()) require(other.getKey() == number || !other.getValue().equalsIgnoreCase(name), "The level name " + name + " is already used.");
        // a name is only a label: it takes effect at once, without test values
        for (IMendixObject l : query("//MyFirstModule.RankingLevel[MyFirstModule.RankingLevel_Scheme='" + s.getId().toLong() + "'][Level=" + number + "]")) if (!name.equals(levelName(l))) { l.setValue(ctx, "LevelName", name); commit(l); }
        for (IMendixObject l : levels(s, hasDraft(s))) if (num(l, "Level") == number && plain(dec(l, "WinPoints")).equals(text(row, "C2").trim()) && plain(dec(l, "PromoteAt")).equals(text(row, "C3").trim()) && plain(dec(l, "DemoteBelow")).equals(text(row, "C4").trim())) { flags(form, state); return; }
        List<IMendixObject> draft = ensureDraft(s); IMendixObject level = null;
        for (IMendixObject d : draft) if (num(d, "Level") == number) level = d;
        require(level != null, "That level is not in the test values.");
        try {
            double win = number(text(row, "C2"), "points for a win"), up = number(text(row, "C3"), "promotion average"), down = number(text(row, "C4"), "demotion average");
            require(win > 0 && up >= 0 && down >= 0, "Points must be above 0 and the averages cannot be negative.");
            level.setValue(ctx, "WinPoints", bd(win)); level.setValue(ctx, "PromoteAt", bd(up)); level.setValue(ctx, "DemoteBelow", bd(down)); level.setValue(ctx, "PointsPublished", false); commit(level);
        } catch (RuntimeException invalid) {
            row.setValue(ctx, "C2", plain(dec(level, "WinPoints"))); row.setValue(ctx, "C3", plain(dec(level, "PromoteAt"))); row.setValue(ctx, "C4", plain(dec(level, "DemoteBelow"))); throw invalid;
        }
        row.setValue(ctx, "RecordKey", "L:" + id(level)); row.setValue(ctx, "C6", levelStatus(level, levels(s, false)));
        flags(form, state);
    }

    private String levelStatus(IMendixObject draft, List<IMendixObject> live) {
        for (IMendixObject l : live) if (num(l, "Level") == num(draft, "Level"))
            return dec(l, "WinPoints") == dec(draft, "WinPoints") && dec(l, "PromoteAt") == dec(draft, "PromoteAt") && dec(l, "DemoteBelow") == dec(draft, "DemoteBelow") ? "Unchanged" : "Changed (test value)";
        return "New level (test value)";
    }

    private void discardDraft(IMendixObject s) throws Exception {
        List<IMendixObject> draft = levels(s, true); if (!draft.isEmpty()) Core.delete(ctx, draft);
        clearSimulation(s, TEST);
    }

    /** Makes the test values the real values. Players on a removed lowest level move up and the move is written to their history. Returns how many moved. */
    private int approveDraft(IMendixObject s) throws Exception {
        List<IMendixObject> draft = levels(s, true); require(!draft.isEmpty(), "There are no test values to approve.");
        List<IMendixObject> live = levels(s, false); Map<Long, IMendixObject> liveByNumber = new HashMap<>();
        for (IMendixObject l : live) liveByNumber.put(num(l, "Level"), l);
        int count = draft.size(), moved = 0;
        for (IMendixObject d : draft) {
            IMendixObject l = liveByNumber.remove(num(d, "Level"));
            if (l == null) { link(make("RankingLevel", "Level", num(d, "Level"), "WinPoints", d.getValue(ctx, "WinPoints"), "PromoteAt", d.getValue(ctx, "PromoteAt"), "DemoteBelow", d.getValue(ctx, "DemoteBelow"), "PointsPublished", false, "IsDraft", false, "LevelName", text(d, "LevelName"), "SharePercent", d.getValue(ctx, "SharePercent")), "RankingLevel_Scheme", s); continue; }
            if (dec(l, "WinPoints") != dec(d, "WinPoints")) l.setValue(ctx, "PointsPublished", false);
            l.setValue(ctx, "WinPoints", d.getValue(ctx, "WinPoints")); l.setValue(ctx, "PromoteAt", d.getValue(ctx, "PromoteAt")); l.setValue(ctx, "DemoteBelow", d.getValue(ctx, "DemoteBelow")); commit(l);
        }
        for (IMendixObject gone : liveByNumber.values()) Core.delete(ctx, gone);
        if (!liveByNumber.isEmpty())
            for (IMendixObject pr : children("PlayerRanking", "PlayerRanking_Scheme", s)) {
                if (num(pr, "Level") <= count) continue;
                recordChange(s, pr, null, "Levels", "Applied", (int)num(pr, "Level"), count, dec(pr, "Average"), (int)num(pr, "Matches"), LocalDate.now(ZoneOffset.UTC), "Level " + num(pr, "Level") + " was removed from the scheme.", "");
                pr.setValue(ctx, "Level", (long)count); commit(pr); moved++;
            }
        Core.delete(ctx, draft);
        s.setValue(ctx, "LevelCount", (long)count); s.setValue(ctx, "Version", num(s, "Version") + 1); commit(s);
        clearSimulation(s);
        return moved;
    }

    private void deleteScheme(IMendixObject s) throws Exception {
        String id = String.valueOf(s.getId().toLong());
        for (String type : new String[]{"RankingChange", "PlayerRanking", "RankingMoment", "RankingLevel", "RankingDiscipline", "RankingDataset", "RankingScenario"})
            Core.delete(ctx, query("//MyFirstModule." + type + "[MyFirstModule." + type + "_Scheme='" + id + "']"));
        Core.delete(ctx, s);
    }

    /** Removes the simulated changes and the simulated reviews of the scheme. */
    int clearSimulation(IMendixObject s) throws Exception { return clearSimulation(s, null); }

    /** Removes the simulations with the given basis (current or test values), or all of them. */
    private int clearSimulation(IMendixObject s, String basis) throws Exception {
        String filter = basis == null ? "" : "[Basis='" + basis + "']";
        List<IMendixObject> reviews = query("//MyFirstModule.RankingMoment[MyFirstModule.RankingMoment_Scheme='" + s.getId().toLong() + "'][Status='Simulated']" + filter);
        int n = 0;
        for (IMendixObject mo : reviews) {
            List<IMendixObject> changes = query("//MyFirstModule.RankingChange[MyFirstModule.RankingChange_Moment='" + mo.getId().toLong() + "'][State='Simulated']");
            if (!changes.isEmpty()) { Core.delete(ctx, changes); n += changes.size(); }
        }
        if (!reviews.isEmpty()) Core.delete(ctx, reviews);
        if (basis == null) { List<IMendixObject> orphans = query("//MyFirstModule.RankingChange[MyFirstModule.RankingChange_Scheme='" + s.getId().toLong() + "'][State='Simulated']"); if (!orphans.isEmpty()) { Core.delete(ctx, orphans); n += orphans.size(); } }
        return n;
    }

    // ------------------------------------------------------------------ match data

    private static final class History {
        final String key, name, discipline; final List<Entry> entries = new ArrayList<>(); Integer lastBracket; LocalDate lastDate;
        History(String key, String name, String discipline) { this.key = key; this.name = name; this.discipline = discipline; }
    }
    private static final class Entry { LocalDate date; boolean won; Integer own; List<String> opponents = new ArrayList<>(); List<String> opponentNames = new ArrayList<>(); List<Integer> opponentBrackets = new ArrayList<>(); }

    private Map<String, String> slotMap(IMendixObject scheme) throws Exception {
        Map<String, String> map = new HashMap<>();
        for (IMendixObject d : children("RankingDiscipline", "RankingDiscipline_Scheme", scheme)) if (bool(d, "Active") && !text(d, "SlotKey").isEmpty()) map.put(text(d, "SlotKey") + "|" + num(d, "Players"), text(d, "Code"));
        return map;
    }

    /** Played, not excluded games of the federation's leagues, per player and active discipline. */
    private Map<String, History> gather(IMendixObject scheme) throws Exception {
        Map<String, String> slots = slotMap(scheme);
        String fed = String.valueOf(ref(scheme, "RankingScheme_Federation").getId().toLong());
        String league = "MyFirstModule.CompetitionLeague/MyFirstModule.CompetitionLeague_Federation='" + fed + "'";
        String fixturePath = "MyFirstModule.CompetitionFixture_League/" + league;
        Map<Long, Date> fixtureStart = new HashMap<>();
        for (IMendixObject f : query("//MyFirstModule.CompetitionFixture[" + fixturePath + "]")) fixtureStart.put(f.getId().toLong(), (Date)f.getValue(ctx, "StartsAt"));
        Map<Long, Long> regFixture = new HashMap<>();
        for (IMendixObject r : query("//MyFirstModule.MatchRegistration[MyFirstModule.MatchRegistration_Fixture/MyFirstModule.CompetitionFixture/" + fixturePath + "]")) {
            IMendixIdentifier f = (IMendixIdentifier)r.getValue(ctx, M + "MatchRegistration_Fixture"); if (f != null) regFixture.put(r.getId().toLong(), f.toLong());
        }
        Map<Long, IMendixObject> contests = new HashMap<>();
        for (IMendixObject c : query("//MyFirstModule.MatchContest[MyFirstModule.MatchContest_Registration/MyFirstModule.MatchRegistration/MyFirstModule.MatchRegistration_Fixture/MyFirstModule.CompetitionFixture/" + fixturePath + "]"))
            contests.put(c.getId().toLong(), c);
        Map<String, History> result = new LinkedHashMap<>();
        Map<Long, List<IMendixObject>> lineups = new HashMap<>();
        for (IMendixObject l : query("//MyFirstModule.MatchLineup[MyFirstModule.MatchLineup_Contest/MyFirstModule.MatchContest/MyFirstModule.MatchContest_Registration/MyFirstModule.MatchRegistration/MyFirstModule.MatchRegistration_Fixture/MyFirstModule.CompetitionFixture/" + fixturePath + "]")) {
            IMendixIdentifier ci = (IMendixIdentifier)l.getValue(ctx, M + "MatchLineup_Contest"); IMendixObject c = ci == null ? null : contests.get(ci.toLong()); if (c == null) continue;
            if (bool(c, "Excluded") || bool(c, "RankingExcluded")) continue;
            String winner = text(c, "OfficialWinner"); if (!winner.equals("Home") && !winner.equals("Away")) continue;
            if (!slots.containsKey(text(c, "Discipline") + "|" + num(c, "PlayersPerSide"))) continue;
            IMendixIdentifier ri = (IMendixIdentifier)c.getValue(ctx, M + "MatchContest_Registration"); Long fi = ri == null ? null : regFixture.get(ri.toLong());
            Date start = fi == null ? null : fixtureStart.get(fi); if (start == null) continue;
            lineups.computeIfAbsent(ci.toLong(), k -> new ArrayList<>()).add(l);
        }
        for (Map.Entry<Long, List<IMendixObject>> e : lineups.entrySet()) {
            IMendixObject c = contests.get(e.getKey());
            String winner = text(c, "OfficialWinner"), discipline = slots.get(text(c, "Discipline") + "|" + num(c, "PlayersPerSide"));
            IMendixIdentifier ri = (IMendixIdentifier)c.getValue(ctx, M + "MatchContest_Registration");
            LocalDate date = fixtureStart.get(regFixture.get(ri.toLong())).toInstant().atZone(ZoneOffset.UTC).toLocalDate();
            Map<String, List<String[]>> sides = new HashMap<>();
            for (IMendixObject l : e.getValue()) for (int i = 1; i <= 2; i++) {
                String raw = text(l, "Player" + i + "Name"); if (raw.isBlank()) continue;
                Integer bracket = RankingRules.bracketLevel(raw);
                sides.computeIfAbsent(text(l, "Side"), k -> new ArrayList<>()).add(new String[]{RankingRules.nameKey(raw), RankingRules.cleanName(raw), bracket == null ? "" : String.valueOf(bracket)});
            }
            for (String side : new String[]{"Home", "Away"}) {
                List<String[]> mine = sides.getOrDefault(side, List.of()), theirs = sides.getOrDefault(side.equals("Home") ? "Away" : "Home", List.of());
                if (mine.isEmpty() || theirs.isEmpty()) continue;
                for (String[] p : mine) {
                    String hk = discipline + "|" + p[0];
                    History h = result.computeIfAbsent(hk, k -> new History(p[0], p[1], discipline));
                    Entry en = new Entry(); en.date = date; en.won = winner.equals(side); en.own = p[2].isEmpty() ? null : Integer.valueOf(p[2]);
                    for (String[] o : theirs) { en.opponents.add(o[0]); en.opponentNames.add(o[1]); en.opponentBrackets.add(o[2].isEmpty() ? null : Integer.valueOf(o[2])); }
                    h.entries.add(en);
                    if (en.own != null && (h.lastDate == null || !date.isBefore(h.lastDate))) { h.lastDate = date; h.lastBracket = en.own; }
                }
            }
        }
        return result;
    }

    /** The player's matches as the rules see them: opponent level from the sheet, else from the current ranking, else the lowest level. */
    private List<RankingRules.Match> toMatches(History h, String disc, int ownLevel, Map<String, Integer> levelOf, int lowest) {
        List<RankingRules.Match> matches = new ArrayList<>();
        for (Entry e : h.entries) {
            double opp = 0; StringBuilder label = new StringBuilder();
            for (int k = 0; k < e.opponents.size(); k++) {
                int level = e.opponentBrackets.get(k) != null ? e.opponentBrackets.get(k) : levelOf.getOrDefault(disc + "|" + e.opponents.get(k), lowest);
                level = Math.min(level, lowest);
                opp += level; if (k > 0) label.append(" & "); label.append(e.opponentNames.get(k)).append(" (").append(level).append(')');
            }
            opp /= e.opponents.size();
            matches.add(new RankingRules.Match(e.date, e.won, opp, Math.min(e.own != null ? e.own : ownLevel, lowest), label.toString()));
        }
        return matches;
    }

    /** One published level from an imported scraper run, display-only and identified by name and club: the source has no stable player id. False when the level is outside the scheme or this name and club already has this discipline; nothing is clamped or overwritten. */
    boolean importPublishedRanking(IMendixObject scheme, String code, String name, String club, int level, String note) throws Exception {
        if (level < 1 || level > num(scheme, "LevelCount")) return false;
        IMendixObject discipline = null;
        for (IMendixObject d : children("RankingDiscipline", "RankingDiscipline_Scheme", scheme)) if (text(d, "Code").equals(code)) discipline = d;
        String key = RankingRules.nameKey(name) + "|" + RankingRules.nameKey(club);
        if (discipline == null) return false;
        for (IMendixObject pr : children("PlayerRanking", "PlayerRanking_Scheme", scheme)) if (key.equals(text(pr, "PlayerKey")) && discipline.getId().equals(pr.getValue(ctx, M + "PlayerRanking_Discipline"))) return false;
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        IMendixObject pr = make("PlayerRanking", "PlayerKey", key, "PlayerName", RankingRules.cleanName(name), "Level", (long)level, "Average", BigDecimal.ZERO, "Matches", 0L, "BonusCounter", 0L, "LastActiveOn", utc(today), "Version", 1L);
        link(pr, "PlayerRanking_Scheme", scheme); link(pr, "PlayerRanking_Discipline", discipline);
        recordChange(scheme, pr, null, "Seed", "Applied", 0, level, 0, 0, today, note, "");
        return true;
    }

    /** Adds a ranking for every player in the match results who has none yet: the level shown on the match sheet, else the scheme's lowest level. */
    int seedPlayers(IMendixObject scheme) throws Exception {
        Map<String, History> history = gather(scheme);
        Map<String, IMendixObject> disciplines = new HashMap<>();
        for (IMendixObject d : children("RankingDiscipline", "RankingDiscipline_Scheme", scheme)) if (bool(d, "Active")) disciplines.put(text(d, "Code"), d);
        Set<String> have = new HashSet<>();
        for (IMendixObject pr : children("PlayerRanking", "PlayerRanking_Scheme", scheme)) have.add(text(ref(pr, "PlayerRanking_Discipline"), "Code") + "|" + text(pr, "PlayerKey"));
        int levels = (int)num(scheme, "LevelCount"), n = 0; LocalDate today = LocalDate.now(ZoneOffset.UTC);
        for (Map.Entry<String, History> e : history.entrySet()) {
            History h = e.getValue(); IMendixObject disc = disciplines.get(h.discipline);
            if (disc == null || !have.add(e.getKey())) continue;
            int level = h.lastBracket != null ? Math.max(1, Math.min(levels, h.lastBracket)) : levels;
            IMendixObject pr = make("PlayerRanking", "PlayerKey", h.key, "PlayerName", h.name, "Level", (long)level, "Average", BigDecimal.ZERO, "Matches", 0L, "BonusCounter", 0L, "LastActiveOn", utc(today), "Version", 1L);
            link(pr, "PlayerRanking_Scheme", scheme); link(pr, "PlayerRanking_Discipline", disc);
            recordChange(scheme, pr, null, "Seed", "Applied", 0, level, 0, 0, today,
                h.lastBracket != null ? "Level " + level + " shown on the match sheet." : "New player starts at the lowest level.", "");
            n++;
        }
        return n;
    }

    // ------------------------------------------------------------------ reviews

    private RankingRules.Params params(IMendixObject s, int levels) {
        return new RankingRules.Params(levels, (int)num(s, "WindowWeeks"), (int)num(s, "MaxMatches"), (int)num(s, "MinDivisor"), (int)num(s, "MaxSpread"),
            (int)num(s, "InactiveWeeks"), (int)num(s, "InactiveMinMatches"), (int)num(s, "InactiveDrop"), bool(s, "IgnoreHigherLosses"));
    }

    /** Points and thresholds of the current values, or of the test values. */
    private RankingRules.Table table(IMendixObject s, boolean draft) throws Exception {
        List<IMendixObject> list = levels(s, draft); int levels = list.size();
        double[] win = new double[levels + 1], up = new double[levels + 1], down = new double[levels + 1];
        for (IMendixObject lv : list) { int i = (int)num(lv, "Level"); if (i < 1 || i > levels) continue; win[i] = dec(lv, "WinPoints"); up[i] = dec(lv, "PromoteAt"); down[i] = dec(lv, "DemoteBelow"); }
        return new RankingRules.Table(levels, win, up, down);
    }

    /** The next scheduled review of the scheme. */
    LocalDateTime nextReviewOf(IMendixObject s) {
        require(s.getValue(ctx, "StartAt") != null && !text(s, "Frequency").isEmpty(), "Set the review schedule first.");
        return RankingRules.nextReview(text(s, "Frequency"), at(s, "StartAt"), s.getValue(ctx, "LastReviewAt") == null ? null : at(s, "LastReviewAt"));
    }

    /**
     * Reviews every player of the scheme as of the given moment. A simulation (with the current or the test values) writes changes with state
     * Simulated and replaces the earlier simulation with the same values; applying always uses the current values, updates the rankings and the last
     * review time. Either way one review record is added to the log.
     */
    IMendixObject review(IMendixObject scheme, LocalDateTime asOf, boolean apply, String source, boolean testValues) throws Exception {
        require(!(apply && testValues), "Approve the test values before applying a review.");
        if (apply) clearSimulation(scheme); else clearSimulation(scheme, testValues ? TEST : CURRENT);
        IMendixObject review = make("RankingMoment", "DueOn", utc(asOf), "Label", RankingRules.frequencyLabel(text(scheme, "Frequency")), "Status", apply ? "Applied" : "Simulated", "Source", source,
            "Basis", testValues ? TEST : CURRENT, "AppliedBy", actor, "Evaluated", 0L, "Promoted", 0L, "Demoted", 0L);
        if (apply) review.setValue(ctx, "AppliedAt", new Date());
        link(review, "RankingMoment_Scheme", scheme);
        int[] counts = evaluate(scheme, asOf.toLocalDate(), apply, review, testValues);
        review.setValue(ctx, "Evaluated", (long)counts[0]); review.setValue(ctx, "Promoted", (long)counts[1]); review.setValue(ctx, "Demoted", (long)counts[2]); commit(review);
        if (apply) { scheme.setValue(ctx, "LastReviewAt", utc(asOf)); commit(scheme); }
        return review;
    }

    private static final class Outcome { IMendixObject pr; String disc, discName; int from; RankingRules.Result r; }

    /** The result of reviewing every player of an active discipline, without writing anything. */
    private List<Outcome> outcomes(IMendixObject scheme, LocalDate date, boolean testValues) throws Exception {
        Map<String, History> history = gather(scheme);
        RankingRules.Table t = table(scheme, testValues); RankingRules.Params p = params(scheme, t.levels());
        com.mendix.thirdparty.org.json.JSONObject config = new RankingWorkbench(this, ctx, actor).liveConfig(scheme);
        if (!config.optString("method", RankingEngine.AVERAGE).equals(RankingEngine.AVERAGE) || !config.optString("placement", RankingEngine.THRESHOLDS).equals(RankingEngine.THRESHOLDS) || config.optInt("maxUp", 1) > 1 || config.optInt("maxDown", 1) > 1) return engineOutcomes(scheme, history, date, t, p, config);
        List<IMendixObject> players = children("PlayerRanking", "PlayerRanking_Scheme", scheme);
        Map<String, Integer> levelOf = new HashMap<>(); Map<String, List<Integer>> byPerson = new HashMap<>(); Map<String, String> codes = new HashMap<>(), names = new HashMap<>(); Map<String, Integer> thresholds = new HashMap<>();
        for (IMendixObject d : children("RankingDiscipline", "RankingDiscipline_Scheme", scheme)) if (bool(d, "Active")) { codes.put(id(d), text(d, "Code")); names.put(id(d), text(d, "Name")); thresholds.put(text(d, "Code"), (int)num(d, "BonusThreshold")); }
        Map<String, LocalDate> lastChange = new HashMap<>();
        for (IMendixObject c : query("//MyFirstModule.RankingChange[MyFirstModule.RankingChange_Scheme='" + scheme.getId().toLong() + "'][State='Applied'][Kind!='Seed']")) {
            if (num(c, "FromLevel") == num(c, "ToLevel")) continue;
            IMendixIdentifier pi = (IMendixIdentifier)c.getValue(ctx, M + "RankingChange_PlayerRanking"); if (pi == null) continue;
            LocalDate d = ((Date)c.getValue(ctx, "EffectiveOn")).toInstant().atZone(ZoneOffset.UTC).toLocalDate(); lastChange.merge(String.valueOf(pi.toLong()), d, (a, b) -> a.isAfter(b) ? a : b);
        }
        String[] discOf = new String[players.size()]; int[] levelNow = new int[players.size()];
        for (int i = 0; i < players.size(); i++) {
            IMendixObject pr = players.get(i); String did = String.valueOf(((IMendixIdentifier)pr.getValue(ctx, M + "PlayerRanking_Discipline")).toLong()); discOf[i] = codes.get(did);
            if (discOf[i] == null) continue;
            levelNow[i] = Math.min((int)num(pr, "Level"), p.levels());
            levelOf.put(discOf[i] + "|" + text(pr, "PlayerKey"), levelNow[i]); byPerson.computeIfAbsent(text(pr, "PlayerKey"), k -> new ArrayList<>()).add(levelNow[i]);
        }
        List<Outcome> result = new ArrayList<>();
        for (int i = 0; i < players.size(); i++) {
            if (discOf[i] == null) continue;
            IMendixObject pr = players.get(i); String disc = discOf[i], key = text(pr, "PlayerKey"); int level = levelNow[i];
            History h = history.get(disc + "|" + key);
            List<RankingRules.Match> matches = h == null ? new ArrayList<>() : toMatches(h, disc, level, levelOf, p.levels());
            List<Integer> others = new ArrayList<>(byPerson.get(key)); others.remove(Integer.valueOf(level));
            LocalDate active = pr.getValue(ctx, "LastActiveOn") == null ? date : day(pr, "LastActiveOn");
            Outcome o = new Outcome(); o.pr = pr; o.disc = disc; o.from = (int)num(pr, "Level");
            o.discName = names.get(String.valueOf(((IMendixIdentifier)pr.getValue(ctx, M + "PlayerRanking_Discipline")).toLong()));
            o.r = RankingRules.evaluate(level, matches, date, active, lastChange.getOrDefault(id(pr), LocalDate.MIN), thresholds.getOrDefault(disc, 0), others, p, t);
            result.add(o);
        }
        return result;
    }

    /** Review of a scheme with another calculation method or a pyramid of shares: the same rules as the simulation, starting from the players' current levels. */
    private List<Outcome> engineOutcomes(IMendixObject scheme, Map<String, History> history, LocalDate date, RankingRules.Table t, RankingRules.Params p, com.mendix.thirdparty.org.json.JSONObject config) throws Exception {
        RankingEngine.Config c = RankingWorkbench.readEngine(config); c.table = t; c.params = p;
        Map<Long, IMendixObject> discs = new HashMap<>(); Map<String, Integer> levelOf = new HashMap<>();
        for (IMendixObject d : children("RankingDiscipline", "RankingDiscipline_Scheme", scheme)) if (bool(d, "Active")) discs.put(d.getId().toLong(), d);
        List<IMendixObject> rankings = children("PlayerRanking", "PlayerRanking_Scheme", scheme);
        Map<RankingEngine.Player, IMendixObject> byPlayer = new LinkedHashMap<>(); Map<String, RankingEngine.Player> byKey = new HashMap<>();
        for (IMendixObject pr : rankings) {
            IMendixObject d = discs.get(((IMendixIdentifier)pr.getValue(ctx, M + "PlayerRanking_Discipline")).toLong()); if (d == null) continue;
            levelOf.put(text(d, "Code") + "|" + text(pr, "PlayerKey"), Math.min((int)num(pr, "Level"), p.levels()));
        }
        for (IMendixObject pr : rankings) {
            IMendixObject d = discs.get(((IMendixIdentifier)pr.getValue(ctx, M + "PlayerRanking_Discipline")).toLong()); if (d == null) continue;
            String code = text(d, "Code"), key = text(pr, "PlayerKey"); int level = Math.min((int)num(pr, "Level"), p.levels());
            RankingEngine.Player player = new RankingEngine.Player(key, text(pr, "PlayerName"), code); player.level = level; player.startLevel = level;
            player.rating = c.eloStart + c.eloStep * (p.levels() - level); player.lastActive = pr.getValue(ctx, "LastActiveOn") == null ? date : day(pr, "LastActiveOn");
            History h = history.get(code + "|" + key); if (h != null) player.matches.addAll(toMatches(h, code, level, levelOf, p.levels()));
            byPlayer.put(player, pr); byKey.put(code + "|" + key, player);
        }
        // the running rating is replayed over the same games, one player's view at a time, in date order
        List<Object[]> views = new ArrayList<>();
        for (History h : history.values()) { RankingEngine.Player me = byKey.get(h.discipline + "|" + h.key); if (me != null) for (Entry e : h.entries) if (!e.date.isAfter(date)) views.add(new Object[]{e.date, me, e}); }
        views.sort(Comparator.comparing((Object[] v) -> (LocalDate)v[0]).thenComparing(v -> ((RankingEngine.Player)v[1]).name));
        for (Object[] v : views) {
            RankingEngine.Player me = (RankingEngine.Player)v[1]; Entry e = (Entry)v[2]; double opponent = 0; int known = 0;
            for (String o : e.opponents) { RankingEngine.Player other = byKey.get(me.discipline + "|" + o); if (other != null) { opponent += other.rating; known++; } }
            double rating = known == 0 ? c.eloStart : opponent / known; me.games++;
            me.rating += RankingEngine.eloDelta(me.rating, rating, e.won, me.games <= c.eloNewGames ? c.eloKNew : c.eloK, c.eloScale);
        }
        RankingEngine.review(byPlayer.keySet(), date, c);
        List<Outcome> result = new ArrayList<>();
        for (Map.Entry<RankingEngine.Player, IMendixObject> e : byPlayer.entrySet()) {
            RankingEngine.Player player = e.getKey(); Outcome o = new Outcome(); o.pr = e.getValue(); o.disc = player.discipline; o.from = (int)num(o.pr, "Level");
            o.discName = text(discs.get(((IMendixIdentifier)o.pr.getValue(ctx, M + "PlayerRanking_Discipline")).toLong()), "Name");
            o.r = new RankingRules.Result(player.level, player.kind, player.reason, player.score, player.counted, player.inWindow, 0, player.lastActive == null ? date : player.lastActive);
            result.add(o);
        }
        return result;
    }

    /** Writes a review: simulated changes only, or applied changes and updated rankings (current values only). Returns evaluated, promoted and demoted counts. */
    private int[] evaluate(IMendixObject scheme, LocalDate date, boolean apply, IMendixObject review, boolean testValues) throws Exception {
        int evaluated = 0, promoted = 0, demoted = 0;
        for (Outcome o : outcomes(scheme, date, testValues)) {
            IMendixObject pr = o.pr; RankingRules.Result r = o.r; evaluated++;
            boolean changed = r.level() != o.from;
            if (changed) { if (r.level() < o.from) promoted++; else demoted++; recordChange(scheme, pr, review, r.kind(), apply ? "Applied" : "Simulated", o.from, r.level(), r.average(), r.matchesInWindow(), date, r.reason(), ""); }
            if (apply) {
                pr.setValue(ctx, "Level", (long)r.level()); pr.setValue(ctx, "Average", bd(r.average())); pr.setValue(ctx, "Matches", (long)r.matchesInWindow());
                pr.setValue(ctx, "BonusCounter", changed ? 0L : (long)r.bonusCounter()); pr.setValue(ctx, "LastActiveOn", utc(r.lastActive() == null ? date : r.lastActive())); pr.setValue(ctx, "Version", num(pr, "Version") + 1);
                commit(pr);
            }
        }
        return new int[]{evaluated, promoted, demoted};
    }

    /** Federation override. The comment is mandatory and is stored on the history entry. */
    void forceChange(IMendixObject scheme, IMendixObject pr, int level, String comment) throws Exception {
        require(comment != null && !comment.isBlank(), "A comment is required for a forced change.");
        require(level >= 1 && level <= num(scheme, "LevelCount"), "Choose one of the levels of this ranking.");
        int from = (int)num(pr, "Level"); require(level != from, "The player is already at that level.");
        List<Integer> others = new ArrayList<>();
        for (IMendixObject o : children("PlayerRanking", "PlayerRanking_Scheme", scheme)) if (text(o, "PlayerKey").equals(text(pr, "PlayerKey")) && !o.getId().equals(pr.getId())) others.add((int)num(o, "Level"));
        boolean spread = !RankingRules.spreadOk(level, others, (int)num(scheme, "MaxSpread"));
        recordChange(scheme, pr, null, "Forced", "Applied", from, level, dec(pr, "Average"), (int)num(pr, "Matches"), LocalDate.now(ZoneOffset.UTC),
            "Forced " + (level < from ? "upgrade" : "downgrade") + " by " + actor + (spread ? " (exceeds the maximum level difference between disciplines)" : "") + ".", comment.trim());
        pr.setValue(ctx, "Level", (long)level); pr.setValue(ctx, "BonusCounter", 0L); pr.setValue(ctx, "Version", num(pr, "Version") + 1); commit(pr);
    }

    void recordChange(IMendixObject scheme, IMendixObject pr, IMendixObject moment, String kind, String state, int from, int to, double average, int matches, LocalDate effective, String reason, String comment) throws Exception {
        IMendixObject c = make("RankingChange", "At", new Date(), "EffectiveOn", utc(effective), "Kind", kind, "State", state, "FromLevel", (long)from, "ToLevel", (long)to, "Average", bd(average), "Matches", (long)matches,
            "Actor", actor, "Reason", reason, "Comment", comment);
        link(c, "RankingChange_Scheme", scheme); link(c, "RankingChange_PlayerRanking", pr); if (moment != null) link(c, "RankingChange_Moment", moment);
    }

    // ------------------------------------------------------------------ review view

    /** What a review key means: the as-of date, whether it uses the test values, the moment (null for the upcoming review) and a description. */
    private static final class Context { LocalDateTime asOf; boolean test; IMendixObject moment; String description; boolean stored; }

    private Context context(IMendixObject s, String key) throws Exception {
        Context c = new Context();
        if (key.startsWith("U:")) { c.asOf = nextReviewOf(s); c.test = false; c.description = "Upcoming review of " + fmt(c.asOf) + ", calculated with the current values."; return c; }
        IMendixObject m = load(key); c.moment = m; c.asOf = at(m, "DueOn"); c.stored = "Applied".equals(text(m, "Status")); c.test = !c.stored && TEST.equals(text(m, "Basis"));
        c.description = c.stored ? "Review of " + fmt(c.asOf) + " as applied (" + text(m, "Source").toLowerCase(Locale.ROOT) + "). Scoring is recalculated with the current values."
            : "Simulation of the review of " + fmt(c.asOf) + ", calculated with the " + (c.test ? "test values" : "current values") + ".";
        return c;
    }

    /** The review page: every player with promoted, demoted or stays, from the current values, the simulation's values or the applied changes. */
    private void openReview(IMendixObject form, String key) throws Exception {
        IMendixObject s = load("S:" + text(form, "SchemeKey")); Context c = context(s, key);
        IMendixObject next = Core.instantiate(ctx, M + "RankingForm");
        next.setValue(ctx, "SchemeKey", id(s)); next.setValue(ctx, "ReviewKey", key); next.setValue(ctx, "CanManage", true); next.setValue(ctx, "HasScheme", true); next.setValue(ctx, "Title", "Review of " + fmt(c.asOf));
        List<String[]> rows = new ArrayList<>(); int up = 0, down = 0, stay = 0; Map<Integer, String> reviewNames = levelNames(s);
        if (c.stored) {
            Map<Long, IMendixObject> changed = new HashMap<>();
            for (IMendixObject ch : query("//MyFirstModule.RankingChange[MyFirstModule.RankingChange_Moment='" + c.moment.getId().toLong() + "'][State='Applied']")) changed.put(((IMendixIdentifier)ch.getValue(ctx, M + "RankingChange_PlayerRanking")).toLong(), ch);
            Map<Long, IMendixObject> discs = new HashMap<>(); for (IMendixObject d : children("RankingDiscipline", "RankingDiscipline_Scheme", s)) discs.put(d.getId().toLong(), d);
            for (IMendixObject pr : children("PlayerRanking", "PlayerRanking_Scheme", s)) {
                IMendixObject d = discs.get(((IMendixIdentifier)pr.getValue(ctx, M + "PlayerRanking_Discipline")).toLong()); if (!bool(d, "Active")) continue;
                IMendixObject ch = changed.get(pr.getId().toLong()); long from = ch == null ? num(pr, "Level") : num(ch, "FromLevel"), to = ch == null ? num(pr, "Level") : num(ch, "ToLevel");
                String outcome = to < from ? "Promoted" : to > from ? "Demoted" : "Stays"; if (outcome.equals("Promoted")) up++; else if (outcome.equals("Demoted")) down++; else stay++;
                rows.add(new String[]{outcome, text(pr, "PlayerName"), text(d, "Name"), nameOf(reviewNames, from), nameOf(reviewNames, to), ch == null ? plain(dec(pr, "Average")) : plain(dec(ch, "Average")), ch == null ? "" : String.valueOf(num(ch, "Matches")), ch == null ? "No change" : text(ch, "Reason"), "P:" + id(pr)});
            }
        } else {
            for (Outcome o : outcomes(s, c.asOf.toLocalDate(), c.test)) {
                int to = o.r.level(); String outcome = to < o.from ? "Promoted" : to > o.from ? "Demoted" : "Stays"; if (outcome.equals("Promoted")) up++; else if (outcome.equals("Demoted")) down++; else stay++;
                rows.add(new String[]{outcome, text(o.pr, "PlayerName"), o.discName, nameOf(reviewNames, o.from), nameOf(reviewNames, to), plain(o.r.average()), String.valueOf(o.r.matchesInWindow()), o.r.reason(), "P:" + id(o.pr)});
            }
        }
        List<String> order = List.of("Promoted", "Demoted", "Stays");
        rows.sort(Comparator.comparingInt((String[] r) -> order.indexOf(r[0])).thenComparing(r -> r[2]).thenComparing(r -> r[1]));
        for (String[] r : rows) row(next, "Outcome", r[8], r[0], r[1], r[2], r[3], r[4], r[5], r[6], r[7]);
        next.setValue(ctx, "Info", c.description + " Promoted " + up + ", demoted " + down + ", stay " + stay + ". Select a player and press View scoring to see the calculation.");
        flags(next, state(next));
        Core.microflowCall(M + "ACT_Ranking_ShowReview").withParam("Form", next).execute(ctx);
    }

    // ------------------------------------------------------------------ view (form, state, rows)

    IMendixObject state(IMendixObject form) throws Exception {
        List<IMendixObject> found = Core.retrieveByPath(ctx, form, M + "RankingState_Form");
        if (!found.isEmpty()) return found.get(0);
        IMendixObject s = Core.instantiate(ctx, M + "RankingState"); s.setValue(ctx, M + "RankingState_Form", form.getId()); return s;
    }

    void clear(IMendixObject form) throws Exception {
        List<IMendixObject> rows = Core.retrieveByPath(ctx, form, M + "RankingRow_Form"); if (!rows.isEmpty()) Core.delete(ctx, rows);
    }

    IMendixObject row(IMendixObject form, String kind, String key, String... cells) throws Exception {
        IMendixObject r = Core.instantiate(ctx, M + "RankingRow"); r.setValue(ctx, "Kind", kind); r.setValue(ctx, "RecordKey", key);
        for (int i = 0; i < cells.length && i < 10; i++) r.setValue(ctx, "C" + (i + 1), cells[i]);
        r.setValue(ctx, M + "RankingRow_Form", form.getId()); return r;
    }

    private List<IMendixObject> visibleFederations() throws Exception {
        List<IMendixObject> result = new ArrayList<>();
        for (IMendixObject f : query("//MyFirstModule.Federation")) if (manage(f)) result.add(f);
        result.sort(Comparator.comparing(f -> text(f, "Name"))); return result;
    }

    /** Without a scheme key the form lists federations and schemes; with one it shows that scheme's levels, reviews, players, disciplines and changes. A review page is never rebuilt. */
    private void refresh(IMendixObject form, IMendixObject state) throws Exception {
        if (!text(form, "ReviewKey").isEmpty()) { flags(form, state); return; }
        if (text(form, "Mode").startsWith("Lab")) { new RankingWorkbench(this, ctx, actor).refresh(form, requireScheme(text(form, "SchemeKey"))); flags(form, state); return; }
        clear(form); form.setValue(ctx, "Selected", "");
        String schemeKey = text(form, "SchemeKey"); IMendixObject s = null;
        if (!schemeKey.isEmpty()) { try { s = load("S:" + schemeKey); } catch (RuntimeException gone) { s = null; } }
        if (s != null && !manage(ref(s, "RankingScheme_Federation"))) s = null;
        if (s == null) { form.setValue(ctx, "SchemeKey", ""); refreshList(form); }
        else refreshScheme(form, s);
        flags(form, state);
    }

    private void refreshList(IMendixObject form) throws Exception {
        List<IMendixObject> feds = visibleFederations(); form.setValue(ctx, "CanManage", !feds.isEmpty()); form.setValue(ctx, "HasScheme", false);
        for (IMendixObject f : feds) {
            List<IMendixObject> ss = schemes(f);
            if (ss.isEmpty()) row(form, "Scheme", "F:" + id(f), text(f, "Name"), text(f, "Sport"), "No ranking scheme", "", "", "", "Not defined");
            for (IMendixObject s : ss) {
                String next = ""; try { if (s.getValue(ctx, "StartAt") != null && !text(s, "Frequency").isEmpty()) next = fmt(nextReviewOf(s)); } catch (RuntimeException ignore) { next = ""; }
                row(form, "Scheme", "S:" + id(s), text(f, "Name"), text(s, "Sport"), text(s, "Name"), String.valueOf(num(s, "LevelCount")), String.valueOf(children("PlayerRanking", "PlayerRanking_Scheme", s).size()),
                    next.isEmpty() ? "No schedule" : "Next review " + next + (bool(s, "AutoRun") ? " (automatic)" : " (manual)"), bool(s, "Active") ? "Active" : "Inactive");
            }
        }
        form.setValue(ctx, "Info", feds.isEmpty() ? "You do not manage the rankings of any federation." : "Select a federation and press Create to define a ranking scheme, or double-click a scheme to open it.");
    }

    private void refreshScheme(IMendixObject form, IMendixObject s) throws Exception {
        form.setValue(ctx, "CanManage", true); form.setValue(ctx, "HasScheme", true);
        boolean draft = hasDraft(s);
        form.setValue(ctx, "Info", text(s, "Name") + " · " + num(s, "LevelCount") + " levels · " + (bool(s, "Active") ? "active" : "inactive") + ". " + SAMPLE_NOTE);
        String schedule = "Reviews are not scheduled yet.";
        if (s.getValue(ctx, "StartAt") != null && !text(s, "Frequency").isEmpty())
            schedule = "Reviews " + RankingRules.frequencyLabel(text(s, "Frequency")) + ", first review " + fmt(at(s, "StartAt")) + " UTC. Next review: " + fmt(nextReviewOf(s)) + ". Run automatically: "
                + (bool(s, "AutoRun") ? "yes" : "no, use Run review now") + (s.getValue(ctx, "LastReviewAt") == null ? "." : ". Last review: " + fmt(at(s, "LastReviewAt")) + ".")
                + (draft ? " Test values exist: select the upcoming review and use Simulate test values." : "");
        form.setValue(ctx, "Schedule", schedule);
        Map<String, Integer> perLevel = new HashMap<>(); Map<Long, IMendixObject> discs = new HashMap<>(); Map<String, Integer> perDiscipline = new HashMap<>();
        List<IMendixObject> disciplines = children("RankingDiscipline", "RankingDiscipline_Scheme", s); disciplines.sort(Comparator.comparingLong(d -> num(d, "Sequence")));
        for (IMendixObject d : disciplines) discs.put(d.getId().toLong(), d);
        List<IMendixObject> players = children("PlayerRanking", "PlayerRanking_Scheme", s);
        for (IMendixObject pr : players) { perLevel.merge(String.valueOf(num(pr, "Level")), 1, Integer::sum); perDiscipline.merge(String.valueOf(((IMendixIdentifier)pr.getValue(ctx, M + "PlayerRanking_Discipline")).toLong()), 1, Integer::sum); }
        // levels: edited in place; shows the test values when there are any, else the current values
        List<IMendixObject> live = levels(s, false), shown = draft ? levels(s, true) : live;
        Map<Integer, String> names = levelNames(s);
        for (IMendixObject l : shown) row(form, "Level", "L:" + id(l), String.valueOf(num(l, "Level")), plain(dec(l, "WinPoints")), num(l, "Level") == 1 ? "0" : plain(dec(l, "PromoteAt")),
            num(l, "Level") == shown.size() ? "0" : plain(dec(l, "DemoteBelow")), String.valueOf(perLevel.getOrDefault(String.valueOf(num(l, "Level")), 0)),
            draft ? levelStatus(l, live) : bool(l, "PointsPublished") ? "Current (published points, sample thresholds)" : "Current", levelName(l));
        for (IMendixObject d : disciplines) row(form, "Discipline", "D:" + id(d), text(d, "Code"), text(d, "Name"), text(d, "SlotKey"), String.valueOf(num(d, "Players")), String.valueOf(num(d, "BonusThreshold")),
            bool(d, "Active") ? "Yes" : "No", String.valueOf(perDiscipline.getOrDefault(id(d), 0)));
        // review log: the upcoming review first, then the latest reviews, or every review inside the chosen dates
        if (s.getValue(ctx, "StartAt") != null && !text(s, "Frequency").isEmpty()) row(form, "Review", "U:", fmt(nextReviewOf(s)), "Upcoming review", "", "", "", "", "Not run yet");
        List<IMendixObject> reviews = children("RankingMoment", "RankingMoment_Scheme", s); reviews.sort(Comparator.comparing((IMendixObject m) -> (Date)m.getValue(ctx, "DueOn")).reversed());
        String from = text(form, "LogFrom"), to = text(form, "LogTo"); int limit = (int)Math.max(5, num(form, "LogLimit")); boolean ranged = !from.isEmpty() || !to.isEmpty(); int shownReviews = 0;
        for (IMendixObject m : reviews) {
            LocalDate d = ((Date)m.getValue(ctx, "DueOn")).toInstant().atZone(ZoneOffset.UTC).toLocalDate();
            if (!from.isEmpty() && d.isBefore(LocalDate.parse(from))) continue; if (!to.isEmpty() && d.isAfter(LocalDate.parse(to))) continue;
            if (!ranged && shownReviews >= limit) break; if (shownReviews >= 500) break; shownReviews++;
            long evaluated = num(m, "Evaluated"), up = num(m, "Promoted"), down = num(m, "Demoted");
            String type = text(m, "Source").equals("Simulation") ? "Simulation, " + (TEST.equals(text(m, "Basis")) ? "test values" : "current values") + " (not applied)" : text(m, "Source") + " review";
            row(form, "Review", "M:" + id(m), fmt(at(m, "DueOn")), type, String.valueOf(evaluated), String.valueOf(up), String.valueOf(down), String.valueOf(evaluated - up - down), text(m, "AppliedBy"));
        }
        Map<Long, LocalDate> latest = new HashMap<>();
        for (IMendixObject c : query("//MyFirstModule.RankingChange[MyFirstModule.RankingChange_Scheme='" + s.getId().toLong() + "'][State='Applied']")) {
            IMendixIdentifier pi = (IMendixIdentifier)c.getValue(ctx, M + "RankingChange_PlayerRanking"); if (pi == null) continue;
            latest.merge(pi.toLong(), ((Date)c.getValue(ctx, "EffectiveOn")).toInstant().atZone(ZoneOffset.UTC).toLocalDate(), (a, b) -> a.isAfter(b) ? a : b);
        }
        players.sort(Comparator.comparingLong((IMendixObject pr) -> num(pr, "Level")).thenComparing(pr -> text(pr, "PlayerName")));
        for (IMendixObject pr : players) row(form, "Player", "P:" + id(pr), text(pr, "PlayerName"), text(discs.get(((IMendixIdentifier)pr.getValue(ctx, M + "PlayerRanking_Discipline")).toLong()), "Name"),
            nameOf(names, num(pr, "Level")), plain(dec(pr, "Average")), String.valueOf(num(pr, "Matches")), String.valueOf(num(pr, "BonusCounter")), latest.containsKey(pr.getId().toLong()) ? latest.get(pr.getId().toLong()).toString() : "");
        Map<Long, IMendixObject> byId = new HashMap<>(); for (IMendixObject pr : players) byId.put(pr.getId().toLong(), pr);
        Map<String, String> basisOf = new HashMap<>(); for (IMendixObject m : reviews) basisOf.put(id(m), TEST.equals(text(m, "Basis")) ? "test values" : "current values");
        List<IMendixObject> changes = query("//MyFirstModule.RankingChange[MyFirstModule.RankingChange_Scheme='" + s.getId().toLong() + "'][Kind!='Seed']");
        changes.sort(Comparator.comparing((IMendixObject c) -> (Date)c.getValue(ctx, "EffectiveOn")).reversed().thenComparing(c -> text(c, "State")));
        for (IMendixObject c : changes.subList(0, Math.min(500, changes.size()))) {
            IMendixObject pr = byId.get(((IMendixIdentifier)c.getValue(ctx, M + "RankingChange_PlayerRanking")).toLong());
            IMendixIdentifier mi = (IMendixIdentifier)c.getValue(ctx, M + "RankingChange_Moment"); String basis = mi == null ? null : basisOf.get(String.valueOf(mi.toLong()));
            row(form, "Change", "C:" + id(c), ((Date)c.getValue(ctx, "EffectiveOn")).toInstant().atZone(ZoneOffset.UTC).toLocalDate().toString(), pr == null ? "" : text(pr, "PlayerName"),
                pr == null ? "" : text(discs.get(((IMendixIdentifier)pr.getValue(ctx, M + "PlayerRanking_Discipline")).toLong()), "Name"), (num(c, "FromLevel") == 0 ? "" : nameOf(names, num(c, "FromLevel"))) + " → " + nameOf(names, num(c, "ToLevel")), text(c, "Kind"),
                text(c, "State") + ("Simulated".equals(text(c, "State")) && basis != null ? " (" + basis + ")" : ""),
                plain(dec(c, "Average")) + " / " + num(c, "Matches") + " matches", text(c, "Reason") + (text(c, "Comment").isEmpty() ? "" : " Comment: " + text(c, "Comment")));
        }
    }

    /** Player page: current levels, the level changes and, per discipline, the calculation of the average as of today or as of the review it was opened from. */
    private void history(IMendixObject form) throws Exception {
        IMendixObject s = load("S:" + text(form, "SchemeKey")); require(manage(ref(s, "RankingScheme_Federation")), "You cannot manage this federation's rankings.");
        Context rc = text(form, "ReviewKey").isEmpty() ? null : context(s, text(form, "ReviewKey"));
        boolean test = rc != null && rc.test; LocalDate asOf = rc == null ? LocalDate.now(ZoneOffset.UTC) : rc.asOf.toLocalDate();
        Map<Long, IMendixObject> discs = new HashMap<>(); for (IMendixObject d : children("RankingDiscipline", "RankingDiscipline_Scheme", s)) discs.put(d.getId().toLong(), d);
        Map<Long, IMendixObject> mine = new LinkedHashMap<>();
        for (IMendixObject pr : children("PlayerRanking", "PlayerRanking_Scheme", s)) if (text(pr, "PlayerKey").equals(text(form, "PlayerKey"))) mine.put(pr.getId().toLong(), pr);
        for (IMendixObject pr : mine.values()) row(form, "Standing", "P:" + id(pr), text(discs.get(((IMendixIdentifier)pr.getValue(ctx, M + "PlayerRanking_Discipline")).toLong()), "Name"), nameOf(levelNames(s), num(pr, "Level")),
            plain(dec(pr, "Average")), String.valueOf(num(pr, "Matches")), String.valueOf(num(pr, "BonusCounter")));
        // calculation with the values of the review (current values, or the simulation's test values) and every other player's current level
        Map<String, History> history = gather(s); RankingRules.Table t = table(s, test); RankingRules.Params p = params(s, t.levels());
        Map<String, Integer> levelOf = new HashMap<>(); Map<Long, String> codeOf = new HashMap<>();
        for (IMendixObject d : discs.values()) codeOf.put(d.getId().toLong(), text(d, "Code"));
        for (IMendixObject pr : children("PlayerRanking", "PlayerRanking_Scheme", s)) levelOf.put(codeOf.get(((IMendixIdentifier)pr.getValue(ctx, M + "PlayerRanking_Discipline")).toLong()) + "|" + text(pr, "PlayerKey"), Math.min((int)num(pr, "Level"), p.levels()));
        for (IMendixObject pr : mine.values()) {
            IMendixObject disc = discs.get(((IMendixIdentifier)pr.getValue(ctx, M + "PlayerRanking_Discipline")).toLong()); String code = text(disc, "Code"); int level = Math.min((int)num(pr, "Level"), p.levels());
            History h = history.get(code + "|" + text(pr, "PlayerKey"));
            RankingRules.Computation c = RankingRules.compute(level, h == null ? new ArrayList<>() : toMatches(h, code, level, levelOf, p.levels()), asOf, p, t);
            for (RankingRules.Detail d : c.details())
                row(form, "Calc", "K:" + code, text(disc, "Name"), d.match().date().toString(), d.match().label(), d.match().won() ? "Won" : "Lost", plain(d.match().opponentLevel()), plain(d.points()), d.note());
            double up = t.promoteAt()[level], down = t.demoteBelow()[level];
            row(form, "Calc", "K:" + code + ":sum", text(disc, "Name") + ": average", "", "Total " + plain(c.total()) + " points ÷ " + plain(c.divisor()) + " (" + c.counted() + " counted match(es), at least " + p.minDivisor() + ")", "", "", plain(c.average()),
                "Level " + nameOf(levelNames(s), level) + (level > 1 ? ": promoted from an average of " + plain(up) : "") + (level < p.levels() ? "; demoted below " + plain(down) + " (needs at least " + p.inactiveMinMatches() + " matches)" : "") + ". Window of " + p.windowWeeks() + " weeks until " + asOf + ".");
        }
        List<IMendixObject> changes = new ArrayList<>();
        for (IMendixObject c : query("//MyFirstModule.RankingChange[MyFirstModule.RankingChange_Scheme='" + s.getId().toLong() + "']"))
            if (mine.containsKey(((IMendixIdentifier)c.getValue(ctx, M + "RankingChange_PlayerRanking")).toLong())) changes.add(c);
        changes.sort(Comparator.comparing((IMendixObject c) -> (Date)c.getValue(ctx, "EffectiveOn")).reversed());
        for (IMendixObject c : changes) { IMendixObject pr = mine.get(((IMendixIdentifier)c.getValue(ctx, M + "RankingChange_PlayerRanking")).toLong());
            row(form, "History", "C:" + id(c), ((Date)c.getValue(ctx, "EffectiveOn")).toInstant().atZone(ZoneOffset.UTC).toLocalDate().toString(), text(discs.get(((IMendixIdentifier)pr.getValue(ctx, M + "PlayerRanking_Discipline")).toLong()), "Name"),
                (num(c, "FromLevel") == 0 ? "" : nameOf(levelNames(s), num(c, "FromLevel"))) + " → " + nameOf(levelNames(s), num(c, "ToLevel")), text(c, "Kind"), text(c, "State"), text(c, "Reason") + (text(c, "Comment").isEmpty() ? "" : " Comment: " + text(c, "Comment")), text(c, "Actor")); }
        form.setValue(ctx, "Info", "Level history of " + text(form, "Title") + " in " + text(s, "Name") + ". Simulated rows were never applied. " + (rc == null
            ? "The calculation uses the current values, opponent levels from the match sheets (else the opponent's current level), and today's date."
            : "The calculation follows the review: " + rc.description + " Opponent levels come from the match sheets (else the opponent's current level)."));
    }

    // ------------------------------------------------------------------ selection and flags

    private void inspect(IMendixObject form, IMendixObject state, String kind, String ids) throws Exception {
        List<String> keys = new ArrayList<>();
        for (String raw : ids.split(",")) {
            if (raw.isBlank()) continue;
            IMendixObject row = Core.retrieveId(ctx, Core.createMendixIdentifier(Long.parseLong(raw.trim())));
            if (row != null && row.getType().equals(M + "RankingRow") && text(row, "Kind").equals(kind)) keys.add(text(row, "RecordKey"));
        }
        Map<String, String> sel = selection(form); sel.put(kind, String.join(",", keys)); form.setValue(ctx, "Selected", encode(sel)); flags(form, state);
    }

    private Map<String, String> selection(IMendixObject form) {
        Map<String, String> sel = new LinkedHashMap<>();
        for (String part : text(form, "Selected").split(";")) { int i = part.indexOf('='); if (i > 0) sel.put(part.substring(0, i), part.substring(i + 1)); }
        return sel;
    }
    private static String encode(Map<String, String> sel) { StringBuilder b = new StringBuilder(); sel.forEach((k, v) -> { if (!v.isEmpty()) b.append(k).append('=').append(v).append(';'); }); return b.toString(); }
    String selected(IMendixObject form, String kind) { List<String> all = selectedAll(form, kind); return all.size() == 1 ? all.get(0) : null; }
    private List<String> selectedAll(IMendixObject form, String kind) { String v = selection(form).getOrDefault(kind, ""); return v.isEmpty() ? List.of() : Arrays.asList(v.split(",", -1)); }
    String requireKey(IMendixObject form, String kind, String prefix) { return requireKeyOf(form, kind, prefix); }
    private String requireKeyOf(IMendixObject form, String kind, String... prefixes) {
        String key = selected(form, kind); boolean ok = false; if (key != null) for (String p : prefixes) ok |= key.startsWith(p);
        require(ok, "Select exactly one row first."); return key;
    }

    void flags(IMendixObject form, IMendixObject state) throws Exception {
        for (String name : new String[]{"SchemeCreate", "SchemeView", "SchemeEdit", "SchemeDelete", "LevelCreate", "LevelSimulate", "LevelDelete", "LevelApprove", "LevelDiscard", "ReviewSchedule", "ReviewSimulate", "ReviewSimulateTest",
            "ReviewView", "ReviewRun", "ReviewFilter", "ReviewMore", "DisciplineCreate", "DisciplineEdit", "DisciplineDeactivate", "DisciplineActivate", "PlayerView", "PlayerForce", "PlayerSeed", "ChangeClear", "LabDataset", "LabScenario", "LabPlayer", "LabLevelRemove", "LabResult"}) set(state, name, false);
        state.setValue(ctx, "Message", "");
        if (text(form, "Mode").startsWith("Lab")) {
            set(state, "LabDataset", selected(form, "Dataset") != null); set(state, "LabScenario", selected(form, "Scenario") != null); set(state, "LabPlayer", selected(form, "LabPlayer") != null);
            set(state, "LabLevelRemove", selected(form, "LabLevel") != null); set(state, "LabResult", !text(form, "Schedule").startsWith("No simulation"));
            return;
        }
        if (!text(form, "ReviewKey").isEmpty()) { set(state, "PlayerView", selected(form, "Outcome") != null); return; }
        boolean can = bool(form, "CanManage"), has = bool(form, "HasScheme");
        String scheme = selected(form, "Scheme"), level = selected(form, "Level"), player = selected(form, "Player"), discipline = selected(form, "Discipline"), review = selected(form, "Review");
        set(state, "SchemeCreate", can && !has && scheme != null && scheme.startsWith("F:")); set(state, "SchemeView", can && !has && scheme != null && scheme.startsWith("S:"));
        set(state, "SchemeEdit", can && !has && scheme != null && scheme.startsWith("S:")); set(state, "SchemeDelete", can && !has && scheme != null && scheme.startsWith("S:"));
        if (!(can && has)) return;
        IMendixObject s = load("S:" + text(form, "SchemeKey")); boolean draft = hasDraft(s);
        boolean lowest = false;
        if (level != null) { try { IMendixObject lv = load(level); lowest = num(lv, "Level") == (draft ? levels(s, true) : levels(s, false)).size() && num(lv, "Level") > 2; } catch (RuntimeException gone) { lowest = false; } }
        int active = 0, inactive = 0;
        for (String key : selectedAll(form, "Discipline")) { try { if (bool(load(key), "Active")) active++; else inactive++; } catch (RuntimeException gone) { /* selection changed */ } }
        boolean simulated = !query("//MyFirstModule.RankingChange[MyFirstModule.RankingChange_Scheme='" + text(form, "SchemeKey") + "'][State='Simulated']").isEmpty();
        boolean upcoming = review != null && review.startsWith("U:"), simulation = false;
        if (review != null && review.startsWith("M:")) { try { simulation = "Simulated".equals(text(load(review), "Status")); } catch (RuntimeException gone) { simulation = false; } }
        set(state, "LevelCreate", true); set(state, "LevelDelete", lowest); set(state, "LevelSimulate", draft); set(state, "LevelApprove", draft); set(state, "LevelDiscard", draft);
        set(state, "ReviewSchedule", true); set(state, "ReviewFilter", true); set(state, "ReviewMore", true); set(state, "ReviewView", review != null);
        set(state, "ReviewSimulate", upcoming || simulation); set(state, "ReviewSimulateTest", draft && (upcoming || simulation)); set(state, "ReviewRun", upcoming);
        set(state, "DisciplineCreate", true); set(state, "DisciplineEdit", discipline != null); set(state, "DisciplineDeactivate", active > 0 && inactive == 0); set(state, "DisciplineActivate", inactive > 0 && active == 0);
        set(state, "PlayerView", player != null); set(state, "PlayerForce", player != null); set(state, "PlayerSeed", true); set(state, "ChangeClear", simulated);
    }
    private void set(IMendixObject state, String name, boolean value) { state.setValue(ctx, name, value); }

    // ------------------------------------------------------------------ permission and data helpers

    private boolean manage(IMendixObject federation) throws Exception {
        if (federation == null) return false; if (user == null || admin) return true;
        return Boolean.TRUE.equals(Core.microflowCall(M + "SUB_Competition_FederationPermission").withParam("Federation", federation).withParam("Operation", "Manage").execute(user));
    }
    IMendixObject requireScheme(String key) throws Exception {
        require(key != null && !key.isEmpty(), "Open a ranking scheme first.");
        IMendixObject s = load("S:" + key); require(manage(ref(s, "RankingScheme_Federation")), "You cannot manage this federation's rankings."); return s;
    }
    private IMendixObject scheme(IMendixObject form, String kind) throws Exception { String k = selected(form, kind); require(k != null && k.startsWith("S:"), "Select one ranking scheme."); return requireScheme(k.substring(2)); }
    private IMendixObject federationOf(String key) throws Exception { return key == null || !key.startsWith("F:") ? null : Core.retrieveId(ctx, Core.createMendixIdentifier(Long.parseLong(key.substring(2)))); }
    private List<IMendixObject> schemes(IMendixObject fed) throws Exception { return query("//MyFirstModule.RankingScheme[MyFirstModule.RankingScheme_Federation='" + fed.getId().toLong() + "']"); }
    IMendixObject load(String key) throws Exception {
        String raw = key.substring(key.indexOf(':') + 1); IMendixObject o = raw.matches("[0-9]+") ? Core.retrieveId(ctx, Core.createMendixIdentifier(Long.parseLong(raw))) : null;
        if (o == null || !Arrays.asList(TYPES).contains(o.getType())) throw new IllegalStateException("The selection changed. Refresh the page.");
        return o;
    }
    List<IMendixObject> children(String type, String assoc, IMendixObject parent) throws Exception { return query("//MyFirstModule." + type + "[MyFirstModule." + assoc + "='" + parent.getId().toLong() + "']"); }
    private List<IMendixObject> query(String xpath) throws Exception { return Core.createXPathQuery(xpath).execute(ctx); }
    private IMendixObject ref(IMendixObject o, String assoc) throws Exception { IMendixIdentifier i = (IMendixIdentifier)o.getValue(ctx, M + assoc); return i == null ? null : Core.retrieveId(ctx, i); }
    IMendixObject make(String type, Object... values) throws Exception {
        IMendixObject o = Core.instantiate(ctx, M + type);
        for (int i = 0; i < values.length; i += 2) o.setValue(ctx, (String)values[i], values[i + 1]);
        Core.commitWithoutEvents(ctx, o); if (created != null) created.add(o); return o;
    }
    void link(IMendixObject o, String assoc, IMendixObject target) throws Exception { o.setValue(ctx, M + assoc, target.getId()); Core.commitWithoutEvents(ctx, o); }
    void commit(IMendixObject o) throws Exception { Core.commitWithoutEvents(ctx, o); }
    String text(IMendixObject o, String a) { Object v = o == null ? null : o.getValue(ctx, a); return v == null ? "" : v.toString(); }
    long num(IMendixObject o, String a) { Object v = o.getValue(ctx, a); return v instanceof Number ? ((Number)v).longValue() : 0; }
    double dec(IMendixObject o, String a) { Object v = o.getValue(ctx, a); return v instanceof Number ? ((Number)v).doubleValue() : 0; }
    boolean bool(IMendixObject o, String a) { return Boolean.TRUE.equals(o.getValue(ctx, a)); }
    LocalDate day(IMendixObject o, String a) { return ((Date)o.getValue(ctx, a)).toInstant().atZone(ZoneOffset.UTC).toLocalDate(); }
    private LocalDateTime at(IMendixObject o, String a) { return LocalDateTime.ofInstant(((Date)o.getValue(ctx, a)).toInstant(), ZoneOffset.UTC); }
    String id(IMendixObject o) { return String.valueOf(o.getId().toLong()); }
    static Date utc(LocalDate d) { return Date.from(d.atStartOfDay(ZoneOffset.UTC).toInstant()); }
    private static Date utc(LocalDateTime d) { return Date.from(d.toInstant(ZoneOffset.UTC)); }
    private static String fmt(LocalDateTime d) { return String.format("%s %02d:%02d", d.toLocalDate(), d.getHour(), d.getMinute()); }
    private static BigDecimal bd(double v) { return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP); }
    static String plain(double v) { return v == Math.rint(v) ? String.valueOf((long)v) : String.format(Locale.ROOT, "%.1f", v); }
    static void require(boolean ok, String message) { if (!ok) throw new IllegalArgumentException(message); }
    static double number(String v, String what) { try { return Double.parseDouble(v.trim().replace(',', '.')); } catch (RuntimeException e) { throw new IllegalArgumentException("Enter a number for the " + what + "."); } }
    private static LocalDate date(String v) { try { return LocalDate.parse(v); } catch (RuntimeException e) { throw new IllegalArgumentException("Enter the date as yyyy-mm-dd."); } }
    private static LocalTime time(String v) { try { return LocalTime.parse(v); } catch (RuntimeException e) { throw new IllegalArgumentException("Enter the hour as hh:mm."); } }
}
