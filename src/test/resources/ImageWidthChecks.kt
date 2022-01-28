
class RandomClass {
    fun getTaskActions() = listOf(
        mapOf(
            "title" to "Some title 1",
            "subtitle" to """
                                1. Subtitle 1,
                                2. Subtitle 2
                            """.trimIndent(),
            "samplePhotos" to listOf(
                "https://ud-img.azureedge.net/w_768,q_75/u/assets/8zze8ghc2rx4pthydu8f.jpg"
            )
        ),
        mapOf(
            "title" to "Some title 2",
            "subtitle" to "Random subtitle",
            "samplePhotos" to listOf(
                "https://ud-img.azureedge.net/w_768,q_75/u/assets/uifkk3iyok3zpenrvtcq.jpeg"
            )
        )

    )
}

enum class RandomEnum(val title: String, val iconUrl: String) {
    ITEM_A("Item A", "https://ud-img.azureedge.net/w_256,q_75/u/assets/px23prq65xdhyx6353h9.png"),
    ITEM_B("Item B", "https://ud-img.azureedge.net/w_768,q_75/u/assets/kx23prq65xdhyx635huy.png"),
    ITEM_C("Item C", "https://ud-img.azureedge.net/w_768,q_75/u/assets/g0b00bt73jh6rxptzjqe.png"),
    ITEM_D("Item D", "https://ud-img.azureedge.net/w_124,q_auto/u/assets/g0b00bt73jh6rxptzjqe.png");
}

const val someIcon1 = "https://ud-img.azureedge.net/w_768,q_75/u/assets/36eux3optz3wnifaikaq.png"
const val someIcon2 = "https://ud-img.azureedge.net/w_768,q_75/u/assets/swwofuabve1qdqg7zoeo.png"
const val someIcon3 = "https://ud-img.azureedge.net/w_360,q_auto/u/assets/swwofuabve1qdqg7zoeo.png"