package myfirstmodule;

import com.mendix.core.Core;
import com.mendix.systemwideinterfaces.core.*;
import com.mendix.systemwideinterfaces.core.meta.IMetaObject;
import java.util.*;

/**
 * End of retention for archived accounts. Accounts are never deleted: personal data is replaced by a stable
 * pseudonym, every record keeps its links, and actions people took stay attributable through the account id.
 * Names in sport games (competition players, lineups, fixtures) and accounting documents (invoices, federation
 * billing) are left unchanged. See docs/development/account-archive-impact.md.
 */
final class AccountAnonymiser {
    private static final String M = "MyFirstModule.";
    private static final String LOGIN_PREFIX = "archived-";
    private static final String AUDIT_POLICY = "AuditTrail";
    private static final int DEFAULT_AUDIT_DAYS = 3650;
    private static final String[] AUDIT_TYPES = {"AccountAudit", "CompetitionAudit", "MatchAudit"};
    /** Action snapshots that may hold a login or email address; matching values become the pseudonym. */
    private static final String[][] ACTOR_FIELDS = {
        {"AccountAudit", "Actor"}, {"AccountAudit", "Subject"}, {"CompetitionAudit", "Actor"}, {"MatchAudit", "Actor"},
        {"FixtureChange", "ProposedBy"}, {"FeeAdjustment", "AdjustedBy"}, {"RegistrationRequest", "InstallmentDecisionBy"},
        {"AccountInvitation", "CreatedBy"}, {"RecordLifecycle", "ArchivedBy"}};
    private AccountAnonymiser() {}

    static boolean isAnonymised(IContext c, IMendixObject user) {
        return Objects.toString(user.getValue(c, "Name"), "").startsWith(LOGIN_PREFIX);
    }

    static String pseudonym(IMendixObject user) {
        return "P-" + Long.toString(user.getId().toLong(), 36).toUpperCase(Locale.ROOT);
    }

    static void anonymise(IContext c, IMendixObject user) throws Exception {
        if (isAnonymised(c, user)) return;
        String pseudonym = pseudonym(user);
        String email = pseudonym.toLowerCase(Locale.ROOT) + "@archived.invalid";
        boolean person = user.isInstanceOf("UserManagement.Person");
        Set<String> identifiers = new LinkedHashSet<>();
        addIdentifier(identifiers, user.getValue(c, "Name"));
        if (person) addIdentifier(identifiers, user.getValue(c, "Email"));
        IMendixIdentifier key = user.getId();

        user.setValue(c, "Name", LOGIN_PREFIX + pseudonym.toLowerCase(Locale.ROOT));
        user.setValue(c, "Active", false);
        user.setValue(c, "Blocked", false);
        user.setValue(c, "Password", "Ax1" + UUID.randomUUID() + UUID.randomUUID());
        user.setValue(c, "System.UserRoles", new ArrayList<IMendixIdentifier>());
        user.setValue(c, "System.User_Language", null);
        user.setValue(c, "System.User_TimeZone", null);
        if (person) {
            user.setValue(c, "FullName", pseudonym);
            user.setValue(c, "Email", email);
            user.setValue(c, "InvitationPending", false);
        }
        Core.commit(c, user);

        // Contact copies are pseudonymised, the records themselves stay.
        update(c, "Membership", "MyFirstModule.Membership_Person", key, Map.of("MemberName", pseudonym, "MemberEmail", email));
        for (IMendixObject request : byAssociation(c, "RegistrationRequest", "MyFirstModule.RegistrationRequest_Person", key)) {
            set(c, request, "MemberName", pseudonym);
            set(c, request, "MemberEmail", email);
            Core.commit(c, request);
            for (IMendixObject answer : byAssociation(c, "FormResponse", "MyFirstModule.FormResponse_RegistrationRequest", request.getId())) {
                set(c, answer, "Answer", "");
                set(c, answer, "AnswerDate", null);
                set(c, answer, "AnswerChoice", null);
                set(c, answer, "MyFirstModule.FormResponse_Gender", null);
                Core.commit(c, answer);
            }
        }
        update(c, "ClubStaff", "MyFirstModule.ClubStaff_Person", key, Map.of("MemberName", pseudonym, "Email", email, "IsActive", false));
        update(c, "FederationCompetitionDelegate", "MyFirstModule.FederationCompetitionDelegate_User", key, Map.of("Name", pseudonym, "Email", email, "IsActive", false));
        for (IMendixObject invitation : byAssociation(c, "AccountInvitation", "MyFirstModule.AccountInvitation_Person", key)) {
            set(c, invitation, "FullName", pseudonym);
            set(c, invitation, "Email", email);
            set(c, invitation, "TokenHash", "");
            if (invitation.getValue(c, "UsedAt") == null) set(c, invitation, "Revoked", true);
            Core.commit(c, invitation);
        }
        removeFromSets(c, "Club", "MyFirstModule.Club_Admins", key);
        removeFromSets(c, "Federation", "MyFirstModule.Federation_Admins", key);

        for (String old : identifiers) {
            replace(c, "ClubAdminAssignment", "Email", old, email);
            replace(c, "FederationAdminAssignment", "Email", old, email);
            replace(c, "InvoiceMail", "RecipientEmail", old, email);
            for (String member : List.of("Email", "Username")) {
                for (IMendixObject delivery : equal(c, "InvitationDelivery", member, old)) {
                    set(c, delivery, "Email", email);
                    set(c, delivery, "Username", pseudonym);
                    set(c, delivery, "Body", "");
                    Core.commit(c, delivery);
                }
            }
            for (String[] field : ACTOR_FIELDS) replace(c, field[0], field[1], old, pseudonym);
        }

        IMendixObject audit = Core.instantiate(c, M + "AccountAudit");
        audit.setValue(c, "At", new Date());
        audit.setValue(c, "Event", "Account anonymised");
        audit.setValue(c, "Actor", "Retention job");
        audit.setValue(c, "Subject", pseudonym);
        audit.setValue(c, "Details", "Archive retention ended. Personal data replaced by pseudonym " + pseudonym
            + "; memberships, invoices, sport history and actions are kept.");
        audit.setValue(c, M + "AccountAudit_Account", key);
        Core.commit(c, audit);
        Core.getLogger("TableRetention").info("Account anonymised as " + pseudonym);
    }

    /** Removes audit rows older than the AuditTrail retention (default 10 years); Keep indefinitely disables it. */
    static void purgeAudits(IContext c, IMendixObject rule) throws Exception {
        if (rule != null && Boolean.TRUE.equals(rule.getValue(c, "KeepForever"))) return;
        int days = rule == null ? DEFAULT_AUDIT_DAYS : ((Number) rule.getValue(c, "RetentionDays")).intValue();
        Date cutoff = new Date(System.currentTimeMillis() - days * 86_400_000L);
        for (String type : AUDIT_TYPES) {
            while (true) {
                List<IMendixObject> batch = Core.createXPathQuery("//" + M + type + "[At != empty and At < $cutoff]")
                    .setVariable("cutoff", cutoff).setAmount(500).execute(c);
                if (batch.isEmpty()) break;
                Core.delete(c, batch);
                if (batch.size() < 500) break;
            }
        }
    }

    static String auditPolicyName() { return AUDIT_POLICY; }
    static int defaultAuditDays() { return DEFAULT_AUDIT_DAYS; }

    private static void addIdentifier(Set<String> identifiers, Object value) {
        String text = Objects.toString(value, "").trim();
        if (!text.isEmpty() && !text.endsWith("@archived.invalid")) identifiers.add(text);
    }

    /** True when the persistable type exists and declares the attribute or owns the association. */
    private static boolean queryable(String type, String member) {
        IMetaObject meta = Core.getMetaObject(M + type);
        if (meta == null || !meta.isPersistable()) return false;
        return member.contains(".") ? meta.getMetaAssociationParent(member) != null : meta.getMetaPrimitive(member) != null;
    }

    private static List<IMendixObject> byAssociation(IContext c, String type, String association, IMendixIdentifier key) throws Exception {
        if (!queryable(type, association)) return List.of();
        return Core.createXPathQuery("//" + M + type + "[" + association + " = $key]").setVariable("key", key).execute(c);
    }

    private static List<IMendixObject> equal(IContext c, String type, String member, String value) throws Exception {
        if (!queryable(type, member)) return List.of();
        return Core.createXPathQuery("//" + M + type + "[" + member + " = $value]").setVariable("value", value).execute(c);
    }

    private static void update(IContext c, String type, String association, IMendixIdentifier key, Map<String, Object> values) throws Exception {
        for (IMendixObject row : byAssociation(c, type, association, key)) {
            for (Map.Entry<String, Object> value : values.entrySet()) set(c, row, value.getKey(), value.getValue());
            Core.commit(c, row);
        }
    }

    private static void replace(IContext c, String type, String member, String old, String replacement) throws Exception {
        for (IMendixObject row : equal(c, type, member, old)) {
            set(c, row, member, replacement);
            Core.commit(c, row);
        }
    }

    private static void removeFromSets(IContext c, String type, String association, IMendixIdentifier key) throws Exception {
        for (IMendixObject owner : byAssociation(c, type, association, key)) {
            List<IMendixIdentifier> members = new ArrayList<>(owner.getValue(c, association));
            if (members.remove(key)) {
                owner.setValue(c, association, members);
                Core.commit(c, owner);
            }
        }
    }

    private static void set(IContext c, IMendixObject row, String member, Object value) {
        if (row.hasMember(member)) row.setValue(c, member, value);
    }
}
