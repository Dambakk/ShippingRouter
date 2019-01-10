object GraphUtils {


    fun createGraph(polygons: List<Polygon>) {

        // 1) Generate outer connection on each polygon:

        val connections = polygons.map { polygon ->
            polygon.polygonPoints
                    .map { position -> GraphNode(polygon.name, position) }
                    .zipWithNext()
                    .map { GraphEdge(it.first, it.second, it.first.position.distanceFrom(it.second.position).toInt()) }
        }.flatten().toMutableList()

        println("1) Generated ${connections.size} connections")


        val size = connections.size


        // 2) Find center in each polygon
        val centersNodes = polygons.map { polygon ->
            GraphNode(polygon.name, polygon.getCenterPosition())
        }

        println("2) Generated ${centersNodes.size} center nodes")





        // 3) Connect center with each node in polygon

        centersNodes.forEach { center ->
            val polygonNodes = polygons.find { it.name == center.name }
                    ?.polygonPoints?.map { position -> GraphNode(center.name, position) }
            val centerConnections = polygonNodes?.map { GraphEdge(center, it, center.position.distanceFrom(it.position).toInt()) }

            if (centerConnections != null) {
                connections.addAll(centerConnections)
            }
        }

        println("3) Generated connections from each center node to corresponding polygon nodes. Now, a total of ${connections.size} connections")




        // 4) Generate inbetween nodes




        // 5) Connect inbetween nodes with surroundings


    }
}


