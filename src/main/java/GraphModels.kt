import com.google.gson.annotations.SerializedName

/**
 *  Representation of a graph.
 */
interface ShippingGraph {
    val edges: List<ShippingEdge>
    val nodes: List<ShippingNode>
}

/**
 * An edge is a connection between two nodes in a graph.
 */
interface ShippingEdge {
    var fromNode: ShippingNode
    var toNode: ShippingNode
    val cost: Int
}

/**
 * A node in a graph. Has a label/name and a position.
 */
interface ShippingNode {
    val name: String
    val position: Position
}


class Graph(
        override val edges: List<ShippingEdge>,
        override val nodes: List<ShippingNode>
) : ShippingGraph



class GraphEdge (
        override var fromNode: ShippingNode,
        override var toNode: ShippingNode,
        override val cost: Int
) : ShippingEdge {
    override fun equals(other: Any?): Boolean {
        return other is GraphEdge &&
                (this.fromNode == other.fromNode && this.toNode == other.toNode ||
                this.fromNode == other.toNode && this.toNode == other.fromNode)
    }

    override fun toString() = "[$fromNode - $toNode]"
}

class GraphPortNode (@SerializedName("name2") override val name: String,
                     @SerializedName("position2") override val position: Position,
                     val klavenessPolygon: KlavenessPolygon,
                     val portId: String) : GraphNode(name, position) {
    override fun toString() = super.toString().replace("False", "True")
}

open class GraphNode (
        override val name: String,
        override val position: Position
) : ShippingNode, GeoJsonInterface {

    override fun toGeoJsonObject() = GeoJson.createGeoJsonElement(GeoJsonType.POINT, "[${position.lat}, ${position.lon}]")


    override fun equals(other: Any?): Boolean {
        return other is GraphNode && (this.position == other.position)
    }

    override fun toString() = "($name $position isPort=False)"

}