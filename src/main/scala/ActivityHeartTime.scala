package org.example
import io.circe._
import io.circe.generic.semiauto._
import io.circe.parser.decode

case class ActivityHeartTime(
    `activities-heart`: Array[activitiesHeartRateZones],
    `activities-heart-intraday`: activitiesHeartIntraday,
    )

object ActivityHeartTime:
  implicit val decoder: Decoder[ActivityHeartTime] = deriveDecoder
  def apply(json: String) =
      println(decode[ActivityHeartTime](json))
      decode[ActivityHeartTime](json).right.get

case class activitiesHeartRateZones(
  customHeartRateZones: Array[HeartRateZones],
  dateTime: String,
  heartRateZones: Array[HeartRateZones],
  value: String
  )
object activitiesHeartRateZones:
  implicit val decoder: Decoder[activitiesHeartRateZones] = deriveDecoder




