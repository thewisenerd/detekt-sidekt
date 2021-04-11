import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class Simple03 {
    private suspend fun foo() {
        val result1 = CompletableFuture.supplyAsync {
            42
        }.get()

        val result2 = CompletableFuture.supplyAsync {
            42
        }.get(500, TimeUnit.MILLISECONDS)
    }

    private suspend fun foo2(test: Test03) {
        val result3 = test.foo()
    }

    private suspend fun foo3() {
        Thread.sleep(1000)
    }
}

class Test03 {
    fun foo(): Int = 42
}