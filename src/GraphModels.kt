

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
    val fromNode: ShippingNode
    val toNode: ShippingNode
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
        override val fromNode: ShippingNode,
        override val toNode: ShippingNode,
        override val cost: Int
) : ShippingEdge {

}


class GraphNode (
        override val name: String,
        override val position: Position
) : ShippingNode {

}