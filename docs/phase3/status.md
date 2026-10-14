# Phase 3 status — 11 October 2026

> Update, 12 October 2026: the save fault and model consistency errors described below are historical. The current app passes consistency checks and starts locally. Competition implementation and its twelve passing Mendix framework tests are documented in ../competition/README.md. This historical phase-3 inventory has not been re-audited; mail, payments, billing integration and guardianship must not be inferred complete from the competition work.

This work is not complete or deployed.

## Created in the open Mendix model

Studio Pro currently cannot save the damaged SE_Billing_IssueDue document. Do not assume every recent in-memory edit has reached disk.

- Admin account-creation and invitation flows, password-setup form, token hashes, expiry, revocation and single-use redemption claim.
- Federations, scoped admin assignments, membership types, annual rates and club affiliations.
- Safe federation registration projections without club form answers.
- Annual census-based federation invoice drafts, club billing/renewal flows and invoice appearance settings.
- Fourteen phase-3 pages and the new Invoice_Detail_Styled page. The styled invoice page is excluded and has passed its expression check.
- Invoice branding snapshots and print styles in theme/web/_nexio-invoices.scss.
- Shared account, registration and verified-payment workflow integration has been started.

No new entity-access rules, app-role grants, or client microflow/page permissions have been activated. The user has now reconnected @mnd and elevated tool access. Scoped security changes remain prepared and will be applied only alongside valid, guarded workflows.

## Revised requirements prepared, not yet applied

See revised-billing.json and guardianship-design.md.

Federations will choose active-at-census, unique-during-year, or prorata-active-days calculation. Full-season payment is the default; instalments require a request and club approval.

Guardianship uses a separate member profile, multiple verified guardian links and exactly one main guardian when active guardians exist. Beneficiary identity is separate from the payer. The design is prepared; family account flows, access rules and beneficiary migration are not implemented.

billing-calculations.js is a standalone financial reference, not code used by the running Mendix app. Its checks covered census boundaries, overlapping memberships, leap years, partial years, unfinished-year previews, distinct beneficiaries and cent-exact instalment totals, including small amounts. These are not Mendix runtime tests.

## Current blocker and diagnostics

The dangling OriginPointer save fault was resolved on 2026-10-11 through Studio Pro and the Mendix MCP. The damaged generated SE_Billing_IssueDue document was replaced from its preserved source and SE_Billing_Daily was rebound. The rebuilt flow renders and Save All no longer throws the exception. See microflow-recovery-note.md for the backup and validation details.

The local Mendix MCP responds to microflow reads, writes, creation and search again after removing the corrupt document. Earlier timeouts were associated with the corrupt document blocking Studio Pro saves.

Latest consistency check (19): 29 errors.
- 29 permission-selection errors for the intentionally unapproved features.
- The duplicate-variable errors in SUB_Registration_SelectedSeason were repaired with minimal variable renames and an updated decision expression.
- SE_Billing_IssueDue has a connected, valid due-invoice loop and end event.

Corrected flow source plans are saved in flow-specs-recovery.json. The current app cannot be rebuilt successfully until the remaining access-role errors are resolved. The existing running deployment has not been updated.

## Integration work still required

- Configure SMTP/sender and the HTTPS application URL. Wire actual successful Send Email delivery to the private invitation delivery-success boundary; queued/prepared mail must not be marked Sent without delivery. No messages have been sent.
- Complete the password-reset route and guest navigation integration after page permissions are approved.
- Configure a Studio Pro scheduled event for the daily billing boundary; the MCP exposes no scheduled-event creation handler.
- Connect and test a payment provider. Server verification must match invoice, amount, currency and unique provider reference.
- Apply the revised on-request instalments and federation calculation choices, then implement the approved guardian profile migration/access design.
- Finish club/member navigation, billing links, filtered federation type selection, invitation requeue UI and runtime scenario validation.

## Required sequence to resume

1. Microflow document access and saving are restored.
2. Both interrupted microflows are repaired; existing account security was preserved during recovery.
3. Implement the revised billing rules and beneficiary/guardianship model.
4. Finish the exact expanded-access review, obtain explicit human approval, then grant scoped access.
5. Run full consistency checks and real role-based runtime scenarios before saying this phase is finished.

