import com.udaan.error.trace.annotations.Severity
import com.udaan.error.trace.annotations.UDErrorMonitored
import com.udaan.error.trace.annotations.UDErrorMonitoredApi

@UDErrorMonitored("1")
class TestApsecResource {

    @Path("/variant01")
    @UDErrorMonitoredApi("1", Severity.LOW, false)
    fun testAPSECVariant01() {
        "This is variation 01"
    }

    @Path("/variant02")
    fun testAPSECVariant02(): String {
        return "This is variation 02"
    }

    @GET
    fun testAPSECVariant03() {
        println("This is variation 03")
    }

    @GET
    @UDErrorMonitoredApi("1", Severity.LOW, false)
    fun testAPSECVariant08(): String {
        println("This is variation 04")
        return ""
    }

}
