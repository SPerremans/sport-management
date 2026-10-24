# Multi-select table toolbars

## Current shared implementation — 23 October 2026

All 79 application Data Grid 2 tables use checkbox multiselect and a toolbar. There are no row action buttons. The general labels are Create, View, Edit, Delete, Export, Archive, Deactivate, Activate and Restore. Selection and server-computed rights control visibility; each request revalidates permissions and scope. Existing bulk-edit/audit workflows and feature-specific operations are preserved.

`Accounts/Records/DS_TableState_New` initializes selection state and Create availability using the current business context. Typed `ACT_Table_*` flows and context routers call the shared Java bridge. Explicit table, field, create-target, legacy-action and feature allowlists are maintained in `TableDefinitions`, `TableCreationDefinitions`, `TableLegacyDefinitions` and `TableFeatureDefinitions`. Context selection for creation uses `TableCreate_Select`, choosing only accessible parent records before invoking existing workflows. Derived history and other immutable projections have no direct create/edit/delete operation.

`Accounts/Retention` holds retention configuration and the private purge entry point. Lifecycle markers and policies have no client access. Defaults: 30 days, permanent deletion permitted where the business permission allows it, automatic purge disabled. Organisation policies can only strengthen global retention. Permanent deletion preflights all selected records and rejects dependencies rather than cascading to other business data. Archive disables active/published flags where available, records ownership and freezes edits; Restore removes the marker without enabling the record. The daily purge rechecks the current effective policy and dependencies.

My registrations and the member dashboard use a text filter for the string-valued club name. Linked searches live inside the owning grid's filter area and retain their configured searchable attributes.

Development sample upgrade `features:table-actions-and-retention-v1` adds a fictional archived unpublished news item and a keep-indefinitely club policy. Both private metadata types and their owning associations are registered for scoped cleanup and ownership-manifest tracking. Existing demo edits are preserved. Fresh/upgrade/removal scenarios require runtime execution; their presence in the generator does not constitute a passing test.

The sections below document the earlier implementation and its historical verification; row actions and the old selection data source described there are superseded.

The admin tables follow the **Application accounts** pattern ([account-details-and-audit.md](account-details-and-audit.md)): Data grid 2 with multi selection (checkboxes and select-all), and a `nexio-table-actions` toolbar above the grid. Each button passes the grid selection to a server microflow. Microflows count the selection with an Aggregate List (Count) activity, so a single-item action refuses anything other than exactly one row and a bulk action refuses an empty selection. Every change also rechecks the user's role and scope on the server. The existing per-row action columns are unchanged.

## Selection-aware buttons

Toolbar buttons are hidden until they apply. Each table's toolbar and grid sit inside a `nexio-table-selection` data view on a transient `TableSelection` (`SelectedCount`, `ActiveCount`, `InactiveCount`), created by `Logic/DS_TableSelection_New`. The grid's **On selection change** calls `OCH_<Table>_SelectionChanged`, which recounts the selection and refreshes the record.

The grid has to be *inside* that data view: Mendix rejects a selection-change argument taken from a sibling data view ("arguments … do not match the expected parameters").

| Button | Visible when |
| --- | --- |
| Single-row actions (Open, Assign administrator, Propose date change, Placement) | exactly 1 row selected |
| Edit, Export | at least 1 row selected |
| Deactivate / Unpublish | at least 1 selected row is active (published for clubs) |
| Enable / Publish | at least 1 selected row is inactive (unpublished for clubs) |

Leagues and fixtures have no active state, so they only use `SelectedCount`. After Deactivate/Unpublish or Enable/Publish, the action sets the counts to match (all selected rows now inactive or active), so the opposite button shows. Visibility is a convenience only; the server checks stay as described below.

## Tables and actions

| Table | Single row | One or more rows | Server scope check |
| --- | --- | --- | --- |
| Clubs › Platform_Clubs (Administrator) | Assign administrator | Edit, Unpublish, Publish, Export | `SUB_Clubs_CanManage` → `SUB_Club_AdministratorOrOwner` |
| Federations › Federation_Overview (Administrator, FederationAdmin) | Open | Edit, Deactivate, Enable, Export | `SUB_Federations_CanManage` → `SUB_Federation_HasAccess` (Administrator, or FederationAdmin assigned to that federation) |
| Competition_Overview › Leagues | Open | Edit, Export | `SUB_CompetitionLeagues_CanManage` → `SUB_Competition_FederationPermission(Federation, 'Manage')` |
| Competition_Overview › My clubs › Teams | Open | Edit, Deactivate, Enable, Export | `SUB_CompetitionTeams_CanManage` → `SUB_Competition_ClubPermission(Club, 'Update')` |
| Competition_Overview › My clubs › Venues | Open | Edit, Deactivate, Enable, Export | `SUB_CompetitionVenues_CanManage` → `SUB_Competition_ClubPermission(Club, 'Update')` |
| Competition_Overview › Published fixtures | Propose date change, Placement | Export | the existing fixture-change and placement microflows |

All Competition_Overview toolbar buttons are available to every role that can open the page. Club staff with a Competition permission and federation competition delegates can manage teams, venues or leagues without the ClubAdmin or FederationAdmin role, so the scope check alone decides. Platform_Clubs and Federation_Overview actions are limited to their page roles. A mixed selection is refused as a whole: nothing changes, and the message asks the user to deselect rows they cannot manage.

**Edit**
- **One row:** opens the existing editor: Club_Edit, `ACT_Federation_Settings`, `ACT_CompetitionLeague_Edit`, or the team or venue workspace.
- **Several rows:** opens a modal with every field empty. Only fields the user fills in are applied; blank fields keep each row's current value.

| Modal | Fields |
| --- | --- |
| `ClubBulkEdit_Edit` | published, registrations require approval, registrations require payment |
| `FederationBulkEdit_Edit` | active |
| `CompetitionLeagueBulkEdit_Edit` | registration opens/closes, changes open/close |
| `CompetitionTeamBulkEdit_Edit` | active, season |
| `CompetitionVenueBulkEdit_Edit` | active |

Yes/no fields use the `BulkFlagChoice` enumeration; empty means keep the current value. League edits fail as a whole if any resulting window would close before it opens, and each changed league gets a `CompetitionAudit` entry ("Bulk edit"). Team, venue and league changes take the competition mutation claim (`SUB_Competition_Lock`) first, as the existing saves do.

**Deactivate / Unpublish** sets `IsActive = false` on federations, teams and venues, and `IsPublished = false` on clubs (which hides them from the club directory). It asks for confirmation first and can be undone with Enable/Publish or Edit. Nothing is deleted. Delete is planned separately as archive with configurable retention and purge.

**Enable / Publish** (`ACT_Federations_Enable`, `ACT_CompetitionTeams_Enable`, `ACT_CompetitionVenues_Enable`, `ACT_Clubs_Publish`) sets the flag back to true. It runs the same empty-selection and scope checks as Deactivate, and teams and venues take `SUB_Competition_Lock` first.

**Bulk-edit modals** use label width 0, so labels sit above the fields. Popup padding comes from one theme rule on the window body (`_nexio.scss`), so every modal keeps its padding whatever widget sits at its root.

**Export** downloads a UTF-8 CSV of the selected rows. Values are quoted, and values starting with `= + - @` are prefixed to prevent spreadsheet formula injection. The file is a `TableExport` (a System.FileDocument specialization) that only its creator can read, and it is deleted after download. Only rows the user can already read are selectable, so export adds no access.

Workflow names follow `ACT_<Table>_Open|Edit|Deactivate|Export`, `ACT_<Entity>BulkEdit_Apply` and `SUB_<Table>_CanManage`. They live in the Clubs, Federations, Competition/Leagues, Competition/Teams, Competition/Venues, Competition/Fixtures, Competition/FixtureChanges and Competition/Planning folders.

## Development examples

The generator's existing samples already cover these tables with several rows each: multiple clubs (published and unpublished), federations, leagues, per-club teams and venues, and fixtures. The samples include both published and unpublished clubs, so both Unpublish and Publish can be shown. No sample records were added. The bulk-edit forms and `TableSelection` are transient, and export files are created per download. So `TableExport` and `TableSelection` are not generated and are not in the cleanup allowlist.

## Verification — 21 October 2026 (selection-aware buttons, Enable/Publish, modal padding)

- **Model consistency:** every new and changed page and microflow reports no errors. Global diagnostics still list the same single error: "Entity access is out of date" in the MyFirstModule domain model, which needs **Update security** clicked in Studio Pro.
- **Compilation:** not run.
- **Runtime scenarios:** not executed. That covers button visibility per selection, counts after Deactivate/Enable, grid layout inside the selection data view, and the popup padding rule.

## Verification — 20 October 2026

- **Model consistency:** global diagnostics list one error: "Entity access is out of date" in the MyFirstModule domain model. Click **Update security** in the domain model editor; the connector cannot trigger it. All new and changed microflows and pages report no errors. All five modal pages are in their feature folders.
- **Compilation:** not run.
- **Runtime scenarios:** not executed. That covers selection passing, count refusals, scope refusals per role, bulk apply, league window validation, deactivate and export downloads.
