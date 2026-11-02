# How individual rankings work in other sports

Research for the ranking design workspace, 31 October 2026. Team sports are left out. Where a source is a third party or could not be confirmed, this says so. The presets in the app are **inspired by** these systems and do not reproduce them: their numbers are editable examples.

## What differs between systems

| System | Score from | What counts | Levels or list | Reviewed | Inactivity | Source quality |
| --- | --- | --- | --- | --- | --- | --- |
| Badminton Vlaanderen | Points for a win by opponent level, averaged | 20 most recent in 52 weeks, divide by at least 7 | 12 numbered levels, thresholds | First Monday, monthly | Levels lost | Published in part, see [rankings.md](rankings.md) |
| [BWF world ranking](https://bwfbadminton.com/how-ranking-works/) | Points per tournament result, added up | Best 10 results in 52 weeks | Ordered list, no levels | Weekly | Results age out | Official |
| [Belgian tennis](https://www.tennisenpadelvlaanderen.be/nl/tennis/faq-klassementen) | Points per round plus win bonus | Best 6 results in 12 months | **Pyramid**: fixed percentages per level; names are point values (3, 5, 15, 95, 115) | Twice a year (weeks 20 and 40) | Keep the level with 0 or 1 match; down one after 18 months | Official pages; the point scales and percentages are in PDFs that could not be read here |
| [French tennis](https://prod-portail.fft.fr/sites/default/files/2022-08/01_BilanTheorie_0.pdf) | Balance V − E − 2I − 5G: wins minus weighted losses against lower levels | Number of wins counted depends on the level | Named ladder (40 ... 15 ... 1re série) | Monthly | Rules per level | Official; details changed in August 2025 and were not read |
| [FIDE chess](https://www.fide.com/docs/regulations/FIDE%20Rating%20Regulations%202022.pdf) | Running rating (Elo), K 40, 20 or 10 | Every game; gap capped at 400 | Number, titles on top | Monthly list | None | Official, 2022 edition; floor rules differ between later sources |
| [German table tennis (TTR)](https://github.com/robing29/ttr-rechner) | Running rating, narrow scale of 150, K about 16 | Every game | Number; a frozen quarterly value for line-ups | Live and quarterly | None found | Third party calculators |
| [USATT table tennis](https://assets.contentstack.io/v3/assets/blteb7d012fc7ebef7f/blt1e29f1ba852bd866/6921425cd6ca225f5e375a3a/USATT_Ratings_Calculation_Explained.pdf) | Point exchange by rating gap: an upset moves more points | Every game | Number | Per event | None found | Official |
| [ITF World Tennis Number](https://worldtennisnumber.com/eng/news/the-science-behind-world-tennis-number) | Pre-match expectation against the result, **per set**, recent results weigh more | Rolling three years | Scale 40 to 1, separate singles and doubles | Weekly | Ages out | Official, formula not published |
| [German tennis (DTB LK)](https://www.tennis.de/news/mitfiebern/2025/neue-lk-berechnung-ab-der-wintersaison-2025-26.html) | Points for a win divided by a hurdle of the own level; a loss has no effect | Each win | Levels 25 to 1 with decimals | Weekly | Yearly drift | Official; rules changed for winter 2025/26 |

## What this meant for the design

The old configuration fixed one system: average points, 12 numbered levels, thresholds. Other systems differ on five independent questions, so these are now separate choices:

1. **What is a level called?** A free name per level. The position (1 is the highest) stays internal. Belgian tennis calls levels 115 and 3, French tennis 15/1 and 40.
2. **How is the score calculated?** Average points, sum of the best results, win balance, or a running rating. These four cover the table above except per-set scoring.
3. **How does a score become a level?** Promotion and demotion scores, or fixed shares per level (a pyramid).
4. **How fast can a player move?** Most levels up and down per review, the largest difference between disciplines (which can now be off), the review frequency.
5. **What happens to inactive players?** Games needed to count as active, the waiting time, the levels lost (which can now be none).

Not built, because the imported data or the model does not carry it yet. These are the logical next steps:

- **Points per tournament round** (BWF, Belgian tennis). The sample tennis results have the draw but not a reliable round per match, so *best results* uses win points by opponent level. This is the largest simplification for tournament sports.
- **Per-set scoring and recency weighting** (World Tennis Number).
- **Weights per event type** and age pyramids (Belgian tennis has four).

## How others review a simulation

No federation publishes a complete simulation tool. What exists is player calculators and rating research, and each gives a useful idea:

| Seen at | Idea | In the workspace |
| --- | --- | --- |
| [LK-Rechner](https://lk-rechner.de/), [TTR calculators](https://ttr-rechner.eu/), [rating scenarios](https://mytennisratings.com/scenarios) | Explain one player's outcome | **Player games**: every game of a player with the opponents, levels and points, plus the path of levels |
| [Tuning Elo on historical results](https://arxiv.org/abs/2512.18013), [log loss and Brier tuning](https://metricgate.com/blogs/es/elo-rating-system-sports-r/) | Judge a configuration by how well it predicted results, using only earlier data | Replay in date order. **Better-levelled side won** and **Prediction error (Brier)**, 0.25 being a coin flip |
| [Calibration per bucket](https://aragora-docs.pages.dev/docs/core-concepts/elo-calibration) | Does a bigger gap really mean a bigger chance? | **Level gaps**: games and win rate per gap |
| [Tennis Vlaanderen FAQ](https://www.tennisenpadelvlaanderen.be/nl/tennis/faq-klassementen) warns that third-party predictions differ because cuts change every calculation | A simulation must state that it is an estimate on the given data | Results say which data and design they come from; a changed design says the result is out of date |

Added on top because the sheets print levels:

- **Agreement with the sheets**: how often the simulated level equals the level printed on the sheet at that moment. It shows how close a design comes to the current official levels. This is the most direct way to calibrate an unpublished threshold.
- **Stability**: level changes per player per year, and how many moves undo a recent one (yo-yo).
- **Warnings** in plain words: too few games per player, empty levels, one level holding half the players, levels that do not predict results, a restless ranking.
- **Scenarios and a baseline**: save a design with its result, compare the numbers, and sort the players who end up differently to the top.

Not built: an automatic sweep over many values (for example K from 8 to 40) and a player what-if ("what if I win this match"). Scenarios cover the sweep by hand.

## Other sources read

[Tennis Vlaanderen rankings](https://www.tennisenpadelvlaanderen.be/tennis/klassementen), [club summary of the calculation](https://www.tcdd.be/index.php/berekening-tennisklassementen), [BWF ranking on Wikipedia](https://en.wikipedia.org/wiki/BWF_World_Ranking), [Glicko and Elo overview](https://arxiv.org/pdf/2104.14012). PDGA and UTR were searched for but no usable source was read, so they are not in the table.
