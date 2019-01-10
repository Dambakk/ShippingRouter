object GraphUtils {


    fun createGraph(polygons: List<Polygon>, ports: List<Port>) {


        // 1) Generate outer connection on each polygon:

        val connections = polygons.map { polygon ->
            polygon.polygonPoints
                    .map { position -> GraphNode(polygon.name, position) }
                    .zipWithNext()
                    .map { GraphEdge(it.first, it.second, it.first.position.distanceFrom(it.second.position).toInt()) }
                    .map {
                        val res = it.splitInTwo()
//                        polygon.graphNodes.add(res.first().fromNode)
                        polygon.graphNodes.add(res.first().toNode) // This is the middle node
                        res
                    }
                    .flatten()
        }.flatten().toMutableList()

        println("1) Generated ${connections.size} connections")


        // 2) Find center in each polygon and connect with polygon
        val centersNodes = polygons.map { polygon ->
            val centerNode = GraphNode(polygon.name, polygon.getCenterPosition())
            val newNodes = mutableListOf<ShippingNode>()
            polygon.graphNodes.forEach {
                val connection = GraphEdge(centerNode, it, centerNode.position.distanceFrom(it.position).toInt()).splitInTwo()
//                polygon.graphNodes.add(split.first().fromNode)
                newNodes.add(connection.first().toNode) // This is the middle node
                connections.addAll(connection)
            }
            polygon.graphNodes.add(centerNode)
            polygon.graphNodes.addAll(newNodes)
            polygon.middleNodes.addAll(newNodes)
            centerNode
        }

        println("2) Generated ${centersNodes.size} center nodes")



        // 2.5) Add connection between the nodes in the middle layer of each polygon

        polygons.forEach { polygon ->
            polygon.middleNodes.add(polygon.middleNodes.first())
            polygon.middleNodes.zipWithNext { a, b ->
                connections.add(GraphEdge(a, b, a.position.distanceFrom(b.position).toInt()))
            }
        }

        // 3) Connect center with each node in polygon

//        centersNodes.forEach { center ->
//            val polygonNodes = polygons.find { it.name == center.name }
//                    ?.polygonPoints?.map { position -> GraphNode(center.name, position) }
//            val polygonNodes = polygons.find { it.name == center.name }
//                    ?.polygonPoints?.map { position -> GraphNode(center.name, position) }
//            val centerConnections = polygonNodes?.map { GraphEdge(center, it, center.position.distanceFrom(it.position).toInt()) }
//
//            if (centerConnections != null) {
//                connections.addAll(centerConnections)
//            }
//        }

        println("3) Generated connections from each center node to corresponding polygon nodes. Now, a total of ${connections.size} connections")


        // 4) Create connections from ports to polygon edges

        val portPoints = ports.map {
            val polygon = it.position.getCorrespondingPolygon(polygons)
            GraphPortNode(it.name, it.position.flip(), polygon!!)
        }

        portPoints.forEach { port ->
            port.polygon.graphNodes.forEach {
                val connection = GraphEdge(port, it, port.position.distanceFrom(it.position).toInt())
                connections.add(connection)
            }
        }

        println("4) Generated connections from ports to nodes in polygon that port belongs to. Now, a total of ${connections.size} connections")


        // 4) Generate inbetween nodes


        // 5) Connect inbetween nodes with surroundings


    }
}


