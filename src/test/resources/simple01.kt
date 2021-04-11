import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.future.future
import kotlinx.coroutines.withContext
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class BlockingCall

class Test01 {
    @BlockingCall
    fun getSync(str: String): Int {
        TODO()
    }

    suspend fun getAwait(str: String): Int {
        TODO()
    }
}

class ObserverScope01 {
    companion object {
        fun <T> futureX(
            context: CoroutineContext = EmptyCoroutineContext,
            block: suspend CoroutineScope.() -> T
        ): CompletableFuture<T> {
            return GlobalScope.future() {
                block()
            }
        }
    }
}

class Simple01(val a: Test01) {
    suspend fun foo() {
        val res = a.getSync("ctx-root")

        val res2 = withContext(Dispatchers.IO) {
            a.getSync("ctx-lambda-withCtx")
        }

        val res3 = withContext(Dispatchers.IO) {
            a.getAwait("ctx-lambda-withCtx-await")
        }
    }

    val regularLambda = { recv1: Int ->
        a.getSync("ctx-lambda-regular")
    }

    fun blockReceiverOne(block: suspend (Int) -> Unit) {
        TODO()
    }

    fun blockReceiverTwo(block: suspend (Int, String) -> Unit) {
        TODO()
    }

    fun invokeBlockReceiver() {
        blockReceiverOne { recv1: Int ->
            a.getSync("ctx-lambda-suspend-one")
        }

        blockReceiverTwo { recv1: Int, recv2: String ->
            a.getSync("ctx-lambda-suspend-two")
        }
    }

    fun callingFuture() {
        ObserverScope01.futureX {
            a.getSync("ctx-lambda-future")
        }
    }
}