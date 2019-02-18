import ch.hsr.geohash.GeoHash

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
        val pos = it.position
        Node(pos, it.name, true, GeoHash.withBitPrecision(pos.lat, pos.lon, 64))
    }

    val polygons = FileHandler.readPolygonsFile()
    println("polygons: ${polygons.size}")

    val polygonPoints = polygons.map {
        it.extractPoints()
    }.flatten()

    val points = polygons.map {polygon ->
        polygon.polygonPoints.map {position ->
            val pos = position.flip()
            Node(pos.flip(), polygon.name, geohash = GeoHash.withBitPrecision(pos.lat, pos.lon, 64))
        }
    }.flatten()

    val allPoints = points + portPoints

    println("Got ${allPoints.size} points")


    val groupedPoints = points.groupBy {
        it.geohash.getGeoHashWithPrecision(16)
    }.map { (key, list) ->
        val newName = list.map { it.name }.toSet().joinToString(separator = "+")
        Node(list.first().position, newName, list.first().isPort, GeoHash.fromBinaryString(key))
    }

    val pointsJsonString = Utils.toJsonString(allPoints)

//    println(pointsJsonString)

    val graph = GraphUtils.createGraph(polygons, ports, groupedPoints)
    println(graph)


    val start = graph.nodes[4] // Taixing
//    val goal = graph.nodes[12] // Manila
    val goal = graph.nodes[19] // Lake charles

    val path = AStar.startAStar(graph, start, goal)
    println(path)

    println("Start: ${start.name}")
    for (item in path!!) {
        println("From ${item.fromNode}      -       To ${item.toNode}")
    }
    println("End: ${goal.name}")


    val geoJson = GeoJson.pathToGeoJson(path)
    println(geoJson)
    writeJsonToFile(geoJson)

    println("Done")










}