package myfirstmodule;

import com.mendix.core.Core;
import com.mendix.systemwideinterfaces.core.*;
import java.util.*;

/** Starts existing create workflows, choosing required owning records without granting additional rights. */
final class TableCreationService {
    private static final String M="MyFirstModule.";
    private static final List<String> CONTEXTS=List.of("Club","Federation","CompetitionTeam","CompetitionLeague","CompetitionVenue","Season","MembershipType","FederationType","MatchForm");
    private static final Map<Long,Pending> FORMS=new java.util.concurrent.ConcurrentHashMap<>();
    private static final class Pending {
        final long user;final long created=System.currentTimeMillis();final String table;final TableCreationDefinitions.Spec spec;final List<IMendixObject> contexts;
        final Map<String,Object> arguments=new LinkedHashMap<>();final Map<Long,IMendixObject> choices=new LinkedHashMap<>();String parameter;boolean opened;
        Pending(IContext user,String table,TableCreationDefinitions.Spec spec,List<IMendixObject> contexts){this.user=user.getSession().getUserId().toLong();this.table=table;this.spec=spec;this.contexts=contexts;}
    }
    static boolean canCreate(IContext c,IContext user,IMendixObject state,String table)throws Exception{
        TableCreationDefinitions.Spec spec=TableCreationDefinitions.get(table);if(spec==null)return false;
        List<IMendixObject> contexts=contexts(c,state);
        if(spec.permission().equals("Admin"))return TableLifecycleService.admin(user);
        if(spec.permission().equals("Authenticated"))return user.getSession().getUserRolesNames().stream().anyMatch(Set.of("Administrator","User","ClubAdmin","FederationAdmin")::contains);
        if(spec.permission().startsWith("Match"))return contexts.stream().anyMatch(o->o.getType().equals(M+"MatchForm")&&Boolean.TRUE.equals(o.getValue(c,spec.permission().equals("MatchManage")?"CanManage":"CanLead")));
        if(spec.permission().equals("Fixture")){
            for(IMendixObject fixture:Core.createXPathQuery("//"+M+"CompetitionFixture").execute(c))if(allowedCandidate(c,user,spec,fixture,new LinkedHashMap<>(),contexts))return true;
            return false;
        }
        String kind=spec.permission().startsWith("Federation")?"Federation":"Club";
        List<IMendixObject> owners=ownerContexts(c,contexts,kind);
        if(!owners.isEmpty()){for(IMendixObject owner:owners)if(!allowedOwner(c,user,spec,owner))return false;return true;}
        for(IMendixObject owner:Core.createXPathQuery("//"+M+kind).execute(c))if(allowedOwner(c,user,spec,owner))return true;
        return false;
    }
    static void start(IContext c,IContext user,IMendixObject state,String table,List<IMendixObject> selection)throws Exception{
        if(!canCreate(c,user,state,table))throw new SecurityException("You cannot create records in this view.");
        TableCreationDefinitions.Spec spec=TableCreationDefinitions.get(table);
        List<IMendixObject> context=contexts(c,state);if(selection.size()==1)context.add(selection.get(0));
        Pending pending=new Pending(user,table,spec,context);
        if(spec.mode().equals("New")){
            IMendixObject record=Core.instantiate(c,spec.entity());Core.microflowCall(spec.flow()).withParam("Item",record).execute(c);return;
        }
        if(spec.parameters().length==0){Core.microflowCall(spec.flow()).execute(c);return;}
        IMendixObject form=Core.instantiate(c,M+"TableCreateForm");form.setValue(c,"Title","Create · "+TableDefinitions.get(table).title());
        FORMS.entrySet().removeIf(e->System.currentTimeMillis()-e.getValue().created>3600000);FORMS.put(form.getId().toLong(),pending);
        advance(c,user,form,pending);
    }
    static void continueCreate(IContext c,IContext user,IMendixObject form)throws Exception{
        Pending pending=FORMS.get(form.getId().toLong());
        if(pending==null||pending.user!=user.getSession().getUserId().toLong()||System.currentTimeMillis()-pending.created>3600000)throw new SecurityException("Reopen Create to start again.");
        IMendixIdentifier selected=form.getValue(c,M+"TableCreateForm_SelectedChoice");IMendixObject chosen=selected==null?null:pending.choices.get(selected.toLong());
        if(chosen==null){TableLifecycleService.message(c,"Choose an option before continuing.");return;}
        if(!allowedCandidate(c,user,pending.spec,chosen,pending.arguments,pending.contexts))throw new SecurityException("Your create permission changed. Reopen Create.");
        pending.arguments.put(pending.parameter,chosen);advance(c,user,form,pending);
    }
    private static void advance(IContext c,IContext user,IMendixObject form,Pending pending)throws Exception{
        for(TableCreationDefinitions.Parameter p:pending.spec.parameters()){
            if(pending.arguments.containsKey(p.name()))continue;
            List<IMendixObject> choices=new ArrayList<>();
            for(IMendixObject context:pending.contexts)if(context.isInstanceOf(p.entity())&&allowedCandidate(c,user,pending.spec,context,pending.arguments,pending.contexts))choices.add(context);
            if(choices.isEmpty()){
                if(!Core.getMetaObject(p.entity()).isPersistable())throw new IllegalArgumentException("Open the record's workspace to create this item.");
                for(IMendixObject candidate:Core.createXPathQuery("//"+p.entity()).execute(c))if(allowedCandidate(c,user,pending.spec,candidate,pending.arguments,pending.contexts))choices.add(candidate);
            }
            choices=new ArrayList<>(new LinkedHashSet<>(choices));
            if(choices.size()==1){pending.arguments.put(p.name(),choices.get(0));continue;}
            if(choices.isEmpty()){TableLifecycleService.message(c,"There are no available "+words(p.entity())+" records where you can create this item.");return;}
            for(IMendixObject old:Core.retrieveByPath(c,form,M+"TableCreateChoice_Form"))old.setValue(c,M+"TableCreateChoice_Form",null);
            pending.choices.clear();pending.parameter=p.name();form.setValue(c,M+"TableCreateForm_SelectedChoice",null);
            form.setValue(c,"Prompt","Choose "+words(p.entity()));
            for(IMendixObject candidate:choices){IMendixObject choice=Core.instantiate(c,M+"TableCreateChoice");choice.setValue(c,"Label",label(user,candidate));choice.setValue(c,M+"TableCreateChoice_Form",form.getId());pending.choices.put(choice.getId().toLong(),candidate);}
            if(!pending.opened){pending.opened=true;Core.microflowCall(M+"ACT_TableCreate_View").withParam("Form",form).execute(c);}return;
        }
        // Recheck every owning record immediately before entering the existing server workflow.
        for(Object value:pending.arguments.values())if(value instanceof IMendixObject o&&!allowedCandidate(c,user,pending.spec,o,pending.arguments,pending.contexts))throw new SecurityException("Your create permission changed.");
        FORMS.remove(form.getId().toLong());if(pending.opened)Core.microflowCall(M+"ACT_TableCreate_Close").execute(c);
        Core.microflowCall(pending.spec.flow()).withParams(pending.arguments).execute(c);
    }
    private static boolean allowedCandidate(IContext c,IContext user,TableCreationDefinitions.Spec spec,IMendixObject o,Map<String,Object> arguments,List<IMendixObject> context)throws Exception{
        if(spec.permission().startsWith("Match"))return o.getType().equals(M+"MatchForm")&&Boolean.TRUE.equals(o.getValue(c,spec.permission().equals("MatchManage")?"CanManage":"CanLead"));
        if(TableLifecycleService.archived(c,o))return false;
        if(spec.permission().equals("Fixture")){
            if(!o.getType().equals(M+"CompetitionFixture"))return false;
            IMendixObject caps=Core.microflowCall(M+"DS_CompetitionFixture_Capabilities").withParam("Fixture",o).execute(c);return Boolean.TRUE.equals(caps.getValue(c,"CanProposeChange"));
        }
        String kind=spec.permission().startsWith("Federation")?"Federation":"Club";
        List<IMendixObject> owners=TableLifecycleService.scopes(c,o,kind);
        if(owners.isEmpty())return spec.permission().equals("Admin")&&TableLifecycleService.admin(user);
        List<IMendixObject> fixed=ownerContexts(c,context,kind);for(Object arg:arguments.values())if(arg instanceof IMendixObject item)fixed.addAll(TableLifecycleService.scopes(c,item,kind));
        for(IMendixObject owner:owners){if(!fixed.isEmpty()&&fixed.stream().noneMatch(x->x.getId().equals(owner.getId())))return false;if(!allowedOwner(c,user,spec,owner))return false;}return true;
    }
    private static boolean allowedOwner(IContext c,IContext user,TableCreationDefinitions.Spec spec,IMendixObject owner)throws Exception{
        if(TableLifecycleService.archived(c,owner))return false;
        if(TableLifecycleService.admin(user))return true;
        return switch(spec.permission()){
            case "Federation"->TableLifecycleService.check(c,"SUB_Federation_HasAccess",Map.of("Federation",owner));
            case "FederationCompetition"->TableLifecycleService.check(c,"SUB_Competition_FederationPermission",Map.of("Federation",owner,"Operation","Manage"));
            case "CompetitionCreate","CompetitionUpdate"->TableLifecycleService.check(c,"SUB_Competition_ClubPermission",Map.of("Club",owner,"Operation",spec.permission().equals("CompetitionCreate")?"Create":"Update"));
            case "ClubOwner"->TableLifecycleService.check(c,"SUB_Club_AdministratorOrOwner",Map.of("Club",owner));
            case "Club"->TableLifecycleService.check(c,"SUB_Club_HasPermission",Map.of("Club",owner,"Resource",spec.resource(),"Operation","Create"));
            case "Affiliation"->TableLifecycleService.check(c,"SUB_Club_HasPermission",Map.of("Club",owner,"Resource","Settings","Operation","Update"));
            default->false;
        };
    }
    private static List<IMendixObject> contexts(IContext c,IMendixObject state)throws Exception{List<IMendixObject> out=new ArrayList<>();for(String name:CONTEXTS)out.addAll(Core.retrieveByPath(c,state,M+"TableSelection_Context"+name));return out;}
    private static List<IMendixObject> ownerContexts(IContext c,List<IMendixObject> records,String kind)throws Exception{List<IMendixObject> out=new ArrayList<>();for(IMendixObject record:records)out.addAll(TableLifecycleService.scopes(c,record,kind));return new ArrayList<>(new LinkedHashSet<>(out));}
    private static String label(IContext user,IMendixObject o){for(String name:List.of("Name","Title","MemberName","Label"))if(o.hasMember(name)&&o.getMember(name).hasReadAccess(user)){String value=Objects.toString(o.getValue(user,name),"");if(!value.isBlank())return value.substring(0,Math.min(195,value.length()));}return words(o.getType());}
    private static String words(String entity){return entity.substring(entity.lastIndexOf('.')+1).replaceAll("([a-z])([A-Z])","$1 $2").toLowerCase(Locale.ROOT);}
}
