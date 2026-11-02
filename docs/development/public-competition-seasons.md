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

## Match sheet details: game leader, shuttle, start hour (29 October 2026)

The published badminton match sheets list **Wedstrijdleider** (game leader), **Gebruikte shuttle** (shuttle used), **Aanvangsuur** (start hour) and **Einduur** (end hour). `build-competition-snapshot.py` now reads them from the already cached sheets (no new public requests; counts unchanged, only the resource hash changed). 541 of the 597 sheets that show a game leader row carry all four values; the others publish them empty and stay empty.

The administrator **Generate sample data** action adds them once, under `features:public-match-sheet-details-v1`, to fresh and existing batches:

- Start hour goes to `MatchRegistration.StartTime`, the shuttle to `Equipment` (**Match ball / shuttle**) and the leader's name to `LeaderName`. The leader is shown by name only: no account is created or attached, as for all imported names.
- Only untouched imported sheets are filled (revision 1 and no start hour, shuttle or leader name yet), so edited sheets keep their values and a second run changes nothing.
- The end hour is not stored: the registration has no end-hour field. No new record types, so the ownership manifest and cleanup lists are unchanged.
- The registration page shows the values on the summary line ("Started 19:15 · Shuttle: Victor Maxima") and under the title ("Game leader: …").
- Executed on 29 October 2026 under the Admin session: the upgrade of the existing batch reported 541 sheets updated, and the Wit-Wit 1G against Olve 3G fixture of the Flemish mixed 2nd league showed the published game leader, start hour and shuttle on its registration page. A repeat run, a fresh generation and the cleanup were not executed for this change. Imported sheets show the stage "Imported public match sheet, read only".

## Starters and roster players on imported badminton sheets (29 October 2026)

The captains' forms are not published, so everyone named on a published badminton sheet is treated as a starter. `PublicCompetitionSamples.fillStarters` creates display-only roster players per team (`CompetitionPlayer` without membership or account, key `public:<team>:<name>`, level suffix such as "(7)" removed from the roster name), links them to the lineups and writes one `MatchSquadPlayer` Starter per distinct player and side. Imported badminton formats show the team sheet (`MaxStarters` 8, the upper bound of the eight-game structure). It runs for fresh generation and, once, for existing batches under `features:public-match-sheet-starters-v1`, only on untouched imported sheets without a team sheet. New records join the ownership manifest; `CompetitionPlayer` and `MatchSquadPlayer` were already in the cleanup lists. Imported sheets are read only on the registration page (no start, setup, captain, appointment or finalise actions).

Executed under the Admin session: the upgrade reported 567 sheets with 5,369 starter entries and 2,772 roster players, and the Wit-Wit 1G against Olve 3G sheet shows both team sheets with every player who played. Not executed: a fresh generation, a repeat run for this feature and Remove sample data. The per-discipline levels in the published names (for example "Nico Van Vliet (7)" in HD, (9) in HE2) stay in the lineup name text.

## Ranking game history, 31 October 2026

`build-ranking-history.py` captures `resources/ranking-history-public.json` for the [ranking design samples](../competition/rankings.md): the three highest classes of the PBA adult league (seasons 2024-2025 to 2026-2027, 12,511 games, 201 without a published winner skipped) and 16 adult Belgian club tennis tournaments (2023 to 2026, 1,039 games, 24 skipped). Tennis Vlaanderen's own competition is not on toernooi.nl, so tennis is club tournaments only. Necessary cookies only, one request per 1.5 seconds, about 2,000 pages, cached in `deployment/tmp/ranking-history` so a rerun sends almost no requests. The site's terms speak against excessive automated use, so keep captures small and rare. Youth events are excluded. It holds real names from public results, like the snapshot above.
