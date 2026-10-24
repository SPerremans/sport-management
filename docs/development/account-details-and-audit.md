# Account details and history

Platform administrators open **Application accounts**, filter by name/login, email, role or status, select rows with the checkboxes, then use the toolbar above the table. The directory is one paginated table with a single header, a selection column and readable minimum column widths. Filters wrap to two columns and then one column on smaller screens. Account details use a separate compact summary, with an audit table below it.

`Accounts_Overview`, `Account_Detail`, `DS_Admin_Accounts`, `SUB_Admin_AccountRow`, `DS_Account_Audit` and `SUB_Account_Audit` are in the **Accounts** model folder. Detail pages are reached from an account row because they require a selected account; they do not have a context-free navigation entry.

The non-persistent summary exposes identity, roles, status, last sign-in and a read-only account reference. It never exposes a password, hash or invitation/session token. Its account reference scopes the history by stable identity even if a login name changes. Both pages, the projection entity, history entity and history data source are restricted to the platform Administrator. The data source also checks the current Administrator role on the server. Clients cannot create, edit or delete audit entries.

`AccountAudit` stores a timestamp, event, actor/login snapshot, subject/login snapshot, details and its owning `AccountAudit_Account` association. The newest event appears first. Native workflows record successful self-registration, invited-account creation, invitation redemption, revocation/requeue, and club/federation administrator assignment in the same transaction as the operation. The existing guarded user-view controller records its start, return, expiry and revocation with the administrator's reason. No audit call receives passwords, password hashes or invitation/session tokens.

History starts with this implementation; existing runtime-log entries are not retrospectively imported. This is account-administration history, not an application-wide record of every business change or every authentication attempt. Last sign-in remains a Mendix account field. The development controller's in-memory return registry does not survive an app restart; a restart therefore does not synthesize an end event. Existing competition/match histories remain with their business records.

The reported takeover failure on `/admin-user-view/start` came from the controller's `no-referrer` policy: HTML form POSTs can serialize their Origin as `null` under that policy. It now sends `same-origin`, preserving the browser's real origin for its forms without accepting opaque or cross-origin submissions. The configured local application URL is `http://localhost:8080/`, matching the reported browser URL. Confirmation must be opened again after rebuilding so it receives the new header.

In local development, eligible active human accounts have **View as user (local development)**. It opens confirmation for the selected numeric account ID; GET never creates a session. POST retains the existing administrator, CSRF, origin, account-eligibility and session-binding checks. Production endpoints return 404. Disabled, blocked, service, anonymous and platform-administrator accounts cannot be impersonated, and no activation flag, role or password is changed. The detail page explains why an account is ineligible. The development-only picker remains available from Platform Accounts. See [View as user](view-as-user.md).

## Multi-select toolbar

The grid uses multi selection (checkboxes, select-all). Each toolbar button passes the selection to a server microflow that rechecks the selection size and, for changes, the platform Administrator role:

| Action | Selection | Behavior |
| --- | --- | --- |
| View account | exactly one | Opens `Account_Detail`. |
| View as user | exactly one | Ineligible accounts show their availability reason. Eligible ones open the `Account_ViewAsUser` confirmation popup, whose **Continue** link goes to the existing guarded controller (reason, CSRF, local-runtime checks unchanged). |
| Edit | one or more | Opens the `AccountBulkEdit_Edit` modal. One account: prefilled. Several: empty, and only filled fields are overwritten. Fields: username and email (single account only, because both are unique), full name, status (Active/Disabled/Blocked) and roles (chosen roles replace all roles). |
| Enable | selection contains disabled accounts | Sets Active = true on the disabled accounts in the selection; active ones are skipped. |
| Deactivate | selection contains active accounts | After confirmation sets Active = false on the active accounts in the selection. Undo with Enable. |
| Export | one or more | Downloads a UTF-8 CSV of the selected rows (username, name, email, roles, status, last sign-in). Values are quoted and formula-leading characters are prefixed. `AccountExport` files are readable only by their creator and deleted after download. |

Buttons are hidden until they apply. Application accounts now uses the shared table toolbar (`docs/development/table-toolbars.md`): its selection state and server-computed rights control visibility. Edit, Activate and Deactivate are routed to the account workflows below. The earlier account-only selection state (`AccountSelection`, `OCH_Accounts_SelectionChanged`) was removed on 24 October 2026. After a change, `SUB_AccountSearch_RefreshAfterChange` refreshes the search object so the grid reloads. Visibility is a convenience only; the server checks again. The edit and View as user popups use labels above fields and the `nexio-account-edit` spacing.

Guards: Edit and Deactivate refuse to disable or block the acting administrator's own account or remove its Administrator role. Every edited or deactivated account receives an `AccountAudit` entry (`Account edited` lists the overwritten fields; `Account deactivated`). Passwords are never shown, exported or changed. The grid reloads after Edit and Deactivate.

Delete is not implemented yet. It is planned as archive-then-purge with a configurable retention period and configurable audit retention. What happens to an account's memberships, invoices and other business records at purge time is still to be decided.

Workflows: `ACT_Accounts_OpenDetail`, `ACT_Accounts_ViewAsUser`, `ACT_Accounts_Edit`, `ACT_AccountBulkEdit_Apply`, `SUB_AccountBulkEdit_ApplyOne`, `ACT_Accounts_Deactivate`, `SUB_Account_Deactivate`, `ACT_Accounts_Export` (Accounts). Transient form: `AccountBulkEdit` with `AccountBulkEdit_Accounts`, `AccountBulkEdit_Roles` and `AccountBulkEdit_Search`; `AccountEditStatus` in Accounts/Configuration.

The earlier per-row **View account** column was removed; it was defined and deployed but did not render in the browser.

## Development examples

The toolbar works on the existing sample accounts: active scoped test administrators (multi-select edit, export, deactivate) and disabled/blocked member examples (reactivate through Edit, View as user ineligibility). No new sample records were added: the edit form is transient and export files are per-download, not sample business data. `AccountExport` is not generated by the sample generator, so it is not in the cleanup allowlist.

`features:account-details-and-audit-v1` appends one explicitly fictional history entry for every owned sample person, including scoped active test administrators and disabled/blocked member examples. It never claims a real session was opened. Upgrading runs once and preserves existing account edits, role assignments, passwords and prior audit rows. `AccountAudit` is in the sample cleanup allowlist, with `AccountAudit_Account` in the dependency map. Every seeded entry is in the private ownership manifest; later audit entries dependent on an owned sample account are included by scoped cleanup. Auditing an already-deleted sample target writes only the runtime log, avoiding new orphan sample history.

The rollback-only sample verifier checks ownership and timestamps, in addition to its existing fresh-generation, upgrade, repeat-generation, preservation and removal checks. These assertions compiled but were not executed in a runtime administrator session for this change.

## Verification — 20 October 2026 (multi-select toolbar)

- **Model consistency:** global diagnostics return **No errors found** after **Update security** was run in the domain model editor and `AccountBulkEdit_Edit` was moved into Accounts.
- **Compilation:** not run.
- **Runtime scenarios:** not executed (selection passing, edit/deactivate/export, guards, audit writes, grid refresh).

## Verification — 17 October 2026

- **Model consistency:** global MCP diagnostics returned **No errors found**. Both pages are confirmed in Accounts. Studio Pro widget definitions were normalized through MCP after column configuration edits produced stale-definition diagnostics.
- **Compilation:** Studio Pro Java/package build succeeded in 4 seconds. Page export and the development bundle succeeded (443 ms). `AdminUserView.class` updated at 2026-10-16T22:35:57Z. Existing module deprecation/bundle warnings remain.
- **Automated policy checks:** 62 standalone assertions passed. These cover pure validation/authorization helpers, not the HTTP session handoff.
- **Runtime startup:** the app reported running=true, launching=false after the build.
- **Runtime scenarios:** browser layout, directory filtering, account-history scope/denial, persistent audit writes, confirmation/handoff/return, and sample lifecycle verification were not executed. The saved localhost browser policy prevents agent inspection; no alternate browser or HTTP automation path was used. Export inspection confirms account detail navigation, filterable identity attributes, the view-URL action and eligibility expression. Seeded examples are not passing tests.
