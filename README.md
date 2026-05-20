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
    type: date
    output_format: yyyy-MM-dd
    stages:
      - target: metadata
        metadata_key: keywords
        regex: "invoice-date=(\\d{4}-\\d{2}-\\d{2})"
        group: 1
        input_format: yyyy-MM-dd
      - target: content
        regex: "Date:\\s+(\\d{2}\\.\\d{2}\\.\\d{4})"
        group: 1
        input_format: dd.MM.yyyy

  cost:
    regex: "Total:\\s+([0-9]+,[0-9]{2} €)"
    group: 1

static:
  issuer: "Example Provider"
  tags:
    - expense

markdown_filename: "{{invoice_date}} Example Invoice {{invoice_number}}"
pdf_filename: "{{invoice_date}} Example Receipt {{invoice_number}}"

markdown: |
  ---
  name: Example Invoice {{invoice_number}}
  receipt: "[[{{pdf_basename}}.pdf]]"
  cost: {{cost}}
  date: {{invoice_date}}
  issuer: {{issuer}}
  tags:
  {% for tag in tags %}  - {{tag}}
  {% endfor %}
  ---
```

### Field Rules

Each field under `fields` extracts one value. By default, regexes are applied to the extracted PDF text.

- `regex`: Java regular expression applied to the selected extraction target.
- `group`: capture group to use, usually `1`.
- `type: date`: optional date coercion.
- `input_format`: Java `DateTimeFormatter` pattern for parsed dates.
- `output_format`: Java `DateTimeFormatter` pattern for rendered dates.
- `target`: extraction target. Defaults to `content`; use `metadata` for PDF metadata.
- `metadata_key`: metadata field to read when `target: metadata`, for example `keywords`.

For multi-stage extraction, add `stages`. Stages are tried in order and the first matching value is used:

```yaml
invoice_date:
  type: date
  output_format: yyyy-MM-dd
  stages:
    - target: metadata
      metadata_key: keywords
      regex: "invoice-date=(\\d{4}-\\d{2}-\\d{2})"
      group: 1
      input_format: yyyy-MM-dd
    - regex: "Date:\\s+(\\d{2}\\.\\d{2}\\.\\d{4})"
      group: 1
      input_format: dd.MM.yyyy
```

If a field has no `stages` key, the field itself is treated as a single stage. When a stage has no `target`, it defaults to `content`.

Values under `static` are copied directly into the render context.

### Templates

`markdown_filename`, `pdf_filename`, and `markdown` are Selmer templates. Extracted fields, static values, `markdown_basename`, `pdf_basename`, and `basename` are available as template variables. `basename` is the same as `markdown_basename` for simple existing templates.

The generated paths are:

- Markdown: `<markdown-dir>/<markdown_basename>.md`
- Receipt PDF: `<receipt-dir>/<pdf_basename>.pdf`

The source PDF name is not trusted. Output files are named from the rendered filename templates. For simple configs, `filename` can be used as a fallback for both `markdown_filename` and `pdf_filename`.
