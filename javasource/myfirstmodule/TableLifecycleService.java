package myfirstmodule;

import com.mendix.core.Core;
import com.mendix.systemwideinterfaces.core.*;
import com.mendix.systemwideinterfaces.core.meta.*;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** Shared table operations. Client visibility is advisory; every request is checked here again. */
public final class TableLifecycleService {
    private static final String M = "MyFirstModule.";
    private static final Set<String> IMMUTABLE = Set.of("AccountAudit", "CompetitionAudit", "MatchAudit", "PaymentVerification", "InvoiceLine", "InvoiceMail", "FormResponse", "FederationRegistration");
    private static final Map<Long, String> FORM_BINDINGS = new java.util.concurrent.ConcurrentHashMap<>();
    private static final ThreadLocal<Boolean> PURGING = ThreadLocal.withInitial(()->false);
    private TableLifecycleService() {}

    public static void dispatch(IContext ctx, String command) throws Exception {
        String[] p = command.split("\\|", -1);
        if (p.length != 5 || !"Table".equals(p[0])) throw new IllegalArgumentException("Invalid table action.");
        String operation = p[1];
        if ("GuardCommit".equals(operation) || "GuardDelete".equals(operation)) {
            IContext elevated=ctx.createSudoClone();IMendixObject record=id(elevated,p[2],p[3]);
            if("GuardCommit".equals(operation)){require(PURGING.get()||lifecycle(elevated,record)==null,"Restore this archived record before editing it.");if(record.getType().equals(M+"CompetitionLeague"))LeagueSports.initializeOrValidate(elevated,record);}
            else {
                if(ctx.getSession()==null||ctx.getSession().isSystemSession())require(PURGING.get(),"Permanent deletion requires an authenticated user or the retention job.");
                else require(permission(elevated,ctx.getSession().createContext(),record,"Delete"),"You cannot permanently delete this record.");
                require(effective(elevated,record).allowDelete,"Permanent deletion is disabled by the retention settings.");checkReferences(elevated,record);
            }return;
        }
        if ("Purge".equals(operation)) {
            if (!ctx.isSudo() || ctx.getSession() != null && !ctx.getSession().isSystemSession()) throw new SecurityException("Private retention job.");
            purge(ctx); return;
        }
        if (ctx.getSession() == null || ctx.getSession().isSystemSession()) throw new SecurityException("Sign in to use table actions.");
        IContext user = ctx.getSession().createContext();
        ctx = ctx.createSudoClone();
        if ("GlobalPolicy".equals(operation)) { if (!admin(user)) throw new SecurityException("Administrator access required."); openPolicy(ctx, "*", "Global"); return; }
        if ("AuditPolicy".equals(operation)) { if (!admin(user)) throw new SecurityException("Administrator access required."); openPolicy(ctx, AccountAnonymiser.auditPolicyName(), "Global"); return; }
        if ("SavePolicy".equals(operation)) { savePolicy(ctx, user, id(ctx, p[2], M+"TableRetentionForm")); return; }
        if ("ContinueCreate".equals(operation)) {TableCreationService.continueCreate(ctx,user,id(ctx,p[2],M+"TableCreateForm"));return;}
        TableDefinitions.Definition table = TableDefinitions.get(p[3]);
        IMendixObject state = id(ctx, p[2], M+"TableSelection");
        if("Initialize".equals(operation)){state.setValue(ctx,"CanCreate",TableCreationService.canCreate(ctx,user,state,p[3]));return;}
        if("Bind".equals(operation)){bind(ctx,state,p[3],p[4]);return;}
        List<IMendixObject> rows = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (String value : p[4].split(",")) {
            if (value.isEmpty()) continue;
            long key = positive(value);
            if (!seen.add(key)) continue;
            IMendixObject row = Core.retrieveId(ctx, Core.createMendixIdentifier(key));
            if (row == null || !row.isInstanceOf(table.entity())) throw new IllegalArgumentException("The selection changed. Refresh the table.");
            if (!readable(user,row,table)) throw new SecurityException("You cannot read every selected row.");
            rows.add(row);
        }
        inspect(ctx,user,state,rows,table);
        inspectFeatures(ctx,user,state,rows,p[3]);
        if ("Inspect".equals(operation)) return;
        if("Create".equals(operation)){TableCreationService.start(ctx,user,state,p[3],rows);return;}
        if (rows.isEmpty()) { message(ctx,"Select one or more rows."); return; }
        TableLegacyDefinitions.Action legacy=TableLegacyDefinitions.get(p[3],operation);
        if(legacy!=null&&Set.of("Deactivate","Activate","Edit").contains(operation)){
            String flag=operation.equals("Edit")?"CanEdit":operation.equals("Activate")?"CanActivate":"CanDeactivate";
            require(Boolean.TRUE.equals(state.getValue(ctx,flag)),"You cannot change every selected record.");
            Map<String,Object> vars=featureVariables(ctx,state,rows.get(0),p[3],new TableFeatureDefinitions.Feature("","",Map.of(),"true",new TableFeatureDefinitions.Context[]{},"",new String[]{}));vars.put("selection",rows);vars.put("state",state);
            Core.microflowCall(legacy.flow()).withParams(arguments(ctx,vars,legacy.arguments())).execute(ctx);
            inspect(ctx,user,state,rows,table);inspectFeatures(ctx,user,state,rows,p[3]);return;
        }
        if(operation.matches("Feature[1-3]")){
            int slot=Integer.parseInt(operation.substring(7))-1;
            require(rows.size()==1&&Boolean.TRUE.equals(state.getValue(ctx,"Feature"+(slot+1))),"You cannot perform this action on the selected record.");
            TableFeatureDefinitions.Feature feature=TableFeatureDefinitions.get(p[3]).features()[slot];
            Map<String,Object> variables=featureVariables(ctx,state,rows.get(0),p[3],feature);
            Map<String,Object> arguments=arguments(ctx,variables,feature.arguments());
            Core.microflowCall(feature.flow()).withParams(arguments).execute(ctx);return;
        }
        switch (operation) {
            case "View":
                require(rows.size()==1,"Select exactly one row to view.");
                IMendixObject view=Core.instantiate(ctx,M+"TableRecordView");
                view.setValue(ctx,"Title",table.title());
                StringBuilder details=new StringBuilder();
                for(TableDefinitions.Field field:table.fields()) details.append(field.title()).append(": ").append(value(user,rows.get(0),field.path())).append("\n\n");
                boolean archived=lifecycle(ctx,target(ctx,rows.get(0)))!=null;
                view.setValue(ctx,"Archived",archived);
                view.setValue(ctx,"Details",details.append("Archived: ").append(archived?"Yes":"No").toString());
                Core.microflowCall(M+"ACT_TableRecord_View").withParam("Record",view).execute(ctx); return;
            case "Export": export(ctx,user,table,rows); return;
            case "Edit":
                require(rows.size()==1,"Select exactly one row to edit.");
                require(Boolean.TRUE.equals(state.getValue(ctx,"CanEdit")),"You cannot edit the selected record.");
                Core.microflowCall(table.editFlow()).withParam("Item",rows.get(0)).execute(ctx); return;
            case "Policy":
                require(rows.size()==1 && Boolean.TRUE.equals(state.getValue(ctx,"CanConfigure")),"Select one record in an organisation you administer.");
                openPolicy(ctx,target(ctx,rows.get(0)).getType(),configurationScope(ctx,user,target(ctx,rows.get(0))));return;
            case "Delete": case "Archive": case "Restore": case "Deactivate": case "Activate":
                String flag=Map.of("Delete","CanDelete","Archive","CanArchive","Restore","CanRestore","Deactivate","CanDeactivate","Activate","CanActivate").get(operation);
                require(Boolean.TRUE.equals(state.getValue(ctx,flag)),"You do not have permission for this action on every selected record.");
                List<IMendixObject> targets=new ArrayList<>();for(IMendixObject row:rows)targets.add(target(ctx,row));
                // Preflight the complete selection before changing any record.
                if("Delete".equals(operation))try{for(IMendixObject row:targets)checkDelete(ctx,user,row);}catch(IllegalArgumentException blocked){message(ctx,blocked.getMessage());return;}
                if("Delete".equals(operation)){
                    if(!Core.delete(ctx,targets))throw new IllegalStateException("The selected records could not be deleted.");
                    for(IMendixObject row:targets){IMendixObject mark=lifecycle(ctx,row);if(mark!=null)Core.delete(ctx,mark);}
                    rows.clear();
                } else if("Archive".equals(operation))for(IMendixObject row:targets)archive(ctx,row,user.getSession().getUserName());
                else if("Restore".equals(operation))for(IMendixObject row:targets){IMendixObject mark=lifecycle(ctx,row);if(mark!=null)Core.delete(ctx,mark);}
                else {for(IMendixObject row:targets)row.setValue(ctx,activeMember(row),"Activate".equals(operation));Core.commit(ctx,targets);}
                inspect(ctx,user,state,rows,table);
                message(ctx, switch(operation){case "Delete"->"The selected records were permanently deleted.";case "Archive"->"The selected records were archived. Archived records remain available for history and can be restored.";case "Restore"->"The selected records were restored. Inactive records stay inactive until activated.";case "Activate"->"The selected records were activated.";default->"The selected records were deactivated.";});return;
            default:throw new IllegalArgumentException("Unknown table action.");
        }
    }

    private static void inspect(IContext c,IContext user,IMendixObject state,List<IMendixObject> rows,TableDefinitions.Definition table)throws Exception{
        boolean edit=!rows.isEmpty()&&!table.editFlow().isEmpty(), del=!rows.isEmpty(), arc=del, restore=del, deactivate=del, activate=del, configure=rows.size()==1;
        long active=0,inactive=0;
        for(IMendixObject projection:rows){IMendixObject row=target(c,projection);boolean mutable=mutable(row), update=mutable&&permission(c,user,row,"Update"), delete=mutable&&permission(c,user,row,"Delete");
            boolean archived=lifecycle(c,row)!=null;Policy policy=effective(c,row);
            // Accounts are archived and later anonymised, never permanently deleted; an anonymised account cannot be restored.
            boolean account=row.isInstanceOf("System.User");
            edit&=update&&!archived;del&=delete&&policy.allowDelete&&!account;arc&=delete&&!archived;restore&=delete&&archived&&!(account&&AccountAnonymiser.isAnonymised(c,row));
            String member=activeMember(row);boolean enabled=member!=null&&Boolean.TRUE.equals(row.getValue(c,member));
            boolean countActive=row.hasMember("IsPublished")?Boolean.TRUE.equals(row.getValue(c,"IsPublished")):enabled;
            if(countActive)active++;else inactive++;deactivate&=update&&member!=null&&enabled&&!archived;
            activate&=update&&member!=null&&!enabled&&!archived;
            if(row.isInstanceOf("System.User")&&(row.getId().equals(user.getSession().getUserId())||Set.of("Admin","MxAdmin").contains(Objects.toString(row.getValue(c,"Name"),"")))){deactivate=false;activate=false;}
            configure&=!configurationScope(c,user,row).isEmpty();
        }
        state.setValue(c,"SelectedCount",(long)rows.size());state.setValue(c,"ActiveCount",active);state.setValue(c,"InactiveCount",inactive);
        state.setValue(c,"CanView",rows.size()==1);state.setValue(c,"CanEdit",edit);state.setValue(c,"CanDelete",del);
        state.setValue(c,"CanArchive",arc);state.setValue(c,"CanRestore",restore);state.setValue(c,"CanDeactivate",deactivate);state.setValue(c,"CanConfigure",configure);state.setValue(c,"Message","");
        state.setValue(c,"CanActivate",activate);
        String tableKey=TableCreationDefinitions.key(table);if(tableKey!=null)state.setValue(c,"CanCreate",TableCreationService.canCreate(c,user,state,tableKey));
    }
    private static void bind(IContext c,IMendixObject state,String tableKey,String bindings)throws Exception{
        TableLegacyDefinitions.prepare();
        Map<String,String> expected=TableFeatureDefinitions.get(tableKey).contexts();
        Set<String> found=new HashSet<>();
        for(String pair:bindings.split(";")){if(pair.isEmpty())continue;String[] bits=pair.split("=",2);require(bits.length==2&&expected.containsKey(bits[0]),"Invalid table context.");id(c,bits[1],expected.get(bits[0]));found.add(bits[0]);}
        require(found.equals(expected.keySet()),"Reopen the table to reload its context.");state.setValue(c,"Bindings",tableKey+"|"+bindings);
    }
    private static Map<String,Object> featureVariables(IContext c,IMendixObject state,IMendixObject row,String tableKey,TableFeatureDefinitions.Feature feature)throws Exception{
        Map<String,Object> vars=new LinkedHashMap<>();String encoded=state.getValue(c,"Bindings");
        if(encoded!=null&&encoded.startsWith(tableKey+"|"))for(String pair:encoded.substring(tableKey.length()+1).split(";")){if(pair.isEmpty())continue;String[] b=pair.split("=",2);vars.put(b[0],id(c,b[1],TableFeatureDefinitions.get(tableKey).contexts().get(b[0])));}
        vars.put("selected",row);vars.put("currentObject",row);
        for(TableFeatureDefinitions.Context nested:feature.nested())vars.put(nested.name(),Core.microflowCall(nested.flow()).withParams(arguments(c,vars,nested.arguments())).execute(c));
        return vars;
    }
    private static Map<String,Object> arguments(IContext c,Map<String,Object> vars,Map<String,String> expressions)throws Exception{
        Map<String,Object> args=new LinkedHashMap<>();for(var p:expressions.entrySet())args.put(p.getKey(),Core.evaluateExpression(c,vars,p.getValue()));return args;
    }
    private static void inspectFeatures(IContext c,IContext user,IMendixObject state,List<IMendixObject> rows,String tableKey)throws Exception{
        TableFeatureDefinitions.Feature[] features=TableFeatureDefinitions.get(tableKey).features();
        for(int i=0;i<3;i++){
            boolean allowed=false;
            if(rows.size()==1&&i<features.length){TableFeatureDefinitions.Feature f=features[i];try{
                Map<String,Object> variables=featureVariables(c,state,rows.get(0),tableKey,f);
                allowed=(f.roles().length==0||Arrays.stream(f.roles()).anyMatch(user.getSession().getUserRolesNames()::contains))&&Boolean.TRUE.equals(Core.evaluateExpression(c,variables,f.condition()));
                if(allowed&&!f.permission().isEmpty()){
                    if("CreateCompetition".equals(f.permission()))allowed=check(c,"SUB_Competition_ClubPermission",Map.of("Club",rows.get(0),"Operation","Create"));
                    else if("UpdateCompetition".equals(f.permission()))allowed=check(c,"SUB_Competition_ClubPermission",Map.of("Club",rows.get(0),"Operation","Update"));
                    else if("Allocate".equals(f.permission())||"FederationAccess".equals(f.permission())){
                        List<IMendixObject> federations=scopes(c,target(c,rows.get(0)),"Federation");allowed=!federations.isEmpty();
                        for(IMendixObject federation:federations)allowed&=check(c,f.permission().equals("Allocate")?"SUB_Competition_FederationPermission":"SUB_Federation_HasAccess",f.permission().equals("Allocate")?Map.of("Federation",federation,"Operation","Allocate"):Map.of("Federation",federation));
                    }
                    else allowed=permission(c,user,target(c,rows.get(0)),f.permission());
                }
            }catch(Exception ex){Core.getLogger("TableActions").debug("Feature unavailable: "+tableKey+" / "+f.label()+": "+ex.getMessage());}}
            state.setValue(c,"Feature"+(i+1),allowed);
        }
    }
    private static boolean mutable(IMendixObject o){return o.getMetaObject().isPersistable() && (o.getType().startsWith(M)||o.isInstanceOf("UserManagement.Person"))&&!IMMUTABLE.contains(local(o));}
    private static boolean readable(IContext u,IMendixObject row,TableDefinitions.Definition table)throws Exception{
        if(row.getMetaObject().isPersistable()&&Core.createXPathQuery("//"+row.getType()+"[id=$record]").setVariable("record",row.getId()).setAmount(1).execute(u).isEmpty())return false;
        for(TableDefinitions.Field f:table.fields()){String name=f.path();if(!name.contains("/")&&row.hasMember(name)&&row.getMember(name).hasReadAccess(u))return true;}
        return false;
    }
    static boolean admin(IContext c){return c.getSession()!=null&&c.getSession().getUserRolesNames().contains("Administrator");}
    private static String local(IMendixObject o){return o.getType().substring(o.getType().lastIndexOf('.')+1);}
    private static boolean permission(IContext c,IContext user,IMendixObject o,String operation)throws Exception{
        if(!mutable(o))return false;
        if(o.isInstanceOf("System.User")){
            if(!admin(user))return false;
            String name=o.getValue(c,"Name");
            if(operation.equals("Delete")&&(o.getId().equals(user.getSession().getUserId())||"Admin".equals(name)||"MxAdmin".equals(name)))return false;
            return true;
        }
        if(admin(user))return true;
        String type=local(o);
        List<IMendixObject> clubs=scopes(c,o,"Club"), federations=scopes(c,o,"Federation");
        if(type.startsWith("Competition")||Set.of("LeagueEntry","FixtureChange","VenueAvailability").contains(type)){
            if(Set.of("CompetitionLeague","CompetitionPoule","CompetitionFixture","FixtureChange").contains(type)){
                if(federations.isEmpty())return false;
                for(IMendixObject f:federations)if(!check(c,"SUB_Competition_FederationPermission",Map.of("Federation",f,"Operation","Manage")))return false;return true;
            }
            if(clubs.isEmpty())return false;for(IMendixObject club:clubs)if(!check(c,"SUB_Competition_ClubPermission",Map.of("Club",club,"Operation",operation)))return false;return true;
        }
        if(!federations.isEmpty()&&clubs.isEmpty()){
            for(IMendixObject f:federations)if(!check(c,"SUB_Federation_HasAccess",Map.of("Federation",f)))return false;return true;
        }
        if(clubs.isEmpty())return false;
        String resource=switch(type){case "Membership","PlayerListRule"->"Members";case "RegistrationRequest"->"Registrations";case "MembershipType"->"MembershipTypes";case "Season","SeasonFee"->"Seasons";case "FormField"->"Forms";case "ClubEvent"->"Events";case "NewsItem"->"News";case "Invoice"->"Invoices";default->"Settings";};
        if(type.equals("ClubRole")||type.equals("ClubStaff"))for(IMendixObject club:clubs)if(!check(c,"SUB_Club_AdministratorOrOwner",Map.of("Club",club)))return false;
        for(IMendixObject club:clubs)if(!check(c,"SUB_Club_HasPermission",Map.of("Club",club,"Resource",resource,"Operation",operation)))return false;return true;
    }
    static boolean check(IContext c,String flow,Map<String,Object> params)throws Exception{return Boolean.TRUE.equals(Core.microflowCall(M+flow).withParams(params).execute(c));}
    private static IMendixObject target(IContext c,IMendixObject o)throws Exception{
        if(o.getType().equals(M+"AdminAccountOverview")){Object key=o.getValue(c,M+"AdminAccountOverview_Account");if(key instanceof IMendixIdentifier i){IMendixObject person=Core.retrieveId(c,i);if(person!=null)return person;}}
        return o;
    }
    private static String activeMember(IMendixObject o){if(o.hasMember("IsActive"))return "IsActive";if(o.isInstanceOf("System.User"))return "Active";return null;}
    static List<IMendixObject> scopes(IContext c,IMendixObject row,String kind)throws Exception{
        LinkedHashMap<Long,IMendixObject> found=new LinkedHashMap<>();scopes(c,row,kind,found,new HashSet<>(),0);return new ArrayList<>(found.values());
    }
    private static void scopes(IContext c,IMendixObject row,String kind,Map<Long,IMendixObject> found,Set<Long> seen,int depth)throws Exception{
        if(row==null||depth>4||!seen.add(row.getId().toLong()))return;
        if(row.getType().equals(M+kind)){found.put(row.getId().toLong(),row);return;}
        for(IMetaAssociation a:row.getMetaObject().getMetaAssociationsParent()){
            if(!a.getName().startsWith(M)||!row.hasMember(a.getName()))continue;
            String child=a.getChild().getName();
            if(child.equals(M+kind)||Set.of(M+"Season",M+"MembershipType",M+"CompetitionTeam",M+"CompetitionLeague",M+"CompetitionVenue",M+"CompetitionFixture",M+"FederationType",M+"RegistrationRequest",M+"ClubRole").contains(child)){
                for(IMendixObject next:Core.retrieveByPath(c,row,a.getName()))scopes(c,next,kind,found,seen,depth+1);
            }
        }
    }
    private static String configurationScope(IContext c,IContext user,IMendixObject o)throws Exception{
        for(String kind:List.of("Club","Federation"))for(IMendixObject scope:scopes(c,o,kind))if(admin(user)||check(c,kind.equals("Club")?"SUB_Club_AdministratorOrOwner":"SUB_Federation_HasAccess",Map.of(kind,scope)))return kind+":"+scope.getId().toLong();
        return admin(user)?"Global":"";
    }
    record Policy(int days,boolean forever,boolean allowDelete,boolean autoPurge){}
    static Policy combine(Policy base,Policy override){return new Policy(Math.max(base.days,override.days),base.forever||override.forever,base.allowDelete&&override.allowDelete,base.autoPurge&&override.autoPurge);}
    private static Policy basePolicy(IContext c,String entity)throws Exception{
        // Audit trail retention is independent of the table defaults: 10 years unless configured.
        if(entity.equals(AccountAnonymiser.auditPolicyName())){IMendixObject rule=policy(c,entity,"Global");return rule!=null?policyValue(c,rule):new Policy(AccountAnonymiser.defaultAuditDays(),false,true,true);}
        Policy p=new Policy(30,false,true,false);
        for(String e:List.of("*",entity)){IMendixObject rule=policy(c,e,"Global");if(rule!=null)p=policyValue(c,rule);}
        return p;
    }
    private static Policy effective(IContext c,IMendixObject row)throws Exception{
        Policy p=basePolicy(c,row.getType());
        for(String kind:List.of("Club","Federation"))for(IMendixObject scope:scopes(c,row,kind))for(String e:List.of("*",row.getType())){
            IMendixObject rule=policy(c,e,kind+":"+scope.getId().toLong());if(rule!=null)p=combine(p,policyValue(c,rule));
        }return p;
    }
    private static Policy policyValue(IContext c,IMendixObject o){return new Policy(((Number)o.getValue(c,"RetentionDays")).intValue(),Boolean.TRUE.equals(o.getValue(c,"KeepForever")),Boolean.TRUE.equals(o.getValue(c,"AllowPermanentDelete")),Boolean.TRUE.equals(o.getValue(c,"AutoPurge")));}
    private static IMendixObject policy(IContext c,String entity,String scope)throws Exception{return first(c,M+"TableRetentionPolicy[PolicyKey='"+entity+"|"+scope+"']");}
    private static IMendixObject lifecycle(IContext c,IMendixObject row)throws Exception{return row.getMetaObject().isPersistable()?first(c,M+"RecordLifecycle[RecordKey='"+row.getId().toLong()+"']"):null;}
    static boolean archived(IContext c,IMendixObject row)throws Exception{return lifecycle(c,row)!=null;}
    private static IMendixObject first(IContext c,String query)throws Exception{List<IMendixObject> list=Core.createXPathQuery("//"+query).setAmount(1).execute(c);return list.isEmpty()?null:list.get(0);}
    private static void openPolicy(IContext c,String entity,String scope)throws Exception{
        require(!scope.isEmpty(),"You cannot configure this organisation.");IMendixObject f=Core.instantiate(c,M+"TableRetentionForm");IMendixObject saved=policy(c,entity,scope);Policy p=saved==null?basePolicy(c,entity):policyValue(c,saved);
        String displayEntity=entity.equals("*")?"All tables":entity.substring(entity.lastIndexOf('.')+1).replaceAll("([a-z])([A-Z])","$1 $2");
        String displayScope="Global defaults";
        if(!scope.equals("Global")){String[] bits=scope.split(":");IMendixObject org=id(c,bits[1],M+bits[0]);displayScope=Objects.toString(org.getValue(c,"Name"),bits[0]);}
        f.setValue(c,"EntityName",entity);f.setValue(c,"ScopeKey",scope);f.setValue(c,"ScopeLabel",displayEntity+" · "+displayScope);
        f.setValue(c,"RetentionDays",(long)p.days);f.setValue(c,"KeepForever",p.forever);f.setValue(c,"AllowPermanentDelete",p.allowDelete);f.setValue(c,"AutoPurge",p.autoPurge);
        FORM_BINDINGS.put(f.getId().toLong(),c.getSession().getUserId().toLong()+"|"+entity+"|"+scope);
        Core.microflowCall(M+"ACT_TableRetention_View").withParam("Form",f).execute(c);
    }
    private static void savePolicy(IContext c,IContext user,IMendixObject f)throws Exception{
        String entity=f.getValue(c,"EntityName"),scope=f.getValue(c,"ScopeKey");
        require(Objects.equals(FORM_BINDINGS.get(f.getId().toLong()),user.getSession().getUserId().toLong()+"|"+entity+"|"+scope),"Reopen the retention settings.");
        if(scope.equals("Global"))require(admin(user),"Administrator access required.");else{
            String[] bits=scope.split(":");require(bits.length==2&&Set.of("Club","Federation").contains(bits[0]),"Invalid organisation.");
            IMendixObject org=id(c,bits[1],M+bits[0]);require(admin(user)||check(c,bits[0].equals("Club")?"SUB_Club_AdministratorOrOwner":"SUB_Federation_HasAccess",Map.of(bits[0],org)),"You cannot configure this organisation.");
        }
        Policy p=policyValue(c,f);require(p.days>=1&&p.days<=36500,"Retention must be between 1 and 36500 days, or choose Keep indefinitely.");
        if(!scope.equals("Global")){Policy base=basePolicy(c,entity);require(p.days>=base.days&&(!base.forever||p.forever)&&(!p.allowDelete||base.allowDelete)&&(!p.autoPurge||base.autoPurge),"Organisation settings may extend retention or prevent deletion, but cannot weaken the global settings.");}
        IMendixObject rule=policy(c,entity,scope);if(rule==null)rule=Core.instantiate(c,M+"TableRetentionPolicy");
        for(String name:List.of("EntityName","ScopeKey","RetentionDays","KeepForever","AllowPermanentDelete","AutoPurge"))rule.setValue(c,name,f.getValue(c,name));rule.setValue(c,"PolicyKey",entity+"|"+scope);
        if(!scope.equals("Global")){String[] bits=scope.split(":");rule.setValue(c,M+"TableRetentionPolicy_"+bits[0],Core.createMendixIdentifier(positive(bits[1])));}
        Core.commit(c,rule);
        if(!scope.equals("Global")){String[] bits=scope.split(":");DevelopmentSamples.claimDerivedRetentionRecord(c,id(c,bits[1],M+bits[0]),rule);}
        FORM_BINDINGS.remove(f.getId().toLong());
    }
    private static void archive(IContext c,IMendixObject row,String actor)throws Exception{
        if(lifecycle(c,row)!=null)return;Policy p=effective(c,row);IMendixObject mark=Core.instantiate(c,M+"RecordLifecycle");
        String active=activeMember(row);if(active!=null){row.setValue(c,active,false);Core.commit(c,row);}
        if(row.hasMember("IsPublished")){row.setValue(c,"IsPublished",false);Core.commit(c,row);}
        mark.setValue(c,"RecordKey",Long.toString(row.getId().toLong()));mark.setValue(c,"EntityName",row.getType());mark.setValue(c,"ArchivedAt",new Date());mark.setValue(c,"ArchivedBy",actor);
        mark.setValue(c,"PurgeAfter",Date.from(Instant.now().plus(p.days,ChronoUnit.DAYS)));
        for(String kind:List.of("Club","Federation")){List<IMendixObject> ss=scopes(c,row,kind);if(ss.size()==1)mark.setValue(c,M+"RecordLifecycle_"+kind,ss.get(0).getId());}
        Core.commit(c,mark);
        DevelopmentSamples.claimDerivedRetentionRecord(c,row,mark);
    }
    private static void checkDelete(IContext c,IContext user,IMendixObject row)throws Exception{
        require(permission(c,user,row,"Delete")&&effective(c,row).allowDelete,"Permanent deletion is not allowed for this record.");checkReferences(c,row);
    }
    private static void checkReferences(IContext c,IMendixObject row)throws Exception{
        if(row.hasMember("Status")&&Set.of("Paid","Verified","Confirmed").contains(String.valueOf(row.getValue(c,"Status"))))throw new IllegalArgumentException("Confirmed financial or sporting history cannot be permanently deleted.");
        // Never let Mendix cascade a table delete to related records, even if its model association allows it.
        for(IMetaAssociation a:row.getMetaObject().getMetaAssociationsChild()){
            if(!a.getParent().isPersistable())continue;
            String parent=a.getParent().getName();if(parent.equals(M+"RecordLifecycle")||parent.equals(M+"TableRetentionPolicy"))continue;
            if(first(c,parent+"["+a.getName()+"="+row.getId().toLong()+"]")!=null)throw new IllegalArgumentException("This record is referenced by other records. Archive it to preserve the history.");
        }
        for(IMetaAssociation a:row.getMetaObject().getMetaAssociationsParent())if(a.getDeleteBehaviourParent()==IMetaAssociation.DeleteBehaviourParent.DELETEPARENTANDCHILDSASWELL&&!Core.retrieveByPath(c,row,a.getName()).isEmpty())throw new IllegalArgumentException("Deleting this record would also delete related data. Archive it instead.");
    }
    private static void purge(IContext c)throws Exception{
        Date now=new Date();List<IMendixObject> due=new ArrayList<>();
        for(int offset=0;;offset+=1000){List<IMendixObject> batch=Core.createXPathQuery("//"+M+"RecordLifecycle[PurgeAfter != empty and PurgeAfter <= $now]").setVariable("now",now).addSort("RecordKey",true).setAmount(1000).setOffset(offset).execute(c);due.addAll(batch);if(batch.size()<1000)break;}
        for(IMendixObject mark:due){
            try{IMendixObject row=Core.retrieveId(c,Core.createMendixIdentifier(positive(mark.getValue(c,"RecordKey"))));if(row==null){Core.delete(c,mark);continue;}
                Policy p=effective(c,row);Date archived=mark.getValue(c,"ArchivedAt");
                if(row.isInstanceOf("System.User")){
                    // Archived accounts: pseudonymise after the retention period instead of deleting. The marker stays without a purge date.
                    if(p.forever||Date.from(archived.toInstant().plus(p.days,ChronoUnit.DAYS)).after(now))continue;
                    PURGING.set(true);try{AccountAnonymiser.anonymise(c,row);}finally{PURGING.remove();}
                    mark.setValue(c,"PurgeAfter",null);mark.setValue(c,"LastPurgeError","");Core.commit(c,mark);continue;
                }
                if(!p.autoPurge||p.forever||!p.allowDelete||Date.from(archived.toInstant().plus(p.days,ChronoUnit.DAYS)).after(now))continue;
                require(mutable(row),"Record type cannot be purged.");checkReferences(c,row);
                PURGING.set(true);try{if(Core.delete(c,row))Core.delete(c,mark);}finally{PURGING.remove();}
            }catch(Exception e){mark.setValue(c,"LastPurgeError",e.getMessage()==null?"Deletion blocked":e.getMessage().substring(0,Math.min(190,e.getMessage().length())));Core.commit(c,mark);Core.getLogger("TableRetention").warn("Archived record retained: "+mark.getValue(c,"RecordKey"));}
        }
        PURGING.set(true);try{AccountAnonymiser.purgeAudits(c,policy(c,AccountAnonymiser.auditPolicyName(),"Global"));}
        catch(Exception e){Core.getLogger("TableRetention").warn("Audit retention purge failed: "+e.getMessage());}finally{PURGING.remove();}
    }
    private static String value(IContext user,IMendixObject row,String path)throws Exception{
        String[] parts=path.split("/");IMendixObject current=row;
        for(int i=0;i<parts.length-1;i++){
            if(!current.hasMember(parts[i])||!current.getMember(parts[i]).hasReadAccess(user))return "";
            List<IMendixObject> related=Core.retrieveByPath(user,current,parts[i]);if(related.size()!=1)return "";current=related.get(0);
        }
        String member=parts[parts.length-1];if(!current.hasMember(member)||!current.getMember(member).hasReadAccess(user))return "";
        Object v=current.getValue(user,member);return v==null?"":v instanceof Boolean b?(b?"Yes":"No"):v instanceof Date d?d.toInstant().toString():String.valueOf(v);
    }
    private static void export(IContext c,IContext user,TableDefinitions.Definition table,List<IMendixObject> rows)throws Exception{
        StringBuilder csv=new StringBuilder("\ufeff");List<String> headers=new ArrayList<>();for(TableDefinitions.Field f:table.fields())headers.add(f.title());headers.add("Archived");appendCsv(csv,headers);
        for(IMendixObject row:rows){List<String> values=new ArrayList<>();for(TableDefinitions.Field f:table.fields())values.add(value(user,row,f.path()));values.add(lifecycle(c,target(c,row))!=null?"Yes":"No");appendCsv(csv,values);}
        IMendixObject file=Core.instantiate(c,M+"TableExport");file.setValue(c,"DeleteAfterDownload",true);
        Core.storeFileDocumentContent(c,file,table.title().replaceAll("[^A-Za-z0-9_-]","_")+".csv",new ByteArrayInputStream(csv.toString().getBytes(StandardCharsets.UTF_8)));
        Core.microflowCall(M+"ACT_Table_Download").withParam("File",file).execute(c);
    }
    static String csvValue(String input){String s=input==null?"":input;String significant=s.stripLeading();if(!significant.isEmpty()&&"=+-@".indexOf(significant.charAt(0))>=0)s="'"+s;return "\""+s.replace("\"","\"\"")+"\"";}
    private static void appendCsv(StringBuilder out,List<String> cells){for(int i=0;i<cells.size();i++){if(i>0)out.append(',');out.append(csvValue(cells.get(i)));}out.append("\r\n");}
    private static long positive(String s){if(s==null||!s.matches("[0-9]{1,19}"))throw new IllegalArgumentException("Invalid record selection.");long n=Long.parseLong(s);if(n<=0)throw new IllegalArgumentException("Invalid record selection.");return n;}
    private static IMendixObject id(IContext c,String s,String type)throws Exception{IMendixObject o=Core.retrieveId(c,Core.createMendixIdentifier(positive(s)));require(o!=null&&o.isInstanceOf(type),"Reopen the table.");return o;}
    private static void require(boolean ok,String message){if(!ok)throw new IllegalArgumentException(message);}
    static void message(IContext c,String text)throws Exception{Core.microflowCall(M+"ACT_Table_Message").withParam("Message",text).execute(c);}
}

