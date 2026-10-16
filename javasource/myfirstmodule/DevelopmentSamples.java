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
    private static final String MATCH_FEATURE = "features:match-registration-v1";
    private static final String MATCH_REVIEW_FEATURE = "features:match-review-v2";
    private static final String DISCOVERY_FEATURE = "features:belgian-club-discovery-v1";
    private static final String TEST_ADMIN_FEATURE = "features:organization-test-admins-v1";
    private static final String TEST_ADMIN_PASSWORD = "TestAccount123!";
    private static final String USER_VIEW_FEATURE = "features:administrator-user-view-v1";
    private static final String DIRECTORY_PROFILE_FEATURE = "features:club-directory-profiles-v2";
    private static final String REGISTRATION_PROGRESS_FEATURE = "features:member-registration-progress-v1";
    private static final String MEMBERSHIP_AUDIENCE_FEATURE = "features:membership-audience-v1";
    private static final String MULTI_AFFILIATION_FEATURE = "features:multi-affiliation-and-account-overview-v1";
    private static final String[] DIRECTORY_TYPES = {"MyFirstModule.ClubDirectoryCity", "MyFirstModule.AffiliationMutationClaim"};
    private static final String[] MATCH_TYPES = {"MyFirstModule.MatchFormat", "MyFirstModule.MatchSlot", "MyFirstModule.MatchRegistration", "MyFirstModule.MatchContest", "MyFirstModule.MatchSet", "MyFirstModule.MatchLineup", "MyFirstModule.MatchAudit", "MyFirstModule.MatchEligibility"};
    private static final String[] TYPES = {"MyFirstModule.PaymentVerification","MyFirstModule.Invoice","MyFirstModule.NewsItem","MyFirstModule.ClubEvent","MyFirstModule.FormResponse","MyFirstModule.FormField","MyFirstModule.RegistrationRequest","MyFirstModule.Membership","MyFirstModule.MembershipType","MyFirstModule.Club","MyFirstModule.FeeAdjustment","MyFirstModule.ClubStaff","MyFirstModule.ClubPermission","MyFirstModule.ClubRole","MyFirstModule.SeasonFee","MyFirstModule.Season","MyFirstModule.InvoiceMail","MyFirstModule.RenewalOffer","MyFirstModule.InvoiceLine","MyFirstModule.BillingPlan","MyFirstModule.ClubBillingPolicy","MyFirstModule.InvoiceStyle","MyFirstModule.FederationRegistration","MyFirstModule.FederationAffiliation","MyFirstModule.FederationRate","MyFirstModule.FederationType","MyFirstModule.Federation","MyFirstModule.InvitationClaim","MyFirstModule.AccountInvitation","MyFirstModule.BillingClaim","MyFirstModule.CompetitionAudit","MyFirstModule.FederationCompetitionDelegate","MyFirstModule.FixtureChange","MyFirstModule.CompetitionFixture","MyFirstModule.ClubCompetitionPolicy","MyFirstModule.VenueAvailability","MyFirstModule.CompetitionVenue","MyFirstModule.CompetitionPoule","MyFirstModule.LeagueEntry","MyFirstModule.CompetitionPlayer","MyFirstModule.CompetitionTeam","MyFirstModule.CompetitionLeague","UserManagement.Person"};
    private static final Map<String, String[]> EDGES = new LinkedHashMap<>();
    static {
        EDGES.put("MyFirstModule.MatchFormat", new String[]{"MyFirstModule.MatchFormat_Federation"});
        EDGES.put("MyFirstModule.MatchSlot", new String[]{"MyFirstModule.MatchSlot_Format", "MyFirstModule.MatchSlot_ScoringOverride"});
        EDGES.put("MyFirstModule.MatchRegistration", new String[]{"MyFirstModule.MatchRegistration_Fixture", "MyFirstModule.MatchRegistration_Format"});
        EDGES.put("MyFirstModule.MatchContest", new String[]{"MyFirstModule.MatchContest_Registration", "MyFirstModule.MatchContest_Format"});
        EDGES.put("MyFirstModule.MatchSet", new String[]{"MyFirstModule.MatchSet_Contest"});
        EDGES.put("MyFirstModule.MatchLineup", new String[]{"MyFirstModule.MatchLineup_Contest", "MyFirstModule.MatchLineup_Player1", "MyFirstModule.MatchLineup_Player2"});
        EDGES.put("MyFirstModule.MatchAudit", new String[]{"MyFirstModule.MatchAudit_Registration", "MyFirstModule.MatchAudit_Federation", "MyFirstModule.MatchAudit_League"});
        EDGES.put("MyFirstModule.MatchEligibility", new String[]{"MyFirstModule.MatchEligibility_Federation", "MyFirstModule.MatchEligibility_Player"});
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
        EDGES.put("MyFirstModule.CompetitionLeague", new String[]{"MyFirstModule.CompetitionLeague_Federation", "MyFirstModule.CompetitionLeague_MatchFormat"});
        EDGES.put("MyFirstModule.Club", new String[]{"MyFirstModule.Club_Admins", "MyFirstModule.Club_DirectoryCity"});
        EDGES.put("MyFirstModule.ClubDirectoryCity", new String[0]);
        EDGES.put("MyFirstModule.AffiliationMutationClaim", new String[]{"MyFirstModule.AffiliationMutationClaim_Club"});
        EDGES.put("MyFirstModule.Federation", new String[]{"MyFirstModule.Federation_Admins"});
    }
    private DevelopmentSamples() {}

    private static void authorize(IContext caller) {
        if (!Core.getConfiguration().isInDevelopment() || !unittesting.ConfigurationManager.isLocalDevelopment())
            throw new IllegalStateException("Sample data is available only in the local Studio Pro development runtime.");
        if (caller.getSession() == null || !caller.getSession().getUserRolesNames().contains("Administrator"))
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
    /** Private developer verification. Every database change is rolled back, including cleanup. */
    static synchronized String verifyMatchLifecycle(IContext caller) throws Exception {
        authorize(caller);
        IContext ctx=Core.createSystemContext();ctx.startTransaction();
        try {
            Set<Long> baseline=ids(all(ctx));IMendixObject existing=batch(ctx);
            Map<Long,String> before=new HashMap<>();for(IMendixObject o:all(ctx))before.put(o.getId().toLong(),primitiveSnapshot(ctx,o));
            if(existing!=null){extendMatchSamples(ctx);extendDiscoveryAndAdminSamples(ctx);Set<Long> once=ids(all(ctx));extendMatchSamples(ctx);extendDiscoveryAndAdminSamples(ctx);require(once.equals(ids(all(ctx))),"Existing-batch feature upgrade is not idempotent.");for(IMendixObject o:all(ctx))if(before.containsKey(o.getId().toLong()))require(before.get(o.getId().toLong()).equals(primitiveSnapshot(ctx,o)),"Existing business record was overwritten by sample upgrade.");}
            // Hide only the registry within this rollback-only transaction to exercise a fresh batch.
            if(existing!=null)Core.deleteWithoutEvents(ctx,Collections.singletonList(existing),false);
            generateIn(ctx,caller);Set<Long> fresh=ids(all(ctx));extendMatchSamples(ctx);generateIn(ctx,caller);require(fresh.equals(ids(all(ctx))),"Fresh generation is not idempotent.");
            IMendixObject registry=batch(ctx);Set<Long> owned=decode(registry.getValue(ctx,"OwnedIds"));
            Set<Long> added=new HashSet<>(fresh);added.removeAll(baseline);require(owned.containsAll(added),"A generated business record is absent from the ownership manifest.");
            verifyMembershipAudience(ctx, owned);
            verifyMultiAffiliation(ctx, owned);
            verifySampleAccounts(ctx, owned);
            removeIn(ctx);require(baseline.equals(ids(all(ctx))),"Sample cleanup changed pre-existing business records.");
            return "PASS: fresh generation, repeated generation, manifest coverage, scoped test administrators, disabled ordinary sample accounts and scoped cleanup (rolled back). "+(existing==null?"Existing-batch upgrade simulated by repeated extension.":"Existing-batch upgrade and preservation of all pre-existing primitive values verified.");
        } finally {ctx.rollbackTransaction();}
    }
    private static String primitiveSnapshot(IContext ctx,IMendixObject o){List<String> values=new ArrayList<>();for(com.mendix.systemwideinterfaces.core.meta.IMetaPrimitive a:o.getMetaObject().getMetaPrimitives())values.add(a.getName()+"="+String.valueOf(o.getValue(ctx,a.getName())));Collections.sort(values);return String.join(";",values);}
    /** Verify a newly generated set inside the same transaction, then leave the final sample set ready. */
    private static void verifyAndPopulate(IContext ctx, IContext caller) throws Exception {
            if (batch(ctx) != null) {
                extendSeasonAndRoleSamples(ctx);
                extendMatchSamples(ctx);
                extendDiscoveryAndAdminSamples(ctx);
                Set<Long> once = ids(all(ctx));
                extendSeasonAndRoleSamples(ctx);
                extendMatchSamples(ctx);
                extendDiscoveryAndAdminSamples(ctx);
                require(once.equals(ids(all(ctx))), "Feature upgrade created duplicate sample records.");
                Core.getLogger("NexioSamples").info("Sample set ready; missing feature examples added without removing existing demo work.");
                return;
            }
            Set<Long> original = ids(all(ctx));
            generateIn(ctx, caller);
            Set<Long> once = ids(all(ctx));
            extendMatchSamples(ctx);
            require(once.equals(ids(all(ctx))), "Match feature upgrade was not idempotent for a fresh batch.");
            generateIn(ctx, caller);
            require(once.equals(ids(all(ctx))), "Repeated generation created duplicate records.");
            Set<Long> manifestOwned = decode(batch(ctx).getValue(ctx, "OwnedIds"));
            verifyMembershipAudience(ctx, manifestOwned);
            verifyMultiAffiliation(ctx, manifestOwned);
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
        for (String type : DIRECTORY_TYPES) result.addAll(query(ctx, "//" + type));
        for (String type : TYPES) result.addAll(query(ctx, "//" + type));
        for (String type : MATCH_TYPES) result.addAll(query(ctx, "//" + type));
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
                    "Description", "Fictional " + title.toLowerCase(Locale.ROOT) +
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

    /** Add isolated feature examples; never attach new formats to user-edited existing divisions. */
    private static void extendMatchSamples(IContext ctx) throws Exception {
        IMendixObject manifest = batch(ctx);
        require(manifest != null, "Sample ownership manifest is missing.");
        String summary = manifest.getValue(ctx, "Summary");
        if (summary != null && summary.contains(MATCH_FEATURE)) { extendMatchReviewSamples(ctx); return; }
        Set<Long> owned = decode(manifest.getValue(ctx, "OwnedIds"));
        IMendixObject fed = query(ctx, "//MyFirstModule.Federation").stream().filter(o -> owned.contains(o.getId().toLong())).findFirst().orElseThrow();
        List<IMendixObject> teams = query(ctx, "//MyFirstModule.CompetitionTeam");
        teams.removeIf(o -> !owned.contains(o.getId().toLong()));
        require(teams.size() >= 2, "Two manifest-owned demo teams are needed for match examples.");
        IMendixIdentifier homeClub = teams.get(0).getValue(ctx, "MyFirstModule.CompetitionTeam_Club");
        IMendixObject homeTeam = teams.get(0);
        IMendixObject awayTeam = teams.stream().filter(t -> !Objects.equals(homeClub, t.getValue(ctx, "MyFirstModule.CompetitionTeam_Club"))).findFirst().orElseThrow(() -> new IllegalStateException("Match examples need owned demo teams from two clubs."));
        teams = Arrays.asList(homeTeam, awayTeam);
        Seed s = new Seed(ctx);
        String[] sports = {"Badminton", "Tennis", "Padel", "Table tennis", "Football"};
        for (int sport = 0; sport < sports.length; sport++) {
            IMendixObject format = s.make("MatchFormat");
            MatchRegistrationService.defaults(ctx, format);
            format.setValue(ctx, "Name", "[DEMO] " + sports[sport] + " format");
            format.setValue(ctx, "Sport", sports[sport]);
            format.setValue(ctx, "AvailableFrom", s.date(-60));
            format.setValue(ctx, "AvailableUntil", s.date(400));
            format.setValue(ctx, "Scoring", sport == 1 || sport == 2 ? "Games" : sport == 4 ? "Total" : "Rally");
            if (sport == 3) { format.setValue(ctx, "Target", 11L); format.setValue(ctx, "Cap", 0L); format.setValue(ctx, "SetsToWin", 3L); }
            if (sport == 1 || sport == 2) format.setValue(ctx, "DecidingTarget", 10L);
            if (sport == 4) { format.setValue(ctx, "WinPoints", s.fee(3)); format.setValue(ctx, "CheckIndex", false); }
            Core.commitWithoutEvents(ctx, format); s.link(format, "MatchFormat_Federation", fed);
            int count = sport == 0 ? 8 : 1;
            List<IMendixObject> slots = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                IMendixObject slot = s.make("MatchSlot", "Name", sport == 0 ? new String[]{"HD", "DD", "HE1", "DE1", "HE2", "DE2", "GD1", "GD2"}[i] : "Match",
                    "Sequence", i + 1, "PlayersPerSide", sport == 4 ? 0 : sport == 2 || (sport == 0 && (i < 2 || i > 5)) ? 2 : 1,
                    "Discipline", sport == 0 ? i == 0 || i == 2 || i == 4 ? "Men" : i == 1 || i == 3 || i == 5 ? "Women" : "Mixed" : "Open");
                s.link(slot, "MatchSlot_Format", format); slots.add(slot);
            }
            IMendixObject league = s.make("CompetitionLeague", "Name", "[DEMO] " + sports[sport] + " result registration", "SeasonName", s.season + " match examples " + sport,
                "StartsOn", s.date(-7), "EndsOn", s.date(90), "RegistrationOpens", s.date(-30), "RegistrationCloses", s.date(-10),
                "ChangesOpen", s.date(-7), "ChangesClose", s.date(80), "MaxTeamsPerPoule", 4L, "DurationMinutes", 90L,
                "Format", "SingleRoundRobin", "Stage", "Published", "Info", "Fictional match-registration examples. Accounts stay disabled; no player ranking is calculated.");
            s.link(league, "CompetitionLeague_Federation", fed); s.link(league, "CompetitionLeague_MatchFormat", format);
            IMendixObject poule = s.make("CompetitionPoule", "Name", "[DEMO] Subdivision A", "Number", 1L, "TeamCount", 2L); s.link(poule, "CompetitionPoule_League", league);
            List<IMendixObject> entries = new ArrayList<>();
            List<IMendixObject> clubs = new ArrayList<>();
            List<List<IMendixObject>> players = new ArrayList<>();
            for (int side = 0; side < 2; side++) {
                IMendixObject team = teams.get(side);
                IMendixIdentifier clubId = team.getValue(ctx, "MyFirstModule.CompetitionTeam_Club");
                IMendixObject club = Core.retrieveId(ctx, clubId); clubs.add(club);
                IMendixObject entry = s.make("LeagueEntry", "TeamName", team.getValue(ctx, "Name"), "ClubName", club.getValue(ctx, "Name"),
                    "ClubKey", s.id(club), "EntryKey", "DEMO-MATCH:" + s.marker + ":" + sport + ":" + side, "Order", (long) side + 1);
                s.link(entry, "LeagueEntry_Team", team); s.link(entry, "LeagueEntry_Club", club); s.link(entry, "LeagueEntry_League", league); s.link(entry, "LeagueEntry_Poule", poule); entries.add(entry);
                List<IMendixObject> roster = query(ctx, "//MyFirstModule.CompetitionPlayer[MyFirstModule.CompetitionPlayer_Team=" + s.id(team) + "]"); players.add(roster);
                if (sport == 0) for (int p = 0; p < roster.size(); p++) {
                    IMendixObject player = roster.get(p);
                    IMendixObject fact = s.make("MatchEligibility", "PlayerKey", player.getValue(ctx, "PersonKey"), "Discipline", p % 2 == 0 ? "Men" : "Women",
                        "Suspended", p == 3 && side == 1, "Index", s.fee(8 + p), "StartsOn", s.date(-30), "EndsOn", s.date(130), "Reason", "Fictional eligibility warning scenario");
                    s.link(fact, "MatchEligibility_Federation", fed); s.link(fact, "MatchEligibility_Player", player);
                }
            }
            IMendixObject fixture = s.make("CompetitionFixture", "HomeName", entries.get(0).getValue(ctx, "TeamName"), "AwayName", entries.get(1).getValue(ctx, "TeamName"),
                "HomeKey", s.id(entries.get(0)), "AwayKey", s.id(entries.get(1)), "HomeClubKey", s.id(clubs.get(0)), "AwayClubKey", s.id(clubs.get(1)),
                "StartsAt", s.hour(-1, 18), "EndsAt", s.hour(-1, 20), "Status", "Published", "Version", 1L, "PairKey", "DEMO-MATCH:" + s.marker + ":" + sport, "Leg", 1L);
            s.link(fixture, "CompetitionFixture_League", league); s.link(fixture, "CompetitionFixture_Poule", poule);
            s.link(fixture, "CompetitionFixture_Home", entries.get(0)); s.link(fixture, "CompetitionFixture_Away", entries.get(1));
            s.link(fixture, "CompetitionFixture_HomeClub", clubs.get(0)); s.link(fixture, "CompetitionFixture_AwayClub", clubs.get(1));
            IMendixObject registration = s.make("MatchRegistration", "State", "Draft", "Revision", 1);
            s.link(registration, "MatchRegistration_Fixture", fixture); s.link(registration, "MatchRegistration_Format", format);
            for (int i = 0; i < slots.size(); i++) {
                IMendixObject slot = slots.get(i);
                IMendixObject contest = s.make("MatchContest", "Name", slot.getValue(ctx, "Name"), "Sequence", i + 1, "PlayersPerSide", slot.getValue(ctx, "PlayersPerSide"),
                    "Discipline", slot.getValue(ctx, "Discipline"), "Outcome", "Pending", "Revision", 1);
                s.link(contest, "MatchContest_Registration", registration); s.link(contest, "MatchContest_Format", format);
                for (int side = 0; side < 2; side++) {
                    IMendixObject lineup = s.make("MatchLineup", "Side", side == 0 ? "Home" : "Away", "Revision", 1);
                    s.link(lineup, "MatchLineup_Contest", contest);
                    int n = ((Number) slot.getValue(ctx, "PlayersPerSide")).intValue();
                    for (int p = 0; p < n && p < players.get(side).size(); p++) {
                        IMendixObject player = players.get(side).get((i + p) % players.get(side).size());
                        lineup.setValue(ctx, "Player" + (p + 1) + "Name", player.getValue(ctx, "MemberName")); s.link(lineup, "MatchLineup_Player" + (p + 1), player);
                    }
                }
                if (sport == 0 || sport == 1 || sport == 2 || sport == 3 || sport == 4) {
                    int home = sport == 1 || sport == 2 ? 6 : sport == 3 ? 11 : sport == 4 ? 2 : 21;
                    int away = sport == 1 || sport == 2 ? 4 : sport == 3 ? 7 : sport == 4 ? 1 : 15;
                    IMendixObject score = s.make("MatchSet", "Sequence", 1, "Home", home, "Away", away);
                    s.link(score, "MatchSet_Contest", contest);
                }
            }
            IMendixObject audit = s.make("MatchAudit", "Action", "Development example", "Actor", "Local development generator", "At", new Date(),
                "Reason", "Fictional draft; assign active captains/official deliberately to exercise the workflow. Seeded data is not runtime test evidence.", "AfterValue", "Format, inherited subdivision, fixture, participant lineups and draft set scores");
            s.link(audit, "MatchAudit_Registration", registration); s.link(audit, "MatchAudit_Federation", fed); s.link(audit, "MatchAudit_League", league);
        }
        owned.addAll(ids(s.owned)); manifest.setValue(ctx, "OwnedIds", encode(owned));
        manifest.setValue(ctx, "Summary", (summary == null ? "" : summary) + " Added five sports' match-registration examples. " + MATCH_FEATURE);
        Core.commitWithoutEvents(ctx, manifest);
        Core.getLogger("NexioSamples").info("Added " + s.owned.size() + " manifest-owned match examples; existing sample records preserved.");
        extendMatchReviewSamples(ctx);
    }

    /** Append review/ruling scenarios once; never rewrite the user's existing sample fixtures. */
    private static void extendMatchReviewSamples(IContext ctx) throws Exception {
        IMendixObject manifest=batch(ctx);String summary=manifest.getValue(ctx,"Summary");if(summary!=null&&summary.contains(MATCH_REVIEW_FEATURE))return;
        Set<Long> owned=decode(manifest.getValue(ctx,"OwnedIds"));Seed s=new Seed(ctx);
        IMendixObject format=query(ctx,"//MyFirstModule.MatchFormat").stream().filter(o->owned.contains(o.getId().toLong())&&"Badminton".equals(o.getValue(ctx,"Sport"))).findFirst().orElseThrow();
        IMendixObject base=query(ctx,"//MyFirstModule.MatchRegistration[MyFirstModule.MatchRegistration_Format="+format.getId().toLong()+"]").stream().filter(o->owned.contains(o.getId().toLong())).findFirst().orElseThrow();
        IMendixObject fixture=Core.retrieveId(ctx,base.getValue(ctx,"MyFirstModule.MatchRegistration_Fixture"));
        IMendixObject league=Core.retrieveId(ctx,fixture.getValue(ctx,"MyFirstModule.CompetitionFixture_League"));
        IMendixObject fed=Core.retrieveId(ctx,league.getValue(ctx,"MyFirstModule.CompetitionLeague_Federation"));
        String[] scenarios={"Retirement","Forfeit against recorded score","Excluded contest","Disputed result","Late submission","Unstarted substitution"};
        for(int scenario=0;scenario<scenarios.length;scenario++){
            IMendixObject fx=s.make("CompetitionFixture","Status","Published","Version",1L,"Leg",1L,"PairKey","DEMO-REVIEW:"+s.marker+":"+scenario,"Issue","[DEMO] "+scenarios[scenario],"StartsAt",s.hour(-5,18),"EndsAt",s.hour(-5,20));
            for(String field:new String[]{"HomeName","AwayName","HomeKey","AwayKey","HomeClubKey","AwayClubKey"})fx.setValue(ctx,field,fixture.getValue(ctx,field));
            for(String edge:new String[]{"League","Poule","Home","Away","HomeClub","AwayClub"})fx.setValue(ctx,"MyFirstModule.CompetitionFixture_"+edge,fixture.getValue(ctx,"MyFirstModule.CompetitionFixture_"+edge));Core.commitWithoutEvents(ctx,fx);
            IMendixObject reg=s.make("MatchRegistration","State",scenario==3?"Disputed":scenario==5?"Started":"Provisional","Revision",1,"StartedAt",s.hour(-5,18),"Reason","[DEMO] "+scenarios[scenario],"Disputed",scenario==3,"DisputeReason",scenario==3?"Fictional team dispute; federation review required":"");s.link(reg,"MatchRegistration_Fixture",fx);s.link(reg,"MatchRegistration_Format",format);
            IMendixObject disabledOfficial=query(ctx,"//UserManagement.Person").stream().filter(o->owned.contains(o.getId().toLong())&&!Boolean.TRUE.equals(o.getValue(ctx,"Active"))).findFirst().orElseThrow();reg.setValue(ctx,"LeaderKey",Long.toString(disabledOfficial.getId().toLong()));reg.setValue(ctx,"LeaderName",disabledOfficial.getValue(ctx,"Name"));Core.commitWithoutEvents(ctx,reg);
            List<MatchRules.ContestResult> results=new ArrayList<>();int index=0;
            for(IMendixObject template:query(ctx,"//MyFirstModule.MatchContest[MyFirstModule.MatchContest_Registration="+base.getId().toLong()+"]")){
                boolean home=index%2==0;String outcome=scenario==5?"Pending":scenario==0&&index==0?"HomeRetired":scenario==1&&index==0?"HomeForfeit":scenario==2&&index==0?"Excluded":"Played";
                IMendixObject contest=s.make("MatchContest","Name",template.getValue(ctx,"Name"),"Sequence",++index,"PlayersPerSide",template.getValue(ctx,"PlayersPerSide"),"Discipline",template.getValue(ctx,"Discipline"),"Outcome",outcome,"Reason","[DEMO] "+scenarios[scenario],"Revision",1);s.link(contest,"MatchContest_Registration",reg);s.link(contest,"MatchContest_Format",format);
                for(IMendixObject lineup:query(ctx,"//MyFirstModule.MatchLineup[MyFirstModule.MatchLineup_Contest="+template.getId().toLong()+"]")){IMendixObject copy=s.make("MatchLineup","Side",lineup.getValue(ctx,"Side"),"Revision",1);for(String field:new String[]{"Player1Name","Player2Name"})copy.setValue(ctx,field,lineup.getValue(ctx,field));for(String edge:new String[]{"MatchLineup_Player1","MatchLineup_Player2"})copy.setValue(ctx,"MyFirstModule."+edge,lineup.getValue(ctx,"MyFirstModule."+edge));s.link(copy,"MatchLineup_Contest",contest);}
                List<MatchRules.SetScore> scores=new ArrayList<>();if(scenario!=5){scores.add(new MatchRules.SetScore(home?21:15,home?15:21));scores.add(scenario==0&&index==1?new MatchRules.SetScore(7,11):new MatchRules.SetScore(home?21:16,home?16:21));}
                for(int n=0;n<scores.size();n++){MatchRules.SetScore score=scores.get(n);IMendixObject set=s.make("MatchSet","Sequence",n+1,"Home",score.home,"Away",score.away);s.link(set,"MatchSet_Contest",contest);}
                MatchRules.ContestResult result=MatchRules.contest(new MatchRules.Format(),scores,outcome,"Fictional development scenario");results.add(result);contest.setValue(ctx,"RecordedWinner",result.recordedWinner);contest.setValue(ctx,"OfficialWinner",result.officialWinner);contest.setValue(ctx,"HomeSets",(long)result.homeSets);contest.setValue(ctx,"AwaySets",(long)result.awaySets);contest.setValue(ctx,"HomeTotal",(long)result.homeTotal);contest.setValue(ctx,"AwayTotal",(long)result.awayTotal);contest.setValue(ctx,"Excluded",result.excluded);contest.setValue(ctx,"RankingEligible",result.rankingEligible);Core.commitWithoutEvents(ctx,contest);
            }
            if(scenario!=5){MatchRules.FixtureResult result=MatchRules.fixture(new MatchRules.Format(),results,Collections.emptyList(),BigDecimal.ZERO,BigDecimal.ZERO);reg.setValue(ctx,"HomeScore",(long)result.home);reg.setValue(ctx,"AwayScore",(long)result.away);reg.setValue(ctx,"Winner",result.winner);reg.setValue(ctx,"HomePoints",result.homePoints);reg.setValue(ctx,"AwayPoints",result.awayPoints);reg.setValue(ctx,"SubmittedAt",scenario==4?new Date():s.hour(-5,20));reg.setValue(ctx,"HomeDeadline",scenario==4?s.hour(1,12):s.hour(-4,12));reg.setValue(ctx,"AwayDeadline",scenario==4?s.hour(3,0):s.hour(-3,0));Core.commitWithoutEvents(ctx,reg);}
            IMendixObject audit=s.make("MatchAudit","Action","Development review scenario","Actor","Local development generator","At",new Date(),"Reason",scenarios[scenario],"AfterValue","Seeded example only; not a passing runtime test.");s.link(audit,"MatchAudit_Registration",reg);s.link(audit,"MatchAudit_League",league);s.link(audit,"MatchAudit_Federation",fed);
        }
        owned.addAll(ids(s.owned));manifest.setValue(ctx,"OwnedIds",encode(owned));manifest.setValue(ctx,"Summary",summary+" Added six review, ruling and substitution examples. "+MATCH_REVIEW_FEATURE);Core.commitWithoutEvents(ctx,manifest);
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
            for (int i=0;i<values.length;i+=2) obj.setValue(ctx, (String)values[i], values[i+1] instanceof Integer ? ((Integer)values[i+1]).longValue() : values[i+1]);
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
            "Description","Fictional local-development federation","Sport","Badminton","IsActive",true,"Currency","EUR",
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
        String[] names = {"Leuven Noord Shuttle","Mechelse Racketkring","Antwerpse Smashers","Brusselse Aces"};
        String[] cities = {"Leuven","Mechelen","Antwerpen","Brussel"};
        String[] streets = {"Sportlaan 24", "Racketstraat 18", "Smashlaan 32", "Shuttlestraat 14"};
        String[] postcodes = {"3000", "2800", "2000", "1000"};
        int personNumber=0;
        IMendixObject currentPerson = null;
        if (caller.getSession()!=null) {
            IMendixObject user = caller.getSession().getUser(caller).getMendixObject();
            if ("UserManagement.Person".equals(user.getType())) currentPerson=user;
        }
        for(int c=0;c<4;c++) {
            int fee=80+10*c;
            IMendixObject club = s.make("Club","Name","[DEMO] "+names[c],"Description","Fictional badminton club for trying Nexio.",
                "City",cities[c],"Address",streets[c],"PostalCode",postcodes[c],
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
        extendMatchSamples(ctx);
        extendDiscoveryAndAdminSamples(ctx);
        Core.getLogger("NexioSamples").info("Generated sample records. Only dedicated organization test administrators can sign in; no email or verified payment.");
    }

    /** Append isolated discovery examples and explicitly requested local test logins, once per version. */
    private static void extendDiscoveryAndAdminSamples(IContext ctx) throws Exception {
        require(Core.getConfiguration().isInDevelopment() && unittesting.ConfigurationManager.isLocalDevelopment(),
            "Test administrators are restricted to local development.");
        extendDirectoryProfileSamples(ctx);
        extendMembershipAudienceSamples(ctx);
        extendMultiAffiliationSamples(ctx);
        IMendixObject manifest = batch(ctx);
        require(manifest != null, "Sample ownership manifest is missing.");
        String summary = Objects.toString(manifest.getValue(ctx, "Summary"), "");
        Set<Long> owned = decode(manifest.getValue(ctx, "OwnedIds"));
        Seed s = new Seed(ctx);
        if (!summary.contains(DISCOVERY_FEATURE)) {
            String[] names = {"Leuven Shuttlekring", "Gent Racketvrienden", "Mechelen Trainingsclub"};
            String[] towns = {"Leuven", "Gent", "Mechelen"};
            String[] streets = {"Sportlaan 12", "Shuttlestraat 8", "Racketlaan 5"};
            String[] postcodes = {"3000", "9000", "2800"};
            for (int i = 0; i < names.length; i++) {
                IMendixObject club = s.make("Club", "Name", "[DEMO] " + names[i],
                    "Description", "Fictional Belgian badminton club. " + (i == 0 ? "Adult and junior registration examples." : i == 1 ? "Registration is currently closed." : "Unpublished directory visibility example."),
                    "Address", streets[i], "City", towns[i], "PostalCode", postcodes[i], "IsPublished", i != 2,
                    "LocationVerified", false, "RequiresApproval", true, "RequiresPayment", true);
                IMendixObject season = s.make("Season", "Name", s.season, "StartsOn", s.date(-30), "EndsOn", s.date(130), "IsPublished", true);
                s.link(season, "Season_Club", club);
                for (int option = 0; option < (i == 0 ? 4 : 1); option++) {
                    String title = option == 1 ? "Junior" : option == 2 ? "Closed option" : option == 3 ? "Future option" : "Adult";
                    IMendixObject type = s.make("MembershipType", "Name", title, "Description", "[DEMO] " + title + " badminton membership",
                        "Fee", s.fee(option == 1 ? 60 : 95), "Currency", "EUR", "IsAvailable", option != 2);
                    if (option == 3) { type.setValue(ctx, "AvailableFrom", s.date(14)); Core.commitWithoutEvents(ctx, type); }
                    s.link(type, "MembershipType_Club", club);
                    federateSampleMembership(ctx, s, club, type, "Badminton");
                    IMendixObject fee = s.make("SeasonFee", "Amount", s.fee(option == 1 ? 60 : 95), "Currency", "EUR", "IsAvailable", i != 1);
                    s.link(fee, "SeasonFee_Club", club); s.link(fee, "SeasonFee_Season", season); s.link(fee, "SeasonFee_MembershipType", type);
                }
            }
            summary += " Fictional Belgian discovery clubs: open, closed, future and unpublished registrations. " + DISCOVERY_FEATURE;
        }
        owned.addAll(ids(s.owned));
        if (samplePasswordAllowed(ctx)) {
            for (String entity : new String[]{"Club", "Federation"}) {
                IMendixObject appRole = first(ctx, "//System.UserRole[Name='" + (entity.equals("Club") ? "ClubAdmin" : "FederationAdmin") + "']");
                require(appRole != null, "The organization administrator app role is missing.");
                for (IMendixObject organization : query(ctx, "//MyFirstModule." + entity)) {
                    if (!owned.contains(organization.getId().toLong())) continue;
                    String username = "demo." + entity.toLowerCase(Locale.ROOT) + "." + s.id(organization) + "@example.invalid";
                    IMendixObject existingAdmin = first(ctx, "//System.User[Name='" + username + "']");
                    if (existingAdmin != null) {
                        require(owned.contains(existingAdmin.getId().toLong()), "A test username is used by an unrelated account; it was not changed.");
                        continue; // Preserve edited sample accounts, passwords, roles and assignments.
                    }
                    IMendixObject administrator = s.make("UserManagement.Person", "Name", username, "Email", username,
                        "FullName", "[DEMO] Administrator - " + organization.getValue(ctx, "Name"), "Active", true,
                        "WebServiceUser", false, "Password", TEST_ADMIN_PASSWORD, "InvitationPending", false,
                        "System.UserRoles", Arrays.asList(s.ordinaryRole.getId(), appRole.getId()));
                    String association = "MyFirstModule." + entity + "_Admins";
                    List<IMendixIdentifier> admins = new ArrayList<>();
                    Object previous = organization.getValue(ctx, association);
                    if (previous instanceof Collection<?>) for (Object value : (Collection<?>) previous) admins.add((IMendixIdentifier)value);
                    admins.add(administrator.getId()); organization.setValue(ctx, association, admins); Core.commitWithoutEvents(ctx, organization);
                    Core.getLogger("NexioSamples").info("Local test administrator: " + username + " for " + organization.getValue(ctx, "Name"));
                }
            }
            if (!summary.contains(TEST_ADMIN_FEATURE))
                summary += " Dedicated login-enabled administrators for each sample organization. " + TEST_ADMIN_FEATURE;
        }
        owned.addAll(ids(s.owned)); manifest.setValue(ctx, "OwnedIds", encode(owned)); manifest.setValue(ctx, "Summary", summary);
        Core.commitWithoutEvents(ctx, manifest);
        extendUserViewExamples(ctx);
        extendRegistrationProgressSamples(ctx);
    }

    /** Add isolated fictional profiles without inferring or replacing existing club metadata. */
    private static void extendDirectoryProfileSamples(IContext ctx) throws Exception {
        IMendixObject manifest = batch(ctx);
        require(manifest != null, "Sample ownership manifest is missing.");
        String summary = Objects.toString(manifest.getValue(ctx, "Summary"), "");
        if (summary.contains(DIRECTORY_PROFILE_FEATURE)) return;
        Set<Long> owned = decode(manifest.getValue(ctx, "OwnedIds"));
        Seed s = new Seed(ctx);
        Map<String, IMendixObject> cities = new HashMap<>();
        for (IMendixObject city : query(ctx, "//MyFirstModule.ClubDirectoryCity"))
            cities.putIfAbsent(Objects.toString(city.getValue(ctx, "NormalizedName"), ""), city);
        String[][] profiles = {
            {"Kade Rackets", "Antwerpen", "Kadeplein 24", "2000", "Badminton"},
            {"Park Tennis", "Leuven", "Parklaan 42", "3000", "Tennis"},
            {"Polder Padel", "Gent", "Polderstraat 18", "9000", "Padel"},
            {"Jeugd Tafeltennis", "Mechelen", "Jeugdlaan 16", "2800", "Table tennis"}
        };
        for (int i = 0; i < profiles.length; i++) {
            String[] p = profiles[i];
            IMendixObject club = s.make("Club", "Name", "[DEMO] " + p[0], "City", p[1], "Address", p[2],
                "PostalCode", p[3],
                "Description", "Fictional directory and registration example. This is not a real Belgian club or contact address.",
                "IsPublished", i != 3, "LocationVerified", false, "RequiresApproval", true, "RequiresPayment", true);
            String key = p[1].trim().toLowerCase(Locale.ROOT);
            IMendixObject city = cities.get(key);
            if (city == null) {
                city = s.make("ClubDirectoryCity", "Name", p[1], "NormalizedName", key);
                cities.put(key, city);
            }
            s.link(club, "Club_DirectoryCity", city);
            IMendixObject season = s.make("Season", "Name", s.season, "StartsOn", s.date(-30), "EndsOn", s.date(130), "IsPublished", true);
            s.link(season, "Season_Club", club);
            for (int option = 0; option < (i == 0 ? 2 : 1); option++) {
                boolean competition = i == 2 || (i == 0 && option == 0);
                int minimumAge = i == 1 || (i == 0 && option == 1) ? 18 : 6;
                int maximumAge = i == 3 || (i == 0 && option == 0) ? 17 : 0;
                IMendixObject type = s.make("MembershipType", "Name", competition ? "Competition" : "Recreational",
                    "Description", "[DEMO] " + p[4] + " registration", "Fee", s.fee(75), "Currency", "EUR", "IsAvailable", true,
                    "IsCompetitionPlayer", competition, "MinimumAge", minimumAge, "MaximumAge", maximumAge);
                s.link(type, "MembershipType_Club", club);
                federateSampleMembership(ctx, s, club, type, p[4]);
                IMendixObject fee = s.make("SeasonFee", "Amount", s.fee(75), "Currency", "EUR", "IsAvailable", true);
                s.link(fee, "SeasonFee_Club", club); s.link(fee, "SeasonFee_Season", season); s.link(fee, "SeasonFee_MembershipType", type);
            }
        }
        owned.addAll(ids(s.owned)); manifest.setValue(ctx, "OwnedIds", encode(owned));
        manifest.setValue(ctx, "Summary", summary + " Four fictional Belgian club profiles with city, sport, age and playing level; one unpublished. " + DIRECTORY_PROFILE_FEATURE);
        Core.commitWithoutEvents(ctx, manifest);
    }

    /** Add isolated age/category examples once, including to existing batches, without rewriting edited records. */
    private static void extendMembershipAudienceSamples(IContext ctx) throws Exception {
        IMendixObject manifest = batch(ctx);
        require(manifest != null, "Sample ownership manifest is missing.");
        String summary = Objects.toString(manifest.getValue(ctx, "Summary"), "");
        if (summary.contains(MEMBERSHIP_AUDIENCE_FEATURE)) return;
        Set<Long> owned = decode(manifest.getValue(ctx, "OwnedIds"));
        Seed s = new Seed(ctx);
        IMendixObject club = s.make("Club", "Name", "[DEMO] Schelde Rackets", "City", "Gent",
            "Address", "Voorbeeldkaai 28", "PostalCode", "9000",
            "Description", "Fictional Belgian club for membership-derived playing categories and age ranges. Not a real contact address.",
            "IsPublished", true, "LocationVerified", false, "RequiresApproval", true, "RequiresPayment", true);
        IMendixObject city = first(ctx, "//MyFirstModule.ClubDirectoryCity[NormalizedName='gent']");
        if (city == null) city = s.make("ClubDirectoryCity", "Name", "Gent", "NormalizedName", "gent");
        s.link(club, "Club_DirectoryCity", city);
        IMendixObject season = s.make("Season", "Name", s.season, "StartsOn", s.date(-30), "EndsOn", s.date(130), "IsPublished", true);
        s.link(season, "Season_Club", club);
        String[] names = {"Junior competition", "Adult competition", "Adult competition - reduced fee", "Recreational adults", "Closed all-ages option", "Future all-ages option"};
        int[] minimums = {6, 18, 18, 40, 0, 0};
        int[] maximums = {17, 35, 35, 65, 0, 0};
        for (int i = 0; i < names.length; i++) {
            IMendixObject type = s.make("MembershipType", "Name", names[i],
                "Description", "[DEMO] Inclusive age range in years; available categories are summarized once per club.",
                "IsCompetitionPlayer", i < 3, "MinimumAge", minimums[i], "MaximumAge", maximums[i],
                "IsAvailable", i != 4, "Fee", s.fee(i == 2 ? 65 : 90), "Currency", "EUR");
            if (i == 5) { type.setValue(ctx, "AvailableFrom", s.date(14)); Core.commitWithoutEvents(ctx, type); }
            s.link(type, "MembershipType_Club", club);
            federateSampleMembership(ctx, s, club, type, "Badminton");
            IMendixObject fee = s.make("SeasonFee", "Amount", s.fee(i == 2 ? 65 : 90), "Currency", "EUR", "IsAvailable", true);
            s.link(fee, "SeasonFee_Club", club); s.link(fee, "SeasonFee_Season", season); s.link(fee, "SeasonFee_MembershipType", type);
        }
        owned.addAll(ids(s.owned)); manifest.setValue(ctx, "OwnedIds", encode(owned));
        manifest.setValue(ctx, "Summary", summary + " Fictional Gent club: repeated competition category/age range, recreational members, disjoint inclusive ages, closed and future options. " + MEMBERSHIP_AUDIENCE_FEATURE);
        Core.commitWithoutEvents(ctx, manifest);
    }

    /** Link only a newly generated membership to an owned federation, never infer or rewrite an older federation's sport. */
    private static IMendixObject federateSampleMembership(IContext ctx, Seed s, IMendixObject club, IMendixObject type, String sport) throws Exception {
        Set<Long> owned = decode(batch(ctx).getValue(ctx, "OwnedIds")); owned.addAll(ids(s.owned));
        IMendixObject federation = null;
        for (IMendixObject candidate : query(ctx, "//MyFirstModule.Federation"))
            if (owned.contains(candidate.getId().toLong()) && sport.equalsIgnoreCase(Objects.toString(candidate.getValue(ctx, "Sport"), ""))
                && Boolean.TRUE.equals(candidate.getValue(ctx, "IsActive"))) { federation = candidate; break; }
        if (federation == null) federation = s.make("Federation", "Name", "[DEMO] " + sport + " Federation", "Sport", sport,
            "Description", "Fictional unisport federation for local development", "IsActive", true,
            "CensusMonth", 1, "CensusDay", 1, "Currency", "EUR", "BillingMethod", "ACTIVE_AT_CENSUS");
        String name = Boolean.TRUE.equals(type.getValue(ctx, "IsCompetitionPlayer")) ? "Competition" : "Recreational";
        owned.addAll(ids(s.owned));
        IMendixObject federationType = null;
        for (IMendixObject candidate : query(ctx, "//MyFirstModule.FederationType"))
            if (owned.contains(candidate.getId().toLong()) && federation.getId().equals(candidate.getValue(ctx, "MyFirstModule.FederationType_Federation"))
                && name.equals(candidate.getValue(ctx, "Name")) && Boolean.TRUE.equals(candidate.getValue(ctx, "IsAvailable"))) { federationType = candidate; break; }
        if (federationType == null) {
            federationType = s.make("FederationType", "Name", name, "Description", "[DEMO] " + sport + " " + name.toLowerCase(Locale.ROOT), "IsAvailable", true);
            s.link(federationType, "FederationType_Federation", federation);
            IMendixObject rate = s.make("FederationRate", "Year", s.today.getYear(), "AnnualFee", s.fee(name.equals("Competition") ? 15 : 5),
                "Currency", "EUR", "RateKey", s.id(federationType) + ":" + s.today.getYear());
            s.link(rate, "FederationRate_FederationType", federationType);
        }
        s.link(type, "MembershipType_FederationType", federationType);
        IMendixObject affiliation = first(ctx, "//MyFirstModule.FederationAffiliation[MyFirstModule.FederationAffiliation_Club=" + s.id(club)
            + "][MyFirstModule.FederationAffiliation_Federation=" + s.id(federation) + "]");
        if (affiliation == null) {
            affiliation = s.make("FederationAffiliation", "ClubName", club.getValue(ctx, "Name"), "Status", "Approved", "IsActive", true,
                "StartsOn", s.date(-30), "EndsOn", s.date(130), "RequestedAt", s.date(-31), "DecidedAt", s.date(-30));
            s.link(affiliation, "FederationAffiliation_Club", club); s.link(affiliation, "FederationAffiliation_Federation", federation);
        }
        return affiliation;
    }

    /** Isolated multi-affiliation and account examples; dates are relative to the generation date. */
    private static void extendMultiAffiliationSamples(IContext ctx) throws Exception {
        IMendixObject manifest = batch(ctx);
        String summary = Objects.toString(manifest.getValue(ctx, "Summary"), "");
        if (summary.contains(MULTI_AFFILIATION_FEATURE)) return;
        Set<Long> owned = decode(manifest.getValue(ctx, "OwnedIds")); Seed s = new Seed(ctx);
        IMendixObject club = s.make("Club", "Name", "[DEMO] Gent Multi Rackets", "City", "Gent", "Address", "Voorbeeldsportplein 40", "PostalCode", "9000",
            "Description", "Fictional Belgian club with two approved federations, a pending request and an expired affiliation.",
            "IsPublished", true, "LocationVerified", false, "RequiresApproval", true, "RequiresPayment", true);
        IMendixObject city = first(ctx, "//MyFirstModule.ClubDirectoryCity[NormalizedName='gent']");
        if (city == null) city = s.make("ClubDirectoryCity", "Name", "Gent", "NormalizedName", "gent");
        s.link(club, "Club_DirectoryCity", city);
        IMendixObject season = s.make("Season", "Name", s.season, "StartsOn", s.date(-30), "EndsOn", s.date(130), "IsPublished", true);
        s.link(season, "Season_Club", club);
        String[] sports = {"Badminton", "Tennis", "Padel", "Table tennis"};
        for (int i = 0; i < sports.length; i++) {
            IMendixObject type = s.make("MembershipType", "Name", sports[i] + (i == 0 ? " competition" : " recreational"),
                "Description", "[DEMO] " + (i == 2 ? "Unavailable draft awaiting federation approval" : i == 3 ? "Ended affiliation: excluded from offers" : "Approved federation membership"),
                "IsCompetitionPlayer", i == 0, "MinimumAge", i == 0 ? 12 : 18, "MaximumAge", 0, "IsAvailable", i != 2,
                "Fee", s.fee(95 + i * 10), "Currency", "EUR");
            s.link(type, "MembershipType_Club", club);
            IMendixObject affiliation = federateSampleMembership(ctx, s, club, type, sports[i]);
            if (i == 2) {
                affiliation.setValue(ctx, "Status", "Pending"); affiliation.setValue(ctx, "IsActive", false);
                affiliation.setValue(ctx, "RequestedAt", s.date(-2)); affiliation.setValue(ctx, "DecidedAt", null);
                Core.commitWithoutEvents(ctx, affiliation);
            } else if (i == 3) {
                affiliation.setValue(ctx, "EndsOn", s.date(-1)); Core.commitWithoutEvents(ctx, affiliation);
            }
            IMendixObject fee = s.make("SeasonFee", "Amount", s.fee(95 + i * 10), "Currency", "EUR", "IsAvailable", true);
            s.link(fee, "SeasonFee_Club", club); s.link(fee, "SeasonFee_Season", season); s.link(fee, "SeasonFee_MembershipType", type);
        }
        IMendixObject claim = s.make("AffiliationMutationClaim", "Key", "sample-cleanup:" + s.marker);
        s.link(claim, "AffiliationMutationClaim_Club", club);
        IMendixObject pending = first(ctx, "//MyFirstModule.FederationAffiliation[MyFirstModule.FederationAffiliation_Club=" + s.id(club) + "][Status='Pending']");
        for (String status : new String[]{"Declined", "Withdrawn"}) {
            IMendixObject history = s.make("FederationAffiliation", "ClubName", club.getValue(ctx, "Name"), "Status", status, "IsActive", false,
                "StartsOn", s.date(-100), "EndsOn", s.date(-31), "RequestedAt", s.date(-100), "DecidedAt", s.date(-90));
            s.link(history, "FederationAffiliation_Club", club);
            history.setValue(ctx, "MyFirstModule.FederationAffiliation_Federation", pending.getValue(ctx, "MyFirstModule.FederationAffiliation_Federation"));
            Core.commitWithoutEvents(ctx, history);
        }
        s.person("[DEMO] Account overview member", 701);
        IMendixObject blocked = s.person("[DEMO] Account overview blocked member", 702);
        blocked.setValue(ctx, "Blocked", true); Core.commitWithoutEvents(ctx, blocked); // Remains disabled.
        owned.addAll(ids(s.owned)); manifest.setValue(ctx, "OwnedIds", encode(owned));
        manifest.setValue(ctx, "Summary", summary + " Multi-affiliated fictional Gent club, pending/expired affiliations and disabled account-directory examples. " + MULTI_AFFILIATION_FEATURE);
        Core.commitWithoutEvents(ctx, manifest);
    }

    /** Exercise the actual native data sources on a fresh sample set; callers already own a rollback-safe transaction. */
    private static void verifyMembershipAudience(IContext ctx, Set<Long> owned) throws Exception {
        IMendixObject club = query(ctx, "//MyFirstModule.Club[Name='[DEMO] Schelde Rackets']").stream()
            .filter(o -> owned.contains(o.getId().toLong())).findFirst().orElseThrow();
        Set<Long> before = ids(all(ctx));
        IMendixObject detail = Core.microflowCall("MyFirstModule.DS_ClubDirectory_Profile").withParam("Club", club).execute(ctx);
        require("Competition, Recreational".equals(detail.getValue(ctx, "PlayingSummary")), "Playing categories were not deduplicated.");
        String ages = detail.getValue(ctx, "AgeSummary");
        List<String> ranges = Arrays.asList(ages.split(", "));
        require(ranges.size() == 3 && new HashSet<>(ranges).equals(new HashSet<>(Arrays.asList("6–17", "18–35", "40–65"))),
            "Age ranges were repeated, merged across gaps, or included closed/future options.");
        require("Both".equals(detail.getValue(ctx, "PlayingLevel")) && "YouthAndAdults".equals(detail.getValue(ctx, "AgeGroups")),
            "Directory filter classifications disagree with memberships.");
        List<IMendixObject> offers = Core.microflowCall("MyFirstModule.DS_Club_Offers").withParam("Club", club).execute(ctx);
        require(offers.size() == 4, "Current offers included closed/future memberships or lost a differently priced membership.");
        Set<String> offerAges = new HashSet<>();
        for (IMendixObject offer : offers) offerAges.add(offer.getValue(ctx, "AgeRange"));
        require(offerAges.equals(new HashSet<>(ranges)), "Offer age labels disagree with the club summary.");
        List<IMendixObject> directory = Core.microflowCall("MyFirstModule.DS_ClubDirectory_Profiles").execute(ctx);
        List<IMendixObject> matching = new ArrayList<>();
        for (IMendixObject row : directory)
            if (club.getId().equals(row.getValue(ctx, "MyFirstModule.ClubDiscoveryProfile_Club"))) matching.add(row);
        require(matching.size() == 1, "Directory should contain one row per published club.");
        require(matching.get(0).getValue(ctx, "PlayingSummary").equals(detail.getValue(ctx, "PlayingSummary"))
            && new HashSet<>(Arrays.asList(((String) matching.get(0).getValue(ctx, "AgeSummary")).split(", "))).equals(new HashSet<>(ranges)),
            "Directory and detail audience summaries disagree.");
        require(before.equals(ids(all(ctx))), "Reading audience summaries persisted business records.");
    }

    /** Native read scenarios on newly generated records only; never assert over or rewrite an edited older batch. */
    private static void verifyMultiAffiliation(IContext ctx, Set<Long> owned) throws Exception {
        IMendixObject club = query(ctx, "//MyFirstModule.Club[Name='[DEMO] Gent Multi Rackets']").stream()
            .filter(o -> owned.contains(o.getId().toLong())).findFirst().orElseThrow();
        Set<Long> before = ids(all(ctx));
        IMendixObject profile = Core.microflowCall("MyFirstModule.DS_ClubDirectory_Profile").withParam("Club", club).execute(ctx);
        Set<String> sports = new HashSet<>(Arrays.asList(((String) profile.getValue(ctx, "Sports")).split(", ")));
        require(sports.equals(new HashSet<>(Arrays.asList("Badminton", "Tennis"))), "Club sports included pending/expired affiliations or missed an approved sport.");
        List<IMendixObject> offers = Core.microflowCall("MyFirstModule.DS_Club_Offers").withParam("Club", club).execute(ctx);
        require(offers.size() == 2, "Multi-affiliated club should offer only its two eligible memberships.");
        Set<String> offeredSports = new HashSet<>();
        for (IMendixObject offer : offers) {
            offeredSports.add(offer.getValue(ctx, "Sport"));
            require(!Objects.toString(offer.getValue(ctx, "FederationName"), "").isBlank(), "Membership offer lost its federation identity.");
        }
        require(offeredSports.equals(sports), "Club summary and offer sports disagree.");
        List<IMendixObject> affiliations = query(ctx, "//MyFirstModule.FederationAffiliation[MyFirstModule.FederationAffiliation_Club=" + club.getId().toLong() + "]");
        require(affiliations.size() == 6 && affiliations.stream().anyMatch(a -> "Pending".equals(a.getValue(ctx, "Status")))
            && affiliations.stream().anyMatch(a -> "Declined".equals(a.getValue(ctx, "Status")))
            && affiliations.stream().anyMatch(a -> "Withdrawn".equals(a.getValue(ctx, "Status"))), "Affiliation review examples are incomplete.");
        List<IMendixObject> current = new ArrayList<>();
        Date today = Date.from(LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC));
        for (IMendixObject affiliation : affiliations) {
            Date end = affiliation.getValue(ctx, "EndsOn");
            if ("Approved".equals(affiliation.getValue(ctx, "Status")) && Boolean.TRUE.equals(affiliation.getValue(ctx, "IsActive")) && (end == null || !end.before(today))) current.add(affiliation);
        }
        IMendixObject pendingType = first(ctx, "//MyFirstModule.MembershipType[MyFirstModule.MembershipType_Club=" + club.getId().toLong() + "][Name='Padel recreational']");
        // Even a stale/forged available flag must not bypass federation approval. This temporary change is never committed.
        pendingType.setValue(ctx, "IsAvailable", true);
        try {
            IMendixObject eligible = Core.microflowCall("MyFirstModule.SUB_MembershipType_EligibleFederation")
                .withParam("MembershipType", pendingType).withParam("Affiliations", current).execute(ctx);
            require(eligible == null, "A pending federation affiliation enabled a membership offer.");
        } finally { pendingType.setValue(ctx, "IsAvailable", false); }
        require(before.equals(ids(all(ctx))), "Reading multi-affiliation summaries persisted business records.");
    }

    /** Give a scoped sample administrator visible examples of each member registration stage. */
    private static void extendRegistrationProgressSamples(IContext ctx) throws Exception {
        IMendixObject manifest = batch(ctx);
        String summary = Objects.toString(manifest.getValue(ctx, "Summary"), "");
        if (summary.contains(REGISTRATION_PROGRESS_FEATURE)) return;
        Set<Long> owned = decode(manifest.getValue(ctx, "OwnedIds")); Seed s = new Seed(ctx);
        IMendixObject person = null, club = null;
        for (IMendixObject candidate : query(ctx, "//MyFirstModule.Club")) {
            if (!owned.contains(candidate.getId().toLong())) continue;
            IMendixObject account = first(ctx, "//UserManagement.Person[Name='demo.club." + s.id(candidate) + "@example.invalid']");
            if (account != null && owned.contains(account.getId().toLong())) { person = account; club = candidate; break; }
        }
        if (person == null) return; // Retry this feature after compliant test accounts can be created.
        IMendixObject season = first(ctx, "//MyFirstModule.Season[MyFirstModule.Season_Club=" + s.id(club) + "][IsPublished=true][StartsOn <= '[%CurrentDateTime%]' and EndsOn >= '[%BeginOfCurrentDay%]']");
        if (season == null) return;
        String[][] stages = {
            {"Draft", "Pending", "NotRequired", "Complete your registration form"},
            {"Pending", "Pending", "AwaitingApproval", "Awaiting club approval"},
            {"Pending", "OnHold", "AwaitingApproval", "On hold / waiting list - the club will contact you when a place is available"},
            {"Pending", "Approved", "Required", "Payment required - view your invoice"},
            {"Active", "Approved", "NotRequired", "Membership active"},
            {"Withdrawn", "Pending", "NotRequired", "Withdrawn by you before approval"}
        };
        for (int i = 0; i < stages.length; i++) {
            String[] state = stages[i];
            IMendixObject type = s.make("MembershipType", "Name", "[DEMO] Progress " + (i + 1), "Description", "Isolated registration status example", "IsAvailable", true, "Fee", s.fee(i == 3 ? 75 : 0), "Currency", "EUR");
            s.link(type, "MembershipType_Club", club);
            federateSampleMembership(ctx, s, club, type, "Badminton");
            IMendixObject fee = s.make("SeasonFee", "Amount", s.fee(i == 3 ? 75 : 0), "Currency", "EUR", "IsAvailable", true);
            s.link(fee, "SeasonFee_Club", club); s.link(fee, "SeasonFee_Season", season); s.link(fee, "SeasonFee_MembershipType", type);
            IMendixObject request = s.make("RegistrationRequest", "MemberName", person.getValue(ctx, "FullName"), "MemberEmail", person.getValue(ctx, "Email"),
                "Status", state[0], "Approval", state[1], "Payment", state[2], "NextAction", state[3], "SeasonName", season.getValue(ctx, "Name"),
                "Fee", s.fee(i == 3 ? 75 : 0), "OriginalFee", s.fee(i == 3 ? 75 : 0), "Currency", "EUR", "RequiresApproval", true, "RequiresPayment", i == 3,
                "RegistrationKey", s.id(person) + ":" + s.id(club) + ":" + s.id(type) + ":" + s.id(season));
            if (i != 0) { request.setValue(ctx, "SubmittedAt", s.hour(-i, 10)); Core.commitWithoutEvents(ctx, request); }
            s.link(request, "RegistrationRequest_Person", person); s.link(request, "RegistrationRequest_Club", club);
            s.link(request, "RegistrationRequest_MembershipType", type); s.link(request, "RegistrationRequest_Season", season); s.link(request, "RegistrationRequest_SeasonFee", fee);
            if (i == 4) s.link(request, "RegistrationRequest_Membership", s.membership(person, club, type, season));
            if (i == 3) {
                IMendixObject invoice = s.make("Invoice", "Number", "DEMO-PROGRESS-" + s.marker, "BillingKey", "DEMO-PROGRESS:" + s.marker,
                    "Description", "[DEMO] Registration payment example - do not pay", "Amount", s.fee(75), "Currency", "EUR", "Status", "Open",
                    "DueDate", s.date(21), "Kind", "Registration", "RecipientName", person.getValue(ctx, "FullName"), "RecipientEmail", person.getValue(ctx, "Email"));
                s.link(invoice, "Invoice_RegistrationRequest", request); s.link(invoice, "Invoice_Club", club); s.link(invoice, "Invoice_Person", person);
            }
        }
        owned.addAll(ids(s.owned)); manifest.setValue(ctx, "OwnedIds", encode(owned));
        manifest.setValue(ctx, "Summary", summary + " Member registration progress: draft, approval, waiting list, unpaid, active and withdrawn history. " + REGISTRATION_PROGRESS_FEATURE);
        Core.commitWithoutEvents(ctx, manifest);
    }

    /** Give the scoped sample administrators their own member view without enabling disabled sample people. */
    private static void extendUserViewExamples(IContext ctx) throws Exception {
        IMendixObject manifest=batch(ctx);
        String summary=Objects.toString(manifest.getValue(ctx,"Summary"),"");
        if(summary.contains(USER_VIEW_FEATURE) || !summary.contains(TEST_ADMIN_FEATURE))return;
        Set<Long> owned=decode(manifest.getValue(ctx,"OwnedIds")); Seed seed=new Seed(ctx);
        List<IMendixObject> clubs=query(ctx,"//MyFirstModule.Club");clubs.removeIf(c->!owned.contains(c.getId().toLong()));
        for(IMendixObject club:clubs) {
            IMendixObject season=first(ctx,"//MyFirstModule.Season[MyFirstModule.Season_Club="+seed.id(club)+"][IsPublished=true][StartsOn <= '[%CurrentDateTime%]' and EndsOn >= '[%BeginOfCurrentDay%]']");
            IMendixObject type=first(ctx,"//MyFirstModule.MembershipType[MyFirstModule.MembershipType_Club="+seed.id(club)+"][IsAvailable=true]");
            if(season==null || type==null || !owned.contains(season.getId().toLong()) || !owned.contains(type.getId().toLong()))continue;
            Collection<IMendixIdentifier> admins=club.getValue(ctx,"MyFirstModule.Club_Admins");
            if(admins==null)continue;
            for(IMendixIdentifier id:admins) {
                if(!owned.contains(id.toLong()))continue;
                IMendixObject person=Core.retrieveId(ctx,id);
                if(!Objects.equals(person.getValue(ctx,"Name"),"demo.club."+seed.id(club)+"@example.invalid"))continue;
                IMendixObject membership=first(ctx,"//MyFirstModule.Membership[MyFirstModule.Membership_Person="+id.toLong()+"][MyFirstModule.Membership_Club="+seed.id(club)+"]");
                if(membership==null)seed.membership(person,club,type,season);
            }
        }
        owned.addAll(ids(seed.owned));manifest.setValue(ctx,"OwnedIds",encode(owned));
        manifest.setValue(ctx,"Summary",summary+" Scoped club administrators also have their own sample membership for View as user. Disabled sample accounts remain disabled. "+USER_VIEW_FEATURE);
        Core.commitWithoutEvents(ctx,manifest);
    }

    private static void verifySampleAccounts(IContext ctx, Set<Long> owned) throws Exception {
        Set<Long> activeAdmins = new HashSet<>();
        for (String entity : new String[]{"Club", "Federation"}) {
            for (IMendixObject organization : query(ctx, "//MyFirstModule." + entity)) {
                if (!owned.contains(organization.getId().toLong())) continue;
                Collection<IMendixIdentifier> admins = organization.getValue(ctx, "MyFirstModule." + entity + "_Admins");
                if (admins == null) continue;
                String expected = "demo." + entity.toLowerCase(Locale.ROOT) + "." + organization.getId().toLong() + "@example.invalid";
                for (IMendixIdentifier id : admins) {
                    if (!owned.contains(id.toLong())) continue;
                    IMendixObject person = Core.retrieveId(ctx, id);
                    if (!expected.equals(person.getValue(ctx, "Name"))) continue;
                    Collection<IMendixIdentifier> roleIds = person.getValue(ctx, "System.UserRoles");
                    Set<String> roles = new HashSet<>();
                    for (IMendixIdentifier roleId : roleIds) roles.add(Core.retrieveId(ctx, roleId).getValue(ctx, "Name"));
                    require(roles.equals(new HashSet<>(Arrays.asList("User", entity.equals("Club") ? "ClubAdmin" : "FederationAdmin"))), "A test administrator has unexpected app roles.");
                    activeAdmins.add(id.toLong());
                }
            }
        }
        for (IMendixObject person : query(ctx, "//UserManagement.Person"))
            if (owned.contains(person.getId().toLong()) && Boolean.TRUE.equals(person.getValue(ctx, "Active")))
                require(activeAdmins.contains(person.getId().toLong()), "An ordinary sample account is active.");
    }

    /** Let supported Mendix validation reject a weak test password without breaking other sample upgrades. */
    private static boolean samplePasswordAllowed(IContext ctx) throws Exception {
        IMendixObject probe = Core.instantiate(ctx, "UserManagement.Person");
        try {
            probe.setValue(ctx, "Password", TEST_ADMIN_PASSWORD);
            return true;
        } catch (com.mendix.systemwideinterfaces.core.UserException rejected) {
            Core.getLogger("NexioSamples").warn("Organization test administrators were not created: the requested test password does not meet the project's password policy. Other sample examples are available. Choose a compliant test password or explicitly approve a project password-policy change, then generate again.");
            return false;
        }
    }
}

