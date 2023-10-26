package org.example
import org.tinylog.Logger
import com.github.psambit9791.jdsp.misc.{Plotting, UtilMethods}
import scala.jdk.CollectionConverters._
import com.github.nscala_time.time.Imports._
import org.joda.time.Seconds
import org.jetbrains.bio.npy._
import java.nio.file.Paths
import com.example.pf.DataStream
import scala.collection.mutable.ArrayBuffer
import com.example.pf.Tensor
import better.files._
import better.files.Dsl.mkdir
import me.shadaj.scalapy.py
import me.shadaj.scalapy.py.SeqConverters


/***
  startTime, endTime: YYYY-MM-dd
 * **/
//@main def ActionDetection(startDate: String) =
@main def ActionDetection() =
    Logger.tags("NOTICE", "INFO").info("Start: {}", "start")
    val np = py.Module("numpy")
/*
    val prop = readProperties
    val startDate = prop.get("StartDate").asInstanceOf[String]
    val startTime = prop.get("StartTime").asInstanceOf[String]
    val endDate = prop.get("EndDate").asInstanceOf[String]
    val endTime = prop.get("EndTime").asInstanceOf[String]
    val detectWindowByMinutes = prop.get("DetectWindowByMinutes").asInstanceOf[Int]
    val downSamplingWindowsBySeconds = prop.get("DownSamplingWindowBySeconds").asInstanceOf[Int]
    val npyFileName = s"pointCloud-${startDate}.npy"
    val processStartDateObj = DateTime.parse(s"${startDate}T${startTime}")
    val processEndDateObj = DateTime.parse(s"${endDate}T${endTime}")
    val interval = new Interval(processStartDateObj, processEndDateObj)
 */
    val fb = FitbitDataStream("src/main/resources/tetsu.sato.toml")

    //val duration = interval.toDuration.getStandardDays.toInt + 1
    
    val pd = ArrayBuffer.empty[Double]
    for
        i <- 0 until Property.duration
    do
        Logger.tags("NOTICE", "INFO").info("Loop {} start.", i)
        val startDateObj = Property.processStartDateObj.plusDays(i)
        val endDateObj = startDateObj.plusDays(1)
        val startDate = f"${startDateObj.getYear}%04d-${startDateObj.getMonthOfYear}%02d-${startDateObj.getDayOfMonth}%02d"
        Logger.tags("NOTICE", "INFO").info("Creating directory {}...", startDate)
        /**
          * Data directory formed "yyyy-mm-dd"
          * */
        val currentDir = File(startDate)
        Logger.tags("INFO").info("Creating directory: {}", startDate)
        var skipFlag = false
        var pfFlag = false
        if currentDir.exists then
            Logger.tags("NOTICE", "INFO").info("Already exists {}", startDate)
            var filename = s"${currentDir}/heartrate-pf-${startDate}.npy"
            if File(filename).exists then
                // ToDo: skipの仕組みを考える
                Logger.tags("NOTICE", "INFO").info("Already exists. Skip filter tuning. {}", filename)
                skipFlag = true
        else
            //currentDir.delete() // deletes children recursively
            Logger.tags("NOTICE", "INFO").info("Creating directory {}", startDate)
            mkdir(currentDir)
        if skipFlag == false then
            /**
              * */
            val dataSeries = DataSeries(startDateObj)
            val ds = DataStream(i)
            Logger.tags("NOTICE", "INFO").info("Getting data via API...", "")
            // endTimeを引数に取るが，24時間を基本としたい
            val heartRateData = fb.getActivityHeartIntradayDataSeries(startDate, Property.startTimeStr, Property.endTimeStr)
            Logger.tags("NOTICE", "INFO").info(s"data length({})={}", startDateObj, heartRateData.length)
            Logger.info(s"full length={}", calculateSeconds(startDateObj, endDateObj))
            Logger.info(s"rate={}", heartRateData.length.toDouble/calculateSeconds(startDateObj, endDateObj).getSeconds)
            // dataSeries.xData/yDataに欠損値有り生データをセット
            dataSeries.setHeartRateData(heartRateData)
            Logger.tags("NOTICE", "INFO").info("Getting sleep via API...", "")
            val sleepData = fb.getSleepDataSeries(startDate) // 欠損値あり生データのはず
            // dataSeries.sleepXData/sleepYDataに欠損値有り生データをセット
            // 睡眠期間は補間されるけど，xData/yDataからピックアップするので
            // 結局欠損値有り生データになる
            dataSeries.setSleepData(sleepData)

            // 睡眠データと同じような処理
            Logger.tags("NOTICE", "INFO").info("Getting active via API...", "")
            val activeData = fb.getActivityLogList(startDate)
            dataSeries.setActiveData(activeData)

            dataSeries.saveAsPNG(s"${currentDir}/heartrate-${startDate}.png")
            //val heartRatePD = getHeartRatePointCloud(dataSeries, i.toDouble)
            //pd ++= heartRatePD

            ds.timeDataStream.multiplePush(dataSeries.xData.toArray, dataSeries.yData.toArray)
            ds.completeTimeSeries = Tensor(dataSeries.completeTime)
            Logger.debug("completeTimeSeries={}", ds.completeTimeSeries.take(3))
            Logger.debug("completeTimeSeries.length={}", ds.completeTimeSeries.length)
            // ds.timePredictStream/timeParticleStreamにセット
            Logger.tags("NOTICE", "INFO").info("Start tuning...", "")
            val (systemModel, observationModel) = ds.tuning // 欠損値無しになる
            // ToDo
            //Logger.tags("NOTICE", "INFO")
            //    .info("End tuning...Saving results: {}, {}", systemModel, observationModel)


            //val sampledPredictionData = ds.timePredictStreamSampling(60*10)
            // (max, min)を取るので，データは1/15*2になる
            Logger.tags("NOTICE", "INFO").info("Start sampling...", "")
            val sampledPredictionData = ds.timePredictStreamSampling(Property.downSamplingWindowsBySeconds)
            Logger.tags("NOTICE", "INFO").info("End sampling...", "")
            val sampleSize = sampledPredictionData.time.length
            // 状態の作成．0初期化しておいて．．．
            ds.timeStateStream.multiplePush(sampledPredictionData.time, Tensor.repeat(Tensor(0.0), sampleSize.toInt))
            Logger.tags("NOTICE", "INFO").info("Writing numpy file...", "")
            if dataSeries.sleepXData.length > 0 then
                var filename = s"${currentDir}/heartrate-sleep-${startDate}.npy"
                NpyFile.write(Paths.get(filename), 
                              dataSeries.sleepXData.map(f => f.toDouble).toArray ++
                              dataSeries.sleepYData.toArray   ,
                  Array(2, dataSeries.sleepXData.length.toInt))
                Logger.tags("NOTICE", "INFO").info("{} done.", filename)
                val sleepIter = dataSeries.sleepXData.iterator
                val sampledTimeIter = sampledPredictionData.time.tensorIterator
                // 状態の作成．0初期化しておいて．．．1で上書き
                // generateSampledStateStreamの中でds.timeStateStreamを更新している（ひでえ）
                val (sampledSleepTime, sampledSleepData) = generateSampledStateStream(sampledPredictionData.time, sampledPredictionData.data, 
                                           dataSeries.sleepXData.toArray, ds, 1.0)
                filename = s"${currentDir}/heartrate-sampled-sleep-${startDate}.npy"
                NpyFile.write(Paths.get(filename), 
                     sampledSleepTime.map(f => f.toDouble).toArray ++ 
                           sampledSleepData.getColumn(0).toArray ++
                           sampledSleepData.getColumn(1).toArray,
                  Array(3, sampledSleepTime.length.toInt))
                Logger.tags("NOTICE", "INFO").info("{} done.", filename)
            if dataSeries.activeXData.length > 0 then
                var filename = s"${currentDir}/heartrate-active-${startDate}.npy"
                NpyFile.write(Paths.get(filename), 
                              dataSeries.activeXData.map(f => f.toDouble).toArray ++
                              dataSeries.activeYData.toArray   ,
                  Array(2, dataSeries.activeXData.length.toInt))
                Logger.tags("NOTICE", "INFO").info("{} done.", filename)
                val activeIter = dataSeries.activeXData.iterator
                val sampledTimeIter = sampledPredictionData.time.tensorIterator
                // 状態の作成．0初期化しておいて．．．2で上書き
                // generateSampledStateStreamの中でds.timeStateStreamを更新している（ひでえ）
                val (sampledActiveTime, sampledActiveData) = generateSampledStateStream(sampledPredictionData.time, sampledPredictionData.data,
                                           dataSeries.activeXData.toArray, ds, 2.0)
                if sampledActiveData.length > 0 then
                    filename = s"${currentDir}/heartrate-sampled-active-${startDate}.npy"
                    NpyFile.write(Paths.get(filename), 
                         sampledActiveTime.map(f => f.toDouble).toArray ++ 
                                  sampledActiveData.getColumn(0).toArray ++
                                  sampledActiveData.getColumn(1).toArray   ,
                      Array(3, sampledActiveTime.length.toInt))
                    Logger.tags("NOTICE", "INFO").info("{} done.", filename)
                else
                    Logger.tags("INFO").info("sampledActiveData.length={}. Skip saving.",
                     sampledActiveData.length)

            var filename = s"${currentDir}/heartrate-pf-${startDate}.npy"
            NpyFile.write(Paths.get(filename), 
                  ds.timePredictStream.time.toArray.zip(ds.timePredictStream.data.toArray)
                    .foldLeft(ArrayBuffer.empty[Double])((g, h) => g ++ Array(h._1, h._2)).toArray,
                  Array(2, ds.timePredictStream.time.length.toInt))
            Logger.tags("NOTICE", "INFO").info("{} done.", filename)
            //NpyFile.write(Paths.get(s"heartrate-pd-${startDate}.npy"), pd.toArray, Array(pd.length/3, 3))
            filename = s"${currentDir}/heartrate-pd-${startDate}.npy"
            NpyFile.write(Paths.get(filename), 
                                  dataSeries.xData.toArray.map(f => f.toDouble) ++
                                  dataSeries.yData.toArray ++
                                  Array.fill(dataSeries.xData.length)(i.toDouble)
                                  , Array(3, dataSeries.xData.length))
            Logger.tags("NOTICE", "INFO").info("{} done.", filename)
            filename = s"${currentDir}/heartrate-sampled-pf-${startDate}.npy"
            NpyFile.write(Paths.get(filename), 
                                  (sampledPredictionData.time.toArray ++
                                   sampledPredictionData.data.getColumn(0).toArray ++
                                   sampledPredictionData.data.getColumn(1).toArray ), 
                                   Array(3, sampledPredictionData.time.length.toInt))
            Logger.tags("NOTICE", "INFO").info("{} done.", filename)
            filename = s"${currentDir}/heartrate-sampledstate-pf-${startDate}.npy"
            NpyFile.write(Paths.get(filename), 
                                  (ds.timeStateStream.time.toArray ++
                                   ds.timeStateStream.data.toArray), 
                                   Array(2, ds.timeStateStream.time.length.toInt))
            Logger.tags("NOTICE", "INFO").info("{} done.", filename)
            Logger.tags("NOTICE", "INFO").info("Writing numpy file...end", "")
        else
            Logger.tags("NOTICE", "INFO").info("Skipped.", "")
    Logger.tags("NOTICE", "INFO").info("End: {}", "end")
    

def getHeartRatePointCloud(dataSeries: DataSeries, z: Double) =
    val pd = ArrayBuffer.empty[Double]
    dataSeries.xData.zip(dataSeries.yData).foreach{f =>
        pd ++= Array(f._1, f._2, z)
    }
    pd.toArray

/**
  * ダウンサンプリングした秒データの中から，アクション秒データの間にあるデータをピックアップした
  * ダウサンプリングアクションデータを作る．
  * ダウンサンプリングデータからピックアップするので，データは(max, min)のペアとなる
  * @args time ダウンサンプリングした秒データ
  * @args data ダウンサンプリングした心拍データ
  * @args action 対象アクションの生データ秒データ
  * */
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

