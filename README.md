# invoice2md

`invoice2md` converts machine-readable invoice PDFs into Markdown notes and copies the source PDFs to matching receipt filenames.

The current implementation is JVM-only and focuses on the conversion pipeline. Fetching invoices from providers will be added later.

## Requirements

- Java
- Clojure CLI / `clj`

## Development

Run the linter:

```bash
clj -M:lint
```

This repo includes a local pre-commit hook that runs the same command. The tracked hook template lives at `scripts/hooks/pre-commit`.

## Run

Convert PDFs from `inbox/` into Markdown files in `out_md/` and renamed receipt PDFs in `out_pdf/`:

```bash
clj -M:run convert \
  --config config/deutsche-bahn.yml \
  --pdf-dir inbox \
  --markdown-dir out_md \
  --receipt-dir out_pdf
```

Preview actions without writing files:

```bash
clj -M:run convert \
  --config config/deutsche-bahn.yml \
  --pdf-dir inbox \
  --markdown-dir out_md \
  --receipt-dir out_pdf \
  --dry-run
```

Overwrite existing Markdown and receipt PDFs:

```bash
clj -M:run convert \
  --config config/deutsche-bahn.yml \
  --pdf-dir inbox \
  --markdown-dir out_md \
  --receipt-dir out_pdf \
  --overwrite
```

By default, if the target Markdown file already exists, the invoice is skipped. This makes repeated runs safe and preserves manual edits.

## Configs

Configs are YAML files. See `config/deutsche-bahn.yml` for the current Deutsche Bahn invoice profile.

Basic shape:

```yaml
profile: my-provider

defaults:
  overwrite: false

fields:
  invoice_number:
    regex: "Invoice No\\.\\s+(\\d+)"
    group: 1

  invoice_date:
    regex: "Date:\\s+(\\d{2}\\.\\d{2}\\.\\d{4})"
    group: 1
    type: date
    input_format: dd.MM.yyyy
    output_format: yyyy-MM-dd

  cost:
    regex: "Total:\\s+([0-9]+,[0-9]{2} €)"
    group: 1

static:
  issuer: "Example Provider"
  tags:
    - expense

filename: "{{invoice_date}} Example Invoice {{invoice_number}}"

markdown: |
  ---
  name: Example Invoice {{invoice_number}}
  receipt: "[[{{basename}}.pdf]]"
  cost: {{cost}}
  date: {{invoice_date}}
  issuer: {{issuer}}
  tags:
  {% for tag in tags %}  - {{tag}}
  {% endfor %}
  ---
```

### Field Rules

Each field under `fields` extracts one value from the PDF text.

- `regex`: Java regular expression applied to the extracted PDF text.
- `group`: capture group to use, usually `1`.
- `type: date`: optional date coercion.
- `input_format`: Java `DateTimeFormatter` pattern for parsed dates.
- `output_format`: Java `DateTimeFormatter` pattern for rendered dates.

Values under `static` are copied directly into the render context.

### Templates

`filename` and `markdown` are Selmer templates. Extracted fields, static values, and `basename` are available as template variables.

The generated paths are:

- Markdown: `<markdown-dir>/<basename>.md`
- Receipt PDF: `<receipt-dir>/<basename>.pdf`

The source PDF name is not trusted. The output PDF is always named from the rendered `filename` template.
