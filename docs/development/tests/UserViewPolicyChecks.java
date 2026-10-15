import myfirstmodule.UserViewPolicy;

public final class UserViewPolicyChecks {
    private static int checks;
    private static void check(boolean condition,String description) {
        checks++;if(!condition)throw new AssertionError(description);
    }
    private static void invalidReason(String value) {
        boolean denied=false;try{UserViewPolicy.reason(value);}catch(IllegalArgumentException expected){denied=true;}
        check(denied,"Invalid audit reason accepted");
    }
    public static void main(String[] args) {
        for(int mask=0;mask<32;mask++) {
            boolean active=(mask&1)!=0, blocked=(mask&2)!=0, anon=(mask&4)!=0, service=(mask&8)!=0, admin=(mask&16)!=0;
            check(UserViewPolicy.eligible(active,blocked,anon,service,admin)==(mask==1),"Account eligibility mask "+mask);
        }
        check(!UserViewPolicy.tokenMatches(null,"x"),"Missing proof accepted");
        check(!UserViewPolicy.tokenMatches("", ""),"Empty proof accepted");
        check(!UserViewPolicy.tokenMatches("original", "different"),"Wrong CSRF accepted");
        check(UserViewPolicy.tokenMatches("proof-123", "proof-123"),"Valid proof rejected");
        check(UserViewPolicy.validBinding("session-a","session-a",1001,1000),"Valid session binding rejected");
        check(!UserViewPolicy.validBinding("session-a","session-b",1001,1000),"Another browser/session accepted");
        check(!UserViewPolicy.validBinding("session-a","session-a",1000,1000),"Expired view accepted");
        check(!UserViewPolicy.validBinding("session-a",null,1001,1000),"Missing session accepted");
        invalidReason(null);invalidReason("");invalidReason("ab");invalidReason("x".repeat(301));
        invalidReason("line1\nline2");invalidReason("line1\rline2");invalidReason("line1\u2028line2");invalidReason("line1\u2029line2");
        check(UserViewPolicy.reason("  Check permissions  ").equals("Check permissions"),"Audit reason normalization failed");
        check(UserViewPolicy.html("<script a=\"x\">&'\"").equals("&lt;script a=&quot;x&quot;&gt;&amp;&#39;&quot;"),"Untrusted account text was not escaped");
        check(UserViewPolicy.html(null).isEmpty(),"Null display value failed");
        System.out.println("PASS: "+checks+" user-view policy checks. HTTP/session integration is not covered by these checks.");
    }
}
