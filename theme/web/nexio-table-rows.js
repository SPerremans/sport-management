/* Data Grid 2 rows: a click on the row selects only that row; the checkboxes in front keep multi-select.
   A double click selects only that row and runs the toolbar's View (or Open) action.
   Server checks are unchanged: the toolbar button still sends the grid selection to its microflow. */
(() => {
  "use strict";
  // Each checkbox change starts its own toolbar refresh on the server; changes are spaced out so the last refresh wins.
  const SETTLE = 450, BUTTON_WAIT = 2500;
  const IGNORE = "input, button, a, select, textarea, label, [role='button'], .widget-datagrid-col-select";
  const wait = ms => new Promise(resolve => setTimeout(resolve, ms));
  let queue = Promise.resolve();
  const enqueue = work => (queue = queue.then(work, work));

  const rowOf = target => {
    const cell = target.closest(".widget-datagrid-selection-method-checkbox .td");
    return cell && !cell.closest(".th") ? cell.closest(".tr[role='row']") : null;
  };
  const checkbox = row => row && row.querySelector(".widget-datagrid-col-select input[type='checkbox']");
  const rowBoxes = grid => Array.from(grid.querySelectorAll(".tr[role='row'] .widget-datagrid-col-select input[type='checkbox']")).filter(box => !box.closest(".th"));
  const textSelected = () => { const s = window.getSelection(); return s && !s.isCollapsed && s.toString().trim() !== ""; };

  function viewButton(grid) {
    const buttons = Array.from(grid.querySelectorAll(".nexio-table-actions button, .nexio-table-actions .btn"))
      .filter(b => b.offsetParent !== null && !b.disabled);
    for (const caption of ["View", "Open"]) {
      const match = buttons.find(b => b.textContent.trim() === caption);
      if (match) return match;
    }
    return null;
  }

  /** Make the row the only selected row: clear the others first, let their refresh settle, then tick the row. */
  async function selectOnly(row) {
    const grid = row.closest(".widget-datagrid"), own = checkbox(row);
    if (!grid || !own || !own.isConnected) return;
    let cleared = false;
    for (const box of rowBoxes(grid)) if (box !== own && box.checked) { box.click(); cleared = true; }
    if (!own.checked) { if (cleared) await wait(SETTLE); own.click(); }
  }

  async function openView(row) {
    const grid = row.closest(".widget-datagrid"), own = checkbox(row);
    if (!grid || !own || !own.isConnected) return;
    const ready = async () => {
      const until = Date.now() + BUTTON_WAIT;
      while (Date.now() < until) {
        const button = viewButton(grid);
        if (button && own.checked && rowBoxes(grid).filter(b => b.checked).length === 1) return button;
        await wait(100);
      }
      return null;
    };
    let button = await ready();
    if (!button && own.checked) {
      // A late refresh from an earlier selection can hide the action; re-select the row once so the final refresh matches.
      own.click(); await wait(SETTLE); own.click(); button = await ready();
    }
    if (button) button.click();
  }

  document.addEventListener("click", event => {
    if (event.detail > 1 || event.button !== 0 || event.ctrlKey || event.metaKey || event.shiftKey || event.target.closest(IGNORE)) return;
    const row = rowOf(event.target);
    if (row && checkbox(row) && !textSelected()) enqueue(() => selectOnly(row));
  });

  document.addEventListener("dblclick", event => {
    if (event.target.closest(IGNORE)) return;
    const row = rowOf(event.target);
    if (!row || !checkbox(row)) return;
    window.getSelection()?.removeAllRanges();
    enqueue(() => selectOnly(row)).then(() => enqueue(() => openView(row)));
  });
})();
