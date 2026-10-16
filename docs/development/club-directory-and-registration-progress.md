# Club directory and member registration progress

16 October 2026.

## Find a club

Both `Club_Directory` and the dashboard's Find a club tab show one comparison table: Club, City, Sport, Players and View club. Fees and registration actions remain on `Club_Detail`; nested fee tables have been removed from search results.

The filters are independent: club-name text, searchable city multiselect, sport text, age-group multiselect and playing-level multiselect. Multiple choices within a filter are alternatives; separate filters combine. Selected cities remain as removable chips while another city is searched. Empty selection means all. Results and city options are limited to published clubs. An empty result shows a clear message.

Club settings expose Sports (comma-separated text, allowing any sport). Audience settings have been replaced with a membership-derived summary; the old stored Club.AgeGroups/PlayingLevel values are retained for compatibility but are no longer read or edited by these views. Sports still use the explicit club setting; blank sports remain Not specified.

`ClubDiscoveryProfile` is a read-only, non-persistent projection shared by the directory and club detail. It uses only available fees in the current published season and membership types whose availability dates include today, matching `DS_Club_Offers`. `IsCompetitionPlayer` identifies Competition memberships; other offered memberships contribute Recreational. Categories and identical age ranges appear once even if multiple types/fees share them. Clubs with no current offers say No memberships currently available. No summary is persisted or refreshed in the client by a data-source microflow, so fee/type/date changes are reflected when the view reloads. The directory bulk-loads fees and membership types before looping; it does not run a database retrieve per club or fee.

Membership types define inclusive `MinimumAge` and `MaximumAge` in whole years. Zero means no lower/upper restriction; both zero display All ages. Negative bounds and a nonzero maximum below the minimum are rejected through the existing save validator. The editor, membership-type table, and available-offers table expose these ages. Summaries retain separate intervals, for example 6–17, 18–35, 40–65, rather than inventing coverage across gaps. A currently offered unrestricted membership makes the overall summary All ages. Youth means below 18; Adults means 18 and above. These are discovery descriptions, not automatic registration eligibility/rejection rules. Existing records default to zero bounds and are not rewritten.

Age-group and playing-level dropdowns filter the derived classifications, including the combined Youth and adults / Recreational and competitive choices. Directory rows reference their club and city; View club rechecks publication on the server. Existing scoped membership-type update permissions govern age edits. Projection attributes/references have read-only client access and no create/delete permission.

`ClubDirectoryCity` is a read-only reference catalogue. `Club.City` remains authoritative. `BCO_ClubDirectory_SyncCity` updates its derived association when a club is saved. Startup calls `ASU_ClubDirectory_BackfillCities` after the existing startup actions. The helper preloads cities, compares trimmed case-insensitive names in memory, batches new-city commits outside its loop, and returns only changed clubs. It does not send client refreshes or seed sample data. Empty city text clears the association; unused catalogue rows are not displayed by the directory.

The native installed Drop-down filter uses its caption attribute for searching when Caption source is Attribute. The hidden `refSearchAttr` setting is unused in this mode. Linked filter metadata is inferred from the enclosing grid. Do not set `linkedDs` to a `Pages$ListenTargetSource`: that is a row-selection source and caused a page-export exception despite clean model diagnostics. The exported name/sport metadata and city association all reference the enclosing club table's data-source ID; city captions/options use their separate options data-source ID.

## Member dashboard

Tabs: Important news, Upcoming events, My registrations, Invoices, Find a club. The former My clubs table is now under My registrations as Other memberships, limited to memberships that have no corresponding registration request. This retains imported/direct membership records without repeating a membership already represented by a request.

Registration columns are Club, Membership, Season, Status, Payment, Next action and Actions. `DS_Member_Registrations` retrieves only the current user's requests and orders them as drafts, awaiting approval, waiting list, awaiting payment, active, and closed history; newest submitted requests come first within each group. The data source performs no writes or client refreshes.

Club staff with Registrations Read and Update can put a pending application on hold. It remains Pending with Approval = OnHold and can subsequently be approved through the existing approval form. The former routine Reject button in the club workspace is replaced with Put on hold. Historical rejection records and their logic remain intact.

Members can withdraw their own draft, awaiting-approval or waiting-list application. The action reloads persisted ownership and eligibility, so another person's request or an approved request cannot be withdrawn by submitting an ID or using an old page. Withdrawal retains the request and its answers with Status = Withdrawn, cancels an unpaid registration invoice if present, synchronizes federation registration state, and treats a linked renewal offer as declined. A paid invoice requires club resolution. Existing active membership is not cancelled by withdrawing a renewal application.

`SUB_Registration_UpdateState` returns immediately for Withdrawn and Cancelled requests, preventing later state recalculation from reactivating them. Existing payment application already requires a Pending request with approved/not-required approval and Payment = Required. Holding an application does not create an invoice or activate membership.

## Development examples

Generate/upgrade sample data appends `features:club-directory-profiles-v2` once, with fictional clubs and unverified addresses in Antwerpen, Leuven, Gent and Mechelen, badminton/tennis/padel/table-tennis profiles and one unpublished profile. Existing club metadata is preserved. New sample clubs receive dedicated local test administrators using the existing compliant sample password; existing accounts are preserved.

`features:member-registration-progress-v1` appends six separate registration types/requests for a scoped sample club administrator: draft, awaiting approval, waiting list, payment required, active and withdrawn. The payment example has an unpaid, unverified invoice; no email is queued. New city records created by the generator are registered in the private ownership manifest; reused catalogue rows are not claimed. `DIRECTORY_TYPES` and the Club_DirectoryCity dependency cover cleanup. Cleanup retains its preflight refusal if an unrelated club later references a sample-owned city.

Both features run on fresh generation and existing-batch upgrade. Existing repeated-generation, ownership coverage, preservation and cleanup checks include these extensions. They have not been executed in an authenticated administrator runtime session during this change; examples are not passing workflow tests.

`features:membership-audience-v1` adds the fictional [DEMO] Schelde Rackets in Gent, once for both fresh and existing batches. It has junior competition (6–17), two differently priced adult competition options (18–35), recreational adults (40–65), and closed/future unrestricted options which must not influence the current summary. Existing sample records are preserved. Fresh profile examples now define their audiences on membership types instead of duplicating club audience settings. All persistent records use existing allowlisted types/associations and are added to the private ownership manifest, including any newly created city. The non-persistent projection needs no cleanup entry. This extension is compiled but its fresh/upgrade/cleanup scenarios have not yet run in an authenticated administrator session.

The fresh-generation and rollback-only lifecycle verifier now also call the actual native directory/detail/offer data sources. Assertions cover category and age deduplication, gaps between ranges, exclusion of closed/future options, agreement between views, one directory row per club, and absence of persistent writes while reading. These assertions are implemented and compiled; they have not been executed in this session.

## Verification

- Model consistency: global MCP diagnostics report No errors found.
- Compilation: the membership-audience update passed a full Studio Pro Java/package build (3 seconds); page export and Rspack bundling succeeded (last bundle 2026-10-16T21:12:39.471Z). Existing CommunityCommons deprecation and widget React warnings remain.
- Export verification: both directory grids use the non-persistent profile and retain city multiselect; the detail exports PlayingSummary/AgeSummary and offer AgeRange; the membership editor exports both age bounds; club editors no longer export manual audience controls. This checks generated wiring, not browser interaction.
- Runtime startup: the app started successfully with the new city-backfill startup action.
- Runtime scenarios: interactive city search/second selection, membership-age saves and invalid bounds, audience summaries, club profile save, waiting-list approval, withdrawal, ownership denial, stale-page approval race, and fresh/upgrade/cleanup scenarios have not been executed. Browser inspection remains blocked by the saved localhost policy. These are pending checks, not successful tests.

Studio Pro UI fallback was used for the two workflow concurrency messages and installed-widget definition refreshes, because MCP did not retain those settings correctly. Model edits otherwise used MCP; Java sample and documentation changes use ordinary file edits.
