package myfirstmodule;

import java.math.BigDecimal;
import java.util.*;

/** Pure league-table rules; pending/disputed results never become standings by inference. */
public final class CompetitionStandings {
    private CompetitionStandings() {}
    public static String summary(String home,String away,int homeScore,int awayScore,String winner,String state,boolean disputed,boolean awarded){
        if(winner==null||winner.isBlank())return "No result yet";
        String team=winner.equals("Home")?home:away;
        String label=winner.equals("Draw")?"Draw":team+" won";
        if(awarded)label=winner.equals("Draw")?"Unplayed":"Awarded to "+team;
        String status=disputed||state.equals("Disputed")?"Disputed · ":Set.of("Accepted","FederationFinal","SourceSnapshot").contains(state)?"":"Provisional · ";
        return homeScore+"–"+awayScore+" · "+status+label;
    }
    public static boolean wholePoints(BigDecimal value) {
        return value != null && value.stripTrailingZeros().scale() <= 0;
    }
    public static String pointsText(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
    public record Policy(boolean games, boolean pointsPerGame, BigDecimal win, BigDecimal draw, BigDecimal loss) {
        public Policy { Objects.requireNonNull(win);Objects.requireNonNull(draw);Objects.requireNonNull(loss);if(pointsPerGame&&!games)throw new IllegalArgumentException("Points per game requires a game breakdown."); }
        public BigDecimal points(int own,int opponent,int drawnGames,String winner,String side) {
            return pointsPerGame ? win.multiply(BigDecimal.valueOf(own)).add(loss.multiply(BigDecimal.valueOf(opponent))).add(draw.multiply(BigDecimal.valueOf(drawnGames)))
                : winner.equals("Draw")?draw:winner.equals(side)?win:loss;
        }
    }
    public static final class Team {
        public final String key,name,poule;
        public int position,played,won,drawn,lost;
        public long forScore,againstScore,forSets,againstSets,forPoints,againstPoints;
        public BigDecimal points=BigDecimal.ZERO;
        public Team(String key,String name,String poule){this.key=key;this.name=name;this.poule=poule;}
    }
    public record Result(String home,String away,int homeScore,int awayScore,String winner,
                         long homeSets,long awaySets,long homeRallies,long awayRallies,int drawnGames,
                         BigDecimal homeAdjustment,BigDecimal awayAdjustment,boolean official) {}
    public static List<Team> table(Collection<Team> entries,Collection<Result> results,Policy policy) {
        Map<String,Team> rows=new LinkedHashMap<>();
        for(Team e:entries){if(rows.put(e.key,new Team(e.key,e.name,e.poule))!=null)throw new IllegalArgumentException("Duplicate league entry.");}
        for(Result r:results){
            if(!r.official)continue;
            Team home=rows.get(r.home),away=rows.get(r.away);
            if(home==null||away==null||home==away||!home.poule.equals(away.poule))continue;
            if(!Set.of("Home","Away","Draw").contains(r.winner))throw new IllegalArgumentException("An official result needs an official winner or draw.");
            apply(home,r.homeScore,r.awayScore,r.homeSets,r.awaySets,r.homeRallies,r.awayRallies,r.winner,"Home",policy.points(r.homeScore,r.awayScore,r.drawnGames,r.winner,"Home").add(r.homeAdjustment));
            apply(away,r.awayScore,r.homeScore,r.awaySets,r.homeSets,r.awayRallies,r.homeRallies,r.winner,"Away",policy.points(r.awayScore,r.homeScore,r.drawnGames,r.winner,"Away").add(r.awayAdjustment));
        }
        List<Team> sorted=new ArrayList<>(rows.values());
        sorted.sort(Comparator.comparing((Team t)->t.poule).thenComparing(t->t.points,Comparator.reverseOrder())
            .thenComparing(Comparator.comparingLong((Team t)->t.forScore-t.againstScore).reversed())
            .thenComparing(Comparator.comparingLong((Team t)->t.forSets-t.againstSets).reversed())
            .thenComparing(Comparator.comparingLong((Team t)->t.forPoints-t.againstPoints).reversed())
            .thenComparing(t->t.name,String.CASE_INSENSITIVE_ORDER).thenComparing(t->t.key));
        String poule=null;int rank=0;for(Team t:sorted){if(!t.poule.equals(poule)){poule=t.poule;rank=0;}t.position=++rank;}
        return sorted;
    }
    private static void apply(Team t,long own,long opponent,long sets,long otherSets,long rallies,long otherRallies,String winner,String side,BigDecimal points){
        t.played++;if(winner.equals("Draw"))t.drawn++;else if(winner.equals(side))t.won++;else t.lost++;
        t.forScore+=own;t.againstScore+=opponent;t.forSets+=sets;t.againstSets+=otherSets;t.forPoints+=rallies;t.againstPoints+=otherRallies;t.points=t.points.add(points);
    }
}
