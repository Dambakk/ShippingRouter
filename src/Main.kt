


fun main(args: Array<String>) {
    val trades = FileHandler.readTradePatternsFile()
    println("Received ${trades.size} trades")

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

    println("Filtered, combined destinations, and calculated average travel time. Got ${timeAtSeaAverage.size} elements")


    val ports = FileHandler.readPortsFile().filter {
        !it.deleted
    }
    println("Ports: ${ports.size}")
}