class Simple02 {
    @Throws(InterruptedException::class)
    fun blockingFunction() {
        // do nothing
    }

    @Throws(InterruptedException::class)
    fun <T> blockingLambda(id: Int, block: () -> T): T {
        return block()
    }

    suspend fun testThrows() {
        val result2 = blockingFunction()

        val result3 = blockingLambda(3) {
            "hello"
        }
    }
}