# Administrator: View as user

Open **Accounts → View as user** as the platform Administrator. Search by name or login, give a reason, and choose an account. The application reloads using that user's own session and permissions. Use **Return to administrator** in the persistent banner to return to the account picker and select someone else.

Alternatively, open **Application accounts → View account → View as user (local development)**. This selects that account on a confirmation screen. A GET only displays the confirmation; enter a reason and submit the CSRF-protected POST to start viewing. The detail action is hidden outside development and for ineligible accounts; the controller independently enforces the stricter local Studio Pro runtime check on every request.

This implementation is restricted to the local Studio Pro development runtime. Production requests return 404. Club and federation administrators cannot initiate a switch. Disabled, blocked, anonymous, service and platform Administrator accounts cannot be selected. No password, activation flag or role assignment is changed.

This is an actual user session: actions can change data, send messages or trigger normal workflows according to that user's permissions. This is not a read-only simulation. Cookies are shared by this browser's app tabs, so those tabs also use the selected session; refresh stale tabs after returning.

Each view lasts up to 15 minutes. The original administrator session is retained and kept alive only while the view remains valid. Return requires both the dedicated browser proof cookie and the correct viewing session, a CSRF-protected POST and a fresh check that the original user remains an active platform Administrator. Returning destroys the newly created viewing session and restores the original administrator session. Expiry or revocation destroys the viewing session. If it expires, the app restarts, or the selected user signs out, sign in again as administrator.

Start/return/expiry/revocation events are written to the **AdminUserView** runtime log and the selected account's persistent **Audit history**, with administrator, target and reason. A start must persist its audit before session handoff; a failure closes the child session. Ending always destroys the child session, even if the audit database write fails (the failure remains in the runtime log). Session identifiers and the return proof are never logged or embedded in URLs. The return proof is random, HttpOnly, SameSite=Strict, scoped to the controller path, and Secure on HTTPS. No persistent authentication token is requested for the viewing session. Controller responses are non-cacheable; account text is escaped. Existing authentication, password, framing and cookie policies are retained.

The controller sends `Referrer-Policy: same-origin`. The earlier `no-referrer` header caused Chrome's HTML form POST to send `Origin: null`, which the strict origin check correctly rejected. Same-origin keeps the real origin on these forms and suppresses referrers to other origins. The origin comparison, CSRF validation, local-runtime and account checks are unchanged. See [Referrer-Policy and Origin](https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers/Referrer-Policy).

Implementation:

- The existing `MyFirstModule.Platform_Accounts` page remains in the **Accounts** model folder and links to the account picker.
- `AdminUserView.java` registers the `/admin-user-view/` controller through the existing guarded application-startup bridge. It uses Mendix's public RequestHandler, initializeSession and addMendixCookies APIs.
- `theme/web/admin-user-view.js` adds the banner to the normal application page using text-only DOM operations. The endpoint returns status only for the bound viewing session. No iframe or external browser service is involved.
- `_nexio-user-view.scss` provides the compact picker and banner styles.
- The private in-memory return registry deliberately does not survive a runtime restart. The original administrator identity is never added to the selected user's permissions.

Development examples:

Generate or upgrade sample data as a local platform administrator. Existing active sample club and federation administrators provide scoped targets. `features:administrator-user-view-v1` adds an owned membership for each sample club administrator so the member dashboard also has content. Existing disabled people remain disabled and demonstrate exclusion from the picker. Membership examples are appended once, preserve existing records, and use the existing private ownership manifest and cleanup dependency map. They have not yet been generated or verified in an administrator browser session.

Verification:

- **Model consistency:** no connector errors after adding the Accounts entry point.
- **Compilation:** Java compilation and JavaScript syntax checks passed. Studio Pro's full Java/package build and theme compilation completed, and the app restarted successfully.
- **Automated policy checks:** 62 assertions cover the account eligibility truth table, selected-account identifiers including overflow and invalid input, missing/wrong tokens, session binding, expiry, reason validation including log line separators, and HTML escaping. These are not HTTP integration tests.
- **Read-only runtime probe:** validated account entity inheritance and executed the three parameterized account/authorization query shapes successfully. It switched no user session and changed no business record; this does not verify the HTTP cookie handoff.
- **Runtime scenarios still to verify:** switch into two differently scoped accounts, navigate while the banner persists, perform Return, reject cross-session/replayed requests, enforce expiry/revocation, and verify sample upgrade/removal. Browser access to localhost was previously declined, so no alternative browser path was used for these checks.

API references: [Core](https://apidocs.rnd.mendix.com/11/runtime/com/mendix/core/Core.html), [RequestHandler](https://apidocs.rnd.mendix.com/11/runtime/com/mendix/externalinterface/connector/RequestHandler.html).
