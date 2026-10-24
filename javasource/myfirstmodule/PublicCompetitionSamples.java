package myfirstmodule;

import com.mendix.core.Core;
import com.mendix.systemwideinterfaces.core.*;
import com.mendix.thirdparty.org.json.*;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.*;
import java.util.*;
import java.util.regex.*;

/** Offline public-season snapshot. Called only inside DevelopmentSamples' authorized transaction. */
final class PublicCompetitionSamples {
    static final String FEATURE = "features:public-competition-seasons-2026-2027-v1";
    static final String PREFIX = "DEMO-PUBLIC-2026:";
    static final String RESOURCE = "competition-public-2026-2027.json";
    private PublicCompetitionSamples() {}

    static JSONObject snapshot() throws Exception {
        return new JSONObject(Files.readString(Core.getConfiguration().getResourcesPath().toPath().resolve(RESOURCE)));
    }

    static void populate(IContext ctx, DevelopmentSamples.Seed s) throws Exception {
        JSONObject data = snapshot();
        if (data.getInt("schemaVersion") != 1) throw new IllegalStateException("Unsupported public sample snapshot.");
        Map<String,IMendixObject> clubsByName = new LinkedHashMap<>();
        Map<String,IMendixObject> federations = new LinkedHashMap<>();Set<String> affiliations = new HashSet<>();
        for (Object value : array(data.getJSONArray("sources"))) {
            JSONObject source = (JSONObject)value;
            String sourceId = source.getString("id"), sport = source.getString("sport");
            String organizer=source.getString("organizer");IMendixObject fed=federations.get(organizer);
            if(fed==null){fed = s.make("Federation", "Name", "[DEMO] " + organizer,
                "Sport", sport, "IsActive", true, "Currency", "EUR", "CensusMonth", 1L, "CensusDay", 1L,
                "BillingMethod", "ACTIVE_AT_CENSUS", "Description", "Public season snapshot " + data.getString("capturedOn") + "; " + source.getString("level") + "; " + source.getString("url"));federations.put(organizer,fed);}
            Map<String,IMendixObject> clubs = new LinkedHashMap<>(), teams = new LinkedHashMap<>(), formats = new LinkedHashMap<>();
            for (Object item : array(source.getJSONArray("clubs"))) {
                JSONObject c = (JSONObject)item;
                String name = c.getString("name"), key = sport + ":" + name.trim().toLowerCase(Locale.ROOT);
                IMendixObject club = clubsByName.get(key);
                if (club == null) {
                    club = s.make("Club", "Name", "[DEMO] " + name, "Description", "Public club name; local development copy. No real accounts or contact details are imported.",
                        "IsPublished", true, "LocationVerified", false, "RequiresApproval", true, "RequiresPayment", false);
                    clubsByName.put(key, club);
                }
                clubs.put(c.getString("id"), club);
                if(!affiliations.add(s.id(fed)+":"+s.id(club)))continue;
                IMendixObject affiliation = s.make("FederationAffiliation", "ClubName", club.getValue(ctx,"Name"),
                    "StartsOn", day(source.getString("startsOn")), "EndsOn", day(source.getString("endsOn")), "IsActive", true,"Status","Approved");
                s.link(affiliation,"FederationAffiliation_Club",club); s.link(affiliation,"FederationAffiliation_Federation",fed);
            }
            for (Object item : array(source.getJSONArray("teams"))) {
                JSONObject t = (JSONObject)item;
                IMendixObject club = clubs.get(t.getString("club"));
                IMendixObject team = s.make("CompetitionTeam", "Name", t.getString("name"), "ClubName", club.getValue(ctx,"Name"),
                    "SeasonName", source.getString("season"), "IsActive", true);
                s.link(team,"CompetitionTeam_Club",club); teams.put(t.getString("id"),team);
            }
            // One division document per actual division; the source's A/B/... groups and stages are poules.
            Map<String,List<JSONObject>> divisions = new LinkedHashMap<>();
            for (Object item : array(source.getJSONArray("draws"))) {
                JSONObject draw = (JSONObject)item;
                divisions.computeIfAbsent(draw.getString("division"), k -> new ArrayList<>()).add(draw);
            }
            for (Map.Entry<String,List<JSONObject>> division : divisions.entrySet()) {
                List<JSONObject> draws = division.getValue(); JSONObject first = draws.get(0);
                String category = first.getString("category");
                IMendixObject format = formats.get(category);
                if (format == null) {
                    format = format(ctx,s,fed,source,category); formats.put(category,format);
                }
                int maxTeams = draws.stream().mapToInt(d -> d.getJSONArray("teams").length()).max().orElse(0);
                IMendixObject league = s.make("CompetitionLeague", "Name", "[DEMO] " + source.getString("name") + " / " + division.getKey(),
                    "ResultSettingsConfigured",true,"UseGameBreakdown",!sport.equals("Football"),"PointsPerGame",source.getString("level").equals("National"),
                    "WinPoints",BigDecimal.valueOf(source.getString("level").equals("National")?1:sport.equals("Football")?3:2),"DrawPoints",source.getString("level").equals("National")?BigDecimal.ZERO:BigDecimal.ONE,"LossPoints",BigDecimal.ZERO,
                    "SeasonName", source.getString("season"), "StartsOn", day(source.getString("startsOn")), "EndsOn", day(source.getString("endsOn")),
                    "RequestedPoules", draws.size(), "MaxTeamsPerPoule", maxTeams, "DurationMinutes", sport.equals("Football") ? 120L : 180L,
                    "Format", first.getString("format"), "Stage", "Published", "Info", "Public snapshot " + data.getString("capturedOn") + ". " + source.getString("url")
                    + " Published groups, pairings, dates and results are preserved. Unknown dates/times stay empty. Duration and review settings are editable demo defaults. No published approval or eligibility decisions are inferred. National play-off groups remain empty until teams are published; the planner supports round-robin generation only.");
                s.link(league,"CompetitionLeague_Federation",fed); s.link(league,"CompetitionLeague_MatchFormat",format);
                int pouleNumber = 0;
                for (JSONObject draw : draws) {
                    IMendixObject poule = s.make("CompetitionPoule", "Name", draw.getString("name"), "Number", ++pouleNumber, "TeamCount", draw.getJSONArray("teams").length());
                    s.link(poule,"CompetitionPoule_League",league);
                    Map<String,IMendixObject> entries = new LinkedHashMap<>(); int order = 0;
                    for (Object tid : array(draw.getJSONArray("teams"))) {
                        IMendixObject team = teams.get(tid.toString());
                        IMendixObject club = Core.retrieveId(ctx,team.getValue(ctx,"MyFirstModule.CompetitionTeam_Club"));
                        IMendixObject entry = s.make("LeagueEntry", "TeamName", team.getValue(ctx,"Name"), "ClubName", club.getValue(ctx,"Name"), "ClubKey", s.id(club),
                            "EntryKey", PREFIX + s.marker + ":" + sourceId + ":" + draw.getString("id") + ":" + tid, "Order", ++order);
                        s.link(entry,"LeagueEntry_Team",team); s.link(entry,"LeagueEntry_Club",club); s.link(entry,"LeagueEntry_League",league); s.link(entry,"LeagueEntry_Poule",poule);
                        entries.put(tid.toString(),entry);
                    }
                    Map<String,Integer> legs = new HashMap<>();
                    for (Object item : array(draw.getJSONArray("fixtures"))) {
                        JSONObject f = (JSONObject)item; IMendixObject home = entries.get(f.getString("home")), away = entries.get(f.getString("away"));
                        IMendixObject hc = Core.retrieveId(ctx,home.getValue(ctx,"MyFirstModule.LeagueEntry_Club")), ac = Core.retrieveId(ctx,away.getValue(ctx,"MyFirstModule.LeagueEntry_Club"));
                        String pair = f.getString("home").compareTo(f.getString("away")) < 0 ? f.getString("home") + ":" + f.getString("away") : f.getString("away") + ":" + f.getString("home");
                        IMendixObject fixture = s.make("CompetitionFixture", "HomeName", home.getValue(ctx,"TeamName"), "AwayName", away.getValue(ctx,"TeamName"),
                            "HomeKey",s.id(home),"AwayKey",s.id(away),"HomeClubKey",s.id(hc),"AwayClubKey",s.id(ac),"VenueName",f.getString("venue"),
                            "StartsAt",instant(f.optString("startsAt","")),"Status","Published","Version",1L,"Leg",legs.merge(pair,1,Integer::sum),
                            "PairKey",PREFIX + s.marker + ":" + sourceId + ":" + f.getString("id"),
                            "Issue",shortText("Snapshot " + data.getString("capturedOn") + " | " + f.optString("date","") + " | " + f.getString("round") + " | " + f.getString("sourceScore") + " | " + f.getString("sourceStatus")));
                        s.link(fixture,"CompetitionFixture_League",league); s.link(fixture,"CompetitionFixture_Poule",poule);
                        s.link(fixture,"CompetitionFixture_Home",home); s.link(fixture,"CompetitionFixture_Away",away);
                        s.link(fixture,"CompetitionFixture_HomeClub",hc); s.link(fixture,"CompetitionFixture_AwayClub",ac);
                        // Future fixtures initialize their eight contests from their division format on first open.
                        if (f.has("games") || (sport.equals("Football") && f.optBoolean("complete",false))) result(ctx,s,fixture,format,f,sport);
                    }
                }
            }
        }
    }

    private static IMendixObject format(IContext ctx,DevelopmentSamples.Seed s,IMendixObject fed,JSONObject source,String category) throws Exception {
        boolean football = source.getString("sport").equals("Football"), national = source.getString("level").equals("National");
        IMendixObject format = s.make("MatchFormat"); MatchRegistrationService.defaults(ctx,format);
        format.setValue(ctx,"Name","[DEMO] " + (football ? "Football" : "Badminton " + category) + (national ? " national" : "") + " 2026-2027");
        format.setValue(ctx,"Sport",source.getString("sport")); format.setValue(ctx,"Scoring",football ? "Total" : "Rally");
        format.setValue(ctx,"AvailableFrom",day(source.getString("startsOn"))); format.setValue(ctx,"AvailableUntil",day(source.getString("endsOn")));
        format.setValue(ctx,"WinPoints",BigDecimal.valueOf(football?3:2)); format.setValue(ctx,"StopWhenDecided",false);
        // Source sheets contain display-only names, not application memberships or eligibility evidence.
        for (String check : Arrays.asList("CheckMembership","CheckRegistration","CheckSuspension","CheckDiscipline","CheckAppearances","CheckIndex")) format.setValue(ctx,check,false);
        s.link(format,"MatchFormat_Federation",fed);
        String[] names = football ? new String[]{"Match"} : category.equals("Men") ? new String[]{"HD1","HD2","HD3","HD4","HE1","HE2","HE3","HE4"}
            : category.equals("Women") ? new String[]{"DD1","DD2","DD3","DD4","DE1","DE2","DE3","DE4"}
            : national ? new String[]{"HD","DD","HE1","HE2","DE1","DE2","GD1","GD2"} : new String[]{"HD","DD","HE1","DE1","HE2","DE2","GD1","GD2"};
        for (int i=0;i<names.length;i++) {
            String name = names[i];
            IMendixObject slot = s.make("MatchSlot","Name",name,"Sequence",i+1,"PlayersPerSide",football?0:name.contains("E")?1:2,
                "Discipline",football?"Open":name.startsWith("H")?"Men":name.startsWith("D")?"Women":"Mixed");
            s.link(slot,"MatchSlot_Format",format);
        }
        return format;
    }

    private static void result(IContext ctx,DevelopmentSamples.Seed s,IMendixObject fixture,IMendixObject format,JSONObject f,String sport) throws Exception {
        IMendixObject reg = s.make("MatchRegistration","State","SourceSnapshot","Revision",1L,"Reason","Public source snapshot: " + f.getString("url"));
        applySourceTotals(ctx,reg,f);
        if(sport.equals("Football")){reg.setValue(ctx,"HasFixtureScore",true);Core.commitWithoutEvents(ctx,reg);}
        s.link(reg,"MatchRegistration_Fixture",fixture); s.link(reg,"MatchRegistration_Format",format);
        Matcher score = Pattern.compile("(\\d+)\\s*-\\s*(\\d+)").matcher(f.getString("sourceScore"));
        if (score.find()) {
            long h=Long.parseLong(score.group(1)),a=Long.parseLong(score.group(2));
            reg.setValue(ctx,"HomeScore",h);reg.setValue(ctx,"AwayScore",a);reg.setValue(ctx,"Winner",h>a?"Home":a>h?"Away":"Draw");
            JSONArray points=f.optJSONArray("points");
            if(points!=null&&points.length()==2){reg.setValue(ctx,"HomePoints",BigDecimal.valueOf(points.getInt(0)));reg.setValue(ctx,"AwayPoints",BigDecimal.valueOf(points.getInt(1)));}
            else if(sport.equals("Football")){reg.setValue(ctx,"HomePoints",BigDecimal.valueOf(h>a?3:h==a?1:0));reg.setValue(ctx,"AwayPoints",BigDecimal.valueOf(a>h?3:h==a?1:0));}
            Core.commitWithoutEvents(ctx,reg);
        }
        JSONArray games=f.optJSONArray("games");
        if(games==null){games=new JSONArray();JSONArray sets=new JSONArray();sets.put(new JSONArray().put((Object)reg.getValue(ctx,"HomeScore")).put((Object)reg.getValue(ctx,"AwayScore")));
            games.put(new JSONObject().put("name","Match").put("sets",sets).put("outcome","Played").put("sourceScore",f.getString("sourceScore")).put("home",new JSONArray()).put("away",new JSONArray()));}
        int sequence=0;
        for(Object value:array(games)){
            JSONObject game=(JSONObject)value;String name=game.getString("name"),outcome=game.getString("outcome");boolean football=sport.equals("Football");
            IMendixObject contest=s.make("MatchContest","Name",name,"Sequence",++sequence,"PlayersPerSide",football?0:name.contains("E")?1:2,
                "Discipline",football?"Open":name.startsWith("H")?"Men":name.startsWith("D")?"Women":"Mixed","Outcome",outcome,"Revision",1L,
                "Reason",shortText("Published source: " + game.getString("sourceScore")),"RankingEligible",false);
            s.link(contest,"MatchContest_Registration",reg);s.link(contest,"MatchContest_Format",format);
            for(String side:Arrays.asList("home","away")){
                JSONArray names=game.getJSONArray(side);IMendixObject lineup=s.make("MatchLineup","Side",side.equals("home")?"Home":"Away","Revision",1L,
                    "Player1Name",names.length()>0?names.getString(0):"","Player2Name",names.length()>1?names.getString(1):"");s.link(lineup,"MatchLineup_Contest",contest);
            }
            int n=0,hs=0,as=0,ht=0,at=0;
            for(Object raw:array(game.getJSONArray("sets"))){
                JSONArray set=(JSONArray)raw;int h=set.getInt(0),a=set.getInt(1);ht+=h;at+=a;
                if(football||MatchRules.pointSetComplete(h,a,21,2,30)){if(h>a)hs++;if(a>h)as++;}
                IMendixObject record=s.make("MatchSet","Sequence",++n,"Home",h,"Away",a);s.link(record,"MatchSet_Contest",contest);
            }
            String recorded=football?(ht>at?"Home":at>ht?"Away":"Draw"):hs==2?"Home":as==2?"Away":"";
            String official=outcome.startsWith("Home")?"Away":outcome.startsWith("Away")?"Home":outcome.equals("Played")?recorded:"";
            contest.setValue(ctx,"HomeSets",(long)hs);contest.setValue(ctx,"AwaySets",(long)as);contest.setValue(ctx,"HomeTotal",(long)ht);contest.setValue(ctx,"AwayTotal",(long)at);
            contest.setValue(ctx,"RecordedWinner",recorded);contest.setValue(ctx,"OfficialWinner",official);Core.commitWithoutEvents(ctx,contest);
        }
    }

    /** Backfill only untouched imported sheets with the provider's credited retirement/forfeit totals. */
    static void initializeStandingsTotals(IContext ctx,Set<Long> owned) throws Exception {
        Map<String,JSONObject> rows=new HashMap<>();
        for(Object source:array(snapshot().getJSONArray("sources")))for(Object draw:array(((JSONObject)source).getJSONArray("draws")))for(Object item:array(((JSONObject)draw).getJSONArray("fixtures"))){JSONObject f=(JSONObject)item;rows.put(((JSONObject)source).getString("id")+":"+f.getString("id"),f);}
        Set<Long> edited=new HashSet<>();for(IMendixObject game:Core.createXPathQuery("//MyFirstModule.MatchContest[Revision>1]").execute(ctx)){IMendixIdentifier reg=game.getValue(ctx,"MyFirstModule.MatchContest_Registration");if(reg!=null)edited.add(reg.toLong());}
        for(IMendixObject reg:Core.createXPathQuery("//MyFirstModule.MatchRegistration[State='SourceSnapshot'][Revision=1]").execute(ctx)){
            if(!owned.contains(reg.getId().toLong())||edited.contains(reg.getId().toLong())||Boolean.TRUE.equals(reg.getValue(ctx,"SourceTotalsAvailable")))continue;
            IMendixObject fixture=Core.retrieveId(ctx,reg.getValue(ctx,"MyFirstModule.MatchRegistration_Fixture"));String pair=fixture.getValue(ctx,"PairKey");
            if(pair==null||!pair.startsWith(PREFIX))continue;String[] parts=pair.split(":");JSONObject source=rows.get(parts[parts.length-2]+":"+parts[parts.length-1]);if(source!=null)applySourceTotals(ctx,reg,source);
        }
    }

    private static void applySourceTotals(IContext ctx,IMendixObject reg,JSONObject fixture) throws Exception {
        JSONObject totals=fixture.optJSONObject("totals");if(totals==null)return;
        reg.setValue(ctx,"SourceTotalsAvailable",true);reg.setValue(ctx,"SourceCountsInStandings",totals.getBoolean("counted"));
        reg.setValue(ctx,"SourceHomeSets",totals.getLong("homeSets"));reg.setValue(ctx,"SourceAwaySets",totals.getLong("awaySets"));
        reg.setValue(ctx,"SourceHomeRallies",totals.getLong("homeRallies"));reg.setValue(ctx,"SourceAwayRallies",totals.getLong("awayRallies"));Core.commitWithoutEvents(ctx,reg);
    }

    static void verify(IContext ctx,Set<Long> owned) throws Exception {
        int expected=0,expectedResults=0;
        for(Object value:array(snapshot().getJSONArray("sources")))for(Object item:array(((JSONObject)value).getJSONArray("draws")))for(Object raw:array(((JSONObject)item).getJSONArray("fixtures"))){
            JSONObject f=(JSONObject)raw;expected++;if(f.has("games")||f.optBoolean("complete",false))expectedResults++;
        }
        List<IMendixObject> fixtures=Core.createXPathQuery("//MyFirstModule.CompetitionFixture[starts-with(PairKey,'"+PREFIX+"')]").execute(ctx);
        fixtures.removeIf(f -> !owned.contains(f.getId().toLong()));
        if(fixtures.size()!=expected)throw new IllegalStateException("Public snapshot fixture coverage mismatch.");
        int results=0;
        for(IMendixObject f:fixtures){
            if(!owned.contains(f.getId().toLong()))throw new IllegalStateException("Public fixture absent from sample ownership manifest.");
            List<IMendixObject> regs=Core.retrieveByPath(ctx,f,"MyFirstModule.MatchRegistration_Fixture");
            if(regs.isEmpty())continue;
            if(regs.size()!=1)throw new IllegalStateException("Duplicate source result sheet.");results++;
            IMendixObject reg=regs.get(0);IMendixObject format=Core.retrieveId(ctx,reg.getValue(ctx,"MyFirstModule.MatchRegistration_Format"));
            int count=Core.retrieveByPath(ctx,reg,"MyFirstModule.MatchContest_Registration").size();
            if(count!=("Badminton".equals(format.getValue(ctx,"Sport"))?8:1))throw new IllegalStateException("Incomplete source result sheet.");
        }
        if(results!=expectedResults)throw new IllegalStateException("Public result sheet coverage mismatch.");
        // Importing public sports names must never create or attach a login identity.
        for(IMendixObject fixture:fixtures)for(String side:Arrays.asList("Home","Away")){
            IMendixObject club=Core.retrieveId(ctx,fixture.getValue(ctx,"MyFirstModule.CompetitionFixture_"+side+"Club"));
            Collection<?> admins=club.getValue(ctx,"MyFirstModule.Club_Admins");
            if(admins!=null&&!admins.isEmpty())throw new IllegalStateException("A public source club received an administrator account.");
        }
    }

    private static Date day(String value){return Date.from(LocalDate.parse(value).atStartOfDay(ZoneOffset.UTC).toInstant());}
    private static Date instant(String value){return value==null||value.isBlank()?null:Date.from(OffsetDateTime.parse(value).toInstant());}
    private static String shortText(String value){return value.length()>200?value.substring(0,200):value;}
    private static List<Object> array(JSONArray value){List<Object> list=new ArrayList<>();for(int i=0;i<value.length();i++)list.add(value.get(i));return list;}
}
