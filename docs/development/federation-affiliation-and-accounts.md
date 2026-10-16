# Federation affiliations and account overview

17 October 2026.

## Club and federation workflow

Each active unisport federation specifies one Sport in Federation settings. Clubs open Settings → Federation affiliations, choose a federation, and request affiliation. The page lists all their affiliations and statuses. A club can have several approved federations. Duplicate pending/current requests for the same pair are refused. Club owners and staff with Settings update permission may request or withdraw; Settings readers see history without mutation controls.

Federation administrators use their workspace's Clubs table to approve or decline pending requests. Approval starts today and activates the affiliation; decline/withdrawal leave an inactive history record. A federation administrator may still create an approved affiliation directly through the existing administration form. Existing affiliations default to Approved when the new status attribute is introduced. They keep their active flags and periods. Status and request/decision timestamps are read-only to clients; server actions recheck persisted pending state and organization permissions. Pending requests cannot be activated in the date/active editor.

`AffiliationMutationClaim` is private, with a unique Key and owning Club reference. Club submissions claim a pair/history slot, and approval/decline/withdrawal share a per-request decision key. Conflicting concurrent club submissions or decisions roll back through the database uniqueness rule. Claims are not exposed to clients. The existing federation maintenance path still uses its scoped period/duplicate validation.

## Memberships and discovery

Membership type editing now includes Federation membership, captioned with federation name and type. Choices come only from approved, active, currently effective affiliations to active federations with a sport. A membership can remain an unavailable draft without a federation link; making it available requires a valid link. Club owners, platform administrators and delegated membership-type editors use the existing CRUD permission checks.

Club discovery and club detail derive sports from the federations behind eligible current-season membership offers, deduplicating case-insensitively. Playing categories and inclusive age intervals still derive from these same offered types. Closed/future types, pending/expired/inactive affiliations, unavailable federation types and inactive federations do not contribute offers or sports. Each offer displays its sport and federation. Both club editors have removed the manual Sports input; the old stored value is retained for compatibility but is ignored.

The directory bulk-loads membership types, fees and current approved affiliations. `SUB_MembershipType_EligibleFederation` performs only in-memory eligibility checks and association traversal. The pure date helper is `SUB_Type_WithinWindow`. Registration entry points use `SUB_Type_Available`, which now also checks affiliation eligibility, and submission checks affiliation independently of the renewal date-window exception. This prevents stale/direct registration actions from bypassing the approved affiliation requirement.

Past memberships, registration history and invoices are retained when an affiliation ends. Existing federations with no Sport require that field to be configured; no sport is guessed from names/descriptions and no older edited demo record is rewritten.

## Account overview

Platform administrators can open Application accounts from navigation or View all accounts on the existing account-invitations page. The compact shared table shows username, name, email, roles, status and last sign-in, with filters for name, email, role and status. It includes all non-anonymous System.User records, including MxAdmin, user accounts and disabled samples. Full name/email come from Person where present; system accounts use their username as the display name.

`DS_Admin_Accounts` explicitly verifies the Administrator application role and returns a read-only non-persistent projection. It never returns password fields, tokens or underlying account references, and changes no accounts. The page and projection are accessible only to platform administrators. Existing local-only View as user behavior is unchanged.

## Development examples

`features:multi-affiliation-and-account-overview-v1` is appended once to fresh and existing demo batches. [DEMO] Gent Multi Rackets has current approved badminton and tennis affiliations/offers, a pending padel request with an unavailable draft membership, an expired table-tennis affiliation, and declined/withdrawn history. Dates are relative to the current UTC generation date. Two disabled member accounts, one also blocked, illustrate account overview rows. Dedicated organization administrator creation follows the existing explicitly authorized local test-account behavior and compliant password policy.

Fresh directory/audience/status examples now link new membership types to owned federations with an explicit sport. Older user-edited records remain untouched; the upgrade adds separate examples instead. All new persistent types and associations are in the cleanup allowlist/dependency map; every generated ID, including the sample mutation claim and any new city, is recorded in the private ownership manifest. Non-persistent projections need no cleanup entry.

Fresh generation and the rollback-only lifecycle verifier additionally assert that native club/offer sources agree on the two approved sports, exclude pending/expired affiliations, retain all six affiliation examples, reject a stale available flag for the pending federation in the eligibility helper, and persist no records during reads. Existing repeated-generation, existing-batch preservation and scoped-cleanup checks include the extension.

## Verification

Model consistency, compilation and runtime checks are recorded separately in federation-affiliation-and-accounts-validation.json. Compiled examples/assertions are not executed runtime tests. Interactive approval/withdrawal, permissions, concurrency, membership saves, account filters and sample lifecycle scenarios remain unexecuted because the saved localhost browser policy blocks inspection. No sample generation is attached to startup.
