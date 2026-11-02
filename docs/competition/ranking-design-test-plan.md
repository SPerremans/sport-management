# Ranking design: what to test

Nothing below has run in the app. The pages were built while nobody could sign in, and the app was not restarted. What was checked without the app is in [rankings.md](rankings.md#verification).

## Before you start

1. Build and run the app. There are two new persistent entities and new columns, so Studio Pro synchronises the database.
2. Sign in as Admin. Go to **Checks → Sample data → Generate sample data**. Expected message: sample set ready, and the batch summary mentions *Ranking design*.

## Scenarios

| # | Do this | Expect |
| --- | --- | --- |
| 1 | **Rankings**: select a federation without a scheme and press **Create** | Name, level names (for example `AA, A, B, C`) and a list of starting points. No numeric fields as in your screenshot |
| 2 | Choose a starting point and save | The design page opens. **Levels** shows AA, A, B, C. With an empty names field you get the names of the starting point |
| 3 | On **Levels**, rename a level, change a share, press **Add lowest level**, select one and press **Remove level** | Saved when you leave a cell. Using a name twice gives a message and restores the value |
| 4 | **Rankings**: open **[DEMO] Badminton Vlaanderen individual ranking**, tab **Levels and points** | New **Name** column; a rename takes effect at once, without test values or approval. Check the name on **Players**, **Changes**, a review and the player page |
| 5 | Same tab, **Design and simulate** | The design page: left the design, right the result of the sample. **Game data** shows the Antwerp league data as *In use* |
| 6 | Change the method to **Running rating (Elo)** | The rule list changes: K factor and scale appear, the divisor disappears. The result text says it is out of date |
| 7 | **Run simulation** | Takes seconds, with a progress bar. **Summary**, **Levels**, **Reviews**, **Level gaps** and **Players** fill. Players keep their levels: check the Players tab of the scheme afterwards |
| 8 | Tab **Summary** | Baseline is *Average points, as published*, with its value and the difference per measure. Measures against the sheet are present for badminton |
| 9 | Tab **Players**: search a name, select a player, **Show this player's games** | Tab **Player games** lists the games, and the message shows the path of levels and the last reason |
| 10 | **Save as scenario**, then on **Scenarios** use **Compare with this one** and **Open in the design** | The baseline moves; players who differ from it sort to the top |
| 11 | **Game data → Import games**, paste the lines below | 3 games imported, 1 line reported as unreadable, and the new data is *In use*. Run it: 4 players |
| 12 | **Game data → Use registered results** on the badminton scheme | A new data set from the federation's match results |
| 13 | Federation **[DEMO] Belgian club tennis**: open its ranking | Levels named 115 to 3, method *best results*, placement *pyramid*. Players come from the simulation |
| 14 | Tennis: **Run simulation**, then **Apply to the ranking** with a reason | A dialog first, then a message. Do this on tennis, not on badminton, unless you want to replace its sample values |
| 15 | Tennis, on the scheme page: simulate the upcoming review, then **Run review now** | Promoted and demoted with level names; it uses the pyramid. The scoring page still shows average points: see known gaps |
| 16 | Press **Remove sample data** | Tennis federation, data sets and scenarios are gone. Run **Generate** twice: the second run adds nothing |
| 17 | Sign in as a federation administrator and as a plain user | Only own federation's rankings; the plain user cannot open the design |

Lines for scenario 11:

```text
date;discipline;winner;partner;loser;partner;score;event
2026-03-01;HE;Ann Peeters (5);;Bo Maes (6);;21-10 21-12;Test
2026-03-08;HE;Bo Maes;;Ann Peeters;;21-15 15-21 21-18;Test
2026-03-15;HD;Ann Peeters;Cas Goossens;Bo Maes;Dirk Smet;21-19 21-19;Test
not a date;HE;A;;B;;;
```

## Where I expect problems

These could not be judged without running it, so look here first:

- **Layout**: the design page is two columns with many columns per grid. Half width may be too narrow for the **Levels** tab on a laptop.
- **Refresh after a cell edit**: results stay visible but are marked out of date. Check that focus does not jump and that the toolbar still works.
- **Selection**: grids use the app's multi-select checkboxes, and a command needs exactly one row. Selecting two rows should hide or refuse the action.
- **Speed**: the Players tab shows at most 400 rows, because every row is a client object. A weekly starting point on three years of data runs the most reviews.
- **Pasting**: a very large paste goes through a text area, so try a few thousand lines. There is no file upload because I had no supported way to add one.

## Known gaps

- The player scoring page of a scheme (`RankingHistory_View`) still explains the average-points calculation. For other methods the review itself is right but that page does not explain it. **Player games** in the design does cover it.
- Live reviews with a running rating replay each player's own view of the games, so the result can differ slightly from the simulation, which processes both sides of a game together.
- The review schedule's **first date and hour** stay on the Reviews tab; only the frequency is in the design.
- *Best results* scores wins by opponent level, not by tournament round.
- Tennis has few games per player, so expect warnings about thin data. That is the data, not a defect.

## Look at this result

On real badminton games the current sample thresholds demote about four times as often as they promote and match the printed level only 17 % of the time. A running rating matches 44 % and stays within one level 86 % of the time. The table is in [rankings.md](rankings.md#what-the-replay-showed). I did not change the thresholds: they are the sample values you know, and the design page is where you can try others.

## Real names in the sample data

The sample data keeps real names from public sheets and tournaments, like the 2026-2027 snapshot already did. This was decided on 31 October 2026 as an exception to the fictional details in `AGENTS.md`, and covers these public results only. Youth events are left out.
