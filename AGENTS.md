# Development examples

Whenever adding a business entity, page, or workflow, extend the development sample generator in `javasource/myfirstmodule/DevelopmentSamples.java` alongside it. Relevant feature views and development scenarios must have example data.

Support both fresh generation and upgrading an already existing demo batch. Add each feature version once, preserve user-edited demo records, and check idempotence. Register new persistent business types and their owning associations in the cleanup allowlist and dependency map. Every generated record must be covered by the private ownership manifest so **Remove sample data** remains complete and scoped.

Use fictional details, disabled test accounts, and unverified/unpaid sample invoices. Do not send email, confirm payments, or alter unrelated accounts or business records. Keep generation restricted to platform administrators in the local development runtime.

Report model consistency, compilation, and runtime scenario verification separately. Do not describe seeded examples or unexecuted checks as passing tests.
