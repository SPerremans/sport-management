# Account archive: data impact overview

Status: **decided 24 October 2026 and implemented in the shared table lifecycle** (see *Implementation* at the end). The tables below were written as a proposal; the decisions that followed are listed under *Decisions*.

## Decisions (24 October 2026)

| Topic | Decision |
| --- | --- |
| Names on memberships and registrations | Pseudonymise (stable `P-…` code per account) so governance and competition-integrity links remain. |
| Invoice recipient name | Keep (accounting retention, about 10 years). Invoices are not changed. |
| Member names in federation billing | Keep (accounting documents). |
| Audit entries holding a login/email | Replace the login/email with the pseudonym; the entries themselves stay. |
| Audit retention | 10 years for account, competition and match audits; configurable. |

## Implementation

- **Archive, not delete:** the shared table toolbar's **Archive** on Application accounts creates the lifecycle marker and disables the account. **Delete** (permanent) is never offered for accounts. **Restore** works until the account is anonymised.
- **Grace period:** this is the table retention policy for accounts, set with the accounts table's **Retention** button. The global default is 30 days, so set 7 days there if that is the target.
- **Anonymisation:** the daily `SE_TableRetention_Purge` job calls `AccountAnonymiser` for archived accounts whose retention has passed, regardless of the automatic-deletion switch, because nothing is deleted. Keep indefinitely postpones it. The account keeps its lifecycle marker without a purge date.
- **What the anonymiser changes:**
  - Login, full name, email, roles, password, language and time zone are replaced or cleared.
  - Membership and registration names and emails are pseudonymised; registration answers are cleared.
  - Staff, delegate and invitation contact data is pseudonymised, open invitations are revoked, and club/federation administrator links are removed.
  - Admin-assignment, invitation-delivery and invoice-mail emails are replaced.
  - Action snapshots that equal the old login or email become the pseudonym.
  - One `Account anonymised` audit entry is written.
- **Not changed:** invoices, federation billing, and sport-game names (competition players, lineups, fixtures).
- **Audit retention:** the same daily job deletes `AccountAudit`, `CompetitionAudit` and `MatchAudit` rows older than the **Audit retention** policy. This is a page-level button on Application accounts, default 3650 days; Keep indefinitely disables the purge.
- **Renewal offers:** open offers are left unchanged (not closed) in this version.
- Code: `javasource/myfirstmodule/AccountAnonymiser.java`; hooks in `TableLifecycleService` (inspect, purge, audit policy, commit guard for the retention job).

Source of the original proposal: entity proxies in `javasource/*/proxies` (23 October 2026).

## Principles (from the product owner)

- **Delete** archives an account. Nothing is hard-deleted. The account record stays so every reference stays valid.
- Sensitive personal data is **anonymised**.
- Actions people took stay intact: approvals, decisions, payments and fixture/match actions.
- Names in sport games (rosters, lineups, fixtures, results) are **not** anonymised.
- **Deactivate** stays a separate, reversible action.

Proposed flow: **Delete** → account disabled and marked *Archive pending* → after the configurable grace period (default 7 days, restorable until then) a scheduled job anonymises → audit rows older than the configurable audit retention are removed.

Legend: **Anonymise** = value replaced (`Archived user #<id>`, `archived-<id>@archived.invalid`) or cleared · **Keep** = unchanged, record retained · **Close** = state change only.

## 1. The account itself

| Entity | Fields | Action |
| --- | --- | --- |
| `System.User` / `UserManagement.Person` | Name (login), FullName, Email | **Anonymise**. Email stays unique and required by using `archived-<id>@archived.invalid`. |
| | Active, Blocked, roles, password, InvitationPending, language, time zone | **Close**: inactive, all roles removed, random password, no pending invitation, preferences cleared. |
| | Record and id | **Keep**, so memberships, invoices and audit rows still resolve. |

## 2. Copies of contact data (anonymised)

| Entity | Link to the person | Fields anonymised | Kept |
| --- | --- | --- | --- |
| `Membership` | `Membership_Person` | MemberEmail; MemberName → `Archived member` ⚠️ | status, dates, type, club, season |
| `RegistrationRequest` | `RegistrationRequest_Person` | MemberName ⚠️, MemberEmail | approval, payment, fees, reasons, instalment decision |
| `FormResponse` | via the person's registration requests | Answer, AnswerDate, AnswerChoice (free answers can hold birth dates, phone numbers, health notes) | field label, kind, order |
| `ClubStaff` | `ClubStaff_Person` | MemberName, Email | role; IsActive → false |
| `ClubAdminAssignment` / `FederationAdminAssignment` | email | Email | assignment row; the person is also removed from `Club_Admins` / `Federation_Admins` |
| `FederationCompetitionDelegate` | `…_User` | Name, Email | permissions history; IsActive → false |
| `AccountInvitation` | `…_Person` | FullName, Email; TokenHash cleared; open invitations revoked | CreatedAt/CreatedBy, state |
| `InvitationDelivery` | email / username | Email, Username, Body (contains name and link) | Subject, timestamps |
| `InvoiceMail` | via the person's invoices | RecipientEmail | subject, delivery state, timestamps |

## 3. Financial records (kept for legal retention)

| Entity | Link | Proposal |
| --- | --- | --- |
| `Invoice` | `Invoice_Person` | **Keep** number, amounts, RecipientName ⚠️ (accounting law requires the invoice recipient). **Anonymise** RecipientEmail. Unpaid invoices stay open. |
| `InvoiceLine`, `FederationRegistration`, `FederationBillingSlice` | MemberName snapshots in federation billing | **Keep** (billed census record) ⚠️ |
| `BillingPlan`, `PaymentVerification`, `FeeAdjustment` | person / invoice | **Keep** (no contact data; AdjustedBy is an action snapshot) |
| `RenewalOffer` | `RenewalOffer_Person` | **Close** open offers (declined: *account archived*); keep history |

## 4. Action and audit history (kept)

| Entity | Fields | Proposal |
| --- | --- | --- |
| `AccountAudit` | Actor, Subject, Details | **Keep** rows; actions stay attributable through the account id. Removed only by the audit retention setting. ⚠️ Pseudonymise the Actor/Subject login snapshot when it is an email address? |
| `CompetitionAudit`, `MatchAudit` | Actor, Reason | **Keep** (sport-game actions) |
| `FixtureChange` | ProposedBy, reasons | **Keep** |
| `RegistrationRequest` | InstallmentDecisionBy | **Keep** (decision actor) |

## 5. Sport games (names never anonymised)

`CompetitionPlayer` (MemberName, PersonKey), `MatchLineup`, `MatchEligibility` (PlayerKey), `MatchRegistration`/`MatchForm` (LeaderName), `CompetitionFixture`, `CompetitionTeam`, `LeagueEntry`: **Keep as-is**.

## 6. Not affected

- Organisation data with contact fields that belong to the organisation, not the person: `Club` (Address, BillingEmail), `Federation`, `InvoiceStyle`, `CompetitionVenue`, seasons, membership types and rules.
- Transient views are rebuilt from the sources above, so they show anonymised values automatically: `AdminAccountOverview`, `AccountBulkEdit`, `AccountSearch`, `AdminAccountForm`, `ClubPlayerEntry`, `PlayerDirectoryEntry`, discovery and competition forms.
- Export files (`AccountExport`, `TableExport`) are deleted after download. Any left over for the archived user are deleted.
- Runtime log files (for example AdminUserView) live outside the database and follow the server's log retention.

## Configuration (new settings)

| Setting | Default | Meaning |
| --- | --- | --- |
| Archive grace period | 7 days | Time between **Delete** and anonymisation. The account can be restored until then. |
| Audit retention | to decide (e.g. 365 days) | `AccountAudit` rows older than this are removed. Sport-game audits are excluded unless chosen otherwise. |

## Open decisions (⚠️ above)

1. Membership and registration **names**: anonymise (proposed) or keep?
2. Invoice **recipient name**: keep for legal retention (proposed)?
3. Federation billing member names: keep (proposed)?
4. `AccountAudit` actor/subject snapshots that are email addresses: pseudonymise?
5. Audit retention: which period, and only `AccountAudit` (proposed) or also competition/match audits?
