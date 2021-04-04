import kotlinx.coroutines.*
import kotlinx.coroutines.future.future

import java.util.concurrent.CompletableFuture
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class BlockingCall

class Test {
    @BlockingCall
    fun getSync(str: String): Int {
        TODO()
    }

    suspend fun getAwait(str: String): Int {
        TODO()
    }
}

val a = Test()
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

class ObserverScope {
    companion object {
        fun <T> futureX(
            context: CoroutineContext = EmptyCoroutineContext,
            block: suspend CoroutineScope.() -> T
        ) : CompletableFuture<T> {
            return GlobalScope.future() {
                block()
            }
        }
    }
}

fun callingFuture() {
    ObserverScope.futureX {
        a.getSync("ctx-lambda-future")
    }
}