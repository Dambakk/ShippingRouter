import CostFunctions.PolygonAngledCost
import CostFunctions.PolygonCost
import CostFunctions.PolygonGradientCost
import CostFunctions.PortServiceTimeWindowHard
import Models.*
import Utilities.*
import com.marcinmoskala.math.combinations
import com.marcinmoskala.math.permutations
import kotlinx.coroutines.*
import me.tongfei.progressbar.ProgressBar
import java.io.*
import java.math.BigInteger
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

typealias Path = List<GraphEdge>
typealias Cost = BigInteger


fun main() = runBlocking {

    val globalStartTime = System.currentTimeMillis()


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

    val subSetOfPorts = portPoints
            .filter { it.portId in Config.portIdsOfInterest }
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


    val graph = getNewGraph(portPoints)
    Logger.log("Graph loaded/created! (Nodes: ${graph.nodes.size}, edges: ${graph.edges.size})")


    val alreadyTestedCombinations = readResultsFile("output/big-run-01/simulationResult.csv")


//    val startNode = graph.getPortById(Config.startPortId)
//    val loadingNode = graph.getPortById(Config.loadingPortId)
//    val goalNode = graph.getPortById(Config.goalPortId)
//    val possibleLoadingPortsWithPortPrice = mapOf("ARRGA" to 1000, "QAMES" to 100, "USCRP" to 2000)
//    val possibleLoadingPortsWithPortPrice = mapOf("ARRGA" to 100)
//    val possibleLoadingPortsWithPortPrice = mapOf("QAMES" to 100)
//    val possibleLoadingPortsWithPortPrice = mapOf("CNTAX" to 100)
//    val possibleLoadingPortsWithPortPrice = mapOf("USCRP" to 100)

    val ship = Ship("Test ship 1", 1000, 25, 250).apply {
        addCostFunction(PolygonCost(1.0f, 10_000, "assets/constraints/suez-polygon.geojson"))
        addCostFunction(PolygonCost(1.0f, 10_000, "assets/constraints/panama-polygon.geojson"))
        addCostFunction(PolygonGradientCost(1.0f, 1, "assets/constraints/antarctica.geojson"))
        addCostFunction(PolygonAngledCost(1f, "assets/constraints/taiwan-strait.geojson", 100000, 225.0, 90.0))
        addCostFunction(PolygonAngledCost(1f, "assets/constraints/gulf-stream.geojson", 100, 45.0, 90.0)) //TODO: Verify this one
        addTimeWindowConstraint(PortServiceTimeWindowHard(1.0f, graph.getPortById("AUBUY"), 0L..100_000L))
        addTimeWindowConstraint(PortServiceTimeWindowHard(1.0f, graph.getPortById("CNXGA"), 0L..10_000L))
        addTimeWindowConstraint(PortServiceTimeWindowHard(1.0f, graph.getPortById("PHMNL"), 0L..5_000L))
        addTimeWindowConstraint(PortServiceTimeWindowHard(1.0f, graph.getPortById("ARRGA"), 0L..10_000L))
        addTimeWindowConstraint(PortServiceTimeWindowHard(1.0f, graph.getPortById("USCRP"), 0L..7000L))
        addTimeWindowConstraint(PortServiceTimeWindowHard(1.0f, graph.getPortById("QAMES"), 0L..4_500))
        addTimeWindowConstraint(PortServiceTimeWindowHard(1.0f, graph.getPortById("JPETA"), 0L..5_000L))
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


    /*
    val configTest = RunConfiguration(
            graph = graph,
            startNode = graph.getPortById("PHMNL"),
            loadingPort = graph.getPortById("CNXGA"),
            goalNode = graph.getPortById("AUBUY"),
            portPricePrTon = 0,
            ship = ship,
            numTonnes = 0
    )

    val startTime = System.currentTimeMillis()

//            val (exhaustivePath, exhaustiveCost) = ExhaustiveSearch.performExhaustiveSearch(runConfig)
//                    ?: Pair(emptyList(), (-1L).toBigInteger())
    val (exhaustivePath, exhaustiveCost) = ExhaustiveSearch().performExhaustiveSearch(configTest)
            ?: Pair(emptyList(), (-1L).toBigInteger())

    val middleTime = System.currentTimeMillis()

//            val (aStarPath, aStarCost) = AStar.startAStar(runConfig) ?: Pair(emptyList(), (-1L).toBigInteger())
    val (aStarPath, aStarCost) = AStar().startAStar(configTest) ?: Pair(emptyList(), (-1L).toBigInteger())

    println(SimulationRecord(
            configTest.startNode as GraphPortNode,
            configTest.loadingPort as GraphPortNode,
            configTest.goalNode as GraphPortNode,
            aStarPath,
            aStarCost,
            0L,
            exhaustivePath,
            exhaustiveCost,
            0L,
            ""
    ))

     */


    println()
    println()
    println()

    val resultRecords = mutableListOf<SimulationRecord>()
    val numCombinations = subSetOfPorts.toSet().combinations(3).size * 6 //times 6 because of all permutations of 3 ports

    val pb = ProgressBar("Progress: ", numCombinations.toLong())


    var counter = 0
    coroutineScope {

        subSetOfPorts.toSet().combinations(3).forEach { ports ->

            withContext(Dispatchers.Default) {
                hey@ for (it in ports.permutations()) {

                    Logger.log("Evaluating combination #${++counter}/$numCombinations...")
                    pb.step()


                    val startPort = it[0]
                    val loadingPort = it[1]
                    val goalPort = it[2]
                    val numTonnes = 1000

                    // Uncomment this if we are running big test
                    if ("${startPort.portId}-${loadingPort.portId}-${goalPort.portId}" in alreadyTestedCombinations) {
                        Logger.log("Combination of ports is already tested. Skipping...", LogType.WARNING)
                        continue@hey
                    }

                    Logger.log("${startPort.portId} -> ${loadingPort.portId} -> ${goalPort.portId}")

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


                    val startTime = System.currentTimeMillis()

//            GlobalScope.launch {
                    //            runBlocking {

                    /*
                    val exhaustiveJob = async {
                        Logger.log("Launching exhaustive search in coroutine")
                        ExhaustiveSearch.performExhaustiveSearch(runConfig)
                                ?: Pair(emptyList(), (-1L).toBigInteger())
                    }

                    val aStarJob = async {
                        Logger.log("Launching A* search in coroutine")
                        AStar.startAStar(runConfig) ?: Pair(emptyList(), (-1L).toBigInteger())
                    }

                    val (exhaustivePath, exhaustiveCost) = exhaustiveJob.await()
                    val (aStarPath, aStarCost) = aStarJob.await()

                     */


                    /*
                    val exhaustiveTime = measureTimeMillis {
                        val (exhaustivePath1, exhaustiveCost1) = withContext(Dispatchers.Default) {
                            Logger.log("Launching exhaustive search in coroutine")
                            ExhaustiveSearch.performExhaustiveSearch(runConfig)
                                    ?: Pair(emptyList(), (-1L).toBigInteger())
                        }
                        exhaustivePath = exhaustivePath1
                        exhaustiveCost = exhaustiveCost1
                    }


                    var aStarPath: Path = emptyList()
                    var aStarCost: Cost = 0.toBigInteger()
                    val aStarTime = measureTimeMillis {
                        val (aStarPath1, aStarCost1) = withContext(Dispatchers.Default) {
                            Logger.log("Launching A* search in coroutine")
                            AStar.startAStar(runConfig) ?: Pair(emptyList(), (-1L).toBigInteger())
                        }
                        aStarPath = aStarPath1
                        aStarCost = aStarCost1
                    }

                     */

                    var exhaustiveDuration1 = 0L
                    var exhaustiveDuration2 = 0L
                    var aStarDuration1 = 0L
                    var aStarDuration2 = 0L

                    val deferredExhaustive1 = async {
                        val start = System.currentTimeMillis()
                        Logger.log("Launching exhaustive search part1 in coroutine")
                        val res = ExhaustivePathfinder().performExhaustivePathfinding(graph, startNode = runConfig.startNode, goalNode = loadingPort, ship = ship, isLoaded = false, progressBarMessage = "Exhaustive (part 1):")
                                ?: run {
                                    Logger.log("Did not find first path of exhaustive search.", LogType.WARNING)
                                    Pair(emptyList<GraphEdge>(), (-1L).toBigInteger())
                                }
                        exhaustiveDuration1 = System.currentTimeMillis() - start
                        Logger.log("Found Exhaustive path 1. Path length: ${res.first.size}, duration: $exhaustiveDuration1", LogType.DEBUG)
                        res
                    }

                    val deferredExhaustive2 = async {
                        val start = System.currentTimeMillis()
                        Logger.log("Launching exhaustive search part2 in coroutine")
                        val res = ExhaustivePathfinder().performExhaustivePathfinding(graph, startNode = runConfig.loadingPort, goalNode = runConfig.goalNode, ship = ship, isLoaded = true, progressBarMessage = "Exhaustive (part 2):")
                                ?: run {
                                    Logger.log("Did not find first path of exhaustive search.", LogType.WARNING)
                                    Pair(emptyList<GraphEdge>(), (-1L).toBigInteger())
                                }
                        exhaustiveDuration2 = System.currentTimeMillis() - start
                        Logger.log("Found Exhaustive path 2. Path length: ${res.first.size}, duration: $exhaustiveDuration2", LogType.DEBUG)
                        res
                    }

                    val deferredAStar1 = async {
                        val start = System.currentTimeMillis()
                        Logger.log("Launching A* search in part1 coroutine")
                        val res = graph.performPathfindingBetweenPorts(runConfig.startNode, runConfig.loadingPort, ship, isLoaded = false, progressBarMsg = "A* (part 1):")
                                ?: run {
                                    Logger.log("Could not find first A-star path", LogType.WARNING)
                                    Pair(emptyList<GraphEdge>(), (-1L).toBigInteger())
                                }
                        aStarDuration1 = System.currentTimeMillis() - start
                        Logger.log("Found A* path 1. Path length: ${res.first.size}, duration: $aStarDuration1", LogType.DEBUG)
                        res
                    }

                    val deferredAStar2 = async {
                        val start = System.currentTimeMillis()
                        Logger.log("Launching A* search in part2 coroutine")
                        val res = graph.performPathfindingBetweenPorts(runConfig.loadingPort, runConfig.goalNode, ship, isLoaded = true, progressBarMsg = "A* (part 1):")
                                ?: run {
                                    Logger.log("Could not find first A-star path", LogType.WARNING)
                                    Pair(emptyList<GraphEdge>(), (-1L).toBigInteger())
                                }
                        aStarDuration2 = System.currentTimeMillis() - start
                        Logger.log("Found A* path 2. Path length: ${res.first.size}, duration: $aStarDuration2", LogType.DEBUG)
                        res
                    }


                    val (aStarPath1, aStarCost1) = deferredAStar1.await()
                    val (aStarPath2, aStarCost2) = deferredAStar2.await()
                    val (exhaustivePath1, exhaustiveCost1) = deferredExhaustive1.await()
                    val (exhaustivePath2, exhaustiveCost2) = deferredExhaustive2.await()

                    val aStarPath = aStarPath1 + aStarPath2
                    val aStarCost = aStarCost1 + aStarCost2 + (numTonnes * loadingPortPrice).toBigInteger()
                    val aStarDuration = aStarDuration1 + aStarDuration2
                    val exhaustiveCost = exhaustiveCost1 + exhaustiveCost2 + (numTonnes * loadingPortPrice).toBigInteger()
                    val exhaustivePath = exhaustivePath1 + exhaustivePath2
                    val exhaustiveDuration = exhaustiveDuration1 + exhaustiveDuration2



                    //Calculate meta:
//                val exhaustiveDuration = (middleTime - startTime) / 1000 // seconds
//                val AStarDuration = (endTime - middleTime) / 1000 // seconds
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
                            aStarDuration / 1000,
                            exhaustivePath,
                            exhaustiveCost,
                            exhaustiveDuration / 1000,
                            infoMsg
                    ))
                }


                val endTime = System.currentTimeMillis()
            }
        }
    }

    pb.close()

    resultRecords.sortBy { it.getPortsAsString() as String? }

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
        if (it.aStarPath.isNotEmpty()) {
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
    val finalTime = System.currentTimeMillis()
    Logger.log("Running time: ${(finalTime - globalStartTime) / 1000} seconds")

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
                Duration (s): $aStarDuration
                ---
                Length of Exhaustive path: ${exhaustivePath.size}
                Exhaustive Cost: $exhaustiveCost
                Exhaustive Duration (s): $exhaustiveDuration
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


fun getNewGraph(portPoints: List<GraphPortNode>) = if (Config.createNewGraph) {
    val worldCountries = GeoJson.readWorldCountriesGeoJSON(Config.worldCountriesGeoJsonFile)
    val g = GraphUtils.createLatLonGraph(portPoints, 100, worldCountries)
//    GraphUtils.createKlavenessGraph(polygons, portPoints, groupedPoints, worldCountries)
    ObjectOutputStream(FileOutputStream(Config.graphFilePath)).use {
        it.writeObject(g)
        Logger.log("Graph written to file")
    }
    g
} else {
    var g: Graph? = null
    ObjectInputStream(FileInputStream(Config.graphFilePath)).use {
        g = it.readObject() as Graph
        Logger.log("Graph read from file")
    }
    g!!
}


fun readResultsFile(path: String): List<String> {
    val records = mutableListOf<String>()

    try {
        val fileReader = BufferedReader(FileReader(path))
        val headers = fileReader.readLine()
        var line = fileReader.readLine()
        while (line != null) {
            val params = line.split(",")
            val item = "${params[0]}-${params[1]}-${params[2]}"
            records.add(item)
            line = fileReader.readLine()
        }

    } catch (e: Exception) {
        println("Error parsing trade pattern file")
        println(e)
    }

    println("Found ${records.size} trade patterns")
    return records
}
