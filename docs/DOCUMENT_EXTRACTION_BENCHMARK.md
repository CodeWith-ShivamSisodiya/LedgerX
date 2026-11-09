# Financial document extraction — experimental baseline

## Current scope
This feature branch builds a **zero-cloud**, zero-subscription OCR-first prototype.
It is **not** a trained document-understanding model. The rules engine's
performance on real bank documents is unknown until measured.

The app extracts candidate fields into an editable draft; nothing from OCR
should be treated as a verified balance or contractual maturity amount.

## Three candidate extraction engines
1. **Original LedgerX keyword baseline** (legacy `WealthRules` in MainActivity):
   lexical labels, basic classification. Retained for comparison only.
2. **Semantic-label baseline** (`SemanticLabelEngine`):
   provider-neutral `DocumentExtractionEngine` interface, expanded aliases,
   adjacent-line values, source evidence for every extracted field. Connected
   to the scan workflow.
3. **Vision-language document understanding (future adapter)**:
   NOT enabled or tested. Needs an approved model/runtime or API configuration.
   Neither the Jio consumer subscription nor an unrestricted cloud API is
   required for the currently shipped MVP.

## Evaluation protocol
Obtain 5–10 user-supplied, redacted documents: at least three distinct FD
layouts, one RD statement, one mutual fund holding statement, plus a
multi-deposit table and a low-quality screenshot. Keep originals off GitHub.

For each case independently record:
- Document type accuracy
- Exact field value match, per field (principal, interest, start/maturity dates,
  maturity amount, institution and reference)
- Unsupported/nonexistent values hallucinated (must be zero)
- Multiple records extracted vs total actual records
- Human corrections required per document
- Processing latency on the user's S25
- Network transfer / privacy and monetary cost

Do **not** use aggregate accuracy on only synthetic fixtures as a proxy for
real performance. Every field should include evidence and a validation status.

## Release gates
- Tested image extraction on real **redacted** source variants
- Multi-record import with confirmation
- No counting projected maturity proceeds as *current* net worth
- Separate total liabilities from assets
- Ability to correct/delete records, encrypted import/export with recovery
- Device-level scan end-to-end testing

## Run the unit benchmark
`gradle testDebugUnitTest --tests "com.ledgerx.app.core.extraction.DocumentExtractionBenchmarkTest"`
