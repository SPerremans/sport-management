package myfirstmodule;

import com.mendix.core.Core;
import com.mendix.systemwideinterfaces.core.*;
import java.util.Objects;

/** A league has one stored sport; format changes cannot silently change it. */
public final class LeagueSports {
    private LeagueSports() {}
    public static String infer(IContext ctx, IMendixObject league) throws Exception {
        IMendixIdentifier formatId=league.getValue(ctx,"MyFirstModule.CompetitionLeague_MatchFormat");
        IMendixObject format=formatId==null?null:Core.retrieveId(ctx,formatId);
        String sport=format==null?"":Objects.toString(format.getValue(ctx,"Sport"),"").trim();
        if(!sport.isEmpty())return sport;
        IMendixIdentifier federationId=league.getValue(ctx,"MyFirstModule.CompetitionLeague_Federation");
        IMendixObject federation=federationId==null?null:Core.retrieveId(ctx,federationId);
        return federation==null?"":Objects.toString(federation.getValue(ctx,"Sport"),"").trim();
    }
    public static void initializeOrValidate(IContext ctx, IMendixObject league) throws Exception {
        String sport=Objects.toString(league.getValue(ctx,"Sport"),"").trim(), inferred=infer(ctx,league);
        if(sport.isEmpty()) {
            if(inferred.isEmpty())throw new IllegalArgumentException("A league must have one sport.");
            league.setValue(ctx,"Sport",inferred);
        } else if(!inferred.isEmpty()&&!sport.equalsIgnoreCase(inferred)) {
            throw new IllegalArgumentException("The match format must use the league's sport ("+sport+").");
        }
    }
}
