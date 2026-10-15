# Shared application tables

Administrative overviews use **Mendix Data Grid 2**, with one column-header definition per table. The repeating list-item heading pattern has been removed from 69 tables on 34 pages, including nested fee/registration tables and current/archived seasons. Do not put column headings inside repeating record templates.

Shared styles live in `theme/web/_nexio-tables.scss`. Change them centrally to keep the same typography, surfaces, spacing and buttons throughout the application.

- Give each field a named column; group record operations under Actions.
- Fill available desktop width, fit action columns to their content, and use column minimum widths. Wide tables scroll together horizontally.
- Use 14px row text, 13px headers, subtle separators and alternating surfaces.
- Use compact soft-coloured buttons, with semantic destructive/confirming states and visible keyboard focus.
- Preserve action handlers, confirmation dialogs, visibility conditions, validation, event handlers and server checks.
- Data Grid 2 owns headers, rows and paging. Preserve data-source scoping, default ordering and page sizes.
- Global text search uses Custom mode, linked to its grid via ListenTargetSource. Preserve all original search attributes. Auto mode in the toolbar drops those attributes and must not be used for global search.
- Keep inline controls in custom-content columns and related lists in nested tables within Details.
- Keep the permissions editor's existing single-header CRUD matrix and shared palette/density.

See `table-inventory.json` for tables and columns, and `table-validation.json` for verification. Model readback confirmed preservation of 159 button actions, 52 input definitions, 339 attribute references and all original data sources. This is structural verification, not an interaction test.

Static text contrast calculations pass AA in default, hover and active row-action states (minimum 4.60:1). Button borders on white exceed 3:1; primary keyboard focus on white is 5.75:1. Browser appearance, keyboard interaction, filtering and scrolling remain unverified because earlier browser access was declined.

This change replaces presentation widgets only. Existing sample records remain the examples for these views. No business entity, page, workflow or generated business record was added. Fresh/upgrade/removal sample scenarios were not rerun.
