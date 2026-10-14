# Saving fault recovery

Studio Pro reported that SE_Billing_IssueDue contains an OriginPointer on a sequence flow referencing a missing activity. This is the concrete storage error accompanying the microflow-operation timeouts.

The affected microflow's intended logic is retained in flow-specs-recovery.json. Current guidance is to keep the project open, discard only this document's unsaved edits if Studio Pro offers that option, then save the remaining work. Do not force-close Studio Pro while saving fails.

After recovery, read the current flow before repairing it. Prefer a minimal repair. If its graph needs reconstruction, remove all old flows before deleting any activities, including loop activities. Removing a loop does not reliably remove flow references to its deleted descendants. Preserve the document and parameter identities when replacing its implementation.

Resolved in Studio Pro on 2026-10-11. Closing the editor and Undo did not restore a readable graph. The saved model and reconstruction source were backed up to C:/Users/simon/AppData/Local/Temp/nexio-model-recovery-20261011-222641. Only the damaged generated SE_Billing_IssueDue document was removed through Studio Pro and recreated through the Mendix MCP. The daily caller was explicitly rebound to the replacement.

The replacement retrieves due draft instalment invoices belonging to active memberships, marks each Open with IssuedAt, and queues its email through SUB_Invoice_QueueMail. It does not mark email delivered or payment verified. The rebuilt graph renders in Studio Pro. Save All completes without the OriginPointer exception; Save and Save All are disabled afterward, indicating no unsaved editors.

The duplicate RequestedType and RequestedSeason variables in SUB_Registration_SelectedSeason were repaired by renaming the earlier fee-validation variables and updating that decision expression, preserving all activity and connection identities in that flow.

Consistency check 19 reports 29 remaining errors, all missing allowed-role selections in the unfinished phase-three pages and public microflows. Neither repaired microflow has a structural diagnostic. The application is not yet runnable until those access-role configuration errors are resolved.

The reconstruction helper now clears all flow references before removing activities, including loop descendants. Future repairs should use minimal updates where possible.
