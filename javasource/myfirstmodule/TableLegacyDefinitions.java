package myfirstmodule;
import java.util.*;
/** Existing audited and competition-locked table mutations retain their original workflows. */
public final class TableLegacyDefinitions {
 public record Action(String flow,Map<String,String> arguments){}
 private static final Map<String,Action> DATA=new HashMap<>();
 public static Action get(String table,String operation){return DATA.get(table+"|"+operation);}
 public static void prepare(){}
 static {
 TableFeatureDefinitions.includeContexts("Accounts_Overview__applicationAccounts",Map.of("accountSearch","MyFirstModule.AccountSearch"));
 DATA.put("Platform_Clubs__clubList|Edit",new Action("MyFirstModule.ACT_Clubs_Edit",Map.ofEntries(Map.entry("Selection","$selection"))));
 DATA.put("Federation_Overview__federationList|Edit",new Action("MyFirstModule.ACT_Federations_Edit",Map.ofEntries(Map.entry("Selection","$selection"))));
 DATA.put("Federation_Overview__federationList|Deactivate",new Action("MyFirstModule.ACT_Federations_Deactivate",Map.ofEntries(Map.entry("Selection","$selection"),Map.entry("TableSelection","$state"))));
 DATA.put("Federation_Overview__federationList|Activate",new Action("MyFirstModule.ACT_Federations_Enable",Map.ofEntries(Map.entry("Selection","$selection"),Map.entry("TableSelection","$state"))));
 DATA.put("Accounts_Overview__applicationAccounts|Edit",new Action("MyFirstModule.ACT_Accounts_Edit",Map.ofEntries(Map.entry("Search","$accountSearch"),Map.entry("Selection","$selection"))));
 DATA.put("Accounts_Overview__applicationAccounts|Activate",new Action("MyFirstModule.ACT_Accounts_Enable",Map.ofEntries(Map.entry("Search","$accountSearch"),Map.entry("Selection","$selection"))));
 DATA.put("Accounts_Overview__applicationAccounts|Deactivate",new Action("MyFirstModule.ACT_Accounts_Deactivate",Map.ofEntries(Map.entry("Search","$accountSearch"),Map.entry("Selection","$selection"))));
 DATA.put("Competition_Overview__competitionWidget204|Edit",new Action("MyFirstModule.ACT_CompetitionLeagues_Edit",Map.ofEntries(Map.entry("Selection","$selection"))));
 DATA.put("Competition_Overview__competitionWidget214|Edit",new Action("MyFirstModule.ACT_CompetitionTeams_Edit",Map.ofEntries(Map.entry("Selection","$selection"))));
 DATA.put("Competition_Overview__competitionWidget214|Deactivate",new Action("MyFirstModule.ACT_CompetitionTeams_Deactivate",Map.ofEntries(Map.entry("Selection","$selection"),Map.entry("TableSelection","$state"))));
 DATA.put("Competition_Overview__competitionWidget214|Activate",new Action("MyFirstModule.ACT_CompetitionTeams_Enable",Map.ofEntries(Map.entry("Selection","$selection"),Map.entry("TableSelection","$state"))));
 DATA.put("Competition_Overview__competitionWidget217|Edit",new Action("MyFirstModule.ACT_CompetitionVenues_Edit",Map.ofEntries(Map.entry("Selection","$selection"))));
 DATA.put("Competition_Overview__competitionWidget217|Deactivate",new Action("MyFirstModule.ACT_CompetitionVenues_Deactivate",Map.ofEntries(Map.entry("Selection","$selection"),Map.entry("TableSelection","$state"))));
 DATA.put("Competition_Overview__competitionWidget217|Activate",new Action("MyFirstModule.ACT_CompetitionVenues_Enable",Map.ofEntries(Map.entry("Selection","$selection"),Map.entry("TableSelection","$state"))));
 }
}
