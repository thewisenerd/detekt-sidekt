import com.udaan.error.trace.annotations.Severity
import com.udaan.error.trace.annotations.UDErrorMonitored
import com.udaan.error.trace.annotations.UDErrorMonitoredApi

@UDErrorMonitored("1")
class TestAsecResource {

    @Path("/variant01")
    @UDErrorMonitoredApi("1", Severity.LOW, false)
    fun testASECVariant01() {
        "This is variation 01"
    }

    @Path("/variant02")
    fun testASECVariant02(): String {
        return "This is variation 02"
    }

    @GET
    fun testASECVariant03() {
        println("This is variation 03")
    }

    @GET
    @UDErrorMonitoredApi("1", Severity.LOW, false)
    fun testASECVariant08(): String {
        println("This is variation 04")
        return ""
    }

}
