import kotlinx.coroutines.runBlocking


    @Path("/variant01")
    fun testRunBlockingVariant01() = runBlocking {
            "This is variation 01"
    }

    @Path("/variant02")
    fun testRunBlockingVariant02() : String = runBlocking{
        "This is variation 02"
    }

    @Path("/variant03")
    fun testRunBlockingVariant03()  = runBlocking{
        return@runBlocking "This is variation 03"
    }

    @Path("/variant04")
    fun testRunBlockingVariant04() : String {
        return runBlocking{
            return@runBlocking "This is variation 04"
        }
    }

    @Path("/variant05")
    fun testRunBlockingVariant05() : String {
        println("This is variation 05")

        runBlocking {
            "This is variation 05"
        }

        return "This is variation 05"
    }

    @Path("/variant06")
    fun testRunBlockingVariant06() : String {
        println("This is variation 06")
        return "This is variation 06"
    }

    @GET
    fun testRunBlockingVariant07() = runBlocking {
        println("This is variation 07")
    }

    @GET
    fun testRunBlockingVariant08() : String {
        println("This is variation 08")
        kotlinx.coroutines.runBlocking {
            return@runBlocking "This is variation 08"
        }
        return ""
    }