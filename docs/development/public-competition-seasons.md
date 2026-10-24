# Public competition season examples — 24 October 2026

The [league standings update](../competition/standings-and-league-results.md) adds team tables and calendar winners, defines competition points at league level and makes individual games optional. The resource now includes credited provider totals for retirements and forfeits; raw scores remain unchanged. Its separate feature version backfills only untouched existing source sheets and policies.

The administrator-only **Checks → Sample data → Generate sample data** action appends this frozen source capture to an existing demo batch, once under `features:public-competition-seasons-2026-2027-v1`. Fresh generation includes the same capture after the older fictional examples. Existing records are not matched by name or rewritten. There are no runtime HTTP calls to sports providers.

| Published source | Level | Division documents | Poules/stages | Fixtures | Badminton result sheets |
| --- | --- | ---: | ---: | ---: | ---: |
| [Victor League 2026–2027](https://badvla.tournamentsoftware.com/sport/tournament.aspx?id=0c62bb49-5d47-4217-864d-ec86875b1c00) | National | 2 | 6 | 56 | 24 |
| [Vlaamse interclubcompetitie 2026–2027](https://www.toernooi.nl/sport/league?id=7c182df5-153d-443a-86db-c21a50e46372) | Flemish | 9 | 22 | 884 | 205 |
| [PBA competitie 2026–2027](https://www.toernooi.nl/sport/league?id=88774B10-ACF3-4F2F-B616-9A803C8B3880) | Provincial | 13 | 31 | 1,148 | 222 |
| [VVBBC interclubcompetitie 2026–2027](https://www.toernooi.nl/sport/league?id=E0230E0A-A3D9-4E95-BA2B-6E8E8D252B6C) | Provincial | 12 | 20 | 702 | 170 |
| [Jupiler Pro League](https://www.proleague.be/jpl-kalender) | First class | 1 | 1 | 342 | — |
| [Challenger Pro League](https://www.proleague.be/cpl-kalender) | Second class | 1 | 1 | 210 | — |

The current model represents a competition division with `CompetitionLeague`. Its name includes the source competition and division, its federation is the published organizer, and its poules preserve the published A/B/... groups or national qualification/play-off stages. Four national play-off/play-down stages have no published teams or fixtures in the capture and remain empty. The planner still generates round-robin fixtures; this change does not add a knockout generator. Jupiler's API returns 342 distinct fixture IDs, including 18 matches in each of rounds 31–34; these source records are retained without inventing a replacement calendar.

Public club and federation names receive the `[DEMO]` prefix. Public team names, classification suffixes, pairings, group membership, dates/times, venue labels, source status and results are retained. Club copies with the same name and sport share one new local club. Source organizer names also share new local federation copies. Existing clubs/federations are never reused. EndsAt is unknown and remains empty; 287 kickoff times are unknown and remain empty. Published date-only information stays in the fixture's snapshot note. Belgian local timestamps are converted with Europe/Brussels, including daylight-saving changes; football UTC timestamps are preserved.

## Fixture settings and details

Open a division's **Match registration and format** to inspect its inherited format. From a fixture, use **Register match** to see all games together and open an individual contest for its full lineup and sets. The fixture table's game details include singles/doubles, discipline, visible names, set scores and outcome. Ordinary pre-start lineups continue to use the existing privacy guard. Only the private, server-created `SourceSnapshot` registration state makes imported public names visible without pretending the fixture has started or been approved in this app.

| Format | Ordered games |
| --- | --- |
| Men's badminton | HD1, HD2, HD3, HD4, HE1, HE2, HE3, HE4 |
| Women's badminton | DD1, DD2, DD3, DD4, DE1, DE2, DE3, DE4 |
| Regional mixed badminton | HD, DD, HE1, DE1, HE2, DE2, GD1, GD2 |
| National mixed badminton | HD, DD, HE1, HE2, DE1, DE2, GD1, GD2 |
| Football | One team-total result |

Badminton uses best of three sets, 21 points, two-point margin and a cap of 30, without early fixture stopping. Scheduled fixtures create their eight empty contest rows from that format on first opening; published source sheets already contain eight contest rows. All source sheets preserve public player names as display text, without creating accounts, memberships, eligibility claims or roster associations. Source checks are disabled on these display-only formats. Match duration and captain review windows are demo defaults, not imported regulations.

There are 621 source sheets, 4,968 game rows and 10,439 numeric set scores. Fifty-four sheets are explicitly unplayed/awarded: their team-level result is kept independently of blank, unplayed game rows. Twelve source retirements include awarded complete set scores, which the normal editor rejects as a retirement after a completed game. Raw scores and source outcomes are retained. Editing these exceptions still requires a valid application outcome. League standings and corrections now use the league policy, including points per game for the national example. Player rankings are not calculated.

The 121 football fixtures marked Full Time carry their published final score. In-progress football scores are left as source notes rather than being finalized. No captain acceptance, federation finalization, actual start/finish time or result notification is fabricated.

## Ownership and maintenance

`resources/competition-public-2026-2027.json` is the frozen input; `competition-snapshot-counts.json` records its hash and source counts. `PublicCompetitionSamples.java` creates records through the existing `DevelopmentSamples.Seed`, adding every object to the private ownership manifest. The existing cleanup allowlist/dependency map already covers every reused business type and owning association; no new business entity was added. `DevSampleBatch.PublicCompetitionOrganizationIds` is private unlimited text, without client access, and prevents the older test-administrator extension from creating enabled accounts for public organization copies on subsequent generation. No invoice, email or payment action is added.

`fetch-competition-snapshot.ps1` caches public pages under `deployment/tmp` using necessary cookies only. `build-competition-snapshot.py` normalizes the cached pages with BeautifulSoup and the published Pro League calendar API, verifies source fixture IDs and membership, then emits the resource and count/hash report. Python requires BeautifulSoup and timezone data. Rebuilding from the same cache is deterministic. Before deliberately refreshing the capture, update its `CAPTURED` value and bump the feature version; existing user-edited imported batches must remain untouched.

## Verification

- **Model consistency:** mnd global diagnostics returned no errors after adding the private manifest field. Existing Match pages were verified under `Competition/Results`; no page or workflow was created at the module root.
- **Compilation:** Java 21 compilation of the changed services and sample generator passed. The final full Studio Pro application build passed (`BUILD SUCCESSFUL`), and the deployed resource hash matches the source snapshot. The runtime started; that alone does not verify sample generation. Details are recorded in `competition-snapshot-verification.json`.
- **Offline source checks:** `PublicCompetitionSnapshotChecks` passed fixture membership/identity checks, all 621 eight-game sheets, 4,968 aggregate game results, 10,439 numeric set scores, the 54 unplayed/awarded sheets and the 12 completed-score retirement exceptions. This verifies source compatibility, not Mendix workflows.
- **Runtime scenarios:** administrator generation, upgrading an older batch, repeating generation, private ownership coverage, edited-record preservation, scoped cleanup and signed-in fixture/format screens are implemented or prepared but **not executed** for this extension. Computer Use ended because it could not determine Chrome's current URL with enough confidence to enforce policy. No alternative authentication or administrator guard bypass was used.

Generation now asserts public fixture/result-sheet coverage and absence of administrator associations on newly imported public clubs. The existing rollback-only **Verify development examples** workflow exercises fresh generation, upgrade/repetition, manifest coverage, preservation and cleanup. Those assertions are not reported as passing until that administrator workflow actually runs.
