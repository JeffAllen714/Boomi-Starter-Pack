# Boomi Starter Pack

A small collection of Groovy script templates for the Boomi **Data Process shape**. Each file is a self-contained starting point with a clean class-based structure, Groovydoc comments, and the standard `dataContext` processing loop already wired up.

## What's in here

| File | Purpose |
|---|---|
| `dataCleanup.groovy` | Normalize strings, phone numbers, emails, and dates |
| `validation.groovy` | Run business rules against records and tag pass/fail via DDPs |
| `jsonTransform.groovy` | Reshape nested JSON payloads into a flatter structure |
| `dynamicProperties.groovy` | Stamp DDPs, DPPs, and timestamped filenames at runtime |
| `loggingAndErrors.groovy` | Context-aware logging and document-scoped error wrapping |
| `xmlTransform.groovy` | Parallel to the JSON transformer, using `XmlSlurper` / `MarkupBuilder` |
| `csvProcessor.groovy` | Parse and write CSV with quoted-field handling |



- The DDP key prefix `document.dynamic.userdefined.` is required — Boomi looks for that exact string.
- Classes defined inside a Data Process script are **local to that script**. Sharing code across scripts requires a custom library or JAR upload.
- Most templates assume JSON input. Swap the slurper for `XmlSlurper`, raw text, or CSV parsing depending on what flows into your shape.
- Throwing an exception halts the process by default. If you'd rather continue past bad records, tag them with a DDP and route failures downstream instead of throwing.

## Testing locally

You can paste any of the class definitions into the [Groovy Playground](https://groovy-playground.appspot.com/) or a local Groovy install to unit-test the logic before deploying to Boomi. Mock `dataContext` with a simple stub if you want to exercise the full loop.
