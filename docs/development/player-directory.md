# Player directory and platform club administration

**Players** in the navigation opens a signed-in player directory. Search is case-insensitive and matches a person's current full name or their club's name. The federation decides who is listed: a membership appears only when it is Active with no EndedAt, its club is published and has an approved, active federation affiliation, and its membership type is linked to a federation type with **Show in public player list** enabled on an active federation. Federation administrators set this switch on the federation membership type page; it is off by default, so competition types must be switched on explicitly. The XPath requires the club to have some approved affiliation; that the linked federation type belongs to an affiliated federation is enforced when the club links its membership type (club admins can only choose types from approved federations). Multiple membership types for the same person/club produce one entry; membership in multiple clubs produces one entry per club. At most 500 matching entries are returned, sorted by player name.

The server returns non-persistent PlayerDirectoryEntry objects with exactly PlayerName and ClubName. The search object contains Query. Only MyFirstModule.User has access to these objects and the page/action/data-source flows; every signed-in app role includes that module role, while Guest does not. Existing Person and Membership access rules are unchanged. Entries have no account/member associations or identifiers.

The Administrator app role can see all clubs under **Club administration**. SUB_Club_AdministratorOrOwner retrieves the current user's Administrator role from the database and a fresh club, otherwise it requires the existing owner association. The common club permission helper, capability source, role/staff actions and admin account creation use this helper. Ordinary club owners/staff retain their previous club-specific access.

Directory flows are under **Members/Directory**, and the administrative helper/data source under **CRM/Clubs/Administration**. Changes replace activity actions without deleting nodes or sequence flows.

Validation: Mendix consistency check reports zero errors and the Java/app build succeeded. In the live app, mixed-case club search `rIvErSiDe` returned the 12 Riverside sample players, and mixed-case name search `pLaYeR 14` returned one player with the correct club. The Administrator club list showed the original club and all four sample clubs. The Administrator opened the Northside workspace with all ten administration tabs and editable settings. Role-isolation checks under a separate ordinary-user session remain unexecuted.


## Club player lists (club managed)

Besides the federation-managed public list, each club has an internal player list. It is shown in the club workspace (**Players** tab) and on the member dashboard (**Club players** tab, then **View players** for one of your clubs).

- Club owners and staff with Members read permission always see every active member with all columns.
- Everyone else sees nothing until the club adds a **player list access** rule in the workspace tab of the same name (owners and staff with Members update permission manage rules). A rule names the members shown (one membership type), the viewers (holders of an active membership of one membership type, or staff holding one active club role) and the columns they may see: Email, Membership type and Team. The name is always shown.
- `DS_ClubPlayerList` evaluates the rules on the server and returns a non-persistent `ClubPlayerList` with per-column flags and `ClubPlayerEntry` rows. Columns that are not permitted are left empty on the server and hidden in the grid, so hidden values are never sent to the browser. Viewers receive rows only for membership types granted to them; when several rules apply, a column is shown if any matching rule allows it.
- Rows are per active membership, sorted by player name; a person with two visible membership types appears twice. Team shows the member's first competition team among the club's active teams.
- `ACT_PlayerListRule_Save` rechecks permission and requires a visible type and exactly one viewer (type or role) from the same club. Flows and pages are under **Members/PlayerLists**. The rules table follows the [table standard](../ui-table-standard.md): one Edit button per row; Delete is in the rule popup.

Sample data (`features:federation-player-lists-v1`, fresh and upgrade, once): owned demo federation types containing "Competition" are published in the public list; each sample club gets a rule letting competition players see each other's name, membership type and team, and, where the club has a recreational type, a names-only rule for recreational members. All rules are in the ownership manifest and cleanup allowlist.

Verification for this change: model consistency check reports no errors for MyFirstModule; `DevelopmentSamples.java` compiles with JDK 21 against the Mendix 11.15 runtime API. The app has not been run and no runtime scenario (public list contents, rule evaluation per role, column hiding, sample upgrade/removal) has been executed.
