import ch.hsr.geohash.GeoHash


class Graph(
        val edges: List<GraphEdge>,
        val nodes: List<GraphNode>
)

class GraphEdge (
        var fromNode: GraphNode,
        var toNode: GraphNode,
        val cost: Int
)  {
    override fun equals(other: Any?): Boolean {
        return other is GraphEdge &&
                (this.fromNode == other.fromNode && this.toNode == other.toNode ||
                this.fromNode == other.toNode && this.toNode == other.fromNode)
    }

    override fun toString() = "[$fromNode - $toNode]"
}

class GraphPortNode (name: String,
                     position: Position,
                     val klavenessPolygon: KlavenessPolygon?,
                     val portId: String
) : GraphNode(name, position, GeoHash.withBitPrecision(position.lat, position.lon, 64)) {

    override fun toString() = super.toString().replace("False", "True")
}

open class GraphNode (
        val name: String,
        val position: Position,
        val geohash: GeoHash = GeoHash.withBitPrecision(position.lat, position.lon, 64)
): GeoJsonInterface {

    init {
        assert(position.lat in (-90.0..90.0) && position.lon in (-180.0..180.0)) {"Port position invalid: $position, $name"}
    }

    override fun toGeoJsonObject() = GeoJson.createGeoJsonElement(GeoJsonType.POINT, "[${position.lat}, ${position.lon}]")


    override fun equals(other: Any?): Boolean {
        return other is GraphNode && (this.position == other.position)
    }

    override fun toString() = "($name $position isPort=False)"

}