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


    fun startAStar(graph: Graph, startNode: GraphNode, goalNode: GraphNode): MutableList<GraphEdge>? {

        assert(startNode != goalNode)
        assert(startNode in graph.nodes)
        assert(goalNode in graph.nodes)


        //TODO: Replace estimatedTotalCost with heuristic function
        val startRecord = NodeRecord(node = startNode, connection = null, costSoFar = 0, estimatedTotalCost = startNode.position.distanceFrom(goalNode.position).toInt())


        val openList = mutableListOf<NodeRecord>(startRecord)
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

//            val connections = graph.getOutgoingConnectionsFrom(currentNode)
            val connections = graph.getConnections(currentNode)
            for (connection in connections) {
                val endNode = if (connection.toNode == currentNode.node) connection.fromNode else connection.toNode
                val endNodeCost = currentNode.costSoFar + connection.cost

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
            path.asReversed()
        }

    }
}