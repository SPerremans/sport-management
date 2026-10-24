# Model simplification review — 23 October 2026

Status: **review and proposals only**. No model elements, business records, Java sources, or existing documents were changed by this review.

## Conclusion

The model has concrete opportunities to reduce repeated table plumbing and publication logic. No persistent entity or page was established as safe to delete. The current model does not provide a clean baseline for proving behavior preservation: Mendixx reported 703 errors initially and 924 on a subsequent fresh check. Diagnostics changed during the review and the original pagination window became stale. These are observations of the live model, not errors introduced by this review.

Fix and stabilize the baseline before implementing the proposals below. Each proposal describes the replacement before considering removal of duplicated implementation.

## Scope and evidence

- Mendixx module inventory: two writable application modules, MyFirstModule and UserManagement; Marketplace/system modules were left alone.
- Connector inventory at the time of reading: MyFirstModule contained 105 entities, 173 associations and 558 microflows; UserManagement contained three microflows. These counts are inventories, not a claim that every document was reviewed in depth.
- Of the MyFirstModule microflow names, 142 contained `Table`, including 48 `ACT_TableContext_` entry points. Those counts do not mean all those workflows are redundant.
- Inspected the season/current-history context wrappers, Season and SeasonFee table operation wrappers, Club Publish/Unpublish workflows, domain inventory, table registries, lifecycle dispatch, and existing verification documentation.
- Used Mendixx model reads, diagnostics and runtime status, supplemented by local Java/documentation inspection. The connector's advertised `mendix://studio-pro/system-prompt` resource was not exposed through the available resource interface or VFS; tool schemas and exposed documentation were available. No model writes were attempted.

## 1. Share the season table context-binding implementation

Observed documents:

- `MyFirstModule.ACT_TableContext_Club_Workspace__clubSeasonList`
- `MyFirstModule.ACT_TableContext_Club_Workspace__historyClubSeasonList`

Both repeat the same visible sequence: obtain the selection-state identifier, construct a binding command, obtain four context identifiers, append the Club/clubView/capabilitiesView/seasonsClubContext bindings, invoke the bridge, and dispatch the table operation. Their table keys differ.

Proposed simplification: retain both named, typed entry points and their parameter signatures, but move the repeated binding sequence into a shared helper taking the table key and the existing contexts. Keep the current/history data sources and bindings separate. Examine the remaining context wrappers for the same pattern individually; context shapes vary.

Preservation requirements: retain document identities, allowed roles, selection/state refresh behavior, binding validation and all argument mappings. Do not assume the three Club-typed context arguments always reference the same object. Verify current and historical season actions independently, including permission refusals and empty/single/multiple selections.

The `ACT_Table_Season` and `ACT_Table_SeasonFee` wrappers also repeat GUID/command encoding, row iteration, bridge dispatch and state refresh. Shared transport code is another candidate, but their different entity list types remain valuable; keep typed wrappers if extracting this code. Do not collapse them into an untyped public endpoint merely to reduce document count.

## 2. Share identical current/history table definitions

Local evidence:

- `javasource/myfirstmodule/TableDefinitions.java`, entries for `Club_Workspace__clubSeasonList` and `Club_Workspace__historyClubSeasonList`.
- `javasource/myfirstmodule/TableFeatureDefinitions.java`, entries for the same two keys.

The two table definitions repeat the same entity, title, edit workflow and four field definitions. Their feature definitions also repeat the same context map, View action, argument mapping and `CanReadSeasons` condition.

Proposed simplification: construct the common metadata through one private definition factory and register it under both existing keys. Retain both keys and their distinct page bindings. This reduces repeated definitions without changing model references or data-source behavior. Use independent values if future mutation of a definition is possible.

Preservation requirements: compare generated fields and order, edit/View targets, context names and permission expression before/after; verify both tables still resolve. Check legacy registrations too, since the runtime registry has a preparation step.

## 3. Share Club publication mutation logic

Observed documents:

- `MyFirstModule.ACT_Clubs_Publish`
- `MyFirstModule.ACT_Clubs_Unpublish`

Both count the selection, reject empty selections, call `SUB_Clubs_CanManage`, refuse an unauthorized selection, set `Club.IsPublished`, commit and refresh, then update the selection counts. Detailed connector reads confirmed the same permission subflow and opposite Boolean/count assignments.

Proposed simplification: retain both user-facing entry workflows and their distinct messages/confirmation behavior, and extract a private helper parameterized by the target published state. It can own permission preflight, mutation, commit and count refresh. Remove repeated inner activities only after both entry points call the helper and their behavior has been checked.

Preservation requirements: keep Administrator-only entry access, all-or-nothing scope refusal, commit events, client refresh, active/inactive counts and directory visibility. Exercise publish/unpublish on mixed published states and mixed authorized/unauthorized selections. Do not generalize account deactivation or competition activation into this helper: those features have different side effects, locks and audit requirements.

## Elements that should stay until further evidence exists

- Server permission checks and entity event guards: UI visibility is advisory. `TableLifecycleService` rechecks reads, mutation permission, archive state, retention policy and references.
- Feature-specific action workflows: `TableLifecycleService` invokes workflows dynamically through `Core.microflowCall(legacy.flow())` and `Core.microflowCall(feature.flow())`. A missing direct model reference does not prove that a workflow is unused.
- Financial/audit snapshots and private claim/key entities: repeated-looking values may preserve historical facts or enforce concurrency/idempotence. No deletion or migration case was established.
- Separate current/history views, scoped projections and typed wrappers: similar presentation does not establish identical data scope or lifecycle.
- Development sample feature markers, ownership manifest, cleanup allowlist and dependency map. They support incremental upgrades and scoped cleanup and are not disposable duplication.

No complete unused-element proof was possible from the connector's exposed read operations. Do not delete based on names, naming age, an excluded document, or simple text-reference counts.

## Verification results

| Area | Observation | Limit |
| --- | --- | --- |
| Model consistency | Initial fresh Mendixx check: 703 errors (check 23). Later fresh check: 924 errors (check 32). | Not passing. Only the first 100 problems in each fresh result were inspected; later pagination of check 23 was rejected as stale. |
| Representative model errors | Out-of-scope `tableState_*`/`clubSelection` variables in button visibility; later results also show arguments not matching `ACT_TableContext_*` parameters. | Examples include CompetitionTeam_Workspace, MyClub_Competitions, Platform_Clubs and MatchFixture. The complete error set was not classified. |
| Compilation | Not run for this review. | Existing model errors prevent claiming that the current source builds. Historical build records do not verify this snapshot. |
| Runtime status | Mendixx reported running=true, launching=false, URL http://localhost:8080/. | A running runtime does not prove it contains or supports the currently edited model. |
| Runtime scenarios | Not executed. | No claim that existing behavior or any proposed refactoring passed interaction/regression tests. |
| Development samples | Not generated, upgraded or removed. | No behavior was added and no sample verification was executed. |

## Implementation sequence after the baseline is stable

1. Resolve current table-state scope and argument mapping errors; obtain a fresh clean global consistency result.
2. Apply one proposal at a time, preserving named document identities and feature folders. Explain any proposed removal first.
3. If introducing a helper workflow, extend the corresponding development scenario coverage in `DevelopmentSamples.java` as required by AGENTS.md. Preserve feature upgrade idempotence, existing edited records and ownership coverage. No new persistent records are needed merely to extract a helper.
4. Check affected documents and global consistency, compile the current application, then execute the relevant role, selection, refusal and state-refresh scenarios. Report each verification category separately.
5. Remove duplicated implementation only when its callers are redirected and the relevant checks have actually completed. Persistent entity removals require an additional data/reference/migration assessment.
