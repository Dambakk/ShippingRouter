package Models

import Utilities.flip
import java.io.Serializable

data class Position(val lat: Double, val lon: Double) : Serializable {
    override fun equals(other: Any?): Boolean {
        return other is Position &&
                this.lat == other.lat &&
                this.lon == other.lon
    }

    override fun toString() = "($lat,$lon)"
}

data class Port(
        val portId: String,
        val name: String,
        val position: Position,
        val unicode: String,
        val startTime: String,
        val endTime: String,
        val timezone: String,
        val deleted: Boolean,
        val replacementPort: String,
        val countryCode: String
) {
    constructor(params: List<String>) : this(
            portId = params[0],
            name = params[1],
            position = Position(params[2].toDouble(), params[3].toDouble()),
            unicode = params[4],
            startTime = params[5],
            endTime = params[6],
            timezone = params[7],
            deleted = params[8].toBoolean(),
            replacementPort = params[9],
            countryCode = params[10]
    )
}


data class KlavenessPolygon(
        val name: String,
        val region: String,
        val subsection: String,
        val basin: String,
        val polygonPoints: List<Position>,
        var graphNodes: MutableList<GraphNode>,
        var outerNodes: MutableList<GraphNode>,
        var middleNodes: MutableList<GraphNode>,
        var centerNode: GraphNode
) {
    constructor(params: List<String>) : this(
            name = params[0],
            region = params[1],
            subsection = params[2],
            basin = params[3],
            polygonPoints = params[4]
                    .split("((")[1]
                    .split("))")[0].trim()
                    .split(" ")
                    .map { it.removeSuffix(",").toDouble() }
                    .toPairedList()
                    .flitPositions(),
            graphNodes = mutableListOf(),
            outerNodes = mutableListOf(),
            middleNodes = mutableListOf(),
            centerNode = GraphNode("", Position(0.0, 0.0))
    ) {
        graphNodes = polygonPoints.map { GraphNode(name, it) }.toMutableList()

    }

    companion object {
        fun List<Double>.toPairedList(): List<Position> {
            val res = mutableListOf<Position>()
            for (i in 0 until this.size step 2) {
                res.add(Position(this[i], this[i + 1]))
            }
            return res
        }

        fun List<Position>.flitPositions(): List<Position> = this.map { it.flip() }
    }
}
