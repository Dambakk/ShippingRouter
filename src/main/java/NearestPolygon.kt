import org.geotools.data.collection.SpatialIndexFeatureCollection
import org.geotools.data.simple.SimpleFeatureCollection
import org.geotools.factory.CommonFactoryFinder
import org.geotools.geometry.jts.JTSFactoryFinder
import org.geotools.geometry.jts.ReferencedEnvelope
import org.locationtech.geomesa.utils.geohash.BoundingBox
import org.locationtech.jts.geom.*
import org.locationtech.jts.linearref.LocationIndexedLine
import org.opengis.feature.simple.SimpleFeature

class NearestPolygon(features: SimpleFeatureCollection) {
    val geometryFactory = JTSFactoryFinder.getGeometryFactory()
    private val index: SpatialIndexFeatureCollection
    var lastMatched: SimpleFeature? = null
        private set

    init {

        index = SpatialIndexFeatureCollection(features.schema)
        index.addAll(features)
    }


    fun isPointInsidePolygon(p: Point): Boolean {


        return false
    }


    fun findNearestPolygon(p: Point): Point? {
        val MAX_SEARCH_DISTANCE = index.bounds.getSpan(0)
        val coordinate = p.coordinate
        val search = ReferencedEnvelope(Envelope(coordinate),
                index.schema.coordinateReferenceSystem)
        search.expandBy(MAX_SEARCH_DISTANCE)
        /*val bbox = ff.bbox(ff.property(index.schema.geometryDescriptor.name), search as BoundingBox)
        val candidates = index.subCollection(bbox)

        var minDist = MAX_SEARCH_DISTANCE + 1.0e-6
        var minDistPoint: Coordinate? = null
        candidates.features().use { itr ->
            while (itr.hasNext()) {

                val feature = itr.next()
                val line = LocationIndexedLine((feature.defaultGeometry as MultiPolygon).boundary)
                val here = line.project(coordinate)
                val point = line.extractPoint(here)
                val dist = point.distance(coordinate)
                if (dist < minDist) {
                    minDist = dist
                    minDistPoint = point
                    lastMatched = feature
                }
            }
        }
        return if (minDistPoint == null) {
            gf.createPoint(null as Coordinate?)
        } else {
            gf.createPoint(minDistPoint)
        }*/
        return null
    }

    companion object {
        private val ff = CommonFactoryFinder.getFilterFactory2()
        private val gf = GeometryFactory()
    }
}