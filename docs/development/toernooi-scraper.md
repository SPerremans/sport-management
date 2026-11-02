# toernooi.nl scraper

`tools/scraper/toernooi_scraper.py` captures public league data into timestamped local files, to inspect and later import as sample data. It is a local script, which a platform administrator can also start from the import page in the local Studio Pro runtime. The application itself still has no HTTP calls to sports providers, and no other environment can start the script. It starts with badminton in Belgium.

## Before you run it

`robots.txt` of the site disallows `/sport/`, `/ranking/`, `/player/` and `/league/` for all crawlers, and the [terms](https://www.toernooi.nl/support/terms-and-conditions) forbid disrupting or overloading the service, including through excessive or automated use. Running it goes against `robots.txt`; this was a deliberate choice of the project owner for small, slow, rare runs. Keep it that way: filter narrowly, and do not lower the delay or schedule it. If the site objects or starts refusing, stop and ask them for permission or an export.

## Install and run

```bash
python -m pip install -r tools/scraper/requirements.txt
```

See the leagues of a season:

```bash
python -X utf8 tools/scraper/toernooi_scraper.py leagues --season 2026-2027
```

Capture one division and a short period, with one ranking category:

```bash
python -X utf8 tools/scraper/toernooi_scraper.py run --season 2026-2027 --league "PBA competitie" --division "Heren 1e" --since 2026-09-01 --until 2026-10-31 --ranking-categories "^HE" --ranking-top 100
```

| Option | Meaning | Default |
| --- | --- | --- |
| `--sport`, `--country` | Sport and three letter country code of the site | `badminton`, `BEL` |
| `--season` | Finds leagues that start in this season. Needed unless you only capture rankings | none |
| `--league` | Regular expression on the league name. `run` requires this, or `--all-leagues` on purpose | none |
| `--division` | Regular expression on the division name | all |
| `--since`, `--until` | Only matches in this period, `YYYY-MM-DD` | all |
| `--include` | Comma list of `clubs`, `players`, `rankings`, `matches` | all four |
| `--ranking-categories` | Regular expression on the ranking category, for example `^HE` | all |
| `--ranking-top` | Adults kept per ranking category, `0` for all | 200 |
| `--delay` | Seconds between requests plus up to half of that at random, at least 3 | 5 |
| `--max-requests` | The run stops and keeps its files at this number | 400 |
| `--list-max-age-hours`, `--sheet-max-age-days` | Reuse cached list pages and played match sheets up to this age | 24 hours, 30 days |
| `--offline` | Use only cached pages and send nothing | off |

At the default delay, 100 requests take about 10 minutes. One league costs one request per club, two per division and one per played match, so a whole provincial league can take hours. An entire ranking costs about 50 requests per large category.

## Limits built into the script

- One request at a time, with a floor of 3 seconds that options cannot lower.
- Any 403, 429, 5xx or connection failure ends the run at once, without a retry. The files so far are kept in a `-partial` folder.
- The request cap ends a run the same way. Running the same command again continues from the cache in a new folder.
- Necessary cookies only, no sign-in, HTML pages only.

## Concurrent runs

You can run several commands at once, for example one per league. It is safe, but **it does not fetch pages faster**: all runs that use the same `scrapes` folder take turns, so together they keep the pace of one run. That keeps the small, slow promise above. Several runs only gain when one reuses pages another has already cached, such as the rankings.

- If the site refuses one run (403, 429 or 5xx), all of them stop and nothing is sent for an hour. Deleting `scrapes/_pace.json` ends that wait; only do so if you know why it happened.
- `--max-requests` counts per command, so several commands together can send more than that number.
- Cached pages are written whole, so a run never reads half a page, and a page another run fetched while this one waited is not fetched twice.
- A copy of the script elsewhere has its own `scrapes` folder and therefore its own pace. Do not run copies side by side.

## Output

Each capture writes a new folder that is never overwritten, in a `scrapes` folder next to the script, with Europe/Brussels times:

```
scrapes/
  vvbbc-interclubcompetitie-2026-2027/
    badminton-bel-20261101-133353/        one league, one folder per run
  pba-competitie-2026-2027/
    badminton-bel-20261101-140512-partial/
  rankings/
    badminton-bel-20261101-133353/        the national ranking, shared by all leagues
  _cache/
```

A command that matches several leagues writes one folder per league, and rankings always get their own. If you copy the script elsewhere, its results go next to that copy. `tools/scraper/scrapes/` is ignored by Git, so real names stay on your machine; a copy inside another repository is not covered by this rule.

| File | Holds |
| --- | --- |
| `manifest.json` | Status and stop reason, filters, request numbers, counts, SHA-256 of each file, warnings |
| `leagues.json` | Name, organizer, place, dates and source link |
| `clubs.json`, `teams.json` | Club number, place and published counts; teams with their club |
| `players.json` | Source id per league, name, teams and latest level per discipline seen on the match sheets |
| `rankings.json` | Week, category, rank, previous rank, name, club, level, points, source id within that publication |
| `matches.json` | Fixture metadata, published score, game leader, shuttle, start and end hour; games with players, levels, sets and outcome |
| `sources.jsonl` | Every page used, with its fetch time, hash and whether it came from the cache |

Pages are kept in `scrapes/_cache` next to the script. Rerun with `--offline` to parse them again without requests. The cache keeps only the newest copy of a page; the output folders are the history. The cache holds the pages as published, so unlike the output files it does contain member numbers and the U19 ranking lists. Delete the folder whenever you no longer need it; the next run then fetches those pages again.

## What is stored and left out

- Real names as published, the same exception as the [public competition capture](public-competition-seasons.md).
- **Never in the output files:** member numbers, birth dates and contact details.
- **Youth:** leagues, divisions and ranking categories with a youth name are skipped. For rankings the U19 list is read only to leave those players out and is not written. If that filter does not visibly apply, the category is skipped with a warning. The manifest counts the entries left out. Players under 19 who play in an adult league still appear on its sheets and player list, because the sheets do not show ages.
- Fields the source does not publish stay empty. A sheet that cannot be read becomes a manifest warning.

League player and team ids belong to one league, and ranking source ids to one publication, so the same person has several ids. The only identifier shared across them is the member number, which is left out, so linking a league player to a ranking, or a ranking between weeks, is a name and club match for the import to handle.

## Scraping from the application

On the import page, **Scrape leagues** runs the `leagues` command and opens `Competition/ScrapedRuns/ScrapedLeague_MultiSelect` with the leagues of the current season. Seasons start on 1 July. Season, name, sport and country can be changed before searching again. `python` must be on the PATH of the Studio Pro runtime, with `tools/scraper/requirements.txt` installed.

**Start scraping** runs the selected leagues one at a time in a background job, optionally followed by the rankings, using `--league-id` and at most 2000 requests per league. Only one job runs at a time. A refusal, an early stop or **Stop scraping** ends the queue, and the page shows the status after **Refresh**. Script output is in `tools/scraper/scrapes/_job.log`. The job lives in the runtime: after a restart the page no longer shows it, and a script that is still running finishes on its own.

Nothing is imported automatically. Each league gets a new version folder.

## Going back to another version

Every league folder keeps all its versions, and the import page lists them all. Only one version of a league or of the rankings can be in the sample batch, so the import refuses another version with a message. To change version, remove the sample data, generate it again and import the version you want. This stays a manual step.

## Importing a run into the application

Open **Checks → Sample data → Import a scraper run** (`Competition/ScrapedRuns/ScrapedRun_Overview`) as a platform administrator in the local Studio Pro runtime. The Java service repeats the same role and local-runtime check as **Generate sample data**. The page only reads local files and sends no requests.

- **List:** every run folder under `tools/scraper/scrapes/<league or rankings>/` of this project, newest first, plus a bundled fictional example. `_cache` and other folder names are ignored. Partial runs are listed with the status **Partial, stopped early**.
- **Preview:** selecting one run shows its counts, warnings and stop reason, and compares every file with the SHA-256 in its manifest. A run that fails this has no import button.
- **Import:** needs an existing sample batch; if there is none, generate one first. It runs in one transaction and rechecks the hashes first. A partial run can be imported, but only what it captured.
- **Repeating:** the batch records `features:scraped-run-import-v1:<hash of manifest.json>`, so importing the same run again adds nothing. Runs of different leagues are separate sets of copies.
- **Removing:** every record joins the private ownership manifest, so **Remove sample data** removes it. No new persistent business types were added, so the cleanup lists are unchanged.

| In the run | In the application |
| --- | --- |
| League organizer | New `[DEMO]` federation |
| Club, team | New `[DEMO]` club (one per name within a run), new team |
| Division, draw | League with a match format, poule. Only teams with a captured match are in a poule |
| Match, sheet | Fixture; a sheet with games becomes a read-only source match sheet, as in the frozen capture |
| Player with a team | Display-only roster player, without membership or account. Players the run links to no team are left out and counted |
| Ranking entry | Level on a `PlayerRanking` of a separate `[DEMO] Published ranking` federation and scheme. Rank, points, club and week are in the change reason. Unknown categories and levels outside the scheme are left out and counted |

The source has no stable player id across leagues or ranking weeks, so none is used. Players are display-only and matched by name within a team; ranking entries by name and club within a discipline. Two different people with the same name in one team become one roster row. Existing records are never matched by name or rewritten, and imported public clubs and federations never receive test login accounts.

Match formats keep the eight game slots of the frozen capture. Sheets with another number of games, such as the six of PBA senior leagues, keep their published games, but a future fixture of that division would open with eight empty contests. Only badminton runs can be imported.

`resources/scrape-example-run` is hand-made and fictional. **Generate sample data** imports it once in fresh and existing batches. If you edit it, update its hashes in its `manifest.json`.

## Extending

- **Another sport:** add it to `SPORTS` with the site's sport id and game labels. Team match sheets of other sports are untested and may need their own parser; the tests and warnings will show this.
- **Another country:** pass `--country`, and add the country's ranking to `RANKINGS` after checking its club names. Rid 334 (BBF Rating) was checked for Belgium.
- Text such as `Opgave` and `Wedstrijdleider` is Dutch, because the `.nl` site serves Dutch pages.

## Tests

```bash
python -X utf8 -m unittest tools/scraper/test_toernooi_scraper.py
```

These use small fictional pages and send no requests.

## Verification, 1 November 2026

- **Parsers against the frozen capture:** run over the cached pages of the 4 badminton leagues in `resources/competition-public-2026-2027.json`. Clubs, teams, divisions and 2,790 fixtures match, as do players, sets, outcomes and sheet details on all 621 match sheets, with 0 differences.
- **Live run:** PBA seniorencompetitie 2026-2027 plus the top 25 adults of ranking HE/SM, at the default delay. Complete with 118 requests, no warnings and no refusals: 21 clubs, 48 teams, 274 players, 244 matches, 408 games. All file hashes match the manifest. All matches link to their teams, and 262 of 274 players are linked to a team through the match sheets.
- **Youth filter:** 7 U19 entries were left out of the ranking pages read. On category HE/SM all 83 U15 players are inside the U19 list of 513. Other categories and age groups were not compared.
- **Not checked:** a run stopped by a real 403 or 429 (covered only by the mocked tests), a whole league with many divisions, other ranking categories, other sports and other countries. This league's sheets have 6 games, which the parser accepts without any sport-specific count.
