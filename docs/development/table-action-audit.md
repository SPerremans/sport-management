# Table action audit — 23 October 2026

Reviewed 79 Data Grid 2 tables on all 40 currently generated MyFirstModule table pages against live Mendixx page reads. The page/table inventory and external buttons are in table-action-audit.json. Marketplace/system module administration tables are excluded.

## Recommended cleanup (save not confirmed)

| Page | Redundant buttons to remove | Button/behavior to retain |
| --- | --- | --- |
| Platform_Clubs | standalone Create (`widget7`) | guarded toolbar Create: TableCreationDefinitions creates Club and opens ACT_TableEdit_Club → Club_Edit, the same editor used by the standalone button |
| Platform_Clubs | `clubsAssignAdmin` | permission-aware `tableFeature1_clubList`; both lead to ACT_ClubAdmin_OpenAssignment |
| Accounts_Overview | `accountsViewAccount` | `accountsViewAccount1`, with identical action, selection mapping and CanView condition |
| Competition_Overview | `leaguesOpen`, `teamsOpen`, `venuesOpen` | each corresponding tableFeature1 View; wrapper flow reads confirmed the same league/team/venue target workflows |
| Competition_Overview | `fixturesProposeChange`, `fixturesPlacement` | permission-aware feature proposal/placement buttons; wrapper flow reads confirmed ACT_FixtureChange_Open and ACT_FixturePlacement_Open |
| Federation_Overview | `federationsOpen` | permission-aware feature View; both routes lead to ACT_Federation_Open |

This removes nine redundant button instances, retaining their underlying workflows and the guarded toolbar entry points. These are proposed button removals, not proposed workflow/entity deletions. Same-caption duplicates were not assumed equivalent: account action JSON, wrapper microflows, and TableFeatureDefinitions were inspected.

Two placement corrections:

- Player_Directory: move the existing query field and Search button into the table's filter area, inside a data view explicitly bound to the existing Search page parameter. Preserve Query, Enter-key behavior, Search handler, and existing result source. Use a compact wrapping row; Search is a filter operation, so it should not sit loose between the form and table. Do not introduce an explicit linkedDs ListenTargetSource override.
- Club_Workspace/Roles: move Add Secretary role into the clubRolesList action toolbar. It is a distinct shortcut from generic role creation and must stay available.

## Deliberately retained outside table selection toolbars

- Search/reset controls belong to their filter form. Account and club discovery Reset filters already follow this arrangement; Player_Directory is the exception identified above.
- Record commands belong with their owning record or workflow: federation settings and invoice appearance, league settings, team league registration, account View as user, role disable/permissions, and match captain/lineup/official/result decisions. Their scope is the open record, not selected table rows.
- League allocation and schedule generation/planning/publication already have compact tab command bars. Keep these distinct workflow operations available. Do not replace them with generic Create/Archive actions.
- Registration review decisions stay with the reviewed request. Invitation creation stays with its input form. Match commands stay beside their related fields so form validation and currentObject scope remain intact.
- Back, Save, Cancel and Close remain page/form navigation controls.

All 79 tables contain a selection action toolbar. Create is configured as Primary and conditionally visible; unsupported operations remain hidden by their capability flags. Seven duplicate-caption groups were found, plus the standalone Club Create and the differently captioned fixture Placement duplication above. No duplicate-caption match alone establishes safe removal.

## Verification and limitation

- Model consistency before changes: global Mendixx diagnostics returned No errors found.
- Save attempts: the initial Platform_Clubs multi-operation patch failed on an array index and its subsequent readback showed the original page. Subsequent page-write requests timed out. Accounts_Overview readback still showed both View buttons. Later minimal retry writes also returned no completion before termination. No cleanup save was confirmed; re-read affected pages before retrying.
- The connector also intermittently returned a network Connection timed out error on page reads. Read operations sometimes recovered; writes did not provide a confirmed result. Do not treat this as successful implementation.
- Compilation/page export: not run for unconfirmed edits.
- Runtime/visual interaction: not executed; authenticated pages are unavailable in the available browser session.
- No new business entity/page/workflow or sample scenario was introduced. Sample generation, upgrades, idempotence and cleanup were not executed.

On reconnect: re-read Platform_Clubs, Accounts_Overview, Competition_Overview, Federation_Overview, Player_Directory and Club_Workspace first. Apply removals from deepest/rightmost paths before parent/sibling removals, or compute filtered toolbar arrays from fresh reads. Preserve handler arguments, visibility, confirmation dialogs, owning data contexts, document identities and folders. Run model diagnostics and actual page export, followed by role/selection/filter interaction checks.
