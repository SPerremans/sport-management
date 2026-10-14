# Competition workflow — 12 October 2026

The competition workflow is implemented in the open Mendix app. The latest model consistency check reports no errors. The application starts with the normal UnitTesting.Startup initializer. Test execution is not attached to normal startup.

## Using the application

1. Open **Competition** from navigation. A federation owner opens their federation workspace and creates a league, season dates, registration window, negotiation window, fixture duration, poule capacity and single or double round-robin format. Owners can assign existing accounts specific competition delegation rights.
2. A club owner or staff member with Competition permissions creates a team with the matching season name. Add active club members whose membership type has **Competition player** enabled. Members must have a beneficiary identity and a club season covering the whole league season.
3. During the registration window, choose a league and submit the team. The club needs an active federation affiliation covering the competition season. Duplicate team registration in the same federation and season is blocked. Rosters lock when registration closes or allocation begins.
4. Clubs create venues, dated availability windows with capacity, and daily/concurrent hosting limits. Availability windows for one venue cannot overlap.
5. After registration closes, the federation allocates poules. Automatic allocation respects maximum size and separates teams from the same club. A requested smaller poule count that forces a same-club pairing requires an explicit override and reason. Singleton poules are flagged for review. Federation users with allocation rights can move entries between poules or compatible leagues before fixtures exist.
6. Generate round-robin fixtures, then plan them. Draft planning considers venue hours, home-club hosting limits, team and player commitments across leagues, and one fixture per team/player per UTC day. Unplaceable fixtures remain unscheduled with an explanation. Review and manually place drafts, then publish only after every fixture validates.
7. Within the negotiation window, either participating club may propose a new date and home venue slot. Both clubs approve independently. The second approval rechecks the fixture version and all capacity constraints before applying the change. Rejection leaves the fixture unchanged. Federation override requires its own permission and a reason; hard hosting and player constraints still apply.

## Security and structure

Persistent competition records are read-only to clients. Guarded actions perform fresh database retrievals and permission checks before writes. Club role permissions use the **Competition** resource. Federation delegates have separate league-management, allocation, scheduling and override flags. Player identifiers are private. Guests have no competition access.

Competition pages and flows are grouped under **MyFirstModule / CRM / Competition**, with flows organized by Configuration, Leagues, Teams, Registrations, Venues, Poules, Fixtures, Planning, FixtureChanges, Permissions and Tests. ACT_ denotes user actions, DS_ data sources, SUB_ private reusable logic, and UT_ framework tests. Documents retain their qualified names and references when moved.

## Validation

The official Mendix **Unit Testing 10.0.8** module is installed. Twelve assertion-based tests call the actual placement validator with synthetic, uncommitted objects. The framework run recorded **12 passed, 0 failed** in the Studio Pro runtime console. See [test-results.json](test-results.json) and [test-cases.md](test-cases.md).

Only the Administrator app role has UnitTesting.TestRunner. Administrators can open **Checks** for the Nexio competition-checks page: a pass/fail summary, Run checks, Refresh results, and plain-language descriptions of all twelve scheduling checks. A separate Integration coverage tab explains the scenarios still to verify with real user roles. Technical report opens the original framework interface when detailed diagnostics are needed. The remote test API remains disabled. The default framework configuration rolls back microflow tests. Run tests in the local development environment; the production environment is not enabled for tests.

Registration, entity-access isolation, allocation, publication and two-club approval cases are documented separately and have not been exercised under real signed-in roles. Passing placement tests does not prove those integration scenarios.

## Practical limits

The planner is a greedy search on a 15-minute grid, not an optimization solver. Conservative overlap counts can leave feasible fixtures unscheduled. Competition day checks use UTC; fixtures cannot cross UTC midnight. Availability currently uses dated windows, without weekly recurrence. Team season names must exactly match the league season name. Post-deadline roster replacement and full team/venue edit-delete administration are not included in this version.

The earlier billing, mail, payment and guardianship work has its own remaining integration tasks; completing this competition implementation does not complete those integrations.

Framework references: https://docs.mendix.com/appstore/modules/unit-testing/ and https://marketplace.mendix.com/link/component/390.
