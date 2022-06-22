package org.example
import org.tinylog.Logger
import com.github.psambit9791.jdsp.misc.{Plotting, UtilMethods}
import scala.jdk.CollectionConverters._
import com.github.nscala_time.time.Imports._
import org.jetbrains.bio.npy._
import java.nio.file.Paths

extension(d: scala.collection.mutable.ArrayBuffer[Int])
    def toDoubleArray: Array[Double] =
        d.map(f => f.toDouble).toArray

class DataSeries(startDateTimeObj: DateTime):
    var completeTime: Array[Double] = _
    val xData = scala.collection.mutable.ArrayBuffer.empty[Int]
    val yData = scala.collection.mutable.ArrayBuffer.empty[Double]
    val sleepXData = scala.collection.mutable.ArrayBuffer.empty[Int]
    val sleepYData = scala.collection.mutable.ArrayBuffer.empty[Double]
    val activeXData = scala.collection.mutable.ArrayBuffer.empty[Int]
    val activeYData = scala.collection.mutable.ArrayBuffer.empty[Double]
    val avgXData = scala.collection.mutable.ArrayBuffer.empty[Int]
    val avgYData = scala.collection.mutable.ArrayBuffer.empty[Double]
    val generatePNG = true
    var fig: Plotting = null
    def setHeartRateData(dataSeries: List[Either[String, Dataset]]) =
        val startDate = f"${startDateTimeObj.getMonthOfYear}%02d-${startDateTimeObj.getDayOfMonth}%02d"
        Logger.debug("startDate: {}", startDate)
        if generatePNG then
            fig = Plotting(600, 300, s"day:${startDate}", "Time", "Heart Rate")
        Logger.debug("dataSeries.length={}", dataSeries.length)
        val time = UtilMethods.linspace(0, dataSeries.length.toDouble, dataSeries.length, false)
        completeTime = time
        Logger.debug("completeTime.length={}", completeTime.length)
        Logger.debug("ActionDetection: {}", dataSeries.take(3))
        if generatePNG then
            fig.initialisePlot
        time.zip(dataSeries).foreach{f =>
           if f._2.isRight then
               val value
                 = f._2 match
                       case Right(dataset) => dataset.value.toDouble
               //fig.addPoints("Heart Rate", Array(f._1), Array(value))
               xData += f._1.toInt
               yData += value
        }
        if generatePNG then
            fig.addSignal("Heart Rate", xData.toDoubleArray, yData.toArray, false)

//    def getHeartRateData =
        
    def setSleepData(sleepData: Sleep) =
        Logger.debug("sleep: {}", sleepData.sleep.take(3).toSeq)
        if sleepData.sleep.length > 0 then
            val sleepMap = scala.collection.mutable.Seq.fill[Boolean](60*60*24)(false)
            sleepData.sleep.foreach{f =>
                val sleeplevels = f.levels
                sleeplevels.data.foreach{g =>
                    val dateTimeString = g.dateTime
                    val dateTime = DateTime.parse(dateTimeString)
                    val secondOfDay = (dateTime.toTimeOfDay.getHourOfDay * 60 
                                      + dateTime.toTimeOfDay.getMinuteOfHour ) * 60 +
                                      dateTime.toTimeOfDay.getSecondOfMinute
                    (secondOfDay until secondOfDay + g.seconds).foreach{h =>
                        sleepMap(h) = true
                    }
                }
            }
            xData.zip(yData).foreach{f =>
                val indexAsSeconds = f._1.toInt
                if sleepMap(indexAsSeconds) then
                    sleepXData += f._1
                    sleepYData += f._2
            }
            if generatePNG then
                if sleepXData.length > 0 then
                    fig.addSignal("Sleep", sleepXData.toDoubleArray, sleepYData.toArray, false)
                else
                    Logger.debug(s"Sleep: {}", "No data")
    def setActiveData(activeData: ActivityLog) =
        val avg = MovingAverage(60*10)
        Logger.debug("active: {}", activeData.activities(0).startTime)
        val activeMap = scala.collection.mutable.Seq.fill[Boolean](60*60*24)(false)
        var activePeriod = 0
        activeData.activities.foreach{f =>
            val dateTimeString = f.startTime
            val activityLevel = f.activityLevel
            Logger.debug("activityLevel=={}", activityLevel.toSeq)
            val substantialDuration = activityLevel.filter(f => f.name == "lightly" ||
                                                                f.name == "fairly" ||
                                                                f.name == "very")
                         .foldLeft(0L)((g, h) => g + h.minutes)
            //Logger.debug("corrected duration=={}(min)", substantialDuration)
            val dateTime = DateTime.parse(dateTimeString)
            Logger.debug("active duration: {}", f.duration)
            Logger.debug("log date:{}", dateTime)
            Logger.debug("processing start date:{}", startDateTimeObj)
            if dateTime.getMonthOfYear == startDateTimeObj.getMonthOfYear &&
               dateTime.getDayOfMonth == startDateTimeObj.getDayOfMonth then
                val secondOfDay = (dateTime.getHourOfDay * 60 
                                  + dateTime.getMinuteOfHour ) * 60 +
                                  dateTime.getSecondOfMinute
                (secondOfDay until secondOfDay + (f.duration/1000).toInt).foreach{h =>
                //(secondOfDay until secondOfDay + substantialDuration.toInt*60).foreach{h =>
                    if h < activeMap.length then
                        activeMap(h) = true
                        activePeriod += 1
                }
        }
        Logger.debug("activePeriod:{}", activePeriod)
        var avgOverPeriod = 0
        xData.zip(yData).foreach{f =>
            val indexAsSeconds = f._1.toInt
            if activeMap(indexAsSeconds) then
                if avg.length == 0 || avg.value > 90 then
                    activeXData += f._1
                    activeYData += f._2
                    avgOverPeriod += 1
                avg += f._2
                avgXData += f._1
                avgYData += avg.value
        }
        Logger.debug("avgOverPeriod:{}", avgOverPeriod)
        if generatePNG then
            if activeXData.length > 0 then
                fig.addSignal("Active", activeXData.toDoubleArray, activeYData.toArray, false)
                fig.addSignal("Active(avg)", avgXData.toDoubleArray, avgYData.toArray, false)
            else
                Logger.debug("Active: {}", "No data")

    def saveAsPNG(filename: String) =
        if generatePNG then
            fig.saveAsPNG(filename)
        
