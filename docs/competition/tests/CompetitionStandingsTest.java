import myfirstmodule.CompetitionStandings;
import com.mendix.thirdparty.org.json.*;
import java.nio.file.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.regex.*;

/** Standalone table-policy tests and source reconciliation; no claim about runtime permissions or UI. */
public final class CompetitionStandingsTest {
    static int checks;
    static final BigDecimal ZERO=BigDecimal.ZERO,ONE=BigDecimal.ONE;
    static CompetitionStandings.Policy fixture=new CompetitionStandings.Policy(false,false,BigDecimal.valueOf(3),ONE,ZERO);
    static CompetitionStandings.Result result(String h,String a,int hs,int as,String winner,boolean official){return new CompetitionStandings.Result(h,a,hs,as,winner,0,0,0,0,0,ZERO,ZERO,official);}
    static CompetitionStandings.Team team(String id,String poule){return new CompetitionStandings.Team(id,id,poule);}
    static CompetitionStandings.Team get(List<CompetitionStandings.Team> t,String key){return t.stream().filter(x->x.key.equals(key)).findFirst().orElseThrow();}
    static void eq(Object actual,Object expected,String message){checks++;if(!Objects.equals(actual,expected))throw new AssertionError(message+": "+actual+" != "+expected);}
    public static void main(String[] args)throws Exception{
        List<CompetitionStandings.Team> entries=List.of(team("A","P"),team("B","P"),team("C","P"),team("D","Q"));
        List<CompetitionStandings.Result> results=List.of(result("A","B",2,0,"Home",true),result("A","B",0,0,"Draw",true),result("B","C",5,0,"Home",false),result("A","D",9,0,"Home",true));
        var table=CompetitionStandings.table(entries,results,fixture);
        eq(get(table,"A").points,new BigDecimal("4"),"Win and draw league points");eq(get(table,"A").played,2,"Only official in-poule fixtures count");eq(get(table,"A").won,1,"Wins");eq(get(table,"A").drawn,1,"Draws");
        eq(get(table,"B").points,ONE,"Draw point for away side");eq(get(table,"B").lost,1,"Loss");eq(get(table,"C").played,0,"Zero-played team remains");eq(get(table,"D").position,1,"Rank resets for every poule");eq(get(table,"A").forScore,2L,"Goals for");
        var changed=CompetitionStandings.table(entries,results,new CompetitionStandings.Policy(false,false,BigDecimal.valueOf(5),BigDecimal.valueOf(2),BigDecimal.valueOf(-1)));
        eq(get(changed,"A").points,BigDecimal.valueOf(7),"Changed league policy recalculates the whole table");eq(get(changed,"B").points,ONE,"Negative loss points supported");eq(get(table,"A").points,BigDecimal.valueOf(4),"Inputs and earlier projections stay independent");
        var gamesPolicy=new CompetitionStandings.Policy(true,true,ONE,ZERO,ZERO);
        var match=new CompetitionStandings.Result("A","B",7,1,"Home",14,3,332,228,0,BigDecimal.valueOf(-1),ZERO,true);
        var gamesTable=CompetitionStandings.table(entries,List.of(match),gamesPolicy);
        eq(get(gamesTable,"A").points,BigDecimal.valueOf(6),"Points per game plus adjustment");eq(get(gamesTable,"B").points,ONE,"Losing fixture retains its game credit");eq(get(gamesTable,"A").forSets,14L,"Game/set totals");eq(get(gamesTable,"A").forPoints,332L,"Rally totals");
        var overridden=CompetitionStandings.table(entries,List.of(result("A","B",1,3,"Home",true)),fixture);
        eq(get(overridden,"A").won,1,"Official winner overrides numeric winner");eq(get(overridden,"A").points,BigDecimal.valueOf(3),"Official winner receives fixture points");
        try{new CompetitionStandings.Policy(false,true,ONE,ZERO,ZERO);throw new AssertionError("Per-game points without games accepted");}catch(IllegalArgumentException expected){checks++;}
        eq(CompetitionStandings.summary("A","B",0,0,"","Draft",false,false),"No result yet","Scheduled fixture is not a draw");
        eq(CompetitionStandings.summary("A","B",2,1,"Home","Accepted",false,false),"2–1 · A won","Home winner is named");
        eq(CompetitionStandings.summary("A","B",0,0,"Draw","Accepted",false,false),"0–0 · Draw","Completed nil-nil draw");
        eq(CompetitionStandings.summary("A","B",1,3,"Away","Provisional",false,false),"1–3 · Provisional · B won","Provisional winner is labelled");
        eq(CompetitionStandings.summary("A","B",2,1,"Home","Accepted",true,false),"2–1 · Disputed · A won","Dispute is never hidden");
        eq(CompetitionStandings.summary("A","B",8,0,"Home","SourceSnapshot",false,true),"8–0 · Awarded to A","Source award is not described as a played win");
        eq(CompetitionStandings.wholePoints(new BigDecimal("22.00000000")),true,"Whole stored decimal accepted");
        eq(CompetitionStandings.wholePoints(new BigDecimal("-1.5")),false,"Fractional deduction rejected");
        eq(CompetitionStandings.wholePoints(new BigDecimal("0.5")),false,"Fractional award rejected");
        eq(CompetitionStandings.pointsText(new BigDecimal("22.00000000")),"22","Whole competition points text");
        eq(CompetitionStandings.pointsText(new BigDecimal("0.00000000")),"0","Zero points without scientific notation");
        reconcileScreenshot(Path.of(args[0]));
        System.out.println("PASS "+checks+" standings checks, including the exact eight-team national screenshot table.");
    }
    static void reconcileScreenshot(Path resource)throws Exception{
        JSONObject root=new JSONObject(Files.readString(resource));JSONObject source=root.getJSONArray("sources").getJSONObject(0);JSONObject draw=null;
        for(int i=0;i<source.getJSONArray("draws").length();i++){var d=source.getJSONArray("draws").getJSONObject(i);if(d.getString("name").equals("Mixed – Division 1 – Qualification"))draw=d;}
        if(draw==null)throw new AssertionError("National qualification source missing");
        Map<String,String> names=new HashMap<>();JSONArray teams=source.getJSONArray("teams");for(int i=0;i<teams.length();i++)names.put(teams.getJSONObject(i).getString("id"),teams.getJSONObject(i).getString("name"));
        List<CompetitionStandings.Team> entries=new ArrayList<>();JSONArray ids=draw.getJSONArray("teams");for(int i=0;i<ids.length();i++)entries.add(new CompetitionStandings.Team(ids.getString(i),names.get(ids.getString(i)),draw.getString("name")));
        List<CompetitionStandings.Result> results=new ArrayList<>();JSONArray fixtures=draw.getJSONArray("fixtures");
        for(int i=0;i<fixtures.length();i++){
            JSONObject f=fixtures.getJSONObject(i);if(!f.has("games"))continue;Matcher m=Pattern.compile("(\\d+)-(\\d+)").matcher(f.getString("sourceScore"));if(!m.find())continue;
            int h=Integer.parseInt(m.group(1)),a=Integer.parseInt(m.group(2));long hs=0,as=0,hp=0,ap=0;JSONArray games=f.getJSONArray("games");
            for(int g=0;g<games.length();g++){JSONArray sets=games.getJSONObject(g).getJSONArray("sets");for(int n=0;n<sets.length();n++){JSONArray set=sets.getJSONArray(n);int x=set.getInt(0),y=set.getInt(1);hp+=x;ap+=y;if(x>y)hs++;if(y>x)as++;}}
            JSONObject totals=f.getJSONObject("totals");hs=totals.getLong("homeSets");as=totals.getLong("awaySets");hp=totals.getLong("homeRallies");ap=totals.getLong("awayRallies");
            results.add(new CompetitionStandings.Result(f.getString("home"),f.getString("away"),h,a,h>a?"Home":a>h?"Away":"Draw",hs,as,hp,ap,0,ZERO,ZERO,totals.getBoolean("counted")));
        }
        var actual=CompetitionStandings.table(entries,results,new CompetitionStandings.Policy(true,true,ONE,ZERO,ZERO));
        String[] expectedNames={"W&L","Hebad 1","Lebad","Bad79","Olve 1","Pluimplukkers","DZ99","Buggenhout"};
        long[][] values={{22,3,3,0,0,22,2,44,9,1069,750},{22,3,3,0,0,22,2,44,9,1058,762},{21,3,3,0,0,21,3,44,9,1089,807},{13,3,2,0,1,13,11,30,23,981,923},{10,3,1,0,2,10,14,24,31,925,1010},{6,3,0,0,3,6,18,16,38,914,1070},{2,3,0,0,3,2,22,8,45,772,1064},{0,3,0,0,3,0,24,2,48,626,1048}};
        eq(actual.size(),8,"All national teams appear");
        for(int i=0;i<actual.size();i++){var t=actual.get(i);eq(t.name,expectedNames[i],"National rank "+(i+1));long[] fields={t.points.longValueExact(),t.played,t.won,t.drawn,t.lost,t.forScore,t.againstScore,t.forSets,t.againstSets,t.forPoints,t.againstPoints};for(int k=0;k<fields.length;k++)eq(fields[k],values[i][k],t.name+" screenshot column "+k);}
    }
}
