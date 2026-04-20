import com.boomi.execution.ExecutionUtil
import groovy.json.JsonSlurper
import groovy.json.JsonOutput

/**
 * Utility class for cleaning and normalizing common data fields.
 * Every method handles null/empty values gracefully and returns
 * null when the input can't be meaningfully cleaned — that way
 * downstream mapping logic has a single "missing" signal to check.
 */
class DataCleaner {

    /** Trims whitespace; returns null for null or empty strings. */
    static String cleanString(String value) {
        if (value == null) return null
        def trimmed = value.trim()
        return trimmed.isEmpty() ? null : trimmed
    }

    /** Strips a phone number to 10 US digits, or null if invalid. */
    static String normalizePhone(String phone) {
        if (phone == null) return null
        def digits = phone.replaceAll(/\D/, '')
        if (digits.length() == 11 && digits.startsWith('1')) {
            digits = digits.substring(1)
        }
        return digits.length() == 10 ? digits : null
    }

    /** Lowercases and trims an email address. */
    static String normalizeEmail(String email) {
        return cleanString(email)?.toLowerCase()
    }

    /** Converts MM/dd/yyyy to ISO 8601 (yyyy-MM-dd). */
    static String toIsoDate(String usDate) {
        def cleaned = cleanString(usDate)
        if (cleaned == null) return null
        try {
            def parts = cleaned.split('/')
            return String.format('%04d-%02d-%02d',
                parts[2] as Integer,
                parts[0] as Integer,
                parts[1] as Integer)
        } catch (Exception e) {
            return null
        }
    }
}

// ---- Main processing loop ----
for (int i = 0; i < dataContext.getDataCount(); i++) {
    def inputStream = dataContext.getStream(i)
    def props       = dataContext.getProperties(i)

    def record = new JsonSlurper().parse(inputStream)

    record.firstName = DataCleaner.cleanString(record.firstName)
    record.lastName  = DataCleaner.cleanString(record.lastName)
    record.email     = DataCleaner.normalizeEmail(record.email)
    record.phone     = DataCleaner.normalizePhone(record.phone)
    record.birthDate = DataCleaner.toIsoDate(record.birthDate)

    def output = JsonOutput.toJson(record)
    dataContext.storeStream(new ByteArrayInputStream(output.bytes), props)
}
