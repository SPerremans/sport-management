# Member dashboard connection error / freeze fix

16 October 2026.

The reported My clubs page freezes after sign-in and displays the generic connection-error dialog, including after refreshing/restarting. Runtime logs contained repeated client `Failed to fetch` errors from `OnlineData.retrieveByMicroflow` / `MicroflowObjectListProperty`. The runtime itself remained running and Member_Dashboard passed model consistency. These logs alone do not establish the transport-level cause.

The redundant refresh was corrected through the Mendix connector: `MyFirstModule.DS_Club_Offers` created each non-persistent `ClubOffer` with **Refresh in client = Yes**. It now uses **No**. This data source returns the offers directly to its consuming widgets, including the nested tables on Member_Dashboard, Club_Directory and Club_Detail. The extra refresh is unnecessary while constructing the returned view rows. [Mendix documents the refresh setting here](https://docs.mendix.com/refguide/create-object/).

The change only sets `/objectCollection/objects/8/objectCollection/objects/5/action/refreshInClient` to false. Model readback confirmed that all offer fields, association assignments, object creation/commit settings and output variable remain unchanged. No database records, permissions, pages, business entities or workflows were added. Existing fictional club-discovery samples cover open, closed and unavailable registration options; the sample generator and cleanup manifest were not changed.

Verification is deliberately separate:

- **Model consistency:** targeted DS_Club_Offers and global checks returned No errors found.
- **Compilation/deployment:** Studio Pro's build succeeded in 8 seconds, regenerated the model/web artifacts, and the connector reported running=true after restart. No Java or theme source was changed by this fix.
- **Runtime interaction:** the user confirmed the requested fresh-tab sign-in check: "Responsive; error is gone" for My clubs, Upcoming events and Find a club. This is user-reported scenario verification, not an automated browser test. The exact transport failure was not captured; browser tool inspection remained blocked by a saved localhost policy, so no alternate browser/debugging surface was used.
- **Sample lifecycle:** not run; no new generated record types or scenarios were introduced.

The runtime also reports the separately documented `SE_Match_AcceptanceExpiry` private-system-job error. This investigation has not established a causal connection between that scheduled-job error and the browser freeze, and it has not modified that job.
