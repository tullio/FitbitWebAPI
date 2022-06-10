package org.example
import org.tinylog.Logger
import com.github.psambit9791.jdsp.misc.{Plotting, UtilMethods}
import scala.jdk.CollectionConverters._
import com.github.nscala_time.time.Imports._
import org.jetbrains.bio.npy._
import java.nio.file.Paths

/***
  startTime, endTime: YYYY-MM-dd
 * **/
@main def ActionDetection(startDate: String) =
    val npyFileName = s"pointCloud-${startDate}.npy"
    val processStartDateObj = DateTime.parse(startDate)
    val fb = FitbitDataStream("src/main/resources/tetsu.sato.toml")
    //val startDateTime = s"${startDate}T00:00"
    //val endDateTime = s"${endDate}T00:00"
    val startTime = "00:00"
    val endTime = "23:59"
    //val endTime = "13:59"

    val duration = 3
    
    val pd = scala.collection.mutable.ArrayBuffer.empty[Double]
    for
        i <- 0 to 3 
    do
        val startDateObj = processStartDateObj.plusDays(i)
        val dataSeries = DataSeries(startDateObj)
        val startDate = f"${startDateObj.getYear}%04d-${startDateObj.getMonthOfYear}%02d-${startDateObj.getDayOfMonth}%02d"
        val heartRateData = fb.getActivityHeartIntradayDataSeries(startDate, startTime, endTime)
        dataSeries.setHeartRateData(heartRateData)
        val sleepData = fb.getSleepDataSeries(startDate)
        dataSeries.setSleepData(sleepData)

        val activeData = fb.getActivityLogList(startDate)
        dataSeries.setActiveData(activeData)

        dataSeries.saveAsPNG(s"heartrate-${startDate}.png")
        val heartRatePD = getHeartRatePointCloud(dataSeries, i.toDouble)
        pd ++= heartRatePD

    NpyFile.write(Paths.get(s"heartrate-${startDate}.npy"), pd.toArray, Array(pd.length/3, 3))

def getHeartRatePointCloud(dataSeries: DataSeries, z: Double) =
    val pd = scala.collection.mutable.ArrayBuffer.empty[Double]
    dataSeries.xData.zip(dataSeries.yData).foreach{f =>
        pd ++= Array(f._1, f._2, z)
    }
    pd.toArray
