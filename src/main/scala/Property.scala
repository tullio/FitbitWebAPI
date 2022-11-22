package org.example
import org.tinylog.Logger
import scala.jdk.CollectionConverters._
import com.github.nscala_time.time.Imports._

object Property:
    val prop = readProperties
    val startDateStr = prop.get("StartDate").asInstanceOf[String]
    val startTimeStr = prop.get("StartTime").asInstanceOf[String]
    val startDateTime = DateTime.parse(s"${startDateStr}T${startTimeStr}")
    val endDateStr = prop.get("EndDate").asInstanceOf[String]
    val endTimeStr = prop.get("EndTime").asInstanceOf[String]
    val endDateTime = DateTime.parse(s"${endDateStr}T${endTimeStr}")
    val interval = new Interval(startDateTime, endDateTime)
    val detectWindowByMinutes = prop.get("DetectWindowByMinutes").asInstanceOf[Int]

    /**
      * DownSamplingWindowBySeconds*this(default: 15*5=75sec)
      * */
    val delayWindow = prop.get("DelayWindow").asInstanceOf[Int]
    val downSamplingWindowsBySeconds = prop.get("DownSamplingWindowBySeconds").asInstanceOf[Int]
