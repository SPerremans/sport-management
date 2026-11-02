package myfirstmodule;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Sport-neutral ranking calculation; no Mendix types, so it is unit-testable and can replay imported game data offline.
 * A level keeps an internal position (1 is the highest) and has a free display name per scheme ("AA", "C+15", "12"). Calculation methods:
 * AveragePoints (Badminton Vlaanderen style: win points by opponent level, averaged), BestResults (BWF, ATP and Belgian tennis style: the best N results added up),
 * WinBalance (FFT style: wins minus weighted losses) and EloRating (FIDE, USATT and German TTR style: a running rating).
 * The score becomes a level by promotion and demotion thresholds per level, or by fixed shares of the players per level (a pyramid, as in Belgian tennis).
 * A replay never looks ahead: a game is judged with the levels known before it was played.
 */
final class RankingEngine {
    private RankingEngine() {}

    static final String AVERAGE = "AveragePoints", BEST = "BestResults", BALANCE = "WinBalance", ELO = "EloRating";
    static final String THRESHOLDS = "Thresholds", SHARES = "Shares";

    /** Everything a federation can configure. Arrays are indexed by level position (index 0 unused). */
    static final class Config {
        String method = AVERAGE, placement = THRESHOLDS, frequency = "FirstMondayMonthly";
        RankingRules.Params params = RankingRules.Params.badmintonVlaanderen();
        RankingRules.Table table = RankingRules.defaultTable(12);
        String[] names; double[] shares;
        int maxUp = 1, maxDown = 1, bestCount = 6, eloNewGames = 30;
        double eloK = 20, eloKNew = 40, eloScale = 400, eloStart = 1000, eloStep = 100, lossEqual = 1, lossOneBelow = 2, lossTwoBelow = 5;
        boolean countWalkovers = false, startFromPublished = true;
        Map<String, Integer> bonusThresholds = new HashMap<>();
        Set<String> disciplines = new HashSet<>();

        int levels() { return table.levels(); }
        String name(int level) { return names != null && level >= 1 && level < names.length && names[level] != null && !names[level].isBlank() ? names[level] : String.valueOf(level); }
        boolean ranks(String discipline) { return disciplines.isEmpty() || disciplines.contains(discipline); }
    }

    /** One game as imported: winners and losers with the levels printed on the sheet (null when the sheet shows none). flag: "" played, R retirement, W walkover. */
    record Game(LocalDate date, String discipline, List<String> winners, List<String> losers, List<Integer> winnerLevels, List<Integer> loserLevels, String flag, String event) {}

    /** One player in one discipline during a replay. */
    static final class Player {
        final String key, name, discipline; int startLevel, level, moves, flips, games, lastDirection, lastMoveReview = -99, inWindow, counted; double rating, score;
        Integer lastPublished; LocalDate lastActive, lastChange = LocalDate.MIN; String reason = "", kind = "None";
        final List<RankingRules.Match> matches = new ArrayList<>(); final StringBuilder path = new StringBuilder();
        Player(String key, String name, String discipline) { this.key = key; this.name = name; this.discipline = discipline; }
    }

    /** What a replay shows: per review, per level, per favourite gap and per player. */
    static final class Result {
        int games, skippedWalkovers, players, moves, promoted, demoted, flips, reviews; LocalDate first, last;
        int decided, favouriteWon, equalGames; double brier; int brierGames;
        int publishedSeen, publishedExact, publishedWithinOne; double publishedDistance;
        final List<Review> timeline = new ArrayList<>(); final Map<Integer, int[]> gaps = new TreeMap<>();
        final List<Player> table = new ArrayList<>(); int[] startCount, endCount; final List<String> warnings = new ArrayList<>();
        double favouriteRate() { return decided == 0 ? 0 : 100.0 * favouriteWon / decided; }
        double brierScore() { return brierGames == 0 ? 0 : brier / brierGames; }
        double movesPerPlayerYear() { double years = first == null ? 0 : Math.max(0.25, ChronoUnit.DAYS.between(first, last) / 365.25); return players == 0 ? 0 : moves / (players * Math.max(years, 0.25)); }
        double flipRate() { return moves == 0 ? 0 : 100.0 * flips / moves; }
        double exactRate() { return publishedSeen == 0 ? -1 : 100.0 * publishedExact / publishedSeen; }
        double withinOneRate() { return publishedSeen == 0 ? -1 : 100.0 * publishedWithinOne / publishedSeen; }
        double meanDistance() { return publishedSeen == 0 ? -1 : publishedDistance / publishedSeen; }
    }

    record Review(LocalDate date, int active, int promoted, int demoted, double favouriteRate) {}

    // ------------------------------------------------------------------ scoring

    /** The rating change of one game: K times the difference between the result and the expected result. */
    static double eloDelta(double own, double opponent, boolean won, double k, double scale) {
        double expected = 1.0 / (1.0 + Math.pow(10, (opponent - own) / scale));
        return k * ((won ? 1 : 0) - expected);
    }

    /** The player's score at a moment: average points, sum of the best results, the win balance or the running rating. */
    static double score(Player p, LocalDate moment, Config c) {
        RankingRules.Computation window = RankingRules.compute(p.level, p.matches, moment, c.params, c.table);
        p.inWindow = window.inWindow(); p.counted = window.counted();
        switch (c.method) {
            case BEST: {
                List<Double> points = new ArrayList<>();
                for (RankingRules.Detail d : window.details()) if (d.match().won()) points.add(c.table.points(d.match().opponentLevel()));
                points.sort(Comparator.reverseOrder());
                double total = 0; for (int i = 0; i < Math.min(c.bestCount, points.size()); i++) total += points.get(i);
                return total; }
            case BALANCE: {
                double total = 0;
                for (RankingRules.Detail d : window.details()) {
                    RankingRules.Match m = d.match();
                    if (m.won()) { total++; continue; }
                    double below = m.opponentLevel() - m.ownLevel();
                    total -= below >= 2 ? c.lossTwoBelow : below >= 1 ? c.lossOneBelow : below > -1 ? c.lossEqual : 0;
                }
                return total; }
            case ELO: return p.rating;
            default: return window.average();
        }
    }

    private static int[] shareBounds(Config c) {
        // bounds[level] = cumulative share of players that fits at this level or higher, in promille
        int[] bounds = new int[c.levels() + 1]; double total = 0, sum = 0;
        for (int l = 1; l <= c.levels(); l++) total += c.shares == null || l >= c.shares.length ? 0 : c.shares[l];
        for (int l = 1; l <= c.levels(); l++) { sum += total <= 0 ? 100.0 / c.levels() : c.shares == null || l >= c.shares.length ? 0 : c.shares[l] * 100.0 / total; bounds[l] = (int)Math.round(sum * 10); }
        bounds[c.levels()] = 1000;
        return bounds;
    }

    /**
     * One review of all given players: updates their level, score and reason. Levels of the other disciplines of a person limit the move by the maximum level difference.
     * Returns promoted and demoted.
     */
    static int[] review(Collection<Player> players, LocalDate moment, Config c) {
        Map<String, List<Integer>> byPerson = new HashMap<>();
        for (Player p : players) byPerson.computeIfAbsent(p.key, k -> new ArrayList<>()).add(p.level);
        int up = 0, down = 0;
        Map<Player, Integer> shareTargets = c.placement.equals(SHARES) ? shareTargets(players, moment, c) : Map.of();
        for (Player p : players) {
            int from = p.level; List<Integer> others = new ArrayList<>(byPerson.get(p.key)); others.remove(Integer.valueOf(from));
            decide(p, moment, others, shareTargets.get(p), c);
            if (p.level < from) up++; else if (p.level > from) down++;
        }
        return new int[]{up, down};
    }

    private static Map<Player, Integer> shareTargets(Collection<Player> players, LocalDate moment, Config c) {
        Map<String, List<Player>> pools = new HashMap<>();
        for (Player p : players) {
            p.score = score(p, moment, c);
            if (p.inWindow >= Math.max(1, c.params.inactiveMinMatches())) pools.computeIfAbsent(p.discipline, k -> new ArrayList<>()).add(p);
        }
        int[] bounds = shareBounds(c); Map<Player, Integer> targets = new HashMap<>();
        for (List<Player> pool : pools.values()) {
            pool.sort(Comparator.comparingDouble((Player p) -> -p.score).thenComparing(p -> p.name));
            for (int i = 0; i < pool.size(); i++) {
                int promille = (int)(1000.0 * (i + 1) / pool.size()), level = 1;
                while (level < c.levels() && promille > bounds[level]) level++;
                targets.put(pool.get(i), level);
            }
        }
        return targets;
    }

    private static void decide(Player p, LocalDate moment, List<Integer> others, Integer shareTarget, Config c) {
        RankingRules.Params rules = c.params; RankingRules.Table t = c.table; int threshold = c.bonusThresholds.getOrDefault(p.discipline, 0);
        p.score = score(p, moment, c); p.kind = "None"; p.reason = "No change: score " + RankingRules.fmt(p.score) + " over " + p.counted + " match(es).";
        boolean active = p.inWindow >= rules.inactiveMinMatches();
        if (!active && p.lastActive != null && rules.inactiveDrop() > 0 && ChronoUnit.WEEKS.between(p.lastActive, moment) >= rules.inactiveWeeks()) {
            int target = Math.min(c.levels(), p.level + rules.inactiveDrop());
            if (target != p.level) { move(p, target, moment, "Inactivity", "Fewer than " + rules.inactiveMinMatches() + " matches in " + rules.windowWeeks() + " weeks for " + rules.inactiveWeeks() + " weeks: down to " + c.name(target) + "."); }
            p.lastActive = moment; return;
        }
        if (active) p.lastActive = moment;
        if (shareTarget != null) {
            int target = limited(p.level, shareTarget, c);
            if (target != p.level && RankingRules.spreadOk(target, others, rules.maxSpread())) move(p, target, moment, "Evaluation", "Score " + RankingRules.fmt(p.score) + " places the player in the share of " + c.name(target) + ".");
            return;
        }
        if (c.method.equals(AVERAGE)) {
            // the published average rule, repeated when more than one level per review is allowed
            int steps = Math.max(1, Math.max(c.maxUp, c.maxDown)); LocalDate since = p.lastChange; int moved = 0;
            for (int i = 0; i < steps; i++) {
                RankingRules.Result r = RankingRules.evaluate(p.level, p.matches, moment, p.lastActive == null ? moment : p.lastActive, since, threshold, others, rules, t);
                if (r.level() == p.level) { if (i == 0) p.reason = r.reason(); break; }
                boolean goingUp = r.level() < p.level;
                if (moved > 0 && (goingUp ? moved >= c.maxUp : moved >= c.maxDown)) break;
                move(p, r.level(), moment, r.kind(), r.reason()); moved++;
                if (r.kind().equals("Bonus")) break;
            }
            return;
        }
        int target = p.level;
        for (int i = 0; i < Math.max(1, c.maxUp) && target > 1 && t.promoteAt()[target] != 0 && p.score >= t.promoteAt()[target]; i++) target--;
        if (target == p.level && active) for (int i = 0; i < Math.max(1, c.maxDown) && target < c.levels() && p.score < t.demoteBelow()[target]; i++) target++;
        if (target == p.level) return;
        if (!RankingRules.spreadOk(target, others, rules.maxSpread())) { p.reason = "Score " + RankingRules.fmt(p.score) + " qualifies for " + c.name(target) + " but the maximum level difference between disciplines blocks it."; return; }
        move(p, target, moment, "Evaluation", "Score " + RankingRules.fmt(p.score) + (target < p.level ? " reaches the promotion score" : " is under the demotion score") + " of " + c.name(p.level) + ": now " + c.name(target) + ".");
    }

    private static int limited(int from, int target, Config c) {
        if (target < from) return c.maxUp > 0 ? Math.max(target, from - c.maxUp) : target;
        return c.maxDown > 0 ? Math.min(target, from + c.maxDown) : target;
    }

    private static void move(Player p, int target, LocalDate moment, String kind, String reason) {
        p.level = target; p.kind = kind; p.reason = reason; p.lastChange = moment;
    }

    // ------------------------------------------------------------------ replay

    /** Starts every player at the first printed level (when allowed), else at the lowest level, and replays the games with reviews on the schedule. */
    static Result replay(List<Game> games, Config c) {
        Result out = new Result(); List<Game> sorted = new ArrayList<>();
        for (Game g : games) if (c.ranks(g.discipline())) { if (!c.countWalkovers && "W".equals(g.flag())) out.skippedWalkovers++; else sorted.add(g); }
        sorted.sort(Comparator.comparing(Game::date));
        out.startCount = new int[c.levels() + 1]; out.endCount = new int[c.levels() + 1];
        if (sorted.isEmpty()) { out.warnings.add("There are no games to replay for the chosen disciplines."); return out; }
        out.first = sorted.get(0).date(); out.last = sorted.get(sorted.size() - 1).date(); out.games = sorted.size();
        Map<String, Player> players = new LinkedHashMap<>(); int[][] gapCounts = new int[64][2];
        List<LocalDate> schedule = schedule(out.first, out.last, c.frequency); int next = 0; int reviewIndex = 0;
        for (Game g : sorted) {
            while (next < schedule.size() && !schedule.get(next).isAfter(g.date())) { runReview(players.values(), schedule.get(next), c, out, reviewIndex++); next++; }
            play(g, players, c, out, gapCounts);
        }
        while (next < schedule.size()) { runReview(players.values(), schedule.get(next), c, out, reviewIndex++); next++; }
        out.reviews = schedule.size(); out.players = players.size(); out.table.addAll(players.values());
        for (Player p : players.values()) {
            out.startCount[Math.min(p.startLevel, c.levels())]++; out.endCount[Math.min(p.level, c.levels())]++;
        }
        warnings(out, c);
        return out;
    }

    /** Review dates inside the data: never before the data has had one full review period, always one at the end. */
    static List<LocalDate> schedule(LocalDate first, LocalDate last, String frequency) {
        List<LocalDate> dates = new ArrayList<>(); java.time.LocalDateTime start = first.plusMonths(1).withDayOfMonth(1).atTime(9, 0);
        for (int n = 0; n < 5000; n++) {
            LocalDate d = RankingRules.occurrence(frequency, start, n).toLocalDate();
            if (d.isBefore(first)) continue;
            if (d.isAfter(last)) break;
            dates.add(d);
        }
        if (dates.isEmpty() || dates.get(dates.size() - 1).isBefore(last)) dates.add(last);
        return dates;
    }

    private static Player player(Map<String, Player> players, String rawName, String discipline, Integer printed, Config c) {
        String key = RankingRules.nameKey(rawName), full = discipline + "|" + key;
        Player p = players.get(full);
        if (p == null) {
            p = new Player(key, RankingRules.cleanName(rawName), discipline);
            p.startLevel = c.startFromPublished && printed != null ? Math.max(1, Math.min(c.levels(), printed)) : c.levels();
            p.level = p.startLevel; p.rating = c.eloStart + c.eloStep * (c.levels() - p.startLevel);
            p.path.append(c.name(p.level)); players.put(full, p);
        }
        return p;
    }

    private static void play(Game g, Map<String, Player> players, Config c, Result out, int[][] gapCounts) {
        List<Player> win = new ArrayList<>(), lose = new ArrayList<>();
        for (int i = 0; i < g.winners().size(); i++) win.add(player(players, g.winners().get(i), g.discipline(), at(g.winnerLevels(), i), c));
        for (int i = 0; i < g.losers().size(); i++) lose.add(player(players, g.losers().get(i), g.discipline(), at(g.loserLevels(), i), c));
        double winLevel = meanLevel(win), loseLevel = meanLevel(lose), winRating = meanRating(win), loseRating = meanRating(lose);
        // judged with the levels known before the game
        int gap = (int)Math.min(gapCounts.length - 1, Math.round(Math.abs(winLevel - loseLevel) * 2));
        if (gap == 0) { out.equalGames++; out.brier += 0.25; out.brierGames++; }
        else {
            boolean favourite = winLevel < loseLevel; int[] seen = gapCounts[gap];
            double expected = seen[1] < 20 ? 0.5 + Math.min(0.45, gap * 0.05) : (seen[0] + 0.5) / (seen[1] + 1.0);
            out.brier += Math.pow((favourite ? 1 : 0) - expected, 2); out.brierGames++;
            seen[1]++; if (favourite) seen[0]++;
            out.decided++; if (favourite) out.favouriteWon++;
            int[] row = out.gaps.computeIfAbsent(gap, k -> new int[2]); row[1]++; if (favourite) row[0]++;
        }
        compareWithSheet(win, g.winnerLevels(), out, c); compareWithSheet(lose, g.loserLevels(), out, c);
        for (int side = 0; side < 2; side++) {
            List<Player> mine = side == 0 ? win : lose, theirs = side == 0 ? lose : win; boolean won = side == 0; double opponent = side == 0 ? loseLevel : winLevel, opponentRating = side == 0 ? loseRating : winRating;
            for (Player p : mine) {
                p.games++; p.lastActive = g.date();
                p.matches.add(new RankingRules.Match(g.date(), won, opponent, p.level, String.join(" & ", side == 0 ? g.losers() : g.winners())));
                double k = p.games <= c.eloNewGames ? c.eloKNew : c.eloK;
                p.rating += eloDelta(p.rating, opponentRating, won, k, c.eloScale);
            }
        }
    }

    private static Integer at(List<Integer> list, int i) { return list != null && i < list.size() ? list.get(i) : null; }
    private static double meanLevel(List<Player> side) { double s = 0; for (Player p : side) s += p.level; return s / side.size(); }
    private static double meanRating(List<Player> side) { double s = 0; for (Player p : side) s += p.rating; return s / side.size(); }

    /** The level this configuration gives at that moment against the level printed on the sheet, once the player has been through at least one change of data. */
    private static void compareWithSheet(List<Player> side, List<Integer> printed, Result out, Config c) {
        for (int i = 0; i < side.size(); i++) {
            Integer level = at(printed, i); Player p = side.get(i);
            if (level == null) continue;
            p.lastPublished = level;
            if (p.games < 5) continue; // the start level comes from the sheet, so the first games prove nothing
            int distance = Math.abs(Math.min(level, c.levels()) - p.level);
            out.publishedSeen++; out.publishedDistance += distance; if (distance == 0) out.publishedExact++; if (distance <= 1) out.publishedWithinOne++;
        }
    }

    private static void runReview(Collection<Player> players, LocalDate date, Config c, Result out, int index) {
        Map<Player, Integer> before = new HashMap<>();
        for (Player p : players) before.put(p, p.level);
        int[] counts = review(players, date, c); int active = 0;
        for (Player p : players) {
            if (p.inWindow >= c.params.inactiveMinMatches()) active++;
            int from = before.get(p);
            if (p.level == from) continue;
            int direction = p.level < from ? -1 : 1;
            p.moves++; out.moves++; if (direction < 0) out.promoted++; else out.demoted++;
            if (p.lastDirection != 0 && p.lastDirection != direction && index - p.lastMoveReview <= 3) { p.flips++; out.flips++; }
            p.lastDirection = direction; p.lastMoveReview = index;
            if (p.path.length() < 120) p.path.append(" > ").append(c.name(p.level));
        }
        out.timeline.add(new Review(date, active, counts[0], counts[1], out.favouriteRate()));
    }

    /** What a reviewer should look at first. */
    private static void warnings(Result out, Config c) {
        int thin = 0;
        for (Player p : out.table) if (p.games < Math.max(c.params.inactiveMinMatches(), 3)) thin++;
        if (out.players > 0 && thin * 100 / out.players >= 30) out.warnings.add(thin * 100 / out.players + "% of the players have too few games to judge: their level mostly stays at the start level.");
        int empty = 0; for (int l = 1; l <= c.levels(); l++) if (out.endCount[l] == 0) empty++;
        if (empty > 0) out.warnings.add(empty + " level(s) end without any player: too many levels for this data, or a threshold that nobody reaches.");
        for (int l = 1; l <= c.levels(); l++) if (out.players > 0 && out.endCount[l] * 100 / out.players >= 50) out.warnings.add("Level " + c.name(l) + " ends with " + out.endCount[l] * 100 / out.players + "% of the players: the levels barely separate them.");
        if (out.decided >= 50 && out.favouriteRate() < 55) out.warnings.add("The better-levelled side won only " + Math.round(out.favouriteRate()) + "% of the games: the levels hardly predict results.");
        if (out.movesPerPlayerYear() > 3) out.warnings.add("Players change level " + RankingRules.fmt(out.movesPerPlayerYear()) + " times per year on average: a ranking this restless is hard to explain.");
        if (out.moves >= 50 && out.flipRate() > 25) out.warnings.add(Math.round(out.flipRate()) + "% of the moves undo a recent move: widen the gap between the promotion and demotion scores.");
        if (out.moves == 0 && out.players > 0) out.warnings.add("Nobody changes level. Check the thresholds or the shares against the score range of this method.");
    }

    // ------------------------------------------------------------------ presets

    /** A preset fills the configuration; every number stays editable. They are inspired by published systems of those federations and do not reproduce them. */
    static final String[][] PRESETS = {
        {"BadmintonAverage", "Average points per match, 12 levels (Badminton Vlaanderen)"}, {"WorldBest", "Sum of the best 10 results in 52 weeks (BWF, ATP style)"},
        {"TennisPyramid", "Best 6 results placed in a pyramid of shares (Belgian tennis)"}, {"WinBalance", "Wins minus weighted losses (French tennis V-E-2I-5G)"},
        {"Elo", "Running rating with K factor (FIDE chess)"}, {"TableTennis", "Running rating, narrow scale (German TTR)"}};

    static String presetLabel(String preset) { for (String[] p : PRESETS) if (p[0].equals(preset)) return p[1]; return ""; }

    /** A configuration from a preset with the given level names (null or empty: the default names of the preset). */
    static Config preset(String preset, String[] names) {
        Config c = new Config();
        String[] given = names == null || names.length == 0 ? defaultNames(preset) : names;
        int levels = given.length; c.names = new String[levels + 1]; for (int i = 0; i < levels; i++) c.names[i + 1] = given[i].trim();
        RankingRules.Table base = RankingRules.defaultTable(levels); double[] win = base.winPoints(), up = new double[levels + 1], down = new double[levels + 1]; c.shares = new double[levels + 1];
        for (int l = 1; l <= levels; l++) c.shares[l] = shareOf(l, levels);
        switch (preset == null ? "" : preset) {
            case "WorldBest":
                c.method = BEST; c.bestCount = 10; c.maxUp = 0; c.maxDown = 0; c.frequency = "Weekly";
                c.params = new RankingRules.Params(levels, 52, 1000, 1, -1, 52, 1, 0, false); c.placement = SHARES; break;
            case "TennisPyramid":
                c.method = BEST; c.bestCount = 6; c.maxUp = 2; c.maxDown = 1; c.frequency = "Quarterly"; c.placement = SHARES;
                c.params = new RankingRules.Params(levels, 52, 1000, 1, -1, 78, 2, 1, false); break;
            case "WinBalance":
                c.method = BALANCE; c.maxUp = 1; c.maxDown = 1; c.frequency = "Monthly";
                c.params = new RankingRules.Params(levels, 52, 1000, 1, -1, 104, 3, 1, false);
                for (int l = 1; l <= levels; l++) { up[l] = l == 1 ? 0 : 5; down[l] = l == levels ? -1000 : -3; }
                win = base.winPoints(); break;
            case "Elo": case "TableTennis": {
                c.method = ELO; c.maxUp = 2; c.maxDown = 2; c.frequency = "Monthly";
                c.eloK = preset.equals("Elo") ? 20 : 16; c.eloKNew = preset.equals("Elo") ? 40 : 32; c.eloScale = preset.equals("Elo") ? 400 : 150;
                c.eloStep = preset.equals("Elo") ? 100 : 60; c.eloStart = 1000;
                c.params = new RankingRules.Params(levels, 104, 1000, 1, -1, 104, 1, 1, false);
                for (int l = 1; l <= levels; l++) {
                    double centre = c.eloStart + c.eloStep * (levels - l);
                    up[l] = l == 1 ? 0 : centre + c.eloStep * 0.7; down[l] = l == levels ? -100000 : centre - c.eloStep * 0.7;
                }
                break; }
            default:
                c.method = AVERAGE; c.frequency = "FirstMondayMonthly";
                c.params = new RankingRules.Params(levels, 52, 20, 7, 2, 104, 3, 2, true);
                up = base.promoteAt(); down = base.demoteBelow();
                c.bonusThresholds.putAll(Map.of("HE", 12, "DE", 8, "HD", 24, "DD", 16, "GD", 24));
        }
        if (c.method.equals(BEST) && c.placement.equals(THRESHOLDS)) { up = base.promoteAt(); down = base.demoteBelow(); }
        c.table = new RankingRules.Table(levels, win, up, down);
        return c;
    }

    private static String[] defaultNames(String preset) {
        switch (preset == null ? "" : preset) {
            case "TennisPyramid": return new String[]{"115", "95", "75", "55", "45", "35", "25", "15", "10", "5", "3"};
            case "WinBalance": return new String[]{"1re série", "2e série", "15", "15/1", "15/2", "15/3", "15/4", "15/5", "30", "30/1", "30/2", "30/3", "30/4", "30/5", "40"};
            case "Elo": return new String[]{"Master", "Expert", "Class A", "Class B", "Class C", "Class D", "Class E"};
            case "TableTennis": return new String[]{"Verbandsliga", "Landesliga", "Bezirksliga", "Kreisliga", "Kreisklasse"};
            default: { String[] names = new String[12]; for (int i = 0; i < 12; i++) names[i] = String.valueOf(i + 1); return names; }
        }
    }

    /** A pyramid: every lower level is about one and a half times as large as the level above it. */
    static double shareOf(int level, int levels) {
        double total = 0; for (int l = 1; l <= levels; l++) total += Math.pow(1.5, l - 1);
        return Math.round(1000 * Math.pow(1.5, level - 1) / total) / 10.0;
    }

    /** Level names from one line: separated by commas, semicolons or new lines. */
    static String[] parseNames(String raw) {
        if (raw == null || raw.isBlank()) return new String[0];
        List<String> names = new ArrayList<>(); Set<String> seen = new HashSet<>();
        for (String part : raw.split("[,;\\r\\n]+")) {
            String name = part.trim(); if (name.isEmpty()) continue;
            if (!seen.add(name.toLowerCase(Locale.ROOT))) throw new IllegalArgumentException("The level name " + name + " is used twice.");
            names.add(name);
        }
        return names.toArray(new String[0]);
    }

    // ------------------------------------------------------------------ text import

    /**
     * Parses pasted game lines: date;discipline;winner;winner partner;loser;loser partner;score;event. A header line is ignored; a comma or tab also separates.
     * A name may carry a level in brackets ("Ann Peeters (5)"). Returns the games; lines that cannot be read are reported in {@code errors} with their number.
     */
    static List<Game> parseLines(String text, List<String> errors) {
        List<Game> games = new ArrayList<>(); int number = 0;
        for (String line : text.split("\\r?\\n")) {
            number++;
            if (line.isBlank()) continue;
            String separator = line.contains(";") ? ";" : line.contains("\t") ? "\t" : ",";
            String[] cell = line.split(separator, -1);
            if (cell.length < 6) { if (number > 1 && errors.size() < 20) errors.add("Line " + number + ": expected date, discipline, winner, partner, loser, partner."); else if (number > 1) errors.add(""); continue; }
            LocalDate date;
            try { date = LocalDate.parse(cell[0].trim()); }
            catch (RuntimeException e) { if (number > 1) errors.add("Line " + number + ": the date must be yyyy-mm-dd."); continue; }
            String discipline = cell[1].trim().toUpperCase(Locale.ROOT);
            if (discipline.isEmpty() || cell[2].isBlank() || cell[4].isBlank()) { errors.add("Line " + number + ": discipline, winner and loser are required."); continue; }
            if (cell[3].isBlank() != cell[5].isBlank()) { errors.add("Line " + number + ": a doubles game needs a partner on both sides."); continue; }
            String score = cell.length > 6 ? cell[6].trim() : "";
            String flag = score.toLowerCase(Locale.ROOT).matches(".*(wo|w\\.o|walk).*") ? "W" : score.toLowerCase(Locale.ROOT).contains("ret") ? "R" : "";
            List<String> win = new ArrayList<>(), lose = new ArrayList<>(); List<Integer> wl = new ArrayList<>(), ll = new ArrayList<>();
            add(cell[2], win, wl); add(cell[3], win, wl); add(cell[4], lose, ll); add(cell[5], lose, ll);
            games.add(new Game(date, discipline, win, lose, wl, ll, flag, cell.length > 7 ? cell[7].trim() : ""));
        }
        return games;
    }

    private static void add(String raw, List<String> names, List<Integer> levels) {
        if (raw.isBlank()) return;
        names.add(RankingRules.cleanName(raw)); levels.add(RankingRules.bracketLevel(raw));
    }
}
