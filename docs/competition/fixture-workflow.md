# Fixture workflow: from game leader to confirmed result

The **Register match** page (`MatchFixture`) follows five stages. The line under the title always shows the current one.

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

## Verification

- **Model consistency:** the global check reports one error, *Entity access is out of date*. The domain model editor's **Update security** button in Studio Pro clears it; the connector cannot press it. Pages, microflows and associations report no errors.
- **Compilation:** `MatchRegistrationService.java` and `DevelopmentSamples.java` compile with JDK 21 against the Mendix 11.15 runtime jars (standalone `javac`, not a Studio Pro build).
- **Runtime scenarios:** not run. Still to check in the app: appointing as home captain, both captains picking, the leader correcting both sides, start-hour validation, auto-save of set scores, a pairing change after the start with and without a comment, finalise, single confirm when captain and leader are the same person, dispute, comments from both teams, read-only view for other users, and a club administrator and a platform administrator stepping in. The sample data has not been generated under an administrator session either.
