# Federation genders, team naming and My club competitions

Federations decide three things that clubs and members then follow:

- the gender options members can choose;
- how teams are named;
- whether a player may hold only one registration of a federation type.

## Gender options

Federation workspace › **Genders and teams** › **Add gender option** (`ACT_FederationGender_Create`, `FederationGender_Edit`). Each option has a name, a code, a position and an active switch. Options belong to one federation (`FederationGender_Federation`), so a federation can add values such as X, or retire one, without affecting other federations.

A registration question with answer type **Gender (federation options)** shows a combo box with the active options of the membership type's federation (`DS_FormResponse_GenderOptions`). `OCH_FormResponse_Gender` copies the chosen name into `Answer`, so review and validation keep working on text.

## Team categories and team names

On the same tab, **Add team category** (`ACT_TeamCategory_Create`, `TeamCategory_Edit`) defines:

- a code, such as H, D or G;
- a name, such as Men, Women or Mixed;
- the genders that may play in the category (`TeamCategory_Genders`). A mixed category lists several.

Each team gets a category and a number, and its name is the number followed by the code. For badminton that gives 1H and 2H for men's teams, 1D and 2D for women's teams, and 1G and 2G for mixed teams.

**Create a club team** (`CompetitionTeam_Edit`) offers the active categories of the club's current federations (approved, not ended), through `DS_CompetitionForm_CategoryOptions`. `ACT_CompetitionTeam_Save` then:

1. Rechecks that the category belongs to one of those federations.
2. Gives the team the club's next number in that category for the season. It takes the highest existing number, including inactive teams, so numbers are not reused.
3. Sets the name to the number followed by the code, for example `2H`.

Without a category the typed team name is used, as before. A league entry made by `ACT_LeagueEntry_Submit` for a numbered team reads "Club 1H", so fixtures stay readable. Free-named teams keep their own name.

Not covered yet:

- Gender eligibility per category is stored but not yet enforced when players are added to a team.
- Renaming or renumbering existing teams through the team bulk edit is not adjusted.

## One registration per player

`FederationType.OnePerPlayer` (switch on `FederationType_Edit`) limits a player to one open or active registration of that federation type, such as one competition registration. `SUB_Registration_FederationTypeLimit` enforces it in `ACT_Registration_Begin` and `ACT_Registration_Submit`.

## My club competitions

Navigation › **My club competitions** (`MyClub_Competitions`) is for members. "My club" means every club where the user holds an active registration (`Membership.Status = Active`).

The page shows one section per active federation of those clubs, ordered by sport, so different sports stay apart. Each section has:

- **Team:** the active teams of the user's clubs in that federation's categories or leagues (`DS_CompetitionSportSection_TeamOptions`). Empty means all teams.
- **Fixtures:** published fixtures of the federation's leagues where one of the user's clubs plays home or away, or only the chosen team's fixtures (`DS_CompetitionSportSection_Fixtures`). The list is in date order and refreshes when the team changes (`OCH_CompetitionSportSection_Team`).

The page is read-only and has no buttons the user cannot use. All data sources apply entity access:

- Published fixtures are readable by every signed-in user.
- Members read their clubs' active teams through the member rule on `CompetitionTeam`.

Player lists are not shown here. They stay governed by the club's player list rules and the viewer's membership type (`Club_Players`).

## Development examples

`extendFederationConventionSamples` (feature `features:federation-genders-team-categories-v1`) runs once for fresh and existing sample batches:

- **Gender options:** every sample federation without gender options gets Female (F), Male (M) and X.
- **Team categories:** the sample badminton federation, if it has none, gets H Men (Male), D Women (Female) and G Mixed (Male and Female).
- **Team renaming:** generated badminton demo teams still named "[DEMO] *Club* 1/2/3" become 1H, 1D and 1G, with number 1 and the category.
  - League entries and fixtures that still show the old name are renamed to "*Club* 1H" and so on.
  - Teams, entries and fixtures edited by hand are skipped.
- **Gender questions:** the generated federation "Gender" questions switch to the Gender answer type. Untouched sample answers become X; empty draft answers stay empty.
- **One registration per player:** sample competition federation types get **One registration per player**. The development administrator already has competition registrations at two sample clubs, so starting another one demonstrates the block.

The new records are in the ownership manifest:

- `FederationGender` and `TeamCategory` are cleanup types.
- `FederationGender_Federation`, `TeamCategory_Federation` and `TeamCategory_Genders` are cleanup edges.
- `CompetitionTeam_Category` and `FormResponse_Gender` are deliberately not edges. A real team or answer that uses a sample category or gender only loses that link when samples are removed.

The existing-batch preservation check (`verifyMatchLifecycle`) now ignores only the specific fields this upgrade may rewrite on manifest-owned demo records:

- team name and number;
- entry team name;
- fixture home and away names;
- question type and help text;
- answer type, help text and answer;
- `OnePerPlayer`.

`verifyFederationConventions` checks the following inside the generator's own verification. It is not a runtime test.

- At least three gender options exist.
- The mixed category allows several genders.
- A demo team named 1H exists, and its entry shows " 1H".
- A competition type is limited to one registration per player.
- The gender question uses the Gender answer type.

## Verification — 23 October 2026

- **Model consistency:** global diagnostics report **No errors found** after all changes.
- **Compilation:** `DevelopmentSamples.java` compiles with JDK 21 against the Mendix 11.15 runtime jars (standalone `javac`, not a Studio Pro build). The full app build was not run.
- **Runtime scenarios:** not executed. That covers:
  - the gender and category editors and the Genders and teams tab;
  - category numbering on team creation and the "Club 1H" entry names;
  - the My club competitions sections, team filter refresh and fixture lists;
  - the one-registration block;
  - sample generation and upgrade.
