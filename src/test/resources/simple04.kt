@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class RegularBlockingCall

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class ReclaimableBlockingCall

class Test04 {
    @RegularBlockingCall
    fun get(str: String): Int {
        TODO()
    }

    @ReclaimableBlockingCall
    fun getSync(str: String): Int {
        TODO()
    }

    suspend fun getAwait(str: String): Int {
        TODO()
    }
}

class CoroutineDispatcherShim
object CustomDispatchers {
    val DB: CoroutineDispatcherShim = TODO()
}

suspend fun <T> withContextShim(dispatcher: CoroutineDispatcherShim, block: suspend () -> T): T {
    return block()
}

class Simple04(val a: Test04) {
    suspend fun foo1() = withContextShim(CustomDispatchers.DB) {
        val res1 = a.get("regular-01") // should
        val res2 = a.getSync("reclaimable-01")
    }

    suspend fun foo2() {
        withContextShim(CustomDispatchers.DB) {
            val res3 = a.get("nested-regular-01")
            val res4 = a.getSync("nested-reclaimable-01")
        }
    }
}