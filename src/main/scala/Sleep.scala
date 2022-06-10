package org.example
import io.circe._
//import io.circe.generic.extras.Configuration
import io.circe.generic.semiauto._
import io.circe.parser.decode
import org.tinylog.Logger

//implicit val config: Configuration = Configuration.default.withDefaults
//case class Sleep(sleep: Array[sleep] = Nil, summary: Summary)
case class Sleep(sleep: Array[sleep], summary: Summary)
object Sleep:
  implicit val decoder: Decoder[Sleep] = deriveDecoder
  def apply(json: String) =
      //Logger.debug("json={}", json.substring(10))
      Logger.debug("decode={}", decode[Sleep](json))
      decode[Sleep](json).right.get

case class sleep(
    dateOfSleep: String,
    duration: Long,
    efficiency: Long,
    endTime: String,
    infoCode: Long,
    isMainSleep: Boolean,
    levels: sleepLevels,
    logId: Long,
    minutesAfterWakeup: Long,
    minutesAsleep: Long,
    minutesAwake: Long,
    minutesToFallAsleep: Long,
    startTime: String,
    timeInBed: Long,
    `type`: String)

object sleep:
  implicit val decoder: Decoder[sleep] = deriveDecoder

case class sleepLevels(
    data: Array[sleepData],
    shortData: Array[sleepData],
    summary: sleepSummary,
)
object sleepLevels:
  implicit val decoder: Decoder[sleepLevels] = deriveDecoder

case class sleepData(
    dateTime: String,
    level: String,
    seconds: Int)
object sleepData:
  implicit val decoder: Decoder[sleepData] = deriveDecoder

case class sleepSummary(
    deep: DeepData,
    light: LightData,
    rem: RemData,
    wake: WakeData
)
case class DeepData(
    count: Long,
    minutes: Long,
    thirtyDayAvgMinutes: Long
)
case class LightData(
    count: Long,
    minutes: Long,
    thirtyDayAvgMinutes: Long
)
case class RemData(
    count: Long,
    minutes: Long,
    thirtyDayAvgMinutes: Long
)
case class WakeData(
    count: Long,
    minutes: Long,
    thirtyDayAvgMinutes: Long
)
/**
case class asleep(
    count: Long,
    minutes: Long)
case class awake(
    count: Long,
    minutes: Long)
case class restless(
    count: Long,
    minutes: Long)
case class sleepSummary(
    asleep: asleep,
    awake: awake,
    restless: restless)
  **/
//implicit val SleepDecoder: Decoder[Sleep] = deriveDecoder
case class Summary(
    stages: Option[Stages],
    totalMinutesAsleep: Long,
    totalSleepRecords: Long,
    totalTimeInBed: Long
)

object Summary:
  implicit val decoder: Decoder[Summary] = deriveDecoder

case class Stages(
    deep: Long,
    light: Long,
    rem: Long,
    wake: Long
)
object Stages:
  implicit val decoder: Decoder[Stages] = deriveDecoder
