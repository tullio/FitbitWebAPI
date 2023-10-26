package org.example
import org.tinylog.Logger
import scala.jdk.CollectionConverters._
import com.github.nscala_time.time.Imports._
import java.nio.file.Paths
import cats.implicits

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

    val sessionName = prop.get("SessionName").asInstanceOf[String].replaceAll(" ", "_")
    val samplingMode = 
            prop.get("SamplingMode").asInstanceOf[String] match
                case "random" => SamplingMode.random
                case "importance" => SamplingMode.importance

    val interval = new Interval(startDateTime, endDateTime)
    val detectWindowByMinutes = prop.get("DetectWindowByMinutes").asInstanceOf[Int]
    val timeIndexFileName = s"time-index-${startDateStr}-${endDateStr}"
    val pdDimension = prop.get("PdDimension").asInstanceOf[Int]
    val pdLength = prop.get("PdLength").asInstanceOf[Int]
    val inputSize = pdDimension * pdLength

    val pdRegularizationUnit = prop.get("PdRegularizationUnit").asInstanceOf[Int]
    /**
      * DownSamplingWindowBySeconds*this(default: 15*5=75sec)
      * */
    val delayWindow = prop.get("DelayWindow").asInstanceOf[Int]
    val downSamplingWindowsBySeconds = prop.get("DownSamplingWindowBySeconds").asInstanceOf[Int]

    val year = Property.startDateTime.year.get
    val month = Property.startDateTime.getMonthOfYear
    val day = Property.startDateTime.getDayOfMonth
    val duration = Property.interval.toDuration.getStandardDays.toInt + 1

    val modelFile = Paths.get("classificationModel")
    /**
      * 
      * */
    // 2次元PDベクトルをフラットにした1次元ベクトルを並べた2次元ベクトル
    // sleepとactiveをくっつけたやつ（正例，負例的な）
    val learningDataFile = "learning-data.npy"
    val learningLabelFile = "learning-label.npy"
    // 2次元PDベクトルをフラットにした1次元ベクトルを並べた2次元ベクトル
    val sleepPdDataFile = "sleep-pd.npy"
    val activePdDataFile = "active-pd.npy"


    /**
      * 引数は，デフォルトではstartDateTimeの値が入る
      * @return f"${basename}-${year}%04d-${month}%02d"
      * */
    def getBaseFilename(basename: String, year: Int = year, month: Int = month, day: Int = day) =
        val baseFilename = f"${basename}-${year}%04d-${month}%02d"
        baseFilename
    /**
      * 引数は，デフォルトではstartDateTimeの値が入る
      * @return f"${year}%04d-${month}%02d-${day}%02d"
      * */
    def getDirname(year: Int = year, month: Int = month, day: Int = day) =
        val dirname = f"${year}%04d-${month}%02d-${day}%02d"
        dirname
