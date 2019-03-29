import Models.Graph
import Models.GraphEdge
import Models.GraphNode
import Models.Ship
import Utilities.*
import me.tongfei.progressbar.ProgressBar

data class NodeRecord(
        var node: GraphNode,
        var connection: GraphEdge?,
        var costSoFar: Long,
        var estimatedTotalCost: Long
) {
    constructor(node: GraphNode) : this(node, null, -1, -1)

    fun flipConnectionNodes(): NodeRecord {
        val tmp = connection?.fromNode
        connection?.fromNode = connection?.toNode!!
        connection?.toNode = tmp!!
        return this
    }
}

object AStar {


    fun startAStar(graph: Graph, startNode: GraphNode, goalNode: GraphNode, possibleLoadingPortsWithPrices: Map<String, Int>, ship: Ship, numTonnes: Int): Pair<List<GraphEdge>?,Long> {

        assert(startNode != goalNode)
        assert(startNode in graph.nodes)
        assert(goalNode in graph.nodes)

        val start = graph.getPortById(Config.startPortId)

        val result = mutableMapOf<GraphNode, Pair<List<GraphEdge>, Long>>()

        possibleLoadingPortsWithPrices.forEach { portId, price ->
            val loadingPort = graph.getPortById(portId)
            Logger.log("Evaluating for loading port: $loadingPort.")
            val (path1, cost1) = graph.performPathfindingBetweenPorts(startNode, loadingPort, ship, isLoaded = false)!!
//            Logger.log("Reached loading port.", LogType.DEBUG)
            val (path2, cost2) = graph.performPathfindingBetweenPorts(loadingPort, goalNode, ship, isLoaded = true)!!

            result[loadingPort] = Pair((path1 + path2.asIterable()), cost1 + cost2 + (numTonnes * price))

        }

        val sortedResult = result
                .toList()
                .sortedByDescending<Pair<GraphNode, Pair<List<GraphEdge>, Long>>, Long> { it.second.second }
                .toMap()


        val geoJsonElements = mutableListOf<String>()
        var thickness = 2

        sortedResult.forEach { k, v ->
            val color = getRandomColor()
            val props = """
                "marker-color": "$color",
                "stroke":"$color",
                "total-cost": ${v.second}
            """.trimIndent()
            val geoJsonElement = Utilities.GeoJson.pathToGeoJson(v.first, color = color, thickness = thickness.toString(), label = "${v.second}")
            val portPoint = GeoJson.createGeoJsonElement(GeoJsonType.POINT, "[${k.position.lon}, ${k.position.lat}]", props)
            geoJsonElements.add(geoJsonElement)
            geoJsonElements.add(portPoint)
            thickness += 2
        }

        val startPin = GeoJson.createGeoJsonElement(GeoJsonType.POINT, "[${start.position.lon}, ${start.position.lat}]")
        val goalPin = GeoJson.createGeoJsonElement(GeoJsonType.POINT, "[${goalNode.position.lon}, ${goalNode.position.lat}]")
        geoJsonElements.add(startPin)
        geoJsonElements.add(goalPin)

        val elements = geoJsonElements.joinToString(separator = ",")
        val geoJson = GeoJson.getGeoJson(elements)

        Logger.log("GeoJson of cheapest path:")
        println(geoJson)

        val minPath = sortedResult
                .minBy { it.value.second }!! // minimize by cost
                .value.first // Return path

        val minCost = sortedResult
                .minBy { it.value.second }!! // minimize by cost
                .value.second // Return path

        return Pair(minPath, minCost)
    }


    private fun Graph.performPathfindingBetweenPorts(startNode: GraphNode, goalNode: GraphNode, ship: Ship, isLoaded: Boolean): Pair<MutableList<GraphEdge>, Long>? {
        //TODO: Replace estimatedTotalCost with heuristic function
        val startRecord = NodeRecord(node = startNode, connection = null, costSoFar = 0, estimatedTotalCost = startNode.position.distanceFrom(goalNode.position).toLong())


        val totalDist = startNode.position.distanceFrom(goalNode.position).toLong()
        val pb = ProgressBar("From ${startNode.name} to ${goalNode.name}: ", totalDist)

        val openList = mutableListOf(startRecord)
        val closedList = mutableListOf<NodeRecord>()

        var currentNode: NodeRecord? = null

        while (openList.isNotEmpty()) {
            openList.sortBy { it.estimatedTotalCost as Long? }
            currentNode = openList.getCheapestEstimatedNode()

            val estimatedDistToDest = currentNode.node.position.distanceFrom(goalNode.position).toLong()
            val progress = if ((totalDist - estimatedDistToDest) > 0) totalDist - estimatedDistToDest else 0
            pb.stepTo(progress)


            if (currentNode.node == goalNode) {
                closedList.add(currentNode.flipConnectionNodes())
//                closedList.add(currentNode)
                break
            }

            val connections = getConnections(currentNode)
            for (connection in connections) {
//                val endNode = if (connection.toNode == currentNode.node) connection.fromNode else connection.toNode // Undirected graph
                val endNode = connection.toNode // Directed graph

//                val endNodeCost = currentNode.costSoFar + connection.distance
//                val endNodeCost = (currentNode.costSoFar + ship.calculateCost(connection, isLoaded)/1000.0).toInt()
                val endNodeCost: Long = currentNode.costSoFar + ship.calculateCost(connection, isLoaded)

                var endNodeRecord: NodeRecord?
                var endNodeHeuristic: Long

                if (closedList.containsNode(endNode)) { // Does closed list contain node?
                    endNodeRecord = closedList.getNodeRecordOfNode(endNode)

                    // If the new node has a lower score than the already stored node, ignore it
                    if (endNodeRecord.costSoFar <= endNodeCost) {
                        continue
                    } else {
                        closedList.remove(endNodeRecord)
                    }

                    endNodeHeuristic = endNodeCost - endNodeRecord.costSoFar

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
                    endNodeHeuristic = endNode.position.distanceFrom(goalNode.position).toLong() // Todo: Perform estimate here...

                }

                assert(endNodeRecord != null)

                endNodeRecord.costSoFar = endNodeCost
                endNodeRecord.connection = connection
                endNodeRecord.estimatedTotalCost = endNodeCost + endNodeHeuristic

                if (!openList.containsNode(endNode)) {
                    openList.add(endNodeRecord)
                }

            } // End of connections for loop

            // Current node is now handled. Move it to closed list.
            openList.remove(currentNode)
            closedList.add(currentNode)

            if (currentNode.node == goalNode) {
                println("Reached goal! ")
                break
            }

        } // End of while loop

        pb.close()


        assert(currentNode != null)
        return if (currentNode!!.node != goalNode) {
            // Did not find a path
            null

        } else {


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
            Pair(path.asReversed(), totalCost)
        }
    }

}


fun List<NodeRecord>.getCheapestEstimatedNode(): NodeRecord {
    assert(this.isNotEmpty())
    return this.minBy { it.estimatedTotalCost }!!
}

fun Graph.getConnections(node: NodeRecord): Set<GraphEdge> {
    return this.edges.filter {
        it.fromNode == node.node ||
                it.toNode == node.node
    }.toSet()
}

fun Graph.getOutgoingConnectionsFrom(node: NodeRecord): Set<GraphEdge> {
    return this.edges.filter {
        it.fromNode == node.node
    }.toSet()
}

fun List<NodeRecord>.containsNode(node: GraphNode): Boolean {
    return this.find { it.node == node } != null
}


fun List<NodeRecord>.getNodeRecordOfNode(node: GraphNode): NodeRecord {
    return this.find { it.node == node }!!
}