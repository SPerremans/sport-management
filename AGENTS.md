# Development examples

Whenever adding a business entity, page, or workflow, extend the development sample generator in `javasource/myfirstmodule/DevelopmentSamples.java` alongside it. Relevant feature views and development scenarios must have example data.

Support both fresh generation and upgrading an already existing demo batch. Add each feature version once, preserve user-edited demo records, and check idempotence. Register new persistent business types and their owning associations in the cleanup allowlist and dependency map. Every generated record must be covered by the private ownership manifest so **Remove sample data** remains complete and scoped.

Use fictional details, disabled test accounts, and unverified/unpaid sample invoices. Do not send email, confirm payments, or alter unrelated accounts or business records. Keep generation restricted to platform administrators in the local development runtime.

Report model consistency, compilation, and runtime scenario verification separately. Do not describe seeded examples or unexecuted checks as passing tests.

# Model organization

Put named pages in a folder that describes their business purpose. Do not create pages at the module root. Group related workflows with their feature, using subfolders when helpful. Reserve the module root for required singleton documents and feature folders.

Use business areas such as Competition, Billing, Clubs, Members, Registration, Accounts, and Federations. CRM is not a catch-all for the application. Avoid phase numbers and generic implementation labels for new feature folders.

Prefer mnd MCP operations. When a page generator would create a page at the root, first create its page document in the intended folder using the supported document API, then populate that existing page. Verify the resulting folder with the connector. Existing document moves must preserve document identity and references; use Studio Pro when the connector has no supported move operation.

Keep the folder guide in `docs/model-organization.md` current. Pure containment moves do not add business behavior and do not require additional sample scenarios.
