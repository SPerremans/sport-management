package myfirstmodule;

import com.mendix.core.Core;
import com.mendix.systemwideinterfaces.core.*;
import com.mendix.systemwideinterfaces.core.meta.IMetaPrimitive;
import java.util.*;

/** Stable String/Void Java-action boundary; all editable data stays in a scoped non-persistent form. */
public final class MatchRegistrationBridge {
    private static boolean expiryScheduled;
    private MatchRegistrationBridge() {}

    public static void dispatch(IContext context, String command) throws Exception {
        if (command != null && command.startsWith("Table|")) {
            TableLifecycleService.dispatch(context, command);
            return;
        }
        if ("Initialize".equals(command)) {
            if (!context.isSudo()) throw new SecurityException("Private startup action.");
            AdminUserView.initialize();
            synchronized (MatchRegistrationBridge.class) {
                if (!expiryScheduled) {
                    Core.scheduleAtFixedRate("MyFirstModule.SE_Match_AcceptanceExpiry",new Date(System.currentTimeMillis()+60000),1,java.util.concurrent.TimeUnit.MINUTES,"MatchAcceptanceExpiry","Accept undisputed submitted results after federation-configured deadlines");
                    Core.scheduleAtFixedRate("MyFirstModule.SE_TableRetention_Purge",new Date(System.currentTimeMillis()+300000),24,java.util.concurrent.TimeUnit.HOURS,"TableRetentionPurge","Delete eligible archived records only when their global and organisation policies explicitly enable it");
                    expiryScheduled=true;
                }
            }
            return;
        }
        if ("Expire".equals(command)) { if(!context.isSudo())throw new SecurityException("Private expiry action."); MatchRegistrationService.expireAcceptance(Core.createSystemContext()); return; }
        if ("VerifyRuntime".equals(command)) {
            if(!context.isSudo()||!unittesting.ConfigurationManager.isLocalDevelopment())throw new SecurityException("Private local developer verification.");
            String report;
            try {report=DevelopmentSamples.verifyMatchLifecycle(context);}
            catch(Exception e){report="FAIL: "+e.toString();Core.getLogger("MatchRegistration").error(report,e);}
            java.nio.file.Files.writeString(java.nio.file.Path.of(Core.getConfiguration().getBasePath().toString(),"match-runtime-checks.txt"),report+"\n");
            return;
        }
        if ("VerifyStorage".equals(command)) {
            String report;
            try {report=MatchRegistrationRuntimeChecks.run(context);}
            catch(Throwable e){report="FAIL: "+e.toString();Core.getLogger("MatchRegistration").error(report,e);}
            java.nio.file.Files.writeString(java.nio.file.Path.of(Core.getConfiguration().getBasePath().toString(),"match-storage-checks.txt"),report+"\n");return;
        }
        String[] parts = command == null ? new String[0] : command.split(":", 2);
        if (parts.length != 2 || !parts[1].matches("[0-9]+")) throw new IllegalArgumentException("Invalid match command.");
        IMendixObject target = Core.retrieveId(context, Core.createMendixIdentifier(Long.parseLong(parts[1])));
        if (target == null || !target.getType().equals("MyFirstModule.MatchForm")) throw new IllegalArgumentException("Reopen the match form.");
        if ("VerifyBridge".equals(parts[0])) {
            if (!context.isSudo() || !unittesting.ConfigurationManager.isLocalDevelopment()) throw new SecurityException("Private local development probe.");
            if (!"Match bridge probe".equals(target.getValue(context,"Title"))) throw new AssertionError("Non-persistent form cache lookup failed.");
            IMendixObject next = Core.instantiate(context,"MyFirstModule.MatchForm"); next.setValue(context,"Info","Bridge verified");
            IMendixObject row = Core.instantiate(context,"MyFirstModule.MatchRow"); row.setValue(context,"Label","Probe row");
            row.setValue(context,"MyFirstModule.MatchRow_Form",next.getId());
            replaceView(context,target,next);
            if (!"Bridge verified".equals(target.getValue(context,"Info"))) throw new AssertionError("Form copy failed.");
            List<IMendixObject> rows = Core.retrieveByPath(context,target,"MyFirstModule.MatchRow_Form");
            if (rows.size()!=1 || !"Probe row".equals(rows.get(0).getValue(context,"Label"))) throw new AssertionError("Non-persistent row transfer failed.");
            Core.getLogger("MatchRegistration").info("PASS: runtime non-persistent form lookup, view copy and associated row transfer through String/Void bridge.");
            java.nio.file.Files.writeString(java.nio.file.Path.of(Core.getConfiguration().getBasePath().toString(),"match-bridge-probe.txt"),"PASS: runtime non-persistent form lookup, view copy and associated row transfer through String/Void bridge.\n");
            return;
        }
        IMendixObject next = MatchRegistrationService.execute(context,target,parts[0]);
        replaceView(context,target,next);
    }

    private static void replaceView(IContext context, IMendixObject target, IMendixObject next) throws Exception {
        if(target.getId().equals(next.getId())) return;
        List<IMendixObject> oldRows = Core.retrieveByPath(context,target,"MyFirstModule.MatchRow_Form");
        List<IMendixObject> newRows = Core.retrieveByPath(context,next,"MyFirstModule.MatchRow_Form");
        // These are non-persistent view rows; detach them without deleting any business data.
        for(IMendixObject row:oldRows) row.setValue(context,"MyFirstModule.MatchRow_Form",null);
        for(IMetaPrimitive attribute:next.getMetaObject().getMetaPrimitives()) target.setValue(context,attribute.getName(),next.getValue(context,attribute.getName()));
        for(IMendixObject row:newRows) row.setValue(context,"MyFirstModule.MatchRow_Form",target.getId());
    }
}
