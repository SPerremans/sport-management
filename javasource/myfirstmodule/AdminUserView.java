package myfirstmodule;

import com.mendix.core.Core;
import com.mendix.externalinterface.connector.RequestHandler;
import com.mendix.m2ee.api.IMxRuntimeRequest;
import com.mendix.m2ee.api.IMxRuntimeResponse;
import com.mendix.systemwideinterfaces.core.*;
import java.net.URI;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/** Short-lived, administrator-owned user sessions. No account passwords or roles are changed. */
public final class AdminUserView extends RequestHandler {
    private static final String PATH = "admin-user-view/";
    private static final String COOKIE = "NexioUserView";
    private static final long DURATION = TimeUnit.MINUTES.toMillis(15);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Map<String, View> VIEWS = new ConcurrentHashMap<>();
    private static boolean initialized;
    private static final class View {
        final UUID administrator, target;
        final String actor, subject, reason;
        final long expires = System.currentTimeMillis() + DURATION;
        View(ISession admin, ISession user, String why) {
            administrator=admin.getId(); target=user.getId(); actor=admin.getUserName(); subject=user.getUserName(); reason=why;
        }
    }
    public static synchronized void initialize() throws Exception {
        if (initialized) return;
        if (available()) verifyRuntimeQueries();
        Core.addRequestHandler(PATH, new AdminUserView());
        Core.scheduleAtFixedRate(new UserAction<Void>(Core.createSystemContext()) {
            public Void executeAction() { sweep(); return null; }
        }, 1, 1, TimeUnit.MINUTES);
        initialized=true;
    }
    /** Read-only startup probe: validates the entity paths and parameterized queries against this runtime. */
    private static void verifyRuntimeQueries() throws Exception {
        IContext ctx=Core.createSystemContext();
        Core.createXPathQuery("//System.User[Active=true and Blocked=false and WebServiceUser=false][contains(Name,$query)][not(System.UserRoles/System.UserRole/Name='Administrator')]")
            .setVariable("query","").setAmount(1).execute(ctx);
        Core.createXPathQuery("//UserManagement.Person[Active=true and Blocked=false and WebServiceUser=false][contains(FullName,$query)][not(System.UserRoles/System.UserRole/Name='Administrator')]")
            .setVariable("query","").setAmount(1).execute(ctx);
        List<IMendixObject> probeUsers=Core.createXPathQuery("//System.User").setAmount(1).execute(ctx);
        if(!probeUsers.isEmpty())Core.createXPathQuery("//System.User[id=$id and Active=true and Blocked=false][System.UserRoles/System.UserRole/Name='Administrator']")
            .setVariable("id",probeUsers.get(0).getId()).setAmount(1).execute(ctx);
        if(!Core.getMetaObject("UserManagement.Person").isSubClassOf(Core.getMetaObject("System.User")))
            throw new IllegalStateException("The configured account type must specialize System.User.");
        java.nio.file.Files.writeString(java.nio.file.Path.of(Core.getConfiguration().getBasePath().toString(),"admin-user-view-checks.txt"),
            "PASS: account entity inheritance and three parameterized account/authorization queries executed. No user sessions were switched and no business records changed.\n");
    }
    private static boolean available() {
        return Core.getConfiguration().isInDevelopment() && unittesting.ConfigurationManager.isLocalDevelopment();
    }
    private static boolean administrator(ISession session) throws Exception {
        if (session == null || session.isSystemSession() || !session.getUserRolesNames().contains("Administrator")) return false;
        return !Core.createXPathQuery("//System.User[id=$id and Active=true and Blocked=false][System.UserRoles/System.UserRole/Name='Administrator']")
            .setVariable("id", session.getUserId()).setAmount(1).execute(Core.createSystemContext()).isEmpty();
    }
    private static boolean eligible(IMendixObject object, boolean checkAdministrator) throws Exception {
        if (object == null) return false;
        IContext system=Core.createSystemContext();
        IUser user=Core.getUser(system, object.getValue(system,"Name"));
        return user != null && UserViewPolicy.eligible(Boolean.TRUE.equals(object.getValue(system,"Active")),
            Boolean.TRUE.equals(object.getValue(system,"Blocked")), Boolean.TRUE.equals(user.isAnonymous()),
            Boolean.TRUE.equals(user.isWebserviceUser()),
            checkAdministrator && !Core.createXPathQuery("//System.User[id=$id][System.UserRoles/System.UserRole/Name='Administrator']")
                .setVariable("id",object.getId()).setAmount(1).execute(system).isEmpty());
    }
    @Override protected void processRequest(IMxRuntimeRequest req, IMxRuntimeResponse resp, String path) throws Exception {
        resp.addHeader("Cache-Control","no-store"); resp.addHeader("Pragma","no-cache");
        resp.addHeader("X-Content-Type-Options","nosniff"); resp.addHeader("Referrer-Policy","no-referrer");
        resp.addHeader("X-Frame-Options","DENY");
        resp.addHeader("Content-Security-Policy","default-src 'self'; script-src 'none'; style-src 'self'; frame-src 'none'; frame-ancestors 'none'; form-action 'self'; base-uri 'none'");
        if (!available()) { error(resp,404,"User viewing is available only in local development."); return; }
        ISession current;
        try { current=getSessionFromRequest(req); } catch(com.mendix.core.CoreException expired) { current=null; }
        String method=req.getHttpServletRequest().getMethod();
        String route=path == null ? "" : path.replaceAll("^/+|/+$","");
        try {
            if ("POST".equals(method)) {
                if (current == null || !UserViewPolicy.tokenMatches(current.getCsrfToken(),req.getParameter("csrf")))
                    throw new SecurityException("This form expired. Reopen View as user.");
                String origin=req.getHeader("Origin");
                URI root=URI.create(req.getRootUrl());
                if (origin != null && !origin.equalsIgnoreCase(root.getScheme()+"://"+root.getAuthority()))
                    throw new SecurityException("The request origin is not allowed.");
                if ("start".equals(route)) { start(req,resp,current); return; }
                if ("return".equals(route)) { restore(req,resp,current); return; }
            } else if ("GET".equals(method)) {
                if (route.isEmpty()) { accounts(req,resp,current); return; }
                if ("status".equals(route)) {
                    if (req.getCookie(COOKIE)==null) { resp.setStatus(204); return; }
                    status(req,resp,current); return;
                }
            }
            error(resp,405,"Use the controls on View as user.");
        } catch (SecurityException denied) {
            Core.getLogger("AdminUserView").warn("Denied user-view request; current user="+(current==null?"anonymous":current.getUserId()));
            error(resp,403,denied.getMessage());
        } catch (IllegalArgumentException invalid) { error(resp,400,invalid.getMessage()); }
    }
    private synchronized void start(IMxRuntimeRequest req, IMxRuntimeResponse resp, ISession admin) throws Exception {
        if (!administrator(admin)) throw new SecurityException("Only the platform Administrator can view as another user.");
        if (VIEWS.values().stream().anyMatch(v->v.administrator.equals(admin.getId())))
            throw new SecurityException("Return from the existing user view first.");
        String id=req.getParameter("user");
        if (id==null || !id.matches("[0-9]{1,19}")) throw new IllegalArgumentException("Select an account.");
        IContext system=Core.createSystemContext();
        IMendixObject selected=Core.retrieveId(system,Core.createMendixIdentifier(Long.parseLong(id)));
        if (selected==null || !("System.User".equals(selected.getType()) || selected.getMetaObject().isSubClassOf(Core.getMetaObject("System.User"))) || !eligible(selected,true))
            throw new SecurityException("Select an active, unblocked human account without the platform Administrator role.");
        String why=UserViewPolicy.reason(req.getParameter("reason"));
        IUser user=Core.getUser(system,selected.getValue(system,"Name"));
        ISession child=Core.initializeSession(user,null);
        if (child.isSystemSession() || child.getUserRolesNames().contains("Administrator") || !child.getUserId().equals(selected.getId())) {
            Core.logout(child); throw new SecurityException("The requested user session could not be isolated.");
        }
        String token=token(); View v=new View(admin,child,why);
        VIEWS.put(token,v);
        try {
            admin.keepAlive(); Core.addMendixCookies(req,resp,child,false); cookie(req,resp,token,900);
            Core.schedule(new UserAction<Void>(Core.createSystemContext()) {
                public Void executeAction() { end(token,v,"expired"); return null; }
            },DURATION,TimeUnit.MILLISECONDS);
            audit("START",v); redirect(resp,base(req)+"index.html");
        } catch (Exception failed) { end(token,v,"start-failed"); throw failed; }
    }
    private void restore(IMxRuntimeRequest req, IMxRuntimeResponse resp, ISession current) throws Exception {
        String token=req.getCookie(COOKIE); View v=ownedView(token,current);
        synchronized(v) {
            if (VIEWS.get(token)!=v) throw new SecurityException("The user view has already ended.");
            ISession admin=Core.getSessionById(v.administrator);
            if (!administrator(admin)) { end(token,v,"administrator-revoked"); throw new SecurityException("Sign in again as administrator."); }
            admin.keepAlive(); Core.addMendixCookies(req,resp,admin,false);
            cookie(req,resp,"",0); end(token,v,"RETURN"); redirect(resp,base(req)+PATH);
        }
    }
    private View ownedView(String token, ISession current) throws Exception {
        View v=token==null?null:VIEWS.get(token);
        if (v==null || current==null || !UserViewPolicy.validBinding(v.target.toString(),current.getId().toString(),v.expires,System.currentTimeMillis()))
            throw new SecurityException("This user view expired or belongs to another session. Sign in again as administrator.");
        if(!eligible(Core.retrieveId(Core.createSystemContext(),current.getUserId()),true)) {
            end(token,v,"target-unavailable"); throw new SecurityException("This account is no longer available for user viewing.");
        }
        if (!administrator(Core.getSessionById(v.administrator))) {
            end(token,v,"administrator-revoked"); throw new SecurityException("Sign in again as administrator.");
        }
        return v;
    }
    private void status(IMxRuntimeRequest req, IMxRuntimeResponse resp, ISession current) throws Exception {
        View v=ownedView(req.getCookie(COOKIE),current);
        resp.setContentType("application/json; charset=UTF-8");
        resp.getWriter().write(new com.mendix.thirdparty.org.json.JSONObject().put("userName",v.subject)
            .put("csrf",current.getCsrfToken()).put("returnPath",base(req)+PATH+"return").put("expiresAt",v.expires).toString());
    }
    private void accounts(IMxRuntimeRequest req, IMxRuntimeResponse resp, ISession admin) throws Exception {
        if (!administrator(admin)) throw new SecurityException("Sign in as the platform Administrator to open View as user.");
        String b=base(req), query=Objects.toString(req.getParameter("q"),"").trim();
        if(query.length()>100)throw new IllegalArgumentException("Use a shorter search.");
        List<IMendixObject> users=Core.createXPathQuery("//System.User[Active=true and Blocked=false and WebServiceUser=false][contains(Name,$query)][not(System.UserRoles/System.UserRole/Name='Administrator')]")
            .setVariable("query",query).addSort("Name",true).setAmount(200).execute(Core.createSystemContext());
        Map<Long,IMendixObject> matches=new LinkedHashMap<>();for(IMendixObject user:users)matches.put(user.getId().toLong(),user);
        for(IMendixObject person:Core.createXPathQuery("//UserManagement.Person[Active=true and Blocked=false and WebServiceUser=false][contains(FullName,$query)][not(System.UserRoles/System.UserRole/Name='Administrator')]")
            .setVariable("query",query).addSort("Name",true).setAmount(200).execute(Core.createSystemContext()))matches.put(person.getId().toLong(),person);
        users=new ArrayList<>(matches.values());users.sort(Comparator.comparing(o->Objects.toString(o.getValue(Core.createSystemContext(),"Name"),""),String.CASE_INSENSITIVE_ORDER));
        if(users.size()>200)users=users.subList(0,200);
        StringBuilder body=new StringBuilder("<main class='nexio-user-view-picker'><h1>View as user</h1><p>Open the app with a user’s actual permissions, then return here to switch accounts. No password is needed. Disabled, blocked, service and platform administrator accounts cannot be selected.</p>");
        body.append("<form method='get'><label for='q'>Search name or login</label><input id='q' name='q' value='").append(esc(query)).append("' maxlength='100'><button class='btn btn-default'>Search</button></form>");
        body.append("<form method='post' action='").append(b).append(PATH).append("start'>").append(csrf(admin));
        body.append("<label for='reason'>Reason for viewing this user</label><input id='reason' name='reason' required minlength='3' maxlength='300' placeholder='For example: check club registration permissions'>");
        body.append("<div class='nexio-user-view-table'><table><thead><tr><th>Name</th><th>Login name</th><th>Account roles</th><th>Action</th></tr></thead><tbody>");
        int count=0;
        for(IMendixObject object:users) {
            if(!eligible(object,false))continue;
            IUser user=Core.getUser(Core.createSystemContext(),object.getValue(Core.createSystemContext(),"Name"));
            Object name=user.getName();
            for(com.mendix.systemwideinterfaces.core.meta.IMetaPrimitive attribute:object.getMetaObject().getMetaPrimitives())
                if(attribute.getName().equals("FullName"))name=object.getValue(Core.createSystemContext(),"FullName");
            body.append("<tr><td>").append(esc(name)).append("</td><td>").append(esc(user.getName())).append("</td><td>").append(esc(String.join(", ",new TreeSet<>(user.getUserRoleNames()))))
                .append("</td><td><button class='btn btn-default' name='user' value='").append(object.getId().toLong()).append("'>View as user</button></td></tr>"); count++;
        }
        if(count==0)body.append("<tr><td colspan='4'>No eligible accounts found. Generate sample data to add active sample club and federation administrators.</td></tr>");
        body.append("</tbody></table></div></form><p>Up to 200 accounts. <a href='").append(b).append("index.html'>Back to administrator app</a></p></main>");
        html(resp,"View as user","nexio-user-view",body.toString(),b);
    }
    private static void sweep() {
        for(Map.Entry<String,View> entry:VIEWS.entrySet())try {
            View v=entry.getValue(); ISession admin=Core.getSessionById(v.administrator), child=Core.getSessionById(v.target);
            if(!available() || System.currentTimeMillis()>=v.expires || !administrator(admin) || child==null || !eligible(Core.retrieveId(Core.createSystemContext(),child.getUserId()),true))
                end(entry.getKey(),v,"expired-or-revoked");
            else admin.keepAlive();
        } catch(Exception failed) { end(entry.getKey(),entry.getValue(),"validation-failed"); }
    }
    private static void end(String token, View v, String event) {
        synchronized(v) {
            if(!VIEWS.remove(token,v))return;
            ISession child=Core.getSessionById(v.target); if(child!=null)Core.logout(child);
            audit(event,v);
        }
    }
    private static void audit(String event, View v) {
        Core.getLogger("AdminUserView").info(event+" administrator="+v.actor.replaceAll("[\\r\\n]", " ")+" target="+v.subject.replaceAll("[\\r\\n]", " ")+" reason="+v.reason);
    }
    private static String token() { byte[] bytes=new byte[32];RANDOM.nextBytes(bytes);return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); }
    private static String base(IMxRuntimeRequest req) { String p=URI.create(req.getRootUrl()).getPath(); return p.endsWith("/")?p:p+"/"; }
    private static void cookie(IMxRuntimeRequest req, IMxRuntimeResponse resp, String value, int age) {
        resp.addHeader("Set-Cookie",COOKIE+"="+value+"; Path="+base(req)+PATH+"; Max-Age="+age+"; HttpOnly; SameSite=Strict"+
            ("https".equalsIgnoreCase(URI.create(req.getRootUrl()).getScheme())?"; Secure":""));
    }
    private static String csrf(ISession s) { return "<input type='hidden' name='csrf' value='"+esc(s.getCsrfToken())+"'>"; }
    private static String esc(Object value) { return UserViewPolicy.html(value); }
    private static void redirect(IMxRuntimeResponse resp,String path) { resp.setStatus(303);resp.addHeader("Location",path); }
    private static void error(IMxRuntimeResponse resp,int status,String message) throws Exception {
        resp.setStatus(status);resp.setContentType("text/plain; charset=UTF-8");resp.getWriter().write(message);
    }
    private static void html(IMxRuntimeResponse resp,String title,String css,String body,String base) throws Exception {
        resp.setContentType("text/html; charset=UTF-8");
        resp.getWriter().write("<!doctype html><html lang='en'><head><meta charset='utf-8'><meta name='viewport' content='width=device-width,initial-scale=1'><title>"+esc(title)+"</title><link rel='stylesheet' href='"+base+"theme.compiled.css'></head><body class='"+css+"'>"+body+"</body></html>");
    }
}
