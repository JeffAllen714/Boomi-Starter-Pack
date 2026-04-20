import com.boomi.execution.ExecutionUtil

/**
 * Parses CSV into a list of maps keyed by header name, and writes maps
 * back out as CSV. Handles quoted fields and embedded delimiters, but
 * is intentionally minimal — if you need full RFC 4180 support, reach
 * for a proper library (opencsv, commons-csv) via a JAR upload.
 */
class CsvProcessor {

    String delimiter

    CsvProcessor(String delimiter = ',') {
        this.delimiter = delimiter
    }

    /** Parses CSV text into row maps. First non-empty line is the header. */
    List<Map> parse(String csvText) {
        def lines = csvText.readLines().findAll { !it.trim().isEmpty() }
        if (lines.isEmpty()) return []

        def headers = splitLine(lines[0])
        return lines.drop(1).collect { line ->
            def values = splitLine(line)
            [headers, values].transpose().collectEntries { k, v -> [k, v] }
        }
    }

    /** Serializes row maps back to CSV. Columns come from the first row's keys. */
    String write(List<Map> rows) {
        if (rows.isEmpty()) return ''
        def headers = rows[0].keySet().toList()
        def out     = new StringBuilder()

        out.append(headers.join(delimiter)).append('\n')
        rows.each { row ->
            def line = headers.collect { quote(row[it]?.toString() ?: '') }.join(delimiter)
            out.append(line).append('\n')
        }
        return out.toString()
    }

    private List<String> splitLine(String line) {
        def result  = []
        def current = new StringBuilder()
        boolean inQuotes = false

        line.each { ch ->
            if (ch == '"') {
                inQuotes = !inQuotes
            } else if (ch == delimiter && !inQuotes) {
                result.add(current.toString())
                current = new StringBuilder()
            } else {
                current.append(ch)
            }
        }
        result.add(current.toString())
        return result
    }

    private String quote(String value) {
        if (value.contains(delimiter) || value.contains('"') || value.contains('\n')) {
            return '"' + value.replace('"', '""') + '"'
        }
        return value
    }
}

// ---- Main processing loop ----
def processor = new CsvProcessor()

for (int i = 0; i < dataContext.getDataCount(); i++) {
    def inputStream = dataContext.getStream(i)
    def props       = dataContext.getProperties(i)

    def csvText = inputStream.text
    def rows    = processor.parse(csvText)

    // Example transformation: lowercase every email field
    rows.each { row ->
        if (row.email) row.email = row.email.toLowerCase()
    }

    def output = processor.write(rows)
    dataContext.storeStream(new ByteArrayInputStream(output.bytes), props)
}
