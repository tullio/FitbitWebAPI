package org.example
import org.tinylog.Logger
import com.github.psambit9791.jdsp.misc.{Plotting, UtilMethods}
import scala.jdk.CollectionConverters._
import com.github.nscala_time.time.Imports._
import org.jetbrains.bio.npy._
import java.nio.file.Paths
import com.example.pf.DataStream
import scala.collection.mutable.ArrayBuffer
import com.example.pf.Tensor
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
    //val startTime = "18:00"
    //val endTime = "03:59"
    val endTime = "23:59"

    val duration = 5
    
    val pd = ArrayBuffer.empty[Double]
    for
        i <- 0 until duration
    do
        val ds = DataStream()
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

        ds.timeDataStream.multiplePush(dataSeries.xData.toArray, dataSeries.yData.toArray)
        ds.completeTimeSeries = Tensor(dataSeries.completeTime)
        Logger.debug("completeTimeSeries={}", ds.completeTimeSeries.take(3))
        Logger.debug("completeTimeSeries.length={}", ds.completeTimeSeries.length)
        ds.tuning
        //val sampledPredictionData = ds.timePredictStreamSampling(60*10)
        val sampledPredictionData = ds.timePredictStreamSampling(15)
        val sampleSize = sampledPredictionData.time.length
        ds.timeStateStream.multiplePush(sampledPredictionData.time, Tensor.repeat(Tensor(0.0), sampleSize.toInt))
        if dataSeries.sleepXData.length > 0 then
            val sleepIter = dataSeries.sleepXData.iterator
            val sampledTimeIter = sampledPredictionData.time.tensorIterator
            val (sampledSleepTime, sampledSleepData) = generateSampledStateStream(sampledPredictionData.time, sampledPredictionData.data, 
                                       dataSeries.sleepXData.toArray, ds, 1.0)
            NpyFile.write(Paths.get(s"heartrate-sampled-sleep-${startDate}.npy"), 
                 sampledSleepTime.map(f => f.toDouble).toArray ++ 
                       sampledSleepData.getColumn(0).toArray ++
                       sampledSleepData.getColumn(1).toArray,
              Array(3, sampledSleepTime.length.toInt))
        if dataSeries.activeXData.length > 0 then
            val activeIter = dataSeries.activeXData.iterator
            val sampledTimeIter = sampledPredictionData.time.tensorIterator
            val (sampledActiveTime, sampledActiveData) = generateSampledStateStream(sampledPredictionData.time, sampledPredictionData.data,
                                       dataSeries.activeXData.toArray, ds, 2.0)
            NpyFile.write(Paths.get(s"heartrate-sampled-active-${startDate}.npy"), 
                 sampledActiveTime.map(f => f.toDouble).toArray ++ 
                          sampledActiveData.getColumn(0).toArray ++
                          sampledActiveData.getColumn(1).toArray   ,
              Array(3, sampledActiveTime.length.toInt))
                                       
        NpyFile.write(Paths.get(s"heartrate-pf-${startDate}.npy"), 
              ds.timePredictStream.time.toArray.zip(ds.timePredictStream.data.toArray)
                .foldLeft(ArrayBuffer.empty[Double])((g, h) => g ++ Array(h._1, h._2)).toArray,
              Array(2, ds.timePredictStream.time.length.toInt))
        //NpyFile.write(Paths.get(s"heartrate-pd-${startDate}.npy"), pd.toArray, Array(pd.length/3, 3))
        NpyFile.write(Paths.get(s"heartrate-pd-${startDate}.npy"), 
                              dataSeries.xData.toArray.map(f => f.toDouble) ++
                              dataSeries.yData.toArray ++
                              Array.fill(dataSeries.xData.length)(i.toDouble)
                              , Array(3, dataSeries.xData.length))
        NpyFile.write(Paths.get(s"heartrate-sampled-pf-${startDate}.npy"), 
                              (sampledPredictionData.time.toArray ++
                               sampledPredictionData.data.getColumn(0).toArray ++
                               sampledPredictionData.data.getColumn(1).toArray ), 
                               Array(3, sampledPredictionData.time.length.toInt))
        NpyFile.write(Paths.get(s"heartrate-sampledstate-pf-${startDate}.npy"), 
                              (ds.timeStateStream.time.toArray ++
                               ds.timeStateStream.data.toArray), 
                               Array(2, ds.timeStateStream.time.length.toInt))


    

def getHeartRatePointCloud(dataSeries: DataSeries, z: Double) =
    val pd = ArrayBuffer.empty[Double]
    dataSeries.xData.zip(dataSeries.yData).foreach{f =>
        pd ++= Array(f._1, f._2, z)
    }
    pd.toArray

def generateSampledStateStream(time: Tensor, data: Tensor, action: Array[Int], ds: DataStream, id: Double) =
    //Logger.debug("data shape={}([:, 1] or [:, 2]?)", data.shape.toSeq)
    //Logger.debug("data {}", data)
    if time.length != data.shape(0) then
        Logger.error("time length and data length must be equal. time={}, data={}", time.length, data.shape.toSeq)
    val actionIter = action.iterator
    val sampledTimeIter = time.tensorIterator
    var sampledIndex = 0
    var actionTime = actionIter.next
    var sampledTime = sampledTimeIter.next.apply(0).toInt
    val sampledActionTime = scala.collection.mutable.ArrayBuffer.empty[Int]
    val sampledActionData = Tensor.create(0, data.shape(1).toInt)

    while sampledTimeIter.hasNext && actionIter.hasNext do
        //Logger.debug("actionTime: {}, sampledTime: {}", actionTime, sampledTime)
        (actionTime, sampledTime) match
                 case (action, sample) if action == sample =>
                                          ds.timeStateStream.data(sampledIndex) = id
                                          sampledActionTime += sample
                                          //Logger.debug("sampledActionData={}", sampledActionData)
                                          //Logger.debug("sampledActionData.shape={}", sampledActionData.shape.toSeq)
                                          //Logger.debug("data.getRow(sampledIndex)={}", data.getRow(sampledIndex))

                                          //Logger.debug("data.getRow(sampledIndex).shape={}", data.getRow(sampledIndex).shape.toSeq)
                                          sampledActionData.push(data.getRow(sampledIndex))
                                          //Logger.debug("sampledIndex({}) = {}", sampledIndex, id)
                                          if actionIter.hasNext then
                                              actionTime = actionIter.next
                                          if sampledTimeIter.hasNext then
                                              sampledTime = sampledTimeIter.next.apply(0).toInt
                                              sampledIndex += 1
                 case (action, sample) if action < sample =>
                                            if actionIter.hasNext then
                                                actionTime = actionIter.next
                                                //Logger.debug("now, actionTime={}", actionTime)
                                                if actionTime > sampledTime then
                                                    ds.timeStateStream.data(sampledIndex) = id
                                                    sampledActionTime += sample
                                                    sampledActionData.push(data.getRow(sampledIndex))
                                                    //Logger.debug("sampledIndex({}) = {}", sampledIndex, id)
                 case (action, sample) if action > sample =>
                                            if sampledTimeIter.hasNext then
                                                val tensorNext = sampledTimeIter.next
                                                //Logger.debug("tensorNext={}", tensorNext)
                                                sampledTime = tensorNext.apply(0).toInt
                                                //Logger.debug("now, sampledTime={}", sampledTime)
                                                sampledIndex += 1
    (sampledActionTime, sampledActionData)
