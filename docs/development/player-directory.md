# Player directory and platform club administration

**Players** in the navigation opens a signed-in player directory. Search is case-insensitive and matches a person's current full name or their club's name. Only Active memberships with no EndedAt in published clubs are included. Multiple membership types for the same person/club produce one entry; membership in multiple clubs produces one entry per club. At most 500 matching entries are returned, sorted by player name.

The server returns non-persistent PlayerDirectoryEntry objects with exactly PlayerName and ClubName. The search object contains Query. Only MyFirstModule.User has access to these objects and the page/action/data-source flows; every signed-in app role includes that module role, while Guest does not. Existing Person and Membership access rules are unchanged. Entries have no account/member associations or identifiers.

The Administrator app role can see all clubs under **Club administration**. SUB_Club_AdministratorOrOwner retrieves the current user's Administrator role from the database and a fresh club, otherwise it requires the existing owner association. The common club permission helper, capability source, role/staff actions and admin account creation use this helper. Ordinary club owners/staff retain their previous club-specific access.

New directory flows are under **CRM/Members/Directory**, and the administrative helper/data source under **CRM/Clubs/Administration**. Changes replace activity actions without deleting nodes or sequence flows.

Validation: Mendix consistency check reports zero errors and the Java/app build succeeded. In the live app, mixed-case club search `rIvErSiDe` returned the 12 Riverside sample players, and mixed-case name search `pLaYeR 14` returned one player with the correct club. The Administrator club list showed the original club and all four sample clubs. The Administrator opened the Northside workspace with all ten administration tabs and editable settings. Role-isolation checks under a separate ordinary-user session remain unexecuted.

