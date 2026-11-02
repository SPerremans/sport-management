package myfirstmodule;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Pure individual-ranking rules; no Mendix types, so they are unit-testable.
 * Level 1 is the highest level and the scheme's lowest level (12 by default) is where a new player starts.
 * Rules published by Badminton Vlaanderen: points of a win by opponent level, an average over the most recent matches
 * of the last 52 weeks (never divided by fewer than 7), one level up or down per evaluation, a bonus counter, and inactivity.
 * NOT published (editable sample defaults): promotion and demotion thresholds per level, how doubles points are derived
 * from the two opponents, what exactly increments the bonus counter, and how losses against a "directly higher" opponent are read.
 */
final class RankingRules {
    private RankingRules() {}

    /** Scheme parameters. */
    record Params(int levels, int windowWeeks, int maxMatches, int minDivisor, int maxSpread,
                  int inactiveWeeks, int inactiveMinMatches, int inactiveDrop, boolean ignoreHigherLosses) {
        static Params badmintonVlaanderen() { return new Params(12, 52, 20, 7, 2, 104, 3, 2, true); }
    }

    /** Points and thresholds per level. Arrays are indexed by level (index 0 unused). */
    record Table(int levels, double[] winPoints, double[] promoteAt, double[] demoteBelow) {
        double points(double opponentLevel) {
            int low = (int)Math.floor(opponentLevel), high = (int)Math.ceil(opponentLevel);
            low = Math.max(1, Math.min(levels, low)); high = Math.max(1, Math.min(levels, high));
            return low == high ? winPoints[low] : (winPoints[low] + winPoints[high]) / 2.0;
        }
    }

    /** Published singles points for levels 1..12 (Badminton Vlaanderen). */
    static final double[] PUBLISHED_POINTS = {0, 2831, 1961, 1359, 942, 652, 452, 313, 217, 150, 104, 72, 50};

    /** Default table: published points for 1..12; extra levels extend by the published ratio. Thresholds are sample defaults. */
    static Table defaultTable(int levels) {
        double[] win = new double[levels + 1], up = new double[levels + 1], down = new double[levels + 1];
        for (int level = 1; level <= levels; level++)
            win[level] = level < PUBLISHED_POINTS.length ? PUBLISHED_POINTS[level] : Math.max(1, Math.round(win[level - 1] * 0.69));
        for (int level = 1; level <= levels; level++) {
            up[level] = level == 1 ? 0 : Math.round(win[level - 1] * 0.55);   // sample default: > half of the games won against the next level up
            down[level] = level == levels ? 0 : Math.round(win[level] * 0.30); // sample default: under a third won against one's own level
        }
        return new Table(levels, win, up, down);
    }

    /** One played match from a player's point of view. {@code opponentLevel} is the average level of the opponent (pair). */
    record Match(LocalDate date, boolean won, double opponentLevel, double ownLevel, String label) {
        Match(LocalDate date, boolean won, double opponentLevel, double ownLevel) { this(date, won, opponentLevel, ownLevel, ""); }
    }

    /** One match of the window with the decision whether it counts and the points it earns. */
    record Detail(Match match, boolean counted, String note, double points) {}

    /** The average of one player in one discipline, with every step shown. */
    record Computation(List<Detail> details, int inWindow, int counted, double total, double divisor, double average) {}

    /**
     * Steps of the average: matches of the window, losses against a directly higher level removed, the most recent matches kept,
     * points of the won ones added and divided by at least the minimum divisor.
     */
    static Computation compute(int level, List<Match> all, LocalDate moment, Params p, Table t) {
        LocalDate from = moment.minusWeeks(p.windowWeeks());
        List<Match> window = new ArrayList<>();
        for (Match m : all) if (m.date().isAfter(from) && !m.date().isAfter(moment)) window.add(m);
        window.sort(Comparator.comparing(Match::date).reversed());
        List<Detail> details = new ArrayList<>(); int counted = 0; double total = 0;
        for (Match m : window) {
            boolean higherLoss = !m.won() && p.ignoreHigherLosses() && m.opponentLevel() < m.ownLevel() && m.ownLevel() - m.opponentLevel() <= 1.0;
            if (higherLoss) { details.add(new Detail(m, false, "Does not count: loss against a directly higher level", 0)); continue; }
            if (counted >= p.maxMatches()) { details.add(new Detail(m, false, "Does not count: only the " + p.maxMatches() + " most recent matches count", 0)); continue; }
            counted++; double points = m.won() ? t.points(m.opponentLevel()) : 0; total += points;
            details.add(new Detail(m, true, m.won() ? "Counts: win" : "Counts: loss, 0 points", points));
        }
        double divisor = Math.max(counted, p.minDivisor());
        return new Computation(details, window.size(), counted, total, divisor, total / divisor);
    }

    /** Why and how a level changes. {@code kind} is Evaluation, Bonus, Inactivity or None. */
    record Result(int level, String kind, String reason, double average, int counted, int matchesInWindow, int bonusCounter, LocalDate lastActive) {
        boolean changes(int from) { return level != from; }
    }

    /**
     * Evaluates one player in one discipline at an evaluation moment.
     * @param since only wins after this date count towards the bonus counter (the last level change)
     * @param otherLevels the same player's levels in the other disciplines (for the maximum spread)
     */
    static Result evaluate(int level, List<Match> all, LocalDate moment, LocalDate lastActive, LocalDate since,
                           int bonusThreshold, List<Integer> otherLevels, Params p, Table t) {
        Computation c = compute(level, all, moment, p, t);
        int inWindow = c.inWindow(); double average = c.average();
        List<Match> window = new ArrayList<>();
        for (Detail d : c.details()) window.add(d.match());
        List<Match> valid = new ArrayList<>();
        for (Detail d : c.details()) if (d.counted()) valid.add(d.match());
        int bonus = 0;
        for (Match m : window) if (m.won() && m.date().isAfter(since) && m.opponentLevel() < m.ownLevel()) bonus++;

        LocalDate active = lastActive;
        boolean inactive = inWindow < p.inactiveMinMatches();
        if (!inactive) active = moment;
        int target = level; String kind = "None"; String reason = "No change: average " + fmt(average) + " over " + valid.size() + " match(es).";

        if (inactive && lastActive != null && ChronoUnit.WEEKS.between(lastActive, moment) >= p.inactiveWeeks()) {
            target = Math.min(p.levels(), level + p.inactiveDrop()); active = moment;
            kind = target != level ? "Inactivity" : "None";
            reason = target != level ? "Fewer than " + p.inactiveMinMatches() + " matches in " + p.windowWeeks() + " weeks for " + p.inactiveWeeks() + " weeks: down " + (target - level) + " level(s)." : reason;
        } else if (bonusThreshold > 0 && bonus >= bonusThreshold && level > 1) {
            int promoted = level - 1;
            if (spreadOk(promoted, otherLevels, p.maxSpread())) { target = promoted; kind = "Bonus"; reason = "Bonus counter " + bonus + " reached " + bonusThreshold + ": up one level."; }
            else reason = "Bonus counter " + bonus + " reached " + bonusThreshold + " but the maximum level difference of " + p.maxSpread() + " between disciplines blocks the promotion.";
        } else if (level > 1 && t.promoteAt()[level] > 0 && average >= t.promoteAt()[level]) {
            if (spreadOk(level - 1, otherLevels, p.maxSpread())) { target = level - 1; kind = "Evaluation"; reason = "Average " + fmt(average) + " is at least " + fmt(t.promoteAt()[level]) + ": up one level."; }
            else reason = "Average " + fmt(average) + " qualifies for promotion but the maximum level difference of " + p.maxSpread() + " between disciplines blocks it.";
        } else if (level < p.levels() && inWindow >= p.inactiveMinMatches() && average < t.demoteBelow()[level]) {
            if (spreadOk(level + 1, otherLevels, p.maxSpread())) { target = level + 1; kind = "Evaluation"; reason = "Average " + fmt(average) + " is below " + fmt(t.demoteBelow()[level]) + ": down one level."; }
            else reason = "Average " + fmt(average) + " qualifies for demotion but the maximum level difference of " + p.maxSpread() + " between disciplines blocks it.";
        }
        return new Result(target, kind, reason, average, c.counted(), inWindow, bonus, active);
    }

    /** A negative maximum means the scheme has no limit between disciplines. */
    static boolean spreadOk(int level, List<Integer> others, int maxSpread) {
        if (maxSpread < 0) return true;
        for (int other : others) if (Math.abs(other - level) > maxSpread) return false;
        return true;
    }

    static final String[][] FREQUENCIES = {{"Weekly", "every week"}, {"EveryTwoWeeks", "every two weeks"}, {"Monthly", "every month on the same day"},
        {"FirstMondayMonthly", "every month on the first Monday"}, {"Quarterly", "every three months"}, {"Yearly", "every year"}};

    static String frequencyLabel(String frequency) {
        for (String[] f : FREQUENCIES) if (f[0].equals(frequency)) return f[1];
        return "not set";
    }

    /** The n-th review moment of a schedule that starts at {@code start}. */
    static java.time.LocalDateTime occurrence(String frequency, java.time.LocalDateTime start, int n) {
        switch (frequency == null ? "" : frequency) {
            case "Weekly": return start.plusWeeks(n);
            case "EveryTwoWeeks": return start.plusWeeks(2L * n);
            case "Monthly": return start.plusMonths(n);
            case "Quarterly": return start.plusMonths(3L * n);
            case "Yearly": return start.plusYears(n);
            case "FirstMondayMonthly": {
                LocalDate month = start.toLocalDate().withDayOfMonth(1).plusMonths(n);
                java.time.LocalDateTime moment = firstMonday(month.getYear(), month.getMonthValue()).atTime(start.toLocalTime());
                return moment;
            }
            default: throw new IllegalArgumentException("Choose how often the ranking is reviewed.");
        }
    }

    /** First review moment after the last review (or the start itself when there was none). */
    static java.time.LocalDateTime nextReview(String frequency, java.time.LocalDateTime start, java.time.LocalDateTime last) {
        for (int n = 0; n < 5000; n++) {
            java.time.LocalDateTime moment = occurrence(frequency, start, n);
            if (moment.isBefore(start)) continue;
            if (last == null || moment.isAfter(last)) return moment;
        }
        throw new IllegalStateException("No next review found.");
    }

    /** First Monday of the month of the given date's month. */
    static LocalDate firstMonday(int year, int month) {
        LocalDate day = LocalDate.of(year, month, 1);
        while (day.getDayOfWeek() != java.time.DayOfWeek.MONDAY) day = day.plusDays(1);
        return day;
    }

    /** The next {@code count} first Mondays strictly after {@code after}. */
    static List<LocalDate> nextFirstMondays(LocalDate after, int count) {
        List<LocalDate> result = new ArrayList<>();
        LocalDate month = after.withDayOfMonth(1);
        while (result.size() < count) {
            LocalDate monday = firstMonday(month.getYear(), month.getMonthValue());
            if (monday.isAfter(after)) result.add(monday);
            month = month.plusMonths(1);
        }
        return result;
    }

    /** "Name (7)" -> 7; null when the sheet shows no level. */
    static Integer bracketLevel(String name) {
        if (name == null) return null;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\((\\d{1,2})\\)\\s*$").matcher(name.trim());
        return m.find() ? Integer.valueOf(m.group(1)) : null;
    }

    static String cleanName(String name) {
        return name == null ? "" : name.replaceAll("\\s*\\(\\d{1,2}\\)\\s*$", "").trim().replaceAll("\\s+", " ");
    }

    /** Stable key that matches one person across teams, case and spacing. */
    static String nameKey(String name) { return cleanName(name).toLowerCase(Locale.ROOT); }

    /** MS, WS, MD, WD or XD from a match slot; null when the sport slot is not a ranked individual discipline. */
    static String discipline(String slot, long playersPerSide) {
        if (slot == null) return null;
        if (slot.equals("Mixed")) return playersPerSide == 2 ? "XD" : null;
        if (!slot.equals("Men") && !slot.equals("Women")) return null;
        String sex = slot.equals("Men") ? "M" : "W";
        return playersPerSide == 1 ? sex + "S" : playersPerSide == 2 ? sex + "D" : null;
    }

    static String fmt(double value) { return String.format(Locale.ROOT, "%.1f", value); }
}
