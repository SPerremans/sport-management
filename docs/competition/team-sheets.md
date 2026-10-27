# Team sheets and substitutions

A fixture can register a team sheet per side: a list of player names, each marked **Starter** or **Reserve**. Football uses it for the starting eleven and the bench. Badminton uses it only for the reserve list; the individual submatches (8 in the current formats) keep naming the players per game.

## Configuration

The federation's match format sets two limits on the **Fixture** tab of *Configure match format*:

- **Team sheet: starters per team** (`MaxStarters`), 0–30. Use 0 when the sport names its players per game, as badminton does.
- **Team sheet: reserves per team** (`MaxReserves`), 0–30. This is the football bench or the badminton reserve list.

A format with both limits at 0 has no team sheet; the fixture page then hides the section. Both values belong to the format version, like the other format settings.

## Fixture page

*Fixture registration* shows **Team sheets** with Home and Away columns. Each column lists its starters and its reserves separately, with a status per player:

- starters show *Started*, or *Off 63' for …* once replaced;
- reserves show *Not used*, or *On 63' for …* once they came on.

Below the columns, **Substitutions** lists every substitution with minute, side, the player coming on and the player going off, who recorded it, when, and the reason.

A side's sheet follows the lineup privacy rule: before kick-off only that team's captain, an independent official or the federation can see it. After kick-off both sheets are visible to anyone who can read the fixture.

## Fixture detail (read-only)

The **Fixture** page opened from any fixture list (league, poule, competition overview) starts with the result across the full width, then the fixture facts in two columns (home | away, start | venue, status | issue; one column on phones, style `nexio-detail-columns`). Below that, above its buttons, it shows:

- **Games and players**: a compact table with one header row (Game, Home, Away, Score) above the list of games, one row per individual game in order (e.g. `HD1 | A / B | C / D | 21-15, 21-18`). The winning side's players are bold; a draw or a game without a result has no bold side. Retirements, forfeits and exclusions are noted after the score. Only games with named participants are listed, so football's single team-score game is not.
- **Team sheets** and **Substitutions**, as on the registration page, when the format has a team sheet.

It is read-only and uses `DS_CompetitionFixture_Games` → service command `FixtureGames`, which never creates a registration and applies the same lineup privacy (opponent players show *Hidden until start* before kick-off). A fixture without a registration shows "No games or players registered yet."

## Game result page

*Contest result* (open a game from **Register match**) uses the compact form style (`nexio-compact-form`). The official outcome is a Combo box (the React client does not support the classic drop-down), the free-text field is labelled **Comment** (still required, and logged, for retirements, forfeits, exclusions and corrections after submission), and the score help text is a single line.

## Team sheet page

**Open home team sheet** / **Open away team sheet** opens *Team sheet* for that side.

- **Registered roster** (before kick-off, captain or official, within the lineup lock): select a player and choose **Add as starter**, **Add as reserve** or **Remove from sheet**. The server refuses more starters or reserves than the format allows and players from another team. Changing the sheet resets the official's warning acknowledgement.
- **Team sheet**: the current sheet with position and substitution status.
- **Record substitution** (official, between kick-off and submission): select a player on the field and choose **Going off**, select an unused reserve and choose **Coming on**, enter the minute (0–200) and choose **Save substitution**. A player who went off cannot come back on and a reserve can come on once.

After kick-off the sheet itself is locked. The federation (override permission) can still correct it, and record or correct substitutions after submission, with a mandatory reason. Every change writes a `MatchAudit` entry; substitutions are also stored as `MatchSubstitution` records with the replaced and replacing player.

When the format has starters, kick-off and submission warn if a side named fewer starters than the limit. This is a warning the official acknowledges, consistent with the existing lineup warnings; it does not block.

## Storage

- `MatchSquadPlayer`: side, position (`MatchSquadPosition`: Starter/Reserve), player name, order; linked to the registration and the roster player.
- `MatchSubstitution`: side, minute, off/on names, reason, recorded by/at; linked to the registration and both players.

Both are private persistent types with no client access, reached only through `MatchRegistrationService` (`OpenHomeSquad`, `OpenAwaySquad`, `SquadStarter`, `SquadReserve`, `SquadRemove`, `SubstitutionOff`, `SubstitutionOn`, `SaveSubstitution`). The two tables on the team sheet page are registered as `MatchSquad__matchSquadRoster` and `MatchSquad__matchSquadSheet`.

## Development data

`DevelopmentSamples` adds `features:football-team-sheets-v1` once to fresh and existing batches (`BelgianFootballSamples.java`):

- **Format limits:** owned football formats with unset limits get 11 starters and 9 reserves; owned badminton formats get 2 reserves. Formats with configured limits are left alone.
- **Federation:** `[DEMO] Royal Belgian Football Association`, with its own format (11 + 9).
- **Divisions** (men's tiers 1 and 2, Jupiler Pro League and Challenger Pro League, were already in the public snapshot):
  - Men tier 3: **Nationale 1 2026-2027**, poules VV (16) and FFA (12).
  - Women tier 1: **Super League Vrouwenvoetbal 2026-2027** (8).
  - Women tier 2: **Eerste Nationale Vrouwen 2026-2027** (16).
  - Women tier 3: **Interprovinciale Vrouwen 2026-2027**, poules A (12), B (12) and FFA (13).
- Team lists come from public listings consulted on 27 October 2026 (Wikipedia's 2026–27 Belgian Division 1 page, Wikipedia's Belgian Women's Super League page, VoetbalInBelgië's women's First division listing and the Dutch Wikipedia Interprovinciale navigation). The Interprovinciale FFA listing names 13 clubs for a 12-club series; all 13 are kept. Reserve and women's sides of clubs already in the Pro League snapshot reuse that `[DEMO]` club; other clubs are new `[DEMO]` copies without accounts and are added to the public-organization list so no test administrator is attached.
- **No schedules or results are imported.** Each new division has one fictional fixture (first two teams of its first poule), kicked off yesterday without a score, so it never counts in standings. Both sides have 11 starters and 7 reserves with fictional names, and three substitutions (home 58' and 72', away 64'). Use **Generate fixtures** for the rest of the schedule.
- The owned badminton match-registration example gets one `[DEMO] Reserve player` on the home reserve list.

All generated records join the ownership manifest. `MatchSquadPlayer` and `MatchSubstitution` are in the cleanup type list with their owning associations, and `MaxStarters`/`MaxReserves` are registered as convention fields for the preservation check. No accounts, invoices, emails or payments are created.

## Verification

- **Model consistency:** global Studio Pro diagnostics report **No errors found** after the domain model, microflow and page changes.
- **Compilation:** the changed Java sources (`MatchRegistrationService`, `BelgianFootballSamples`, `DevelopmentSamples` and the three table registries) compile with JDK 21 against the Mendix 11.15 runtime jars (standalone `javac`, not a Studio Pro build).
- **Runtime scenarios:** not executed. Sample generation and upgrade, the team sheet page (roster selection, limits, privacy before kick-off, refresh after each action), substitution recording and federation corrections still need a run in the app. Seeded examples are not passing tests.
