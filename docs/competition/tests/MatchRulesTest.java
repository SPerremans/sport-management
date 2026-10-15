import myfirstmodule.MatchRules;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

/** Standalone rule tests; this does not verify Mendix permissions or UI/runtime wiring. */
public final class MatchRulesTest {
    private static int checks;
    private static void check(boolean condition, String name) { checks++; if (!condition) throw new AssertionError(name); }
    private static void rejects(Runnable action, String name) {
        checks++; try { action.run(); } catch (IllegalArgumentException expected) { return; }
        throw new AssertionError(name);
    }
    private static MatchRules.ContestResult result(boolean home) {
        MatchRules.Format f = new MatchRules.Format();
        return MatchRules.contest(f, Arrays.asList(new MatchRules.SetScore(home ? 21 : 10, home ? 10 : 21),
            new MatchRules.SetScore(home ? 21 : 12, home ? 12 : 21)), "Played", "");
    }
    public static void main(String[] args) {
        MatchRules.Format f = new MatchRules.Format();
        check(MatchRules.pointSetComplete(21, 19, 21, 2, 30), "normal endpoint");
        check(MatchRules.pointSetComplete(22, 20, 21, 2, 30), "deuce endpoint");
        check(MatchRules.pointSetComplete(30, 29, 21, 2, 30), "cap overrides margin");
        check(!MatchRules.pointSetComplete(30, 27, 21, 2, 30), "cannot play past an earlier win");
        check(!MatchRules.pointSetComplete(22, 18, 21, 2, 30), "cannot continue past 21-18");
        check(!MatchRules.pointSetComplete(21, 20, 21, 2, 30), "margin required before cap");
        check(!MatchRules.pointSetComplete(31, 29, 21, 2, 30), "above cap");
        check(MatchRules.pointSetComplete(15, 0, 15, 2, 15), "target may equal cap");
        check(MatchRules.pointSetComplete(11, 9, 11, 2, 0), "table tennis endpoint");
        check(MatchRules.pointSetComplete(28, 26, 11, 2, 0), "uncapped deuce");
        MatchRules.ContestResult home = result(true), away = result(false);
        check(home.officialWinner.equals("Home") && home.rankingEligible, "completed singles result");
        rejects(() -> MatchRules.contest(f, Arrays.asList(new MatchRules.SetScore(21, 10)), "Played", ""), "incomplete match");
        rejects(() -> MatchRules.contest(f, Arrays.asList(new MatchRules.SetScore(21, 10), new MatchRules.SetScore(21, 10), new MatchRules.SetScore(0, 21)), "Played", ""), "set after winner");
        MatchRules.ContestResult retired = MatchRules.contest(f, Arrays.asList(new MatchRules.SetScore(21, 15), new MatchRules.SetScore(8, 6)), "HomeRetired", "Injury");
        check(retired.officialWinner.equals("Away") && retired.homeTotal == 29 && retired.awayTotal == 21, "retirement retains actual score");
        rejects(() -> MatchRules.contest(f, Collections.emptyList(), "HomeRetired", ""), "exception requires reason");
        MatchRules.ContestResult forfeit = MatchRules.contest(f, Arrays.asList(new MatchRules.SetScore(21, 15), new MatchRules.SetScore(21, 15)), "HomeForfeit", "Federation ruling");
        check(forfeit.recordedWinner.equals("Home") && forfeit.officialWinner.equals("Away") && !forfeit.rankingEligible, "recorded score and awarded outcome differ");
        List<MatchRules.ContestResult> regular = new ArrayList<>(Arrays.asList(home, home, home, home, home, away, away, away));
        check(MatchRules.fixture(f, regular, Collections.emptyList(), BigDecimal.ZERO, BigDecimal.ZERO).home == 5, "5-3 fixture");
        regular.set(4, MatchRules.contest(f, Arrays.asList(new MatchRules.SetScore(21, 15), new MatchRules.SetScore(21, 15)), "Excluded", "Federation ruling"));
        MatchRules.FixtureResult excluded = MatchRules.fixture(f, regular, Collections.emptyList(), BigDecimal.ZERO, BigDecimal.ZERO);
        check(excluded.home == 4 && excluded.away == 3 && !regular.get(4).rankingEligible, "excluded win produces 4-3");
        check(MatchRules.fixture(f, Arrays.asList(home, away), Collections.emptyList(), BigDecimal.ZERO, BigDecimal.ZERO).winner.equals("Draw"), "allowed draw");
        f.tiePolicy = "Decider";
        rejects(() -> MatchRules.fixture(f, Arrays.asList(home, away), Collections.emptyList(), BigDecimal.ZERO, BigDecimal.ZERO), "decider required");
        check(MatchRules.fixture(f, Arrays.asList(home, away), Arrays.asList(away), BigDecimal.ZERO, BigDecimal.ZERO).winner.equals("Away"), "deciding contest");
        f.tiePolicy = "SetsPoints";
        rejects(() -> MatchRules.fixture(f, Arrays.asList(home, away), Collections.emptyList(), BigDecimal.ZERO, BigDecimal.ZERO), "unresolved tiebreak needs ruling");
        f.tiePolicy = "Draw"; f.stopWhenDecided = true;
        MatchRules.ContestResult unplayed = MatchRules.contest(f, Collections.emptyList(), "NotPlayed", "Winner decided");
        check(MatchRules.fixture(f, Arrays.asList(home, home, home, unplayed, unplayed), Collections.emptyList(), BigDecimal.ZERO, BigDecimal.ZERO).winner.equals("Home"), "stop after clinch");
        rejects(() -> MatchRules.fixture(f, Arrays.asList(home, away, unplayed), Collections.emptyList(), BigDecimal.ZERO, BigDecimal.ZERO), "cannot stop before clinch");
        f.stopWhenDecided = false;
        rejects(() -> MatchRules.fixture(f, Arrays.asList(home, home, home, unplayed), Collections.emptyList(), BigDecimal.ZERO, BigDecimal.ZERO), "must play all");
        f.family = "Games"; f.decidingTarget = 10;
        MatchRules.ContestResult tennis = MatchRules.contest(f, Arrays.asList(new MatchRules.SetScore(6, 4),
            new MatchRules.SetScore(6, 7, 5, 7, true, false), new MatchRules.SetScore(10, 8, 0, 0, false, true)), "Played", "");
        check(tennis.officialWinner.equals("Home"), "tennis/padel deciding match tiebreak");
        rejects(() -> MatchRules.contest(f, Arrays.asList(new MatchRules.SetScore(7, 6), new MatchRules.SetScore(6, 4)), "Played", ""), "tiebreak score required");
        rejects(() -> MatchRules.contest(f, Arrays.asList(new MatchRules.SetScore(7, 6, 5, 7, true, false), new MatchRules.SetScore(6, 4)), "Played", ""), "tiebreak winner agrees with set");
        f.family = "Total"; f.tiePolicy = "Draw";
        MatchRules.ContestResult football = MatchRules.contest(f, Arrays.asList(new MatchRules.SetScore(2, 2)), "Played", "");
        check(MatchRules.fixture(f, Arrays.asList(football), Collections.emptyList(), BigDecimal.ZERO, BigDecimal.ZERO).winner.equals("Draw"), "football draw");
        MatchRules.ContestResult footballForfeit = MatchRules.contest(f, Arrays.asList(new MatchRules.SetScore(3, 1)), "HomeForfeit", "Ruling");
        MatchRules.FixtureResult awarded = MatchRules.fixture(f, Arrays.asList(footballForfeit), Collections.emptyList(), new BigDecimal("-1"), BigDecimal.ZERO);
        check(awarded.home == 3 && awarded.winner.equals("Away") && awarded.homePoints.equals(new BigDecimal("-1")), "team forfeit retains score and separate points adjustment");
        Instant start = Instant.parse("2026-10-12T20:00:00Z"), submitted = start.plusSeconds(3 * 3600);
        MatchRules.Deadlines d = MatchRules.deadlines(f, start, start.plusSeconds(600), submitted);
        check(d.home.equals(start.plusSeconds(36 * 3600)) && d.away.equals(start.plusSeconds(72 * 3600)), "36+36 scheduled windows");
        Instant late = start.plusSeconds(90 * 3600);
        d = MatchRules.deadlines(f, start, null, late);
        check(d.home.equals(late.plusSeconds(12 * 3600)) && d.away.equals(late.plusSeconds(48 * 3600)), "late submission protects both review windows");
        rejects(() -> MatchRules.deadlines(f, start, null, null), "no acceptance before submission");
        f.approvalAnchor = "Actual";
        rejects(() -> MatchRules.deadlines(f, start, null, submitted), "actual clock requires actual start");
        check(MatchRules.available(true, start, late, start), "availability lower boundary inclusive");
        check(!MatchRules.available(true, start, late, late), "availability upper boundary exclusive");
        check(!MatchRules.available(false, null, null, start), "disabled format cannot be selected");
        check(MatchRules.lineupEditable("Start", 0, start, null, late), "captain edits until actual start");
        check(!MatchRules.lineupEditable("Start", 0, start, start, late), "actual start locks lineup");
        check(!MatchRules.lineupEditable("Offset", 30, start, null, start.minusSeconds(1800)), "offset deadline locks at boundary");
        MatchRules.Format rulingFormat = new MatchRules.Format(); rulingFormat.tiePolicy = "SetsPoints";
        check(MatchRules.fixture(rulingFormat, Arrays.asList(home, away), Collections.emptyList(), BigDecimal.ZERO, BigDecimal.ZERO, "Away").winner.equals("Away"), "federation can resolve an otherwise unresolved tie");
        for (int target : new int[]{11,15,21}) for (int cap : new int[]{0,target,target+9}) {
            for (int h=0;h<35;h++) for (int a=0;a<35;a++) {
                int high=Math.max(h,a),low=Math.min(h,a);
                boolean wins=high>=target && (high-low>=2 || (cap>0 && high==cap));
                boolean previousWon=high>0 && high-1>=target && high-1-low>=2;
                boolean oracle=h!=a && (cap==0 || high<=cap) && wins && !previousWon;
                check(MatchRules.pointSetComplete(h,a,target,2,cap)==oracle,"endpoint property "+target+"/"+cap+" "+h+"-"+a);
            }
        }
        System.out.println("PASS: " + checks + " standalone scoring, outcome, availability and approval-clock assertions.");
    }
}
