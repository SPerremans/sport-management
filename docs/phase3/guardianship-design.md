# Shared guardianship proposal

Status: reviewable design; not implemented or activated. The Mendix document API currently times out, and expanded access still needs explicit approval.

## Person and login

Keep UserManagement.Person as the existing shared login account. Introduce MemberProfile for the person who joins a club. A profile can optionally link to a login; a child does not need an email address or password. An adult account initially gets its own profile. Memberships, registration requests, renewal offers and invoice beneficiaries link to the profile.

Backfill one profile per existing Person using an idempotent migration. Keep existing account associations during migration, then update every registration, duplicate check, dashboard, federation projection and payment guard to use the beneficiary profile. Two children sharing one payer remain distinct members; two guardians of one child do not create two memberships. Do not activate guardian access while any workflow still uses the current login as the member identity.

## Several guardians, one main guardian

GuardianLink joins an account to a profile and contains Active/Pending/Revoked state, verification timestamp, CanRegister, CanPay and CanViewMemberships permissions. Profile.MainGuardian is a single reference to a GuardianLink. When a profile has active guardians it must reference exactly one active, verified link belonging to that same profile. This is a server-validated invariant, not a client-editable IsMain checkbox.

A signed-in adult can create a new dependent profile and become its first verified main guardian. This cannot claim an existing person's profile. Linking another existing person requires that person's consent or a platform-admin verification workflow with an audit record.

Only the profile's own account or its current main guardian can invite another guardian. Invite acceptance requires a sent, unexpired single-use token and the intended recipient's signed-in account. The new link remains Pending until acceptance; an email address match by itself gives no access. With no email delivery configured, these invitations remain queued.

The main guardian can transfer the main role to another active verified guardian of the same profile in one transaction. Revoking a main link requires transfer first, or removal of all links by a profile that has its own account. A dependent without a login must retain a main guardian. Platform-admin recovery is audited. Another guardian cannot promote themselves or increase their own permissions.

## Registration and invoices

The registration page starts with a person selector: Myself or a dependent for whom the account has active CanRegister permission. Required club forms are completed for that person. Duplicate detection uses profile + club + membership type + season, independent of who submits or pays.

Invoice.BeneficiaryProfile identifies the member. Invoice.BillingContactAccount snapshots the main guardian when issued, and Payment.PayerAccount records the actual payer. Only the main guardian receives the billing email by default. Other active guardians with CanPay can view and pay issued invoices for that dependent. Membership and private registration-answer access is a separate permission.

A guardian can pay an entire season invoice or an approved instalment invoice, and decline a next-season renewal if allowed to manage that registration. Declining does not cancel the current season. Payment provider references remain unique; an invoice is marked Paid only from a verified callback matching amount and currency. A second guardian opening checkout cannot cause a second successful payment against the same invoice.

Transfer changes future billing contacts; issued invoice issuer/recipient details remain snapshots. Revocation immediately removes delegated access on the server, including from existing browser sessions. Guardianship does not grant club or federation administrator privileges.

## Screens

- My family: own profile, dependents, pending invitations and each dependent's main guardian.
- Dependent details: memberships, registration status and invoices according to the account's permissions.
- Guardian management: invite, permissions, transfer main role, revoke.
- Registration: explicit beneficiary selector and full-season fee by default.
- Invoice: member name, billing contact, amount, renewal decision and payment action.

## Access and validation

Members read their own profile and the profiles for which they have active verified delegated permissions. GuardianLink fields and main-guardian associations are read-only; guarded server actions make changes. Club admins see their club's membership/request data and only the contact information supplied for those records. Federations receive only their registration projection; they cannot browse family links, unrelated memberships, club form answers or accounts.

Tests required before activation: two guardians/one child, two children/one payer, revoked guardian with an existing session, forged profile parameters, self-promotion attempts, main transfer/revocation invariants, reused/expired/wrong-recipient invitations, concurrent checkout and duplicate registration from different guardians.

