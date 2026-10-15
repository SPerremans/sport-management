package myfirstmodule;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/** Result registration rules. Pure Java: no runtime, account, email or payment side effects. */
public final class MatchRules {
    private MatchRules() {}

    public static final class Format {
        public String family = "Rally", tiePolicy = "Draw", approvalAnchor = "Scheduled";
        public int setsToWin = 2, target = 21, margin = 2, cap = 30;
        public int gamesToWin = 6, gamesMargin = 2, tieAt = 6, tieTarget = 7, decidingTarget;
        public int homeHours = 36, awayHours = 36, minimumReviewHours = 12;
        public boolean stopWhenDecided;
        public BigDecimal winPoints = new BigDecimal("2"), drawPoints = BigDecimal.ONE, lossPoints = BigDecimal.ZERO;

        public void validate() {
            require(Arrays.asList("Rally", "Games", "Total").contains(family), "Choose a supported scoring family.");
            require(setsToWin > 0 && setsToWin <= 10, "Sets needed to win must be between 1 and 10.");
            require(target > 0 && target <= 100000 && margin > 0 && margin <= 100000 && (cap == 0 || (cap >= target && cap <= 100000)), "Check the target, winning margin and optional cap.");
            require(gamesToWin > 0 && gamesMargin > 0 && tieAt >= 0 && tieTarget > 0 && decidingTarget >= 0,
                "Check the game and tie-break settings.");
            require(tieAt == 0 || tieAt >= gamesToWin - 1, "The tie-break trigger must allow a normal set to finish.");
            require(homeHours >= 0 && awayHours >= 0 && minimumReviewHours > 0,
                "Approval windows cannot be negative and the minimum review period must be positive.");
            require(homeHours <= 8760 && awayHours <= 8760 && minimumReviewHours <= 8760, "Review windows cannot exceed one year.");
            require(Arrays.asList("Draw", "SetsPoints", "Decider").contains(tiePolicy), "Choose how a tied fixture is resolved.");
            require(Arrays.asList("Scheduled", "Actual", "Submitted").contains(approvalAnchor), "Choose the approval clock anchor.");
            require(winPoints != null && drawPoints != null && lossPoints != null, "Configure competition points.");
        }
    }

    public static final class SetScore {
        public final int home, away, homeTie, awayTie;
        public final boolean hasTie, matchTie;
        public SetScore(int home, int away) { this(home, away, 0, 0, false, false); }
        public SetScore(int home, int away, int homeTie, int awayTie, boolean hasTie, boolean matchTie) {
            this.home = home; this.away = away; this.homeTie = homeTie; this.awayTie = awayTie;
            this.hasTie = hasTie; this.matchTie = matchTie;
        }
        public String toString() { return (matchTie ? "[" : "") + home + "-" + away +
            (hasTie ? " (" + homeTie + "-" + awayTie + ")" : "") + (matchTie ? "]" : ""); }
    }

    public static final class ContestResult {
        public String recordedWinner = "", officialWinner = "", outcome;
        public int homeSets, awaySets, homeTotal, awayTotal;
        public boolean excluded, rankingEligible, resolved;
        public String score;
    }

    /** End score must stop at the first winning point; a cap takes precedence over the margin. */
    public static boolean pointSetComplete(int home, int away, int target, int margin, int cap) {
        if (home < 0 || away < 0 || home == away || (cap > 0 && Math.max(home, away) > cap)) return false;
        int high = Math.max(home, away), low = Math.min(home, away);
        if (cap > 0 && high == cap) return high >= target && (high == target || low >= cap - margin);
        return high >= target && high - low >= margin &&
            (high == target || high - low == margin);
    }

    private static boolean pointsStillInProgress(int home, int away, int target, int margin, int cap) {
        if (home < 0 || away < 0 || (cap > 0 && Math.max(home, away) >= cap)) return false;
        int high = Math.max(home, away), low = Math.min(home, away);
        return high < target || high - low < margin;
    }

    private static boolean gamesComplete(Format f, SetScore s) {
        if (s.home < 0 || s.away < 0 || s.home == s.away) return false;
        int high = Math.max(s.home, s.away), low = Math.min(s.home, s.away);
        if (f.tieAt > 0 && high == f.tieAt + 1 && low == f.tieAt) {
            return s.hasTie && pointSetComplete(s.homeTie, s.awayTie, f.tieTarget, 2, 0) &&
                Integer.signum(s.home - s.away) == Integer.signum(s.homeTie - s.awayTie);
        }
        if (s.hasTie || (f.tieAt > 0 && high > f.tieAt + 1)) return false;
        return pointSetComplete(s.home, s.away, f.gamesToWin, f.gamesMargin, 0);
    }

    private static boolean gamesStillInProgress(Format f, SetScore s) {
        if (s.hasTie) return f.tieAt > 0 && s.home == f.tieAt && s.away == f.tieAt &&
            pointsStillInProgress(s.homeTie, s.awayTie, f.tieTarget, 2, 0);
        return pointsStillInProgress(s.home, s.away, f.gamesToWin, f.gamesMargin, 0) &&
            (f.tieAt == 0 || Math.max(s.home, s.away) <= f.tieAt);
    }

    /** Administrative outcomes retain the raw score; exclusion removes official and ranking credit. */
    public static ContestResult contest(Format f, List<SetScore> sets, String outcome, String reason) {
        f.validate();
        require(outcome != null && Arrays.asList("Pending", "Played", "HomeRetired", "AwayRetired", "HomeForfeit", "AwayForfeit", "NotPlayed", "Excluded").contains(outcome), "Choose a contest outcome.");
        ContestResult r = new ContestResult(); r.outcome = outcome;
        boolean retired = outcome.endsWith("Retired"), ruling = outcome.endsWith("Forfeit") || outcome.equals("Excluded");
        if (retired || ruling) require(reason != null && !reason.trim().isEmpty(), "Record a reason for this outcome.");
        List<String> display = new ArrayList<>();
        for (int i = 0; i < sets.size(); i++) {
            SetScore s = sets.get(i);
            require(s.home >= 0 && s.away >= 0 && s.homeTie >= 0 && s.awayTie >= 0 && Math.max(Math.max(s.home,s.away),Math.max(s.homeTie,s.awayTie)) <= 1000000, "Scores must be between zero and one million.");
            require(r.homeSets < f.setsToWin && r.awaySets < f.setsToWin, "Scores contain a set after the contest was already won.");
            require(i < 2 * f.setsToWin - 1, "Too many sets for the selected format.");
            boolean deciding = f.family.equals("Games") && f.decidingTarget > 0 && i == 2 * f.setsToWin - 2;
            require(s.matchTie == deciding || f.family.equals("Total"), deciding ? "Enter the deciding set as a match tie-break." : "A match tie-break is not allowed in this set.");
            boolean complete;
            boolean inProgress;
            if (f.family.equals("Total")) {
                require(sets.size() == 1 && !s.hasTie && !s.matchTie, "Team-total scoring takes one final score.");
                complete = true; inProgress = true;
            } else if (deciding) {
                require(!s.hasTie, "Enter match tie-break points directly.");
                complete = pointSetComplete(s.home, s.away, f.decidingTarget, 2, 0);
                inProgress = pointsStillInProgress(s.home, s.away, f.decidingTarget, 2, 0);
            } else if (f.family.equals("Games")) {
                complete = gamesComplete(f, s); inProgress = gamesStillInProgress(f, s);
            } else {
                require(!s.hasTie && !s.matchTie, "This format uses points per set.");
                complete = pointSetComplete(s.home, s.away, f.target, f.margin, f.cap);
                inProgress = pointsStillInProgress(s.home, s.away, f.target, f.margin, f.cap);
            }
            require(complete || ((retired || outcome.equals("Pending") || ruling) && i == sets.size() - 1 && inProgress), "Invalid final set score: " + s);
            if (complete && s.home > s.away) r.homeSets++;
            if (complete && s.away > s.home) r.awaySets++;
            r.homeTotal += s.home; r.awayTotal += s.away; display.add(s.toString());
        }
        if (f.family.equals("Total") && !sets.isEmpty()) r.recordedWinner = side(Integer.compare(r.homeTotal, r.awayTotal));
        else if (r.homeSets == f.setsToWin) r.recordedWinner = "Home";
        else if (r.awaySets == f.setsToWin) r.recordedWinner = "Away";
        if (outcome.equals("Played")) {
            require(!sets.isEmpty() && (!r.recordedWinner.isEmpty()), "The entered scores do not produce a completed contest.");
            r.officialWinner = r.recordedWinner; r.resolved = true;
        } else if (retired) {
            require(r.recordedWinner.isEmpty() || f.family.equals("Total"), "A finished contest cannot subsequently be recorded as a retirement.");
            r.officialWinner = outcome.equals("HomeRetired") ? "Away" : "Home"; r.resolved = true;
        } else if (outcome.endsWith("Forfeit")) {
            r.officialWinner = outcome.equals("HomeForfeit") ? "Away" : "Home"; r.resolved = true;
        } else if (outcome.equals("Excluded")) { r.excluded = true; r.resolved = true;
        } else if (outcome.equals("NotPlayed")) {
            require(sets.isEmpty(), "An unplayed contest cannot have scores."); r.resolved = true;
        }
        r.rankingEligible = r.resolved && !r.excluded && !outcome.equals("NotPlayed") && !outcome.endsWith("Forfeit") && !sets.isEmpty();
        r.score = String.join("  ", display);
        return r;
    }

    public static final class FixtureResult {
        public int home, away;
        public String winner;
        public BigDecimal homePoints, awayPoints;
    }

    public static FixtureResult fixture(Format f, List<ContestResult> regular, List<ContestResult> deciders,
            BigDecimal homeAdjustment, BigDecimal awayAdjustment) {
        return fixture(f, regular, deciders, homeAdjustment, awayAdjustment, "");
    }

    public static FixtureResult fixture(Format f, List<ContestResult> regular, List<ContestResult> deciders,
            BigDecimal homeAdjustment, BigDecimal awayAdjustment, String federationWinner) {
        f.validate(); FixtureResult r = new FixtureResult();
        int hs = 0, as = 0, hp = 0, ap = 0, unplayed = 0;
        require(!regular.isEmpty(), "A fixture needs at least one regular contest.");
        if (f.family.equals("Total")) {
            require(regular.size() == 1, "Team-total fixtures have one contest.");
        }
        for (ContestResult c : regular) {
            require(c.resolved, "Complete every contest or record an authorized exceptional outcome.");
            if (c.outcome.equals("NotPlayed")) { unplayed++; continue; }
            if (c.excluded) continue;
            if (c.officialWinner.equals("Home")) r.home++;
            if (c.officialWinner.equals("Away")) r.away++;
            hs += c.homeSets; as += c.awaySets; hp += c.homeTotal; ap += c.awayTotal;
        }
        if (unplayed > 0) require(f.stopWhenDecided && Math.abs(r.home - r.away) > unplayed,
            "Remaining contests may be unplayed only when the fixture winner is already certain.");
        r.winner = side(Integer.compare(r.home, r.away));
        if (f.family.equals("Total")) {
            ContestResult c = regular.get(0); r.home = c.homeTotal; r.away = c.awayTotal;
            r.winner = c.excluded ? "Draw" : c.officialWinner;
        }
        boolean needsDecider = r.winner.equals("Draw") && f.tiePolicy.equals("Decider") && federationWinner.isEmpty();
        if (!federationWinner.isEmpty()) {
            require(Arrays.asList("Home", "Away", "Draw").contains(federationWinner), "Choose a valid federation-awarded fixture outcome.");
            r.winner = federationWinner;
        } else if (r.winner.equals("Draw") && f.tiePolicy.equals("SetsPoints")) {
            r.winner = side(Integer.compare(hs, as));
            if (r.winner.equals("Draw")) r.winner = side(Integer.compare(hp, ap));
            require(!r.winner.equals("Draw"), "Sets and score totals remain equal. A federation ruling is required.");
        } else if (r.winner.equals("Draw") && f.tiePolicy.equals("Decider")) {
            require(deciders.size() == 1 && deciders.get(0).resolved && !deciders.get(0).excluded &&
                Arrays.asList("Home", "Away").contains(deciders.get(0).officialWinner), "Complete the predefined deciding contest.");
            r.winner = deciders.get(0).officialWinner;
        }
        for (ContestResult c : deciders) if (!needsDecider)
            require(c.outcome.equals("Pending") || c.outcome.equals("NotPlayed"), "This fixture does not use a deciding contest.");
        r.homePoints = award(f, r.winner, "Home").add(homeAdjustment);
        r.awayPoints = award(f, r.winner, "Away").add(awayAdjustment);
        return r;
    }

    public static BigDecimal award(Format f, String winner, String team) {
        require(Arrays.asList("Home", "Away", "Draw").contains(winner), "The fixture needs an official outcome.");
        return winner.equals("Draw") ? f.drawPoints : winner.equals(team) ? f.winPoints : f.lossPoints;
    }

    public static final class Deadlines {
        public final Instant home, away;
        private Deadlines(Instant home, Instant away) { this.home = home; this.away = away; }
    }

    public static Deadlines deadlines(Format f, Instant scheduled, Instant actual, Instant submitted) {
        f.validate(); require(submitted != null, "A result must be submitted before approval is possible.");
        Instant anchor = f.approvalAnchor.equals("Submitted") ? submitted : f.approvalAnchor.equals("Actual") ? actual : scheduled;
        require(anchor != null, "The configured approval clock has no start time.");
        Instant minimum = submitted.plus(Duration.ofHours(f.minimumReviewHours));
        Instant home = later(anchor.plus(Duration.ofHours(f.homeHours)), minimum);
        Instant away = later(anchor.plus(Duration.ofHours((long) f.homeHours + f.awayHours)), home.plus(Duration.ofHours(f.awayHours)));
        return new Deadlines(home, away);
    }

    /** Availability controls selection for the event start; an existing snapshot remains unchanged. */
    public static boolean available(boolean enabled, Instant from, Instant until, Instant eventStart) {
        return enabled && eventStart != null && (from == null || !eventStart.isBefore(from)) && (until == null || eventStart.isBefore(until));
    }

    public static boolean lineupEditable(String mode, int offsetMinutes, Instant scheduled, Instant actualStart, Instant now) {
        if (actualStart != null) return false;
        if ("Start".equals(mode)) return true;
        if (scheduled == null) return false;
        Instant lock = "Offset".equals(mode) ? scheduled.minus(Duration.ofMinutes(offsetMinutes)) : scheduled;
        return now.isBefore(lock);
    }

    private static Instant later(Instant a, Instant b) { return a.isAfter(b) ? a : b; }
    private static String side(int comparison) { return comparison > 0 ? "Home" : comparison < 0 ? "Away" : "Draw"; }
    private static void require(boolean condition, String message) { if (!condition) throw new IllegalArgumentException(message); }
}
