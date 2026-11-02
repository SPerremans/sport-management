# Entity access

How Mendix entity access and the app's custom access layer work together, and which module role can reach which entity. Last reviewed 1 November 2026, against the live model in Studio Pro.

## The two layers

| Layer | What it controls | Where it lives |
|---|---|---|
| Entity access | What a client can read or write directly, for example in a data grid, an association path or a page field | Access rules in the domain model |
| Custom access | What a command may do, and for which club or federation | Allowed roles on microflows, then scope checks inside them and in Java services |

They do not overlap much, and neither replaces the other:

- **Most client-callable microflows skip entity access.** Of 722 microflows, 470 can be called by a role and 457 of those do not apply entity access. Their only protection is the allowed roles and their own checks, so an entity rule does not limit them.
- **Removing or narrowing a rule cannot be judged from the rule alone.** Pages and the 13 microflows that apply entity access use the rules. Java using a user context does too: `TableLifecycleService` checks `readable(user, row, table)` that way.
- **Having no rule is the strictest setting.** A client cannot reach the entity at all, and server code still can. Adding a rule to a private entity would widen access, not protect it.

**Scope is mostly narrowed in the custom layer, not in entity rules.** A wide entity rule is therefore not a defect in itself, and new narrowing should normally go into the custom checks. Entity rules still need to hold the limit in one case: a custom check runs only when its microflow or service is called, so it does not cover what a client reads or writes directly under an entity rule.

The main custom checks are:

- `SUB_Club_AdministratorOrOwner`, for club management
- `SUB_Federation_HasAccess`, for federation management
- `SUB_Competition_ClubPermission` and `SUB_Competition_FederationPermission`, for competition and rankings
- `MatchRegistrationService`, which checks the current user, scope, appointed role and revision for each command
- `RankingService`, reached through `MatchRegistrationBridge` and `JA_MatchRegistration`
- `TableLifecycleService`, for table actions and retention
- `AdminUserView`, for View as user. It opens a session with the target user's real roles, so entity access applies normally during the view. It runs in local development only.

Staff permissions (`ClubRole` and `ClubPermission`) appear in both layers: the XPath of many `ClubAdmin` rules reads the same `Resource` and `CanRead`/`CanUpdate` values the microflows check.

## Roles

Security level: **Production** (`CheckEverything`). Guest access is on. Demo users are on, which should be reviewed before a production deployment.

| User role | MyFirstModule roles | UserManagement role |
|---|---|---|
| Administrator | User, ClubAdmin, FederationAdmin, Administrator | Administrator |
| User | User | Member |
| ClubAdmin | User, ClubAdmin | Member |
| FederationAdmin | User, FederationAdmin | Member |
| Guest | Guest | Guest |

A person's rights are the union of their module roles. A ClubAdmin also has everything in the User column, and the Administrator user role has every column.

## How to read the tables

Each cell combines all rules for that module role.

- **R**: read only. **RW**: at least one member is writable.
- **+C** and **+D**: the client may create or delete.
- **scoped**: every rule has an XPath constraint. **all**: at least one rule has none.
- **-**: no rule.

Write access usually covers only some members. Keys, tokens and account links are mostly set to no access.

## Persistent entities with rules (47)

| Entity | User | ClubAdmin | FederationAdmin | Administrator |
|---|---|---|---|---|
| AccountAudit | - | - | - | R all |
| AccountExport | - | - | - | R scoped |
| AccountInvitation | - | R scoped | - | R all |
| BillingPlan | - | R scoped | - | R all |
| Club | R scoped | RW scoped | R scoped | RW+C all |
| ClubBillingPolicy | - | RW scoped | - | RW all |
| ClubCompetitionPolicy | R scoped | R scoped | R scoped | R all |
| ClubDirectoryCity | R all | R all | R all | R all |
| ClubEvent | R scoped | RW scoped | - | RW all |
| ClubPermission | - | RW scoped | - | RW all |
| ClubRole | - | RW scoped | - | RW all |
| ClubStaff | - | RW scoped | - | RW all |
| CompetitionAudit | R scoped | R scoped | R scoped | R all |
| CompetitionFixture | R scoped | R scoped | R scoped | R all |
| CompetitionLeague | R all | R all | R all | R all |
| CompetitionPlayer | R scoped | R scoped | R scoped | R all |
| CompetitionPoule | R all | R all | R all | R all |
| CompetitionTeam | R scoped | R scoped | R scoped | R all |
| CompetitionVenue | R scoped | R scoped | R scoped | R all |
| Federation | R scoped | R scoped | RW scoped | RW all |
| FederationAffiliation | - | R scoped | RW scoped | RW all |
| FederationCompetitionDelegate | R scoped | R scoped | R scoped | R all |
| FederationGender | R all | R all | R all, RW+C+D scoped | RW+C+D all |
| FederationRate | - | - | RW scoped | RW all |
| FederationRegistration | - | R scoped | R scoped | R all |
| FederationType | - | R scoped | RW scoped | RW all |
| FeeAdjustment | R scoped | R scoped | - | R all |
| FixtureChange | R scoped | R scoped | R scoped | R all |
| FormField | R scoped | RW scoped | RW+C+D scoped | RW all |
| FormResponse | RW scoped | R scoped | - | R all |
| Invoice | R scoped | RW scoped | R scoped | R scoped |
| InvoiceLine | R scoped | R scoped | R scoped | R all |
| InvoiceMail | - | R scoped | R scoped | R all |
| InvoicePdf | R scoped | R scoped | R scoped | - |
| InvoiceStyle | - | RW scoped | RW scoped | RW all |
| LeagueEntry | R all | R all | R all | R all |
| Membership | R scoped | RW scoped | R scoped | RW all |
| MembershipType | R scoped | RW scoped | - | RW all |
| NewsItem | R scoped | RW scoped | - | RW all |
| PlayerListRule | - | RW+C+D scoped | - | RW+C+D all |
| RegistrationRequest | RW scoped | RW scoped | - | RW all |
| RenewalOffer | R scoped | R scoped | - | R all |
| Season | R scoped | RW scoped | - | RW all |
| SeasonFee | R scoped | RW scoped | - | RW all |
| TableExport | R scoped | R scoped | R scoped | R scoped |
| TeamCategory | R all | R all | R all, RW+C+D scoped | RW+C+D all |
| VenueAvailability | R scoped | R scoped | R scoped | R all |

`InvoicePdf` has no rule of its own for the Administrator module role, because the Administrator user role reaches it through the other roles, limited to files the person owns. The Administrator rule on `Invoice` covers only `FederationAnnual` invoices.

No client role can delete any of these except where **+D** is shown. Deleting goes through microflows.

## Non-persistent entities with rules (44)

These are forms and server-built views. They exist only in the session that received them, so **all** does not mean every record in the database. Guarded microflows decide what goes into them.

| Entity | User | ClubAdmin | FederationAdmin | Administrator |
|---|---|---|---|---|
| AccountBulkEdit | - | - | - | RW all |
| AccountFilterChoice | - | - | - | R all |
| AccountSearch | - | - | - | RW all |
| AdminAccountForm | - | RW+C all | - | RW+C all |
| AdminAccountOverview | - | - | - | R all |
| ClubAdminAssignment | - | - | - | RW+C all |
| ClubBulkEdit | - | - | - | RW all |
| ClubCapabilities | - | R all | - | - |
| ClubDiscoveryChoice | R all | R all | - | R all |
| ClubDiscoveryOffer | R all | R all | - | R all |
| ClubDiscoveryProfile | R all | R all | - | R all |
| ClubDiscoverySearch | RW all | RW all | - | RW all |
| ClubOffer | R all | - | - | - |
| ClubPlayerEntry | R all | R all | R all | R all |
| ClubPlayerList | R all | R all | R all | R all |
| ClubSearch | RW+C all | - | - | - |
| ClubStaffAssignment | - | RW all | - | - |
| CommentDialog | RW all | RW all | RW all | RW all |
| CompetitionForm | RW all | RW all | RW all | RW all |
| CompetitionLeagueBulkEdit | RW all | RW all | RW all | RW all |
| CompetitionSportSection | RW all | RW all | RW all | RW all |
| CompetitionTeamBulkEdit | RW all | RW all | RW all | RW all |
| CompetitionVenueBulkEdit | RW all | RW all | RW all | RW all |
| FederationAdminAssignment | - | - | - | RW+C all |
| FederationBillingForm | - | - | RW+C all | RW+C all |
| FederationBulkEdit | - | - | RW all | RW all |
| FixtureCapability | R all | R all | R all | R all |
| InstalmentDecision | - | RW+C all | - | - |
| MatchForm | RW all | RW all | RW all | RW all |
| MatchRow | RW all | RW all | RW all | RW all |
| PasswordResetForm | - | - | - | - |
| PlayerDirectoryEntry | R all | - | - | - |
| PlayerDirectorySearch | RW all | - | - | - |
| RankingForm | RW all | RW all | RW all | RW all |
| RankingRow | RW all | RW all | RW all | RW all |
| RankingState | R all | R all | R all | R all |
| RegistrationApproval | - | RW all | - | - |
| ScrapedRunForm | - | - | - | R+C all |
| ScrapedRunRow | - | - | - | R all |
| TableCreateChoice | R all | R all | R all | R all |
| TableCreateForm | RW all | RW all | RW all | RW all |
| TableRecordView | R all | R all | R all | R all |
| TableRetentionForm | RW all | RW all | RW all | RW all |
| TableSelection | R all | R all | R all | R all |

`PasswordResetForm` is the only `MyFirstModule.Guest` rule (read/write plus create). The other Guest rule in the app is on `UserManagement.Signup`.

## UserManagement

| Entity | Role | Access |
|---|---|---|
| Signup (non-persistent) | Guest | Read/write plus create |
| Person | Administrator | Create. Read/write on name, email and `Club_Admins`. No access to `InvitationPending` and `Federation_Admins`. |
| Person | Member | Own record only. Writes `FullName`; reads `Email` and `Club_Admins`. No access to `InvitationPending` and `Federation_Admins`. |

## Private entities: no rules, on purpose

Each of these has "Private: no client access rules" in its entity documentation, so the choice is visible in the domain model. No page refers to them.

- **Persistent (28):** AffiliationMutationClaim, BillingClaim, CompetitionGate, CompetitionMutationClaim, DevSampleBatch, InvitationClaim, MatchAudit, MatchComment, MatchContest, MatchEligibility, MatchFormat, MatchLineup, MatchRegistration, MatchSet, MatchSlot, MatchSquadPlayer, MatchSubstitution, PaymentVerification, PlayerRanking, RankingChange, RankingDataset, RankingDiscipline, RankingLevel, RankingMoment, RankingScenario, RankingScheme, RecordLifecycle, TableRetentionPolicy
- **Non-persistent (4):** AllocationChoice, FederationBillingQuote, FederationBillingSlice, InvitationDelivery

The user approved this boundary for match storage on 15 October 2026 (see [match-registration-status.md](../competition/match-registration-status.md)). Rankings follow the same pattern (see [rankings.md](../competition/rankings.md)).

Because of these entities, **App Security shows entity access as Incomplete for `MyFirstModule`, and this is accepted**. `UnitTesting` also shows Incomplete, because the Marketplace entity `UnitTestContext` has no rule. The overall app status therefore stays Incomplete. The user chose on 1 November 2026 not to add no-rights rules just to clear the warning.

## Adding an entity or attribute

- New attributes start with no access on persistent entities, because every persistent rule now has a default of **None**. Choose the rights in each rule after **Update security**. A page that uses the attribute shows a consistency error until you do.
- Non-persistent form rules keep their default of read or read/write, so new form fields follow the rule's default.
- Give a new persistent entity rules only if a client must read it directly. Otherwise leave it private, say so in its documentation, and add it to this page.

## Open findings

Not changed, because each one could change current behaviour. These are direct client access points to weigh, not a list of rules to tighten: where the custom layer is the intended limit and the direct read is acceptable, the finding can be closed as it stands.

| Finding | Effect | Recommendation |
|---|---|---|
| `FederationAffiliation_Club` is writable for FederationAdmin | A client could point an affiliation at another club, bypassing the claim and approval flows | Kept writable: the club picker on `Affiliation_NewEdit` writes it, so read only would break that page. To close this, choose the club in a non-persistent form and set it in `ACT_FederationAffiliation_Save`. |
| `CompetitionLeague`, `CompetitionPoule` and `LeagueEntry` have an unconstrained read rule | Any signed-in user can read every league, including leagues still being set up, and `ClubKey`/`EntryKey` | Decide whether that matches the competition design; if not, limit by `Stage` and hide the keys |
| Internal keys are readable, such as `Invoice.BillingKey` and `RegistrationKey` on `RegistrationRequest` and `Membership` | People can read them on their own records | Set them to no access, after checking they are not used on pages |
| `DS_ClubSearch_Create` and `ACT_ClubSearch_Search` allow Guest, but `ClubSearch` has no Guest rule and no Guest page uses it | Unused route | Remove Guest from both microflows |
| Six pages have no allowed roles in the files on disk: `MyAccount`, `AccountBulk_Edit`, `Invoice_Detail_Styled`, `ChangeMyPasswordForm`, `ChangePasswordForm`, `ScrapedRun_Import_DeleteMe` | Page access, not entity access. The live model confirms this for `AccountBulk_Edit`; the others were not rechecked. | Check them in Studio Pro; some may be unused |

[`docs/phase3/security-review.json`](../phase3/security-review.json) still says "draft for explicit approval". It is out of date; this page describes the current rules.

## Narrowed on 1 November 2026

Approved by the user:

- `Club.BillingEmail`: no access under the `User` rule. Club administrators of that club and Administrators keep their access. No page or microflow file refers to this attribute.
- `TeamCategory_Federation` and `FederationGender_Federation`: read only for FederationAdmin. `ACT_TeamCategory_Create` and `ACT_FederationGender_Create` set them on the server without entity access, and `Federation_Workspace` only reads them in grid XPath. Administrators keep write access.

## What was reviewed and how far

- Rules, roles and defaults came from the live model.
- Microflow and page figures came from the `mprcontents` files, which may lag behind unsaved renames.
- The scope checks inside the 457 microflows were not audited one by one. A text search flagged 31 with no obvious check for further reading; the ones opened so far take their scope from a parameter the client must already be able to read, or return public data.
- No role-based runtime scenario has been run against these rules.
