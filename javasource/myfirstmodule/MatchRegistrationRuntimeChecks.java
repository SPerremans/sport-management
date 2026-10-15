package myfirstmodule;

import com.mendix.core.Core;
import com.mendix.systemwideinterfaces.core.*;
import java.util.*;

/** Private local persistence checks. No accounts, mail, payments or existing business records are changed. */
final class MatchRegistrationRuntimeChecks {
    private static final String M="MyFirstModule.";
    static String run(IContext caller) throws Exception {
        if(!caller.isSudo()||!unittesting.ConfigurationManager.isLocalDevelopment())throw new SecurityException("Private local runtime verification.");
        IContext ctx=Core.createSystemContext();List<IMendixIdentifier> created=new ArrayList<>();int checks=0;ctx.startTransaction();
        try {
            IMendixObject format=make(ctx,"MatchFormat",created);MatchRegistrationService.defaults(ctx,format);format.setValue(ctx,"Name","Fictional persistence verification");Core.commitWithoutEvents(ctx,format);
            require(((Number)format.getValue(ctx,"Target")).intValue()==21,"Numeric format fields");checks++;
            IMendixObject slot=make(ctx,"MatchSlot",created);slot.setValue(ctx,M+"MatchSlot_Format",format.getId());slot.setValue(ctx,"PlayersPerSide",2L);Core.commitWithoutEvents(ctx,slot);checks++;
            IMendixObject registration=make(ctx,"MatchRegistration",created);registration.setValue(ctx,M+"MatchRegistration_Format",format.getId());registration.setValue(ctx,"State","Started");Core.commitWithoutEvents(ctx,registration);
            for(String outcome:Arrays.asList("Pending","Played","HomeRetired","AwayRetired","HomeForfeit","AwayForfeit","NotPlayed","Excluded")){
                List<MatchRules.SetScore> scores=new ArrayList<>();
                if(!outcome.equals("Pending")&&!outcome.equals("NotPlayed")){scores.add(new MatchRules.SetScore(21,19));scores.add(outcome.endsWith("Retired")?new MatchRules.SetScore(7,11):new MatchRules.SetScore(21,18));}
                MatchRules.ContestResult result=MatchRules.contest(new MatchRules.Format(),scores,outcome,"Fictional runtime verification");
                IMendixObject contest=make(ctx,"MatchContest",created);contest.setValue(ctx,M+"MatchContest_Registration",registration.getId());contest.setValue(ctx,M+"MatchContest_Format",format.getId());contest.setValue(ctx,"Outcome",outcome);contest.setValue(ctx,"RecordedWinner",result.recordedWinner);contest.setValue(ctx,"OfficialWinner",result.officialWinner);contest.setValue(ctx,"Excluded",result.excluded);contest.setValue(ctx,"RankingEligible",result.rankingEligible);contest.setValue(ctx,"RankingExcluded",false);Core.commitWithoutEvents(ctx,contest);
                IMendixObject retrieved=Core.retrieveId(ctx,contest.getId());require(outcome.equals(retrieved.getValue(ctx,"Outcome")),"Outcome round trip");checks++;
                require(result.officialWinner.equals(retrieved.getValue(ctx,"OfficialWinner")),"Official winner round trip");checks++;
                int order=0;for(MatchRules.SetScore score:scores){IMendixObject set=make(ctx,"MatchSet",created);set.setValue(ctx,M+"MatchSet_Contest",contest.getId());set.setValue(ctx,"Sequence",(long)++order);set.setValue(ctx,"Home",(long)score.home);set.setValue(ctx,"Away",(long)score.away);Core.commitWithoutEvents(ctx,set);}
                require(Core.retrieveByPath(ctx,contest,M+"MatchSet_Contest").size()==scores.size(),"Stored set association");checks++;
                if(outcome.equals("HomeForfeit")){require(result.recordedWinner.equals("Home")&&result.officialWinner.equals("Away")&&!result.rankingEligible,"Forfeit retains contrary recorded score");checks++;}
                if(outcome.equals("Excluded")){require(result.officialWinner.isEmpty()&&!result.rankingEligible,"Exclusion removes both credits");checks++;}
                IMendixObject lineup=make(ctx,"MatchLineup",created);lineup.setValue(ctx,"Side","Home");lineup.setValue(ctx,"Player1Name","Fictional Player One");lineup.setValue(ctx,M+"MatchLineup_Contest",contest.getId());Core.commitWithoutEvents(ctx,lineup);
            }
            IMendixObject audit=make(ctx,"MatchAudit",created);audit.setValue(ctx,"Reason","Fictional runtime check");audit.setValue(ctx,M+"MatchAudit_Registration",registration.getId());Core.commitWithoutEvents(ctx,audit);
            IMendixObject eligibility=make(ctx,"MatchEligibility",created);eligibility.setValue(ctx,"Suspended",true);Core.commitWithoutEvents(ctx,eligibility);checks++;
            IMendixObject form=Core.instantiate(ctx,M+"MatchForm");String longText="verification ".repeat(100);form.setValue(ctx,"ScoresText",longText);IMendixObject row=Core.instantiate(ctx,M+"MatchRow");row.setValue(ctx,"Detail",longText);row.setValue(ctx,M+"MatchRow_Form",form.getId());require(Core.retrieveByPath(ctx,form,M+"MatchRow_Form").size()==1,"Nonpersistent row association");checks++;
        } finally {ctx.rollbackTransaction();}
        IContext after=Core.createSystemContext();for(IMendixIdentifier id:created)require(Core.retrieveId(after,id)==null,"Verification data survived rollback");checks++;
        return "PASS: "+checks+" native runtime persistence/outcome assertions; all eight private match types, typed scores, associations, contrary forfeit winner, exclusions, temporary rows and rollback. Role/UI scenarios and sample lifecycle are separate checks.";
    }
    private static IMendixObject make(IContext ctx,String type,List<IMendixIdentifier> created){IMendixObject o=Core.instantiate(ctx,M+type);created.add(o.getId());return o;}
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
}
