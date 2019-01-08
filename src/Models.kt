import javafx.geometry.Pos

fun String.toInt() = if (this.isEmpty()) 0 else this.toInt(10)

fun String.toFloat() = if (this.isEmpty()) 0.0f else this.toFloatOrNull()


data class TradePattern (
      val id: String,
      val imo: String,
      val from: String,
      val fromEta: String,
      val fromEtaTimestamp: String,
      val fromEts: String,
      val fromEtsTimestamp: String,
      val to: String,
      val toEta: String,
      val toEtaTimestamp: String,
      val toEts: String,
      val toEtsTimestamp: String,
      val hoursBetweenFromEtsAndToEtaDepDest: Int,
      val atSea: Int,
      val inAnchorageBeforeEnteringDeparture: Int,
      val inDeparturePort: Int,
      val inDepartureBerth: Int,
      val inAnchorageLeavingDeparture: Int,
      val transitAnchorageTime: Int,
      val inAnchorageBeforeEnteringDestinationPort: Int,
      val inDestinationPort: Int,
      val inDestinationBerth: Int,
      val portPolygonsVisited: Int,
      val berthPolygonsVisited: Int,
      val anchoragePolygonsVisited: Int,
      val destinationComponent: String,
      val query: String,
      val approved: Int,
      val approvedBy: String,
      val approvedDateTime: String,
      val metricScore: Float?,
      val pathScore: Float?,
      val disapproved: Int,
      val disapprovedBy: String,
      val disapprovedDateTime: String,
      val duplicatedId: String
) : ShippingObject {
    constructor(list: List<String>) : this(
            list[0],
            list[1],
            list[2],
            list[3],
            list[4],
            list[5],
            list[6],
            list[7],
            list[8],
            list[9],
            list[10],
            list[11],
            list[12].toInt(),
            list[13].toInt(),
            list[14].toInt(),
            list[15].toInt(),
            list[16].toInt(),
            list[17].toInt(),
            list[18].toInt(),
            list[19].toInt(),
            list[20].toInt(),
            list[21].toInt(),
            list[22].toInt(),
            list[23].toInt(),
            list[24].toInt(),
            list[25],
            list[26],
            list[27].toInt(),
            list[28],
            list[29],
            list[30].toFloat(),
            list[31].toFloat(),
            list[32].toInt(),
            list[33],
            list[34],
            list[35]
    )

    fun getFromAndTo(): Pair<String, String>  = this.from to this.to

    fun getFromAndToAlphabetical(): Pair<String, String> {
        val (start, end) = getFromAndTo()
        return if (start < end) (start to end) else (end to start)
    }
}

data class Position(val lat: Double, val lon: Double)

data class Port (
        val portId: String,
        val portName: String,
        val position: Position,
        val unicode: String,
        val startTime: String,
        val endTime: String,
        val timezone: String,
        val deleted: Boolean,
        val replacementPort: String,
        val countryCode: String
) {
    constructor(params: List<String>) : this (
        portId = params[0],
            portName = params[1],
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


data class Polygon (
        val name: String,
        val region: String,
        val subsection: String,
        val basin: String,
        val polygonEdges: List<Position>
) {
    constructor(params: List<String>) : this (
            name = params[0],
            region = params[1],
            subsection = params[2],
            basin = params[3],
            polygonEdges = params[4]
                    .split("((")[1]
                    .split("))")[0].trim()
                    .split(" ")
                    .map { it.removeSuffix(",").toDouble() }
                    .toPairedList()
    )

    companion object {
        fun List<Double>.toPairedList(): List<Position> {
            val res = mutableListOf<Position>()
            for (i in 0 until this.size step 2) {
                res.add(Position(this[i], this[i+1]))
            }
            return res
        }
    }
}


