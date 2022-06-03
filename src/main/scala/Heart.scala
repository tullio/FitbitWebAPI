package org.example
import io.circe._
import io.circe.generic.semiauto._
import io.circe.parser.decode
case class Heart(
    `activities-heart`: Array[activitiesHeart],
    `activities-heart-intraday`: activitiesHeartIntraday,
    )
case class activitiesHeart(
    dateTime: String,
    value: Value,
    )

object activitiesHeart:
  implicit val decoder: Decoder[activitiesHeart] = deriveDecoder

object Heart:
  implicit val decoder: Decoder[Heart] = deriveDecoder
  def apply(json: String) =
///      println(decode[Heart](json))
      decode[Heart](json).right.get


case class Value(
    customHeartRateZones: Array[HeartRateZones],
    heartRateZones: Array[HeartRateZones],
    restingHeartRate: Long
)
object Value:
  implicit val decoder: Decoder[Value] = deriveDecoder

case class HeartRateZones(
    caloriesOut: Double,
    max: Long,
    min: Long,
    minutes: Long,
    name: String
)
object HeartRateZones:
  implicit val decoder: Decoder[HeartRateZones] = deriveDecoder

case class activitiesHeartIntraday(
    dataset: Array[Dataset],
    datasetInterval: Long,
    datasetType: String
    )
object activitiesHeartIntraday:
  implicit val decoder: Decoder[activitiesHeartIntraday] = deriveDecoder

case class Dataset(
    time: String,
    value: Long
    )
object Dataset:
  implicit val decoder: Decoder[Dataset] = deriveDecoder
