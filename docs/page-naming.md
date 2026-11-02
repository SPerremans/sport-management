# Page naming

Applied on 1 November 2026. New pages follow this rule. The rule follows Mendix [Naming Convention Best Practices](https://docs.mendix.com/refguide/naming-convention-best-practices/): a page name ends in a suffix that says what the page is for.

| Page purpose | Suffix |
| --- | --- |
| List objects of a single entity type | `_Overview` |
| Create an object | `_New` |
| Update an object | `_Edit` |
| Create or update an object | `_NewEdit` |
| View an object (read-only) | `_View` |
| Select a single object | `_Select` |
| Select multiple objects | `_MultiSelect` |
| Tooltip | `_Tooltip` |
| Interact with a workflow user task | `_Workflow` |

## Project rules on top of that

- **Shape:** `{Subject}_{Suffix}`, with the subject in singular PascalCase.
- **Shorter subjects:** drop `Competition` and `Match` when the rest of the name is unambiguous in this module (`CompetitionLeague_Edit` becomes `League_NewEdit`). Business folders already give the context.
- **`_Workspace`:** the one project-specific suffix, for tabbed hub pages that have no documented equivalent.
- **Create and edit on one page:** `_NewEdit`, even where this adds three characters, because the page is opened by both a create and an edit flow.
- **Not renamed:** marketplace modules (read-only), `Login` and `Signup`, and microflow names.
- **Table IDs stay as they are.** Keys such as `Club_Workspace__memberList` and `MatchSquad__*` are registered in `TableDefinitions.java` and used by the table routers. They are stable keys, not page references, so they keep their current text after a rename.

## Renames

| Folder | Old name | New name |
| --- | --- | --- |
| Accounts | `AccountBulkEdit_Edit` | `AccountBulk_Edit` |
| Accounts | `Account_Detail` | `Account_View` |
| Accounts | `Account_ViewAsUser` | `AccountAsUser_View` |
| Accounts | `Accounts_Overview` | `Account_Overview` |
| Accounts | `FederationAdmin_Assign` | `FederationAdmin_New` |
| Accounts | `Platform_Accounts` | `Invitation_Overview` |
| Billing | `FederationBilling_Generate` | `FederationBilling_New` |
| Billing/Invoices | `Invoice_AdminEdit` | `Invoice_NewEdit` |
| Billing/Invoices | `Invoice_Detail` | `Invoice_View` |
| Clubs | `ClubBulkEdit_Edit` | `ClubBulk_Edit` |
| Clubs | `Club_Administration` | `MyClub_Overview` |
| Clubs | `Club_Detail` | `Club_View` |
| Clubs | `Platform_Clubs` | `ClubSetup_Overview` |
| Clubs/Administration | `ClubAdmin_Assign` | `ClubAdmin_New` |
| Clubs/Administration | `ClubRole_Edit` | `ClubRole_NewEdit` |
| Clubs/Administration | `ClubStaff_Assign` | `ClubStaff_New` |
| Clubs/Affiliations | `Club_Federations` | `Affiliation_Overview` |
| Competition | `ClubCompetitionPolicy_Edit` | `HostingLimit_Edit` |
| Competition | `CompetitionAllocation_Edit` | `Allocation_Edit` |
| Competition | `CompetitionDelegate_Edit` | `Delegate_Edit` |
| Competition | `CompetitionFederation_Workspace` | `FederationLeague_Overview` |
| Competition | `CompetitionLeague_Edit` | `League_NewEdit` |
| Competition | `CompetitionLeague_Workspace` | `League_Workspace` |
| Competition | `CompetitionPlayer_Add` | `Player_New` |
| Competition | `CompetitionTeam_Edit` | `Team_New` |
| Competition | `CompetitionTeam_Workspace` | `Team_Workspace` |
| Competition | `CompetitionVenue_Edit` | `Venue_New` |
| Competition | `CompetitionVenue_Workspace` | `Venue_Workspace` |
| Competition | `FixtureChange_Propose` | `FixtureChange_New` |
| Competition | `FixturePlacement_Edit` | `Placement_Edit` |
| Competition/FixtureChanges | `FixtureChange_Review` | `FixtureChange_View` |
| Competition/Leagues | `CompetitionLeagueBulkEdit_Edit` | `LeagueBulk_Edit` |
| Competition/MyClub | `MyClub_Competitions` | `MyClubLeague_Overview` |
| Competition/Rankings | `RankingGames_Import` | `RankingGame_New` |
| Competition/Rankings | `RankingPlayer_History` | `RankingHistory_View` |
| Competition/Rankings | `RankingValues_Edit` | `Ranking_Edit` |
| Competition/Rankings | `Ranking_Overview` | `RankingScheme_Edit` |
| Competition/Rankings | `Ranking_Review` | `RankingReview_View` |
| Competition/Rankings | `Ranking_Schemes` | `RankingScheme_Overview` |
| Competition/Rankings | `Ranking_Workbench` | `RankingDesign_Edit` |
| Competition/Registrations | `LeagueEntry_Move` | `LeagueEntryMove_Edit` |
| Competition/Registrations | `LeagueEntry_Poule` | `LeagueEntryPoule_Edit` |
| Competition/Registrations | `LeagueEntry_Register` | `LeagueEntry_New` |
| Competition/Results | `CommentDialog_Edit` | `Comment_Edit` |
| Competition/Results | `CompetitionPoule_Standings` | `Poule_View` |
| Competition/Results | `MatchAssignments` | `MyMatch_Overview` |
| Competition/Results | `MatchContest` | `Contest_Edit` |
| Competition/Results | `MatchDivision` | `Division_Overview` |
| Competition/Results | `MatchEligibility` | `Eligibility_Overview` |
| Competition/Results | `MatchEligibility_Edit` | `Eligibility_Edit` |
| Competition/Results | `MatchFixture` | `Fixture_Edit` |
| Competition/Results | `MatchFormat_Edit` | `MatchFormat_NewEdit` |
| Competition/Results | `MatchFormats` | `MatchFormat_Overview` |
| Competition/Results | `MatchLineup` | `Lineup_Edit` |
| Competition/Results | `MatchRoster` | `LineupPlayer_Select` |
| Competition/Results | `MatchSet_Edit` | `MatchSet_NewEdit` |
| Competition/Results | `MatchSlot_Edit` | `MatchSlot_NewEdit` |
| Competition/Results | `MatchSquad` | `Squad_Edit` |
| Competition/ScrapedRuns | `ScrapedRun_Import` | `ScrapedRun_Overview` |
| Competition/Teams | `CompetitionTeamBulkEdit_Edit` | `TeamBulk_Edit` |
| Competition/Tests | `CompetitionChecks_Overview` | `Check_Overview` |
| Competition/Venues | `CompetitionVenueBulkEdit_Edit` | `VenueBulk_Edit` |
| Content | `ClubEvent_Edit` | `ClubEvent_NewEdit` |
| Content | `NewsItem_Edit` | `NewsItem_NewEdit` |
| Discovery | `Club_Directory` | `Club_Overview` |
| Discovery | `Player_Directory` | `Player_Overview` |
| Federations | `FederationAffiliation_Edit` | `Affiliation_NewEdit` |
| Federations | `FederationBulkEdit_Edit` | `FederationBulk_Edit` |
| Federations | `FederationGender_Edit` | `FederationGender_NewEdit` |
| Federations | `FederationRate_Edit` | `FederationRate_NewEdit` |
| Federations | `FederationType_Edit` | `FederationType_NewEdit` |
| Federations | `Federation_Edit` | `Federation_NewEdit` |
| Federations | `TeamCategory_Edit` | `TeamCategory_NewEdit` |
| Members | `Member_Dashboard` | `Member_Overview` |
| Members | `MembershipType_Edit` | `MembershipType_NewEdit` |
| Members/PlayerLists | `Club_Players` | `ClubPlayer_Overview` |
| Members/PlayerLists | `PlayerListRule_Edit` | `PlayerListRule_NewEdit` |
| Registration | `FormField_Edit` | `FormField_NewEdit` |
| Registration | `Registration_Approval` | `RegistrationApproval_Edit` |
| Registration | `Registration_Edit` | `Registration_NewEdit` |
| Registration | `Registration_Review` | `Registration_View` |
| Registration | `Registrations` | `Registration_Overview` |
| Seasons | `SeasonFee_Edit` | `SeasonFee_NewEdit` |
| Seasons | `Season_Edit` | `Season_NewEdit` |

Pages that already fit keep their name: `Club_Edit`, `Club_Workspace`, `Competition_Overview`, `Federation_Overview`, `Federation_Workspace`, `InvoiceStyle_Edit`, `Membership_Edit`, `NewsItem_View`, `TableCreate_Select`, `TableRecord_View`, `TableRetention_Edit` and `VenueAvailability_Edit`.

## Unused pages

Found by scanning the saved model for references from navigation, pages, snippets, microflows and every other document, plus Java and theme code. All of these are excluded from the build. The connector cannot delete documents, so they still need deleting in Studio Pro, together with their excluded opener microflows `ACT_InstalmentDecision_Open`, `ACT_ClubBillingPolicy_Open` and `ACT_ClubAccount_Open`.

| Page | Finding | Risk |
| --- | --- | --- |
| `ScrapedRun_Import_DeleteMe` | Excluded, no references, left over from the import page | None |
| `Invoice_Detail_Styled` | Excluded, no references | None |
| `Renewals` | Nothing opens it. It has a registered table in `TableDefinitions.java` | Season renewals may have lost their entry point |
| `PasswordReset` | Nothing opens it and it has no URL, although `docs/development/navigation-and-actions.md` says an email link reaches it | The reset flow may be broken rather than unused |
| `InstalmentDecision_Edit` | Only opened by `ACT_InstalmentDecision_Open`, which nothing calls | Instalment review may have lost its entry point |
| `ClubBillingPolicy_Edit` | Only opened by `ACT_ClubBillingPolicy_Open`, which nothing calls | Season billing settings may have lost their entry point |
| `AdminAccount_Create` | Only opened by `ACT_ClubAccount_Open`, which nothing calls | Account creation may have lost its entry point |
