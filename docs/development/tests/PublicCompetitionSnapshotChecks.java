import com.mendix.thirdparty.org.json.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import myfirstmodule.MatchRules;

/** Offline source-to-model rule compatibility; does not exercise Mendix persistence, roles or sample lifecycle. */
public final class PublicCompetitionSnapshotChecks {
    static int fixtures, sheets, games, sets, awards, football, awardedRetirements;
    public static void main(String[] args) throws Exception {
        JSONObject data=new JSONObject(Files.readString(Path.of(args[0])));
        JSONArray sources=data.getJSONArray("sources");check(sources.length()==6,"Six source seasons required");
        for(int i=0;i<sources.length();i++){
            JSONObject source=sources.getJSONObject(i);boolean badminton=source.getString("sport").equals("Badminton");
            Set<String> fixtureIds=new HashSet<>(),teamIds=new HashSet<>();JSONArray teams=source.getJSONArray("teams");
            for(int t=0;t<teams.length();t++)check(teamIds.add(teams.getJSONObject(t).getString("id")),"Duplicate source team");
            JSONArray draws=source.getJSONArray("draws");
            for(int d=0;d<draws.length();d++){
                JSONObject draw=draws.getJSONObject(d);Set<String> members=new HashSet<>();JSONArray ids=draw.getJSONArray("teams");
                for(int t=0;t<ids.length();t++){check(teamIds.contains(ids.getString(t)),"Unknown draw member");members.add(ids.getString(t));}
                JSONArray rows=draw.getJSONArray("fixtures");
                for(int f=0;f<rows.length();f++){
                    JSONObject fixture=rows.getJSONObject(f);fixtures++;
                    check(fixtureIds.add(fixture.getString("id")),"Duplicate fixture");
                    check(members.contains(fixture.getString("home"))&&members.contains(fixture.getString("away")),"Fixture outside its poule");
                    check(!fixture.getString("home").equals(fixture.getString("away")),"Self fixture");
                    if(!badminton){if(fixture.optBoolean("complete",false))football++;continue;}
                    if(!fixture.has("games"))continue;
                    JSONArray contests=fixture.getJSONArray("games");check(contests.length()==8,"Badminton sheet must contain eight games");sheets++;
                    int h=0,a=0;boolean awarded=fixture.getString("sourceScore").contains("Niet gespeeld");
                    if(awarded)awards++;
                    Set<String> slots=new HashSet<>();
                    for(int g=0;g<contests.length();g++){
                        JSONObject game=contests.getJSONObject(g);games++;check(slots.add(game.getString("name")),"Duplicate game slot");
                        JSONArray score=game.getJSONArray("sets");List<MatchRules.SetScore> values=new ArrayList<>();
                        for(int n=0;n<score.length();n++){JSONArray set=score.getJSONArray(n);values.add(new MatchRules.SetScore(set.getInt(0),set.getInt(1)));sets++;}
                        MatchRules.ContestResult result;
                        try{result=MatchRules.contest(new MatchRules.Format(),values,game.getString("outcome"),"Published source outcome");}
                        catch(IllegalArgumentException ex){
                            if(!game.getString("outcome").endsWith("Retired"))throw new AssertionError(source.getString("name")+" fixture "+fixture.getString("id")+" "+game.getString("name"),ex);
                            // The website sometimes awards complete sets on retirement. Validate those numeric
                            // scores independently, retain the source outcome and never claim editor compatibility.
                            result=MatchRules.contest(new MatchRules.Format(),values,"Played","Published awarded retirement scores");
                            result.officialWinner=game.getString("outcome").equals("HomeRetired")?"Away":"Home";awardedRetirements++;
                        }
                        if(result.officialWinner.equals("Home"))h++;if(result.officialWinner.equals("Away"))a++;
                        int perSide=game.getString("name").contains("E")?1:2;
                        check(game.getJSONArray("home").length()<=perSide&&game.getJSONArray("away").length()<=perSide,"Too many source lineup names");
                    }
                    if(!awarded){Matcher m=Pattern.compile("(\\d+)\\s*-\\s*(\\d+)").matcher(fixture.getString("sourceScore"));
                        check(m.find()&&h==Integer.parseInt(m.group(1))&&a==Integer.parseInt(m.group(2)),"Game winners do not reconcile to fixture "+fixture.getString("id"));}
                }
            }
        }
        check(fixtures==3342&&sheets==621&&games==4968&&sets==10439&&football==121,"Frozen snapshot counts changed");
        System.out.println("PASS offline snapshot: "+fixtures+" fixture memberships; "+sheets+" badminton sheets; "+games+" game totals reconciled; "+sets+" numeric set scores checked; "+awards+" source-marked unplayed/awarded sheets; "+awardedRetirements+" source retirements with awarded completed scores (retained outside normal editor validation); "+football+" completed football results.");
    }
    static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
