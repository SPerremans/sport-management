package myfirstmodule;

import com.mendix.core.Core;
import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.systemwideinterfaces.core.IMendixObject;
import com.mendix.systemwideinterfaces.core.IMendixIdentifier;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;

/** Disposable, local-development-only data. The private manifest records ownership, not names. */
public final class DevelopmentSamples {
    private static final String KEY = "nexio-development-samples-v1";
    private static final String BATCH = "MyFirstModule.DevSampleBatch";
    private static final String SEASON_ROLE_FEATURE = "features:season-roles-v2";
    private static final String[] TYPES = {"MyFirstModule.PaymentVerification","MyFirstModule.Invoice","MyFirstModule.NewsItem","MyFirstModule.ClubEvent","MyFirstModule.FormResponse","MyFirstModule.FormField","MyFirstModule.RegistrationRequest","MyFirstModule.Membership","MyFirstModule.MembershipType","MyFirstModule.Club","MyFirstModule.FeeAdjustment","MyFirstModule.ClubStaff","MyFirstModule.ClubPermission","MyFirstModule.ClubRole","MyFirstModule.SeasonFee","MyFirstModule.Season","MyFirstModule.InvoiceMail","MyFirstModule.RenewalOffer","MyFirstModule.InvoiceLine","MyFirstModule.BillingPlan","MyFirstModule.ClubBillingPolicy","MyFirstModule.InvoiceStyle","MyFirstModule.FederationRegistration","MyFirstModule.FederationAffiliation","MyFirstModule.FederationRate","MyFirstModule.FederationType","MyFirstModule.Federation","MyFirstModule.InvitationClaim","MyFirstModule.AccountInvitation","MyFirstModule.BillingClaim","MyFirstModule.CompetitionAudit","MyFirstModule.FederationCompetitionDelegate","MyFirstModule.FixtureChange","MyFirstModule.CompetitionFixture","MyFirstModule.ClubCompetitionPolicy","MyFirstModule.VenueAvailability","MyFirstModule.CompetitionVenue","MyFirstModule.CompetitionPoule","MyFirstModule.LeagueEntry","MyFirstModule.CompetitionPlayer","MyFirstModule.CompetitionTeam","MyFirstModule.CompetitionLeague","UserManagement.Person"};
    private static final Map<String, String[]> EDGES = new LinkedHashMap<>();
    static {
        EDGES.put("MyFirstModule.PaymentVerification", new String[]{"MyFirstModule.PaymentVerification_Invoice"});
        EDGES.put("MyFirstModule.Invoice", new String[]{"MyFirstModule.Invoice_RegistrationRequest","MyFirstModule.Invoice_Membership","MyFirstModule.Invoice_Club","MyFirstModule.Invoice_RenewalOffer","MyFirstModule.Invoice_RecipientClub","MyFirstModule.Invoice_Federation","MyFirstModule.Invoice_BillingPlan","MyFirstModule.Invoice_Person"});
        EDGES.put("MyFirstModule.NewsItem", new String[]{"MyFirstModule.NewsItem_Club"});
        EDGES.put("MyFirstModule.ClubEvent", new String[]{"MyFirstModule.ClubEvent_Club"});
        EDGES.put("MyFirstModule.FormResponse", new String[]{"MyFirstModule.FormResponse_FormField","MyFirstModule.FormResponse_RegistrationRequest"});
        EDGES.put("MyFirstModule.FormField", new String[]{"MyFirstModule.FormField_Club"});
        EDGES.put("MyFirstModule.RegistrationRequest", new String[]{"MyFirstModule.RegistrationRequest_Membership","MyFirstModule.RegistrationRequest_MembershipType","MyFirstModule.RegistrationRequest_Club","MyFirstModule.RegistrationRequest_SeasonFee","MyFirstModule.RegistrationRequest_Season","MyFirstModule.RegistrationRequest_Person"});
        EDGES.put("MyFirstModule.Membership", new String[]{"MyFirstModule.Membership_MembershipType","MyFirstModule.Membership_Club","MyFirstModule.Membership_Season","MyFirstModule.Membership_Person"});
        EDGES.put("MyFirstModule.MembershipType", new String[]{"MyFirstModule.MembershipType_Club","MyFirstModule.MembershipType_FederationType"});
        EDGES.put("MyFirstModule.FeeAdjustment", new String[]{"MyFirstModule.FeeAdjustment_Club","MyFirstModule.FeeAdjustment_RegistrationRequest"});
        EDGES.put("MyFirstModule.ClubStaff", new String[]{"MyFirstModule.ClubStaff_ClubRole","MyFirstModule.ClubStaff_Person"});
        EDGES.put("MyFirstModule.ClubPermission", new String[]{"MyFirstModule.ClubPermission_ClubRole"});
        EDGES.put("MyFirstModule.ClubRole", new String[]{"MyFirstModule.ClubRole_Club"});
        EDGES.put("MyFirstModule.SeasonFee", new String[]{"MyFirstModule.SeasonFee_MembershipType","MyFirstModule.SeasonFee_Season","MyFirstModule.SeasonFee_Club"});
        EDGES.put("MyFirstModule.Season", new String[]{"MyFirstModule.Season_Club"});
        EDGES.put("MyFirstModule.InvoiceMail", new String[]{"MyFirstModule.InvoiceMail_Invoice"});
        EDGES.put("MyFirstModule.RenewalOffer", new String[]{"MyFirstModule.RenewalOffer_Club","MyFirstModule.RenewalOffer_SeasonFee","MyFirstModule.RenewalOffer_Membership","MyFirstModule.RenewalOffer_RegistrationRequest","MyFirstModule.RenewalOffer_Person"});
        EDGES.put("MyFirstModule.InvoiceLine", new String[]{"MyFirstModule.InvoiceLine_FederationRegistration","MyFirstModule.InvoiceLine_Invoice"});
        EDGES.put("MyFirstModule.BillingPlan", new String[]{"MyFirstModule.BillingPlan_Club","MyFirstModule.BillingPlan_Membership","MyFirstModule.BillingPlan_RegistrationRequest","MyFirstModule.BillingPlan_Person"});
        EDGES.put("MyFirstModule.ClubBillingPolicy", new String[]{"MyFirstModule.ClubBillingPolicy_RenewalSeason","MyFirstModule.ClubBillingPolicy_Season","MyFirstModule.ClubBillingPolicy_Club"});
        EDGES.put("MyFirstModule.InvoiceStyle", new String[]{"MyFirstModule.InvoiceStyle_Federation","MyFirstModule.InvoiceStyle_Club"});
        EDGES.put("MyFirstModule.FederationRegistration", new String[]{"MyFirstModule.FederationRegistration_Membership","MyFirstModule.FederationRegistration_RegistrationRequest","MyFirstModule.FederationRegistration_Club","MyFirstModule.FederationRegistration_FederationType","MyFirstModule.FederationRegistration_Federation"});
        EDGES.put("MyFirstModule.FederationAffiliation", new String[]{"MyFirstModule.FederationAffiliation_Club","MyFirstModule.FederationAffiliation_Federation"});
        EDGES.put("MyFirstModule.FederationRate", new String[]{"MyFirstModule.FederationRate_FederationType"});
        EDGES.put("MyFirstModule.FederationType", new String[]{"MyFirstModule.FederationType_Federation"});
        EDGES.put("MyFirstModule.AccountInvitation", new String[]{"MyFirstModule.AccountInvitation_Club","MyFirstModule.AccountInvitation_Creator","MyFirstModule.AccountInvitation_Person"});
        EDGES.put("MyFirstModule.CompetitionAudit", new String[]{"MyFirstModule.CompetitionAudit_Fixture","MyFirstModule.CompetitionAudit_League"});
        EDGES.put("MyFirstModule.FederationCompetitionDelegate", new String[]{"MyFirstModule.FederationCompetitionDelegate_Federation","MyFirstModule.FederationCompetitionDelegate_User"});
        EDGES.put("MyFirstModule.FixtureChange", new String[]{"MyFirstModule.FixtureChange_Slot","MyFirstModule.FixtureChange_Fixture"});
        EDGES.put("MyFirstModule.CompetitionFixture", new String[]{"MyFirstModule.CompetitionFixture_Slot","MyFirstModule.CompetitionFixture_AwayClub","MyFirstModule.CompetitionFixture_HomeClub","MyFirstModule.CompetitionFixture_Away","MyFirstModule.CompetitionFixture_Home","MyFirstModule.CompetitionFixture_Poule","MyFirstModule.CompetitionFixture_League"});
        EDGES.put("MyFirstModule.ClubCompetitionPolicy", new String[]{"MyFirstModule.ClubCompetitionPolicy_Club"});
        EDGES.put("MyFirstModule.VenueAvailability", new String[]{"MyFirstModule.VenueAvailability_Club","MyFirstModule.VenueAvailability_Venue"});
        EDGES.put("MyFirstModule.CompetitionVenue", new String[]{"MyFirstModule.CompetitionVenue_Club"});
        EDGES.put("MyFirstModule.CompetitionPoule", new String[]{"MyFirstModule.CompetitionPoule_League"});
        EDGES.put("MyFirstModule.LeagueEntry", new String[]{"MyFirstModule.LeagueEntry_Poule","MyFirstModule.LeagueEntry_Club","MyFirstModule.LeagueEntry_Team","MyFirstModule.LeagueEntry_League"});
        EDGES.put("MyFirstModule.CompetitionPlayer", new String[]{"MyFirstModule.CompetitionPlayer_Membership","MyFirstModule.CompetitionPlayer_Team"});
        EDGES.put("MyFirstModule.CompetitionTeam", new String[]{"MyFirstModule.CompetitionTeam_Club"});
        EDGES.put("MyFirstModule.CompetitionLeague", new String[]{"MyFirstModule.CompetitionLeague_Federation"});
        EDGES.put("MyFirstModule.Club", new String[]{"MyFirstModule.Club_Admins"});
        EDGES.put("MyFirstModule.Federation", new String[]{"MyFirstModule.Federation_Admins"});
    }
    private DevelopmentSamples() {}

    private static void authorize(IContext caller) {
        if (!Core.getConfiguration().isInDevelopment() || !unittesting.ConfigurationManager.isLocalDevelopment())
            throw new IllegalStateException("Sample data is available only in the local Studio Pro development runtime.");
        if (caller.getSession() != null && !caller.getSession().getUserRolesNames().contains("Administrator"))
            throw new SecurityException("Only a platform administrator can manage development sample data.");
    }

    public static synchronized void generate(IContext caller) throws Exception {
        authorize(caller);
        transact(ctx -> verifyAndPopulate(ctx, caller));
    }
    public static synchronized void remove(IContext caller) throws Exception {
        authorize(caller);
        transact(DevelopmentSamples::removeIn);
    }
    /** Verify a newly generated set inside the same transaction, then leave the final sample set ready. */
    private static void verifyAndPopulate(IContext ctx, IContext caller) throws Exception {
            if (batch(ctx) != null) {
                extendSeasonAndRoleSamples(ctx);
                Set<Long> once = ids(all(ctx));
                extendSeasonAndRoleSamples(ctx);
                require(once.equals(ids(all(ctx))), "Feature upgrade created duplicate sample records.");
                Core.getLogger("NexioSamples").info("Sample set ready; missing feature examples added without removing existing demo work.");
                return;
            }
            Set<Long> original = ids(all(ctx));
            generateIn(ctx, caller);
            Set<Long> once = ids(all(ctx));
            generateIn(ctx, caller);
            require(once.equals(ids(all(ctx))), "Repeated generation created duplicate records.");
            Set<Long> manifestOwned = decode(batch(ctx).getValue(ctx, "OwnedIds"));
            IMendixObject demoClub = query(ctx, "//MyFirstModule.Club").stream()
                .filter(o -> manifestOwned.contains(o.getId().toLong())).findFirst().orElseThrow();
            IMendixObject extra = Core.instantiate(ctx, "MyFirstModule.ClubEvent");
            extra.setValue(ctx, "Title", "[DEMO] Cleanup dependency check");
            extra.setValue(ctx, "MyFirstModule.ClubEvent_Club", demoClub.getId());
            Core.commitWithoutEvents(ctx, extra);
            IMendixObject protectedEvent = Core.instantiate(ctx, "MyFirstModule.ClubEvent");
            protectedEvent.setValue(ctx, "Title", "Sample cleanup preservation check");
            Core.commitWithoutEvents(ctx, protectedEvent);
            // Exercise the protected-record guard without ever modifying a real pre-existing object.
            IMendixObject manifest = batch(ctx);
            String baselineText = manifest.getValue(ctx, "BaselineIds");
            manifest.setValue(ctx, "BaselineIds", baselineText + protectedEvent.getId().toLong() + "\n");
            Core.commitWithoutEvents(ctx, manifest);
            protectedEvent.setValue(ctx, "MyFirstModule.ClubEvent_Club", demoClub.getId());
            Core.commitWithoutEvents(ctx, protectedEvent);
            boolean blocked = false;
            try { removeIn(ctx); } catch (IllegalStateException expectedFailure) { blocked = true; }
            require(blocked && batch(ctx) != null && ids(all(ctx)).containsAll(once), "Linked existing records were not protected.");
            protectedEvent.setValue(ctx, "MyFirstModule.ClubEvent_Club", null);
            Core.commitWithoutEvents(ctx, protectedEvent);
            manifest.setValue(ctx, "BaselineIds", baselineText);
            Core.commitWithoutEvents(ctx, manifest);
            removeIn(ctx);
            Set<Long> after = ids(all(ctx));
            Set<Long> expected = new HashSet<>(original);
            expected.add(protectedEvent.getId().toLong());
            require(after.equals(expected), "Cleanup did not preserve unrelated records or left sample dependencies.");
            require(batch(ctx) == null, "Cleanup left the ownership manifest.");
            require(Core.deleteWithoutEvents(ctx, Collections.singletonList(protectedEvent), false), "Validation record removal failed.");
            require(original.equals(ids(all(ctx))), "Existing records changed during sample lifecycle verification.");
            generateIn(ctx, caller);
            Core.getLogger("NexioSamples").info("PASS: generate, duplicate prevention, linked-existing-record guard, remove including new dependent, unrelated-record preservation, and regenerate. Samples ready.");
    }

    private interface Work { void run(IContext ctx) throws Exception; }
    private static void transact(Work work) throws Exception {
        IContext ctx = Core.createSystemContext();
        ctx.startTransaction();
        try { work.run(ctx); ctx.endTransaction(); }
        catch (Exception e) { ctx.rollbackTransaction(); throw e; }
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
    private static List<IMendixObject> query(IContext ctx, String xpath) throws Exception {
        return Core.createXPathQuery(xpath).execute(ctx);
    }
    private static IMendixObject first(IContext ctx, String xpath) throws Exception {
        List<IMendixObject> rows = Core.createXPathQuery(xpath).setAmount(1).execute(ctx);
        return rows.isEmpty() ? null : rows.get(0);
    }
    private static IMendixObject batch(IContext ctx) throws Exception {
        return first(ctx, "//" + BATCH + "[Key='" + KEY + "']");
    }
    private static List<IMendixObject> all(IContext ctx) throws Exception {
        List<IMendixObject> result = new ArrayList<>();
        for (String type : TYPES) result.addAll(query(ctx, "//" + type));
        return result;
    }
    private static Set<Long> ids(Collection<IMendixObject> objects) {
        Set<Long> result = new LinkedHashSet<>();
        for (IMendixObject obj : objects) result.add(obj.getId().toLong());
        return result;
    }
    private static String encode(Collection<Long> values) {
        StringBuilder s = new StringBuilder();
        for (Long value : values) s.append(value).append('\n');
        return s.toString();
    }
    private static Set<Long> decode(String value) {
        Set<Long> result = new LinkedHashSet<>();
        if (value != null) for (String id : value.split("\\s+")) if (!id.isEmpty()) result.add(Long.valueOf(id));
        return result;
    }
    private static Set<Long> references(IContext ctx, IMendixObject obj) {
        Set<Long> result = new HashSet<>();
        for (String edge : EDGES.getOrDefault(obj.getType(), new String[0])) {
            Object value = obj.getValue(ctx, edge);
            if (value instanceof IMendixIdentifier) result.add(((IMendixIdentifier)value).toLong());
            else if (value instanceof Collection<?>) for (Object item : (Collection<?>)value)
                if (item instanceof IMendixIdentifier) result.add(((IMendixIdentifier)item).toLong());
        }
        return result;
    }

    /** Add each feature once to either a new batch or an existing batch. Preserve user-edited examples. */
    private static void extendSeasonAndRoleSamples(IContext ctx) throws Exception {
        IMendixObject manifest = batch(ctx);
        require(manifest != null, "Sample ownership manifest is missing.");
        String summary = manifest.getValue(ctx, "Summary");
        if (summary != null && summary.contains(SEASON_ROLE_FEATURE)) return;
        Set<Long> owned = decode(manifest.getValue(ctx, "OwnedIds"));
        Seed s = new Seed(ctx);
        IMendixObject staffAppRole = first(ctx, "//System.UserRole[Name='ClubAdmin']");
        require(staffAppRole != null, "The ClubAdmin app role is missing.");
        int staffNumber = 1000;
        for (IMendixObject club : query(ctx, "//MyFirstModule.Club")) {
            if (!owned.contains(club.getId().toLong())) continue;
            String clubId = s.id(club);
            LocalDate earliest = s.today;
            LocalDate latest = s.today;
            List<IMendixObject> existingSeasons = query(ctx,
                "//MyFirstModule.Season[MyFirstModule.Season_Club=" + clubId + "]");
            for (IMendixObject season : existingSeasons) {
                Date start = season.getValue(ctx, "StartsOn");
                Date end = season.getValue(ctx, "EndsOn");
                if (start != null) {
                    LocalDate day = start.toInstant().atZone(ZoneOffset.UTC).toLocalDate();
                    if (day.isBefore(earliest)) earliest = day;
                }
                if (end != null) {
                    LocalDate day = end.toInstant().atZone(ZoneOffset.UTC).toLocalDate();
                    if (day.isAfter(latest)) latest = day;
                }
            }
            LocalDate pastEnd = earliest.minusDays(1);
            LocalDate laterStart = latest.plusDays(1);
            IMendixObject past = s.make("Season", "Name", "[DEMO] Previous season",
                "StartsOn", utcDate(pastEnd.minusDays(364)), "EndsOn", utcDate(pastEnd), "IsPublished", true);
            s.link(past, "Season_Club", club);
            IMendixObject later = s.make("Season", "Name", "[DEMO] Later planned season",
                "StartsOn", utcDate(laterStart), "EndsOn", utcDate(laterStart.plusDays(364)), "IsPublished", false);
            s.link(later, "Season_Club", club);
            for (IMendixObject type : query(ctx,
                    "//MyFirstModule.MembershipType[MyFirstModule.MembershipType_Club=" + clubId + "]")) {
                if (!owned.contains(type.getId().toLong())) continue;
                BigDecimal amount = type.getValue(ctx, "Fee");
                if (amount == null) amount = BigDecimal.ZERO;
                for (IMendixObject season : Arrays.asList(past, later)) {
                    IMendixObject fee = s.make("SeasonFee", "Amount", season == past ? amount : amount.add(s.fee(10)),
                        "Currency", "EUR", "IsAvailable", true);
                    s.link(fee, "SeasonFee_Club", club);
                    s.link(fee, "SeasonFee_Season", season);
                    s.link(fee, "SeasonFee_MembershipType", type);
                }
            }
            for (boolean secretary : new boolean[]{true, false}) {
                String title = secretary ? "Secretary" : "Competition coordinator";
                IMendixObject role = s.make("ClubRole", "Name", "[DEMO] " + title, "IsActive", true,
                    "CanAdjustFee", false, "Description", "Fictional " + title.toLowerCase(Locale.ROOT) +
                    " role for trying the CRUD permissions matrix. Assigned demo account remains disabled.");
                s.link(role, "ClubRole_Club", club);
                String[] resources = {"Settings", "Members", "Registrations", "MembershipTypes", "Seasons",
                    "Forms", "Events", "News", "Invoices", "Competition"};
                for (String resource : resources) {
                    boolean supportsCreate = !Arrays.asList("Settings", "Members", "Registrations").contains(resource);
                    boolean supportsDelete = !"Settings".equals(resource);
                    boolean manages = secretary ? "Registrations".equals(resource) : "Competition".equals(resource);
                    boolean reads = manages || "Members".equals(resource) ||
                        (secretary ? "Invoices".equals(resource) : "Events".equals(resource));
                    IMendixObject permission = s.make("ClubPermission", "Resource", resource,
                        "SupportsCreate", supportsCreate, "SupportsDelete", supportsDelete,
                        "CanRead", reads, "CanCreate", manages && supportsCreate,
                        "CanUpdate", manages, "CanDelete", manages && supportsDelete);
                    s.link(permission, "ClubPermission_ClubRole", role);
                }
                IMendixObject person = s.person("[DEMO] " + title + " — " + club.getValue(ctx, "Name"), ++staffNumber);
                // Only new, disabled, manifest-owned test accounts receive this test role.
                person.setValue(ctx, "System.UserRoles", Arrays.asList(s.ordinaryRole.getId(), staffAppRole.getId()));
                Core.commitWithoutEvents(ctx, person);
                IMendixObject staff = s.make("ClubStaff", "MemberName", person.getValue(ctx, "FullName"),
                    "Email", person.getValue(ctx, "Email"), "IsActive", true);
                s.link(staff, "ClubStaff_ClubRole", role);
                s.link(staff, "ClubStaff_Person", person);
            }
        }
        owned.addAll(ids(s.owned));
        manifest.setValue(ctx, "OwnedIds", encode(owned));
        manifest.setValue(ctx, "Summary", (summary == null ? "" : summary) +
            " Added previous/later seasons and their fees, secretary/coordinator roles, CRUD permissions and disabled staff. " + SEASON_ROLE_FEATURE);
        Core.commitWithoutEvents(ctx, manifest);
        Core.getLogger("NexioSamples").info("Added " + s.owned.size() + " owned season/role examples. Existing sample work preserved.");
    }

    private static Date utcDate(LocalDate day) {
        return Date.from(day.atStartOfDay().toInstant(ZoneOffset.UTC));
    }
    private static void removeIn(IContext ctx) throws Exception {
        IMendixObject manifest = batch(ctx);
        if (manifest == null) return;
        Set<Long> baseline = decode(manifest.getValue(ctx, "BaselineIds"));
        Set<Long> owned = decode(manifest.getValue(ctx, "OwnedIds"));
        List<IMendixObject> current = all(ctx);
        Set<Long> present = ids(current);
        owned.retainAll(present);
        // Grow towards dependent records only. Parents and unrelated accounts are never pulled into deletion.
        boolean changed;
        do {
            changed = false;
            for (IMendixObject obj : current) {
                long id = obj.getId().toLong();
                if (!"MyFirstModule.Club".equals(obj.getType()) && !"MyFirstModule.Federation".equals(obj.getType())
                    && !"UserManagement.Person".equals(obj.getType()) && !baseline.contains(id) && !owned.contains(id)
                    && !Collections.disjoint(references(ctx, obj), owned)) {
                    owned.add(id);
                    changed = true;
                }
            }
        } while (changed);
        for (IMendixObject obj : current) {
            if (!owned.contains(obj.getId().toLong()) && !Collections.disjoint(references(ctx, obj), owned))
                throw new IllegalStateException("Removal stopped: an existing record is linked to sample data. Unlink it first; no records have been removed.");
        }
        List<IMendixObject> deletion = new ArrayList<>();
        for (IMendixObject obj : current) if (owned.contains(obj.getId().toLong())) deletion.add(obj);
        // Every business association keeps referenced objects. Disable events to avoid billing/mail side effects.
        require(Core.deleteWithoutEvents(ctx, deletion, false), "Sample removal was not completed.");
        require(Collections.disjoint(ids(all(ctx)), owned), "Sample records remain; removal rolled back.");
        require(Core.deleteWithoutEvents(ctx, Collections.singletonList(manifest), false), "Sample manifest removal failed.");
        Core.getLogger("NexioSamples").info("Removed " + deletion.size() + " owned sample records; existing records preserved.");
    }

    private static final class Seed {
        final IContext ctx;
        final List<IMendixObject> owned = new ArrayList<>();
        final String marker = UUID.randomUUID().toString().substring(0, 8);
        final LocalDate today = LocalDate.now(ZoneOffset.UTC);
        final String season = "Demo " + today.getYear() + "/" + (today.getYear()+1);
        final IMendixObject ordinaryRole;
        Seed(IContext ctx) throws Exception {
            this.ctx = ctx;
            ordinaryRole = first(ctx, "//System.UserRole[Name='User']");
            require(ordinaryRole != null, "The ordinary User app role is missing.");
        }
        IMendixObject make(String type, Object... values) throws Exception {
            IMendixObject obj = Core.instantiate(ctx, type.contains(".") ? type : "MyFirstModule." + type);
            for (int i=0;i<values.length;i+=2) obj.setValue(ctx, (String)values[i], values[i+1]);
            Core.commitWithoutEvents(ctx, obj);
            owned.add(obj);
            return obj;
        }
        void link(IMendixObject obj, String edge, IMendixObject target) throws Exception {
            obj.setValue(ctx, "MyFirstModule." + edge, target.getId());
            Core.commitWithoutEvents(ctx, obj);
        }
        Date date(int days) { return Date.from(today.plusDays(days).atStartOfDay().toInstant(ZoneOffset.UTC)); }
        Date hour(int days, int hour) { return Date.from(today.plusDays(days).atTime(hour,0).toInstant(ZoneOffset.UTC)); }
        BigDecimal fee(int n) { return BigDecimal.valueOf(n); }
        String id(IMendixObject obj) { return Long.toString(obj.getId().toLong()); }
        IMendixObject person(String name, int number) throws Exception {
            String email = "demo." + marker + "." + number + "@example.invalid";
            return make("UserManagement.Person", "Name",email,"Email",email,"FullName",name,
                "Active",false,"WebServiceUser",false,"Password",UUID.randomUUID().toString()+"aA9!",
                "System.UserRoles",Collections.singletonList(ordinaryRole.getId()),"InvitationPending",false);
        }
        IMendixObject membership(IMendixObject person, IMendixObject club, IMendixObject type, IMendixObject period) throws Exception {
            IMendixObject membership = make("Membership", "MemberName",person.getValue(ctx,"FullName"),
                "MemberEmail",person.getValue(ctx,"Email"),"Status","Active","JoinedAt",date(-20),
                "SeasonName",season,"RegistrationKey",id(person)+":"+id(club)+":"+id(type)+":"+id(period));
            link(membership,"Membership_Person",person); link(membership,"Membership_Club",club);
            link(membership,"Membership_MembershipType",type); link(membership,"Membership_Season",period);
            return membership;
        }
        void invoice(IMendixObject person, IMendixObject club, IMendixObject member, int amount, int number) throws Exception {
            IMendixObject invoice = make("Invoice","Number","DEMO-"+marker+"-"+number,"Description","[DEMO] Full season membership",
                "Amount",fee(amount),"Currency","EUR","Status","Open","DueDate",date(21),"IssuedAt",date(0),
                "RecipientName",person.getValue(ctx,"FullName"),"RecipientEmail",person.getValue(ctx,"Email"),
                "IssuerName",club.getValue(ctx,"Name"),"Kind","Member","BrandTone","Blue","LayoutPreset","Modern",
                "HeaderText","Development example","FooterText","Sample invoice — do not pay.",
                "PaymentInstructions","Development sample only. No real payment is requested.","BillingKey","DEMO:"+marker+":"+number);
            link(invoice,"Invoice_Person",person); link(invoice,"Invoice_Club",club); link(invoice,"Invoice_Membership",member);
        }
    }

    private static void generateIn(IContext ctx, IContext caller) throws Exception {
        if (batch(ctx) != null) { Core.getLogger("NexioSamples").info("Sample data already exists; generation skipped."); return; }
        Set<Long> baseline = ids(all(ctx));
        Seed s = new Seed(ctx);
        IMendixObject fed = s.make("Federation","Name","[DEMO] Nexio Badminton Federation",
            "Description","Fictional local-development federation","IsActive",true,"Currency","EUR",
            "CensusMonth",1L,"CensusDay",1L,"BillingMethod","ACTIVE_AT_CENSUS");
        IMendixObject fedType = s.make("FederationType","Name","[DEMO] Competition player","Description","Annual federation competition membership","IsAvailable",true);
        s.link(fedType,"FederationType_Federation",fed);
        IMendixObject rate = s.make("FederationRate","Year",(long)s.today.getYear(),"AnnualFee",s.fee(15),"Currency","EUR",
            "RateKey",s.id(fedType)+":"+s.today.getYear());
        s.link(rate,"FederationRate_FederationType",fedType);
        IMendixObject league = s.make("CompetitionLeague","Name","[DEMO] Ready to schedule","SeasonName",s.season,
            "StartsOn",s.date(14),"EndsOn",s.date(105),"RegistrationOpens",s.date(-25),"RegistrationCloses",s.date(-1),
            "ChangesOpen",s.date(0),"ChangesClose",s.date(100),"MaxTeamsPerPoule",4L,"RequestedPoules",2L,
            "DurationMinutes",90L,"Format","DoubleRoundRobin","Stage","Registration",
            "Info","Eight eligible teams. Allocate poules, generate fixtures, plan and publish.");
        s.link(league,"CompetitionLeague_Federation",fed);
        IMendixObject openLeague = s.make("CompetitionLeague","Name","[DEMO] Open registration","SeasonName",s.season,
            "StartsOn",s.date(14),"EndsOn",s.date(105),"RegistrationOpens",s.date(-1),"RegistrationCloses",s.date(10),
            "ChangesOpen",s.date(0),"ChangesClose",s.date(100),"MaxTeamsPerPoule",4L,"RequestedPoules",1L,
            "DurationMinutes",90L,"Format","SingleRoundRobin","Stage","Registration",
            "Info","Register each club's third team here, then close registration before allocation.");
        s.link(openLeague,"CompetitionLeague_Federation",fed);
        String[] names = {"Northside Shuttle","Riverside Rackets","Parkside Smash","Lakeside Aces"};
        String[] cities = {"Leuven","Mechelen","Antwerp","Brussels"};
        int personNumber=0;
        IMendixObject currentPerson = null;
        if (caller.getSession()!=null) {
            IMendixObject user = caller.getSession().getUser(caller).getMendixObject();
            if ("UserManagement.Person".equals(user.getType())) currentPerson=user;
        }
        for(int c=0;c<4;c++) {
            int fee=80+10*c;
            IMendixObject club = s.make("Club","Name","[DEMO] "+names[c],"Description","Fictional badminton club for trying Nexio.",
                "City",cities[c],"Address","Demo Sports Centre "+(c+1),"PostalCode","0000",
                "LocationVerified",false,"IsPublished",true,
                "RequiresApproval",true,"RequiresPayment",true,"BillingEmail","demo."+s.marker+".club"+c+"@example.invalid");
            IMendixObject period = s.make("Season","Name",s.season,"StartsOn",s.date(-30),"EndsOn",s.date(130),"IsPublished",true);
            s.link(period,"Season_Club",club);
            IMendixObject type = s.make("MembershipType","Name","Competition player","Description","[DEMO] Club competition membership",
                "Fee",s.fee(fee),"Currency","EUR","IsAvailable",true,"IsCompetitionPlayer",true);
            s.link(type,"MembershipType_Club",club); s.link(type,"MembershipType_FederationType",fedType);
            IMendixObject seasonFee = s.make("SeasonFee","Amount",s.fee(fee),"Currency","EUR","IsAvailable",true);
            s.link(seasonFee,"SeasonFee_Club",club); s.link(seasonFee,"SeasonFee_Season",period); s.link(seasonFee,"SeasonFee_MembershipType",type);
            IMendixObject nextPeriod = s.make("Season","Name",s.season+" next","StartsOn",s.date(131),"EndsOn",s.date(495),"IsPublished",true);
            s.link(nextPeriod,"Season_Club",club);
            IMendixObject nextFee = s.make("SeasonFee","Amount",s.fee(fee+5),"Currency","EUR","IsAvailable",true);
            s.link(nextFee,"SeasonFee_Club",club); s.link(nextFee,"SeasonFee_Season",nextPeriod); s.link(nextFee,"SeasonFee_MembershipType",type);
            IMendixObject affiliation=s.make("FederationAffiliation","ClubName",club.getValue(ctx,"Name"),
                "StartsOn",s.date(-30),"EndsOn",s.date(130),"IsActive",true);
            s.link(affiliation,"FederationAffiliation_Club",club); s.link(affiliation,"FederationAffiliation_Federation",fed);
            IMendixObject venue=s.make("CompetitionVenue","Name","[DEMO] "+names[c]+" hall","Address","Demo Sports Centre "+(c+1),"IsActive",true);
            venue.setValue(ctx,"VenueKey",s.id(venue)); Core.commitWithoutEvents(ctx,venue);
            s.link(venue,"CompetitionVenue_Club",club);
            IMendixObject policy=s.make("ClubCompetitionPolicy","MaxHostedPerDay",3L,"MaxConcurrent",2L,"ClubKey",s.id(club));
            s.link(policy,"ClubCompetitionPolicy_Club",club);
            for(int week=0;week<13;week++) for(int evening=0;evening<2;evening++) {
                int day=14+week*7+evening*3;
                IMendixObject slot=s.make("VenueAvailability","StartsAt",s.hour(day,18),"EndsAt",s.hour(day,22),"Capacity",2L,
                    "VenueName",venue.getValue(ctx,"Name"),"ClubKey",s.id(club),"VenueKey",s.id(venue),"IsActive",true);
                s.link(slot,"VenueAvailability_Venue",venue); s.link(slot,"VenueAvailability_Club",club);
            }
            IMendixObject news=s.make("NewsItem","Title","[DEMO] Welcome to the season","Body","Try registrations, club administration and competition planning with this fictional club.",
                "PublishedAt",s.date(-1),"IsImportant",true,"IsPublished",true);
            s.link(news,"NewsItem_Club",club);
            IMendixObject event=s.make("ClubEvent","Title","[DEMO] Season opening evening","Description","Friendly games at the sample club.",
                "StartsAt",s.hour(7,18),"Location",venue.getValue(ctx,"Name"),"IsPublished",true);
            s.link(event,"ClubEvent_Club",club);
            IMendixObject field=s.make("FormField","Label","Emergency contact","HelpText","Development example; use fictional details.",
                "Kind","Text","IsRequired",true,"SortOrder",1L,"IsActive",true);
            s.link(field,"FormField_Club",club);
            for(int t=0;t<3;t++) {
                IMendixObject team=s.make("CompetitionTeam","Name","[DEMO] "+names[c]+" "+(t+1),
                    "ClubName",club.getValue(ctx,"Name"),"IsActive",true,"SeasonName",s.season);
                s.link(team,"CompetitionTeam_Club",club);
                for(int p=0;p<4;p++) {
                    IMendixObject person=s.person("[DEMO] Player "+(++personNumber),personNumber);
                    IMendixObject member=s.membership(person,club,type,period);
                    IMendixObject player=s.make("CompetitionPlayer","MemberName",person.getValue(ctx,"FullName"),
                        "MemberKey",s.id(team)+":"+s.id(person),"PersonKey",s.id(person));
                    s.link(player,"CompetitionPlayer_Membership",member); s.link(player,"CompetitionPlayer_Team",team);
                    IMendixObject registration=s.make("FederationRegistration","MemberName",person.getValue(ctx,"FullName"),
                        "ClubName",club.getValue(ctx,"Name"),"TypeName",fedType.getValue(ctx,"Name"),"JoinedAt",s.date(-20),
                        "SeasonStartsOn",s.date(-30),"SeasonEndsOn",s.date(130),"Status","Active",
                        "MemberKey",s.id(person),"RegistrationKey",s.id(member)+":"+s.id(fedType));
                    s.link(registration,"FederationRegistration_Membership",member); s.link(registration,"FederationRegistration_Club",club);
                    s.link(registration,"FederationRegistration_FederationType",fedType); s.link(registration,"FederationRegistration_Federation",fed);
                    if(t==0 && p==0) s.invoice(person,club,member,fee,c+1);
                }
                if(t<2) {
                    IMendixObject entry=s.make("LeagueEntry","TeamName",team.getValue(ctx,"Name"),"ClubName",club.getValue(ctx,"Name"),
                        "ClubKey",s.id(club),"EntryKey",s.id(team)+":"+s.id(fed)+":"+s.season,"Order",(long)(c*2+t+1));
                    s.link(entry,"LeagueEntry_Team",team); s.link(entry,"LeagueEntry_Club",club); s.link(entry,"LeagueEntry_League",league);
                }
            }
            IMendixObject applicant=s.person("[DEMO] Applicant "+(c+1),++personNumber);
            IMendixObject request=s.make("RegistrationRequest","MemberName",applicant.getValue(ctx,"FullName"),
                "MemberEmail",applicant.getValue(ctx,"Email"),"SubmittedAt",s.date(-1),"Approval","Pending","Payment","AwaitingApproval",
                "Status","Pending","Fee",s.fee(fee),"OriginalFee",s.fee(fee),"Currency","EUR",
                "RequiresApproval",true,"RequiresPayment",true,"NextAction","Awaiting club approval","SeasonName",s.season,
                "RegistrationKey",s.id(applicant)+":"+s.id(club)+":"+s.id(type)+":"+s.id(period));
            s.link(request,"RegistrationRequest_Person",applicant); s.link(request,"RegistrationRequest_Club",club);
            s.link(request,"RegistrationRequest_MembershipType",type); s.link(request,"RegistrationRequest_Season",period);
            s.link(request,"RegistrationRequest_SeasonFee",seasonFee);
            IMendixObject answer=s.make("FormResponse","Label","Emergency contact","Kind","Text","IsRequired",true,"SortOrder",1L,"Answer","[DEMO] Example guardian");
            s.link(answer,"FormResponse_FormField",field); s.link(answer,"FormResponse_RegistrationRequest",request);
            if(currentPerson!=null && c<2) {
                IMendixObject ownMembership=s.membership(currentPerson,club,type,period);
                s.invoice(currentPerson,club,ownMembership,fee,10+c);
            }
        }
        IMendixObject manifest = Core.instantiate(ctx,BATCH);
        manifest.setValue(ctx,"Key",KEY); manifest.setValue(ctx,"BaselineIds",encode(baseline));
        manifest.setValue(ctx,"OwnedIds",encode(ids(s.owned))); manifest.setValue(ctx,"CreatedAt",new Date());
        manifest.setValue(ctx,"Summary","4 clubs, 1 federation, 52 disabled people, 12 teams, 2 leagues, 104 venue slots, seasons, registrations, news, events and unpaid invoices.");
        Core.commitWithoutEvents(ctx,manifest);
        extendSeasonAndRoleSamples(ctx);
        Core.getLogger("NexioSamples").info("Generated "+s.owned.size()+" sample records. All demo accounts disabled; no email or verified payment.");
    }
}

