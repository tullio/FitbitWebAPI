package org.example
import io.circe._
import io.circe.generic.semiauto._
import io.circe.parser.decode

case class ActivityLog(
  activities: Array[Activities],
  pagination: Pagination
)
object ActivityLog:
    implicit val decoder: Decoder[ActivityLog] = deriveDecoder
    def apply(json: String) =
        println(decode[ActivityLog](json))
        decode[ActivityLog](json).right.get

case class Activities(
    activeDuration: Long,
    activeZoneMinutes: ActiveZoneMinutes,
    activityLevel: Array[ActivityLevel],
    activityName: String,
    activityTypeId: Long,
    averageHeartRate: Long,
    calories: Long,
    caloriesLink: String,
    distance: Option[Double],
    distanceUnit: Option[String],
    duration: Long,
    elevationGain: Double,
    hasActiveZoneMinutes: Boolean,
    heartRateLink: String,
    heartRateZones: Array[HeartRateZones],
    lastModified: String,
    logId: Long,
    logType: String,
    manualValuesSpecified: ManualValuesSpecified,
    originalDuration: Long,
    originalStartTime: String,
    pace: Option[Double],
    source: Option[Source],
    speed: Option[Double],
    startTime: String,
    steps: Option[Long],
    tcxLink: String
)

object Activities:
    implicit val decoder: Decoder[Activities] = deriveDecoder

case class Pagination(
    afterDate: String,
    limit: Long,
    next: String,
    offset: Long,
    previous: String,
    sort: String
)
object Pagination:
    implicit val decoder: Decoder[Pagination] = deriveDecoder

case class ActiveZoneMinutes(
    minutesInHeartRateZones: Array[MinutesInHeartRateZones],
    totalMinutes: Long
)
object ActiveZoneMinutes:
    implicit val decoder: Decoder[ActiveZoneMinutes] = deriveDecoder

case class MinutesInHeartRateZones(
    minuteMultiplier: Long,
    minutes: Long,
    order: Long,
    `type`: String,
    zoneName: String
)
object MinutesInHeartRateZones:
    implicit val decoder: Decoder[MinutesInHeartRateZones] = deriveDecoder


case class ActivityLevel(
    minutes: Long,
    name: String
)
object ActivityLevel:
    implicit val decoder: Decoder[ActivityLevel] = deriveDecoder

case class ManualValuesSpecified(
    calories: Boolean,
    distance: Boolean,
    steps: Boolean
)
object ManualValuesSpecified:
    implicit val decoder: Decoder[ManualValuesSpecified] = deriveDecoder


case class Source(
    id: String,
    name: String,
    trackerFeatures: Array[String],
    `type`: String,
    url: String
)
object Source:
    implicit val decoder: Decoder[Source] = deriveDecoder

