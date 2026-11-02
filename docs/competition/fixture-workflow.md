# Fixture workflow: from game leader to confirmed result

The **Register match** page (`Fixture_Edit`) follows five stages. The line under the title always shows the current one.

| Stage | Who | What happens on the page |
| --- | --- | --- |
| 1. Appoint | Home captain or home club administrator (federation and platform administrators can too) | Claim the captain role with **I am the home / away captain**, then enter the game leader's sign-in name and press **Appoint game leader**. |
| 2. Check setup | Game leader | Both captains pick their players in the games table (dropdowns in the Home / Away columns; the row order is the playing order). The leader sees both sides, corrects them, fills in **Starting hour** and **Match ball / shuttle**, presses **Save setup**, then **Start match**. |
| 3. Match | Game leader; everyone else reads | Set scores are typed in the same table. A score saves when the cursor leaves the away box of a set. Everyone who can read the fixture follows along. |
| 4. Finalise and confirm | Game leader, then both captains | **Finalise result** locks the scores. Each captain presses **Confirm result** (or **Dispute result** with a comment). A captain who is also the game leader has confirmed already. |
| 5. Final | Federation or platform administrator | **Federation ruling** can overrule at any time, with a mandatory reason. |

Comments from either team and from the officials are listed under the games table (**Comments**) and can be added at any stage by the captains, team managers and officials.

## Who can change what

`MatchRegistrationService.controls` is the "runs the fixture" rule: the game leader, a club administrator of either team (club *Update* permission), federation staff (*Manage* or *Override*) and platform administrators. It decides who can set up, start, change players after the start, enter scores and finalise.

- Before the start a side's players are picked by that side's captain or club administrator, the game leader, or federation/platform staff. A club administrator does not see the opposing lineup before the start; only an independent game leader, the federation or a platform administrator does.
- After the start only `controls` can change players. A comment is required (the **Comment for a change or dispute** box), the game must not be played yet, and the change is audited as *Players changed after the start*. The old and new pairing show in the audit trail, and the games table shows the comment under the result.
- After finalising, only federation override or a platform administrator can correct scores or players.

## Changes compared with the previous page

- The long text block (state, result, official winner, points, deadlines, warnings) is replaced by the stage line and a one-line summary under the games.
- Removed: the warnings checkbox (pressing **Start match** or **Finalise result** is the acknowledgement; the warnings are shown above Start and stored in the audit), the **Refresh fixture** button, separate home/away accept and dispute buttons, and the per-row **Save** and **Details** buttons.
- The generic microflow `ACT_Match_Command` (Competition/Results) runs a service command by name for the page buttons: ClaimHome, ClaimAway, AppointLeader, SaveSetup, Start, AddComment, Confirm, DisputeResult.
- Fixture detail page: back link at the top, one `nexio-page-actions` row of secondary buttons under the title (open match registration, propose date change, federation placement); the loose Close button is gone.
- Retirements, forfeits, exclusions and tie-breaks still use the game page (`MatchContest`).

## Storage

`MatchRegistration` gains `StartTime` (HH:mm) and `Equipment` (free text, up to 100 characters; "Match ball / shuttle"). `MatchLineup.ChangeNote` keeps the latest after-start comment. The new private entity `MatchComment` (side, author, message, time, association `MatchComment_Registration`) has no client access and is reached only through the service.

## Development data

`DevelopmentSamples` adds `features:fixture-workflow-v1` once, to fresh and existing batches: every owned, started sample registration with no starting hour gets one (taken from its start time) and a sample ball/shuttle, and up to two of them get a home and an away sample comment. `MatchComment` is in the ownership list and the cleanup edges, so **Remove sample data** deletes the comments with their registration. Edited registrations keep their values; a second run changes nothing.

## Result, winner and rulings

- The fixture result and winner are calculated only when the game leader presses **Finalise result**. Before that the page only says how many games are still to finish.
- The game leader can finalise only when every game is decided: played, awarded, or excluded by the federation. A fixture result counts game points: the side that won more games wins; excluded games do not count, so fewer than all games can count.
- Federation and platform administrators overrule at game level, in the games table: tick games (or **Select all**) and press **Award to home**, **Award to away**, **Exclude from result** or **Restore played result**. This replaces the former fixture-level adjustment and winner text fields. On an already finalised or disputed fixture the result is recalculated and becomes the federation's final ruling.

## Comment popup

Actions that need a reason (player change after the start, dispute, award, exclude) open the reusable **Comment dialog** instead of showing an error. The comment is mandatory, is stored with the action and appears in the audit trail.

- Page `Comment_Edit` and non-persistent entity `CommentDialog` (title, prompt, comment, action key) are generic. Another screen opens it by creating a `CommentDialog`, then adds its own continuation inside `ACT_CommentDialog_Confirm` and `ACT_CommentDialog_Cancel`; today they run the match command named by the action key.
- Match specific: `ACT_Match_ReasonOpen` opens the dialog; `ACT_Match_PickAskHome` / `ACT_Match_PickAskAway` open it for player changes after the start and otherwise pick directly.
- Housekeeping for Studio Pro: `Comment_Edit` and the `ACT_CommentDialog_*` microflows currently sit in Competition/Results and can be dragged to a shared folder. `ACT_Match_ReasonConfirm` and `ACT_Match_ReasonCancel` are obsolete leftovers and can be deleted.

## Audit trail

The audit table has separate **When**, **Who**, **Action** and **Details** columns. **When** is stored as a timezone independent date and shown in the viewer's local time, formatted like 29 Oct 2026, 11:46. The grid refreshes every 5 seconds, which also lets other viewers follow the registration.

## Errors

The registration microflows (`ACT_Match_Command`, `ACT_Match_GameSave`, `ACT_Match_GamePickHome/Away`) catch service errors, reload the page data and show the real message, for example "Tick the games to overrule first." instead of "An error occurred".

## Verification

- **Model consistency:** the global check reports **No errors found**.
- **Compilation:** `MatchRegistrationService.java` and `DevelopmentSamples.java` compile with JDK 21 against the Mendix 11.15 runtime jars (standalone `javac`), and the app builds and starts in Studio Pro.
- **Runtime scenarios, 29 October 2026, local Admin and View as user.** Executed and working:
  - Setup flow on the badminton and tennis demo fixtures: claiming captain, appointing the leader, saving the starting hour, starting the match, set scores per game, comments, finalising.
  - Player change after the start: without a comment it was refused with a readable message; with a comment (through the popup) it changed the pairing, showed the note and was audited.
  - Federation mass rulings: select all, award to home, exclude two games, finalise (result 6–0 in game points, winner Home); award on a disputed fixture turned it into the federation's final ruling.
  - Away captain dispute through the popup; plain User leader (demo_user) finalising without being a captain and without seeing federation buttons.
  - Sample data generator under the Admin session: the upgrade added the fixture-workflow examples once (10 started fixtures, 4 comments); a second run changed nothing.
- **Not exercised:** the away captain pressing **Confirm result** (the dispute used the same fixture), club administrator and federation administrator sessions (no enabled test accounts exist: the generated organisation accounts are disabled and View as user cannot open disabled accounts), a read-only viewer without any role on the fixture (the available User account has no club, so no navigation path leads to the fixture), and **Remove sample data** (not run, to keep the demo data).
- **Defects found and fixed while testing:** text limits, stale response data, read-only games list, member names, 0–0 set boxes, comments missing in the response, finalise gate, error messages, selection lost after a refused action, and the audit grid not refreshing.

## One fixture page (29 October 2026)

The former **Fixture** detail page and the separate **Register match** page are one page: `Fixture_Edit`, titled **Fixture**. Every table that has a fixture **View** action (league workspace, poule standings, competition overview) now opens it through `ACT_TablePage_CompetitionFixture_Detail` → `ACT_Match_FixtureEntry`. `CompetitionFixture_Detail` was deleted on 31 October 2026.

- **Action bar:** all buttons sit in one `nexio-page-actions` row under the title: Propose date change, Federation placement / override, I am the home/away captain, Appoint game leader, Save setup, Start match, Finalise result, Confirm result, Dispute result. Each shows only when its stage and the user's rights allow it, and the one next step is primary. The leader name and setup fields stay in a panel below; the comment box and Add comment button stay in the comments section; the federation game toolbar stays on the games table.
- **Facts line:** start (shown in the viewer's local time), venue and status of the fixture, from `MatchForm.StartsAt`, `Venue`, `FixtureStatus`; the buttons for date change and placement read the fixture through the association `MatchForm_CompetitionFixture`.
- **Viewers:** anyone who may read the fixture can open the page. Only the people who may start a registration (federation management, team managers, platform administrators) create one by opening it; for everybody else a fixture without a registration shows "No match registration yet" and an empty games area (`fixtureReadOnly`).
- **Federation edits in this view:** after the result is finalised, federation override and platform administrators still see editable players and score boxes in the games table. Saving a score or changing a player then opens the comment dialog; the correction recalculates the result as a federation final result and is audited as *Federation score correction*. Score saves go through `ACT_Match_GameSaveAsk`, which asks for the comment only on a finalised fixture.
- **Imported public sheets** stay read only.
- **Shareable link:** the page URL is `fixture` with two optional parameters, `LeagueSlug` and `MatchSlug`, for example `/p/fixture?LeagueSlug=<division>&MatchSlug=<home>-<away>-<date>`. Both are readable text built from the division and team names (no ids or guids). The page data source `DS_Match_FixtureForm` resolves them (`OpenFixtureLink`) and applies the normal read access. Studio Pro does not accept primitive path segments for required parameters, so the values travel in the query string. Opening a fixture from any table (`LinkFixture`) sets the slugs on the form; `fixtureFacts` also sets them, so the legacy `ACT_Match_*` flows still reopen the same page.
