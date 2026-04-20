import com.boomi.execution.ExecutionUtil
import groovy.json.JsonSlurper
import groovy.json.JsonOutput

/**
 * Reshapes an incoming customer payload into the flatter structure
 * our downstream system expects. Each sub-transform lives in its own
 * private method so mapping changes stay local and easy to review.
 */
class JsonTransformer {

    /** Top-level transform: returns the reshaped map. */
    Map transform(Map source) {
        return [
            customerId  : source.id,
            fullName    : buildFullName(source),
            contact     : flattenContact(source),
            orderSummary: summarizeOrders(source.orders ?: [])
        ]
    }

    private String buildFullName(Map source) {
        def parts = [source.firstName, source.middleName, source.lastName]
        return parts.findAll { it }.join(' ')
    }

    private Map flattenContact(Map source) {
        def contact = source.contact ?: [:]
        return [
            email: contact.email,
            phone: contact.phone,
            city : contact.address?.city,
            state: contact.address?.state
        ]
    }

    private Map summarizeOrders(List orders) {
        return [
            totalOrders: orders.size(),
            totalSpend : orders.sum { it.amount ?: 0 } ?: 0,
            lastOrderId: orders ? orders[-1].id : null
        ]
    }
}

// ---- Main processing loop ----
def transformer = new JsonTransformer()

for (int i = 0; i < dataContext.getDataCount(); i++) {
    def inputStream = dataContext.getStream(i)
    def props       = dataContext.getProperties(i)

    def source      = new JsonSlurper().parse(inputStream)
    def transformed = transformer.transform(source)

    def output = JsonOutput.prettyPrint(JsonOutput.toJson(transformed))
    dataContext.storeStream(new ByteArrayInputStream(output.bytes), props)
}
