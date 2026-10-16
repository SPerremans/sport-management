# Development sample data

16 October update: [club directory and registration progress](club-directory-and-registration-progress.md). The next Generate/upgrade adds four fictional Belgian club profiles and six member registration stages (including waiting list and withdrawn history), once per version. Sample-created city references are tracked for cleanup; existing club metadata is preserved. This extension has compiled but has not been executed in an authenticated administrator session.

Administrator user-view examples: [View as user](view-as-user.md). The next sample upgrade appends memberships to the sample club administrators so their own member dashboards contain examples. Existing accounts and user edits are preserved; disabled sample people remain disabled. This extension has not yet been executed in an administrator browser session.

15 October update: [club discovery, test administrator logins and shared tables](club-discovery-and-tables.md). The next Generate/upgrade adds fictional Belgian club examples and dedicated active administrators per sample organization with password `TestAccount123!`. The existing 12-character/mixed-case/digit policy is preserved. Other sample accounts remain disabled. Generation and login scenarios for this extension have not yet been executed; the earlier counts and disabled-account statements below describe the original sample set.

Match-registration extension (15 October 2026): the next generation/upgrade includes five separate sports' format, fixture, lineup, draft-score and eligibility examples. Its version marker and ownership cleanup are implemented, but the extension has not yet been executed in an administrator session. Match-registration screens are wired and model-checked; see [implementation status](../competition/match-registration-status.md).

Open **Checks → Sample data** as a platform administrator in the local Studio Pro runtime.

- **Generate sample data** creates one disposable batch. Repeating it also adds newly introduced feature examples to an older batch, once per feature version, while preserving existing sample work.
- **Remove sample data** removes the batch and new dependent records created through its clubs, leagues, teams, registrations or invoices. The page asks for confirmation. Unrelated accounts and older records are preserved. Removal refuses to proceed if an older record references a sample record; unlink it first.
- Remove and generate again to refresh the sample dates. Dates use UTC, matching the competition planner.

## Included data

Four fictional clubs marked **[DEMO]**, one federation, four current and four next club seasons, four previous seasons and four later draft seasons, four competition membership types, seasonal fees of €80–€110 (next season €5 higher), federation affiliation and annual rate, 48 active player memberships, 48 federation registrations, 12 teams with four distinct players each, four venues, 104 availability windows, hosting policies, four pending registration requests with form answers, events, news and unpaid example invoices. Previous/later seasons have their own fees and lie outside all existing season dates, including any dates already edited in the demo batch. Club coordinates are not populated by the generator.

There are 52 disabled ordinary User accounts: 48 players and four applicants. Eight additional disabled staff accounts hold the User and ClubAdmin app roles, each assigned only to their own sample club's secretary or competition coordinator role. Each role has a complete ten-row CRUD permission matrix; unsupported operations remain disabled. Secretaries manage registrations, while coordinators manage competition. Fictional addresses use example.invalid; randomly generated passwords are not displayed or retained separately. No invitations or email deliveries are queued, and no payment is confirmed. If a real Person account generates the batch, two sample memberships/invoices are linked to it without altering its account or roles.

## What to try

1. **Players:** search `[DEMO] Player` or a sample club name. The directory shows active memberships in published clubs, one result per person and club.
2. **Club administration:** a platform administrator can open every club workspace. Inspect registrations, member types, season fees and form answers. Existing club owners/staff retain their scoped permissions.
3. **Competitions → My federations:** open the sample federation, then **[DEMO] Ready to schedule**. Eight teams (two per club) are registered. Allocate two poules of four, generate double round-robin fixtures, plan and publish.
4. **Competitions → My clubs:** each sample club has a third team available to register in **[DEMO] Open registration**, which uses single round-robin.
5. **Venue planning:** each club has 13 weeks of two evening availability windows per week, capacity two, daily hosting limit three and concurrent limit two.
6. **Seasons:** the default view should contain two rows (current and next). **All seasons and history** also contains a previous season and a later draft. Earlier season dates and fees remain read-only for club staff; the platform administrator can edit them. Overlapping dates, including unpublished dates, are rejected.
7. **Team and roles:** open the sample Secretary or Competition coordinator to inspect a populated CRUD matrix and its staff assignment. Disabled staff accounts cannot sign in; these are development records, not shared test credentials.
8. Delete the sample batch using **Remove sample data** when finished. Do not use sample invoices for real payments.

## Implementation and validation

Private `DevSampleBatch` stores the exact original object IDs and original sample IDs. It has no client access rules. Both actions check the Administrator app role and require both Mendix development mode and the local Studio Pro runtime. Mutations run atomically in a separate system transaction. Deletion disables entity events and cascading delete behavior, then verifies the owned records are gone. Private concurrency/billing claim infrastructure is preserved.

On a new batch, the generator verifies duplicate prevention, protection of an existing linked record, cleanup of a newly added dependent event, preservation of unrelated records and regeneration inside the same transaction. It commits only after those checks succeed. The season/role upgrade extends the existing ownership manifest, keeps its original baseline, records a feature-version marker and checks that a second extension creates no duplicates. Cleanup covers the new records through the same manifest.

When adding another application feature, extend the generator and its upgrade path alongside that feature. Register any new persistent business entity and its owning associations in TYPES/EDGES so removal remains complete and scoped. A new feature must have examples in both newly generated and already existing demo batches; do not silently return early for an older batch.

The first local generation completed the original generate / duplicate / cleanup / unrelated-record preservation / regenerate checks, and the four clubs and two leagues were observed in the live app. The extra linked-record preflight check was added after that run and executes on the next newly generated batch. It is not claimed as executed in this session.

Normal startup calls **ASU_MatchRegistration_Initialize**, which preserves **UnitTesting.Startup** and registers the private result-acceptance sweep. No sample generation is attached to startup. The private verification startup draft is excluded.

The source Java actions were created through Studio Pro because the MCP treats Java actions as read-only. Public entrypoints are named `ACT_DevelopmentSamples_Generate` and `ACT_DevelopmentSamples_Remove`, under Development/SampleData; they call the private Java service.


## Match registration examples

`features:match-registration-v1` appends five sport formats and divisions (badminton, tennis, padel, table tennis and football), inherited subdivisions, fixture slots, hidden lineups, draft set scores, eligibility facts and audit history. `features:match-review-v2` appends six separate retirement, forfeit, exclusion, dispute, late-submission and unstarted-substitution fixtures. Appointments reference fictional disabled sample accounts; the generator never enables them. Existing sample business records are preserved. Both extensions run once and contribute every new business ID to the existing private ownership manifest.

From federation **Match formats and eligibility**, a local platform administrator can use **Verify development examples** to exercise fresh generation, repeated generation, existing-batch upgrades (when present), ownership coverage, disabled accounts and cleanup. It runs in a transaction that always rolls back. This new verification action was not executed with an administrator session during implementation: the startup probe was denied by the role guard. Seeded scenarios are example data, not passing workflow tests.
