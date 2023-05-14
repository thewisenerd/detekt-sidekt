class SomeRandomDao {
    fun daoCall(): Int {
        return 42
    }
}

class Simple07(
    private val dao: SomeRandomDao
) {
    suspend fun test() {
        dao.daoCall()
    }
}
