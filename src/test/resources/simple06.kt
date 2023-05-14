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


@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class BlockingClass

interface MyDao {
    fun daoCall(): Int {
        return 42
    }
}

@BlockingClass
interface MyInterface {
    fun blockingInterfaceFunction(): Int {
        return 42
    }
}

@BlockingClass
abstract class MyClass {
    fun blockingClassFunction(): Int {
        return 42
    }

    abstract fun dao(): MyDao
}

@BlockingClass
object MyBlockingObject {
    fun blockingObjectFunction(): Int {
        return 42
    }
}

fun MyClass.blockingExtensionFunction(): Int {
    return 42
}

class Simple06(
    private val interface1: MyInterface,
    private val cls1: MyClass
) {
    @BlockingClass
    open class MyNestedClass {
        fun blockingNestedClassFunction(): Int {
            return 42
        }
    }

    private fun directReturnInterface(): MyInterface {
        TODO()
    }

    private fun directReturnClass(): MyClass {
        TODO()
    }

    private fun <T> inferredReturn(clazz: Class<T>): T {
        TODO()
    }

    private fun directReturnNestedClass(): MyNestedClass {
        TODO()
    }

    // interface
    private val interface2 = object : MyInterface {
        override fun blockingInterfaceFunction(): Int {
            TODO()
        }
    }

    private val interface3 = directReturnInterface()
    private val interface4 = inferredReturn(MyInterface::class.java)

    // class
    private val cls2 = object : MyClass() {
        override fun dao(): MyDao {
            TODO()
        }
    }
    private val cls3 = directReturnClass()
    private val cls4 = inferredReturn(MyClass::class.java)

    private val nestedCls2 = object : MyNestedClass() {}
    private val nestedCls3 = directReturnNestedClass()
    private val nestedCls4 = inferredReturn(MyNestedClass::class.java)

    suspend fun test() {
/*00*/  interface1.blockingInterfaceFunction()
        // interface2.blockingInterfaceFunction() // this is not supported at the moment
        interface3.blockingInterfaceFunction()
        interface4.blockingInterfaceFunction()

/*05*/  cls1.blockingClassFunction()
        cls2.blockingClassFunction()
        cls3.blockingClassFunction()
        cls4.blockingClassFunction()

/*10*/  nestedCls2.blockingNestedClassFunction()
        nestedCls3.blockingNestedClassFunction()
        nestedCls4.blockingNestedClassFunction()

        MyBlockingObject.blockingObjectFunction()
/*15*/
        // cls1.blockingExtensionFunction()

        cls1.dao().daoCall()
        // cls2.dao().daoCall()
/*20*/  cls3.dao().daoCall()
        cls4.dao().daoCall()
    }
}
