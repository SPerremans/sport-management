# Shared table actions — verification, 23 October 2026

The application tables use a single selection-toolbar convention. Record cells contain no action buttons. General labels are Create, View, Edit, Delete, Export, Archive, Deactivate, Activate and Restore; business-specific actions retain their descriptive names. Unsupported operations and operations outside the current user's rights are hidden. Server dispatch rechecks the selection, record type, scope, state and applicable policy.

## Model consistency

- The complete Mendix consistency check returned **No errors found** after saving Studio Pro's Update security result.
- Readback covered all **79** application Data Grid 2 tables: checkbox multiselect, select-all and the shared toolbar; **zero** ActionButton widgets in record columns.
- Create is initialized independently of row selection and routes to existing creation workflows. Read-only history and derived projections do not gain arbitrary CRUD workflows.
- Row operations were moved to selection toolbars, including their context bindings and original confirmations. Existing guarded bulk edit, account audit, competition locking and feature-specific server workflows remain in use.
- The club column in My registrations and the member dashboard uses a string-compatible text filter. The registration search retains explicit member, season and next-action attributes inside its grid's filter area. Permission-controlled player-list columns retain their projection context.
- New dialogs are under Accounts/Records and Accounts/Retention, with typed business wrappers and lifecycle guards in their feature folders.

## Compilation and focused checks

- The changed Java sources compiled against the Mendix 11.15 runtime.
- The full Studio Pro application build succeeded (`build_clean-custom-classes_compile_package_gradle_log.txt`: BUILD SUCCESSFUL).
- `docs/development/tests/TableActionChecks.java` executed successfully. It covers longer-retention precedence, indefinite retention, organisation/global permanent-deletion prohibitions, automatic-purge opt-in, and CSV formula neutralisation, quoting, multiline and Unicode values.
- The local runtime started at http://localhost:8080/; the Mendix runtime handler reported running=true. The browser reached the application's sign-in page.

## Runtime scenarios still requiring an authenticated session

The test browser has no authenticated app session. The following are **not verified** by the model check, compilation, seeded examples or standalone policy checks:

- CRUD/feature visibility for ordinary users, club staff, federation staff and platform administrators, including empty tables and mixed selections.
- Toolbar selection refresh, view/editor navigation, create-parent selection, CSV download and existing bulk-edit/audit behavior.
- My registrations name search, club text filtering and combined enum filters against actual registrations.
- Archive/restore, direct delete protection, dependencies, deactivation and expiry/purge behavior against disposable business data.
- Fresh sample generation, upgrading an existing batch twice, preservation of edited demo records and scoped removal.

No destructive runtime scenarios, account creation, email sending, invoice confirmation or payments were executed for this verification.

## Retention behavior and development data

Delete permanently removes selected records after all records pass preflight; dependencies prevent cascades. Archive preserves a record, disables existing active/published flags and blocks editing until Restore. Restore keeps those flags disabled. Global defaults are 30 days with automatic deletion **off**. Club/federation rules can extend retention, retain indefinitely, block permanent deletion or disable automatic purge. The daily private job re-evaluates current policy and dependencies; blocked records do not prevent later eligible records from being considered.

Development sample feature `features:table-actions-and-retention-v1` adds a fictional archived, unpublished notice and a club policy with indefinite retention and automatic deletion off. Both new persistent metadata types, owning associations, dependency edges and derived records participate in the private ownership manifest. The upgrade marker prevents repeating the extension; existing policies are preserved. These generator paths were added but were not executed during this verification.
