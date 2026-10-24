# Table standard

Every list of records in Nexio uses the same table pattern, so tables look and behave alike across clubs, members, federations and competitions.

## Pattern

- **Widget:** Data Grid 2 with the CSS class `nexio-data-table`. Styling lives in `theme/web/_nexio-tables.scss`; do not add per-page table styling.
- **Cells:** each column contains one `Pages$DivContainer` with class `nexio-table-cell` holding a single Dynamic Text (`{1}` bound to the attribute). Headers are short nouns ("Name", "Annual fee").
- **Booleans:** show `Yes` / `No` text (two Dynamic Texts with opposite visibility), never `true` / `false`.
- **No row actions:** table cells contain data only. Remove the Actions column. All record operations use the selection toolbar above the table.
- **Workflow and destructive actions** (approve, decline, cancel, delete, put on hold, add child record) go on the record's edit/detail page or popup, not in the table row. Show them there with conditional visibility based on status or permission.
- **No nested tables.** Child records (fees, answers, lines) are a separate table on the parent's edit/detail page, with an "Add …" button above it.
- **Selection:** enable checkbox multiselect and select-all in every application Data Grid 2.
- **General actions:** use the exact names **Create**, **View**, **Edit**, **Delete**, **Export**, **Archive**, **Deactivate**, **Activate** and **Restore**. Hide actions when the selection, business type, state or user's rights do not support them. Create is available without selecting rows. View and ordinary Edit require one row; existing guarded bulk editors remain available. Export uses the selection and readable table columns.
- **Create buttons** sit in the table toolbar as one primary button named **Create**. Existing create workflows choose the owning club, federation or other parent when the page does not supply it.
- **Specific actions:** retain operations such as Assign administrator, Publish, placement and proposals in the toolbar. Single-record operations require exactly one selected row and preserve their existing state and permission checks.
- **Lifecycle:** Delete is permanent; Archive preserves history and blocks editing until Restore. Restore leaves inactive records inactive. Deactivate changes an existing active flag and remains distinct from Archive. Global retention defaults to 30 days with automatic deletion disabled. Club/federation overrides can extend retention, retain forever, disable permanent deletion or disable automatic deletion. Dependencies and confirmed financial history block permanent deletion.
- **Button spacing:** neighbouring buttons always have at least 8px between them. This is global in `theme/web/_nexio.scss` (sibling margin) and uses `gap: 8px` in flex containers (`.nexio-table-actions`, data view footers); never place buttons without one of these.
- **Hidden columns:** when a column depends on permissions, hide it with the column's `visible` expression (`$currentObject/<Flag>` refers to the enclosing data view) and leave the value empty on the server.

## Page header and command bars

Only one row of buttons may sit above a tab bar or a table.

- **Workspace header:** record name as H2 with class `nexio-page-heading`, then one muted line of key facts (`nexio-page-meta`, e.g. "Season 2026 · Round robin · Planning"), then an optional muted description.
- **Record-level actions** (settings, invoice appearance, assign administrator) go in one `nexio-page-actions` container directly under the header, all in Default (secondary) style. Never use a primary button here.
- **Tab or table actions** go in a `nexio-tab-toolbar` container at the top of the tab, or directly above the table they act on: at most one Primary button (the main next step, e.g. "Publish schedule", "Create league", "Add player"), the rest Default.
- Never place loose action buttons next to each other: always wrap them in one of these two containers (compact 36px buttons, 8px gap).

Applied to: CompetitionLeague_Workspace (Allocate poules moved to the Poules tab; Generate / Plan draft / Publish moved to the Fixtures tab), Federation_Workspace, CompetitionFederation_Workspace, CompetitionTeam_Workspace and MatchFormats.

## Reuse

Mendix has no parameterised table component that can be reused for different entities in Data Grid 2: columns are bound to one entity. Reuse therefore comes from:

1. The shared stylesheet and class names above (one look).
2. A Studio Pro **building block** can provide a preconfigured Data Grid 2 with checkbox selection, data columns and a selection toolbar. It must not contain row action buttons.
3. The page JSON helpers used by Claude in this repo's sessions (`grid`, `grid_text_column`, `yes_no_column`), which produce this exact structure.

## Historical status (19 October 2026; superseded by the toolbar rules above)

Conforming: all application tables in MyFirstModule. Reworked in this change:

- Federation workspace membership types (fees moved to the type page, Yes/No column, one Edit), federation club links (Approve/Decline moved to the link page), club player list access rules (Yes/No columns, Delete moved into the rule popup).
- Club_Workspace → Registrations: one **Open** button to the new `Registration/Registration_Review` page (request summary, the answers table, Approve / Put on hold / Cancel registration). The nested answers column is gone. The page takes the request and the club; it loads `DS_Club_Capabilities` for the club so the buttons keep the workspace's visibility rules (`CanUpdateRegistrations`, `CanDeleteRegistrations`, pending status for Put on hold). The microflows still do their own server checks.
- Club_Workspace → Members: one **Edit** button. Create invoice and Cancel membership moved to `Membership_Edit` under "Membership actions", with the same `CanCreateInvoices` / `CanDeleteMembers` visibility through `DS_Club_Capabilities`. `Membership_Edit` now also takes the club.
- CompetitionLeague_Workspace → Fixtures: one **Open** button to the new `Competition/Fixtures/CompetitionFixture_Detail` page with Propose date change, Federation placement / override and Register match. These had no visibility rules in the row; the microflows check permissions.
- CompetitionLeague_Workspace → Date proposals: Home approved / Away approved show Yes/No, and one **Open** button leads to the new `Competition/FixtureChanges/FixtureChange_Review` page. Approve for home club, Approve for away club and Reject proposal show only while the proposal is pending (and that side hasn't approved yet), which matches the microflows' own checks.

Nested data views can't have a footer in Mendix, so on these pages the action buttons sit in a plain row inside the data view, spaced by the global sibling margin.

Verification: Studio Pro model consistency check on MyFirstModule reports no errors. Compilation and runtime scenarios (opening each new page and running its actions) were not run for this change.

Marketplace Administration module grids (`Account_Overview`, `ActiveSessions`, `RuntimeInstances`, `ScheduledEvents`) are not changed.
