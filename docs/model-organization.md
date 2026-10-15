# Studio Pro model organization

Pages and workflows belong to the business feature they implement. The module root holds required singleton documents and feature folders. CRM and implementation phase numbers are not application-wide categories.

## Folder guide for MyFirstModule

| Folder | Responsibility |
| --- | --- |
| Accounts | Platform accounts, administrator assignment, invitations, password reset, administrator View as user entry point, and their data sources |
| Billing | Billing policies, federation billing, invoice generation and related jobs; Invoices, Instalments, Appearance and SeasonPolicies contain narrower concerns |
| Clubs | Club details and workspaces; Administration contains club roles and staff assignments |
| Competition | Competition, league, team, venue, registration, scheduling and fixture management |
| Competition/Results | Dynamic match registration: formats, slots, division assignment, lineups, scores, eligibility, official assignment, approvals and rulings; includes the Match pages and JA_MatchRegistration |
| Competition/Results/Configuration | Match configuration enumerations |
| Competition/Results/System | Private startup and acceptance-expiry workflows |
| Competition/Tests | Development checks and the excluded runtime probe |
| Content | Club events and news |
| Development | Local administrator sample-data generation and removal |
| Discovery | Club and player directories |
| Federations | Federation details, affiliation, membership types and rates |
| Members | Member dashboard, membership editing, membership types, renewal pages and lifecycle workflows |
| Registration | Registration forms, fields, overview, approval and season lookup |
| Seasons | Season dates, fees, permissions and season selectors |
| Team | Existing club team support |
| Enumerations | Existing shared enumerations; new feature-specific enumerations should stay with their feature |
| Logic | Existing shared/integration workflows; put new feature workflows in their owning feature folder |

`Phase3` was a historical delivery-stage bucket mixing accounts, federation administration, billing and renewals. Its 11 pages and all supporting workflows have been redistributed to the folders above. The empty `CRM` and `Phase3` folders have been deleted through Studio Pro.

UserManagement/Login and UserManagement/Signup are grouped under that module's Accounts folder alongside the signup workflows.

Some existing validator document names still end in `_Phase3`. Their names are retained to preserve qualified references; they now live in the relevant business folder.

## Creating or changing documents

1. Choose the business folder before creating a named page or workflow.
2. Prefer mnd MCP and provide the folder when creating the document. If using page generation, create the page shell in that folder first, then populate the existing page.
3. Move existing documents in Studio Pro when the connector has no supported move operation. Preserve document identities and qualified names; do not recreate pages to change their location.
4. Verify the saved containment using the connector's folder listing and document reads, then run model consistency diagnostics.
5. Add development examples for new business behavior as required by AGENTS.md. Folder-only moves need no new scenarios.

## Verification of this reorganization

- Model consistency: global mnd diagnostics returned **No errors found** after the moves and empty-folder deletions.
- Folder inventory: all active MyFirstModule pages are in business folders. All 12 Match pages and the Java action are under Competition/Results. Connector folder listings confirm CRM and Phase3 are absent.
- Connector limitation: the excluded Invoice_Detail_Styled page is displayed inside Billing/Invoices in Studio Pro, but folder reads omit it and find-document reports an empty path for it. This is recorded as an inventory limitation rather than a loose active page.
- Compilation: not rerun for these containment-only changes.
- Runtime scenarios: not rerun for these containment-only changes. Existing runtime validation limitations remain in competition/match-registration-status.md.
