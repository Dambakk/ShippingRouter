import Models.*
import Utilities.*
import java.math.BigInteger

data class NodeRecord(
        var node: GraphNode,
        var connection: GraphEdge?,
        var costSoFar: BigInteger,
        var timeSoFar: Long,
        var estimatedTotalCost: BigInteger
) {
    constructor(node: GraphNode) : this(node = node,
            connection = null,
            costSoFar = (-1).toBigInteger(),
            timeSoFar = -1,
            estimatedTotalCost = (-1).toBigInteger()
    )
}

object AStar {

    var openListHistory = mutableListOf<NodeRecord>()


    fun startAStar(c: RunConfiguration) =
            startAStar(c.graph, c.startNode, c.goalNode, mapOf((c.loadingPort as GraphPortNode).portId to c.portPricePrTon), c.ship, c.numTonnes, emptyList())


    fun startAStar(
            graph: Graph,
            startNode: GraphNode,
            goalNode: GraphNode,
            possibleLoadingPortsWithPrices: Map<String, Int>,
            ship: Ship,
            numTonnes: Int,
            subsetOfPorts: List<GraphPortNode>
    ): Pair<Path, Cost>? {

        assert(startNode != goalNode)
        assert(startNode in graph.nodes)
        assert(goalNode in graph.nodes)

        val result = mutableMapOf<GraphNode, Pair<List<GraphEdge>, BigInteger>>()

        possibleLoadingPortsWithPrices.forEach { (portId, pricePrTon) ->
            val loadingPort = graph.getPortById(portId)
            Logger.log("Evaluating for loading port: $loadingPort.")
            val (path1, cost1) = graph.performPathfindingBetweenPorts(startNode, loadingPort, ship, isLoaded = false, progressBarMsg = "A* (part 1):")
                    ?: run {
                        Logger.log("Could not find first A-star path", LogType.WARNING)
                        return null
                    }
            val (path2, cost2) = graph.performPathfindingBetweenPorts(loadingPort, goalNode, ship, isLoaded = true, progressBarMsg = "A* (part 2):")
                    ?: run {
                        Logger.log("Could not second first A-star path", LogType.WARNING)
                        return null
                    }

            result[loadingPort] = Pair((path1 + path2.asIterable()), cost1 + cost2 + (numTonnes * pricePrTon).toBigInteger())

        }

        val sortedResult = result
                .toList()
                .sortedByDescending<Pair<GraphNode, Pair<List<GraphEdge>, BigInteger>>, BigInteger> { it.second.second }
                .toMap()

        var thickness = 2

        val geoJson = featureCollection {
            point(startNode.position.lon, startNode.position.lat) {
                "marker-color" value "#FF0000"
            }
            point(goalNode.position.lon, goalNode.position.lat) {
                "marker-color" value "#00FF00"
            }
            sortedResult.forEach { (k, v) ->
                val color = getRandomColor(k.name)
                point(k.position.lon, k.position.lat) {
                    "marker-color" value color
                    "label" value k.name
                }
                lineString {
                    v.first.forEach { add(coord lat it.fromNode.position.lat lng it.fromNode.position.lon) }
                    "stroke" value color
                    "stroke-width" value thickness
                    "stroke-opacity" value 1
                    "label" value k.name
                    "aStarCost" value v.second
                }
                thickness += 2
            }
        }.toGeoJson()

//        Logger.log("HERE IS GEOJSON WE'VE ALL BEEN WAITING FOR:", LogType.SUCCESS)
//        println()
//        println(geoJson)
//        println()

        val openListMarkers = featureCollection {
            openListHistory.forEach {
                point(it.node.position.lon, it.node.position.lat)
            }
        }.toGeoJson()


        val minPath = sortedResult
                .minBy { it.value.second }!! // sort by cost
                .value.first // Return aStarPath

        val minCost = sortedResult
                .minBy { it.value.second }!! // sort by cost
                .value.second // Return aStarPath

        return Pair(minPath, minCost)
    }


}


fun Graph.performPathfindingBetweenPorts(
        startNode: GraphNode,
        goalNode: GraphNode,
        ship: Ship,
        isLoaded: Boolean,
        startTime: Long = 0L,
        progressBarMsg: String = ""
): Triple<Path, Cost, Triple<Int, Int, Int>>? {
    val startRecord = NodeRecord(node = startNode, connection = null, costSoFar = 0.toBigInteger(), timeSoFar = startTime, estimatedTotalCost = startNode.position.distanceFrom(goalNode.position).toBigInteger())

    var numberOfClosedNodesVisited = 0
    var numberOfOpenNodesVisited = 0
    var numberOfEdgesProcessed = 0

    val openList = mutableListOf(startRecord)
    val closedList = mutableListOf<NodeRecord>()

    var currentNode: NodeRecord? = null
    var currentTime = 0L

    loop@ while (openList.isNotEmpty()) {
        openList.sortBy { it.estimatedTotalCost as BigInteger? }
        currentNode = openList.getCheapestEstimatedNode()
        numberOfOpenNodesVisited++

        if (currentNode.node == goalNode) {
            closedList.add(currentNode)
            numberOfClosedNodesVisited++
            break
        }

        val connections = getOutgoingConnectionsFromNodeRecord(currentNode, goalNode)
        for (connection in connections) {
            numberOfEdgesProcessed++
            val endNode = connection.toNode // Directed graph
            val endNodeTime: Long = currentNode!!.timeSoFar + ship.calculateTimeSpentOnEdge(connection)
            val reachedAllTimeWindows = ship.isObeyingAllTimeWindows(connection.toNode, endNodeTime)
            if (!reachedAllTimeWindows) {
                Logger.log("Breaking loop because we did not meet time window at $endNodeTime for node ${connection.toNode.name}", LogType.ERROR)
                currentNode = null
                break@loop
            }
            val endNodeCost: BigInteger = currentNode.costSoFar + ship.calculateCost(connection, isLoaded, endNodeTime)

            var endNodeRecord: NodeRecord?
            var endNodeHeuristic: BigInteger

            if (closedList.containsNode(endNode)) { // Does closed list contain node?
                endNodeRecord = closedList.getNodeRecordOfNode(endNode)

                // If the new node has a lower score than the already stored node, ignore it
                if (endNodeRecord.costSoFar <= endNodeCost) {
                    continue
                } else {
                    closedList.remove(endNodeRecord)
                }

                // Heuristic:
//                endNodeHeuristic = (endNodeCost - endNodeRecord.costSoFar)
                endNodeHeuristic = endNode.position.distanceFrom(goalNode.position).toBigInteger() // Heuristic: Great-cicle distance

            } else if (openList.containsNode(endNode)) {
                endNodeRecord = openList.getNodeRecordOfNode(endNode)

                if (endNodeRecord.costSoFar <= endNodeCost) {
                    continue
                }

                // Heuristic:
//                endNodeHeuristic = endNodeCost - endNodeRecord.costSoFar
                endNodeHeuristic = endNode.position.distanceFrom(goalNode.position).toBigInteger() // Heuristic: Great-cicle distance

            } else { //Unvisited (new) node
                endNodeRecord = NodeRecord(endNode)
                endNodeHeuristic = endNode.position.distanceFrom(goalNode.position).toBigInteger() // Heuristic: Great-cicle distance
            }

            currentTime = endNodeTime
            endNodeRecord.costSoFar = endNodeCost
            endNodeRecord.timeSoFar = endNodeTime
            endNodeRecord.connection = connection
            endNodeRecord.estimatedTotalCost = endNodeCost + endNodeHeuristic

            if (!openList.containsNode(endNode)) {
                openList.add(endNodeRecord)
//                openListHistory.add(endNodeRecord)
            }

        } // End of connections for loop

        // Current node is now handled. Move it to closed list.
        openList.remove(currentNode)
        closedList.add(currentNode!!)
        numberOfClosedNodesVisited++


        if (currentNode.node == goalNode) {
            Logger.log("Reached goal! (2) ", LogType.DEBUG)
            break
        }

    } // End of while loop

    return when (currentNode) {
        null -> {
            Logger.log("Did not meet time requirements", LogType.ERROR)
            null
        }
        else -> {
            if (currentNode.node != goalNode) {
                Logger.log("Did not find aStarPath", LogType.ERROR)
                Logger.log("Current node for A* is this: $currentNode", LogType.WARNING)
                null
            } else {

                //TODO: Make snapshot of closed and open lists to make cool graphics afterwards

                val totalCost = currentNode.costSoFar
                //Did find a valid aStarPath
                val path = mutableListOf<GraphEdge>()
                while (currentNode!!.node != startNode) {
                    path.add(currentNode.connection!!)
                    closedList.remove(currentNode)
                    currentNode = closedList.find {
                        it.node == currentNode!!.connection!!.fromNode ||
                                it.node == currentNode!!.connection!!.toNode
                    }!!
                }
                Triple(path.asReversed(), totalCost, Triple(numberOfOpenNodesVisited, numberOfClosedNodesVisited, numberOfEdgesProcessed))
            }
        }
    }
}


fun List<NodeRecord>.getCheapestEstimatedNode(): NodeRecord {
    assert(this.isNotEmpty())
    return this.minBy { it.estimatedTotalCost }!!
}


fun Graph.getOutgoingConnectionsFromNodeRecordNoPortsExceptGoal(node: NodeRecord, goalNode: GraphNode): Set<GraphEdge> {
    return this.edges
            .filter { it.fromNode == node.node }
            .filter { it.toNode == goalNode || it.toNode !is GraphPortNode }
            .toSet()
}

fun Graph.getOutgoingConnectionsFromNodeRecord(node: NodeRecord, goalNode: GraphNode): Set<GraphEdge> {
    return this.edges
            .filter { it.fromNode == node.node }
            .toSet()
}

fun List<NodeRecord>.containsNode(node: GraphNode): Boolean {
    return this.find { it.node == node } != null
}

fun List<NodeRecord>.getNodeRecordOfNode(node: GraphNode): NodeRecord {
    return this.find { it.node == node }!!
}
