# Individual rankings

A federation can define an **individual ranking scheme** for its sport. A scheme exists only where a federation defines one: the generated football federations have none, and a federation without a scheme simply shows **No ranking scheme** with a **Create** button.

Open it from the **Rankings** menu item (`ACT_Ranking_Open`, page `Competition/Rankings/RankingScheme_Edit`). Platform administrators see every federation; other users see the federations where they have **Manage** permission (`SUB_Competition_FederationPermission`). Every command is rechecked on the server.

## Design and simulation

A ranking is no longer fixed on numbered levels or on one sport. On **Create** a federation chooses level names and a starting point; **Edit** in the list and **Design and simulate** on the Levels tab open the design page (`RankingDesign_Edit`). Why it looks like this: [ranking-design-research.md](ranking-design-research.md). What to test: [ranking-design-test-plan.md](ranking-design-test-plan.md).

- **Level names** are free text per level. The position (1 is the highest) stays internal and is what the sheets print and the rules calculate with.
- **Calculation method**: average points per match, sum of the best results, wins minus weighted losses, or a running rating (Elo). The rule list shows only what applies to the method.
- **Placement**: promotion and demotion scores per level, or a fixed share of the players per level (a pyramid).
- **Movement and inactivity**: most levels up and down per review, largest difference between disciplines (can be off), games needed to be active, waiting time and levels lost (can be none).
- **Starting points**: six presets inspired by Badminton Vlaanderen, BWF, Belgian tennis, French tennis, FIDE and German table tennis. Their numbers are editable examples, not the official systems.
- **Game data**: paste up to three years of games (older ones are left out, and unreadable lines are reported) or copy the federation's registered results. Stored per scheme as one `RankingDataset`.
- **Simulation**: replays the games in date order with reviews on the chosen frequency. A game is judged only with the levels known before it. Shows measures, levels at the start and end, every review, win rate per level gap, players with their path, one player's games, and warnings in plain words.
- **Scenarios**: save a design with its result and compare with a baseline.
- **Apply**: asks for a reason, then makes the design the ranking's configuration. Player levels stay until the next review, except on a removed level; a scheme without players gets the levels of the simulation.

Nothing in the design changes a player until **Apply**. Reviews of a scheme with another method, a pyramid or moves of more than one level run through the same engine as the simulation.

## Pages

- **Rankings list** (`RankingScheme_Overview`, menu item **Rankings**): one row per federation, with its scheme or **No ranking scheme**. Toolbar: **Create**, **View**, **Edit**, **Delete**. **Double-click a scheme** (or select it and press **View**) to open it on its own page; the list is not reloaded.
- **Scheme page** (`RankingScheme_Edit`): tabs **Reviews**, **Levels and points**, **Disciplines**, **Players**, **Changes and simulation**. Double-click a player to open the player page, double-click a review to open it.
- **Review page** (`RankingReview_View`): every player of a review with **Promoted**, **Demoted** or **Stays**, the from and to level, average, matches and reason. Select a player and press **View scoring** (or double-click) for the calculation.
- **Player page** (`RankingHistory_View`): current level per discipline, **how the average is calculated** (every match of the window with opponents and their level at the match, result, opponent level, points and whether it counts, then the total, the divisor and the average with the promotion and demotion thresholds), and the level changes. Opened from a review it calculates as of that review and with its values (the current values, or the values of the simulation).

## What a federation can do

| Need | Where | Notes |
| --- | --- | --- |
| Define a ranking for a federation and sport | List: select a federation without scheme, **Create** | Creates the scheme with the published points, five disciplines, 12 levels and a monthly schedule that is not automatic |
| Set when rankings are reviewed | **Reviews** tab: **Edit schedule** | First review date, hour (UTC), **frequency** from a drop-down (every week, every two weeks, every month on the same day, every month on the first Monday, every three months, every year) and whether reviews run automatically |
| See what reviews did | **Reviews** tab log | The latest 5 reviews (when, scheduled/manual/simulation, players reviewed, promoted, demoted, unchanged, by). **Show more** adds 10; **Filter** shows every review between two dates or a chosen number |
| Preview the upcoming review | **Reviews** tab: the first row is the **Upcoming review**; select it and press **View** | Shows every player as promoted, demoted or staying, calculated with the current values, without writing anything |
| Simulate before applying | **Simulate** (current values) or **Simulate test values**, with the upcoming review or an earlier simulation selected | Writes changes with state *Simulated* (see **Changes and simulation**) and a log row; no player changes. A new simulation with the same values replaces the earlier one; **Clear simulation** removes all. Select the simulation and press **View** for the players, then **View scoring** for one player's calculation with the simulated values |
| Apply a review | Automatic at the scheduled moment (when *run automatically* is yes), or **Run review now** | Updates levels, averages and bonus counter, writes *Applied* changes and a review log entry. Automatic reviews are checked once a minute while the app runs and catch up at most three missed reviews per check |
| Forced upgrade or downgrade | **Players** tab: select one player, **Force change** | Dialog with the new level and a **mandatory comment**, stored on the history entry (kind *Forced*). It may exceed the maximum level difference between disciplines; the entry says so |
| Change the disciplines | **Disciplines** tab: **Create**, **Edit**, **Deactivate**, **Activate** | A discipline is a code, a name, the **match slot** on the sheets (Men, Women, Mixed or Open), players per side (1 or 2) and a bonus threshold. Matches are attached to the active discipline with that slot and player count, so a federation can add a discipline or retire one over time; deactivating keeps all rankings and history but stops counting its matches. Use **Initialise players** after adding one |
| Change points, thresholds and the number of levels | **Levels and points** tab: edit the cells in the table; **Create** adds a lowest level, **Delete** removes the selected lowest level | There is no edit dialog. Every edit is saved as a **test value**; the current values stay in use and the *Values* column shows *Changed (test value)* or *New level*. Edited points lose the *Published* mark |
| Try test values | **Simulate test values** (Levels tab, or Reviews tab with the upcoming review or a simulation selected) | Simulates the upcoming review with the test values; the simulation is labelled *test values* in the log and on the review page |
| Approve or discard test values | **Approve** / **Discard** (Levels tab) | **Approve** makes the test values the real values (players on a removed lowest level move up and the move is written to their history, kind *Levels*) and clears simulations; **Discard** drops them. Without test values the old values stay. **Run review now** and automatic reviews always use the current values |
| Change evaluation parameters | List: select a scheme, **Edit** | Window weeks, matches counted, minimum divisor, maximum level difference, inactivity period, matches that count as active, levels lost, ignore losses against a directly higher level |
| Initialise players | **Players** tab: **Initialise players** | Adds a ranking for every player in the federation's match results who has none yet in an active discipline: the level in brackets on the match sheet (for example `Nico Van Vliet (7)`), else the lowest level. Existing rankings are never changed |

The **Players** tab shows **Latest change**: the date of the player's most recent applied change in that discipline (the initial placement counts when nothing changed since).

## Rules and sources

Basis: <https://badminton.vlaanderen/competitie/rankings/> and its bonus-points sub page. The numbers below are **sample defaults that stay editable**; the right column says what is published.

| Rule | Default | Published? |
| --- | --- | --- |
| 12 levels, 12 lowest, 1 highest; new player starts at 12 | yes | published |
| Separate rankings per discipline | men's/women's singles, men's/women's doubles, mixed doubles | published. The public match sheets do not show the gender of a mixed player, so mixed doubles is **one** pool here |
| Levels of one player differ by at most 2 | 2 | published |
| Points for a win by opponent level (singles) | 1=2831, 2=1961, 3=1359, 4=942, 5=652, 6=452, 7=313, 8=217, 9=150, 10=104, 11=72, 12=50 | published |
| A loss earns 0 points | yes | published |
| Doubles/mixed points | mean of the points of the two opponents' levels | **not published**: sample reading of "pair average" |
| Average = points over valid matches; at most the 20 most recent matches in the last 52 weeks; divided by at least 7 | 20 / 52 / 7 | FAQ summary, unverified |
| Promotion/demotion by at most one level per evaluation | yes | published |
| Evaluation on the first Monday of each month | schedule frequency *every month on the first Monday*; the frequency is configurable | published for Badminton Vlaanderen |
| Promotion average per level | 55 % of the win points of the next level up | **not published**: sample default |
| Demotion average per level | 30 % of the win points of the own level; only for players with at least as many matches in the window as the inactivity threshold (3); fewer matches is handled by the inactivity rule | **not published**: sample default |
| Losses against an opponent of a directly higher average level no longer count | opponent average level above the player's and at most 1 level apart | published in principle, the exact reading is **not published** |
| Inactivity: fewer than 3 matches in 52 weeks is inactive; 104 inactive weeks cost two levels | 3 / 104 / 2 | FAQ summary, unverified |
| Bonus counter per discipline, promotion when it reaches a threshold, counter resets | men singles 12, doubles 24, mixed 24; women singles 8, doubles 16 | published thresholds. **What increments the counter is not published**: here a win against an opponent of a higher (lower number) level, counted after the player's last level change |
| A player can request a review at any time | not implemented: a federation uses **Force change** | out of scope |

Levels shown on public match sheets are levels at match time. They seed the initial rankings and are used as the opponent level in later calculations; when a name carries no level the current ranking level is used.

Bonus promotion is checked whenever an evaluation is simulated or applied. It is **not** triggered live by a single match.

## Data model

Persistent, with **no client access** (only the Java service reads and writes them): `RankingScheme` (federation, sport, parameters, schedule start, frequency, automatic yes/no, last review), `RankingDiscipline` (code, name, match slot, players per side, bonus threshold, active), `RankingLevel` (a row with *IsDraft* is a test value), `RankingMoment` (one **review**: when, scheduled/manual/simulation, basis current/test values, players reviewed, promoted, demoted), `PlayerRanking` (scheme, discipline, player key and name, level, average, matches, bonus counter, last active) and `RankingChange` (the history: kind Seed/Evaluation/Bonus/Inactivity/Forced/Levels, state Applied/Simulated, from/to level, average, matches, reason, mandatory comment for forced changes, actor, time). Associations: `RankingScheme_Federation`, `RankingDiscipline_Scheme`, `RankingLevel_Scheme`, `RankingMoment_Scheme`, `PlayerRanking_Scheme`, `PlayerRanking_Discipline`, `RankingChange_Scheme`, `RankingChange_PlayerRanking`, `RankingChange_Moment`.

New for the design: `RankingLevel.LevelName` and `SharePercent`, `RankingScheme.ConfigJson`, `RankingDataset` (games as JSON, `RankingDataset_Scheme`) and `RankingScenario` (design and result as JSON, one working design per scheme, one baseline, `RankingScenario_Scheme`). Both have no client access.

Non-persistent view: `RankingForm` (page state, edit dialog values), `RankingRow` (grid rows by kind), `RankingState` (toolbar flags).

Players are identified by name (without the level in brackets), because the imported public sheets have no accounts; a ranking is one row per scheme, discipline and name.

## Code

- `javasource/myfirstmodule/RankingEngine.java`: pure calculation of all methods, placement, presets, replay, measures, warnings and reading pasted games.
- `javasource/myfirstmodule/RankingWorkbench.java`: the design page: configuration JSON, data sets, scenarios, commands, rows and apply.
- `docs/competition/tests/RankingReplayCheck.java`: every starting point on the frozen public data. `docs/development/build-ranking-history.py` builds that data.
- `javasource/myfirstmodule/RankingRules.java`: pure rules (points, average, evaluation, bonus, inactivity, first Mondays).
- `javasource/myfirstmodule/RankingRulesTest.java`: 13 JUnit checks of those rules.
- `javasource/myfirstmodule/RankingService.java`: commands, simulation/apply, forced change, rows and toolbar flags. Reached through `MatchRegistrationBridge` (`Ranking|<command>|<form id>|<ids>`) and the existing `JA_MatchRegistration` Java action.
- Microflows in `Competition/Rankings`: `ACT_Ranking_Open`, `ACT_Ranking_Do` (toolbar button), `ACT_Ranking_Select` (selection change), `ACT_Ranking_OpenRow` (double-click), `ACT_Ranking_Run`, `ACT_Ranking_Cell` (an edited level cell), `ACT_Ranking_Save`, `ACT_Ranking_ShowScheme`, `ACT_Ranking_ShowEdit`, `ACT_Ranking_ShowHistory`, `ACT_Ranking_ShowReview`, `DS_Ranking_State`, `DS_Ranking_RowsOfKind`. Enumeration `RankingFrequency`.
- Pages: `RankingScheme_Overview` (list), `RankingScheme_Edit` (one scheme), `RankingReview_View`, `Ranking_Edit` (generic edit dialog, up to 10 values, the frequency drop-down and the comment), `RankingHistory_View`.
- Automatic reviews: `RankingService.startScheduler` is started from the private startup action and checks once a minute (`runDue`).

### Deviations from the shared standards

- The forced-change comment is a mandatory field inside `Ranking_Edit`, checked on the server and written to the history. It does **not** use `Comment_Edit`: that page and `ACT_CommentDialog_*` were being changed by another session, so the shared dialog was left untouched. Moving the comment to the shared dialog needs one new branch in `ACT_CommentDialog_Confirm`.
- The history page grids have no selection checkboxes because they have no toolbar actions.

## Development data

`DevelopmentSamples` adds `features:individual-rankings-v3` once to fresh and existing batches (`extendRankingSamples`): for the public **Badminton Vlaanderen vzw** federation one scheme with the published points, five configurable disciplines, the players found on the imported sheets seeded at the level shown on the sheet (else 12), a **monthly review schedule that is not automatic** (so the demo rankings do not change by themselves) a **simulation** (not applied) of the next review with the current values, and **sample test values** (promotion and demotion averages 20 % lower, not approved) with a second simulation. Football and the other federations get no scheme. Existing batches of the earlier versions are upgraded (test values and their simulation are added once; existing or edited test values are kept): empty schedule and discipline fields are filled, planned moments (the first version's design) are replaced by the schedule and a new simulation; applied reviews, forced changes and edited values are kept. The six ranking types and their owning associations are in the cleanup allowlist and dependency map, and every created record is in the ownership manifest, so **Remove sample data** removes them with the federation. A second run changes nothing; an existing scheme is never replaced.

## Development data, design examples

`features:individual-rankings-v4` is added once to fresh and existing batches (`extendRankingDesignSamples`), from the frozen public resource `resources/ranking-history-public.json`, with no runtime calls to providers. The badminton sample scheme gets the Antwerp provincial league as game data and three scenarios, compared with *Average points, as published*; no badminton player changes level. A new **[DEMO] Belgian club tennis** federation shows named levels and a pyramid, with game data from public Belgian club tournaments, two scenarios and player rankings from the simulation. Data sets, scenarios and the federation are in the ownership manifest, the cleanup allowlist and the dependency map. An existing data set or edited design is kept. Youth events are not captured.

## Verification of the design

Reported separately; nothing here is claimed beyond what was executed on 31 October 2026.

- **Model consistency:** Studio Pro diagnostics report no errors in the whole app after the last model edit.
- **Compilation:** `RankingRules`, `RankingEngine`, `RankingService`, `RankingWorkbench`, `DevelopmentSamples`, `MatchRegistrationBridge`, `RankingRulesTest` and `RankingReplayCheck` compile with standalone JDK 21 against the Mendix 11.15 jars. This is not a Studio Pro build.
- **Rule checks that ran:** `RankingRulesTest`, 29 JUnit tests, all passing (13 new for the engine). They test arithmetic only.
- **Offline replay that ran:** `RankingReplayCheck`, all six starting points on both public data sets, including a repeat through the stored configuration: same result every time, and every player is counted once at the start and end. This proves the calculation on real games, not the pages.
- **Runtime scenarios:** **none were executed.** The pages, microflows, dialogs, sample generation, Apply and cleanup have not run. See [ranking-design-test-plan.md](ranking-design-test-plan.md).

### What the replay showed

Antwerp provincial badminton league, 12,511 games, 822 people, 2,064 player-discipline rankings, compared with the level printed on the sheets:

| Starting point (12 levels) | Up | Down | Better level won | Brier | Changes per player per year | Same as sheet | Within one |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| Average points, current sample values | 659 | 2,507 | 74.0 % | 0.195 | 0.71 | 17.1 % | 60.4 % |
| Running rating, K 20, scale 400 | 758 | 697 | 75.6 % | 0.188 | 0.33 | 43.9 % | 85.6 % |
| Running rating, K 16, scale 150 | 1,022 | 961 | 76.4 % | 0.184 | 0.45 | 43.2 % | 87.6 % |
| Best 6 in a pyramid | 1,432 | 5,202 | 71.7 % | 0.205 | 1.49 | 12.8 % | 39.7 % |
| Best 10 in a pyramid, weekly | 6,082 | 7,337 | 69.4 % | 0.214 | 3.01 | 10.4 % | 30.4 % |
| Wins minus weighted losses | 3,868 | 4,744 | 68.8 % | 0.217 | 1.93 | 25.9 % | 50.6 % |

The **current sample thresholds demote about four times as often as they promote**, and end up further from the printed levels than a plain running rating. They were never published, so this says those sample values are off, not that the federation's method is wrong. A likely cause: with fewer than 7 games the divisor stays 7, so a player with few games has a low average. The pyramid figures depend on shares that are not the sport's real ones. These are one provincial league, and the printed level is itself the outcome of a system that sees more games than this data holds.

Belgian club tennis, 1,039 games, 614 people, 731 rankings: too few games per player for a strong result. With everybody starting at the lowest level, threshold methods barely move anyone (98 % stay on the lowest level), and the pyramid is what spreads players. The warnings on the page say this. It is also the argument for offering pyramids at all.


## Verification

The rounds below are of version 3 and do not cover the design.


Reported separately; nothing here is claimed beyond what was executed. Third round (version 3, test values and review view): 29 October 2026, signed-in Admin browser session on the local app.

- **Model consistency:** Studio Pro diagnostics report **No errors found** after the last model edit (new attributes `IsDraft`, `Basis`, `ReviewKey`, state flags; `ACT_Ranking_Cell`, `ACT_Ranking_ShowReview`; page `RankingReview_View`; reworked Reviews and Levels tabs). Update security was pressed after the domain change.
- **Compilation:** `RankingRules`, `RankingService`, `DevelopmentSamples`, `MatchRegistrationBridge` and `RankingRulesTest` compile with JDK 21 against the Mendix 11.15 jars; Studio Pro's build started the app.
- **Rule checks that ran:** `RankingRulesTest`, 16 JUnit tests, all passing (the new one shows that a different threshold table changes the outcome without touching the other table). They test arithmetic only.
- **Runtime scenarios executed (version 3):**
  - **Generator upgrade** of the existing batch ran ("Sample set ready"): the scheme shows the **Upcoming review**, a *Simulation, current values* (21 promoted, 579 demoted of 2,772) and a *Simulation, test values* (46 promoted, 438 demoted).
  - **Review page** of the test-values simulation: promoted, demoted and stays with from/to level, average, matches and reason. **View scoring** opened the player page; its thresholds were the test values (for example promotion at 414.4 instead of 518) and the text says the calculation follows the simulation.
  - **Upcoming review view** with current values: 21 promoted, 579 demoted, 2,172 stay.
  - **Levels tab:** the cells are editable inputs (after setting the data view editable). Editing the points of level 12 from 50 to 55 was saved; **Simulate test values** then gave 437 demoted instead of 438. **Create** added level 13 (*New level*), **Delete** with its confirmation removed it again.
- **Defect found and fixed during this round:** the level cells were read-only because the surrounding data view was not editable.
- **Not run in the app:** **Approve** and **Discard** (they would replace or drop the sample test values), **Run review now**, automatic reviews, an edit with invalid text, **Show more**, **Filter** and the discipline, schedule, force-change and scheme dialogs in this version, **Remove sample data**, and any run as a federation administrator or plain user. Results of earlier rounds for those dialogs apply to version 2 only.
- **Known weakness, not changed:** every refresh rebuilds one view row per player and per change; with 2,772 players the console warns about ~3,300 `RankingRow` objects and pages take several seconds. The review page builds one row per player as well.
