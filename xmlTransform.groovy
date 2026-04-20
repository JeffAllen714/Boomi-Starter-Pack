import com.boomi.execution.ExecutionUtil
import groovy.xml.XmlSlurper
import groovy.xml.MarkupBuilder

/**
 * Reshapes an incoming XML document into a different XML structure.
 * Mirrors the JSON transformer pattern: one public transform() entrypoint,
 * private helpers per sub-section. Keeps the mapping logic easy to review.
 */
class XmlTransformer {

    /** Takes parsed XML (a GPathResult) and returns the reshaped XML as a String. */
    String transform(def source) {
        def writer = new StringWriter()
        def xml    = new MarkupBuilder(writer)

        xml.customer(id: source.@id.text()) {
            fullName(buildFullName(source))
            contact {
                email(source.contact.email.text())
                phone(source.contact.phone.text())
            }
            orderCount(source.orders.order.size())
        }
        return writer.toString()
    }

    private String buildFullName(def source) {
        def first = source.firstName.text()
        def last  = source.lastName.text()
        return "${first} ${last}".trim()
    }
}

// ---- Main processing loop ----
def transformer = new XmlTransformer()

for (int i = 0; i < dataContext.getDataCount(); i++) {
    def inputStream = dataContext.getStream(i)
    def props       = dataContext.getProperties(i)

    def source = new XmlSlurper().parse(inputStream)
    def output = transformer.transform(source)

    dataContext.storeStream(new ByteArrayInputStream(output.bytes), props)
}
