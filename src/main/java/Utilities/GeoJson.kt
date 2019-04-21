package Utilities

import Models.GraphEdge
import Models.GraphNode
import Models.Position
import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Polygon
import java.io.FileReader

interface GeoJsonInterface {
    fun toGeoJsonObject(): String
}

infix fun Coordinate.notIn(countries: List<Polygon>) = !countries.any { it.contains(GeometryFactory().createPoint(this)) }

infix fun GraphNode.notIn(countries: List<Polygon>) = Coordinate(this.position.lon, this.position.lat) notIn countries
//        !countries.any { it.contains(GeometryFactory().createPoint(Coordinate(this.position.lon, this.position.lat))) }


data class ShippingGeoJsonObject(val type: String,
                                 val features: List<GeoJsonFeature>) {

    /**
     * If geojson object only contains one polygon, this function returns a list
     * containing one element of type polygon
     *
     * If geojson object containts many polygons (multipolygon), a list of polygons
     * will be returned.
     */
    fun getAllPolygonsLocationTech(): List<Polygon> {
        return features.map {
            it.getPolygonPositions()
                    .map { polygon ->
                        val coords = polygon.map { pos -> Coordinate(pos.lat, pos.lon) } as MutableList
//                        val coords = polygon.map { pos -> Coordinate(pos.lon, pos.lat) } as MutableList
                        coords.add(coords[0]) //To make a closed ring
                        GeometryFactory()
                                .createPolygon(coords.toTypedArray())
                    }
        }.flatten()
    }

}


data class GeoJsonFeature(val type: String,
                          val properties: Map<String, String>,
                          val geometry: GeoJsonGeometry) {

    fun getPolygonPositions(): List<List<Position>> {
        val positions = if (geometry.type == "Polygon") {
            val poss = geometry.coordinates.first().map {
                it as List<Double>
                Position(it[0], it[1])
            }
            listOf<List<Position>>(poss)
        } else { //MultiPolygon
            geometry.coordinates.map { pol ->
                pol.map {
                    it as List<List<Double>>
                    it.map { pos ->
                        Position(pos[0], pos[1])
                    }
                }.flatten()
            }
        }
        return positions
    }
}


class GeoJsonGeometry(val type: String,
                      coordinates: List<List<Any>>) {

    val coordinates: List<List<Any>> = coordinates
        get() = when (type) {
            "Polygon" -> field as List<List<Double>>
            "MultiPolygon" -> field as List<List<List<Double>>>
            else -> field as List<List<Double>>
        }
}


enum class GeoJsonType(val typeName: String) {
    LINE_STRING("LineString"),
    POINT("Point"),
    POLYGON("Polygon"),
    MULTIPOLYGON("MultiPolygon")
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
        "coordinates":
            COORDINATES

      },
      "properties": {
        PROPERTIES
      }
    }
    """.trimIndent()

    fun getGeoJson(elements: String, properties: String = "") = templateGeoJsonBase
            .replace("ELEMENTS", elements)
            .replace("PROPERTIES", properties)

    fun createGeoJsonElement(type: GeoJsonType, coordinates: String, props: String = "") = templateGeoJsonElement
            .replace("GEOJSONTYPE", type.typeName)
            .replace("COORDINATES", coordinates)
            .replace("PROPERTIES", props)


    fun pointsToGeoJson(points: List<GraphNode>): String {
        val elements = points.map { point ->
            createGeoJsonElement(GeoJsonType.POINT, "[${point.position.lon}, ${point.position.lat}]")
        }
        val elementsAsString = elements.joinToString(separator = ",")
        return getGeoJson(elementsAsString)
    }

    fun pointToGeoJson(point: GraphNode) = createGeoJsonElement(GeoJsonType.POINT, "[${point.position.lon}, ${point.position.lat}]")


    fun pathToGeoJson(path: List<GraphEdge>, color: String = "#000000", thickness: String = "5", label: String = "no-label"): String {
        val coords = path.map { item ->
            "[${item.fromNode.position.lon}, ${item.fromNode.position.lat}],[${item.toNode.position.lon}, ${item.toNode.position.lat}]"
        }.toString()

        val props = """
                "stroke": "$color",
                "stroke-width": "$thickness",
                "stroke-opacity": 1,
                "label": "$label"
        """.trimIndent()

        val element = createGeoJsonElement(GeoJsonType.LINE_STRING, coords, props)

        return getGeoJson(element)
    }

    fun edgesToGeoJson(edges: List<GraphEdge>): String {
        val props = """
        "stroke": "#555555",
        "stroke-width": 2,
        "stroke-opacity": 1,
        "marker-color": "#36c136",
        "fill-opacity": 0.1
        """.trimIndent()

        val elements = mutableListOf<String>()

        for (edge in edges) {
            val element = createGeoJsonElement(GeoJsonType.LINE_STRING, "[[${edge.fromNode.position.lon}, ${edge.fromNode.position.lat}],[${edge.toNode.position.lon}, ${edge.toNode.position.lat}]]", props)
            elements.add(element)
        }

        val geoJsonString = elements.joinToString(separator = ",")
        return getGeoJson(geoJsonString)

    }


    fun readWorldCountriesGeoJSON(path: String): List<Polygon> {
        val gson = Gson()
        val reader = JsonReader(FileReader(path))
        val data = gson.fromJson<ShippingGeoJsonObject>(reader, ShippingGeoJsonObject::class.java)
        val worldCountryPolygons = data.getAllPolygonsLocationTech()
        return worldCountryPolygons
    }


    fun readSinglePolygonGeoJson(path: String): Polygon {
        val gson = Gson()
        val reader = JsonReader(FileReader(path))
        val data = gson.fromJson<ShippingGeoJsonObject>(reader, ShippingGeoJsonObject::class.java)
        return data.getAllPolygonsLocationTech().first()
    }

}