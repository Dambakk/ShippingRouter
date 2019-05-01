import Models.*
import Utilities.*
import com.marcinmoskala.math.combinations
import me.tongfei.progressbar.ProgressBar
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
            estimatedTotalCost = (-1).toBigInteger())

    fun flipConnectionNodes(): NodeRecord {
        val tmp = connection?.fromNode
        connection?.fromNode = connection?.toNode!!
        connection?.toNode = tmp!!
        return this
    }
}

object AStar {


    var openListHistory = mutableListOf<NodeRecord>()


    fun startAStar(
            graph: Graph,
            startNode: GraphNode,
            goalNode: GraphNode,
            possibleLoadingPortsWithPrices: Map<String, Int>,
            ship: Ship,
            numTonnes: Int,
            subsetOfPorts: List<GraphPortNode>
    ): Pair<List<GraphEdge>, BigInteger> {

//        assert(startNode != goalNode)
        assert(startNode in graph.nodes)
        assert(goalNode in graph.nodes)

        val start = graph.getPortById(Config.startPortId)

        val result = mutableMapOf<GraphNode, Pair<List<GraphEdge>, BigInteger>>()



        possibleLoadingPortsWithPrices.forEach { (portId, pricePrTon) ->
            val loadingPort = graph.getPortById(portId)
            Logger.log("Evaluating for loading port: $loadingPort.")
            val (path1, cost1) = graph.performPathfindingBetweenPorts(startNode, loadingPort, ship, isLoaded = false)!!
//            Logger.log("Reached loading port.", LogType.DEBUG)
            val (path2, cost2) = graph.performPathfindingBetweenPorts(loadingPort, goalNode, ship, isLoaded = true)!!

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
                    "cost" value v.second
                }
                thickness += 2
            }
        }.toGeoJson()

        Logger.log("HERE IS GEOJSON WE'VE ALL BEEN WAITING FOR:")
        println()
        println(geoJson)
        println()

        val openListMarkers = featureCollection {
            openListHistory.forEach {
                point(it.node.position.lon, it.node.position.lat)
            }
        }.toGeoJson()

        Logger.log("Here is list of markers that has been in the open list:")
        println()
        println(openListMarkers)
        println()


//        val geoJsonElements = mutableListOf<String>()
//
//        sortedResult.forEach { k, v ->
//            val color = getRandomColor()
//            val props = """
//                "marker-color": "$color",
//                "stroke":"$color",
//                "total-factor": ${v.second}
//            """.trimIndent()
//            val geoJsonElement = GeoJson.pathToGeoJson(v.first, color = color, thickness = thickness.toString(), label = "${v.second}")
//            val portPoint = GeoJson.createGeoJsonElement(GeoJsonType.POINT, "[${k.position.lon}, ${k.position.lat}]", props)
//            geoJsonElements.add(geoJsonElement)
//            geoJsonElements.add(portPoint)
//            thickness += 2
//        }
//
//        val startPin = GeoJson.createGeoJsonElement(GeoJsonType.POINT, "[${start.position.lon}, ${start.position.lat}]")
//        val goalPin = GeoJson.createGeoJsonElement(GeoJsonType.POINT, "[${goalNode.position.lon}, ${goalNode.position.lat}]")
//        geoJsonElements.add(startPin)
//        geoJsonElements.add(goalPin)
//
//        val elements = geoJsonElements.joinToString(separator = ",")
//        val geoJson = GeoJson.getGeoJson(elements)

        Logger.log("GeoJson of cheapest path:")
        println(geoJson)

        val minPath = sortedResult
                .minBy { it.value.second }!! // minimize by factir
                .value.first // Return path

        val minCost = sortedResult
                .minBy { it.value.second }!! // minimize by factir
                .value.second // Return path

        return Pair(minPath, minCost)
    }

    private fun Graph.performPathfindingBetweenPorts(startNode: GraphNode, goalNode: GraphNode, ship: Ship, isLoaded: Boolean, startTime: Long = 0L): Pair<MutableList<GraphEdge>, BigInteger>? {
        val startRecord = NodeRecord(node = startNode, connection = null, costSoFar = 0.toBigInteger(), timeSoFar = startTime, estimatedTotalCost = startNode.position.distanceFrom(goalNode.position).toBigInteger())

        val totalDist = startNode.position.distanceFrom(goalNode.position).toLong()
        val pb = ProgressBar("From ${startNode.name} to ${goalNode.name}: ", totalDist)

        val openList = mutableListOf(startRecord)
        val closedList = mutableListOf<NodeRecord>()

        var currentNode: NodeRecord? = null

        loop@ while (openList.isNotEmpty()) {
            openList.sortBy { it.estimatedTotalCost as BigInteger? }
            currentNode = openList.getCheapestEstimatedNode()

            val estimatedDistToDest = currentNode.node.position.distanceFrom(goalNode.position).toLong()
            val progress = if ((totalDist - estimatedDistToDest) > 0) totalDist - estimatedDistToDest else 0
            pb.stepTo(progress)


            if (currentNode.node == goalNode) {
                Logger.log("Reached goal", LogType.DEBUG)
                closedList.add(currentNode.flipConnectionNodes())
//                closedList.add(currentNode)
                break
            }

//            val connections = getConnectionsForNode(currentNode)
            val connections = getOutgoingConnectionsFromNodeRecord(currentNode, goalNode)
            for (connection in connections) {
//                val endNode = if (connection.toNode == currentNode.node) connection.fromNode else connection.toNode // Undirected graph
                val endNode = connection.toNode // Directed graph

//                val endNodeCost = currentNode.costSoFar + connection.distance
//                val endNodeCost = (currentNode.costSoFar + ship.calculateCost(connection, isLoaded)/1000.0).toInt()
                val endNodeTime: Long = currentNode!!.timeSoFar + ship.calculateTimeSpentOnEdge(connection)
                val reachedAllTimeWindows = ship.isObeyingAllTimeWindows(connection.toNode, endNodeTime)
                if (!reachedAllTimeWindows) {
                    currentNode = null
                    break@loop
                }
                val endNodeCost: BigInteger = currentNode.costSoFar + ship.calculateCost(connection, isLoaded, endNodeTime)
//                Logger.log("EndNodeCost: $endNodeCost")

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

                    endNodeHeuristic = (endNodeCost - endNodeRecord.costSoFar)

                } else if (openList.containsNode(endNode)) {
                    endNodeRecord = openList.getNodeRecordOfNode(endNode)

                    if (endNodeRecord.costSoFar <= endNodeCost) {
                        continue
                    }

                    endNodeHeuristic = endNodeCost - endNodeRecord.costSoFar

                } else { //Unvisited (new) node

                    //Calculate heuristic here....
                    endNodeRecord = NodeRecord(endNode)
//                    endNodeHeuristic = endNode.position.distanceFrom(goalNode.position).toInt() * operationCost // Todo: Perform estimate here...
//                    endNodeHeuristic = ship.calculateHeuristic()
                    endNodeHeuristic = endNode.position.distanceFrom(goalNode.position).toBigInteger() // Todo: Perform estimate here...

                }

                endNodeRecord.costSoFar = endNodeCost
                endNodeRecord.timeSoFar = endNodeTime
                endNodeRecord.connection = connection
                endNodeRecord.estimatedTotalCost = endNodeCost + endNodeHeuristic

                if (!openList.containsNode(endNode)) {
                    openList.add(endNodeRecord)
                    openListHistory.add(endNodeRecord)
                }

            } // End of connections for loop

            // Current node is now handled. Move it to closed list.
            openList.remove(currentNode)
            closedList.add(currentNode!!)

            if (currentNode.node == goalNode) {
                println("Reached goal! ")
                break
            }

        } // End of while loop

        pb.close()


        return when {
            currentNode == null -> {
                Logger.log("Did not meet time requirements", LogType.WARNING)
                null
            }
            currentNode.node != goalNode -> {
                // Did not find a path
                Logger.log("Did not find path", LogType.WARNING)
                null

            }
            else -> {


                //TODO: Make snapshot of closed and open lists to make cool graphics afterwards

                val totalCost = currentNode.costSoFar
                //Did find a valid path
                val path = mutableListOf<GraphEdge>()
                while (currentNode!!.node != startNode) {
                    path.add(currentNode.connection!!)
                    closedList.remove(currentNode)
                    currentNode = closedList.find {
                        (
                                it.node == currentNode!!.connection!!.fromNode
                                        ||
                                        it.node == currentNode!!.connection!!.toNode)
                    }!!
                }
                Logger.log("Did find a path")
                Pair(path.asReversed(), totalCost)
            }
        }
    }

}


fun List<NodeRecord>.getCheapestEstimatedNode(): NodeRecord {
    assert(this.isNotEmpty())
    return this.minBy { it.estimatedTotalCost }!!
}

fun Graph.getConnectionsForNode(node: NodeRecord): Set<GraphEdge> {
    return this.edges.filter {
        it.fromNode == node.node ||
                it.toNode == node.node
    }.toSet()
}

fun Graph.getOutgoingConnectionsFromNodeRecord(node: NodeRecord, goalNode: GraphNode): Set<GraphEdge> {
    return this.edges
            .filter { it.fromNode == node.node }
            .filter { it.toNode == goalNode || it.toNode !is GraphPortNode }
            .toSet()
}

fun List<NodeRecord>.containsNode(node: GraphNode): Boolean {
    return this.find { it.node == node } != null
}


fun List<NodeRecord>.getNodeRecordOfNode(node: GraphNode): NodeRecord {
    return this.find { it.node == node }!!
}