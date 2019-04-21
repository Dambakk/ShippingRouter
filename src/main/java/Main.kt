import CostFunctions.PolygonCost
import CostFunctions.PolygonGradientCost
import CostFunctions.PortServiceTimeWindowCost
import Models.GraphNode
import Models.GraphPortNode
import Models.Ship
import Utilities.*
import com.fasterxml.jackson.databind.ObjectMapper
import org.wololo.geojson.GeoJSONFactory
import org.wololo.jts2geojson.GeoJSONWriter


fun main() {

//    readShapeFile()
    val test = testGeoJsonCreator()

    val json = ObjectMapper().writeValueAsString(test)

    println(json)

    return

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
    val pointsJsonString = Utils.toJsonString(allPoints)
    println(pointsJsonString)

//    val groupedPoints = points.groupBy { it.geohash.getGeoHashWithPrecision(32) }
//            .map { (key, list) ->
//                val newName = list.map { it.name }.toSet().joinToString(separator = "+")
//                GraphNode(newName, list.first().position, GeoHash.fromBinaryString(key))
//            }
//            .toSet()



    val graph = GraphUtils.createLatLonGraph(portPoints, 100,  worldCountries)
//    val graph = GraphUtils.createKlavenessGraph(polygons, portPoints, groupedPoints, worldCountries)

    Logger.log("Graph created! (Nodes: ${graph.nodes.size}, edges: ${graph.edges.size})")

    val startNode = graph.getPortById(Config.startPortId)
    val goalNode = graph.getPortById(Config.goalPortId)

    val ship = Ship("Test ship 1", 1000, 25, 100).apply {
        addCostFunction(PolygonCost(1.0f, 100, "assets/constraints/suez-polygon.geojson"))
        addCostFunction(PolygonCost(1.0f, 100, "assets/constraints/panama-polygon.geojson"))
        addCostFunction(PolygonGradientCost(1.0f, 1, "assets/constraints/antarctica.geojson"))
        addTimeWindow(PortServiceTimeWindowCost(1.0f, graph.getPortById("ARRGA"), 700_000..800_000L))
//            .addCostFunction(PolygonCost(1.0f, 2100000000, "assets/constraints/test.geojson"))
    }

//    val possibleLoadingPortsWithPortPrice = mapOf("ARRGA" to 100, "QAMES" to 2000, "JPETA" to 500, "USCRP" to 10000)
    val possibleLoadingPortsWithPortPrice = mapOf("ARRGA" to 100)
//    val possibleLoadingPortsWithPortPrice = mapOf("QAMES" to 100)
//    val possibleLoadingPortsWithPortPrice = mapOf("JPETA" to 100)
//    val possibleLoadingPortsWithPortPrice = mapOf("USCRP" to 100)

    val intermediateTime1 = System.currentTimeMillis()

    val loadingPort = graph.getPortById("ARRGA")
    val (pathBruteForce, costBruteForce) = ExhaustiveSearch.performExhaustiveSearch(graph, startNode, goalNode, loadingPort, 100, ship, 1000)
    val intermediateTime2 = System.currentTimeMillis()
    val (path, cost) = AStar.startAStar(graph, startNode, goalNode, possibleLoadingPortsWithPortPrice, ship, 1000)
    println("Path: $path")

    val endTime = System.currentTimeMillis()


//    path.zip(pathBruteForce).forEach {
//        if (it.first != it.second) {
//            Logger.log("Paths are not equal", LogType.WARNING)
//        }
//        assert(it.first == it.second)
//    }


    val geoJsonAStar = Utilities.GeoJson.pathToGeoJson(path, color = "#009933", label = "$cost")
    val geoJsonExhaustice = Utilities.GeoJson.pathToGeoJson(pathBruteForce, color = "#42f474", label = "$costBruteForce")
    println("-----------------------------------")
    println(" ")
    println("A* solution:")
    println(geoJsonAStar)
    println("A* cost: $cost")
    println(" ")
    println("-----------------------------------")
    println(" ")
    println("Brute force solution:")
    println(geoJsonExhaustice)
    println("Brute force cost: $costBruteForce")
    println("-----------------------------------")
//    writeJsonToFile(geoJson)

    Logger.log("Preparation time: \t${(intermediateTime1 - startTime) / 1000.0} seconds")
    Logger.log("Exhaustice search:\t${(intermediateTime2 - intermediateTime1) / 1000.0} seconds, \t|\tCost: $costBruteForce")
    Logger.log("A-star duration: \t${(endTime - intermediateTime2) / 1000.0} seconds, \t|\tCost: $cost")
    Logger.log("Total duration: \t\t${(endTime - startTime) / 1000.0} seconds")

    Logger.log("Done")

    if (Config.isMacOS) {
        try {
            val notificationCommand = listOf(
                    "/usr/local/Cellar/terminal-notifier/2.0.0/terminal-notifier.app/Contents/MacOS/terminal-notifier",
                    "-title 'Shipping Router project'",
                    "-message 'Your run has finished!'",
                    "-sound 'default'"
            )

            Runtime.getRuntime().exec(notificationCommand.toTypedArray())
        } catch (e: Exception) {
            Logger.log("When running on MacOS you can install terminal-notifier to get a system notification when " +
                    "the pathfinding is complete.")
            Logger.log("Install by running: 'brew install terminal-notifier'.")
        }

    }


}
