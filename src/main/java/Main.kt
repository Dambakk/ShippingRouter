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

    val graph = getGraph(portPoints)
    Logger.log("Graph loaded/created! (Nodes: ${graph.nodes.size}, edges: ${graph.edges.size})")

    val alreadyTestedCombinations = readResultsFile("output/big-run-03/simulationResult.csv")

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
                                    Triple(emptyList<GraphEdge>(), (-1L).toBigInteger(), Pair(-1, -1))
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
                                    Triple(emptyList<GraphEdge>(), (-1L).toBigInteger(), Pair(-1, -1))
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
                                    Triple(emptyList<GraphEdge>(), (-1L).toBigInteger(), Triple(-1, -1, -1))
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
                                    Triple(emptyList<GraphEdge>(), (-1L).toBigInteger(), Triple(-1, -1, -1))
                                }
                        aStarDuration2 = System.currentTimeMillis() - start
                        Logger.log("Found A* path 2. Path length: ${res.first.size}, duration: $aStarDuration2", LogType.DEBUG)
                        res
                    }


                    val (aStarPath1, aStarCost1, aMeta1) = deferredAStar1.await()
                    val (aStarPath2, aStarCost2, aMeta2) = deferredAStar2.await()
                    val (exhaustivePath1, exhaustiveCost1, eMeta1) = deferredExhaustive1.await()
                    val (exhaustivePath2, exhaustiveCost2, eMeta2) = deferredExhaustive2.await()

                    val aStarPath = aStarPath1 + aStarPath2
                    val aStarCost = aStarCost1 + aStarCost2 + (numTonnes * loadingPortPrice).toBigInteger()
                    val aStarDuration = aStarDuration1 + aStarDuration2
                    val exhaustiveCost = exhaustiveCost1 + exhaustiveCost2 + (numTonnes * loadingPortPrice).toBigInteger()
                    val exhaustivePath = exhaustivePath1 + exhaustivePath2
                    val exhaustiveDuration = exhaustiveDuration1 + exhaustiveDuration2

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
                            startPort = startPort,
                            loadingPort = loadingPort,
                            goalPort = goalPort,
                            aStarPath = aStarPath,
                            aStarCost = aStarCost,
                            aStarDuration = aStarDuration / 1000,
                            aStarOpenNodesVisited = aMeta1.first + aMeta2.first,
                            aStarClosedNodesVisited = aMeta1.second + aMeta2.second,
                            aStarEdgesVisited = aMeta1.third + aMeta2.third,
                            exhaustivePath = exhaustivePath,
                            exhaustiveCost = exhaustiveCost,
                            exhaustiveDuration = exhaustiveDuration / 1000,
                            exhaustiveNodesVisited = eMeta1.first + eMeta2.first,
                            exhaustiveEdgesVisited = eMeta1.second + eMeta2.second,
                            infoMsg = infoMsg
                    ))
                }
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
        val aStarOpenNodesVisited: Int,
        val aStarClosedNodesVisited: Int,
        val aStarEdgesVisited: Int,
        val exhaustivePath: Path,
        val exhaustiveCost: BigInteger,
        val exhaustiveDuration: Long,
        val exhaustiveNodesVisited: Int,
        val exhaustiveEdgesVisited: Int,
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
                NumOpenNodes: $aStarOpenNodesVisited
                NumClosedNodes: $aStarClosedNodesVisited
                NumEdges: $aStarEdgesVisited
                ---
                Length of Exhaustive path: ${exhaustivePath.size}
                Exhaustive Cost: $exhaustiveCost
                Exhaustive Duration (s): $exhaustiveDuration
                NumNodes: $exhaustiveNodesVisited
                NumEdges: $exhaustiveEdgesVisited
            """.trimIndent()

    fun toCommaSeparatedString() =
            "${startPort.portId},${loadingPort.portId},${goalPort.portId},$aStarCost,$aStarDuration,${aStarPath.size},$aStarOpenNodesVisited,$aStarClosedNodesVisited,$aStarEdgesVisited,${startPort.portId}-${loadingPort.portId}-${goalPort.portId}-aStar.json,$exhaustiveCost,$exhaustiveDuration,${exhaustivePath.size},$exhaustiveNodesVisited,$exhaustiveEdgesVisited,${startPort.portId}-${loadingPort.portId}-${goalPort.portId}-exhaustive.json,$infoMsg" as CharSequence

    fun getPortsAsString() =
            "${startPort.portId}-${loadingPort.portId}-${goalPort.portId}"

    companion object {
        fun getCommaSeparatedHeaders() =
                "startPortId,loadingPortId,goalPortId,aStarCost,aStarDuration,aStarPathSize,aStarOpenNodesVisited,aStarClosedNodesVisited,aStarEdgesVisited,aStarGeojsonPath,exhaustiveCost,exhaustiveDuration,exhaustivePathSize,exhaustiveNodesVisited,exhaustiveEdgesVisited,exhaustiveGeojsonPath,infoMsg"

    }
}

data class RunConfiguration(val graph: Graph,
                            val startNode: GraphNode,
                            val loadingPort: GraphNode,
                            val goalNode: GraphNode,
                            val portPricePrTon: Int,
                            val ship: Ship,
                            val numTonnes: Int)


fun getGraph(portPoints: List<GraphPortNode>) = if (Config.createNewGraph) {
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
