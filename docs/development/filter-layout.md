# Filter layout — 23 October 2026

- Shared discovery filters (Club_Directory, Member_Dashboard and Accounts_Overview) now use matching text geometry for native inputs and comboboxes. Combobox idle captions are separate absolutely positioned spans; these now have explicit padding and line height matching the actual input. Removed the widget's negative chip-container margin within these bars, centered icons, and retained wrapping for selected chips.
- Registrations has one wrapping filter area containing search, registration status, approval, payment and club, followed by the existing action row. Column headings no longer contain filters. Moved dropdown filters use explicit attributes and the enclosing grid's linked data context; club text filtering retains its association path. Search retains MemberName, SeasonName and NextAction.
- Connector readback confirmed all seven column filter slots are empty, all five filter widgets appear in the new area, and the action container and data source are unchanged.
- Model consistency: Registrations returned **No errors found** after the edit and on readback. Global consistency was not checked for this presentation change.
- Compilation: the complete deployment/sass/main.scss entry point compiled successfully with the installed Mendix Dart Sass compiler. Full application/Java compilation was not run.
- Runtime scenarios/visual QA: not executed. The available browser session redirects to login.html; authenticated pages and selection/filter interactions could not be inspected.
- No business entities, pages or workflows were added. Existing development sample scenarios remain applicable; fresh generation, upgrade, idempotence and removal were not executed.

## Deployment correction later on 23 October

The initial dropdown configuration included an explicit `Pages$ListenTargetSource` in `linkedDs`. Model validation accepted it, but page export threw InvalidOperationException in WithDataSourceProperty while exporting Registrations. Removed that unsupported override from all three dropdown filters. They retain Custom attribute mode and the Status/Approval/Payment references and use their enclosing grid context, like the existing text filters.

After correction: page diagnostics returned No errors found; Studio Pro local run completed and runtime status returned running=true, launching=false. The newly generated Registrations.js contains registrationFilters and the moved dropdowns (file timestamp 15:48 on 23 October). This verifies page export, unlike the initial model-only check. Authenticated filter interaction remains unverified.

## Shared table toolbar padding

Club setup exposed that adjacent search and action rows had no vertical gap. Shared nexio-data-table headers now have 12px padding and a 12px gap between their filter/search and action sections. Registrations drops its additional filter-row bottom margin inside this header so that it uses the same single gap. This is a theme-only adjustment; page actions and data bindings are unchanged. Complete theme compilation and diff whitespace checks passed. Model consistency and full application compilation were not rerun for this CSS-only change; authenticated visual/runtime verification remains unavailable.
