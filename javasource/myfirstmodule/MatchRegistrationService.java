package myfirstmodule;



import com.mendix.core.Core;

import com.mendix.systemwideinterfaces.core.*;

import java.math.BigDecimal;

import java.time.Instant;

import java.util.*;



/** Server boundary for private match storage. Every command rechecks its scope and revision. */

public final class MatchRegistrationService {

    private static final String M = "MyFirstModule.";

    private static final String[] CONFIG = ("Name Sport Version Scoring SetsToWin Target Margin Cap GamesToWin GamesMargin TieAt TieTarget DecidingTarget GameMode WinPoints DrawPoints LossPoints TiePolicy StopWhenDecided HomeHours AwayHours MinimumReviewHours ApprovalAnchor LineupLock LockMinutes MaxAppearances MaxSingles MaxDoubles MinIndex MaxIndex MaxTeamIndex CheckMembership CheckRegistration CheckSuspension CheckDiscipline CheckAppearances CheckIndex AvailableFrom AvailableUntil IsAvailable MaxStarters MaxReserves RecordPlayers RecordSetScores").split(" ");

    private final IContext caller, ctx;

    private final IMendixObject user;

    private final String actor, userKey;

    private final List<IMendixObject> dirty = new ArrayList<>();



    private MatchRegistrationService(IContext caller, IContext ctx) throws Exception {

        if (caller.getSession() == null) throw new SecurityException("Sign in to register a match.");

        this.caller = caller; this.ctx = ctx;

        this.user = caller.getSession().getUser(caller).getMendixObject();

        this.actor = text(user, "Name"); this.userKey = id(user);

        if (!bool(user, "Active")) throw new SecurityException("This account is disabled.");

    }



    private MatchRegistrationService(IContext systemContext) {

        this.caller = systemContext; this.ctx = systemContext; this.user = null;

        this.actor = "Automatic result acceptance"; this.userKey = "";

    }



    /** Intended for a private scheduled microflow; a signed-in client cannot invoke the system sweep. */

    public static synchronized void expireAcceptance(IContext caller) throws Exception {

        if (caller.getSession() != null) throw new SecurityException("Acceptance expiry is a private system job.");

        IContext ctx = Core.createSystemContext(); ctx.startTransaction();

        try {

            MatchRegistrationService s = new MatchRegistrationService(ctx);

            Core.microflowCall(M + "SUB_Competition_Lock").execute(ctx);

            for (IMendixObject r : s.query("MatchRegistration", "[SubmittedAt != empty][FederationFinal = false][Disputed = false][State = 'Provisional']")) s.refreshAcceptance(r);

            if (!s.dirty.isEmpty()) Core.commitWithoutEvents(ctx, s.dirty);

            ctx.endTransaction();

        } catch (Exception e) { ctx.rollbackTransaction(); throw e; }

    }



    /** Serializes local mutations; the existing database claim also detects cross-runtime races. */

    public static synchronized IMendixObject execute(IContext caller, IMendixObject input, String operation) throws Exception {

        // Calculated calendar cells are read-only and must also work in private system reads.
        if("CalendarSummary".equals(operation)){
            IContext readContext=Core.createSystemContext();
            MatchRegistrationService s=caller.getSession()==null&&caller.isSudo()?new MatchRegistrationService(readContext):new MatchRegistrationService(caller,readContext);
            IMendixObject fixture=s.required(input,"FixtureKey","CompetitionFixture");
            if(s.user!=null)s.require(s.canRead(fixture),"You cannot access this fixture.");
            IMendixObject result=s.form("Calendar result");s.put(result,"Info",s.calendarSummary(fixture,s.registration(fixture,false)));return result;
        }

        IContext ctx = Core.createSystemContext(); ctx.startTransaction();

        try {

            MatchRegistrationService s = new MatchRegistrationService(caller, ctx);

            if(!"Division".equals(operation)&&!"FixtureGames".equals(operation))Core.microflowCall(M + "SUB_Competition_Lock").execute(ctx);

            IMendixObject output = s.command(input, operation);

            if (!s.dirty.isEmpty()) Core.commitWithoutEvents(ctx, s.dirty);

            ctx.endTransaction(); return output;

        } catch (Exception e) { ctx.rollbackTransaction(); throw e; }

    }



    private IMendixObject command(IMendixObject f, String op) throws Exception {

        if (f == null) f = make("MatchForm");

        switch (op) {

            case "Formats": return formats(required(f, "FederationKey", "Federation"));

            case "MyFixtures": return myFixtures();

            case "NewFormat": return newFormat(f);

            case "OpenFormat": return openFormat(required(f, "SelectedKey", "MatchFormat"));

            case "SaveFormat": return saveFormat(f);

            case "NewVersion": return newVersion(f);

            case "SaveAvailability": return availability(f);

            case "NewSlot": return newSlot(f);

            case "SaveSlot": return saveSlot(f);

            case "OpenSlot": return openSlot(f);

            case "RemoveSlot": return removeSlot(f);

            case "PickSlotScoring": put(f,"Sport",text(required(f,"SelectedKey","MatchFormat"),"Name")); return f;

            case "AssignFormat": return assign(f);

            case "Division": return division(required(f, "LeagueKey", "CompetitionLeague"));

            case "SaveLeagueResults": return saveLeagueResults(f);

            case "SaveFixtureScore": return saveFixtureScore(f);

            case "OpenFixture": return fixture(required(f, "FixtureKey", "CompetitionFixture"));

            case "FixtureGames": return fixtureGames(required(f, "FixtureKey", "CompetitionFixture"));

            case "ClaimHome": return claim(f, "Home");

            case "ClaimAway": return claim(f, "Away");

            case "AppointLeader": return appoint(f);

            case "Start": return start(f);

            case "OpenContest": return contestView(required(f, "SelectedKey", "MatchContest"));

            case "OpenHomeLineup": return lineupView(f, "Home");

            case "OpenAwayLineup": return lineupView(f, "Away");

            case "SaveLineup": return saveLineup(f);

            case "PickPlayer1": return pickPlayer(f, 1);

            case "PickPlayer2": return pickPlayer(f, 2);

            case "OpenRoster": return roster(f);

            case "OpenHomeSquad": return squadView(required(f,"RecordKey","MatchRegistration"),"Home");

            case "OpenAwaySquad": return squadView(required(f,"RecordKey","MatchRegistration"),"Away");

            case "SquadStarter": return saveSquadPlayer(f,"Starter");

            case "SquadReserve": return saveSquadPlayer(f,"Reserve");

            case "SquadRemove": return removeSquadPlayer(f);

            case "SubstitutionOff": return pickSubstitution(f,1);

            case "SubstitutionOn": return pickSubstitution(f,2);

            case "SaveSubstitution": return saveSubstitution(f);

            case "NewSet": return setView(f, null);

            case "OpenSet": return setView(f, required(f, "SelectedKey", "MatchSet"));

            case "SaveSet": return saveSet(f);

            case "SaveContest": return saveContest(f);

            case "SaveScores": return saveScores(f);
            case "PickHome": return pickPlayers(f,"Home");
            case "PickAway": return pickPlayers(f,"Away");
            case "SaveGame": return saveGame(f);

            case "Submit": return submit(f);

            case "AcceptHome": return accept(f, "Home");

            case "AcceptAway": return accept(f, "Away");

            case "DisputeHome": return dispute(f, "Home");

            case "DisputeAway": return dispute(f, "Away");

            case "FederationFinalize": return finalizeRuling(f);

            case "Eligibility": return eligibilityView(f);

            case "OpenEligibility": required(f,"SelectedKey","CompetitionPlayer"); require(fedPermission(required(f,"FederationKey","Federation"),"Manage"),"Federation management permission is required."); put(f,"Title","Effective player eligibility"); return f;

            case "SaveEligibility": return saveEligibility(f);

            case "VerifySamples": put(f,"Info",DevelopmentSamples.verifyMatchLifecycle(caller));return f;

            default: throw new IllegalArgumentException("Unknown match registration command.");

        }

    }



    private IMendixObject formats(IMendixObject fed) throws Exception {

        require(fedPermission(fed, "Manage"), "Only federation managers can configure formats.");

        IMendixObject f = form("Federation match formats"); put(f, "FederationKey", id(fed)); put(f, "CanManage", true);

        for (IMendixObject rule : query("MatchFormat", "[" + M + "MatchFormat_Federation=" + id(fed) + "]"))

            row(f, rule, "Format", text(rule, "Name") + " v" + integer(rule, "Version"),

                text(rule, "Sport") + " · " + text(rule, "Scoring") + " · " + (bool(rule, "IsAvailable") ? "Available" : "Unavailable"));

        return f;

    }



    private IMendixObject myFixtures() throws Exception {

        IMendixObject f=form("My appointed matches");put(f,"Info","Fixtures where you are the appointed official or captain. Team managers can also open fixtures through their division workspace.");

        for(IMendixObject r:query("MatchRegistration","[LeaderKey='"+userKey+"' or HomeCaptainKey='"+userKey+"' or AwayCaptainKey='"+userKey+"']")){IMendixObject fixture=ref(r,"MatchRegistration_Fixture");if(canRead(fixture))row(f,fixture,"Fixture",text(fixture,"HomeName")+" — "+text(fixture,"AwayName"),text(fixture,"StartsAt")+" · "+text(r,"State"));}return f;

    }



    private IMendixObject newFormat(IMendixObject f) throws Exception {

        IMendixObject fed = required(f, "FederationKey", "Federation");

        require(fedPermission(fed, "Manage"), "Federation management permission is required.");

        IMendixObject out = form("New match format"); put(out, "FederationKey", id(fed)); defaults(out); put(out, "CanManage", true); return out;

    }



    static void defaults(IContext c, IMendixObject o) {

        Map<String, Object> values = new LinkedHashMap<>();

        values.put("Version", 1); values.put("Scoring", "Rally"); values.put("SetsToWin", 2); values.put("Target", 21); values.put("Margin", 2); values.put("Cap", 30);

        values.put("GamesToWin", 6); values.put("GamesMargin", 2); values.put("TieAt", 6); values.put("TieTarget", 7); values.put("GameMode", "Advantage");

        values.put("WinPoints", new BigDecimal("2")); values.put("DrawPoints", BigDecimal.ONE); values.put("LossPoints", BigDecimal.ZERO);

        values.put("TiePolicy", "Draw"); values.put("HomeHours", 36); values.put("AwayHours", 36); values.put("MinimumReviewHours", 12);

        values.put("ApprovalAnchor", "Scheduled"); values.put("LineupLock", "Start"); values.put("IsAvailable", true); values.put("RecordPlayers", true); values.put("RecordSetScores", true);

        for (String key : Arrays.asList("CheckMembership", "CheckRegistration", "CheckSuspension", "CheckDiscipline", "CheckAppearances", "CheckIndex")) values.put(key, true);

        values.forEach((k,v) -> o.setValue(c,k,v instanceof Integer ? ((Integer)v).longValue() : v));

    }

    private void defaults(IMendixObject o) { defaults(ctx, o); }



    private IMendixObject openFormat(IMendixObject rule) throws Exception {

        IMendixObject fed = ref(rule, "MatchFormat_Federation"); require(fedPermission(fed, "Manage"), "Federation management permission is required.");

        IMendixObject f = form("Match format · " + text(rule, "Name")); copy(rule, f, CONFIG); put(f, "RecordKey", id(rule)); put(f, "FederationKey", id(fed)); put(f, "CanManage", true);

        put(f, "Info", used(rule) ? "This version is in use. Create a new version to change scoring or slots. Availability can be changed with a reason; existing fixtures retain their rules." : "This draft can be edited until it is assigned or used.");

        for (IMendixObject slot : slots(rule)) row(f, slot, "Slot", integer(slot,"Sequence") + ". " + text(slot,"Name"), integer(slot,"PlayersPerSide") + " player(s) per side · " + text(slot,"Discipline") + (bool(slot,"IsDecider") ? " · Decider" : ""));

        for (IMendixObject league : query("CompetitionLeague", "["+M+"CompetitionLeague_MatchFormat="+id(rule)+"]")) row(f, league, "AffectedDivision", text(league,"Name"), "Starts " + text(league,"StartsOn") + "; includes every poule/subdivision");

        return f;

    }



    private IMendixObject saveFormat(IMendixObject f) throws Exception {

        IMendixObject fed=required(f,"FederationKey","Federation"); require(fedPermission(fed,"Manage"),"Federation management permission is required.");

        IMendixObject rule=optional(f,"RecordKey","MatchFormat");

        if(rule!=null) { require(same(ref(rule,"MatchFormat_Federation"),fed),"Format belongs to another federation."); require(!used(rule),"Create a new version; this format is already in use."); }

        require(!text(f,"Name").trim().isEmpty() && !text(f,"Sport").trim().isEmpty(),"Name and sport are required.");

        rules(f).validate(); require(integer(f,"LockMinutes")>=0 && integer(f,"MaxAppearances")>=0 && integer(f,"MaxSingles")>=0 && integer(f,"MaxDoubles")>=0,"Limits cannot be negative.");
        require(integer(f,"MaxStarters")>=0&&integer(f,"MaxStarters")<=30&&integer(f,"MaxReserves")>=0&&integer(f,"MaxReserves")<=30,"Team sheet starters and reserves must be between 0 and 30.");
        require(!bool(f,"RecordSetScores")||text(f,"Scoring").equals("Total")||(integer(f,"SetsToWin")>=1&&integer(f,"SetsToWin")<=3),"The fixture score table has up to five set columns: use one to three sets to win.");

        require(date(f,"AvailableFrom")==null || date(f,"AvailableUntil")==null || date(f,"AvailableFrom").before(date(f,"AvailableUntil")),"Availability end must follow its start.");

        String before=rule==null?"New format":snapshot(rule,CONFIG);

        if(rule==null) {rule=make("MatchFormat"); link(rule,"MatchFormat_Federation",fed); put(f,"Version",1);}

        int version=integer(rule,"Version"); copy(f,rule,CONFIG); put(rule,"Version",Math.max(1,version)); save(rule);

        audit(null,fed,null,"Format saved",reasonOr(f,"Format configuration"),before,snapshot(rule,CONFIG),"");

        return openFormat(rule);

    }



    private IMendixObject newVersion(IMendixObject f) throws Exception {

        IMendixObject old=required(f,"RecordKey","MatchFormat"), fed=ref(old,"MatchFormat_Federation"); require(fedPermission(fed,"Manage"),"Federation management permission is required.");

        int latest=integer(old,"Version");for(IMendixObject other:query("MatchFormat","["+M+"MatchFormat_Federation="+id(fed)+"]"))if(text(other,"Name").equals(text(old,"Name")))latest=Math.max(latest,integer(other,"Version"));

        IMendixObject next=make("MatchFormat"); copy(old,next,CONFIG); put(next,"Version",latest+1); link(next,"MatchFormat_Federation",fed); save(next);

        for(IMendixObject slot:slots(old)){IMendixObject clone=make("MatchSlot");copy(slot,clone,"Name","Sequence","PlayersPerSide","Discipline","IsDecider"); link(clone,"MatchSlot_Format",next); link(clone,"MatchSlot_ScoringOverride",ref(slot,"MatchSlot_ScoringOverride")); save(clone);}

        audit(null,fed,null,"Format version created",reasonOr(f,"New format version"),id(old),id(next),""); return openFormat(next);

    }



    private IMendixObject availability(IMendixObject f) throws Exception {

        IMendixObject rule=required(f,"RecordKey","MatchFormat"),fed=ref(rule,"MatchFormat_Federation"); require(fedPermission(fed,"Manage"),"Federation management permission is required."); requireReason(f);

        Date from=date(f,"AvailableFrom"),until=date(f,"AvailableUntil");require(from==null||until==null||from.before(until),"Availability end must follow its start.");

        String before=snapshot(rule,"IsAvailable","AvailableFrom","AvailableUntil"); copy(f,rule,"IsAvailable","AvailableFrom","AvailableUntil"); save(rule);

        audit(null,fed,null,"Format availability changed",text(f,"Reason"),before,snapshot(rule,"IsAvailable","AvailableFrom","AvailableUntil"),""); return openFormat(rule);

    }



    private IMendixObject newSlot(IMendixObject f) throws Exception {

        IMendixObject rule=required(f,"RecordKey","MatchFormat"); require(fedPermission(ref(rule,"MatchFormat_Federation"),"Manage")&&!used(rule),"Slots are editable only in an unused format version.");

        IMendixObject out=form("New fixture slot"); put(out,"ContestKey",id(rule)); put(out,"FederationKey",id(ref(rule,"MatchFormat_Federation"))); put(out,"Sequence",slots(rule).size()+1); put(out,"PlayersPerSide",1);put(out,"Discipline","Open"); slotChoices(out,rule); return out;

    }



    private IMendixObject openSlot(IMendixObject f) throws Exception {

        IMendixObject slot=required(f,"SelectedKey","MatchSlot"),rule=ref(slot,"MatchSlot_Format");

        require(fedPermission(ref(rule,"MatchFormat_Federation"),"Manage")&&!used(rule),"Create a new version to edit slots already in use.");

        IMendixObject out=form("Fixture slot");copy(slot,out,"Name","Sequence","PlayersPerSide","Discipline","IsDecider");put(out,"RecordKey",id(slot));put(out,"ContestKey",id(rule));put(out,"FederationKey",id(ref(rule,"MatchFormat_Federation")));

        IMendixObject scoring=ref(slot,"MatchSlot_ScoringOverride");put(out,"SelectedKey",scoring==null?"":id(scoring));put(out,"Sport",scoring==null?"Inherit fixture format":text(scoring,"Name"));slotChoices(out,rule);return out;

    }

    private void slotChoices(IMendixObject f,IMendixObject rule) throws Exception {for(IMendixObject choice:query("MatchFormat","["+M+"MatchFormat_Federation="+id(ref(rule,"MatchFormat_Federation"))+"]"))if(!same(choice,rule))row(f,choice,"SlotScoring",text(choice,"Name")+" v"+integer(choice,"Version"),text(choice,"Scoring"));}

    private IMendixObject removeSlot(IMendixObject f) throws Exception {IMendixObject slot=required(f,"RecordKey","MatchSlot"),rule=ref(slot,"MatchSlot_Format");require(fedPermission(ref(rule,"MatchFormat_Federation"),"Manage")&&!used(rule),"Slots are editable only in an unused format.");requireReason(f);audit(null,ref(rule,"MatchFormat_Federation"),null,"Fixture slot removed",text(f,"Reason"),snapshot(slot,"Name","Sequence"),"Removed","");Core.delete(ctx,slot);return openFormat(rule);}



    private IMendixObject saveSlot(IMendixObject f) throws Exception {

        IMendixObject rule=required(f,"ContestKey","MatchFormat"),fed=ref(rule,"MatchFormat_Federation"); require(fedPermission(fed,"Manage")&&!used(rule),"Slots are editable only in an unused format version.");

        require(integer(f,"Sequence")>0 && integer(f,"PlayersPerSide")>=0 && integer(f,"PlayersPerSide")<=2 && !text(f,"Name").trim().isEmpty(),"Name, positive order and 0–2 participants per side are required. Use zero for team-only scoring.");

        IMendixObject s=optional(f,"RecordKey","MatchSlot");if(s!=null)require(same(ref(s,"MatchSlot_Format"),rule),"Slot belongs to another format.");

        for(IMendixObject other:slots(rule))require(same(other,s)||integer(other,"Sequence")!=integer(f,"Sequence"),"Slot order must be unique.");

        IMendixObject scoring=optional(f,"SelectedKey","MatchFormat");if(scoring!=null)require(!same(scoring,rule)&&same(ref(scoring,"MatchFormat_Federation"),fed),"Choose a scoring override from this federation.");

        if(s==null)s=make("MatchSlot");copy(f,s,"Name","Sequence","PlayersPerSide","Discipline","IsDecider");link(s,"MatchSlot_Format",rule);link(s,"MatchSlot_ScoringOverride",scoring);save(s);

        audit(null,fed,null,"Fixture slot added",reasonOr(f,"Format configuration"),"",snapshot(s,"Name","Sequence","PlayersPerSide","Discipline","IsDecider"),"");return openFormat(rule);

    }



    private IMendixObject assign(IMendixObject f) throws Exception {

        IMendixObject league=required(f,"LeagueKey","CompetitionLeague"),rule=required(f,"SelectedKey","MatchFormat"),fed=ref(league,"CompetitionLeague_Federation");

        require(fedPermission(fed,"Manage")&&same(ref(rule,"MatchFormat_Federation"),fed),"Choose a format from this federation.");

        require(MatchRules.available(bool(rule,"IsAvailable"),instant(rule,"AvailableFrom"),instant(rule,"AvailableUntil"),instant(league,"StartsOn")),"This format is unavailable for the division start date.");

        require(!slots(rule).isEmpty(),"Define fixture slots before assigning this format.");

        if(date(league,"StartsOn")!=null&&!date(league,"StartsOn").after(new Date())){require(fedPermission(fed,"Override"),"A started season requires federation override permission.");requireReason(f);}

        IMendixObject old=ref(league,"CompetitionLeague_MatchFormat");String leagueSport=text(league,"Sport").trim();require(leagueSport.isEmpty()||leagueSport.equalsIgnoreCase(text(rule,"Sport").trim()),"The match format must use the league's sport.");link(league,"CompetitionLeague_MatchFormat",rule);LeagueSports.initializeOrValidate(ctx,league);save(league);

        audit(null,fed,league,"Division format assigned",reasonOr(f,"Division format selection"),old==null?"None":id(old),id(rule)+"; existing fixture snapshots preserved","");return division(league);

    }



    private IMendixObject division(IMendixObject league) throws Exception {

        IMendixObject fed=ref(league,"CompetitionLeague_Federation"); boolean readable="Published".equals(text(league,"Stage"))||fedPermission(fed,"Read");for(IMendixObject entry:query("LeagueEntry","["+M+"LeagueEntry_League="+id(league)+"]")){IMendixObject club=ref(entry,"LeagueEntry_Club");if(club!=null&&Boolean.TRUE.equals(Core.microflowCall(M+"SUB_Competition_ClubPermission").withParam("Club",club).withParam("Operation","Read").execute(caller)))readable=true;}require(readable,"You cannot access this division."); IMendixObject f=form(text(league,"Name")+" · Results and standings");put(f,"LeagueKey",id(league));put(f,"FederationKey",id(fed));put(f,"CanManage",fedPermission(fed,"Manage"));

        IMendixObject rule=ref(league,"CompetitionLeague_MatchFormat");put(f,"Info",rule==null?"Federation: choose a match format before registering fixtures.":"Format: "+text(rule,"Name")+" v"+integer(rule,"Version")+". All poules/subdivisions inherit this format.");

        if(bool(f,"CanManage"))for(IMendixObject r:query("MatchFormat","["+M+"MatchFormat_Federation="+id(fed)+"]"))row(f,r,"FormatChoice",text(r,"Name")+" v"+integer(r,"Version"),MatchRules.available(bool(r,"IsAvailable"),instant(r,"AvailableFrom"),instant(r,"AvailableUntil"),instant(league,"StartsOn"))?"Available for division start":"Unavailable for division start");

        CompetitionStandings.Policy policy=leaguePolicy(league);put(f,"UseGameBreakdown",policy.games());put(f,"PointsPerGame",policy.pointsPerGame());put(f,"WinPoints",policy.win());put(f,"DrawPoints",policy.draw());put(f,"LossPoints",policy.loss());
        put(f,"Info",text(f,"Info")+"\nLeague points: win "+CompetitionStandings.pointsText(policy.win())+", draw "+CompetitionStandings.pointsText(policy.draw())+", loss "+CompetitionStandings.pointsText(policy.loss())+(policy.pointsPerGame()?" per individual game.":" per fixture.")+" Standings include accepted, federation-final and published source results. Ties: score difference, set difference, rally-point difference, team name.");
        appendLeagueOverview(f,league,policy);

        return f;

    }

    private CompetitionStandings.Policy leaguePolicy(IMendixObject league) throws Exception {
        IMendixObject format=ref(league,"CompetitionLeague_MatchFormat");
        if(bool(league,"ResultSettingsConfigured"))return new CompetitionStandings.Policy(bool(league,"UseGameBreakdown"),bool(league,"PointsPerGame"),decimal(league,"WinPoints"),decimal(league,"DrawPoints"),decimal(league,"LossPoints"));
        boolean games=format!=null&&!"Total".equals(text(format,"Scoring"));
        return new CompetitionStandings.Policy(games,false,format==null?new BigDecimal("2"):decimal(format,"WinPoints"),format==null?BigDecimal.ONE:decimal(format,"DrawPoints"),format==null?BigDecimal.ZERO:decimal(format,"LossPoints"));
    }

    private IMendixObject saveLeagueResults(IMendixObject f) throws Exception {
        IMendixObject league=required(f,"LeagueKey","CompetitionLeague"),fed=ref(league,"CompetitionLeague_Federation");
        require(fedPermission(fed,"Manage"),"Only federation league managers can change result settings.");
        CompetitionStandings.Policy previous=leaguePolicy(league),next=new CompetitionStandings.Policy(bool(f,"UseGameBreakdown"),bool(f,"PointsPerGame"),decimal(f,"WinPoints"),decimal(f,"DrawPoints"),decimal(f,"LossPoints"));
        require(CompetitionStandings.wholePoints(next.win())&&CompetitionStandings.wholePoints(next.draw())&&CompetitionStandings.wholePoints(next.loss()),"Competition points must be whole numbers.");
        require(next.win().abs().compareTo(BigDecimal.valueOf(100000))<=0&&next.draw().abs().compareTo(BigDecimal.valueOf(100000))<=0&&next.loss().abs().compareTo(BigDecimal.valueOf(100000))<=0,"Competition points must be between -100000 and 100000.");
        if(previous.games()!=next.games())require(first("MatchRegistration","["+M+"MatchRegistration_Fixture/"+M+"CompetitionFixture/"+M+"CompetitionFixture_League="+id(league)+"]")==null,"The game breakdown cannot change after fixture registration has started. Create a separate league to change its result structure.");
        if(date(league,"StartsOn")!=null&&!new Date().before(date(league,"StartsOn"))){require(fedPermission(fed,"Override"),"Changing an active league's points requires federation override permission.");requireReason(f);}
        if(next.games()){IMendixObject format=ref(league,"CompetitionLeague_MatchFormat");require(format!=null&&!slots(format).isEmpty(),"Select a federation format with game slots before enabling the breakdown.");}
        copy(f,league,"UseGameBreakdown","PointsPerGame","WinPoints","DrawPoints","LossPoints");put(league,"ResultSettingsConfigured",true);save(league);
        audit(null,fed,league,"League result settings changed",reasonOr(f,"Federation league configuration"),previous.toString(),next.toString(),"");return division(league);
    }

    private String calendarSummary(IMendixObject fixture,IMendixObject reg){
        if(reg==null)return "No result yet";
        return CompetitionStandings.summary(text(fixture,"HomeName"),text(fixture,"AwayName"),integer(reg,"HomeScore"),integer(reg,"AwayScore"),text(reg,"Winner"),text(reg,"State"),bool(reg,"Disputed"),text(reg,"State").equals("SourceSnapshot")&&bool(reg,"SourceTotalsAvailable")&&!bool(reg,"SourceCountsInStandings"));
    }

    private void appendLeagueOverview(IMendixObject form,IMendixObject league,CompetitionStandings.Policy policy) throws Exception {
        String lid=id(league);List<IMendixObject> fixtures=query("CompetitionFixture","["+M+"CompetitionFixture_League="+lid+"]");
        Map<String,IMendixObject> poules=new HashMap<>(),entries=new LinkedHashMap<>(),registrations=new HashMap<>();Map<String,List<IMendixObject>> games=new HashMap<>();
        for(IMendixObject p:query("CompetitionPoule","["+M+"CompetitionPoule_League="+lid+"]"))poules.put(id(p),p);
        for(IMendixObject e:query("LeagueEntry","["+M+"LeagueEntry_League="+lid+"]"))entries.put(id(e),e);
        for(IMendixObject r:query("MatchRegistration","["+M+"MatchRegistration_Fixture/"+M+"CompetitionFixture/"+M+"CompetitionFixture_League="+lid+"]"))registrations.put(referenceKey(r,"MatchRegistration_Fixture"),r);
        if(policy.games())for(IMendixObject c:query("MatchContest","["+M+"MatchContest_Registration/"+M+"MatchRegistration/"+M+"MatchRegistration_Fixture/"+M+"CompetitionFixture/"+M+"CompetitionFixture_League="+lid+"]"))games.computeIfAbsent(referenceKey(c,"MatchContest_Registration"),k->new ArrayList<>()).add(c);
        List<CompetitionStandings.Team> teams=new ArrayList<>();List<CompetitionStandings.Result> results=new ArrayList<>();
        for(IMendixObject e:entries.values()){IMendixObject p=poules.get(referenceKey(e,"LeagueEntry_Poule"));teams.add(new CompetitionStandings.Team(id(e),text(e,"TeamName"),p==null?"Unallocated":text(p,"Name")));}
        fixtures.sort(Comparator.comparing((IMendixObject fx)->date(fx,"StartsAt"),Comparator.nullsLast(Comparator.naturalOrder())).thenComparing(fx->text(fx,"HomeName")));
        for(IMendixObject fixture:fixtures){
            if(!"Published".equals(text(fixture,"Status"))&&!canRead(fixture))continue;
            IMendixObject reg=registrations.get(id(fixture)),poule=poules.get(referenceKey(fixture,"CompetitionFixture_Poule"));
            IMendixObject row=row(form,fixture,"Fixture",text(fixture,"HomeName")+" — "+text(fixture,"AwayName"),calendarSummary(fixture,reg));
            put(row,"PouleName",text(poule,"Name"));put(row,"StartsAt",date(fixture,"StartsAt"));copy(fixture,row,"HomeName","AwayName","VenueName");put(row,"ResultSummary",calendarSummary(fixture,reg));
            if(reg==null)continue;
            List<IMendixObject> contests=games.getOrDefault(id(reg),Collections.emptyList());
            boolean official=Set.of("Accepted","FederationFinal","SourceSnapshot").contains(text(reg,"State"))&&!bool(reg,"Disputed")&&!text(reg,"Winner").isBlank();
            // A provider may pre-award future withdrawals while its standings still count zero appearances.
            if(text(reg,"State").equals("SourceSnapshot")&&policy.games()&&!contests.isEmpty()&&contests.stream().allMatch(c->text(c,"Outcome").equals("NotPlayed"))&&decimal(reg,"HomePoints").signum()==0&&decimal(reg,"AwayPoints").signum()==0)official=false;
            long hs=0,as=0,hp=0,ap=0;int draws=0;
            for(IMendixObject c:contests)if(!bool(c,"Excluded")){hs+=integer(c,"HomeSets");as+=integer(c,"AwaySets");hp+=integer(c,"HomeTotal");ap+=integer(c,"AwayTotal");if(text(c,"OfficialWinner").equals("Draw"))draws++;}
            if(text(reg,"State").equals("SourceSnapshot")&&bool(reg,"SourceTotalsAvailable")&&integer(reg,"Revision")==1&&contests.stream().allMatch(c->integer(c,"Revision")==1)){
                official=official&&bool(reg,"SourceCountsInStandings");hs=integer(reg,"SourceHomeSets");as=integer(reg,"SourceAwaySets");hp=integer(reg,"SourceHomeRallies");ap=integer(reg,"SourceAwayRallies");
            }
            results.add(new CompetitionStandings.Result(referenceKey(fixture,"CompetitionFixture_Home"),referenceKey(fixture,"CompetitionFixture_Away"),integer(reg,"HomeScore"),integer(reg,"AwayScore"),text(reg,"Winner"),hs,as,hp,ap,draws,decimal(reg,"HomeAdjustment"),decimal(reg,"AwayAdjustment"),official));
        }
        for(CompetitionStandings.Team t:CompetitionStandings.table(teams,results,policy)){
            IMendixObject row=row(form,entries.get(t.key),"Standing",t.name,t.poule);put(row,"PouleName",t.poule);put(row,"Position",t.position);put(row,"Points",t.points);
            put(row,"Played",t.played);put(row,"Won",t.won);put(row,"Drawn",t.drawn);put(row,"Lost",t.lost);put(row,"ScoreForAgainst",t.forScore+" – "+t.againstScore);
            if(policy.games()){put(row,"SetsForAgainst",t.forSets+" – "+t.againstSets);put(row,"PointsForAgainst",t.forPoints+" – "+t.againstPoints);}
        }
    }

    private String referenceKey(IMendixObject object,String association){IMendixIdentifier value=object.getValue(ctx,M+association);return value==null?"":Long.toString(value.toLong());}

    private IMendixObject saveFixtureScore(IMendixObject f) throws Exception {
        IMendixObject reg=required(f,"RecordKey","MatchRegistration"),fixture=ref(reg,"MatchRegistration_Fixture");
        require(!leaguePolicy(ref(fixture,"CompetitionFixture_League")).games(),"This league requires results for its individual games.");require(canEnter(reg),"Only the appointed official or federation may enter this score.");version(reg,f);
        int home=integer(f,"Home"),away=integer(f,"Away");require(home>=0&&away>=0&&home<=1000000&&away<=1000000,"Scores must be between zero and one million.");if(date(reg,"SubmittedAt")!=null)requireReason(f);
        String before=snapshot(reg,"HomeScore","AwayScore","Winner");put(reg,"HomeScore",home);put(reg,"AwayScore",away);put(reg,"HasFixtureScore",true);recalculate(reg,date(reg,"SubmittedAt")!=null);touch(reg);
        audit(reg,null,null,"Fixture score saved",reasonOr(f,"Official fixture result entry"),before,snapshot(reg,"HomeScore","AwayScore","Winner"),"");return fixture(fixture);
    }



    private IMendixObject registration(IMendixObject fixture, boolean create) throws Exception {

        IMendixObject r=first("MatchRegistration","["+M+"MatchRegistration_Fixture="+id(fixture)+"]");

        if(r!=null||!create)return r;

        IMendixObject league=ref(fixture,"CompetitionFixture_League"),rule=ref(league,"CompetitionLeague_MatchFormat");boolean games=leaguePolicy(league).games();require(!games||rule!=null,"The federation must select a division format first.");

        require(fedPermission(ref(league,"CompetitionLeague_Federation"),"Manage")||club(fixture,"Home","Update")||club(fixture,"Away","Update"),"Only participating team managers or the federation can initialize registration.");

        require(!games||!slots(rule).isEmpty(),"This format has no fixture slots.");r=make("MatchRegistration");link(r,"MatchRegistration_Fixture",fixture);if(rule!=null)link(r,"MatchRegistration_Format",rule);put(r,"State","Draft");put(r,"Revision",1);save(r);

        for(IMendixObject slot:games?slots(rule):Collections.<IMendixObject>emptyList()){

            IMendixObject c=make("MatchContest");copy(slot,c,"Name","Sequence","PlayersPerSide","Discipline","IsDecider");put(c,"Outcome","Pending");link(c,"MatchContest_Registration",r);IMendixObject override=ref(slot,"MatchSlot_ScoringOverride");link(c,"MatchContest_Format",override==null?rule:override);put(c,"Revision",1);save(c);

            for(String side:Arrays.asList("Home","Away")){IMendixObject l=make("MatchLineup");put(l,"Side",side);put(l,"Revision",1);link(l,"MatchLineup_Contest",c);save(l);}

        }

        audit(r,null,league,"Registration initialized","Division format snapshot","",id(rule),"");return r;

    }



    private IMendixObject fixture(IMendixObject fixture) throws Exception {

        require(canRead(fixture),"You cannot access this fixture."); IMendixObject r=registration(fixture,true), rule=ref(r,"MatchRegistration_Format"); refreshAcceptance(r);

        IMendixObject f=form(text(fixture,"HomeName")+" — "+text(fixture,"AwayName")); put(f,"FixtureKey",id(fixture));put(f,"RecordKey",id(r));put(f,"Revision",integer(r,"Revision"));put(f,"LeaderName",text(r,"LeaderName"));
        CompetitionStandings.Policy pointsPolicy=leaguePolicy(ref(fixture,"CompetitionFixture_League"));put(f,"UseGameBreakdown",pointsPolicy.games());put(f,"Home",integer(r,"HomeScore"));put(f,"Away",integer(r,"AwayScore"));

        put(f,"CanHome",club(fixture,"Home","Update"));put(f,"CanAway",club(fixture,"Away","Update"));put(f,"CanLead",isLeader(r));put(f,"CanOverride",override(r));put(f,"CanManage",manage(r));

        boolean sourceSnapshot="SourceSnapshot".equals(text(r,"State"));
        int drawnGames=pointsPolicy.pointsPerGame()?(int)contests(r).stream().filter(c->text(c,"OfficialWinner").equals("Draw")&&!bool(c,"Excluded")).count():0;
        BigDecimal homePoints=text(r,"Winner").isBlank()?BigDecimal.ZERO:pointsPolicy.points(integer(r,"HomeScore"),integer(r,"AwayScore"),drawnGames,text(r,"Winner"),"Home").add(decimal(r,"HomeAdjustment"));
        BigDecimal awayPoints=text(r,"Winner").isBlank()?BigDecimal.ZERO:pointsPolicy.points(integer(r,"AwayScore"),integer(r,"HomeScore"),drawnGames,text(r,"Winner"),"Away").add(decimal(r,"AwayAdjustment"));
        put(f,"Info",text(r,"State")+" · "+(sourceSnapshot&&text(r,"Winner").isBlank()?"No published score":"Result "+integer(r,"HomeScore")+"–"+integer(r,"AwayScore"))+" · Official winner: "+text(r,"Winner")+" · Competition points "+CompetitionStandings.pointsText(homePoints)+" / "+CompetitionStandings.pointsText(awayPoints)+(sourceSnapshot?"\n"+text(fixture,"Issue")+"\n"+text(r,"Reason"):" · Home: "+text(r,"HomeAcceptance")+" until "+text(r,"HomeDeadline")+" · Away: "+text(r,"AwayAcceptance")+" until "+text(r,"AwayDeadline"))+(bool(r,"Disputed")?" · DISPUTED: "+text(r,"DisputeReason"):""));

        if(bool(f,"UseGameBreakdown"))for(IMendixObject c:contests(r))row(f,c,"Contest",integer(c,"Sequence")+". "+text(c,"Name"),fixtureContestDetail(r,c));

        if(rule!=null){put(f,"MaxStarters",integer(rule,"MaxStarters"));put(f,"MaxReserves",integer(rule,"MaxReserves"));}teamSheetRows(f,r,true);
        gameTable(f,r,rule);

        for(IMendixObject a:query("MatchAudit","["+M+"MatchAudit_Registration="+id(r)+"]"))if(text(a,"PrivateSide").isEmpty()||date(r,"StartedAt")!=null||independentLeader(r)||override(r)||userKey.equals(text(r,text(a,"PrivateSide")+"CaptainKey")))row(f,a,"Audit",text(a,"At")+" · "+text(a,"Actor")+" · "+text(a,"Action"),text(a,"Reason")+" · Before: "+text(a,"BeforeValue")+" · After: "+text(a,"AfterValue"));

        List<String> visibleWarnings=new ArrayList<>();for(IMendixObject c:contests(r))for(IMendixObject l:lineups(c))if(canSeeLineup(r,l)){String w=lineupWarnings(r,c,l);if(!w.isEmpty())visibleWarnings.add(text(c,"Name")+" · "+text(l,"Side")+": "+w);}put(f,"Info",text(f,"Info")+"\n"+String.join("\n",visibleWarnings));

        copy(r,f,"HomeAdjustment","AwayAdjustment","WinnerOverride");return f;

    }

    /** Read-only game and player breakdown for the fixture detail page; never creates a registration. */
    private IMendixObject fixtureGames(IMendixObject fixture) throws Exception {
        require(canRead(fixture),"You cannot access this fixture.");IMendixObject r=registration(fixture,false);
        IMendixObject f=form("Games");put(f,"FixtureKey",id(fixture));
        if(r==null){put(f,"Info","No games or players registered yet.");return f;}
        IMendixObject rule=ref(r,"MatchRegistration_Format");if(rule!=null){put(f,"MaxStarters",integer(rule,"MaxStarters"));put(f,"MaxReserves",integer(rule,"MaxReserves"));}
        List<IMendixObject> contests=contests(r);
        put(f,"UseGameBreakdown",contests.stream().anyMatch(c->integer(c,"PlayersPerSide")>0));
        for(IMendixObject c:contests){
            if(integer(c,"PlayersPerSide")==0)continue;
            Map<String,String> names=new HashMap<>();
            for(IMendixObject l:lineups(c))names.put(text(l,"Side"),!canSeeLineup(r,l)?"Hidden until start":text(l,"Player1Name").isBlank()?"Not entered":text(l,"Player1Name")+(text(l,"Player2Name").isBlank()?"":" / "+text(l,"Player2Name")));
            // Score column holds only the sets; the page bolds the winning side (Detail = official winner).
            String scores=scoreText(c).replace("\n",", "),note=text(c,"Outcome").equals("Played")||text(c,"Outcome").equals("Pending")?"":" ("+text(c,"Outcome")+")";
            if(bool(c,"Excluded"))note+=" (excluded)";
            IMendixObject row=row(f,c,"Game",text(c,"Name"),text(c,"OfficialWinner"));
            put(row,"HomeName",names.getOrDefault("Home","Not entered"));put(row,"AwayName",names.getOrDefault("Away","Not entered"));put(row,"ResultSummary",(scores.isBlank()?"–":scores)+note);
            put(row,"Position",integer(c,"Sequence"));
        }
        teamSheetRows(f,r,true);
        if(!bool(f,"UseGameBreakdown")&&integer(f,"MaxStarters")+integer(f,"MaxReserves")==0)put(f,"Info","This fixture has no individual games or team sheet.");
        return f;
    }

    /** All games stay visible together; each participant side still uses the existing lineup privacy guard. */
    private String fixtureContestDetail(IMendixObject registration,IMendixObject contest) throws Exception {
        List<String> detail=new ArrayList<>();
        detail.add(integer(contest,"PlayersPerSide")==0?"Team score":integer(contest,"PlayersPerSide")==1?"Singles":"Doubles");
        detail.add(text(contest,"Discipline"));
        for(IMendixObject lineup:lineups(contest))if(canSeeLineup(registration,lineup))
            detail.add(text(lineup,"Side")+": "+(text(lineup,"Player1Name").isBlank()?"Not entered":text(lineup,"Player1Name"))+(text(lineup,"Player2Name").isBlank()?"":" / "+text(lineup,"Player2Name")));
        String scores=scoreText(contest);detail.add(scores.isBlank()?"No score entered":scores.replace("\n"," · "));
        detail.add(text(contest,"Outcome"));detail.add("Official winner: "+text(contest,"OfficialWinner"));
        if(bool(contest,"Excluded"))detail.add("EXCLUDED from fixture and ranking");
        return String.join(" · ",detail);
    }



    /** Fixture games table: one row per configured game, player pickers per side before the start and set-score boxes once started. */
    private void gameTable(IMendixObject f,IMendixObject r,IMendixObject rule) throws Exception {
        boolean players=rule==null||bool(rule,"RecordPlayers"),started=date(r,"StartedAt")!=null,scoring=canEnter(r)&&date(r,"SubmittedAt")==null;int widest=0;
        Map<String,Map<String,IMendixObject>> rosters=new HashMap<>();List<IMendixObject> gameRows=new ArrayList<>();
        for(IMendixObject c:contests(r)){
            int perSide=players?integer(c,"PlayersPerSide"):0,columns=setColumns(c);widest=Math.max(widest,columns);
            IMendixObject row=row(f,c,"Game",text(c,"Name"),text(c,"OfficialWinner"));gameRows.add(row);
            put(row,"Position",integer(c,"Sequence"));put(row,"PlayersPerSide",perSide);put(row,"CanScore",scoring&&!bool(c,"Excluded"));
            String scores=scoreText(c).replace("\n",", "),outcome=text(c,"Outcome");put(row,"ResultSummary",outcome.equals("Played")||outcome.equals("Pending")?scores:(scores.isBlank()?outcome:scores+" ("+outcome+")"));
            List<IMendixObject> sets=sets(c);for(int i=0;i<Math.min(5,sets.size());i++){put(row,"Set"+(i+1)+"Home",integer(sets.get(i),"Home"));put(row,"Set"+(i+1)+"Away",integer(sets.get(i),"Away"));}
            for(String side:Arrays.asList("Home","Away")){
                IMendixObject l=first("MatchLineup","["+M+"MatchLineup_Contest="+id(c)+"][Side='"+side+"']");
                boolean visible=l!=null&&canSeeLineup(r,l),edit=visible&&perSide>0&&!started&&canEditLineup(r,side);
                put(row,side+"Name",perSide==0?"":!visible?"Hidden until start":text(l,"Player1Name").isBlank()?"Not entered":text(l,"Player1Name")+(text(l,"Player2Name").isBlank()?"":" / "+text(l,"Player2Name")));
                put(row,"CanEdit"+side,edit);if(!edit)continue;
                Map<String,IMendixObject> roster=rosters.get(side);
                if(roster==null){roster=new HashMap<>();List<IMendixObject> squad=query("CompetitionPlayer","["+M+"CompetitionPlayer_Team="+id(team(r,side))+"]");squad.sort(Comparator.comparing(p->text(p,"MemberName")));for(IMendixObject p:squad)roster.put(id(p),row(f,p,side+"Player",text(p,"MemberName"),""));rosters.put(side,roster);}
                IMendixObject p1=ref(l,"MatchLineup_Player1"),p2=ref(l,"MatchLineup_Player2");
                if(p1!=null)link(row,"MatchRow_"+side+"Pick1",roster.get(id(p1)));if(p2!=null)link(row,"MatchRow_"+side+"Pick2",roster.get(id(p2)));
            }
        }
        // Every row shows the widest game's set columns so the table stays aligned; extra sets are rejected on save.
        for(IMendixObject row:gameRows)put(row,"SetColumns",widest);
        put(f,"RecordPlayers",players);put(f,"RecordSetScores",rule==null||bool(rule,"RecordSetScores"));put(f,"SetColumns",widest);put(f,"IsStarted",started);put(f,"CanScore",scoring);
    }

    /** Configured set columns for a game: one final score for team totals or when the format records no set scores. */
    private int setColumns(IMendixObject c) throws Exception {MatchRules.Format rules=contestRules(c);return rules.family.equals("Total")?1:Math.min(5,Math.max(1,2*rules.setsToWin-1));}

    /** The format's "record set scores" switch turns every game into a single final score. */
    private MatchRules.Format contestRules(IMendixObject c) throws Exception {MatchRules.Format rules=rules(ref(c,"MatchContest_Format"));IMendixObject rule=ref(ref(c,"MatchContest_Registration"),"MatchRegistration_Format");if(rule!=null&&!bool(rule,"RecordSetScores"))rules.family="Total";return rules;}

    /** The games-table row the client sent with this command (it carries the picked players and typed scores). */
    private IMendixObject gameRow(IMendixObject f,IMendixObject c) throws Exception {
        for(IMendixObject row:Core.retrieveByPath(caller,f,M+"MatchRow_Form"))if("Game".equals(text(row,"Kind"))&&id(c).equals(text(row,"RecordKey")))return row;
        require(false,"Reopen the fixture; this game is no longer on the page.");return null;
    }

    private IMendixObject picked(IMendixObject row,String association) throws Exception {
        Object key=row.getValue(caller,M+association);if(key==null)return null;
        IMendixObject pick=Core.retrieveId(caller,(IMendixIdentifier)key);if(pick==null)return null;
        return byKey(text(pick,"RecordKey"),"CompetitionPlayer");
    }

    /** Captain (or game leader) picks a side's players for one game straight from the games table, before the start. */
    private IMendixObject pickPlayers(IMendixObject f,String side) throws Exception {
        IMendixObject c=required(f,"SelectedKey","MatchContest"),r=ref(c,"MatchContest_Registration"),fixture=ref(r,"MatchRegistration_Fixture");
        IMendixObject l=first("MatchLineup","["+M+"MatchLineup_Contest="+id(c)+"][Side='"+side+"']");
        require(l!=null&&date(r,"StartedAt")==null&&canSeeLineup(r,l)&&canEditLineup(r,side),"Players are picked here before the start by the team captain or the game leader.");
        IMendixObject row=gameRow(f,c),p1=picked(row,"MatchRow_"+side+"Pick1"),p2=integer(c,"PlayersPerSide")>1?picked(row,"MatchRow_"+side+"Pick2"):null;
        for(IMendixObject p:Arrays.asList(p1,p2))if(p!=null)require(same(ref(p,"CompetitionPlayer_Team"),team(r,side)),"Choose a player from the team's registered roster.");
        require(p1==null||p2==null||!same(p1,p2),"Choose two different players.");
        String before=participants(l);link(l,"MatchLineup_Player1",p1);link(l,"MatchLineup_Player2",p2);put(l,"Player1Name",p1==null?"":text(p1,"MemberName"));put(l,"Player2Name",p2==null?"":text(p2,"MemberName"));put(l,"Warnings",lineupWarnings(r,c,l));touch(l);put(r,"WarningsAcknowledged",false);touch(r);
        audit(r,null,null,"Lineup saved","Games table entry",before,participants(l),side);return fixture(fixture);
    }

    /** Game leader saves one game's set scores from the games table; partial scores stay pending until a side has won. */
    private IMendixObject saveGame(IMendixObject f) throws Exception {
        IMendixObject c=required(f,"SelectedKey","MatchContest"),r=ref(c,"MatchContest_Registration");
        require(canEnter(r)&&date(r,"SubmittedAt")==null,"The game leader enters scores after the start and before submission. Corrections go through the game page.");
        IMendixObject row=gameRow(f,c);int columns=setColumns(c);MatchRules.Format rules=contestRules(c);
        List<MatchRules.SetScore> scores=new ArrayList<>();List<String> raw=new ArrayList<>();
        for(int i=1;i<=5;i++){
            Object home=row.getValue(caller,M+"MatchRow.Set"+i+"Home"),away=row.getValue(caller,M+"MatchRow.Set"+i+"Away");if(home==null&&away==null)continue;
            require(i<=columns,"This game has "+columns+(columns==1?" score.":" sets."));require(home!=null&&away!=null,"Enter both scores for set "+i+".");require(scores.size()==i-1,"Fill in the sets in order; set "+(scores.size()+1)+" is empty.");
            int h=((Number)home).intValue(),a=((Number)away).intValue();require(h>=0&&a>=0,"Scores cannot be negative.");
            boolean deciding=rules.family.equals("Games")&&rules.decidingTarget>0&&i-1==2*rules.setsToWin-2;
            scores.add(new MatchRules.SetScore(h,a,0,0,false,deciding));raw.add(h+"-"+a);
        }
        String current=text(c,"Outcome"),outcome=current.equals("Pending")||current.equals("Played")?"Pending":current;
        MatchRules.ContestResult result=MatchRules.contest(rules,scores,outcome,text(c,"Reason"));
        if(outcome.equals("Pending")&&!result.recordedWinner.isEmpty()){outcome="Played";result=MatchRules.contest(rules,scores,outcome,text(c,"Reason"));}
        String before=scoreText(c)+"; "+snapshot(c,"Outcome","RecordedWinner","OfficialWinner");replaceSets(c,scores);put(c,"Outcome",outcome);applyResult(c,result);touch(c);touch(r);
        audit(r,null,null,"Game score saved","Games table entry",before,String.join(", ",raw)+"; "+snapshot(c,"Outcome","RecordedWinner","OfficialWinner"),"");
        return fixture(ref(r,"MatchRegistration_Fixture"));
    }

    private void replaceSets(IMendixObject c,List<MatchRules.SetScore> scores) throws Exception {
        for(IMendixObject s:sets(c)){dirty.remove(s);Core.delete(ctx,s);}int sequence=0;
        for(MatchRules.SetScore score:scores){IMendixObject s=make("MatchSet");put(s,"Sequence",++sequence);put(s,"Home",score.home);put(s,"Away",score.away);put(s,"HomeTie",score.homeTie);put(s,"AwayTie",score.awayTie);put(s,"HasTie",score.hasTie);put(s,"IsMatchTie",score.matchTie);link(s,"MatchSet_Contest",c);save(s);}
    }

    private IMendixObject claim(IMendixObject f,String side) throws Exception {

        IMendixObject fixture=required(f,"FixtureKey","CompetitionFixture"),r=registration(fixture,true);require(club(fixture,side,"Update"),"Your club competition permission does not cover this team.");require(date(r,"StartedAt")==null,"Captain assignments are locked after fixture start.");

        String key=side+"CaptainKey",before=text(r,key);put(r,key,userKey);touch(r);audit(r,null,null,side+" captain appointed",reasonOr(f,"Captain claimed by authorized team manager"),before,userKey,"");return fixture(fixture);

    }



    private IMendixObject appoint(IMendixObject f) throws Exception {

        IMendixObject r=required(f,"RecordKey","MatchRegistration"),fixture=ref(r,"MatchRegistration_Fixture");version(r,f);

        require(userKey.equals(text(r,"HomeCaptainKey"))||override(r),"Only the home captain or federation may appoint the game leader.");require(date(r,"StartedAt")==null||override(r),"Only the federation can replace an official after fixture start.");

        IMendixObject leader=Core.getUser(ctx,text(f,"LeaderName"))==null?null:Core.getUser(ctx,text(f,"LeaderName")).getMendixObject();require(leader!=null&&bool(leader,"Active"),"Choose an active account by its sign-in name.");

        if(date(r,"StartedAt")!=null)requireReason(f);String before=text(r,"LeaderName");put(r,"LeaderKey",id(leader));put(r,"LeaderName",text(leader,"Name"));touch(r);audit(r,null,null,"Game leader appointed",reasonOr(f,"Home captain appointment"),before,text(r,"LeaderName"),"");return fixture(fixture);

    }



    private IMendixObject start(IMendixObject f) throws Exception {

        IMendixObject r=required(f,"RecordKey","MatchRegistration"),fixture=ref(r,"MatchRegistration_Fixture");version(r,f);require(isLeader(r),"Only the appointed game leader starts the fixture.");require(date(r,"StartedAt")==null&&date(r,"SubmittedAt")==null,"This fixture has already started.");require(text(fixture,"Status").equals("Published"),"Publish the scheduled fixture first.");

        require(bool(f,"WarningsAcknowledged"),"The game leader must acknowledge the lineup and eligibility warnings.");String warnings=warnings(r);put(r,"WarningsAcknowledged",true);put(r,"WarningAcknowledgment",warnings);put(r,"StartedAt",new Date());put(r,"State","Started");touch(r);audit(r,null,null,"Fixture started","Official acknowledged eligibility warnings",warnings,"Lineups locked and revealed","");return fixture(fixture);

    }



    private IMendixObject contestView(IMendixObject c) throws Exception {

        IMendixObject r=ref(c,"MatchContest_Registration"),fixture=ref(r,"MatchRegistration_Fixture");require(canRead(fixture),"You cannot access this fixture.");

        IMendixObject f=form(text(c,"Name"));put(f,"ContestKey",id(c));put(f,"FixtureKey",id(fixture));put(f,"RecordKey",id(c));put(f,"Revision",integer(c,"Revision"));copy(c,f,"Sequence","Outcome","Reason","RankingExcluded");put(f,"CanLead",isLeader(r)&&date(r,"SubmittedAt")==null);put(f,"CanOverride",override(r));

        put(f,"CanHome",date(r,"StartedAt")!=null||isLeader(r)||override(r)||userKey.equals(text(r,"HomeCaptainKey")));put(f,"CanAway",date(r,"StartedAt")!=null||isLeader(r)||override(r)||userKey.equals(text(r,"AwayCaptainKey")));

        put(f,"Info","Recorded score winner: "+text(c,"RecordedWinner")+" · Official outcome winner: "+text(c,"OfficialWinner")+" · "+text(c,"Reason"));

        put(f,"ScoresText",scoreText(c));

        for(IMendixObject s:sets(c))row(f,s,"Set","Set "+integer(s,"Sequence"),integer(s,"Home")+"–"+integer(s,"Away")+(bool(s,"HasTie")?" ("+integer(s,"HomeTie")+"–"+integer(s,"AwayTie")+")":"")+(bool(s,"IsMatchTie")?" · Match tie-break":""));

        for(IMendixObject l:lineups(c))if(canSeeLineup(r,l))row(f,l,"Lineup",text(l,"Side")+": "+text(l,"Player1Name")+(text(l,"Player2Name").isEmpty()?"":" / "+text(l,"Player2Name")),lineupWarnings(r,c,l));

        return f;

    }



    private IMendixObject lineupView(IMendixObject f,String side) throws Exception {

        IMendixObject c=required(f,"ContestKey","MatchContest"),r=ref(c,"MatchContest_Registration");IMendixObject l=first("MatchLineup","["+M+"MatchLineup_Contest="+id(c)+"][Side='"+side+"']");require(l!=null&&canSeeLineup(r,l),"The opponent lineup is private until fixture start.");

        IMendixObject out=form(side+" lineup · "+text(c,"Name"));put(out,"FixtureKey",id(ref(r,"MatchRegistration_Fixture")));put(out,"FederationKey",id(ref(ref(ref(r,"MatchRegistration_Fixture"),"CompetitionFixture_League"),"CompetitionLeague_Federation")));put(out,"RecordKey",id(l));put(out,"LineupKey",id(l));put(out,"ContestKey",id(c));put(out,"Side",side);put(out,"Revision",integer(l,"Revision"));copy(l,out,"Player1Name","Player2Name");put(out,"Info",lineupWarnings(r,c,l));

        IMendixObject p1=ref(l,"MatchLineup_Player1"),p2=ref(l,"MatchLineup_Player2");put(out,"Player1Key",p1==null?"":id(p1));put(out,"Player2Key",p2==null?"":id(p2));put(out,"CanEditLineup",canEditLineup(r,side));return out;

    }



    private IMendixObject roster(IMendixObject f) throws Exception {

        IMendixObject l=required(f,"LineupKey","MatchLineup"),c=ref(l,"MatchLineup_Contest"),r=ref(c,"MatchContest_Registration");require(canEditLineup(r,text(l,"Side")),"You cannot change this lineup.");

        IMendixObject team=team(r,text(l,"Side"));for(IMendixObject p:query("CompetitionPlayer","["+M+"CompetitionPlayer_Team="+id(team)+"]"))row(f,p,"Player",text(p,"MemberName"),"Registered team roster");return f;

    }



    private IMendixObject pickPlayer(IMendixObject f,int position) throws Exception {

        IMendixObject l=required(f,"LineupKey","MatchLineup"),c=ref(l,"MatchLineup_Contest"),r=ref(c,"MatchContest_Registration"),p=required(f,"SelectedKey","CompetitionPlayer");require(canEditLineup(r,text(l,"Side")),"You cannot change this lineup.");

        require(same(ref(p,"CompetitionPlayer_Team"),team(r,text(l,"Side"))),"Choose a player from this team's roster.");put(f,"Player"+position+"Key",id(p));put(f,"Player"+position+"Name",text(p,"MemberName"));return f;

    }



    private IMendixObject saveLineup(IMendixObject f) throws Exception {

        IMendixObject l=required(f,"LineupKey","MatchLineup"),c=ref(l,"MatchLineup_Contest"),r=ref(c,"MatchContest_Registration");require(canEditLineup(r,text(l,"Side")),"This lineup is locked or belongs to another team.");version(l,f);

        if(date(r,"SubmittedAt")!=null){require(override(r),"Only the federation may correct submitted participants.");requireReason(f);}

        else if(date(r,"StartedAt")!=null){require(bool(f,"NotStartedConfirmed"),"The official must confirm this contest has not started.");requireReason(f);require(text(c,"Outcome").equals("Pending")&&sets(c).isEmpty(),"A contest with a recorded result cannot have substitutions.");}

        IMendixObject p1=optional(f,"Player1Key","CompetitionPlayer"),p2=optional(f,"Player2Key","CompetitionPlayer");

        for(IMendixObject p:Arrays.asList(p1,p2))if(p!=null)require(same(ref(p,"CompetitionPlayer_Team"),team(r,text(l,"Side"))),"Choose a player from the team's registered roster.");

        String before=participants(l);link(l,"MatchLineup_Player1",p1);link(l,"MatchLineup_Player2",p2);put(l,"Player1Name",p1==null?"":text(p1,"MemberName"));put(l,"Player2Name",p2==null?"":text(p2,"MemberName"));put(l,"Warnings",lineupWarnings(r,c,l));touch(l);put(r,"WarningsAcknowledged",false);touch(r);

        audit(r,null,null,date(r,"StartedAt")==null?"Lineup saved":date(r,"SubmittedAt")==null?"Substitution recorded":"Federation participant correction",reasonOr(f,"Captain lineup entry"),before,participants(l),date(r,"StartedAt")==null?text(l,"Side"):"");return lineupView(f,text(l,"Side"));

    }



    /** Starters, reserves and substitutions per side; a side's sheet stays private until fixture start like its lineups. */
    private void teamSheetRows(IMendixObject f,IMendixObject r,boolean prefixSide) throws Exception {
        List<IMendixObject> substitutions=substitutions(r);
        for(String side:Arrays.asList("Home","Away")){
            if(!canSeeSide(r,side))continue;
            for(IMendixObject p:squad(r,side)){String position=text(p,"Position");row(f,p,(prefixSide?side:"")+position,text(p,"PlayerName"),squadStatus(p,substitutions));}
        }
        for(IMendixObject s:substitutions)if(canSeeSide(r,text(s,"Side")))row(f,s,"Substitution",integer(s,"Minute")+"' · "+text(s,"Side")+": "+text(s,"OnName")+" on for "+text(s,"OffName"),text(s,"RecordedBy")+" · "+text(s,"RecordedAt")+(text(s,"Reason").isBlank()?"":" · "+text(s,"Reason")));
    }

    private String squadStatus(IMendixObject p,List<IMendixObject> substitutions){
        List<String> status=new ArrayList<>();
        for(IMendixObject s:substitutions){if(same(ref0(s,"MatchSubstitution_On"),p))status.add("On "+integer(s,"Minute")+"' for "+text(s,"OffName"));if(same(ref0(s,"MatchSubstitution_Off"),p))status.add("Off "+integer(s,"Minute")+"' for "+text(s,"OnName"));}
        if(status.isEmpty())return text(p,"Position").equals("Starter")?"Started":"Not used";
        return String.join(" · ",status);
    }

    private IMendixObject squadView(IMendixObject r,String side) throws Exception {
        IMendixObject fixture=ref(r,"MatchRegistration_Fixture"),rule=ref(r,"MatchRegistration_Format");require(canRead(fixture),"You cannot access this fixture.");require(canSeeSide(r,side),"The opponent team sheet is private until fixture start.");
        require(rule!=null&&integer(rule,"MaxStarters")+integer(rule,"MaxReserves")>0,"This league's match format has no team sheet.");
        IMendixObject out=form(side+" team sheet · "+text(fixture,"HomeName")+" — "+text(fixture,"AwayName"));put(out,"RecordKey",id(r));put(out,"FixtureKey",id(fixture));put(out,"Side",side);put(out,"Revision",integer(r,"Revision"));
        put(out,"MaxStarters",integer(rule,"MaxStarters"));put(out,"MaxReserves",integer(rule,"MaxReserves"));put(out,"CanEditLineup",canEditSquad(r,side));put(out,"CanLead",canSubstitute(r));put(out,"CanOverride",override(r));
        List<IMendixObject> sheet=squad(r,side),substitutions=substitutions(r);
        long starters=sheet.stream().filter(p->text(p,"Position").equals("Starter")).count();
        put(out,"Info","Starters "+starters+" of "+integer(rule,"MaxStarters")+" · Reserves "+(sheet.size()-starters)+" of "+integer(rule,"MaxReserves")+". "
            +(date(r,"StartedAt")==null?"Select roster players and add them as starter or reserve. The sheet locks with the lineups.":"The sheet is locked. The official records substitutions: mark the player going off and the reserve coming on, then save with the minute.")
            +(date(r,"SubmittedAt")!=null?" Corrections after submission require federation override and a reason.":""));
        Map<String,IMendixObject> bySheet=new HashMap<>();for(IMendixObject p:sheet)bySheet.put(referenceKey(p,"MatchSquadPlayer_Player"),p);
        for(IMendixObject p:query("CompetitionPlayer","["+M+"CompetitionPlayer_Team="+id(team(r,side))+"]")){IMendixObject entry=bySheet.get(id(p));row(out,p,"Player",text(p,"MemberName"),entry==null?"Not on team sheet":text(entry,"Position")+" · "+squadStatus(entry,substitutions));}
        // Sheet rows carry the roster player's key so the substitution features can select them.
        for(IMendixObject entry:sheet){IMendixObject p=ref(entry,"MatchSquadPlayer_Player");if(p!=null)row(out,p,"Sheet",text(entry,"PlayerName"),text(entry,"Position")+" · "+squadStatus(entry,substitutions)+(onField(entry,substitutions)&&date(r,"StartedAt")!=null?" · On the field":""));}
        teamSheetRows(out,r,false);return out;
    }

    private IMendixObject saveSquadPlayer(IMendixObject f,String position) throws Exception {
        IMendixObject r=required(f,"RecordKey","MatchRegistration"),p=required(f,"SelectedKey","CompetitionPlayer"),rule=ref(r,"MatchRegistration_Format");String side=side(f);
        require(rule!=null,"This league's match format has no team sheet.");require(canEditSquad(r,side),"This team sheet is locked or belongs to another team.");if(date(r,"StartedAt")!=null)requireReason(f);
        require(same(ref(p,"CompetitionPlayer_Team"),team(r,side)),"Choose a player from this team's registered roster.");
        List<IMendixObject> sheet=squad(r,side);IMendixObject entry=null;for(IMendixObject o:sheet)if(referenceKey(o,"MatchSquadPlayer_Player").equals(id(p)))entry=o;
        if(entry!=null&&!text(entry,"Position").equals(position))require(substitutions(r).stream().noneMatch(s->same(ref0(s,"MatchSubstitution_On"),p)||same(ref0(s,"MatchSubstitution_Off"),p)),"A player involved in a substitution cannot change position.");
        long count=sheet.stream().filter(o->text(o,"Position").equals(position)&&!referenceKey(o,"MatchSquadPlayer_Player").equals(id(p))).count();
        int limit=integer(rule,position.equals("Starter")?"MaxStarters":"MaxReserves");require(count<limit,"This format allows "+limit+" "+(position.equals("Starter")?"starters":"reserves")+" per team.");
        String before=entry==null?"Not on team sheet":text(entry,"Position");
        if(entry==null){entry=make("MatchSquadPlayer");put(entry,"Side",side);link(entry,"MatchSquadPlayer_Registration",r);link(entry,"MatchSquadPlayer_Player",p);put(entry,"PlayerName",text(p,"MemberName"));}
        put(entry,"Position",position);put(entry,"Sequence",(int)count+1);save(entry);put(r,"WarningsAcknowledged",false);touch(r);
        audit(r,null,null,date(r,"StartedAt")==null?"Team sheet saved":"Team sheet correction",reasonOr(f,"Captain team sheet entry"),id(p)+" "+text(p,"MemberName")+": "+before,position,date(r,"StartedAt")==null?side:"");
        return squadView(r,side);
    }

    private IMendixObject removeSquadPlayer(IMendixObject f) throws Exception {
        IMendixObject r=required(f,"RecordKey","MatchRegistration"),p=required(f,"SelectedKey","CompetitionPlayer");String side=side(f);
        require(canEditSquad(r,side),"This team sheet is locked or belongs to another team.");if(date(r,"StartedAt")!=null)requireReason(f);
        IMendixObject entry=null;for(IMendixObject o:squad(r,side))if(referenceKey(o,"MatchSquadPlayer_Player").equals(id(p)))entry=o;require(entry!=null,"This player is not on the team sheet.");
        require(substitutions(r).stream().noneMatch(s->same(ref0(s,"MatchSubstitution_On"),p)||same(ref0(s,"MatchSubstitution_Off"),p)),"A player involved in a substitution cannot be removed.");
        audit(r,null,null,date(r,"StartedAt")==null?"Team sheet player removed":"Team sheet correction",reasonOr(f,"Captain team sheet entry"),id(p)+" "+text(p,"MemberName")+": "+text(entry,"Position"),"Not on team sheet",date(r,"StartedAt")==null?side:"");
        dirty.remove(entry);Core.delete(ctx,entry);put(r,"WarningsAcknowledged",false);touch(r);return squadView(r,side);
    }

    private IMendixObject pickSubstitution(IMendixObject f,int slot) throws Exception {
        IMendixObject r=required(f,"RecordKey","MatchRegistration"),p=required(f,"SelectedKey","CompetitionPlayer");String side=side(f);require(canSubstitute(r),"Only the official records substitutions during the fixture.");
        IMendixObject entry=null;for(IMendixObject o:squad(r,side))if(referenceKey(o,"MatchSquadPlayer_Player").equals(id(p)))entry=o;require(entry!=null,"Choose a player on this team sheet.");
        List<IMendixObject> substitutions=substitutions(r);
        if(slot==1)require(onField(entry,substitutions),"The player going off must be on the field.");
        else require(text(entry,"Position").equals("Reserve")&&substitutions.stream().noneMatch(s->same(ref0(s,"MatchSubstitution_On"),p)),"The player coming on must be an unused reserve.");
        put(f,"Player"+slot+"Key",id(p));put(f,"Player"+slot+"Name",text(p,"MemberName"));return f;
    }

    private IMendixObject saveSubstitution(IMendixObject f) throws Exception {
        IMendixObject r=required(f,"RecordKey","MatchRegistration"),off=required(f,"Player1Key","CompetitionPlayer"),on=required(f,"Player2Key","CompetitionPlayer");String side=side(f);
        require(canSubstitute(r),"Only the official records substitutions during the fixture.");if(date(r,"SubmittedAt")!=null)requireReason(f);
        int minute=integer(f,"Sequence");require(minute>=0&&minute<=200,"Enter the minute of the substitution (0-200).");
        List<IMendixObject> sheet=squad(r,side),substitutions=substitutions(r);IMendixObject offEntry=null,onEntry=null;
        for(IMendixObject o:sheet){if(referenceKey(o,"MatchSquadPlayer_Player").equals(id(off)))offEntry=o;if(referenceKey(o,"MatchSquadPlayer_Player").equals(id(on)))onEntry=o;}
        require(offEntry!=null&&onField(offEntry,substitutions),"The player going off must be on the field.");
        require(onEntry!=null&&text(onEntry,"Position").equals("Reserve")&&substitutions.stream().noneMatch(s->same(ref0(s,"MatchSubstitution_On"),on)),"The player coming on must be an unused reserve.");
        IMendixObject s=make("MatchSubstitution");put(s,"Side",side);put(s,"Minute",minute);put(s,"OffName",text(off,"MemberName"));put(s,"OnName",text(on,"MemberName"));put(s,"Reason",text(f,"Reason").length()>200?text(f,"Reason").substring(0,200):text(f,"Reason"));put(s,"RecordedBy",actor);put(s,"RecordedAt",new Date());
        link(s,"MatchSubstitution_Registration",r);link(s,"MatchSubstitution_Off",off);link(s,"MatchSubstitution_On",on);save(s);touch(r);
        audit(r,null,null,date(r,"SubmittedAt")==null?"Substitution recorded":"Federation substitution correction",reasonOr(f,"Official substitution"),side+" "+minute+"' off: "+id(off)+" "+text(off,"MemberName"),"on: "+id(on)+" "+text(on,"MemberName"),"");
        return squadView(r,side);
    }

    private boolean onField(IMendixObject entry,List<IMendixObject> substitutions) throws Exception {
        IMendixObject p=ref(entry,"MatchSquadPlayer_Player");
        boolean cameOn=substitutions.stream().anyMatch(s->same(ref0(s,"MatchSubstitution_On"),p)),wentOff=substitutions.stream().anyMatch(s->same(ref0(s,"MatchSubstitution_Off"),p));
        return !wentOff&&(text(entry,"Position").equals("Starter")||cameOn);
    }

    private String side(IMendixObject f){String side=text(f,"Side");require(side.equals("Home")||side.equals("Away"),"Reopen the team sheet.");return side;}

    private boolean canSeeSide(IMendixObject r,String side) throws Exception{return canRead(ref(r,"MatchRegistration_Fixture"))&&("SourceSnapshot".equals(text(r,"State"))||date(r,"StartedAt")!=null||independentLeader(r)||override(r)||userKey.equals(text(r,side+"CaptainKey")));}

    /** Before start the captain or official edits within the lineup lock; afterwards only federation corrections, with a reason. */
    private boolean canEditSquad(IMendixObject r,String side) throws Exception {if(date(r,"StartedAt")!=null)return override(r);return canEditLineup(r,side);}

    private boolean canSubstitute(IMendixObject r) throws Exception {return date(r,"StartedAt")!=null&&((isLeader(r)&&date(r,"SubmittedAt")==null)||override(r));}

    private List<IMendixObject> squad(IMendixObject r,String side) throws Exception {List<IMendixObject> list=query("MatchSquadPlayer","["+M+"MatchSquadPlayer_Registration="+id(r)+"][Side='"+side+"']");for(IMendixObject o:dirty)if(o.getType().equals(M+"MatchSquadPlayer")&&same(ref(o,"MatchSquadPlayer_Registration"),r)&&side.equals(text(o,"Side"))&&!list.contains(o))list.add(o);list.sort(Comparator.comparing((IMendixObject o)->text(o,"Position").equals("Starter")?0:1).thenComparingInt(o->integer(o,"Sequence")).thenComparing(o->text(o,"PlayerName")));return list;}

    private List<IMendixObject> substitutions(IMendixObject r) throws Exception {List<IMendixObject> list=query("MatchSubstitution","["+M+"MatchSubstitution_Registration="+id(r)+"]");for(IMendixObject o:dirty)if(o.getType().equals(M+"MatchSubstitution")&&same(ref(o,"MatchSubstitution_Registration"),r)&&!list.contains(o))list.add(o);list.sort(Comparator.comparing((IMendixObject o)->text(o,"Side")).thenComparingInt(o->integer(o,"Minute")));return list;}

    private IMendixObject ref0(IMendixObject o,String association){try{return ref(o,association);}catch(Exception e){throw new IllegalStateException(e);}}

    private IMendixObject setView(IMendixObject f,IMendixObject s) throws Exception {

        IMendixObject c=s==null?required(f,"ContestKey","MatchContest"):ref(s,"MatchSet_Contest"),r=ref(c,"MatchContest_Registration");require(canEnter(r),"Only the game leader may enter results before submission; afterward only the federation may correct them.");

        IMendixObject out=form("Set score · "+text(c,"Name"));put(out,"ContestKey",id(c));put(out,"Revision",integer(c,"Revision"));if(s!=null){put(out,"RecordKey",id(s));copy(s,out,"Sequence","Home","Away","HomeTie","AwayTie","HasTie","IsMatchTie");}else put(out,"Sequence",sets(c).size()+1);return out;

    }



    private IMendixObject saveSet(IMendixObject f) throws Exception {

        IMendixObject c=required(f,"ContestKey","MatchContest"),r=ref(c,"MatchContest_Registration");require(canEnter(r),"You cannot enter this result.");version(c,f);require(integer(f,"Sequence")>0,"Set order must be positive.");if(date(r,"SubmittedAt")!=null)requireReason(f);

        IMendixObject s=optional(f,"RecordKey","MatchSet");if(s!=null)require(same(ref(s,"MatchSet_Contest"),c),"Set belongs to another contest.");for(IMendixObject other:sets(c))require(same(other,s)||integer(other,"Sequence")!=integer(f,"Sequence"),"Set order must be unique.");

        String before=s==null?"":snapshot(s,"Sequence","Home","Away","HomeTie","AwayTie","HasTie","IsMatchTie");if(s==null){s=make("MatchSet");link(s,"MatchSet_Contest",c);}copy(f,s,"Sequence","Home","Away","HomeTie","AwayTie","HasTie","IsMatchTie");save(s);

        // Full score validation is atomic with outcome save/submission; drafts can be incomplete.

        require(integer(s,"Home")>=0&&integer(s,"Away")>=0&&integer(s,"HomeTie")>=0&&integer(s,"AwayTie")>=0,"Scores cannot be negative.");

        touch(c);touch(r);audit(r,null,null,"Set score saved",reasonOr(f,"Official score entry"),before,snapshot(s,"Sequence","Home","Away","HomeTie","AwayTie","HasTie","IsMatchTie"),"");if(date(r,"SubmittedAt")!=null)recalculate(r,true);return contestView(c);

    }



    private IMendixObject saveContest(IMendixObject f) throws Exception {

        IMendixObject c=required(f,"ContestKey","MatchContest"),r=ref(c,"MatchContest_Registration");require(canEnter(r),"You cannot enter this result.");version(c,f);

        String outcome=text(f,"Outcome");if(outcome.equals("Excluded")||(outcome.endsWith("Forfeit")&&!sets(c).isEmpty())||date(r,"SubmittedAt")!=null){require(override(r),"This change requires a federation ruling.");requireReason(f);}

        require(integer(f,"Sequence")>0,"Play order must be positive.");for(IMendixObject other:contests(r))require(same(other,c)||integer(other,"Sequence")!=integer(f,"Sequence"),"Play order must remain unique.");

        String before=snapshot(c,"Outcome","Sequence","OfficialWinner","Reason","RankingExcluded");if(integer(f,"Sequence")!=integer(c,"Sequence")){require(bool(f,"NotStartedConfirmed"),"Confirm that reordering concerns a contest that has not started.");requireReason(f);require(override(r)||(text(c,"Outcome").equals("Pending")&&sets(c).isEmpty()),"Only unstarted contests can be reordered by the game leader.");}

        if(bool(c,"RankingExcluded")!=bool(f,"RankingExcluded")){require(override(r),"Only the federation may exclude a contest from player ranking.");requireReason(f);}put(c,"RankingExcluded",bool(f,"RankingExcluded"));put(c,"Sequence",integer(f,"Sequence"));put(c,"Outcome",outcome);put(c,"Reason",text(f,"Reason"));applyResult(c,calculate(c));touch(c);touch(r);audit(r,null,null,"Contest outcome saved",reasonOr(f,"Official result entry"),before,snapshot(c,"Outcome","Sequence","RecordedWinner","OfficialWinner","Reason","RankingExcluded"),"");if(date(r,"SubmittedAt")!=null)recalculate(r,true);return contestView(c);

    }



    private String scoreText(IMendixObject c) throws Exception {List<String> lines=new ArrayList<>();for(IMendixObject s:sets(c))lines.add(integer(s,"Home")+"-"+integer(s,"Away")+(bool(s,"HasTie")?" ("+integer(s,"HomeTie")+"-"+integer(s,"AwayTie")+")":"")+(bool(s,"IsMatchTie")?" MT":""));return String.join("\n",lines);}

    /** Replace the entire score and outcome in one transaction, including federation corrections. */

    private IMendixObject saveScores(IMendixObject f) throws Exception {

        IMendixObject c=required(f,"ContestKey","MatchContest"),r=ref(c,"MatchContest_Registration");require(canEnter(r),"Only the appointed official or federation may enter scores.");version(c,f);

        String outcome=text(f,"Outcome");if(date(r,"SubmittedAt")!=null||outcome.equals("Excluded")||(outcome.endsWith("Forfeit")&&!text(f,"ScoresText").trim().isEmpty())){require(override(r),"A federation ruling is required.");requireReason(f);}

        String before=scoreText(c)+"; "+snapshot(c,"Outcome","RecordedWinner","OfficialWinner","Reason","RankingExcluded");

        List<MatchRules.SetScore> scores=new ArrayList<>();String raw=text(f,"ScoresText").trim();

        if(!raw.isEmpty())for(String line:raw.split("[\\r\\n;]+")){java.util.regex.Matcher m=java.util.regex.Pattern.compile("\\s*(\\d+)\\s*[-–:]\\s*(\\d+)(?:\\s*\\(\\s*(\\d+)\\s*[-–:]\\s*(\\d+)\\s*\\))?\\s*(MT)?\\s*",java.util.regex.Pattern.CASE_INSENSITIVE).matcher(line);require(m.matches(),"Enter one set per line, for example 21-19, 7-6 (7-4), or 10-8 MT.");scores.add(new MatchRules.SetScore(Integer.parseInt(m.group(1)),Integer.parseInt(m.group(2)),m.group(3)==null?0:Integer.parseInt(m.group(3)),m.group(4)==null?0:Integer.parseInt(m.group(4)),m.group(3)!=null,m.group(5)!=null));}

        MatchRules.ContestResult result=MatchRules.contest(contestRules(c),scores,outcome,text(f,"Reason"));

        replaceSets(c,scores);

        if(bool(c,"RankingExcluded")!=bool(f,"RankingExcluded")){require(override(r),"Only the federation may exclude a contest from player ranking.");requireReason(f);}put(c,"RankingExcluded",bool(f,"RankingExcluded"));put(c,"Outcome",outcome);put(c,"Reason",text(f,"Reason"));applyResult(c,result);touch(c);touch(r);audit(r,null,null,"Complete contest score saved",reasonOr(f,"Official result entry"),before,raw+"; "+snapshot(c,"Outcome","RecordedWinner","OfficialWinner","Reason","RankingExcluded"),"");

        if(date(r,"SubmittedAt")!=null)recalculate(r,true);return contestView(c);

    }



    private IMendixObject submit(IMendixObject f) throws Exception {

        IMendixObject r=required(f,"RecordKey","MatchRegistration");version(r,f);require(isLeader(r)&&date(r,"StartedAt")!=null&&date(r,"SubmittedAt")==null,"Only the appointed game leader can submit a started fixture once.");

        require(bool(f,"WarningsAcknowledged"),"Acknowledge the final participant and eligibility warnings.");put(r,"WarningAcknowledgment",warnings(r));put(r,"WarningsAcknowledged",true);recalculate(r,false);

        put(r,"SubmittedAt",new Date());MatchRules.Deadlines d=MatchRules.deadlines(rules(ref(r,"MatchRegistration_Format")),instant(ref(r,"MatchRegistration_Fixture"),"StartsAt"),instant(r,"StartedAt"),instant(r,"SubmittedAt"));put(r,"HomeDeadline",Date.from(d.home));put(r,"AwayDeadline",Date.from(d.away));put(r,"State","Provisional");

        for(String side:Arrays.asList("Home","Away"))if(userKey.equals(text(r,side+"CaptainKey")))approve(r,side,"Official submission by the same captain");

        touch(r);refreshAcceptance(r);audit(r,null,null,"Fixture submitted","Official submitted completed fixture", "",snapshot(r,"HomeScore","AwayScore","Winner","HomePoints","AwayPoints","HomeDeadline","AwayDeadline"),"");return fixture(ref(r,"MatchRegistration_Fixture"));

    }



    private void recalculate(IMendixObject r,boolean federation) throws Exception {

        CompetitionStandings.Policy policy=leaguePolicy(ref(ref(r,"MatchRegistration_Fixture"),"CompetitionFixture_League"));
        if(!policy.games()){
            require(bool(r,"HasFixtureScore")||!text(r,"Winner").isBlank(),"Enter the final fixture score first.");
            int h=integer(r,"HomeScore"),a=integer(r,"AwayScore");String winner=text(r,"WinnerOverride").isBlank()?(h>a?"Home":a>h?"Away":"Draw"):text(r,"WinnerOverride");
            require(Set.of("Home","Away","Draw").contains(winner),"Choose Home, Away or Draw for the federation winner override.");
            put(r,"Winner",winner);put(r,"HomePoints",policy.points(h,a,0,winner,"Home").add(decimal(r,"HomeAdjustment")));put(r,"AwayPoints",policy.points(a,h,0,winner,"Away").add(decimal(r,"AwayAdjustment")));
            if(federation){put(r,"FederationFinal",true);put(r,"Disputed",false);put(r,"State","FederationFinal");}save(r);return;
        }

        List<MatchRules.ContestResult> regular=new ArrayList<>(),deciders=new ArrayList<>();

        for(IMendixObject c:contests(r)){MatchRules.ContestResult result=calculate(c);applyResult(c,result);save(c);(bool(c,"IsDecider")?deciders:regular).add(result);}

        MatchRules.Format configured=rules(ref(r,"MatchRegistration_Format"));configured.winPoints=policy.win();configured.drawPoints=policy.draw();configured.lossPoints=policy.loss();
        MatchRules.FixtureResult result=MatchRules.fixture(configured,regular,deciders,decimal(r,"HomeAdjustment"),decimal(r,"AwayAdjustment"),text(r,"WinnerOverride"));put(r,"HomeScore",result.home);put(r,"AwayScore",result.away);put(r,"Winner",result.winner);put(r,"HomePoints",result.homePoints);put(r,"AwayPoints",result.awayPoints);
        if(policy.pointsPerGame()){int draws=(int)regular.stream().filter(c->c.officialWinner.equals("Draw")).count();put(r,"HomePoints",policy.points(result.home,result.away,draws,result.winner,"Home").add(decimal(r,"HomeAdjustment")));put(r,"AwayPoints",policy.points(result.away,result.home,draws,result.winner,"Away").add(decimal(r,"AwayAdjustment")));}

        if(federation){put(r,"FederationFinal",true);put(r,"Disputed",false);put(r,"State","FederationFinal");}save(r);

    }



    private IMendixObject accept(IMendixObject f,String side) throws Exception {

        IMendixObject r=required(f,"RecordKey","MatchRegistration");require(userKey.equals(text(r,side+"CaptainKey")),"Only this team's appointed captain can accept.");require(date(r,"SubmittedAt")!=null&&!bool(r,"FederationFinal")&&!bool(r,"Disputed"),"Only submitted, undisputed results can be accepted.");refreshAcceptance(r);if(!bool(r,side+"Accepted")){approve(r,side,"Captain acceptance");touch(r);audit(r,null,null,side+" accepted","Captain accepted result","Pending","Accepted","");}refreshAcceptance(r);return fixture(ref(r,"MatchRegistration_Fixture"));

    }



    private IMendixObject dispute(IMendixObject f,String side) throws Exception {

        IMendixObject r=required(f,"RecordKey","MatchRegistration");require(userKey.equals(text(r,side+"CaptainKey")),"Only this team's appointed captain can dispute.");require(date(r,"SubmittedAt")!=null&&!bool(r,"FederationFinal"),"A submitted result is required.");refreshAcceptance(r);require(!bool(r,side+"Accepted"),"This team's review period has closed.");requireReason(f);put(r,"Disputed",true);put(r,"DisputeReason",text(f,"Reason"));put(r,"State","Disputed");touch(r);audit(r,null,null,side+" disputed",text(f,"Reason"),"Provisional","Federation action required","");return fixture(ref(r,"MatchRegistration_Fixture"));

    }



    private IMendixObject finalizeRuling(IMendixObject f) throws Exception {

        IMendixObject r=required(f,"RecordKey","MatchRegistration");require(override(r),"Federation override permission is required.");version(r,f);requireReason(f);require(date(r,"StartedAt")!=null,"Start the fixture before applying an official ruling.");String before=snapshot(r,"Winner","WinnerOverride","HomePoints","AwayPoints","HomeAdjustment","AwayAdjustment");require(CompetitionStandings.wholePoints(decimal(f,"HomeAdjustment"))&&CompetitionStandings.wholePoints(decimal(f,"AwayAdjustment")),"Competition points adjustments must be whole numbers.");copy(f,r,"HomeAdjustment","AwayAdjustment","WinnerOverride");recalculate(r,true);if(date(r,"SubmittedAt")==null)put(r,"SubmittedAt",new Date());put(r,"Reason",text(f,"Reason"));touch(r);audit(r,null,null,"Federation ruling finalized",text(f,"Reason"),before,snapshot(r,"Winner","WinnerOverride","HomePoints","AwayPoints","HomeAdjustment","AwayAdjustment"),"");return fixture(ref(r,"MatchRegistration_Fixture"));

    }



    private void approve(IMendixObject r,String side,String method){put(r,side+"Accepted",true);put(r,side+"AcceptedAt",new Date());put(r,side+"Acceptance",method);save(r);}

    private void refreshAcceptance(IMendixObject r) throws Exception {

        if(date(r,"SubmittedAt")==null||bool(r,"FederationFinal")||bool(r,"Disputed"))return;

        for(String side:Arrays.asList("Home","Away"))if(!bool(r,side+"Accepted")&&date(r,side+"Deadline")!=null&&!new Date().before(date(r,side+"Deadline"))){approve(r,side,"Accepted by silence after configured deadline");touch(r);audit(r,null,null,side+" automatically accepted","Configured review deadline elapsed","Pending","Accepted by silence","");}

        if(bool(r,"HomeAccepted")&&bool(r,"AwayAccepted")){put(r,"State","Accepted");save(r);}

    }



    private IMendixObject eligibilityView(IMendixObject f) throws Exception {

        IMendixObject fed=required(f,"FederationKey","Federation");require(fedPermission(fed,"Manage"),"Federation management permission is required.");IMendixObject out=form("Federation eligibility warnings");put(out,"FederationKey",id(fed));put(out,"Discipline","Open");put(out,"Info","Effective facts trigger warnings only. Overlapping facts require federation review.");

        Set<String> seen=new HashSet<>();for(IMendixObject league:query("CompetitionLeague","["+M+"CompetitionLeague_Federation="+id(fed)+"]"))for(IMendixObject entry:query("LeagueEntry","["+M+"LeagueEntry_League="+id(league)+"]"))for(IMendixObject p:query("CompetitionPlayer","["+M+"CompetitionPlayer_Team="+id(ref(entry,"LeagueEntry_Team"))+"]"))if(seen.add(id(p)))row(out,p,"EligibilityPlayer",text(p,"MemberName"),text(league,"Name"));

        for(IMendixObject e:query("MatchEligibility","["+M+"MatchEligibility_Federation="+id(fed)+"]"))row(out,e,"Eligibility",text(e,"PlayerKey"),snapshot(e,"Suspended","Index","Discipline","StartsOn","EndsOn","Reason"));return out;

    }

    private IMendixObject saveEligibility(IMendixObject f) throws Exception {

        IMendixObject fed=required(f,"FederationKey","Federation"),player=required(f,"SelectedKey","CompetitionPlayer");require(fedPermission(fed,"Manage"),"Federation management permission is required.");requireReason(f);

        IMendixObject team=ref(player,"CompetitionPlayer_Team");boolean enrolled=false;for(IMendixObject entry:query("LeagueEntry","["+M+"LeagueEntry_Team="+id(team)+"]"))if(same(ref(ref(entry,"LeagueEntry_League"),"CompetitionLeague_Federation"),fed))enrolled=true;require(enrolled,"Player is not registered in a competition of this federation.");

        require(date(f,"StartsOn")!=null&&date(f,"EndsOn")!=null&&!date(f,"EndsOn").before(date(f,"StartsOn")),"Set a valid effective period.");

        IMendixObject e=make("MatchEligibility");copy(f,e,"Discipline","Suspended","Index","StartsOn","EndsOn","Reason");put(e,"PlayerKey",text(player,"PersonKey"));link(e,"MatchEligibility_Player",player);link(e,"MatchEligibility_Federation",fed);save(e);audit(null,fed,null,"Eligibility fact recorded",text(f,"Reason"),"",snapshot(e,"PlayerKey","Discipline","Suspended","Index","StartsOn","EndsOn"),"");return eligibilityView(f);

    }



    private String warnings(IMendixObject r) throws Exception {List<String> result=new ArrayList<>();for(IMendixObject c:contests(r))for(IMendixObject l:lineups(c)){String warning=lineupWarnings(r,c,l);if(!warning.isEmpty())result.add(text(c,"Name")+" · "+text(l,"Side")+": "+warning);}
        IMendixObject rule=ref(r,"MatchRegistration_Format");int starters=rule==null?0:integer(rule,"MaxStarters");
        if(starters>0)for(String side:Arrays.asList("Home","Away")){long named=squad(r,side).stream().filter(p->text(p,"Position").equals("Starter")).count();if(named<starters)result.add("Team sheet · "+side+": "+named+" of "+starters+" starters named");}
        return String.join("\n",result);}

    private String lineupWarnings(IMendixObject r,IMendixObject c,IMendixObject l) throws Exception {

        IMendixObject rule=ref(r,"MatchRegistration_Format"),fixture=ref(r,"MatchRegistration_Fixture"),league=ref(fixture,"CompetitionFixture_League"),fed=ref(league,"CompetitionLeague_Federation");Date played=date(fixture,"StartsAt");List<String> warnings=new ArrayList<>();

        IMendixObject a=ref(l,"MatchLineup_Player1"),b=ref(l,"MatchLineup_Player2");int count=(a==null?0:1)+(b==null?0:1);if(count!=integer(c,"PlayersPerSide"))warnings.add("Expected "+integer(c,"PlayersPerSide")+" participant(s), found "+count);if(a!=null&&same(a,b))warnings.add("The same player is listed twice");

        Set<String> categories=new HashSet<>();

        for(IMendixObject p:Arrays.asList(a,b))if(p!=null){String name=text(p,"MemberName");IMendixObject membership=ref(p,"CompetitionPlayer_Membership");if(bool(rule,"CheckMembership")){if(membership==null||!text(membership,"Status").equals("Active"))warnings.add(name+": membership is not active");else{IMendixObject season=ref(membership,"Membership_Season");if(played==null||season==null||date(season,"StartsOn")==null||date(season,"EndsOn")==null||played.before(date(season,"StartsOn"))||played.after(date(season,"EndsOn")))warnings.add(name+": membership season does not cover the fixture date");}}

            if(bool(rule,"CheckRegistration")){IMendixObject reg=membership==null?null:first("FederationRegistration","["+M+"FederationRegistration_Membership="+id(membership)+"]["+M+"FederationRegistration_Federation="+id(fed)+"]");if(reg==null||!text(reg,"Status").equals("Active")||played==null||date(reg,"SeasonStartsOn")==null||date(reg,"SeasonEndsOn")==null||played.before(date(reg,"SeasonStartsOn"))||played.after(date(reg,"SeasonEndsOn")))warnings.add(name+": active federation registration does not cover the fixture date");}

            List<IMendixObject> facts=query("MatchEligibility","["+M+"MatchEligibility_Federation="+id(fed)+"][PlayerKey='"+escape(text(p,"PersonKey"))+"']");facts.removeIf(e->played==null||date(e,"StartsOn")==null||date(e,"EndsOn")==null||played.before(date(e,"StartsOn"))||played.after(date(e,"EndsOn")));

            if(facts.size()!=1&&(bool(rule,"CheckSuspension")||bool(rule,"CheckDiscipline")||bool(rule,"CheckIndex")))warnings.add(name+": eligibility facts "+(facts.isEmpty()?"unavailable":"overlap; federation review needed"));

            if(facts.size()==1){IMendixObject fact=facts.get(0);if(bool(rule,"CheckSuspension")&&bool(fact,"Suspended"))warnings.add(name+": suspended for this date");String d=text(c,"Discipline"),pd=text(fact,"Discipline");categories.add(pd);if(bool(rule,"CheckDiscipline")&&!d.equals("Open")&&!d.equals("Mixed")&&!pd.equals(d))warnings.add(name+": discipline eligibility differs");BigDecimal index=decimal(fact,"Index");if(bool(rule,"CheckIndex")&&((decimal(rule,"MinIndex").signum()>0&&index.compareTo(decimal(rule,"MinIndex"))<0)||(decimal(rule,"MaxIndex").signum()>0&&index.compareTo(decimal(rule,"MaxIndex"))>0)))warnings.add(name+": classification/index is outside the configured limits");}

            if(bool(rule,"CheckAppearances")){int total=0,singles=0,doubles=0;for(IMendixObject other:contests(r))for(IMendixObject ol:lineups(other)){if(text(ol,"Side").equals(text(l,"Side"))&&(same(ref(ol,"MatchLineup_Player1"),p)||same(ref(ol,"MatchLineup_Player2"),p))){total++;if(integer(other,"PlayersPerSide")==1)singles++;if(integer(other,"PlayersPerSide")==2)doubles++;}}if((integer(rule,"MaxAppearances")>0&&total>integer(rule,"MaxAppearances"))||(integer(rule,"MaxSingles")>0&&singles>integer(rule,"MaxSingles"))||(integer(rule,"MaxDoubles")>0&&doubles>integer(rule,"MaxDoubles")))warnings.add(name+": appearance limit exceeded");}

        }

        if(bool(rule,"CheckDiscipline")&&text(c,"Discipline").equals("Mixed")&&count==2&&(!categories.contains("Men")||!categories.contains("Women")))warnings.add("Mixed-doubles eligibility requires federation review");

        if(bool(rule,"CheckIndex")&&decimal(rule,"MaxTeamIndex").signum()>0){Set<String> seen=new HashSet<>();BigDecimal total=BigDecimal.ZERO;boolean known=true;for(IMendixObject other:contests(r))for(IMendixObject ol:lineups(other))if(text(ol,"Side").equals(text(l,"Side")))for(IMendixObject p:Arrays.asList(ref(ol,"MatchLineup_Player1"),ref(ol,"MatchLineup_Player2")))if(p!=null&&seen.add(text(p,"PersonKey"))){List<IMendixObject> facts=query("MatchEligibility","["+M+"MatchEligibility_Federation="+id(fed)+"][PlayerKey='"+escape(text(p,"PersonKey"))+"']");facts.removeIf(e->played==null||date(e,"StartsOn")==null||date(e,"EndsOn")==null||played.before(date(e,"StartsOn"))||played.after(date(e,"EndsOn")));if(facts.size()!=1)known=false;else total=total.add(decimal(facts.get(0),"Index"));}if(!known)warnings.add("Team index cannot be verified");else if(total.compareTo(decimal(rule,"MaxTeamIndex"))>0)warnings.add("Team index exceeds the configured maximum");}

        return String.join("; ",warnings);

    }



    private MatchRules.ContestResult calculate(IMendixObject c) throws Exception {List<MatchRules.SetScore> scores=new ArrayList<>();int expected=1;for(IMendixObject s:sets(c)){require(integer(s,"Sequence")==expected++,"Set order must be consecutive from one.");scores.add(new MatchRules.SetScore(integer(s,"Home"),integer(s,"Away"),integer(s,"HomeTie"),integer(s,"AwayTie"),bool(s,"HasTie"),bool(s,"IsMatchTie")));}return MatchRules.contest(contestRules(c),scores,text(c,"Outcome"),text(c,"Reason"));}

    private void applyResult(IMendixObject c,MatchRules.ContestResult r){put(c,"RecordedWinner",r.recordedWinner);put(c,"OfficialWinner",r.officialWinner);put(c,"HomeSets",r.homeSets);put(c,"AwaySets",r.awaySets);put(c,"HomeTotal",r.homeTotal);put(c,"AwayTotal",r.awayTotal);put(c,"Excluded",r.excluded);put(c,"RankingEligible",r.rankingEligible&&!bool(c,"RankingExcluded")&&integer(c,"PlayersPerSide")>0);}

    private MatchRules.Format rules(IMendixObject o){MatchRules.Format f=new MatchRules.Format();if(o==null)return f;f.family=text(o,"Scoring");f.tiePolicy=text(o,"TiePolicy");f.approvalAnchor=text(o,"ApprovalAnchor");f.setsToWin=integer(o,"SetsToWin");f.target=integer(o,"Target");f.margin=integer(o,"Margin");f.cap=integer(o,"Cap");f.gamesToWin=integer(o,"GamesToWin");f.gamesMargin=integer(o,"GamesMargin");f.tieAt=integer(o,"TieAt");f.tieTarget=integer(o,"TieTarget");f.decidingTarget=integer(o,"DecidingTarget");f.homeHours=integer(o,"HomeHours");f.awayHours=integer(o,"AwayHours");f.minimumReviewHours=integer(o,"MinimumReviewHours");f.stopWhenDecided=bool(o,"StopWhenDecided");f.winPoints=decimal(o,"WinPoints");f.drawPoints=decimal(o,"DrawPoints");f.lossPoints=decimal(o,"LossPoints");return f;}



    private boolean canRead(IMendixObject fixture) throws Exception {if("Published".equals(text(fixture,"Status")))return true;IMendixObject r=registration(fixture,false);return fedPermission(ref(ref(fixture,"CompetitionFixture_League"),"CompetitionLeague_Federation"),"Read")||club(fixture,"Home","Read")||club(fixture,"Away","Read")||(r!=null&&isLeader(r));}

    private boolean fedPermission(IMendixObject fed,String op) throws Exception{return fed!=null&&Boolean.TRUE.equals(Core.microflowCall(M+"SUB_Competition_FederationPermission").withParam("Federation",fed).withParam("Operation",op).execute(caller));}

    private boolean club(IMendixObject fixture,String side,String op) throws Exception {IMendixObject club=ref(fixture,"CompetitionFixture_"+side+"Club");return club!=null&&Boolean.TRUE.equals(Core.microflowCall(M+"SUB_Competition_ClubPermission").withParam("Club",club).withParam("Operation",op).execute(caller));}

    private boolean manage(IMendixObject r) throws Exception{return fedPermission(ref(ref(ref(r,"MatchRegistration_Fixture"),"CompetitionFixture_League"),"CompetitionLeague_Federation"),"Manage");}

    private boolean override(IMendixObject r) throws Exception{return fedPermission(ref(ref(ref(r,"MatchRegistration_Fixture"),"CompetitionFixture_League"),"CompetitionLeague_Federation"),"Override");}

    private boolean isLeader(IMendixObject r){return userKey.equals(text(r,"LeaderKey"));}

    private boolean canEnter(IMendixObject r) throws Exception{return date(r,"StartedAt")!=null&&((isLeader(r)&&date(r,"SubmittedAt")==null)||override(r));}

    private boolean independentLeader(IMendixObject r){return isLeader(r)&&!userKey.equals(text(r,"HomeCaptainKey"))&&!userKey.equals(text(r,"AwayCaptainKey"));}

    private boolean canSeeLineup(IMendixObject r,IMendixObject l) throws Exception{return canRead(ref(r,"MatchRegistration_Fixture"))&&("SourceSnapshot".equals(text(r,"State"))||date(r,"StartedAt")!=null||independentLeader(r)||override(r)||userKey.equals(text(r,text(l,"Side")+"CaptainKey")));}

    private boolean canEditLineup(IMendixObject r,String side) throws Exception {if(date(r,"SubmittedAt")!=null)return override(r);if(date(r,"StartedAt")!=null)return isLeader(r);IMendixObject rule=ref(r,"MatchRegistration_Format"),fixture=ref(r,"MatchRegistration_Fixture");return (isLeader(r)||userKey.equals(text(r,side+"CaptainKey")))&&MatchRules.lineupEditable(text(rule,"LineupLock"),integer(rule,"LockMinutes"),instant(fixture,"StartsAt"),null,Instant.now());}

    private IMendixObject team(IMendixObject r,String side) throws Exception{return ref(ref(ref(r,"MatchRegistration_Fixture"),"CompetitionFixture_"+side),"LeagueEntry_Team");}

    private boolean used(IMendixObject rule) throws Exception{return first("CompetitionLeague","["+M+"CompetitionLeague_MatchFormat="+id(rule)+"]")!=null||first("MatchRegistration","["+M+"MatchRegistration_Format="+id(rule)+"]")!=null||first("MatchSlot","["+M+"MatchSlot_ScoringOverride="+id(rule)+"]")!=null;}

    private List<IMendixObject> slots(IMendixObject rule) throws Exception{List<IMendixObject> list=query("MatchSlot","["+M+"MatchSlot_Format="+id(rule)+"]");for(IMendixObject o:dirty)if(o.getType().equals(M+"MatchSlot")&&same(ref(o,"MatchSlot_Format"),rule)&&!list.contains(o))list.add(o);return sorted(list,"Sequence");}

    private List<IMendixObject> contests(IMendixObject r) throws Exception {List<IMendixObject> list=query("MatchContest","["+M+"MatchContest_Registration="+id(r)+"]");for(IMendixObject o:dirty)if(o.getType().equals(M+"MatchContest")&&same(ref(o,"MatchContest_Registration"),r)&&!list.contains(o))list.add(o);return sorted(list,"Sequence");}

    private List<IMendixObject> sets(IMendixObject c) throws Exception {List<IMendixObject> list=query("MatchSet","["+M+"MatchSet_Contest="+id(c)+"]");for(IMendixObject o:dirty)if(o.getType().equals(M+"MatchSet")&&same(ref(o,"MatchSet_Contest"),c)&&!list.contains(o))list.add(o);return sorted(list,"Sequence");}

    private List<IMendixObject> lineups(IMendixObject c) throws Exception {List<IMendixObject> list=query("MatchLineup","["+M+"MatchLineup_Contest="+id(c)+"]");for(IMendixObject o:dirty)if(o.getType().equals(M+"MatchLineup")&&same(ref(o,"MatchLineup_Contest"),c)&&!list.contains(o))list.add(o);return list;}

    private List<IMendixObject> sorted(List<IMendixObject> list,String field){list.sort(Comparator.comparingInt(o->integer(o,field)));return list;}

    private void audit(IMendixObject reg,IMendixObject fed,IMendixObject league,String action,String reason,String before,String after,String side) throws Exception {IMendixObject a=make("MatchAudit");put(a,"Action",action);put(a,"Actor",actor);put(a,"At",new Date());put(a,"Reason",reason);put(a,"BeforeValue",before);put(a,"AfterValue",after);put(a,"PrivateSide",side);link(a,"MatchAudit_Registration",reg);if(league==null&&reg!=null)league=ref(ref(reg,"MatchRegistration_Fixture"),"CompetitionFixture_League");if(fed==null&&league!=null)fed=ref(league,"CompetitionLeague_Federation");link(a,"MatchAudit_League",league);link(a,"MatchAudit_Federation",fed);save(a);}

    private IMendixObject form(String title){IMendixObject f=make("MatchForm");put(f,"Title",title);return f;}

    private IMendixObject row(IMendixObject f,IMendixObject record,String kind,String label,String detail){IMendixObject row=make("MatchRow");put(row,"RecordKey",id(record));put(row,"Kind",kind);put(row,"Label",label);put(row,"Detail",detail);link(row,"MatchRow_Form",f);return row;}

    private IMendixObject make(String type){return Core.instantiate(type.equals("MatchForm")||type.equals("MatchRow")?caller:ctx,M+type);}

    private void save(IMendixObject o){if(!dirty.contains(o))dirty.add(o);}

    private void touch(IMendixObject o){put(o,"Revision",integer(o,"Revision")+1);save(o);}

    private void version(IMendixObject o,IMendixObject f){require(integer(o,"Revision")==integer(f,"Revision"),"This record changed. Reopen it before saving.");}

    private IMendixObject required(IMendixObject f,String field,String type) throws Exception {IMendixObject o=optional(f,field,type);require(o!=null,"Select a valid "+type+".");return o;}

    private IMendixObject optional(IMendixObject f,String field,String type) throws Exception {return byKey(text(f,field),type);}

    private IMendixObject byKey(String key,String type) throws Exception {if(key.isEmpty())return null;require(key.matches("[0-9]+"),"Invalid record reference.");IMendixObject o=Core.retrieveId(ctx,Core.createMendixIdentifier(Long.parseLong(key)));require(o!=null&&o.getType().equals(M+type),"Record is missing or has the wrong type.");return o;}

    private IMendixObject ref(IMendixObject o,String association) throws Exception {if(o==null)return null;IMendixIdentifier id=o.getValue(ctx,M+association);return id==null?null:Core.retrieveId(ctx,id);}

    private List<IMendixObject> query(String type,String constraint) throws Exception{return new ArrayList<>(Core.createXPathQuery("//"+M+type+constraint).execute(ctx));}

    private IMendixObject first(String type,String constraint) throws Exception {List<IMendixObject> list=Core.createXPathQuery("//"+M+type+constraint).setAmount(1).execute(ctx);return list.isEmpty()?null:list.get(0);}

    private void link(IMendixObject o,String association,IMendixObject target){o.setValue(ctx,M+association,target==null?null:target.getId());}

    private void put(IMendixObject o,String field,Object value){o.setValue(ctx,field,value instanceof Integer ? ((Integer)value).longValue() : value);}

    private String text(IMendixObject o,String field){Object v=o==null?null:o.getValue(ctx,field);return v==null?"":v.toString();}

    private int integer(IMendixObject o,String field){Object v=o.getValue(ctx,field);return v==null?0:((Number)v).intValue();}

    private boolean bool(IMendixObject o,String field){return o!=null&&Boolean.TRUE.equals(o.getValue(ctx,field));}

    private BigDecimal decimal(IMendixObject o,String field){Object v=o.getValue(ctx,field);return v==null?BigDecimal.ZERO:(BigDecimal)v;}

    private Date date(IMendixObject o,String field){return o==null?null:o.getValue(ctx,field);}

    private Instant instant(IMendixObject o,String field){Date d=date(o,field);return d==null?null:d.toInstant();}

    private void copy(IMendixObject from,IMendixObject to,String... fields){for(String field:fields)to.setValue(ctx,field,from.getValue(ctx,field));}

    private String snapshot(IMendixObject o,String... fields){List<String> values=new ArrayList<>();for(String field:fields)values.add(field+"="+text(o,field));return String.join("; ",values);}

    private String participants(IMendixObject l) throws Exception {IMendixObject a=ref(l,"MatchLineup_Player1"),b=ref(l,"MatchLineup_Player2");return (a==null?"None":id(a)+" "+text(l,"Player1Name"))+" / "+(b==null?"None":id(b)+" "+text(l,"Player2Name"));}

    private static String id(IMendixObject o){return Long.toString(o.getId().toLong());}

    private static boolean same(IMendixObject a,IMendixObject b){return a!=null&&b!=null&&a.getId().equals(b.getId());}

    private static String escape(String value){return value.replace("'","''");}

    private void requireReason(IMendixObject f){require(!text(f,"Reason").trim().isEmpty(),"A comment is required for this change and will be logged.");}

    private String reasonOr(IMendixObject f,String fallback){return text(f,"Reason").trim().isEmpty()?fallback:text(f,"Reason");}

    private static void require(boolean condition,String message){if(!condition)throw new IllegalArgumentException(message);}

}
