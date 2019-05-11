import CostFunctions.*
import Models.*
import Utilities.*
import com.fasterxml.jackson.databind.ObjectMapper
import com.marcinmoskala.math.combinations
import com.marcinmoskala.math.permutations
import scala.reflect.io.Directory
import java.io.*
import java.math.BigInteger
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

typealias Path = List<GraphEdge>
typealias Cost = BigInteger


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
            .also { ports ->
                ports.forEach {
                    assert(it.position.lat in (-90.0..90.0) && it.position.lon in (-180.0..180.0)) { "Port position invalid: ${it.position}, ${it.portId}" }
                }
            }
            .also { println("Number of ports: ${it.size}") }

    val subSetOfPortsMini = portPoints
            .filter { it.portId in Config.portIdsOfInterestMicro }
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
        val g = GraphUtils.createLatLonGraph(portPoints, 100, worldCountries)
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
//        addCostFunction(PolygonAngledCost(1f, "assets/constraints/taiwan-strait.geojson", 100000, 225.0, 90.0))
//        addTimeWindowConstraint(PortServiceTimeWindowHard(1.0f, graph.getPortById("ARRGA"), 0L..100_000L))
//        addTimeWindowConstraint(PortServiceTimeWindowHard(1.0f, graph.getPortById("CNTAX"), 0L..90_000L))
//        addTimeWindowConstraint(PortServiceTimeWindowHard(1.0f, graph.getPortById("JPETA"), 1_000L..650_000L))

//        addCostFunction(PolygonCost(1f, 1000,"assets/constraints/taiwan-strait.geojson"))
//        addTimeWindow(PortServiceTimeWindowCost(1.0f, graph.getPortById("CNTAX"), 200_000L..220_000L))
//            .addCostFunction(PolygonCost(1.0f, 2100000000, "assets/constraints/test.geojson"))
    }


    val portPrices = mapOf<String, Int>(
            "ARRGA" to 1000,
            "AUBUY" to 5000,
            "BMFPT" to 100,
            "CNTAX" to 400,
            "CNTNJ" to 2000,
            "CNTXG" to 1200,
            "CNXGA" to 6000,
            "CNZJG" to 300,
            "JPETA" to 1500,
            "JPKSM" to 900,
            "JPSAK" to 500,
            "KRYOS" to 8000,
            "PHMNL" to 300,
            "QAMES" to 600,
            "SAJUB" to 4100,
            "TWMLI" to 3750,
            "USCRP" to 50,
            "USFPO" to 1900,
            "USHOU" to 2300,
            "USLCH" to 2800,
            "USPCR" to 9900,
            "USPLQ" to 7000,
            "USWWO" to 6500
    )


//    val intermediateTime1 = System.currentTimeMillis()


    val resultRecords = mutableListOf<SimulationRecord>()
    val numCombinations = subSetOfPortsMini.toSet().combinations(3).size
    var counter = 0
    subSetOfPortsMini.toSet().combinations(3).forEach { ports ->
        val p = ports.toList()
        val startPort = p[0]
        val loadingPort = p[1]
        val goalPort = p[2]
        val numTonnes = 1000

        Logger.log("Evaluating combination #${++counter}/$numCombinations...")
        Logger.log("${startPort.portId} -> ${loadingPort.portId} -> ${goalPort.portId}", LogType.DEBUG)

        val loadingPortPrice = portPrices[loadingPort.portId] ?: 1000

        val runConfig = RunConfiguration(
                graph,
                startPort,
                loadingPort,
                goalPort,
                loadingPortPrice,
                ship,
                numTonnes
        )

//        val configTest = RunConfiguration(
//            graph = graph,
//            startNode = graph.getPortById("CNXGA"),
//            loadingPort = graph.getPortById("CNTAX"),
//            goalNode = graph.getPortById("CNXGA"),
//            portPricePrTon = 0,
//            ship = ship,
//            numTonnes = 0
//    )

        val startTime = System.currentTimeMillis()

        val (exhaustivePath, exhaustiveCost) = ExhaustiveSearch.performExhaustiveSearch(runConfig)
                ?: Pair(emptyList(), (-1L).toBigInteger())
//        val (exhaustivePath, exhaustiveCost) = ExhaustiveSearch.performExhaustiveSearch(configTest) ?: Pair(emptyList(), (-1L).toBigInteger())

        val middleTime = System.currentTimeMillis()

        val (aStarPath, aStarCost) = AStar.startAStar(runConfig) ?: Pair(emptyList(), (-1L).toBigInteger())
//        val (aStarPath, aStarCost) = AStar.startAStar(configTest) ?: Pair(emptyList(), (-1L).toBigInteger())
//        val (aStarPath, aStarCost) = Pair(emptyList<GraphEdge>(), (-1L).toBigInteger())

        val endTime = System.currentTimeMillis()


        //Calculate meta:
        val exhaustiveDuration = (middleTime - startTime) / 1000 // seconds
        val AStarDuration = (endTime - middleTime) / 1000 // seconds
        val infoMsg = if (exhaustivePath.isEmpty() && aStarPath.isEmpty()) {
            "Both Exhaustive search and A* failed"
        } else if (exhaustivePath.isEmpty()) {
            "Exhaustive result failed"
        } else if (aStarPath.isEmpty()) {
            "A* result failed"
        } else {
            "OK"
        }


        resultRecords.add(SimulationRecord(
                startPort,
                loadingPort,
                goalPort,
                aStarPath,
                aStarCost,
                AStarDuration,
                exhaustivePath,
                exhaustiveCost,
                exhaustiveDuration,
                infoMsg
        ))
    }

    Logger.log("RESULTS", LogType.SUCCESS)
    resultRecords.forEach {
        println(it)
    }
    println("----------------------------")
    Logger.log("Number of results elements: \t${resultRecords.size}", LogType.INFO)
    Logger.log("Number of non-ok elements: \t${resultRecords.count { it.infoMsg != "OK" }}")

    val current = LocalDateTime.now()

    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd--HH:mm")
    val dateString = current.format(formatter)

    val dir = File("output/$dateString/")
    if (!dir.exists()) {
        dir.mkdirs()
    }

    resultRecords.forEach {
        if(it.aStarPath.isNotEmpty()) {
            val aStarGeoJson = GeoJson.newEdgesToGeoJson(it.aStarPath)
            File("output/$dateString/${it.getPortsAsString()}-aStar.json").writeText(aStarGeoJson)
        }
        if (it.exhaustivePath.isNotEmpty()) {
            val exhaustiveGeoJson = GeoJson.newEdgesToGeoJson(it.exhaustivePath)
            File("output/$dateString/${it.getPortsAsString()}-exhaustive.json").writeText(exhaustiveGeoJson)
        }
    }

    val csvString = SimulationRecord.getCommaSeparatedHeaders() + "\n" +
            resultRecords.joinToString(separator = "\n") { it.toCommaSeparatedString() }

    File("output/$dateString/simulationResult.csv").writeText(csvString)

//    println("Number of port combinations: ${subSetOfPorts.toSet().combinations(3).size}")


    val config = RunConfiguration(
            graph = graph,
            startNode = graph.getPortById("CNXGA"),
            loadingPort = graph.getPortById("CNTAX"),
            goalNode = graph.getPortById("CNXGA"),
            portPricePrTon = 0,
            ship = ship,
            numTonnes = 0
    )


//    val (pathBruteForce, costBruteForce) = ExhaustiveSearch.performExhaustiveSearch(config) ?: Pair(emptyList<GraphEdge>(), 0.toBigInteger())
//    val (aStarPath1, aStarCost1) = AStar.startAStar(config) ?: Pair(emptyList<GraphEdge>(), 0.toBigInteger())

//    val (aStarPath, aStarCost) = AStar.startAStar(graph, startNode, goalNode, possibleLoadingPortsWithPortPrice, ship, 1000, subSetOfPorts)
//    val (path, cost) = AStar.startAStar(graph, startNode, goalNode, mapOf(loadingNode.portId to 0), ship, 1000, subSetOfPortsMini)
//    val (aStarPath, aStarCost) = AStar.startAStar(graph, startNode, goalNode, possibleLoadingPortsWithPortPrice, ship, 1000, subSetOfPorts)
//    println("Path: $aStarPath1")

//    Logger.log("A* Result: Path length: ${aStarPath1.size}, Cost: $aStarCost1", LogType.DEBUG)
//    Logger.log("Exhaustive Result: Path length: ${pathBruteForce.size}, Cost: $costBruteForce", LogType.DEBUG)
//    val endTime = System.currentTimeMillis()


//    val geoJsonAStar = GeoJson.pathToGeoJson(path, color = "#009933", label = "$cost")
//    val geoJsonExhaustice = GeoJson.pathToGeoJson(pathBruteForce, color = "#42f474", label = "$costBruteForce")
//    println("-----------------------------------")
//    println(" ")
//    println("A* solution:")
//    println(geoJsonAStar)
//    println("A* aStarCost: $cost")
//    println(" ")
//    println("-----------------------------------")
//    println(" ")
//    println("Brute force solution:")
//    println(geoJsonExhaustice)
//    println("Brute force aStarCost: $costBruteForce")
//    println("-----------------------------------")
//    writeJsonToFile(geoJson)

//    Logger.log("Preparation time: \t${(intermediateTime1 - startTime) / 1000.0} seconds")
//    Logger.log("Exhaustice search:\t${(intermediateTime2 - intermediateTime1) / 1000.0} seconds, \t|\tCost: $costBruteForce")
//    Logger.log("A-star duration: \t${(endTime - intermediateTime2) / 1000.0} seconds, \t|\tCost: $cost")
//    Logger.log("Total duration: \t\t${(endTime - startTime) / 1000.0} seconds")

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
                    "the pathfinding is complete.", LogType.ERROR)
            Logger.log("Install by running: 'brew install terminal-notifier'.", LogType.INFO)
        }

    }
}


data class SimulationRecord(
        val startPort: GraphPortNode,
        val loadingPort: GraphPortNode,
        val goalPort: GraphPortNode,
        val aStarPath: Path,
        val aStarCost: BigInteger,
        val aStarDuration: Long,
        val exhaustivePath: Path,
        val exhaustiveCost: BigInteger,
        val exhaustiveDuration: Long,
        val infoMsg: String = ""
) {
    override fun toString() =
            """
                ----------------------------
                ${startPort.portId} -> ${loadingPort.portId} -> ${goalPort.portId}
                Info: ${if (infoMsg.isEmpty()) "No Info" else infoMsg}
                ---
                Length of aStarPath: ${aStarPath.size}
                Cost: $aStarCost
                Duration: $aStarDuration
                ---
                Length of Exhaustive path: ${exhaustivePath.size}
                Exhaustive Cost: $exhaustiveCost
                Exhaustive Duration: $exhaustiveDuration
            """.trimIndent()

    fun toCommaSeparatedString() =
            "${startPort.portId},${loadingPort.portId},${goalPort.portId},$aStarCost,$aStarDuration,${aStarPath.size},${startPort.portId}-${loadingPort.portId}-${goalPort.portId}-aStar.json,$exhaustiveCost,$exhaustiveDuration,${exhaustivePath.size},${startPort.portId}-${loadingPort.portId}-${goalPort.portId}-exhaustive.json,$infoMsg" as CharSequence

    fun getPortsAsString() =
            "${startPort.portId}-${loadingPort.portId}-${goalPort.portId}"

    companion object {
        fun getCommaSeparatedHeaders() =
                "startPortId,loadingPortId,goalPortId,aStarCost,aStarDuration,aStarPathSize,aStarGeojsonPath,exhaustiveCost,exhaustiveDuration,exhaustivePathSize,exhaustiveGeojsonPath,infoMsg"

    }
}

data class RunConfiguration(val graph: Graph,
                            val startNode: GraphNode,
                            val loadingPort: GraphNode,
                            val goalNode: GraphNode,
                            val portPricePrTon: Int,
                            val ship: Ship,
                            val numTonnes: Int)
