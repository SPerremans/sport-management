package myfirstmodule;

import org.junit.Test;
import static org.junit.Assert.*;
import java.time.LocalDate;
import java.util.*;

/** Pure ranking-rule checks (no database). They prove the arithmetic, not the pages or the stored data. */
public class RankingRulesTest {
    private static final RankingRules.Params P = RankingRules.Params.badmintonVlaanderen();
    private static final RankingRules.Table T = RankingRules.defaultTable(12);
    private static final LocalDate MOMENT = LocalDate.of(2026, 11, 2);

    private static List<RankingRules.Match> wins(int count, double opponent, double own) {
        List<RankingRules.Match> list = new ArrayList<>();
        for (int i = 0; i < count; i++) list.add(new RankingRules.Match(MOMENT.minusDays(7L * (i + 1)), true, opponent, own));
        return list;
    }

    @Test public void Points_follow_the_published_table() {
        assertEquals(313, T.points(7), 0.001); assertEquals(50, T.points(12), 0.001); assertEquals(2831, T.points(1), 0.001);
        assertEquals((313 + 217) / 2.0, T.points(7.5), 0.001); // doubles: pair average of the two opponents (unpublished reading)
    }

    @Test public void Default_scheme_has_twelve_levels() { assertEquals(12, RankingRules.defaultTable(12).levels()); assertEquals(13, RankingRules.defaultTable(13).winPoints().length - 1); }

    @Test public void Average_is_never_divided_by_fewer_than_seven() {
        var r = RankingRules.evaluate(8, wins(2, 8, 8), MOMENT, MOMENT, LocalDate.MIN, 0, List.of(), P, T);
        assertEquals(2 * 217 / 7.0, r.average(), 0.001);
    }

    @Test public void Only_the_twenty_most_recent_matches_in_52_weeks_count() {
        List<RankingRules.Match> list = wins(25, 8, 8);
        list.add(new RankingRules.Match(MOMENT.minusWeeks(60), true, 1, 8)); // outside the window
        var r = RankingRules.evaluate(8, list, MOMENT, MOMENT, LocalDate.MIN, 0, List.of(), P, T);
        assertEquals(20, r.counted()); assertEquals(25, r.matchesInWindow()); assertEquals(20 * 217 / 20.0, r.average(), 0.001);
    }

    @Test public void Promotes_at_most_one_level_per_evaluation() {
        var r = RankingRules.evaluate(8, wins(20, 4, 8), MOMENT, MOMENT, LocalDate.MIN, 0, List.of(), P, T);
        assertEquals(7, r.level()); assertEquals("Evaluation", r.kind());
    }

    @Test public void Demotes_one_level_when_the_average_is_too_low() {
        List<RankingRules.Match> list = new ArrayList<>();
        for (int i = 0; i < 10; i++) list.add(new RankingRules.Match(MOMENT.minusDays(7L * (i + 1)), i == 0, 8, 8));
        var r = RankingRules.evaluate(8, list, MOMENT, MOMENT, LocalDate.MIN, 0, List.of(), P, T);
        assertEquals(9, r.level());
    }

    @Test public void A_player_without_matches_is_not_demoted_by_the_average() {
        var r = RankingRules.evaluate(8, List.of(), MOMENT, MOMENT, LocalDate.MIN, 0, List.of(), P, T);
        assertEquals(8, r.level()); assertEquals("None", r.kind());
    }

    @Test public void Level_one_cannot_be_promoted_and_the_lowest_level_cannot_be_demoted() {
        assertEquals(1, RankingRules.evaluate(1, wins(20, 1, 1), MOMENT, MOMENT, LocalDate.MIN, 0, List.of(), P, T).level());
        List<RankingRules.Match> losses = new ArrayList<>(); losses.add(new RankingRules.Match(MOMENT.minusDays(7), false, 12, 12));
        assertEquals(12, RankingRules.evaluate(12, losses, MOMENT, MOMENT, LocalDate.MIN, 0, List.of(), P, T).level());
    }

    @Test public void Losses_against_a_directly_higher_level_do_not_count() {
        List<RankingRules.Match> list = wins(7, 8, 8);
        list.add(new RankingRules.Match(MOMENT.minusDays(1), false, 7, 8));
        assertEquals(7, RankingRules.evaluate(8, list, MOMENT, MOMENT, LocalDate.MIN, 0, List.of(), P, T).counted());
        list.add(new RankingRules.Match(MOMENT.minusDays(2), false, 9, 8)); // a loss against a lower level still counts
        assertEquals(8, RankingRules.evaluate(8, list, MOMENT, MOMENT, LocalDate.MIN, 0, List.of(), P, T).counted());
    }

    @Test public void Disciplines_of_one_player_stay_within_two_levels() {
        var r = RankingRules.evaluate(8, wins(20, 4, 8), MOMENT, MOMENT, LocalDate.MIN, 0, List.of(10), P, T);
        assertEquals(8, r.level()); // promotion to 7 is fine (10-7 = 3 > 2 blocks it)
    }

    @Test public void Bonus_counter_promotes_immediately_and_resets() {
        var r = RankingRules.evaluate(8, wins(12, 6, 8), MOMENT, MOMENT, LocalDate.MIN, 12, List.of(), P, T);
        assertEquals(7, r.level()); assertEquals("Bonus", r.kind()); assertEquals(12, r.bonusCounter());
        var after = RankingRules.evaluate(7, wins(12, 6, 7), MOMENT, MOMENT, MOMENT.minusDays(1), 12, List.of(), P, T);
        assertNotEquals("Bonus", after.kind()); // wins before the last level change do not count again
    }

    @Test public void Two_years_without_matches_drops_two_levels() {
        var r = RankingRules.evaluate(5, List.of(), MOMENT, MOMENT.minusWeeks(104), LocalDate.MIN, 0, List.of(), P, T);
        assertEquals(7, r.level()); assertEquals("Inactivity", r.kind());
        assertEquals(12, RankingRules.evaluate(11, List.of(), MOMENT, MOMENT.minusWeeks(110), LocalDate.MIN, 0, List.of(), P, T).level());
    }

    @Test public void First_mondays_and_sheet_parsing() {
        assertEquals(LocalDate.of(2026, 11, 2), RankingRules.firstMonday(2026, 11));
        assertEquals(12, RankingRules.nextFirstMondays(LocalDate.of(2026, 10, 29), 12).size());
        assertEquals(LocalDate.of(2026, 11, 2), RankingRules.nextFirstMondays(LocalDate.of(2026, 10, 29), 1).get(0));
        assertEquals(Integer.valueOf(7), RankingRules.bracketLevel("Nico Van Vliet (7)")); assertNull(RankingRules.bracketLevel("Nico Van Vliet"));
        assertEquals("nico van vliet", RankingRules.nameKey("Nico  Van Vliet (7)"));
        assertEquals("MD", RankingRules.discipline("Men", 2)); assertEquals("WS", RankingRules.discipline("Women", 1)); assertEquals("XD", RankingRules.discipline("Mixed", 2)); assertNull(RankingRules.discipline("Open", 0));
    }

    @Test public void Review_schedule_follows_the_chosen_frequency() {
        var start = java.time.LocalDateTime.of(2026, 11, 2, 9, 0);
        assertEquals(start, RankingRules.nextReview("Weekly", start, null));
        assertEquals(start.plusWeeks(1), RankingRules.nextReview("Weekly", start, start));
        assertEquals(start.plusWeeks(4), RankingRules.nextReview("EveryTwoWeeks", start, start.plusWeeks(2)));
        assertEquals(java.time.LocalDateTime.of(2026, 12, 7, 9, 0), RankingRules.nextReview("FirstMondayMonthly", start, start));
        assertEquals(java.time.LocalDateTime.of(2027, 2, 2, 9, 0), RankingRules.nextReview("Quarterly", start, start));
        assertEquals(java.time.LocalDateTime.of(2027, 11, 2, 9, 0), RankingRules.nextReview("Yearly", start, start));
        assertEquals(java.time.LocalDateTime.of(2026, 12, 2, 9, 0), RankingRules.nextReview("Monthly", start, start));
        assertEquals("every month on the first Monday", RankingRules.frequencyLabel("FirstMondayMonthly"));
        rejects(() -> RankingRules.nextReview("", start, null));
    }

    private static void rejects(Runnable r) { try { r.run(); } catch (IllegalArgumentException expected) { return; } fail("expected a validation error"); }

    @Test public void Calculation_shows_every_match_with_the_reason_it_counts_or_not() {
        List<RankingRules.Match> list = wins(22, 8, 8);
        list.add(new RankingRules.Match(MOMENT.minusDays(1), false, 7, 8, "Higher opponent (7)"));
        list.add(new RankingRules.Match(MOMENT.minusWeeks(60), true, 1, 8)); // outside the window: not listed
        var c = RankingRules.compute(8, list, MOMENT, P, T);
        assertEquals(23, c.details().size()); assertEquals(20, c.counted());
        assertEquals(1, c.details().stream().filter(d -> d.note().contains("directly higher")).count());
        assertEquals(2, c.details().stream().filter(d -> d.note().contains("20 most recent")).count());
        assertEquals(20 * 217.0, c.total(), 0.001); assertEquals(20, c.divisor(), 0.001); assertEquals(217.0, c.average(), 0.001);
        assertEquals("Higher opponent (7)", c.details().get(0).match().label());
    }

    @Test public void Test_values_change_the_outcome_without_touching_current_values() {
        double[] up = T.promoteAt().clone(), down = T.demoteBelow().clone(); up[8] = 1; // a draft table that promotes level 8 at an average of 1
        RankingRules.Table draft = new RankingRules.Table(12, T.winPoints(), up, down);
        var matches = wins(2, 8, 8);
        assertEquals(8, RankingRules.evaluate(8, matches, MOMENT, MOMENT, LocalDate.MIN, 0, List.of(), P, T).level());
        assertEquals(7, RankingRules.evaluate(8, matches, MOMENT, MOMENT, LocalDate.MIN, 0, List.of(), P, draft).level());
        assertEquals(217, T.winPoints()[8], 0.001);
    }

    // ------------------------------------------------------------------ RankingEngine: level names, methods and the replay

    private static RankingEngine.Game game(String date, String winner, String loser) { return new RankingEngine.Game(LocalDate.parse(date), "HE", List.of(winner), List.of(loser), Arrays.asList((Integer)null), Arrays.asList((Integer)null), "", ""); }

    private static List<RankingEngine.Game> ladder() {
        // Ann beats everybody, Cas loses to everybody: any sensible method separates them
        List<RankingEngine.Game> games = new ArrayList<>(); LocalDate day = LocalDate.of(2025, 1, 6);
        for (int week = 0; week < 40; week++) {
            games.add(game(day.plusWeeks(week).toString(), "Ann", "Bo")); games.add(game(day.plusWeeks(week).toString(), "Bo", "Cas")); games.add(game(day.plusWeeks(week).toString(), "Ann", "Cas"));
        }
        return games;
    }

    private static RankingEngine.Player player(RankingEngine.Result r, String name) { for (RankingEngine.Player p : r.table) if (p.name.equals(name)) return p; throw new AssertionError(name); }

    @Test public void Level_names_are_free_text_and_the_position_stays_internal() {
        RankingEngine.Config c = RankingEngine.preset("Elo", new String[]{"AA", "A", "B", "C"});
        assertEquals(4, c.levels()); assertEquals("AA", c.name(1)); assertEquals("C", c.name(4));
        assertEquals("7", new RankingEngine.Config().name(7));
    }

    @Test public void Level_names_are_read_from_one_line_and_may_not_repeat() {
        assertArrayEquals(new String[]{"AA", "A", "B"}, RankingEngine.parseNames("AA, A; B"));
        assertEquals(0, RankingEngine.parseNames("  ").length);
        assertThrows(IllegalArgumentException.class, () -> RankingEngine.parseNames("A, a"));
    }

    @Test public void Every_starting_point_separates_a_clear_winner_from_a_clear_loser() {
        for (String[] preset : RankingEngine.PRESETS) {
            RankingEngine.Config c = RankingEngine.preset(preset[0], new String[]{"AA", "A", "B", "C", "D", "E"});
            c.params = new RankingRules.Params(6, c.params.windowWeeks(), c.params.maxMatches(), c.params.minDivisor(), c.params.maxSpread(), c.params.inactiveWeeks(), 1, c.params.inactiveDrop(), c.params.ignoreHigherLosses());
            RankingEngine.Result r = RankingEngine.replay(ladder(), c);
            assertEquals(preset[0], 3, r.players);
            assertTrue(preset[0] + ": Ann " + player(r, "Ann").level + " Cas " + player(r, "Cas").level, player(r, "Ann").level < player(r, "Cas").level);
        }
    }

    @Test public void Elo_change_follows_the_formula_and_a_draw_of_ratings_gives_half_of_k() {
        assertEquals(10, RankingEngine.eloDelta(1000, 1000, true, 20, 400), 0.0001);
        assertEquals(-10, RankingEngine.eloDelta(1000, 1000, false, 20, 400), 0.0001);
        assertEquals(20 * (1 - 1.0 / 11), RankingEngine.eloDelta(1400, 1000, true, 20, 400) + 20 * (1 - 1.0 / 11) - RankingEngine.eloDelta(1400, 1000, true, 20, 400), 0.0001);
        assertEquals(20 * (1 - 10.0 / 11), RankingEngine.eloDelta(1400, 1000, true, 20, 400), 0.0001);
    }

    @Test public void Best_results_add_up_only_the_best_ones() {
        RankingEngine.Config c = RankingEngine.preset("TennisPyramid", new String[]{"A", "B", "C"}); c.bestCount = 2;
        RankingEngine.Player p = new RankingEngine.Player("x", "X", "HE"); p.level = 3;
        LocalDate m = LocalDate.of(2026, 1, 1);
        p.matches.add(new RankingRules.Match(m.minusDays(1), true, 1, 3, "")); p.matches.add(new RankingRules.Match(m.minusDays(2), true, 2, 3, "")); p.matches.add(new RankingRules.Match(m.minusDays(3), true, 3, 3, "")); p.matches.add(new RankingRules.Match(m.minusDays(4), false, 1, 3, ""));
        assertEquals(c.table.winPoints()[1] + c.table.winPoints()[2], RankingEngine.score(p, m, c), 0.001);
    }

    @Test public void Win_balance_weighs_losses_by_how_far_below_the_opponent_is() {
        RankingEngine.Config c = RankingEngine.preset("WinBalance", new String[]{"A", "B", "C", "D", "E"});
        RankingEngine.Player p = new RankingEngine.Player("x", "X", "HE"); p.level = 2; LocalDate m = LocalDate.of(2026, 1, 1);
        p.matches.add(new RankingRules.Match(m.minusDays(1), true, 1, 2, "")); p.matches.add(new RankingRules.Match(m.minusDays(2), true, 1, 2, "")); p.matches.add(new RankingRules.Match(m.minusDays(3), true, 1, 2, ""));
        p.matches.add(new RankingRules.Match(m.minusDays(4), false, 2, 2, "")); p.matches.add(new RankingRules.Match(m.minusDays(5), false, 3, 2, "")); p.matches.add(new RankingRules.Match(m.minusDays(6), false, 5, 2, "")); p.matches.add(new RankingRules.Match(m.minusDays(7), false, 1, 2, ""));
        assertEquals(3 - 1 - 2 - 5, RankingEngine.score(p, m, c), 0.001); // a loss to a higher level costs nothing
    }

    @Test public void A_pyramid_fills_the_levels_by_share_and_the_top_stays_small() {
        RankingEngine.Config c = RankingEngine.preset("TennisPyramid", new String[]{"A", "B", "C"}); c.shares = new double[]{0, 10, 30, 60}; c.maxUp = 0; c.maxDown = 0; c.bestCount = 100;
        c.params = new RankingRules.Params(3, 52, 1000, 1, -1, 104, 1, 0, false);
        List<RankingEngine.Player> players = new ArrayList<>(); LocalDate m = LocalDate.of(2026, 1, 1);
        for (int i = 0; i < 10; i++) {
            RankingEngine.Player p = new RankingEngine.Player("p" + i, "P" + i, "HE"); p.level = 3;
            for (int w = 0; w <= i; w++) p.matches.add(new RankingRules.Match(m.minusDays(w + 1), true, 1, 3, ""));
            p.matches.add(new RankingRules.Match(m.minusDays(20), false, 1, 3, "")); players.add(p);
        }
        RankingEngine.review(players, m, c);
        assertEquals(1, players.get(9).level); assertEquals(2, players.get(8).level); assertEquals(2, players.get(6).level); assertEquals(3, players.get(5).level); assertEquals(3, players.get(0).level);
    }

    @Test public void Moves_per_review_are_limited() {
        RankingEngine.Config c = RankingEngine.preset("TennisPyramid", new String[]{"A", "B", "C", "D"}); c.shares = new double[]{0, 100, 0, 0, 0}; c.maxUp = 1; c.maxDown = 1;
        c.params = new RankingRules.Params(4, 52, 1000, 1, -1, 104, 1, 0, false); LocalDate m = LocalDate.of(2026, 1, 1);
        RankingEngine.Player p = new RankingEngine.Player("p", "P", "HE"); p.level = 4; p.matches.add(new RankingRules.Match(m.minusDays(1), true, 1, 4, ""));
        RankingEngine.review(List.of(p), m, c); assertEquals(3, p.level);
    }

    @Test public void A_replay_never_looks_ahead_when_it_judges_a_game() {
        // before their first review both players are level 12, so the first games count as equal levels whatever happens later
        RankingEngine.Config c = RankingEngine.preset("Elo", null);
        List<RankingEngine.Game> games = new ArrayList<>(ladder().subList(0, 3));
        RankingEngine.Result r = RankingEngine.replay(games, c);
        assertEquals(3, r.equalGames); assertEquals(0, r.decided);
    }

    @Test public void Printed_levels_start_a_player_and_are_compared_later() {
        RankingEngine.Config c = RankingEngine.preset("BadmintonAverage", null); List<RankingEngine.Game> games = new ArrayList<>(); LocalDate day = LocalDate.of(2025, 1, 6);
        for (int i = 0; i < 8; i++) games.add(new RankingEngine.Game(day.plusWeeks(i), "HE", List.of("A"), List.of("B"), List.of(4), List.of(9), "", ""));
        RankingEngine.Result r = RankingEngine.replay(games, c);
        assertEquals(4, player(r, "A").startLevel); assertEquals(9, player(r, "B").startLevel); assertTrue(r.publishedSeen > 0); assertTrue(r.exactRate() >= 0);
    }

    @Test public void Walkovers_are_left_out_unless_the_design_counts_them() {
        List<RankingEngine.Game> games = List.of(new RankingEngine.Game(LocalDate.of(2025, 1, 6), "HE", List.of("A"), List.of("B"), Arrays.asList((Integer)null), Arrays.asList((Integer)null), "W", ""));
        RankingEngine.Config c = RankingEngine.preset("Elo", null); assertEquals(0, RankingEngine.replay(games, c).games); assertEquals(1, RankingEngine.replay(games, c).skippedWalkovers);
        c.countWalkovers = true; assertEquals(1, RankingEngine.replay(games, c).games);
    }

    @Test public void Pasted_lines_are_read_and_bad_lines_are_reported() {
        List<String> errors = new ArrayList<>();
        List<RankingEngine.Game> games = RankingEngine.parseLines("date;discipline;winner;partner;loser;partner;score\n2025-03-01;HE;Ann Peeters (5);;Bo Maes (6);;21-10 21-12\n2025-03-02;HD;A;B;C;D;wo\nnot a date;HE;A;;B;;\n2025-03-03;HD;A;B;C;;", errors);
        assertEquals(2, games.size()); assertEquals(Integer.valueOf(5), games.get(0).winnerLevels().get(0)); assertEquals("Ann Peeters", games.get(0).winners().get(0)); assertEquals("W", games.get(1).flag()); assertEquals(2, errors.size());
    }

    @Test public void An_empty_selection_of_disciplines_warns_instead_of_failing() {
        RankingEngine.Config c = RankingEngine.preset("Elo", null); c.disciplines.add("XX");
        RankingEngine.Result r = RankingEngine.replay(ladder(), c); assertEquals(0, r.games); assertFalse(r.warnings.isEmpty());
    }
}
