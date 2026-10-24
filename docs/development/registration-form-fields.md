# Registration form and federation fields

A member registration asks the club's own questions and the fields the federation requires. Each piece of information is asked once.

## Who defines what

| Owner | Where | What |
| --- | --- | --- |
| Federation | Federation workspace › **Registration requirements** (`ACT_FederationField_Create`, `FormField_Edit`) | Fields every registration with an affiliated club must contain, such as date of birth and gender. A field is a `FormField` linked by `FormField_Federation`. New fields start as required and inactive. |
| Club | Club workspace › **Registration form** | The club's own questions (`FormField_Club`). Below them, **Federation requirements** lists the active fields of every active federation the club is affiliated with (approved, not ended), and shows which club question covers each field. |

## Mapping club questions to federation fields

In the question editor, a club question has **Covers federation fields** (`FormField_FederationFields`, a reference set from the club question to federation fields). The choices come from `DS_FormField_FederationOptions`: the active fields of the club's current federations, shown as "Federation – Field".

One club question can cover the same field of several federations. For example, "Date of birth" can cover both the badminton and the tennis federation's date of birth, so overlapping federations do not produce duplicate questions. `ACT_FormField_Save` applies entity access and enforces two rules:

- The answer type must match every covered field. Otherwise the save is refused with a message.
- The question becomes required when any covered field is required.

## What the member sees

`ACT_Registration_Begin` (new registrations) and `SUB_Renewal_EnsureDraft` (renewal drafts) snapshot as `FormResponse` records:

- every active club question;
- every active field of the membership type's federation (membership type › federation type › federation) that no active club question of this club covers.

Existing registrations keep their snapshot. `Registration_Edit` shows the snapshot as a stacked form: one question per row, with the label above the input, `*` for required questions and the help text below. The input depends on the answer type:

- **Text and Number:** text box.
- **Date:** date picker (`AnswerDate`).
- **YesNo:** Yes/No radio buttons (`AnswerChoice`, `YesNoAnswer`).
- **Gender (federation options):** combo box with the federation's active gender options (`FormResponse_Gender`); see [federation-teams-and-genders.md](federation-teams-and-genders.md).

`Answer` remains the canonical text value. `ACT_Registration_Submit` copies `AnswerDate` (as `yyyy-MM-dd`) and `AnswerChoice` (as `yes`/`no`) into `Answer` before its existing validation, so staff review and validation are unchanged. A draft answer typed as text before this change, for a Date or YesNo question, must be entered again in the new control.

The answer list on `Registration_Edit` is an editable list view. A non-editable list view renders every input inside it read-only.

## Restarting and finding registrations

- **Start a new registration** shows on `Registration_Edit` only for the member's own withdrawn or rejected registration. `ACT_Registration_Restart` closes the page and runs `ACT_Registration_Begin` for the same membership type. Begin rechecks availability, the current season and price, duplicates, and the current club and federation questions. The ended registration stays as history, and its answers are not copied.
- **My registrations** (`Registrations`) uses the standard Data grid 2 filters: a search box (member, season, next action) in the filter bar, and multi-select dropdown filters on the Registration, Approval, Payment and Club columns.

## Access

- Members read and write only their own draft responses, including the new `AnswerDate` and `AnswerChoice` (unchanged rule shape).
- Club administrators and club staff with **Forms** read permission can read the fields of their club's affiliated federations. They cannot edit them.
- Federation administrators create, edit and delete their own federation's fields. Club questions stay editable only through the existing club rules, including the new mapping.

## Development examples

`extendFederationFieldSamples` (feature `features:federation-registration-fields-v1`) adds the following, once, to fresh and existing sample batches:

- Every sample federation gets three required fields: **Date of birth** (Date) and **Gender** (Text), which overlap between federations, plus a per-sport **code of conduct accepted** (YesNo). A federation that already has fields is left alone.
- Every sample club that has none yet gets a club **Date of birth** question covering the date-of-birth field of each of its current federations. Gender and code of conduct stay uncovered, so they show up as automatic questions. "[DEMO] Gent Multi Rackets" shows one question covering both the badminton and the tennis federation.
- Open sample registrations get the questions they are missing: pending ones with fictional answers (1990-05-14, "[DEMO] x", yes), drafts empty. Existing answers are not changed.

All new records are in the ownership manifest. `FormField_Federation` is a cleanup dependency edge, so fields added by hand to a sample federation are removed with it. `FormField_FederationFields` is deliberately not an edge: a real club question covering a sample field only loses that link on cleanup. `verifyFederationFields` checks that the multi-federation club asks date of birth exactly once and still asks gender. This check runs only inside the generator's own verification; it is not a runtime test.

## Verification — 21 October 2026

- **Model consistency:** every new and changed page, microflow and the domain model report no errors except the known "Entity access is out of date" in MyFirstModule. Click **Update security** in Studio Pro.
- **Compilation:** `DevelopmentSamples.java` compiles with JDK 21 against the Mendix 11.15 runtime jars (standalone `javac`, not a Studio Pro build). The full app build was not run.
- **Runtime scenarios:** not executed. That covers the stacked registration form, the date and yes/no inputs and their copy on submit, the mapping combo box and its "Federation – Field" captions, the requirements overview, federation field creation, the omission of covered fields on Begin and renewal, and sample generation.
