package CostFunctions

import Models.GraphEdge
import Models.GraphNode


class PolygonCost : CostFunction {
    override var weight: Float
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    constructor(weight: Float, geoJsonFilePath: String) {
        this.weight = weight

    }

    override fun getCost(node: GraphNode) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getCost(edge: GraphEdge) {
        val a = getCost(edge.fromNode)
        val b = getCost(edge.toNode)

    }

}