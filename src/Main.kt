

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

    val ports = FileHandler.readPortsFile()
            .filter { !it.deleted }
            .filter { it.portId in Config.portIdsOfInterest }
    println("Ports: ${ports.size}")


//    val graph = DefaultUndirectedGraph<Port, DefaultEdge>(DefaultEdge::class.java)
//    for (p in ports) {
//        graph.addVertex(p)
//    }

    val portPoints = ports.map {
        Node(it.position, it.name, true)
    }

    val polygons = FileHandler.readPolygonsFile()
    println("polygons: ${polygons.size}")

    val polygonPoints = polygons.map {
        it.extractPoints()
    }.flatten()

    val points = polygons.map {polygon ->
        polygon.polygonPoints.map {position ->
            Node(position, polygon.name)
        }
    }.flatten()

    val allPoints = points + portPoints

    println("Got ${allPoints.size} points")

    val pointsJsonString = Utils.toJsonString(allPoints)

//    println(pointsJsonString)

    GraphUtils.createGraph(polygons)





}