import CostFunctions.*
import Models.*
import Utilities.*
import com.fasterxml.jackson.databind.ObjectMapper
import com.marcinmoskala.math.combinations
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.math.BigInteger

fun main() {

    val test = testGeoJsonCreator()
    val json = ObjectMapper().writeValueAsString(test)
    println(json)

    val startTime = System.currentTimeMillis()

    val worldCountries = GeoJson.readWorldCountriesGeoJSON(Config.worldCountriesGeoJsonFile)

    val portPoints = FileHandler.readPortsFile()
            .filter { !it.deleted }
            .filter { it.portId in Config.portIdsOfInterestFull }
            .map { GraphPortNode(it.name, it.position, null, it.portId) }
            .also { ports -> ports.forEach {
                assert( it.position.lat in (-90.0..90.0) && it.position.lon in (-180.0..180.0)) { "Port position invalid: ${it.position}, ${it.portId}"} }
            }
            .also { println("Number of ports: ${it.size}") }

    val subSetOfPorts = portPoints
            .filter { it.portId in Config.portIdsOfInterestMini }
            .also { println("Number of ports (mini): ${it.size}") }

//region old
//    val polygons = FileHandler.readPolygonsFile()
//    println("Number of polygons: ${polygons.size}")
//
//    val points = polygons
//            .asSequence()
//            .map { polygon ->
//                polygon.polygonPoints.map { position ->
//                    val pos = position
//                    GraphNode(polygon.name, pos)
//                }
//            }
//            .flatten()
//            .toList()
//
//    points.forEach {
//        assert(it.position.lat in (-90.0..90.0) && it.position.lon in (-180.0..180.0)) { "Node position invalid: ${it.position}" }
//    }
//
//    val allPoints = points + portPoints
//    println("Total number of nodes: ${allPoints.size}")
//    val pointsJsonString = Utils.toJsonString(allPoints)
//    println(pointsJsonString)
//
    //endregion

    val createNewGraph = false
    val graphFile = "graph-0.5.graph"

    val graph = if (createNewGraph) {
        val g = GraphUtils.createLatLonGraph(portPoints, 100,  worldCountries)
//    GraphUtils.createKlavenessGraph(polygons, portPoints, groupedPoints, worldCountries)
        ObjectOutputStream(FileOutputStream(graphFile)).use {
            it.writeObject(g)
            Logger.log("Graph written to file")
        }
        g
    } else {
        var g: Graph? = null
        ObjectInputStream(FileInputStream(graphFile)).use {
            g = it.readObject() as Graph
            Logger.log("Graph read from file")
        }
            g!!
    }


    Logger.log("Graph created! (Nodes: ${graph.nodes.size}, edges: ${graph.edges.size})")

    val startNode = graph.getPortById(Config.startPortId)
    val loadingNode = graph.getPortById(Config.loadingPortId)
    val goalNode = graph.getPortById(Config.goalPortId)
//    val possibleLoadingPortsWithPortPrice = mapOf("ARRGA" to 1000, "QAMES" to 100, "USCRP" to 2000)
//    val possibleLoadingPortsWithPortPrice = mapOf("ARRGA" to 100)
//    val possibleLoadingPortsWithPortPrice = mapOf("QAMES" to 100)
//    val possibleLoadingPortsWithPortPrice = mapOf("CNTAX" to 100)
//    val possibleLoadingPortsWithPortPrice = mapOf("USCRP" to 100)

    val ship = Ship("Test ship 1", 1000, 25, 250).apply {
//        addCostFunction(PolygonCost(1.0f, 10_000, "assets/constraints/suez-polygon.geojson"))
//        addCostFunction(PolygonCost(1.0f, 10_000, "assets/constraints/panama-polygon.geojson"))
//        addCostFunction(PolygonGradientCost(1.0f, 1, "assets/constraints/antarctica.geojson"))
        addCostFunction(PolygonAngledCost(1f, "assets/constraints/taiwan-strait.geojson",100000, 225.0, 90.0))
//        addTimeWindowConstraint(PortServiceTimeWindowHard(1.0f, graph.getPortById("ARRGA"), 0L..10_000_000L))

//        addCostFunction(PolygonCost(1f, 1000,"assets/constraints/taiwan-strait.geojson"))
//        addTimeWindow(PortServiceTimeWindowCost(1.0f, graph.getPortById("CNTAX"), 200_000L..220_000L))
//            .addCostFunction(PolygonCost(1.0f, 2100000000, "assets/constraints/test.geojson"))
    }



    val intermediateTime1 = System.currentTimeMillis()

    val resultMap = mutableMapOf<Triple<GraphPortNode, GraphPortNode, GraphPortNode>, Pair<Pair<List<GraphEdge>, BigInteger>, Pair<List<GraphEdge>, BigInteger>>>()

    subSetOfPorts.toSet().combinations(3).forEach { ports ->
        val p = ports.toList()
        val startPort = p[0]
        val loadingPort = p[1]
        val goalPort = p[2]
//        println("${startPort.name} -> ${loadingPort.name} -> ${goalPort.name}")

//        val (pathBruteForce, costBruteForce) = ExhaustiveSearch.performExhaustiveSearch(graph, startPort, goalPort, loadingPort, 100, ship, 1000)
//        val (path, cost) = AStar.startAStar(graph, startPort, goalPort, mapOf(loadingPort.portId to 100), ship, 1000, subSetOfPorts)
//        resultMap[Triple(startPort, loadingPort, goalPort)] = Pair(Pair(pathBruteForce, costBruteForce), Pair(path, cost))

    }

//    println("Number of port combinations: ${subSetOfPorts.toSet().combinations(3).size}")

//    val (pathBruteForce, costBruteForce) = ExhaustiveSearch.performExhaustiveSearch(graph, startNode, goalNode, loadingNode, 100, ship, 1000)
    val intermediateTime2 = System.currentTimeMillis()
//    val (path, cost) = AStar.startAStar(graph, startNode, goalNode, possibleLoadingPortsWithPortPrice, ship, 1000, subSetOfPorts)
    val (path, cost) = AStar.startAStar(graph, startNode, goalNode, mapOf(loadingNode.portId to 0), ship, 1000, subSetOfPorts)
//    val (path, cost) = AStar.startAStar(graph, startNode, goalNode, possibleLoadingPortsWithPortPrice, ship, 1000, subSetOfPorts)
    println("Path: $path")

    val endTime = System.currentTimeMillis()


//    path.zip(pathBruteForce).forEach {
//        if (it.first != it.second) {
//            Logger.log("Paths are not equal", LogType.WARNING)
//        }
//        assert(it.first == it.second)
//    }


    val geoJsonAStar = GeoJson.pathToGeoJson(path, color = "#009933", label = "$cost")
//    val geoJsonExhaustice = GeoJson.pathToGeoJson(pathBruteForce, color = "#42f474", label = "$costBruteForce")
    println("-----------------------------------")
    println(" ")
    println("A* solution:")
    println(geoJsonAStar)
    println("A* cost: $cost")
    println(" ")
    println("-----------------------------------")
    println(" ")
    println("Brute force solution:")
//    println(geoJsonExhaustice)
//    println("Brute force cost: $costBruteForce")
    println("-----------------------------------")
//    writeJsonToFile(geoJson)

    Logger.log("Preparation time: \t${(intermediateTime1 - startTime) / 1000.0} seconds")
//    Logger.log("Exhaustice search:\t${(intermediateTime2 - intermediateTime1) / 1000.0} seconds, \t|\tCost: $costBruteForce")
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
