# Club discovery, sample logins and shared tables

Implemented on 15 October 2026.

Club discovery uses a database-backed Data Grid 2 text filter across published club names, descriptions, addresses, towns and postcodes. The standalone page and member dashboard use the same configuration. Current available membership options, their published season and fee appear in each club's Details column; Register keeps the existing server registration checks. The unused distance/radius form is removed from both views.

The sample generator appends three fictional Belgian discovery clubs: Leuven Shuttlekring (Adult and Junior open; closed and future options hidden), Gent Racketvrienden (no current open registrations), and an unpublished Mechelen Trainingsclub. Fresh base examples also use fictional Belgian club names and addresses. Existing demo club details are preserved on upgrade.

The generator adds a dedicated active administrator to every manifest-owned sample club and federation. Club accounts have only User and ClubAdmin app roles; federation accounts have only User and FederationAdmin. Ownership associations scope each account to its own organization. Other sample accounts remain disabled. Local test password: `TestAccount123!`. Normal application password-policy settings are unchanged.

The user chose to retain the existing password policy. Mendix requires a minimum of 12 characters, mixed case and a digit, so the shorter suggested `Test1234!` is also rejected. The compatible alternative explained in the conversation, `TestAccount123!`, is configured instead. Supported Mendix validation remains in place; no password-hash bypass is used. If a future policy rejects the sample password, the generator warns and skips the administrator extension without blocking the other examples, leaving its version marker unset for a later retry. These logins have not been created or tested in this change.

Login names are `demo.club.<organization ID>@example.invalid` and `demo.federation.<organization ID>@example.invalid`; generation writes the actual names to the NexioSamples runtime log. Run **Checks → Sample data → Generate sample data** as a platform administrator in the local development runtime to add these to an existing sample batch. Repeating generation preserves edited examples and test accounts, including later password/activation changes. Remove sample data covers the new clubs, seasons, membership types, fees and test accounts through the private ownership manifest and existing cleanup dependency map.

Registrations **Read + Update** now authorizes approval and fee adjustment; fee changes still require a reason and retain numeric/invoice validation. The separate role toggle is removed. The legacy CanAdjustFee storage field is retained for compatibility and no longer authorizes fee changes.

Shared table styling is in theme/web/_nexio-tables.scss, imported after the other app styles. The subsequent table correction replaces 69 repeating list layouts with Data Grid 2 on 34 pages. Each table owns one header, aligned columns, soft-coloured action buttons, subtle separators and horizontal scrolling. Existing widget names, sources, action mappings and input bindings are preserved. See [table standards](table-standards.md) and [current verification](table-validation.json).

Validation status:

- Model consistency: global connector diagnostics returned no errors after the page and permission changes.
- Compilation: DevelopmentSamples.java compiled with JDK 21; Studio Pro deployment completed and the shared SCSS compiled. The app reported running after restart.
- Static contracts: the final table conversion preserved 159 button actions, 52 input definitions, 339 attribute references and all existing data sources across the 34 pages. This is structural verification, not runtime workflow coverage.
- Runtime scenarios: search, registration, sample generation/upgrade/removal, test-admin login/isolation, fee approval and browser visual checks have not been executed in this change. Browser access to localhost was declined; no alternate browser path was used.
