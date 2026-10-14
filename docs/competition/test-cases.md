# Competition test cases

## Automated Mendix framework tests — passed

Open Administrator → Checks and select Run checks, then Refresh results. Technical report provides the original framework detail view for the **MyFirstModule** suite. Each UT_Competition_ microflow calls SUB_Competition_TestPlacementCase and UnitTesting.AssertUsingExpression, which invokes the real SUB_CompetitionFixture_CanPlace validator. No accounts, mail or committed business records are created. The observed framework result on 2026-10-12 is 12 passed and 0 failed.

| Test suffix | Scenario | Expected placement |
| --- | --- | --- |
| FreeCalendar | Active home-club window, empty calendar, valid duration | Allowed |
| ClosedAvailability | Inactive availability | Denied |
| WrongHostClub | Slot belongs to another club | Denied |
| BeforeOpening | Fixture starts before the availability opens | Denied |
| AfterClosing | Fixture ends after the availability closes | Denied |
| CapacityAllowsTwo | Two different fixtures fit venue and club capacity | Allowed |
| ExclusiveEndBoundary | Existing fixture ends exactly when another begins | Allowed |
| TeamOncePerDay | A participating team already plays that day | Denied |
| DailyHostLimit | Home club reaches its daily fixture limit | Denied |
| ClubConcurrencyLimit | Concurrent home fixtures reach the club limit | Denied |
| VenueCapacity | Overlapping fixtures exhaust venue capacity | Denied |
| SharedPlayer | A player belongs to two teams scheduled that day | Denied |

## Integration cases — prepared, not executed

Use a disposable local database. Create federations F1/F2, clubs A/B/C, season S, owner accounts and delegated accounts with individual rights. Create competition and ordinary membership types, active and expired members, and one shared beneficiary in different teams. Use dates relative to the run date; preserve the original fixture IDs, versions and timestamps before change tests. For denied actions, check both the message and absence of database changes. Test direct action calls as well as page navigation.

| ID | Action / setup | Required outcome |
| --- | --- | --- |
| REG-01 | A has active F1 affiliation, nonempty eligible roster, matching S; submit during window | One entry linked to A, its team and F1 league; audit recorded |
| REG-02 | Submit before opening, at closing, or after allocation | Rejected with no entry |
| REG-03 | Empty roster, ordinary membership type, expired member, wrong club or insufficient season coverage | Rejected; no registration |
| REG-04 | Affiliation missing, inactive or shorter than league season | Rejected |
| REG-05 | Submit the same team twice or to another F1 league in S | Exactly one entry; duplicate rejected |
| REG-06 | Add/remove roster member after closing or allocation | Rejected; roster unchanged |
| REG-07 | Select a member from another club by supplying its object reference | Rejected by server before selected name or reference changes |
| ACL-01 | A staff has Competition.Read but lacks Create/Update | May view A metadata; cannot register or change rosters/settings |
| ACL-02 | A staff invokes B action or requests B roster | Denied; no unrelated member identity exposed |
| ACL-03 | F1 delegate has Allocate only | May allocate F1; cannot create leagues, schedule, override or access F2 drafts |
| ACL-04 | Guest or ordinary unassigned member invokes management action | Denied; public signed-in league/fixture metadata remains limited to allowed content |
| ACL-05 | Non-administrator attempts UnitTesting runner or report page | Denied; Administrator can access it; remote API disabled |
| POOL-01 | 8 entries, maximum 4, no club has more than 2 teams | All entries assigned once; no poule above 4; same-club teams separated |
| POOL-02 | 3 teams from one club, max 4 | At least 3 automatic poules; no same-club pairing; singleton review message if applicable |
| POOL-03 | Request too few poules for club separation without override or reason | No allocation change; validation message |
| POOL-04 | Explicit override with reason then manual same-club move | Capacity still respected; reassignment and reason audited |
| POOL-05 | Move entry to F2, incompatible season, or league with fixtures | Rejected; existing assignments unchanged |
| FIX-01 | Poule of n teams, single round robin | n×(n−1)/2 fixtures, each unordered pair once |
| FIX-02 | Same poule, double round robin | n×(n−1) fixtures, reversed home/away for second leg |
| FIX-03 | Generate twice or before all entries have a poule | No duplicate pairs; invalid operation rejected |
| PLAN-01 | Sufficient windows for all pairs | Drafts placed inside home windows; no team/player same-day collision; limits respected |
| PLAN-02 | Insufficient windows/capacity | Unplaceable fixtures stay Unscheduled with Issue; no forced booking |
| PLAN-03 | Publish with an unscheduled or newly invalid draft | Rejected; no partial publication |
| PLAN-04 | Valid plan, then publish | All fixtures Published; league stage and audit updated |
| PLAN-05 | Lower club limits below already planned future fixture usage | Policy change rejected |
| CHANGE-01 | A proposes valid change during window | Pending proposal; original fixture remains unchanged |
| CHANGE-02 | A approves its side only | Pending; no fixture update; B approval still required |
| CHANGE-03 | B approves valid proposal after A | Exactly one fixture update, version increment, Applied proposal and audit |
| CHANGE-04 | Unrelated C approves, approval after window, or stale ExpectedVersion | Rejected; fixture unchanged |
| CHANGE-05 | Capacity becomes unavailable before second approval | Rejected; no invalid relocation |
| CHANGE-06 | Either participating club rejects | Proposal Rejected; original date retained |
| CHANGE-07 | Federation scheduling delegate without Override tries published move | Rejected |
| CHANGE-08 | Override delegate moves published fixture with reason | Valid placement applied; pending proposal superseded; old/new dates audited |
| CHANGE-09 | Override tries capacity or player conflict | Rejected despite override right |
| RACE-01 | Concurrent duplicate submissions, proposals or schedule mutations | Unique constraints/global mutation claim prevent inconsistent duplicate commits; losing transaction rolls back |

These cases require role/session and database integration execution. They are not counted in the twelve passing unit tests.
