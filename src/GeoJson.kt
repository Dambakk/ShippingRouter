interface GeoJsonInterface {
    fun toGeoJsonObject()
}

enum class GeoJsonType(val typeName: String) {
    LINE_STRING("LineString"),
    POINT("Point"),
    POLYGON("Polygon")
}

object GeoJson {

    private val templateGeoJsonBase = """
{
  "type": "FeatureCollection",
  "features": [
    ELEMENTS
  ]
}
    """.trimIndent()

    private val templateGeoJsonElement = """
    {
      "type": "Feature",
      "geometry": {
        "type": "GEOJSONTYPE",
        "coordinates": [
            COORDINATES
        ]
      },
      "properties": {
        PROPERTIES
      }
    }
    """.trimIndent()

    fun getGeoJsonBaseTemplate() = templateGeoJsonBase

    fun getGeoJson(elements: String, properties: String) = templateGeoJsonBase
            .replace("ELEMENTS", elements)
            .replace("PROPERTIES", properties)

    fun createGeoJsonElement(type: GeoJsonType, coordinates: String) = templateGeoJsonElement
            .replace("GEOJSONTYPE", type.typeName)
            .replace("COORDINATES", coordinates)


    fun pathToGeoJson(path: List<ShippingEdge>): String {
        val coords = path.map { item ->
            "[${item.fromNode.position.lat}, ${item.fromNode.position.lon}],[${item.toNode.position.lat}, ${item.toNode.position.lon}]"
        }.toString()

        val element = createGeoJsonElement(GeoJsonType.LINE_STRING, coords)

        return getGeoJson(element, "")
    }
}