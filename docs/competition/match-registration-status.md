# Match registration - 15 October 2026

24 October update: [league standings and result settings](standings-and-league-results.md) adds team tables and calendar winners, league-level competition points and optional game breakdowns. This supersedes the competition-table limitation below; player rankings remain outside scope.

Implemented in the Mendix model and connected to the Java service. Final validation has the limits below.

## Where to use it

- **Competitions > My federations > federation > Match formats and eligibility**: configure versioned scoring, slots, effective availability, review windows, warning checks and eligibility facts.
- **Division workspace > Match registration and format**: select the division format; every poule/subdivision inherits it. A started season requires federation override permission and a reason. Existing fixture snapshots remain fixed.
- **Division fixtures > Register match**: captain claims, home captain appointment of the official, hidden lineups, official start, result registration, submission, acceptance/dispute and federation rulings.
- **My matches** in navigation: fixtures assigned to the signed-in captain or official, including officials with no club-management role.

The 12 feature pages use non-persistent forms. The seven enumerations, eight private persistent match types, two temporary view types and their associations were added through mnd. There are 61 view/action/data-source microflows plus the private initializer and acceptance sweep. Java-action model editing is read-only in mnd; the existing String/Void action is retained and wrappers pass a temporary form GUID through the verified bridge. Only Mendix's Update security reconciliation required Studio Pro in this continuation.

## Behavior

Formats cover point-based sets (target/margin/cap), tennis/padel games, set tie-breaks and deciding match tie-breaks, and team totals. Singles/doubles/team slots, discipline, order and per-slot scoring overrides are configurable. Used versions are immutable; availability is checked against the division's event start, allowing overlapping versions. Advantage/golden/star choices are metadata because individual rallies are outside this tool.

Captains see their own setup before fixture start. An official who is also a captain cannot use that role to inspect the opposing setup before start. An independent official or federation override can review both. The official acknowledges eligibility warnings. Membership, dated registration, suspension, discipline, appearances and classification/index checks warn; they do not veto sporting eligibility. Substitutions require the official to attest that a contest is unstarted, require a reason and log previous/replacement identities. Completed or scored contests cannot be substituted by an official.

The complete-score editor saves every set and outcome atomically, including federation corrections. Draft set entry remains available. Retirement retains partial scores and awards the opponent. Walkovers use a forfeit with an empty score; a forfeit retaining a contrary recorded score requires federation authority. Recorded and official winners are displayed separately. A federation exclusion removes fixture and ranking credit; ranking-only exclusion is also available. Every ruling requires a reason and audit record. Fixture points are calculated only from completed results; adjustments remain separate. No player ranking or competition ranking table is calculated.

Review windows support scheduled/actual/submission anchors, early acceptance, sequential home/away periods and a minimum review interval after late submission. Submission by a captain who is also the official counts as that captain's acceptance. Disputes stop automatic acceptance. Federation corrections finalize without further captain approvals. The private startup initializer preserves UnitTesting.Startup and registers the native Mendix acceptance sweep every minute. The mnd connector cannot create ScheduledEvent documents, so the initializer uses Mendix Core.scheduleAtFixedRate to run a private microflow; the database mutation claim protects concurrent runtimes.

## Access

The user explicitly approved the temporary form boundary on 15 October 2026. Approved inputs are writable only on non-persistent forms; identifiers, revisions, permission flags, generated rows and calculated fields remain read-only. Persistent match storage has no client access rules. Each service command independently checks the current user, fresh record types, federation/club scope, appointed role, revision and applicable state. The exact boundary is recorded in match-view-access-proposal.json.

## Development examples

DevelopmentSamples.java extends fresh and existing batches using features:match-registration-v1 and features:match-review-v2. It adds five sports' formats/divisions, inherited subdivisions, slots, fixtures, lineups, scores, effective eligibility and audit examples, plus six separate retirement, contrary-forfeit, exclusion, dispute, late-submission and unstarted-substitution scenarios. All generated business records join the ownership manifest, cleanup allowlist and dependency map. Existing demo business records are preserved. Accounts remain fictional and disabled; invoices remain unpaid/unverified; no mail or payment actions are performed.

The local platform-administrator **Verify development examples** action runs fresh generation, repeated generation, ownership, disabled-account and scoped-cleanup checks in a transaction that always rolls back. It also verifies existing-batch upgrade preservation when a batch exists. Startup probes cannot run this administrator-only action. No sample generation runs on startup.

## Verification

- **Model consistency:** final mnd global check returned No errors found, including pages, microflows, access and navigation.
- **Compilation:** Java 21 compilation against the Mendix 11.15 public API passed. Studio Pro's full application build passed and the runtime started. The existing CommunityCommons deprecation warning remains.
- **Standalone rules:** 11,066 assertions passed in MatchRulesTest (scoring endpoints, outcome exceptions, aggregation, tie resolution, early stopping, availability, lock timing and approval clocks).
- **Native runtime:** temporary form GUID lookup/copy/row transfer passed. A separate local probe passed 31 persistence/outcome assertions across all eight private match types, score and association storage, contrary forfeits, exclusions, temporary rows and rollback. The probe's business changes were rolled back and its startup microflow is excluded again.
- **Sample lifecycle:** not verified under an administrator session in this continuation. The startup probe was rejected by the platform-administrator guard; this is not a passing generation/upgrade/cleanup test.
- **Role/UI workflows:** not exercised with signed-in captains, independent officials and federation administrators. Forged-ID isolation, opposing-lineup privacy, concurrent/stale saves, substitutions, approvals, disputes and multi-set federation corrections still need those integration scenarios. Model consistency, seeded examples and storage probes do not establish those results.

## Boundaries

During the 2026-10-15 folder review, Studio Pro's console repeatedly reported `Acceptance expiry is a private system job` from `SE_Match_AcceptanceExpiry`. Automatic acceptance expiry therefore remains an observed runtime defect requiring correction and scenario verification. The earlier persistence probes do not cover a successful scheduled-expiry run. The folder reorganization did not change Java or workflow behavior.

This is final-result registration, with no live scorer and no ranking engine. Tournament integration can reuse formats but no tournament module was added. Federation format reassignment preserves already initialized fixture snapshots. Reordering currently requires a free order number; an official can use an unused temporary position when swapping two unstarted contests. Warnings perform several scoped roster queries; larger rosters have not been performance-tested.
