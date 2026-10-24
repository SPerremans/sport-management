# Back navigation and action visibility

## Back on every full-screen page

Every full-screen page (`Atlas_Default` layout) without its own Close or Cancel action starts with a `nexioBackBar`: a **← Back** link (`ClosePageClientAction`, one page) that returns to the previous page. Popups already have their close button and footer actions. Three pages are deliberately excluded:

- `Member_Dashboard`: the start page; there is nothing to go back to.
- `PasswordReset`: reached from an email link, outside the app's page history.
- `Invoice_Detail_Styled`: excluded from the project.

New full-screen pages should include the same back bar, unless they already have a Close or Cancel action. Styling is in `_nexio.scss` (`.nexio-back-bar`).

## Buttons the user cannot use are hidden

The server always rechecks permissions. Pages hide actions the user cannot perform, so nobody sees a button that only answers "not allowed":

- **Multi-select table toolbars:** buttons show only when they apply to the selection (see [table-toolbars.md](table-toolbars.md)).
- **Published fixtures rows:** each row's action buttons sit in a data view on a transient `FixtureCapability`, filled by `DS_CompetitionFixture_Capabilities` with the same checks as the actions:
  - **Propose date change** needs the update right on the home or away club.
  - **Federation placement / override** needs the federation's Override right for published fixtures, or Schedule otherwise.

  This costs one data-source call per visible row (page size 50).

Not covered yet: the fixtures toolbar's single-row Propose date change and Placement still show for any single selection, and the server still refuses them when the user lacks the right. Other pages have not been audited for this rule.

## Verification — 21 October 2026

- **Model consistency:** global diagnostics report no errors.
- **Compilation and runtime:** not run. In particular, check in the running app that the row data view receives the fixture of its own row.
