package Utilities

import Models.*
import ch.hsr.geohash.GeoHash
import me.tongfei.progressbar.ProgressBar
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Polygon

object GraphUtils {


    fun createLatLonGraph(ports: List<GraphPortNode>, initialPrecision: Int, worldCountries: List<Polygon>): Graph {
        assert(initialPrecision in 0..100) { "Invalid precision" }

        val step = 1 // Can be 1

        val nodes = mutableListOf<GraphNode>()
        var connections = mutableListOf<GraphEdge>()
        val nodesMap = mutableMapOf<Pair<Int, Int>, GraphNode>()

        val pb = ProgressBar("Creating nodes...", 119 * 360)

        for (i in 31..150 step step) {
            val lat = i - 90

            for (j in 0..360 step step) {
                val lon = j - 180

                pb.step()

                val node = GraphNode("($lat, $lon)", Position(lat.toDouble(), lon.toDouble()))

                if (node notIn worldCountries) {
                    nodes.add(node)
                    nodesMap[Pair(lat, lon)] = node
                }

                if (i > 0) {
                    val prev = nodesMap[Pair(lat - step, lon)]
                    if (prev != null) {
                        val conn = GraphEdge(prev, node, prev.position.distanceFrom(node.position).toInt())
                        connections.add(conn)
                    }
                }

                if (j > 0) {
                    val prev = nodesMap[Pair(lat, lon - step)]
                    if (prev != null) {
                        val conn = GraphEdge(prev, node, prev.position.distanceFrom(node.position).toInt())
                        connections.add(conn)
                    }
                }

                if (i > 0 && j > 0) {
                    val prev = nodesMap[Pair(lat - step, lon - step)]
                    if (prev != null) {
                        val conn = GraphEdge(prev, node, prev.position.distanceFrom(node.position).toInt())
                        connections.add(conn)
                    }
                }

                if (i < 180 && j < 360) {
                    val prev = nodesMap[Pair(lat - step, lon + step)]
                    if (prev != null) {
                        val conn = GraphEdge(prev, node, prev.position.distanceFrom(node.position).toInt())
                        connections.add(conn)
                    }
                }
            }
        }

        pb.close()

        Logger.log("Got ${nodes.size} nodes")
        val pointsAsGeoJson = GeoJson.pointsToGeoJson(nodes)

        Logger.log("Num generated connections: ${connections.size}")
        connections = connections.removeEdgesOnLand(worldCountries)
        Logger.log("Num filtered connections: ${connections.size}")

        val portPoints = ports.map {
            GraphPortNode(it.name, it.position, portId = it.portId)
        }

        nodes.addAll(portPoints)

        //TODO: Move this part to below the filtering of illage edges?
        var counter = 0
        portPoints.forEach { port ->
            val portLat = (((port.position.lat.toInt()) / step) * step).toInt() // Round off to match graph grid
            val portLon = (((port.position.lon.toInt()) / step) * step).toInt() // Round off to match graph grid

            assert(portLat % step == 0 && portLon % step == 0) { "Port position is not part of graph grid" }
            nodesMap[Pair(portLat, portLon)]?.run {
                val conn = GraphEdge(this, port, port.position.distanceFrom(port.position).toInt())
                connections.add(conn)
                counter++
            }

            for (t in 1..2) {

                nodesMap[Pair(portLat - step * t, portLon)]?.run {
                    val conn = GraphEdge(this, port, port.position.distanceFrom(port.position).toInt())
                    connections.add(conn)
                    counter++
                }
                nodesMap[Pair(portLat, portLon - step * t)]?.run {
                    val conn = GraphEdge(this, port, port.position.distanceFrom(port.position).toInt())
                    connections.add(conn)
                    counter++
                }
                nodesMap[Pair(portLat - step * t, portLon - step * t)]?.run {
                    val conn = GraphEdge(this, port, port.position.distanceFrom(port.position).toInt())
                    connections.add(conn)
                    counter++
                }



                nodesMap[Pair(portLat + step * t, portLon)]?.run {
                    val conn = GraphEdge(this, port, port.position.distanceFrom(port.position).toInt())
                    connections.add(conn)
                    counter++
                }
                nodesMap[Pair(portLat, portLon + step * t)]?.run {
                    val conn = GraphEdge(this, port, port.position.distanceFrom(port.position).toInt())
                    connections.add(conn)
                    counter++
                }
                nodesMap[Pair(portLat + step * t, portLon + step * t)]?.run {
                    val conn = GraphEdge(this, port, port.position.distanceFrom(port.position).toInt())
                    connections.add(conn)
                    counter++
                }
            }

        }
        Logger.log("Added $counter connections to ports")
        Logger.log("Now, a total of ${connections.size} connections")


        Logger.log("Number of connections before making the world round: ${connections.size} connections")

        val leftSideOfWorld = nodes.filter { it.position.lon.toInt() == -179 }
        val rightSideOfWorld = nodes.filter { it.position.lon.toInt() == 179 }

        Logger.log("Number on left side: ${leftSideOfWorld.size}", LogType.DEBUG)
        Logger.log("Number on right side: ${rightSideOfWorld.size}", LogType.DEBUG)

//        for (t in 0 until leftSideOfWorld.size) {
//            val a = leftSideOfWorld[t]
//            val b = rightSideOfWorld[t]
//            assert(a.position.lat == b.position.lat) { "Not equal latitude when connecting the round world" }
//            val conn = listOf(GraphEdge(a, b, 0), GraphEdge(b, a, 0)) //Distance is 0
//            connections.addAll(conn)
//        }

        val a = leftSideOfWorld[35]
        val b = rightSideOfWorld[35]
        assert(a.position.lat == b.position.lat) { "Not equal latitude when connecting the round world" }
        val conns = listOf(GraphEdge(a, b, 0), GraphEdge(b, a, 0)) //Distance is 0
        connections.addAll(conns)

//        rightSideOfWorld.zip(leftSideOfWorld).forEach {
//            val conn = GraphEdge(it.first, it.second, it.first.position.distanceFrom(it.second.position).toInt()) //Distance is 0
//            val conn = listOf(GraphEdge(it.first, it.second, 0), GraphEdge(it.second, it.first, 0)) //Distance is 0
//            connections.addAll(conn)
//        }


        //Hard test: Removing all nodes and connections in the middle of the world:
        nodes.removeIf { it.position.lon.toInt() in -26..100 }
        connections.removeIf { it.fromNode.position.lon.toInt() in -26..100 || it.toNode.position.lon.toInt() in -26..100 }


        val start = nodes.find { it is GraphPortNode && it.portId == Config.startPortId }!!
        val goal = nodes.find { it is GraphPortNode && it.portId == Config.goalPortId }!!

//        val conn = GraphEdge(start, goal, 100)
//        connections.add(conn)

        val geoJson = GeoJson.edgesToGeoJson(connections)
        Logger.log("Number of connections after making the world round: ${connections.size} connections")


        val directedConnections = connections
                .map { listOf(it, it.createFlipped()) }
                .flatten()

        Logger.log("(${directedConnections.size} directed connections)")


        return Graph(directedConnections, nodes + ports)
    }

    fun createKlavenessGraph(klavenessPolygons: List<KlavenessPolygon>, ports: List<GraphPortNode>, sanitizedGroupedPoints: Set<GraphNode>, worldCountries: List<Polygon>): Graph {


        // 1) Generate outer connection on each klavenessPolygon:

        val connections = klavenessPolygons.map { polygon ->
            polygon.polygonPoints
                    .asSequence()
                    .map { position -> sanitizedGroupedPoints.getNodeWithPosition(position) }
                    .zipWithNext()
                    .map { GraphEdge(it.first, it.second, it.first.position.distanceFrom(it.second.position).toInt()) }
                    .map {
                        val res = it.splitInTwo()
                        polygon.graphNodes.add(res.first().toNode) // This is the middle node
                        res
                    }
                    .flatten()
                    .toList()
        }
                .flatten()
                .toMutableList()

        println("1) Generated ${connections.size} connections")


        // 2) Find center in each klavenessPolygon and connect with klavenessPolygon
        val centersNodes = klavenessPolygons.map { polygon ->
            val centerPos = polygon.getCenterPosition()
            assert(centerPos.lat in (-90.0..90.0) && centerPos.lon in (-180.0..180.0)) { "Center position invalid: $centerPos, ${polygon.name}" }

            val centerNode = GraphNode(polygon.name, centerPos, geohash = GeoHash.withBitPrecision(centerPos.lat, centerPos.lon, 64))
            val newNodes = mutableListOf<GraphNode>()
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
        }

        klavenessPolygons.forEach { polygon ->
            polygon.middleNodes.zipWithNext { a: GraphNode, b: GraphNode ->
                connections.add(GraphEdge(a, b, a.position.distanceFrom(b.position).toInt()))
            }
        }


        /**
         *  This will generate connections between the middel nodes and the neighbouring outer nodes.
         *
         *  However, it will generate many connections that travels across land. I need to remove such connections
         *  before i can enable this again.
         */
        klavenessPolygons.forEach { polygon ->
            polygon.middleNodes.forEach {
                val outerNode = connections.find { c -> c.fromNode == it }!!.toNode
                val neighbours = connections.filter { c ->
                    (c.fromNode == outerNode && c.toNode != it) ||
                            (c.toNode == outerNode && c.fromNode != it)
                }

                val newCs = neighbours.map { n ->
                    val t = if (n.fromNode == it) n.toNode else n.fromNode
                    Models.GraphEdge(it, t, it.position.distanceFrom(t.position).toInt())
                }
                connections.addAll(newCs)
            }
        }


        println("3) Generated connections from each center node to corresponding klavenessPolygon nodes. Now, a total of ${connections.size} connections")


        // 4) Create connections from ports to klavenessPolygon edges

        //TODO: Move this part to below the filtering of illage edges?
        val portPoints = ports.map {
            val polygon = it.position.getCorrespondingPolygon(klavenessPolygons)
            assert(polygon is KlavenessPolygon)
            GraphPortNode(it.name, it.position, polygon!!, it.portId)
        }
        //TODO: Move this part to below the filtering of illage edges?
        portPoints.forEach { port ->
            port.klavenessPolygon!!.graphNodes.forEach {
                val connection = GraphEdge(port, it, port.position.distanceFrom(it.position).toInt())
                connections.add(connection)
            }
        }

        println("4) Generated connections from ports to nodes in klavenessPolygon that port belongs to. Now, a total of ${connections.size} connections")


        println("5) Remove points on land and edges connected to these ports.")


        val filteredNodes = klavenessPolygons
                .map { it.graphNodes }
                .flatten()
                .filter { it notIn worldCountries }

        val groupedPoints = filteredNodes
                .groupBy { GeoHash.withBitPrecision(it.position.lat, it.position.lon, 16) }
                .map { (key, list) ->
                    val newName = list.map { it.name }.toSet().joinToString(separator = "+")
                    GraphNode(newName, list.first().position, GeoHash.withBitPrecision(list.first().position.lat, list.first().position.lon, 16))
                }
                .toSet()
                .toList()

        // Make sure all connections use the same node (grouped point)
        connections.forEach {
            it.fromNode = groupedPoints.find { n -> n.geohash.contains(it.fromNode.geohash.point) } ?: it.fromNode
            it.toNode = groupedPoints.find { n -> n.geohash.contains(it.toNode.geohash.point) } ?: it.toNode
        }


        val filteredConnections = connections.removeEdgesOnLand(worldCountries)

//        val filteredConnections = connections.filter {
//            ((it.fromNode !is GraphPortNode && it.fromNode notIn worldCountries) && (it.toNode !is GraphPortNode && it.toNode notIn worldCountries)) ||
//                    ((it.fromNode is GraphPortNode) && (it.toNode !is GraphPortNode && it.toNode notIn worldCountries)) ||
//                    ((it.fromNode !is GraphPortNode && it.fromNode notIn worldCountries) && (it.toNode is GraphPortNode))
//        }
//                .filterNot { edge ->
//                                        //val line = GeometryFactory().createLineString(listOf(Coordinate(edge.fromNode.position.lat, edge.fromNode.position.lon), Coordinate(edge.toNode.position.lat, edge.toNode.position.lon)).toTypedArray())
//                    if (edge.fromNode is GraphPortNode || edge.toNode is GraphPortNode) {
//                        false
//                    } else {
//                        val line = GeometryFactory().createLineString(listOf(Coordinate(edge.fromNode.position.lon, edge.fromNode.position.lat), Coordinate(edge.toNode.position.lon, edge.toNode.position.lat)).toTypedArray())
//                        worldCountries.any { it.crosses(line) }
//                    }
//                }
//                .toMutableList()

        val filteredConnectionsAsGeoJson = GeoJson.edgesToGeoJson(filteredConnections)


        // Make the connections directed
        val directedConnections = filteredConnections
                .map { listOf(it, it.createFlipped()) }
                .flatten()

        println("6) Remove edges that connects to nodes on land and make graph directed. Now, a total of ${directedConnections.size} connections")

//        val allNodes = portPoints + klavenessPolygons.map { it.graphNodes }.flatten()

//        val graph = Models.Graph(connections, allNodes)
//        val graph = Models.Graph(filteredConnections, filteredNodes + portPoints)
        val graph = Graph(directedConnections, groupedPoints + portPoints)

        return graph

    }


    fun MutableList<GraphEdge>.removeEdgesOnLand(worldCountries: List<Polygon>): MutableList<GraphEdge> = this
            .filter {
                ((it.fromNode !is GraphPortNode && it.fromNode notIn worldCountries) && (it.toNode !is GraphPortNode && it.toNode notIn worldCountries)) ||
                        ((it.fromNode is GraphPortNode) && (it.toNode !is GraphPortNode && it.toNode notIn worldCountries)) ||
                        ((it.fromNode !is GraphPortNode && it.fromNode notIn worldCountries) && (it.toNode is GraphPortNode))
            }
            .filterNot { edge ->
                if (edge.fromNode is GraphPortNode || edge.toNode is GraphPortNode) {
                    false
                } else {
                    val line = GeometryFactory().createLineString(listOf(Coordinate(edge.fromNode.position.lon, edge.fromNode.position.lat), Coordinate(edge.toNode.position.lon, edge.toNode.position.lat)).toTypedArray())
                    worldCountries.any { it.crosses(line) }
                }
            }
            .toMutableList()
}



