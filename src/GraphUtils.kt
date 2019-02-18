import ch.hsr.geohash.GeoHash

object GraphUtils {


    fun createGraph(polygons: List<Polygon>, ports: List<Port>, sanitizedGroupedPoints: List<Node>): Graph {


        // 1) Generate outer connection on each polygon:

        val connections = polygons.map { polygon ->
            polygon.polygonPoints
//                    .map { position -> GraphNode(polygon.name, position) }
                    .map { position -> sanitizedGroupedPoints.getNodeWithPosition(position.flip()) }
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
            val centerPos = polygon.getCenterPosition()
            val centerPos2 = centerPos.flip()
            val centerNode = Node(centerPos, polygon.name, isPort = false, geohash = GeoHash.withBitPrecision(centerPos2.lat, centerPos2.lon, 64))
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


        val newConnections = connections.filterIndexed {i, edge ->
            connections.subList(i, connections.size).contains(edge)
        }

        println("Size of newConnections: ${newConnections.size}")

        val allNodes = portPoints + polygons.map { it.graphNodes }.flatten()

        val graph = Graph(connections, allNodes)

        return graph

    }
}


