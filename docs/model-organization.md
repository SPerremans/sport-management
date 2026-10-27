# Studio Pro model organization

Pages and workflows belong to the business feature they implement. The module root holds required singleton documents and feature folders. CRM and implementation phase numbers are not application-wide categories.

## Folder guide for MyFirstModule

| Folder | Responsibility |
| --- | --- |
| Accounts | Platform account overview and detail with audit history, the multi-select toolbar (view, View as user, edit modal, deactivate, CSV export) and its workflows, administrator assignment, invitations, password reset, development View as user entry points, and their data sources |
| Accounts/Configuration | Account status enumeration used by the account overview and its status filter; the edit-status choices used by the account edit modal |
| Accounts/Filters | Account overview search state, role/status choices (selected first), refresh, reset and in-memory row filtering, using the shared discovery filter bar |
| Accounts/Records | Shared selection state, table dispatch, record view and create-context choice dialogs; explicit server action allowlists |
| Accounts/Retention | Global and organisation retention settings, private purge workflow and account archive/delete guards |
| Billing | Billing policies, federation billing, invoice generation and related jobs; Invoices, Instalments, Appearance and SeasonPolicies contain narrower concerns |
| Clubs | Club details and workspaces, the club setup table toolbar (assign administrator, bulk-edit modal, unpublish, CSV export) and its scope check; Administration contains club roles and staff assignments |
| Clubs/Affiliations | Club-facing federation affiliation requests, statuses and withdrawal |
| Competition | Competition, league, team, venue, registration, scheduling and fixture management; the Leagues, Teams and Venues subfolders hold their table toolbar workflows, scope checks and bulk-edit modals, and Fixtures holds the fixture CSV export |
| Competition/Fixtures | Fixture generation and the fixture detail page with date change, placement and match registration actions; the read-only calculated calendar result callback (`CAL_CompetitionFixture_ResultSummary`) |
| Competition/MyClub | Member-facing **My club competitions** page: one section per sport/federation of the clubs where the user has an active registration, with a team filter and the published fixtures (`DS_CompetitionSportSection_*`, `OCH_CompetitionSportSection_Team`) |
| Competition/Teams | Team creation with the federation team category and automatic numbering (`ACT_CompetitionTeam_Save`, `DS_CompetitionForm_CategoryOptions`), plus the team table toolbar workflows |
| Competition/FixtureChanges | Fixture date proposals: propose, slot selection, home/away approval, rejection and the proposal review page |
| Competition/Results | Poule page with standings and fixtures tabs (`CompetitionPoule_Standings`, `DS_CompetitionPoule_Standings`, `DS_CompetitionPoule_StandingRows`, `ACT_TablePage_CompetitionPoule_Standings`; opened with View on the league's Poules tab) and inherited result settings (`DS_CompetitionLeague_Overview`, `DS_Match_Standings`, `ACT_CompetitionLeague_SaveResults`), direct scores (`ACT_Match_SaveFixtureScore`), and match registration: formats, slots, lineups, scores, eligibility, officials, approvals and rulings. Team sheets: the format limits (`MaxStarters`, `MaxReserves`), the `MatchSquad` page (roster and team-sheet toolbars, substitution entry), `ACT_Match_HomeSquad`/`AwaySquad`, `ACT_Match_Squad*`, `ACT_Match_Substitution*`, `ACT_Match_SaveSubstitution`, `DS_Match_RowsOfKind` and the `MatchSquad__*` table routers; `MatchSquadPosition` lives in Configuration. Individual games are shown only when enabled on the league; source sheets retain raw scores and credited provider totals separately Fixture entry games table: `ACT_Match_GamePickHome`, `ACT_Match_GamePickAway`, `ACT_Match_GameSave`, `ACT_Match_GameOpen` (see [fixture-entry.md](competition/fixture-entry.md)). |
| Competition/Results/Configuration | Match configuration enumerations |
| Competition/Results/System | Private startup and acceptance-expiry workflows |
| Competition/Tests | Checks and sample data page, complete discovered-test results and diagnostics, development/role guides, legacy runner regressions and the excluded runtime probe. Pure Java policy checks live in `javasource/myfirstmodule/DevelopmentChecksTest.java` |
| Content | Club events and news |
| Development | Local administrator sample-data generation and removal, including the frozen public 2026–2027 competition snapshot. The private DevSampleBatch manifest tracks source organization IDs separately so public club/federation copies never acquire test login accounts |
| Discovery | Club and player directories; read-only club audience projections derived from current membership offers |
| Discovery/Configuration | Derived club age-group and playing-level filter choices |
| Discovery/Filters | Searchable multiselect choices from current offers, exact-age filtering and transient search state; shared by both club directories |
| Discovery/System | Derived city references: club save handler and idempotent startup backfill |
| Federations | Federation details, affiliation, membership types and rates; the federation table toolbar (open, bulk-edit modal, deactivate, CSV export) and its scope check; the federation's required registration fields (`ACT_FederationField_Create`); the federation's gender options and team categories (`FederationGender_Edit`, `TeamCategory_Edit`, `ACT_FederationGender_Create`, `ACT_TeamCategory_Create`, `DS_TeamCategory_GenderOptions`) |
| Federations/Affiliations | Affiliation request/approval/decline workflows and private concurrency claims |
| Members | Member dashboard, membership editing, membership types, renewal pages and lifecycle workflows |
| Members/PlayerLists | Club-managed internal player lists: access rules, rule editor, rule option sources, and the server-built player list view |
| Members/Configuration | Membership age-range formatting and validation, and explicit Youth/Adults/Seniors audience summaries |
| Registration | Registration forms, fields, overview, staff review and approval, waiting list, member withdrawal and season lookup; the question editor's save and its federation-field choices (`ACT_FormField_Save`, `DS_FormField_FederationOptions`) and the `YesNoAnswer` choice used by yes/no questions |
| Seasons | Season dates, fees, permissions and season selectors |
| Team | Existing club team support |
| Enumerations | Existing shared enumerations, including `BulkFlagChoice` (Yes/No, empty = keep) used by every bulk-edit modal; new feature-specific enumerations should stay with their feature |
| Logic | Existing shared/integration workflows, plus `DS_TableSelection_New` (the selection counts shared by every multi-select table toolbar); put new feature workflows in their owning feature folder |

`Phase3` was a historical delivery-stage bucket mixing accounts, federation administration, billing and renewals. Its 11 pages and all supporting workflows have been redistributed to the folders above. The empty `CRM` and `Phase3` folders have been deleted through Studio Pro.

UserManagement/Login and UserManagement/Signup are grouped under that module's Accounts folder alongside the signup workflows.

Some existing validator document names still end in `_Phase3`. Their names are retained to preserve qualified references; they now live in the relevant business folder.

## Creating or changing documents

Shared table record dialogs, selection state, create-context choices and dispatch workflows live under **Accounts/Records**. Global and organisation retention settings, the private purge workflow and account deletion guards live under **Accounts/Retention**. Typed business table dispatch, editor and lifecycle guard workflows remain with their owning business feature. `TableRecord_View`, `TableCreate_Select` and `TableRetention_Edit` are created in those folders before page generation.

1. Choose the business folder before creating a named page or workflow.
2. Prefer mnd MCP and provide the folder when creating the document. If using page generation, create the page shell in that folder first, then populate the existing page.
3. Move existing documents in Studio Pro when the connector has no supported move operation. Preserve document identities and qualified names; do not recreate pages to change their location.
4. Verify the saved containment using the connector's folder listing and document reads, then run model consistency diagnostics.
5. Add development examples for new business behavior as required by AGENTS.md. Folder-only moves need no new scenarios.

## Verification of this reorganization

- Model consistency: global mnd diagnostics returned **No errors found** after the moves and empty-folder deletions.
- Folder inventory: all active MyFirstModule pages are in business folders. All 12 Match pages and the Java action are under Competition/Results. Connector folder listings confirm CRM and Phase3 are absent.
- 20 October 2026: `AccountBulkEdit_Edit` (the account edit modal) was created at the module root by the page generator after a timeout. It was then dragged into **Accounts** in Studio Pro's App Explorer. That move keeps the document's identity and references, which App Explorer cut/paste did not do. The connector listing confirms the module root again holds only singleton documents and folders.
- Connector limitation: the excluded Invoice_Detail_Styled page is displayed inside Billing/Invoices in Studio Pro, but folder reads omit it and find-document reports an empty path for it. This is recorded as an inventory limitation rather than a loose active page.
- Compilation: not rerun for these containment-only changes.
- Runtime scenarios: not rerun for these containment-only changes. Existing runtime validation limitations remain in competition/match-registration-status.md.
