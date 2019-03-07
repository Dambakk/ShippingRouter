import Models.Graph
import Models.GraphEdge
import Models.GraphNode
import Models.Ship
import Utilities.*

data class NodeRecord(
        var node: GraphNode,
        var connection: GraphEdge?,
        var costSoFar: Int,
        var estimatedTotalCost: Int
) {
    constructor(node: GraphNode) : this(node, null, -1, -1)

    fun flipConnectionNodes(): NodeRecord {
        val tmp = connection?.fromNode
        connection?.fromNode = connection?.toNode!!
        connection?.toNode = tmp!!
        return this
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


object AStar {


    fun startAStar(graph: Graph, startNode: GraphNode, goalNode: GraphNode, possibleLoadingPorts: List<String>, ship: Ship): List<GraphEdge>? {

        assert(startNode != goalNode)
        assert(startNode in graph.nodes)
        assert(goalNode in graph.nodes)



        val start = graph.getPortById(Config.startPortId)

        val loadingPorts = possibleLoadingPorts.map { graph.getPortById(it) }


        val res = mutableMapOf<GraphNode, Pair<List<GraphEdge>, Int>>()

        loadingPorts.forEach { port ->
            println(port)
            val (path1, cost1) = graph.performPathfindingBetweenPorts(startNode, port)!!
            val (path2, cost2) = graph.performPathfindingBetweenPorts(port, goalNode)!!

            res[port] = Pair((path1 + path2.asIterable()), cost1 + cost2)

        }

        val sortedResult = res.toList().sortedByDescending <Pair<GraphNode, Pair<List<GraphEdge>, Int>>, Int> { it.second.second }.toMap()



        val geoJsonElements = mutableListOf<String>()
        var thickness = 2

        sortedResult.forEach { k, v ->
            val color = getRandomColor()
            val props = """
                "marker-color": "$color",
                "stroke":"$color"
            """.trimIndent()
            val geoJsonElement = Utilities.GeoJson.pathToGeoJson(v.first, color = color, thickness = thickness.toString())
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
        print(geoJson)


//        val res = graph.performPathfindingBetweenPorts(startNode, goalNode)

        return null

    }



    private fun Graph.performPathfindingBetweenPorts(startNode: GraphNode, goalNode: GraphNode): Pair<MutableList<GraphEdge>, Int>? {
        //TODO: Replace estimatedTotalCost with heuristic function
        val startRecord = NodeRecord(node = startNode, connection = null, costSoFar = 0, estimatedTotalCost = startNode.position.distanceFrom(goalNode.position).toInt())


        val openList = mutableListOf(startRecord)
        val closedList = mutableListOf<NodeRecord>()

        var currentNode: NodeRecord? = null

        while (openList.isNotEmpty()) {
            openList.sortBy { it.estimatedTotalCost as Int?}
            currentNode = openList.getCheapestEstimatedNode()

            if (currentNode.node == goalNode) {
                closedList.add(currentNode.flipConnectionNodes())
//                closedList.add(currentNode)
                break
            }

            val connections = getConnections(currentNode)
            for (connection in connections) {
//                val endNode = if (connection.toNode == currentNode.node) connection.fromNode else connection.toNode // Undirected graph
                val endNode =  connection.toNode // Directed graph
                val endNodeCost = currentNode.costSoFar + connection.distance

                var endNodeRecord: NodeRecord?
                var endNodeHeuristic: Int

                if (closedList.containsNode(endNode)) { // Does closed list contain node?
                    endNodeRecord = closedList.getNodeRecordOfNode(endNode)

                    // If the new node has a lower score than the already stored node, ignore it
                    if (endNodeRecord.costSoFar <= endNodeCost) {
                        continue
                    } else {
                        closedList -= endNodeRecord
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
                    endNodeHeuristic = endNode.position.distanceFrom(goalNode.position).toInt() // Todo: Perform estimate here...

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

        assert(currentNode != null)
        return if (currentNode!!.node != goalNode) {
            // Did not find a path
            null

        } else {

            val totalCost = currentNode.costSoFar
            //Did find a valid path
            val path = mutableListOf<GraphEdge>()
            while (currentNode!!.node != startNode) {
                path.add(currentNode.connection!!)
                closedList.remove(currentNode)
                currentNode = closedList.find {
                    (
                            it.node == currentNode!!.connection!!.fromNode ||
                                    it.node == currentNode!!.connection!!.toNode)
                }!!
            }
            Pair(path.asReversed(), totalCost)
        }
    }

}