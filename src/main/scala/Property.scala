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
    val processStartDateObj = DateTime.parse(s"${startDateStr}T${startTimeStr}")
    val processEndDateObj = DateTime.parse(s"${endDateStr}T${endTimeStr}")

    val interval = new Interval(startDateTime, endDateTime)
    val detectWindowByMinutes = prop.get("DetectWindowByMinutes").asInstanceOf[Int]

    /**
      * DownSamplingWindowBySeconds*this(default: 15*5=75sec)
      * */
    val delayWindow = prop.get("DelayWindow").asInstanceOf[Int]
    val downSamplingWindowsBySeconds = prop.get("DownSamplingWindowBySeconds").asInstanceOf[Int]

    val year = Property.startDateTime.year.get
    val month = Property.startDateTime.getMonthOfYear
    val day = Property.startDateTime.getDayOfMonth
    val duration = Property.interval.toDuration.getStandardDays.toInt + 1

    def getBaseFilename(basename: String, year: Int = year, month: Int = month, day: Int = day) =
        val baseFilename = f"${basename}-${year}%04d-${month}%02d"
        baseFilename
    def getDirname(year: Int = year, month: Int = month, day: Int = day) =
        val dirname = f"${year}%04d-${month}%02d-${day}%02d"
        dirname
