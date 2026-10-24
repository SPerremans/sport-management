package myfirstmodule;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
import java.time.Instant;

/** Discovered by Mendix UnitTesting and runnable with JUnitCore. No database or external effects.
 * These checks prove policy behavior, not signed-in role isolation or browser workflows. */
public class DevelopmentChecksTest {
    private static void rejects(Runnable action) {
        try { action.run(); } catch (IllegalArgumentException expected) { return; }
        fail("Invalid input was accepted; expected a validation error.");
    }
    private static List<MatchRules.SetScore> homeWin() {
        return Arrays.asList(new MatchRules.SetScore(21, 15), new MatchRules.SetScore(21, 18));
    }
    @Test public void Scoring_Completed_match_has_official_winner() {
        var r = MatchRules.contest(new MatchRules.Format(), homeWin(), "Played", "");
        assertEquals("Home", r.officialWinner); assertTrue(r.rankingEligible);
    }
    @Test public void Scoring_Incomplete_match_is_rejected() {
        rejects(() -> MatchRules.contest(new MatchRules.Format(), List.of(new MatchRules.SetScore(21, 15)), "Played", ""));
    }
    @Test public void Scoring_Sets_after_the_winner_are_rejected() {
        var scores = new ArrayList<>(homeWin()); scores.add(new MatchRules.SetScore(0, 21));
        rejects(() -> MatchRules.contest(new MatchRules.Format(), scores, "Played", ""));
    }
    @Test public void Scoring_Forfeit_preserves_score_but_changes_official_winner() {
        var r = MatchRules.contest(new MatchRules.Format(), homeWin(), "HomeForfeit", "Fictional ruling");
        assertEquals("Home", r.recordedWinner); assertEquals("Away", r.officialWinner); assertFalse(r.rankingEligible);
    }
    @Test public void Scoring_Exclusion_removes_official_and_ranking_credit() {
        var r = MatchRules.contest(new MatchRules.Format(), homeWin(), "Excluded", "Fictional exclusion");
        assertEquals("", r.officialWinner); assertTrue(r.excluded); assertFalse(r.rankingEligible);
    }
    @Test public void Scoring_Administrative_outcomes_require_reasons() {
        for (String outcome : List.of("HomeRetired", "AwayRetired", "HomeForfeit", "AwayForfeit", "Excluded"))
            rejects(() -> MatchRules.contest(new MatchRules.Format(), List.of(), outcome, " "));
    }
    @Test public void Scoring_Negative_and_excessive_scores_are_rejected() {
        for (int score : new int[]{-1, 1000001})
            rejects(() -> MatchRules.contest(new MatchRules.Format(), List.of(new MatchRules.SetScore(score, 0)), "Pending", ""));
    }
    @Test public void Scoring_Invalid_configuration_is_rejected() {
        var f = new MatchRules.Format(); f.setsToWin = 0; rejects(f::validate);
        f.setsToWin = 2; f.cap = 20; rejects(f::validate);
    }
    @Test public void Scoring_Deuce_and_cap_boundaries_are_enforced() {
        assertTrue(MatchRules.pointSetComplete(22, 20, 21, 2, 30));
        assertTrue(MatchRules.pointSetComplete(30, 29, 21, 2, 30));
        assertFalse(MatchRules.pointSetComplete(21, 20, 21, 2, 30));
        assertFalse(MatchRules.pointSetComplete(31, 29, 21, 2, 30));
        assertFalse(MatchRules.pointSetComplete(22, 18, 21, 2, 30));
    }
    @Test public void Scoring_Disabled_format_is_unavailable() {
        assertFalse(MatchRules.available(false, null, null, Instant.parse("2026-01-01T12:00:00Z")));
    }
    @Test public void Accounts_Only_eligible_user_view_targets_are_allowed() {
        for (int mask = 0; mask < 32; mask++)
            assertEquals("Eligibility combination " + mask, mask == 1,
                UserViewPolicy.eligible((mask&1)!=0, (mask&2)!=0, (mask&4)!=0, (mask&8)!=0, (mask&16)!=0));
    }
    @Test public void Accounts_Missing_or_wrong_user_view_proof_is_rejected() {
        assertFalse(UserViewPolicy.tokenMatches(null, "proof")); assertFalse(UserViewPolicy.tokenMatches("", ""));
        assertFalse(UserViewPolicy.tokenMatches("proof", "other")); assertTrue(UserViewPolicy.tokenMatches("proof", "proof"));
    }
    @Test public void Accounts_User_view_is_bound_to_session_and_expiry() {
        assertTrue(UserViewPolicy.validBinding("a", "a", 1001, 1000));
        assertFalse(UserViewPolicy.validBinding("a", "b", 1001, 1000));
        assertFalse(UserViewPolicy.validBinding("a", "a", 1000, 1000));
        assertFalse(UserViewPolicy.validBinding("a", null, 1001, 1000));
    }
    @Test public void Accounts_Invalid_selected_identifiers_are_rejected() {
        for (String id : Arrays.asList(null, "", "0", "-1", "1 or 1=1", "9223372036854775808"))
            rejects(() -> UserViewPolicy.accountId(id));
        assertEquals(Long.MAX_VALUE, UserViewPolicy.accountId(Long.toString(Long.MAX_VALUE)));
    }
    @Test public void Accounts_Audit_reason_is_required_and_single_line() {
        for (String reason : Arrays.asList(null, "", "ab", "x".repeat(301), "first\nsecond", "first\u2028second"))
            rejects(() -> UserViewPolicy.reason(reason));
        assertEquals("Check permissions", UserViewPolicy.reason(" Check permissions "));
    }
    @Test public void Accounts_Untrusted_display_values_are_escaped() {
        assertEquals("&lt;script&gt;&amp;&#39;&quot;", UserViewPolicy.html("<script>&'\""));
        assertEquals("", UserViewPolicy.html(null));
    }
    @Test public void Retention_Organisation_cannot_shorten_global_retention() {
        var p = TableLifecycleService.combine(new TableLifecycleService.Policy(90,false,true,true), new TableLifecycleService.Policy(1,false,true,true));
        assertEquals(90, p.days());
    }
    @Test public void Retention_Indefinite_retention_and_delete_blocks_survive_overrides() {
        var p = TableLifecycleService.combine(new TableLifecycleService.Policy(30,true,false,false), new TableLifecycleService.Policy(365,false,true,true));
        assertTrue(p.forever()); assertFalse(p.allowDelete()); assertFalse(p.autoPurge());
    }
    @Test public void Retention_Automatic_purge_requires_every_scope_to_opt_in() {
        for (int mask=0; mask<4; mask++) {
            var p = TableLifecycleService.combine(new TableLifecycleService.Policy(30,false,true,(mask&1)!=0), new TableLifecycleService.Policy(60,false,true,(mask&2)!=0));
            assertEquals(mask==3, p.autoPurge());
        }
    }
    @Test public void Exports_Spreadsheet_formulas_are_neutralised() {
        for (String cell : List.of("=1+1", "+SUM(A1)", "-2+3", "@SUM(A1)", "  =1+1", "\t=1+1", "\n@SUM(A1)"))
            assertTrue(cell, TableLifecycleService.csvValue(cell).startsWith("\"'"));
    }
    @Test public void Exports_Quotes_newlines_and_unicode_are_preserved() {
        assertEquals("\"Leuven, \"\"North\"\"\nClub\"", TableLifecycleService.csvValue("Leuven, \"North\"\nClub"));
        assertEquals("\"Équipe fictive\"", TableLifecycleService.csvValue("Équipe fictive"));
        assertEquals("\"\"", TableLifecycleService.csvValue(null));
    }
}
