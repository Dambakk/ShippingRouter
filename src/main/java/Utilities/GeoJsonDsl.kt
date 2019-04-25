package Utilities

import com.fasterxml.jackson.databind.ObjectMapper
import org.geojson.*

abstract class FeatureDsl {

    private val props = mutableMapOf<String, Any>()

    infix fun String.value(property: Any) {
        assert(!props.containsKey(this)) { "Properties already contains key $this" }
        props[this] = property
    }

    internal fun toGeoJson() =
            Feature().apply {
                geometry = geoJsonObject()
                properties = this@FeatureDsl.props
            }

    protected abstract fun geoJsonObject(): GeoJsonObject
}


class ShippingCoord {
    infix fun lng(lng: Double) = CoordLng(lng)
    infix fun lat(lat: Double) = CoordLat(lat)

    inner class CoordLng(private val lng: Double) {
        infix fun lat(lat: Double) =
                LngLatAlt(lng, lat)
    }

    inner class CoordLat(private val lat: Double) {
        infix fun lng(lng: Double) =
                LngLatAlt(lng, lat)
    }
}

class PointFeatureDsl() : FeatureDsl() {

    lateinit var lngLat: LngLatAlt

    constructor(lngLat: LngLatAlt) : this() {
        this.lngLat = lngLat
    }

    override fun geoJsonObject(): GeoJsonObject = Point(lngLat)
}

class LineStringDsl : FeatureDsl() {

    private val coordinates = mutableListOf<LngLatAlt>()

    val coord: ShippingCoord
        get() = ShippingCoord()

    fun add(lngLat: LngLatAlt) {
        coordinates.add(lngLat)
    }

    override fun geoJsonObject(): GeoJsonObject {
        assert(coordinates.size > 1) { "LineString must have at least two coordinates" }
        return LineString().apply {
            coordinates = this@LineStringDsl.coordinates
        }
    }
}

class PolygonDsl : FeatureDsl() {

    private val coordinates = mutableListOf<LngLatAlt>()

    val coord: ShippingCoord
        get() = ShippingCoord()

    fun add(lngLat: LngLatAlt) {
        coordinates.add(lngLat)
    }

    public override fun geoJsonObject(): GeoJsonObject {
        assert(coordinates.size > 2) { "Polygon must have at least three coordinates" }
        if (coordinates.first() != coordinates.last()) {
            coordinates.add(coordinates.first())
        }
        return Polygon().apply {
            coordinates = listOf(this@PolygonDsl.coordinates)
        }
    }
}

class MultiPolygonDsl : FeatureDsl() {

    private val polygons = mutableListOf<PolygonDsl>()

    override fun geoJsonObject(): GeoJsonObject {
        return MultiPolygon().apply {
            listOf(
                    polygons.forEach {
                        add(it.geoJsonObject() as Polygon)
                    }
            )
        }
    }
}

class FeatureCollectionDsl {

    private val features = mutableListOf<Feature>()

    private fun <T : FeatureDsl> add(feature: T, init: T.() -> Unit) {
        feature.init()
        features.add(feature.toGeoJson())
    }

    internal fun toGeoJson(): FeatureCollection =
            FeatureCollection().apply {
                addAll(this@FeatureCollectionDsl.features)
            }

    fun point(lng: Double, lat: Double, init: PointFeatureDsl.() -> Unit = {}): Unit =
            add(PointFeatureDsl(LngLatAlt(lng, lat)), init)

    fun lineString(init: LineStringDsl.() -> Unit): Unit =
            add(LineStringDsl(), init)

    fun polygon(init: PolygonDsl.() -> Unit): Unit =
            add(PolygonDsl(), init)

    fun multiPolygon(init: MultiPolygonDsl.() -> Unit): Unit =
            add(MultiPolygonDsl(), init)
}

fun featureCollection(init: FeatureCollectionDsl.() -> Unit): FeatureCollection =
        FeatureCollectionDsl()
                .apply(init)
                .toGeoJson()

fun FeatureCollection.toGeoJson() =
        ObjectMapper().writeValueAsString(this)

fun testGeoJsonCreator(): FeatureCollection {

    return featureCollection {
        point(69.0, 69.69) {
            "marker-color" value "#ff0000"
        }
        point(0.0, 0.0) {
            "name" value "Test"
            "marker-color" value "#363636"
        }
        lineString {
            add(coord lng 15.0 lat 69.0)
            add(coord lat 12.0 lng 71.0)
            "description" value "testLineString"
            "line-color" value "#F3F2F1"
            "distance" value 158
        }
        polygon {
            add(coord lat 69.0 lng 70.0)
            add(coord lat 68.0 lng 70.0)
            add(coord lat 67.0 lng 77.0)
            "description" value "test"
            "fill" value "#ff0000"
        }
        multiPolygon {
            polygon {
                add(coord lat 69.0 lng 70.0)
                add(coord lat 68.0 lng 70.0)
                add(coord lat 67.0 lng 77.0)
                "fill" value "#0000ff"
            }
            polygon {
                add(coord lat 29.0 lng 61.0)
                add(coord lat 28.0 lng 60.0)
                add(coord lat 27.0 lng 67.0)
            }
            polygon {
                add(coord lat 19.0 lng 50.0)
                add(coord lat 18.0 lng 50.0)
                add(coord lat 17.0 lng 57.0)
            }
            "description" value "TestCountryName"
            "fill" value "#00ff00"
            "area" value 6969

            "myKey" value 69
        }
    }
}
