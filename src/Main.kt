


fun main(args: Array<String>) {
    val trades = FileHandler.readTradePatternsFile()
    print("Received ${trades.size} trades")

    val test = trades.map { it.getFromAndToAlphabetical() to it.atSea}

    val timeAtSea = mutableMapOf<Pair<String, String>, MutableList<Int>>()
    trades.forEach {
        if (timeAtSea[it.getFromAndToAlphabetical()] == null) {
            timeAtSea[it.getFromAndToAlphabetical()] = mutableListOf(it.atSea)
        } else {
            timeAtSea[it.getFromAndToAlphabetical()]!!.add(it.atSea)
        }
    }

    val timeAtSeaAverage = timeAtSea.map {
        it.key to it.value.average()
    }


}