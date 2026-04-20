import com.boomi.execution.ExecutionUtil
import groovy.json.JsonSlurper
import groovy.json.JsonOutput

/**
 * Holds the outcome of validating a single record.
 * isValid() is true only when no errors have been added.
 */
class ValidationResult {
    List<String> errors = []

    void addError(String message) { errors.add(message) }
    boolean isValid()             { errors.isEmpty() }
    String summary()              { errors.join('; ') }
}

/**
 * Business-rule validator for customer records.
 * To add a new rule, write a private check... method
 * and call it from validate(). Keeps rules isolated and testable.
 */
class CustomerValidator {

    /** Runs every rule against the record and returns the combined result. */
    ValidationResult validate(Map record) {
        def result = new ValidationResult()
        checkRequiredFields(record, result)
        checkEmailFormat(record, result)
        checkUsZipCode(record, result)
        return result
    }

    private void checkRequiredFields(Map record, ValidationResult result) {
        ['firstName', 'lastName', 'email'].each { field ->
            if (!record[field] || record[field].toString().trim().isEmpty()) {
                result.addError("Missing required field: ${field}")
            }
        }
    }

    private void checkEmailFormat(Map record, ValidationResult result) {
        def email = record.email
        if (email && !(email ==~ /^[\w.+-]+@[\w-]+\.[\w.-]+$/)) {
            result.addError("Invalid email format: ${email}")
        }
    }

    private void checkUsZipCode(Map record, ValidationResult result) {
        if (record.country == 'US' && record.zip) {
            if (!(record.zip.toString() ==~ /^\d{5}(-\d{4})?$/)) {
                result.addError("Invalid US zip code: ${record.zip}")
            }
        }
    }
}

// ---- Main processing loop ----
def validator = new CustomerValidator()
def logger    = ExecutionUtil.getBaseLogger()

for (int i = 0; i < dataContext.getDataCount(); i++) {
    def inputStream = dataContext.getStream(i)
    def props       = dataContext.getProperties(i)

    def record = new JsonSlurper().parse(inputStream)
    def result = validator.validate(record)

    // Tag the document with DDPs so a Route shape can split on them
    props.setProperty('document.dynamic.userdefined.validationStatus',
        result.isValid() ? 'PASS' : 'FAIL')
    props.setProperty('document.dynamic.userdefined.validationErrors',
        result.summary())

    if (!result.isValid()) {
        logger.warning("Record ${i} failed validation: ${result.summary()}")
    }

    def output = JsonOutput.toJson(record)
    dataContext.storeStream(new ByteArrayInputStream(output.bytes), props)
}
