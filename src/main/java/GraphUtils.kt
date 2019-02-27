import ch.hsr.geohash.GeoHash
import org.locationtech.jts.geom.Polygon

object GraphUtils {


    fun createGraph(klavenessPolygons: List<KlavenessPolygon>, ports: List<Node>, sanitizedGroupedPoints: List<Node>, worldCountries: List<Polygon>): Graph {


        // 1) Generate outer connection on each klavenessPolygon:

//        val connections = klavenessPolygons.map { klavenessPolygon ->
//            klavenessPolygon.polygonPoints
//                    .map { position -> GraphNode(klavenessPolygon.name, position) }
//                    .map { position -> sanitizedGroupedPoints.getNodeWithPosition(position.flip()) }
//                    .zipWithNext()
//                    .map { GraphEdge(it.first, it.second, it.first.position.distanceFrom(it.second.position).toInt()) }
//                    .map {
//                        val res = it.splitInTwo()
//                        klavenessPolygon.graphNodes.add(res.first().fromNode)
//                        klavenessPolygon.graphNodes.add(res.first().toNode) // This is the middle node
//                        res
//                    }
//                    .flatten()
//        }.flatten().toMutableList()


        val connections = klavenessPolygons.map { polygon ->
            polygon.polygonPoints
//                    .map { position -> GraphNode(klavenessPolygon.name, position) }
                    .map { position -> sanitizedGroupedPoints.getNodeWithPosition(position) }
                    .zipWithNext()
                    .map { GraphEdge(it.first, it.second, it.first.position.distanceFrom(it.second.position).toInt()) }
                    .map {
                        val res = it.splitInTwo()
//                        klavenessPolygon.graphNodes.add(res.first().fromNode)
                        polygon.graphNodes.add(res.first().toNode) // This is the middle node
                        res
                    }
                    .flatten()
        }
                .flatten()
                .toMutableList()

        println("1) Generated ${connections.size} connections")


        // 2) Find center in each klavenessPolygon and connect with klavenessPolygon
        val centersNodes = klavenessPolygons.map { polygon ->
            val centerPos = polygon.getCenterPosition()
            assert(centerPos.lat in (-90.0..90.0) && centerPos.lon in (-180.0..180.0)) { "Center position invalid: $centerPos, ${polygon.name}" }

//            val centerPos2 = centerPos.flip()
            val centerNode = Node(centerPos, polygon.name, isPort = false, geohash = GeoHash.withBitPrecision(centerPos.lat, centerPos.lon, 64))
            val newNodes = mutableListOf<ShippingNode>()
            polygon.graphNodes.forEach {
                val connection = GraphEdge(centerNode, it, centerNode.position.distanceFrom(it.position).toInt()).splitInTwo()
//                klavenessPolygon.graphNodes.add(split.first().fromNode)
                newNodes.add(connection.first().toNode) // This is the middle node
                connections.addAll(connection)
            }
            polygon.graphNodes.add(centerNode)
            polygon.graphNodes.addAll(newNodes)
            polygon.middleNodes.addAll(newNodes)
            centerNode
        }

        println("2) Generated ${centersNodes.size} center nodes")


        // 2.5) Add connection between the nodes in the middle layer of each klavenessPolygon

        klavenessPolygons.forEach { polygon ->
            polygon.middleNodes.add(polygon.middleNodes.first())
            polygon.middleNodes.zipWithNext { a, b ->
                connections.add(GraphEdge(a, b, a.position.distanceFrom(b.position).toInt()))
            }
        }


        println("3) Generated connections from each center node to corresponding klavenessPolygon nodes. Now, a total of ${connections.size} connections")


        // 4) Create connections from ports to klavenessPolygon edges

        val portPoints = ports.map {
            val polygon = it.position.getCorrespondingPolygon(klavenessPolygons)
            assert(polygon is KlavenessPolygon)
            GraphPortNode(it.name, it.position, polygon!!, it.portId)
        }

        portPoints.forEach { port ->
            port.klavenessPolygon.graphNodes.forEach {
                val connection = GraphEdge(port, it, port.position.distanceFrom(it.position).toInt())
                connections.add(connection)
            }
        }

        println("4) Generated connections from ports to nodes in klavenessPolygon that port belongs to. Now, a total of ${connections.size} connections")


//        val newConnections = connections.filterIndexed {i, edge ->
//            connections.subList(i, connections.size).contains(edge)
//        }
//
//        println("Size of newConnections: ${newConnections.size}")


        println("5) Remove points on land and edges connected to these ports.")


        val filteredNodes = klavenessPolygons
                .map { it.graphNodes }
                .flatten()
                .filter { it notIn worldCountries }

        val filteredConnections = connections.filter {
            ((it.fromNode !is GraphPortNode && it.fromNode notIn worldCountries) && (it.toNode !is GraphPortNode && it.toNode notIn worldCountries)) ||
                    ((it.fromNode is GraphPortNode) && (it.toNode !is GraphPortNode && it.toNode notIn worldCountries)) ||
                    ((it.fromNode !is GraphPortNode && it.fromNode notIn worldCountries) && (it.toNode is GraphPortNode ))
        }


        val allNodes = portPoints + klavenessPolygons.map { it.graphNodes }.flatten()

//        val graph = Graph(connections, allNodes)
        val graph = Graph(filteredConnections, filteredNodes + portPoints)

        return graph

    }
}



