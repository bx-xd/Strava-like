package io.github.bx_xd.velotrack.utils

import io.github.bx_xd.velotrack.model.GpsPoint
import io.github.bx_xd.velotrack.model.Segment
import io.github.bx_xd.velotrack.model.SegmentEffort

object SegmentMatcher {

    private const val MATCH_RADIUS_M = 40.0

    fun match(
        activityId: String,
        activityDate: Long,
        points: List<GpsPoint>,
        segments: List<Segment>
    ): List<SegmentEffort> {
        if (points.size < 10) return emptyList()
        return segments.mapNotNull { seg ->
            if (seg.startLat == 0.0 && seg.startLng == 0.0) null  // old segment, no coords
            else matchOne(activityId, activityDate, points, seg)
        }
    }

    private fun matchOne(
        activityId: String,
        activityDate: Long,
        points: List<GpsPoint>,
        segment: Segment
    ): SegmentEffort? {
        // Closest point to segment start
        var minStartDist = Double.MAX_VALUE
        var startIdx = -1
        points.forEachIndexed { i, p ->
            val d = haversine(p.lat, p.lng, segment.startLat, segment.startLng) * 1000
            if (d < minStartDist) { minStartDist = d; startIdx = i }
        }
        if (minStartDist > MATCH_RADIUS_M) return null

        // Closest point to segment end, strictly after startIdx
        var minEndDist = Double.MAX_VALUE
        var endIdx = -1
        for (i in (startIdx + 3) until points.size) {
            val d = haversine(points[i].lat, points[i].lng, segment.endLat, segment.endLng) * 1000
            if (d < minEndDist) { minEndDist = d; endIdx = i }
        }
        if (minEndDist > MATCH_RADIUS_M || endIdx <= startIdx) return null

        val slice = points.subList(startIdx, endIdx + 1)
        val durationSecs = ((slice.last().ts - slice.first().ts) / 1000).toInt()
        if (durationSecs <= 0) return null

        val distKm = totalDistanceKm(slice)
        val avgSpeedKmh = distKm / (durationSecs / 3600.0)

        return SegmentEffort(
            id           = "${segment.id}_$activityId",
            segmentId    = segment.id,
            segmentName  = segment.name,
            activityId   = activityId,
            startIndex   = startIdx,
            endIndex     = endIdx,
            durationSecs = durationSecs,
            distKm       = distKm,
            avgSpeedKmh  = avgSpeedKmh,
            date         = activityDate
        )
    }
}
