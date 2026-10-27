# Fixture entry: players and set scores

The **Register match** page (`MatchFixture`) has a **Games** table that replaces the old games grid. It has one row per game the federation configured, and one column per set the format allows.

## Federation configuration

Everything that can be filled in for a fixture comes from the federation's match format (*Configure match format*):

| Setting | Where | Effect on the fixture |
| --- | --- | --- |
| Games: name, order, singles/doubles/team | Fixture slots (for example HD1, DD1, GD1) | One row per game, in slot order. Players per side (0, 1 or 2) sets how many player pickers the row shows. |
| **Captains name the players per game** (`RecordPlayers`) | Fixture tab | Off hides the Home and Away player columns. Use it for team-only sports such as football, which uses the team sheet instead. |
| **Record a score per set** (`RecordSetScores`) | Fixture tab | Off: every game takes one final score, and the higher score wins (team-total scoring). |
| **Sets needed to win** (`SetsToWin`) | Scoring tab | The table shows 2 × sets to win − 1 set columns, for example 3 columns for best of three. With set scores on, the limit is 1–3 sets to win, which gives at most 5 columns. |
| **Minimum score to win a set** (`Target`) | Scoring tab | The winner of a set needs at least this score, plus the winning margin. |
| **Winning margin** (`Margin`) | Scoring tab | The lead required to win a set. |
| **Maximum score per set** (`Cap`) | Scoring tab | The highest score allowed in a set, which ends the set even without the margin. 0 means unlimited. |

New formats start with both switches on. Both switches belong to the format version, like the other format settings.

## Flow

1. **Before the start: captains.** The home captain picks the home players in each game's Home column, and the away captain picks the away players in the Away column. Each pick saves immediately: the server checks the team roster, the lineup lock and that two different players were chosen. The opponent's picks show *Hidden until start*.
2. **At the start: game leader / referee.** An independent game leader sees both sides and can correct either one in the same table. They then acknowledge the warnings and press **Start**. Starting locks the lineups and reveals them to everyone.
3. **After the start: scores.** The game leader types the home and away score per set and presses **Save** on that game's row. Rules for saving:
   - Sets must be filled in order.
   - A game that isn't finished stays *Pending*, and becomes *Played* once a side has won enough sets.
   - Scores are checked against the minimum, margin and maximum.
   - Each save is audited.
4. **Details** on a row opens the existing game page for:
   - retirements, forfeits and exclusions with a comment;
   - tennis tie-break points;
   - substitutions after the start;
   - federation corrections after submission.

Submission, acceptance and disputes are unchanged.

## Implementation

- **Java service** `MatchRegistrationService`:
  - `gameTable` builds the rows.
  - The new commands are `PickHome`, `PickAway` and `SaveGame`.
  - `contestRules` applies the *Record a score per set* switch everywhere: the table, the game page and recalculation.
- **Row data:** each table row is a non-persistent `MatchRow` (Kind `Game`) with `Set1Home`…`Set5Away`, `PlayersPerSide`, `CanEditHome`, `CanEditAway` and `CanScore`. The player pickers are Combo boxes on the self-associations `MatchRow_HomePick1`/`2` and `MatchRow_AwayPick1`/`2`. Their options are roster rows of Kind `HomePlayer`/`AwayPlayer`.
- **Microflows** (Competition/Results): `ACT_Match_GamePickHome`, `ACT_Match_GamePickAway`, `ACT_Match_GameSave` and `ACT_Match_GameOpen`.
  - Each one receives the row, then copies its key into the form.
  - The server re-checks the user, the fixture state and the roster on every call.
- **Fix in passing:** the format form's team-sheet starter and reserve limits were read-only in entity access. They are now editable, so the format editor can save them.

## Development data

`DevelopmentSamples` adds `features:fixture-entry-config-v1` once, to both fresh and existing batches:
- Owned team-total formats (football) get *Record a score per set* off.
- Owned formats without player slots get *Captains name the players per game* off.
- Badminton and tennis formats keep both switches on.

The existing badminton and tennis fixtures in the generated sample data show both stages: player picking on unstarted fixtures, and set-score entry on started ones. No records are created, so the ownership manifest and cleanup list are unchanged. Both new fields are registered as convention fields.

## Verification

- **Model consistency:** Studio Pro's global check reports **No errors found** after the domain model, microflow and page changes.
- **Compilation:** the changed Java sources compile with standalone JDK 21 `javac` against the Mendix 11.15 runtime jars. This is not a Studio Pro build.
- **Runtime scenarios:** not run yet. Still to check in the app: picking players as each captain, the hidden opponent side, the leader correcting both sides, set-score saving and validation, the Details page, and the football format (one score per game). Seeded examples are not passing tests.
