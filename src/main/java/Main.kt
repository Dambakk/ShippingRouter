import CostFunctions.PolygonCost
import Models.GraphNode
import Models.GraphPortNode
import Models.Ship
import Utilities.*
import ch.hsr.geohash.GeoHash


fun main() {

//    readShapeFile()

    val startTime = System.currentTimeMillis()

    val worldCountries = Utilities.GeoJson.readWorldCountriesGeoJSON(Config.worldCountriesGeoJsonFile)
//    val test = FileInputStream("world-borders.shp").channel

//    val f = File("world-borders.shp")
//    val dataStore = ShapefileDataStore(f.toURI().toURL())
//    val featureSource = dataStore.featureSource
//    print(featureSource.features.size())
//    val geomAttrName = featureSource.getSchema()
//            .getGeometryDescriptor().getLocalName()


    /**
     * Trades:
     */
//    val trades = Utilities.FileHandler.readTradePatternsFile()
//    println("Received ${trades.size} trades")
//
//    val timeAtSea = mutableMapOf<Pair<String, String>, MutableList<Int>>()
//    trades.forEach {
//        if (timeAtSea[it.getFromAndToAlphabetical()] == null) {
//            timeAtSea[it.getFromAndToAlphabetical()] = mutableListOf(it.atSea)
//        } else {
//            timeAtSea[it.getFromAndToAlphabetical()]!!.add(it.atSea)
//        }
//    }
//
//    val timeAtSeaAverage = timeAtSea.map {
//        it.key to it.value.average()
//    }
//
//    println("Filtered, combined destinations, and calculated average travel time. Got ${timeAtSeaAverage.size} elements")

    val portPoints = Utilities.FileHandler.readPortsFile()
            .filter { !it.deleted }
            .filter { it.portId in Config.portIdsOfInterest }
            .map { GraphPortNode(it.name, it.position, null, it.portId) }
            .also { println("Number of ports: ${it.size}") }

    portPoints.forEach {
        assert(it.position.lat in (-90.0..90.0) && it.position.lon in (-180.0..180.0)) { "Port position invalid: ${it.position}, ${it.portId}" }
    }


    val polygons = Utilities.FileHandler.readPolygonsFile()
    println("Number of polygons: ${polygons.size}")

    val points = polygons
            .asSequence()
            .map { polygon ->
                polygon.polygonPoints.map { position ->
                    val pos = position
                    GraphNode(polygon.name, pos)
                }
            }
            .flatten()
            .toList()

    points.forEach {
        assert(it.position.lat in (-90.0..90.0) && it.position.lon in (-180.0..180.0)) { "Node position invalid: ${it.position}" }
    }

    val allPoints = points + portPoints

    println("Total number of nodes: ${allPoints.size}")


//    val groupedPoints = points.groupBy { it.geohash.getGeoHashWithPrecision(32) }
//            .map { (key, list) ->
//                val newName = list.map { it.name }.toSet().joinToString(separator = "+")
//                GraphNode(newName, list.first().position, GeoHash.fromBinaryString(key))
//            }
//            .toSet()

    val pointsJsonString = Utils.toJsonString(allPoints)
    println(pointsJsonString)

    val graph = GraphUtils.createLatLonGraph(portPoints, 100,  worldCountries)
//    val graph = GraphUtils.createKlavenessGraph(polygons, portPoints, groupedPoints, worldCountries)

    Logger.log("Graph created! (Nodes: ${graph.nodes.size}, edges: ${graph.edges.size})")

    val start = graph.getPortById(Config.startPortId)
    val goal = graph.getPortById(Config.goalPortId)


    val ship = Ship("Test ship 1", 1000, 25, 100).apply {
        addCostFunction(PolygonCost(1.0f, Integer.MAX_VALUE, "assets/constraints/suez-polygon.geojson"))
//        addCostFunction(PolygonCost(1.0f, 100, "assets/constraints/panama-polygon.geojson"))
    }
//            .addCostFunction(PolygonCost(1.0f, 2100000000, "assets/constraints/test.geojson"))

//    val possibleLoadingPortsWithPortPrice = mapOf("ARRGA" to 100, "QAMES" to 2000, "JPETA" to 500, "USCRP" to 10000)
//    val possibleLoadingPortsWithPortPrice = mapOf("QAMES" to 100)
    val possibleLoadingPortsWithPortPrice = mapOf("JPETA" to 100)

    val intermediateTime = System.currentTimeMillis()

    val (path, cost) = AStar.startAStar(graph, start, goal, possibleLoadingPortsWithPortPrice, ship, 1000)
    println("Path: $path")

    val endTime = System.currentTimeMillis()

    println("Start: ${start.name}")
    for (item in path!!) {
        println("From ${item.fromNode}      -       To ${item.toNode}")
    }
    println("End: ${goal.name}")


    val geoJson = Utilities.GeoJson.pathToGeoJson(path, color = "#009933", label = "$cost")
    println(geoJson)
//    writeJsonToFile(geoJson)

    Logger.log("Preparation time: \t${(intermediateTime - startTime) / 1000.0} seconds")
    Logger.log("A-star duration: \t${(endTime - intermediateTime) / 1000.0} seconds")
    Logger.log("Total duration: \t\t${(endTime - startTime) / 1000.0} seconds")

    Logger.log("Done")

}
