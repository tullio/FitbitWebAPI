package org.example
import io.circe._
import io.circe.generic.semiauto._
case class Sleep(sleep: Array[sleep])
object Sleep:
  implicit val decoder: Decoder[Sleep] = deriveDecoder

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
//implicit val SleepDecoder: Decoder[Sleep] = deriveDecoder
