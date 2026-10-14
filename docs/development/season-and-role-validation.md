# Seasons, role permissions and development examples

Implemented model changes:

- Club settings and club creation no longer expose latitude, longitude or a manual location-verification checkbox. Coordinate fields remain read-only integration storage. Address geocoding is not configured, and radius filtering does not use unverified locations.
- Season overlap validation covers both published and unpublished seasons, with inclusive first/last dates.
- Club owner and delegated staff write rules exclude expired seasons and their fees. Matching historical rules retain scoped read access. The platform Administrator retains the explicitly approved write access.
- Save/delete workflows check both submitted dates and a database predicate for the persisted historical season. Fee validation checks its season too; changing dates or a fee's season cannot unlock a historical record.
- The normal workspace lists at most two current/upcoming seasons by start date. All seasons and history is an explicit secondary tab.
- Season and fee forms use server-supplied capabilities for conditional editability. Past fees cannot be added or changed through ordinary club administration.
- Role permissions have an aligned Feature / Read / Create / Update / Delete matrix, with unsupported operations shown as unavailable.

Development data upgrade `features:season-roles-v2` adds previous/later seasons, their fees, two club roles, ten permission rows per role and disabled staff assignments. It extends the same ownership manifest and does not change existing account roles or existing demo work. New disabled staff accounts receive only the User and ClubAdmin app roles.

Verification must distinguish consistency/build checks from runtime checks. Pending runtime scenarios: two default seasons, explicit history, expired season/fee restrictions under club staff, Administrator historical editing, overlapping draft/published dates, populated CRUD matrix, upgrade idempotence and removal of upgraded records.

Verified on 2026-10-13: the count activity is configured to count the Selected list; Mendix reports zero consistency errors; the app's Java/model build succeeded and its runtime is running. The sample generator also passed a separate JDK 21 compilation. The feature upgrade runs on the next **Generate sample data** click; it has not yet been executed in the runtime.

Platform administrators can also add competition permissions to a club role through the same server-side Administrator-or-owner guard used for other role management. Ordinary user and staff restrictions remain unchanged.
