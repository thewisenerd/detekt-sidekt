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
}
