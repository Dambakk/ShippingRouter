import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

// To parse the JSON, install Klaxon and do:
//
//   val geoJSONCountries = GeoJSONCountries.fromJson(jsonString)



private val klaxon = Gson()

data class GeoJSONCountries (
        val type: String,
        val features: List<Feature>
) {
    public fun toJson() = klaxon.toJson(this)

    companion object {
        public fun fromJson(json: String) = klaxon.fromJson<GeoJSONCountries>(json, GeoJSONCountries::class.java)
    }
}

data class Feature (
        val type: String,
        val properties: Properties,
        val geometry: Geometry
)

data class Geometry (
        val type: String,
        val coordinates: List<List<List<Double>>>
)

data class Properties (
        @SerializedName("ADMIN")
        val admin: String,

        @SerializedName("ISO_A3")
        val isoA3: String
)


/*
import com.beust.klaxon.*
import com.fasterxml.jackson.annotation.JsonValue
import com.google.gson.JsonArray

private fun <T> Klaxon.convert(k: kotlin.reflect.KClass<*>, fromJson: (JsonValue) -> T, toJson: (T) -> String, isUnion: Boolean = false) =
        this.converter(object: Converter {
            @Suppress("UNCHECKED_CAST")
            override fun toJson(value: Any)        = toJson(value as T)
            override fun fromJson(jv: JsonValue)   = fromJson(jv) as Any
            override fun canConvert(cls: Class<*>) = cls == k.java || (isUnion && cls.superclass == k.java)
        })

private val klaxon = Klaxon()
        .convert(Coordinate::class, { Coordinate.fromJson(it) }, { it.toJson() }, true)

data class GeoJSONCountries (
        val type: String,
        val features: List<Feature>
) {
    public fun toJson() = klaxon.toJsonString(this)

    companion object {
        public fun fromJson(json: String) = klaxon.parse<GeoJSONCountries>(json)
    }
}

data class Feature (
        val type: String,
        val properties: Properties,
        val geometry: Geometry
)

data class Geometry (
        val type: String,
        val coordinates: List<List<List<Coordinate>>>
)

sealed class Coordinate {
    class DoubleArrayValue(val value: List<Double>) : Coordinate()
    class DoubleValue(val value: Double)            : Coordinate()

    public fun toJson(): String = klaxon.toJsonString(when (this) {
        is DoubleArrayValue -> this.value
        is DoubleValue      -> this.value
    })

    companion object {
        public fun fromJson(jv: JsonValue): Coordinate = when (jv.inside) {
            is JsonArray<*> -> DoubleArrayValue(jv.array?.let { klaxon.parseFromJsonArray<Double>(it) }!!)
            is Double       -> DoubleValue(jv.double!!)
            else            -> throw IllegalArgumentException()
        }
    }
}

data class Properties (
        @Json(name = "ADMIN")
        val admin: String,

        @Json(name = "ISO_A3")
        val isoA3: String
)
*/