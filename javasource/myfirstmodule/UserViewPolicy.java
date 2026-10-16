package myfirstmodule;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Independently testable checks shared by the administrator user-view controller. */
public final class UserViewPolicy {
    private UserViewPolicy() {}
    public static boolean eligible(boolean active, boolean blocked, boolean anonymous, boolean webservice, boolean administrator) {
        return active && !blocked && !anonymous && !webservice && !administrator;
    }
    public static boolean tokenMatches(String expected, String supplied) {
        return expected != null && supplied != null && !expected.isEmpty() &&
            MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), supplied.getBytes(StandardCharsets.UTF_8));
    }
    public static boolean validBinding(String expectedSession, String currentSession, long expiresAt, long now) {
        return now < expiresAt && tokenMatches(expectedSession, currentSession);
    }
    public static long accountId(String input) {
        if (input == null || !input.matches("[0-9]{1,19}"))
            throw new IllegalArgumentException("Select an account.");
        try {
            long id = Long.parseLong(input);
            if (id > 0) return id;
        } catch (NumberFormatException invalid) { /* Report the same validation error. */ }
        throw new IllegalArgumentException("Select an account.");
    }
    public static String reason(String input) {
        String value = input == null ? "" : input.trim();
        if (value.length() < 3 || value.length() > 300 || value.codePoints().anyMatch(c -> Character.isISOControl(c) || c == 0x2028 || c == 0x2029))
            throw new IllegalArgumentException("Enter a reason of 3–300 characters on one line.");
        return value;
    }
    public static String html(Object value) {
        return String.valueOf(value == null ? "" : value).replace("&", "&amp;").replace("<", "&lt;")
            .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
}
