package org.example

//import com.example.pf.DataStream.timeData
import com.github.nscala_time.time.Imports._
import me.shadaj.scalapy.py
import me.shadaj.scalapy.py.SeqConverters
import org.tinylog.Logger

import scala.jdk.CollectionConverters._
import better.files._
import me.shadaj.scalapy.py
import me.shadaj.scalapy.py.SeqConverters
import ai.djl.ndarray.{NDArrays, NDManager} 
import ai.djl.ndarray.NDArray
import ai.djl.ndarray.index.NDIndex
import ai.djl.ndarray.types.{Shape, DataType}
import scala.annotation.targetName
import ai.djl.Device
import scala.collection.mutable.ListBuffer
import org.example.ActionTarget
import scala.languageFeature.existentials

import scala.collection.parallel.CollectionConverters.*

import com.github.nscala_time.time.Imports._

class PersistenceDiagramTransform:
//    val manager = NDManager.newBaseManager(Device.cpu)
    val hc = py.module("homcloud.interface")
    val np = py.module("numpy")
    val pv = py.module("homcloud.paraview_interface")
    val go = py.module("plotly.graph_objects")
    val p3d = py.module("homcloud.plotly_3d")
    val plt = py.module("matplotlib.pyplot")
    val manager = NDManager.newBaseManager(Device.cpu)
    import Property.sessionName
    File(sessionName).createDirectoryIfNotExists
    Logger.tags("NOTICE", "INFO").info("session={}", sessionName)

    /**
      * Save an NDArray object as a NumPy ndarray object into the file named 
      * $fileName in the session directory
      * */
    def npSave(fileName: String, array: NDArray) =
        val filePath = s"${sessionName}/${fileName}"
        // yyyy-mm-ddがセッションディレクトリに最初は無いから作る
        Logger.tags("NOTICE").info("parent={}",File(filePath).parent)
        if !File(filePath).parent.exists then
            Logger.tags("NOTICE").info("parent {} is not exists. creating...",File(filePath).parent)
            File(filePath).parent.createDirectory
        Logger.tags("NOTICE").info("parent={}",File(filePath).parent)
        np.save(s"${sessionName}/${fileName}", array.toNumpy(np))

    /**
      * Save a NumPy object as a NumPy ndarray object into the file named 
      * $fileName in the session directory
      * */
    def npSave(fileName: String, array: py.Dynamic) =
        np.save(s"${sessionName}/${fileName}", array)

    /**
      * Load an NDArray object as a NumPy ndarray object from the file named 
      * $fileName in the session directory
      * */
    def npLoad(fileName: String) =
        val filePath = s"${sessionName}/${fileName}"
        Logger.tags("NOTICE", "INFO").info("loading {}", filePath)
        val ret = np.load(s"${sessionName}/${fileName}")
        Logger.tags("NOTICE", "INFO").info("loading results shape={}", ret.shape.as[Seq[Int]])
        ret

    def npExists(fileName: String) =
        File(s"${sessionName}/${fileName}").exists

    /**
      * 全てのデータが入っているサンプリング済みデータから，時間と状態を見て
      * 心拍データを抜き出す
      * 元データは2023-02-17/heartrate-sampled-pf-2023-02-17.npyみたいな
      * 状態データは2023-02-17/heartrate-sampledstate-pf-2023-02-17.npyみたいな
      * ActionDetection.tomlで指定された期間分をNDArrayで返し，
      * その時間情報はファイルに保存する
      * loadDailyDataは入力データはファイルにあるからembeddingしか
      * 返さないけど，ファイルに無いので元データも返したい
      * これ時間かかるのでキャッシュしたい
      * 管理とか考えてる時間が今日はないので，メソッド内キャッシュでいい
      * sessionName/extracted-${target}-pc.npy
      * */
    def extractDailyData(target: ActionTarget, plan: String = "A"): NDArray =

        val year = Property.startDateTime.year.get
        val month = Property.startDateTime.getMonthOfYear
        val day = Property.startDateTime.getDayOfMonth
        val duration = Property.interval.toDuration.getStandardDays.toInt + 1

        val baseName = f"heartrate-sampled-pf"
        val baseFilename = Property.getBaseFilename(baseName)
        val stateBaseName = f"heartrate-sampledstate-pf"
        val stateBaseFilename = Property.getBaseFilename(stateBaseName)
        val cacheFileName = s"${sessionName}/extracted-${target}-pc.npy"


        val intervalRange = Range(day, day + duration)
        Logger.tags("NOTICE", "INFO").info("start={}, end={}", 
                          Property.startDateTime, Property.endDateTime)
        val dimension = 3
        val dataSize = 3 // time, max, min
        var pcs = 
            plan match
                case "A" | "B" => 
                    manager.create(Shape(0, dataSize + 1), DataType.FLOAT32) // time, max, min, z
                case "C" =>
                    manager.create(Shape(0, dimension, dataSize), DataType.FLOAT32)
        if File(cacheFileName).exists then
            Logger.tags("NOTICE", "INFO").info("cache file found{}", cacheFileName)
            val cachedPcsNpArray = npLoad(s"extracted-${target}-pc.npy")

            val shape = cachedPcsNpArray.shape.as[Seq[Long]] // List(105302, 3, 3)
            Logger.tags("NOTICE", "INFO").info("cache file shape={}", shape)
            val flattenNpArray = cachedPcsNpArray.flatten()
            val cachedPcsNDArray = manager.create(flattenNpArray.as[Seq[Double]])
                                   .toType(DataType.FLOAT32, false)
            Logger.tags("NOTICE", "INFO").info("cache fileed NDArray={}", cachedPcsNDArray)

            val cachedPcsNDArrayFilnal = cachedPcsNDArray.reshape(shape:_*)

            Logger.tags("NOTICE", "INFO").info("loaded pcs={}", cachedPcsNDArrayFilnal)
            return cachedPcsNDArrayFilnal
        for i <- intervalRange do
            //val dir_name = f"${year}%04d-${month}%02d-${i}%02d"
            val dir_name = Property.getDirname(day = i)
            val filename = f"${dir_name}/${baseFilename}-${i}%02d.npy"
            if File(filename).exists then // loadDailyDataと違い，基本は存在するはず
                Logger.tags("NOTICE", "INFO").info("Loading...{}", filename)
                // raw dataはユニークでsession関係ないので，npLoadは使わない
                val dayPfPc = manager.create(np.load(filename).T.as[Seq[Seq[Double]]])//.get("0:7, :")
                          .toType(DataType.FLOAT32, false)
                Logger.tags("NOTICE", "INFO").info("loadded. shape={}, {}", 
                                              dayPfPc.getShape, dayPfPc.get("0:3, :"))
                val pcTimeIndex = dayPfPc.get(NDIndex().addAllDim.addIndices(0))
                                      .toFloatArray.map(f => f.toDouble)

                // Unknown=0, sleep=1, active=2の状態ファイル
                val statefilename = f"${dir_name}/${stateBaseFilename}-${i}%02d.npy"
                if File(statefilename).exists then // 存在するはず
                    Logger.tags("NOTICE", "INFO").info("Loading...{}", statefilename)
                    // raw dataはユニークでsession関係ないので，npLoadは使わない
                    val state = manager.create(np.load(statefilename).T
                                   .as[Seq[Seq[Double]]])//.get("0:7, :")
                                   .toType(DataType.FLOAT32, false)
                    Logger.tags("NOTICE", "INFO").info("loadded. state shape={}, {}"
                                    , state.getShape, state.get("0:3, :"))
                    val stTimeIndex = state.get(NDIndex().addAllDim.addIndices(0))
                                      .toFloatArray.map(f => f.toDouble)
                    // 対象の状態のデータをツメツメにしたNDArray
                    // このままembeddingすると混ざる
                    if pcTimeIndex.size == stTimeIndex.size then
                        val timeIndex = pcTimeIndex
                        val pcArray = timeIndex.zipWithIndex.map{f =>
                            // 状態はNDArrayではdoubleで入ってるけど，enumと比較するためにIntに
                            val currentState = state.get(
                                                 NDIndex().addIndices(f._2).addSliceDim(1, 2)//.addIndices(0)
                                                 )
                                                .getFloat(0).toInt
                            //Logger.tags("NOTICE", "INFO").info("currentState={}", currentState)
                            if currentState == target.ordinal then
                                Some(dayPfPc.get(
                                                 NDIndex().addIndices(f._2).addAllDim)//.addIndices(0)
                                )
                            else
                                None
                        }.filter(f => f.isDefined)
                         .map(f => f.get) // Array[NDArray]
                        // embedding作ってから，連続性をチェックするか？
                        val shape = Shape(pcArray.size, 3) // time, min, max
                        val pc = manager.create(shape)
                        //Logger.tags("NOTICE", "INFO").info("creating empty pc set. shape={}", shape)
                        pcArray.zipWithIndex.foreach{f =>
                        //Logger.tags("NOTICE", "INFO").info("set row({}) to {}", f._2, f._1)
                            pc.set(NDIndex().addIndices(f._2).addAllDim, f._1)
                        }
                        val dayPc = plan match
                            case "A" =>
                                val index = i - day // スタートからの日数
                                //val z = np.full(pc.shape[0], index) // z-axis
                                val z = manager.full(Shape(pc.getShape.get(0), 1), index.toFloat, DataType.FLOAT32)
                                //Logger.info("z coordinate shape={}, {}", z.getShape, z.get("0:3, :"))
                                val pc4 = pc.concat(z, 1)
                                Logger.tags("NOTICE").info("stacked. shape={}", pc4.getShape)
                                pc4
                            case "B" =>
                                //end_index = len(pc[:, 0]) - 1 # len(pc[:, 0])は86400になるはず
                                //endIndex = pc.getShape.get(0) - 1
                                val pc4 = createEmbedding(pc)
                                val dayOffset = (i - day) * 60*60*24
                                val updateRange = NDIndex().addAllDim.addIndices(0)
                                pc4.set(updateRange, pc4.get(updateRange).add(dayOffset))
                                pc4
                            case "C" =>
                                // detectWindowByMinutesごとにembeddingを作りたい
                                val numRecord = pc.getShape.getShape()(0)
                                val dimension = pc.getShape.getShape()(1)
                                if numRecord - dimension > 5 then
                                    Logger.tags("NOTICE", "INFO")
                                        .info("data size is enough. target={}, record={}, dimension={}",
                                              target, numRecord, dimension)
                                    Logger.tags("NOTICE", "INFO").info("embedding target={}({})", pc, pc.get("0:3, :"))
                                    val pc4 = createSlidingWindowEmbedding(pc)
                                    Logger.tags("NOTICE", "INFO").info("embedding={}({})", pc4, pc4.get("0:3, :"))
                                    val dayOffset = (i - day) * 60*60*24
                                    val updateRange = NDIndex().addAllDim
                                                               .addAllDim // dimension
                                                                .addIndices(0) // time in data
                                    pc4.set(updateRange, pc4.get(updateRange).add(dayOffset))
                                    pc4
                                else
                                    Logger.tags("NOTICE", "INFO")
                                        .info("data size is too small. target={}, record={}, dimension={}",
                                              target, numRecord, dimension)
                                    manager.create(Shape(0, pcs.getShape.getShape()(1)
                                                            ,pcs.getShape.getShape()(2) ))
                        pcs = pcs.concat(dayPc , 0)
            else
                Logger.tags("DEBUG").info("{filename} does not exists. skip.", filename)
        npSave(s"extracted-${target}-pc.npy", pcs.toNumpy(np))
        pcs
    /**
      * 独立したファイルに入っている，Active/Sleepのデータを読む
      * ActionDetection.tomlで指定された期間分をNDArrayで返し，
      * その時間情報はファイルに保存する
      * */
    def loadDailyData(target: ActionTarget, plan: String = "A") =
        val baseName = f"heartrate-sampled-${target}"
        Logger.tags("NOTICE", "INFO").debug("target={}, PLAN={}", target, plan)
        val year = Property.startDateTime.year.get
        val month = Property.startDateTime.getMonthOfYear
        val day = Property.startDateTime.getDayOfMonth
        val duration = Property.interval.toDuration.getStandardDays.toInt + 1
        //val baseFilename = f"${basename}-${year}%04d-${month}%02d"
        val baseFilename = Property.getBaseFilename(baseName)
        val intervalRange = Range(day, day + duration)
        var index = 0
        Logger.tags("NOTICE", "INFO").info("start={}, end={}", Property.startDateTime, Property.endDateTime)
        /**** planA/Bのコード ****
        var pcs = manager.create(Shape(0, 4), DataType.FLOAT32)
         * ***/
        val dimension = 3
        val dataSize = 3 // time, max, min
        var pcs = 
            plan match
                case "A" | "B" => 
                    manager.create(Shape(0, dataSize + 1), DataType.FLOAT32) // time, max, min, z
                case "C" =>
                    manager.create(Shape(0, dimension, dataSize), DataType.FLOAT32)
        for i <- intervalRange do
            //val dir_name = f"${year}%04d-${month}%02d-${i}%02d"
            val dir_name = Property.getDirname(day = i)
            val filename = f"${dir_name}/${baseFilename}-${i}%02d.npy"
            Logger.tags("NOTICE", "INFO").info("Loading...{}", filename)
            if File(filename).exists then
                // raw dataはユニークでsession関係ないので，npLoadは使わない
                val pc = manager.create(np.load(filename).T.as[Seq[Seq[Double]]])//.get("0:7, :")
                          .toType(DataType.FLOAT32, false)
                Logger.tags("NOTICE", "INFO").info("loadded. shape={}, {}", pc.getShape, pc.get("0:3, :"))
                val dayPc = plan match
                    case "A" =>
                        //val z = np.full(pc.shape[0], index) // z-axis
                        val z = manager.full(Shape(pc.getShape.get(0), 1), index.toFloat, DataType.FLOAT32)
                        //Logger.info("z coordinate shape={}, {}", z.getShape, z.get("0:3, :"))
                        val pc4 = pc.concat(z, 1)
                        Logger.tags("NOTICE").info("stacked. shape={}", pc4.getShape)
                        pc4
                    case "B" =>
                        //end_index = len(pc[:, 0]) - 1 # len(pc[:, 0])は86400になるはず
                        //endIndex = pc.getShape.get(0) - 1
                        val pc4 = createEmbedding(pc)
                        val dayOffset = (i - day) * 60*60*24
                        val updateRange = NDIndex().addAllDim.addIndices(0)
                        pc4.set(updateRange, pc4.get(updateRange).add(dayOffset))
                        pc4
                    case "C" =>
                        // detectWindowByMinutesごとにembeddingを作りたい
                        val numRecord = pc.getShape.getShape()(0)
                        val dimension = pc.getShape.getShape()(1)
                        if numRecord - dimension > 5 then
                            val pc4 = createSlidingWindowEmbedding(pc)
                            val dayOffset = (i - day) * 60*60*24
                            val updateRange = NDIndex().addAllDim
                                                       .addAllDim // dimension
                                                        .addIndices(0) // time in data
                            pc4.set(updateRange, pc4.get(updateRange).add(dayOffset))
                            pc4
                        else
                            Logger.tags("NOTICE", "INFO")
                                .info("data size is too small. target={}, record={}, dimension={}",
                                      target, numRecord, dimension)
                            manager.create(Shape(0, pcs.getShape.getShape()(1)
                                                    ,pcs.getShape.getShape()(2) ))
                pcs = pcs.concat(dayPc , 0)
            else
                Logger.tags("DEBUG").info("{filename} does not exists. skip.", filename)
            index += 1

        //manager.close
        //hc.del()
        //np.del()
        //pv.del()
        //go.del()
        // ここがカラの場合はある
        Logger.tags("DEBUG").info("pcs for getFixedStepIndexFromPcs={}", pcs.get("0:3"))
        val numRecord = pcs.getShape.getShape()(0)
        val numDimension = pcs.getShape.getShape()(1)
        if numRecord * numDimension > 3 then
            saveTimeIndexExtractFromPCS(pcs)
        else
            Logger.tags("NOTICE", "INFO")
                        .info("data size is too small. record={}, dimension={}",
                        numRecord, dimension)
        pcs
    def saveTimeIndexExtractFromPCS(pcs: NDArray) =
        val timeIndex = manager.create(getFixedStepIndexFromPcs(pcs).map(f => f.toDouble)).uniq
        Logger.tags("NOTICE", "DEBUG").info("Saving time index {} to ...{}", timeIndex.get("0:3"), Property.timeIndexFileName)
        if File(s"${sessionName}/${Property.timeIndexFileName}.npy").exists then
            Logger.tags("NOTICE", "DEBUG")
                 .info("file {} is already exists. merging....", Property.timeIndexFileName)
            val timeRangeNp = npLoad(s"${Property.timeIndexFileName}.npy") // ToDo: NDArrayにする
            val existingTimeRange = manager.create(timeRangeNp.as[Seq[Double]])
            Logger.tags("NOTICE", "DEBUG")
                 .info("merging {} and {}", timeIndex.get("0:3"), existingTimeRange.get("0:3"))

            val mergedTimeIndex = timeIndex.concat(existingTimeRange).sort.uniq
            Logger.tags("NOTICE", "DEBUG").debug("merged={}", mergedTimeIndex.get("0:3"))
            //np.save(Property.timeIndexFileName, mergedTimeIndex.toNumpy(np))
            npSave(Property.timeIndexFileName, mergedTimeIndex)
        else
            Logger.tags("NOTICE", "DEBUG")
                 .info("file {} is not exists. writing {}...", Property.timeIndexFileName,
                            timeIndex.get("0:3"))
            //np.save(Property.timeIndexFileName, timeIndex.toNumpy(np))
            npSave(Property.timeIndexFileName, timeIndex)

    def refreshTimeIndex =
        val baseName = f"heartrate-sampled-pf"
        val year = Property.startDateTime.year.get
        val month = Property.startDateTime.getMonthOfYear
        val day = Property.startDateTime.getDayOfMonth
        val duration = Property.interval.toDuration.getStandardDays.toInt + 1
        //val baseFilename = f"${basename}-${year}%04d-${month}%02d"
        val baseFilename = Property.getBaseFilename(baseName)
        val intervalRange = Range(day, day + duration)
        val dataSize = 3 // time, max, min
        var pcs =  manager.create(Shape(0, dataSize), DataType.FLOAT32) // time, max, min

        for i <- intervalRange do
            //val dir_name = f"${year}%04d-${month}%02d-${i}%02d"
            val dir_name = Property.getDirname(day = i)
            val filename = f"${dir_name}/${baseFilename}-${i}%02d.npy"
            Logger.tags("NOTICE", "INFO").info("Loading...{}", filename)
            if File(filename).exists then
                // raw dataはユニークでsession関係ないので，npLoadは使わない
                val pc = manager.create(np.load(filename).T.as[Seq[Seq[Double]]])//.get("0:7, :")
                          .toType(DataType.FLOAT32, false)
                Logger.tags("NOTICE", "INFO").info("loadded. shape={}, {}", pc.getShape, pc.get("0:3, :"))
                pcs = pcs.concat(pc , 0)
            else
                Logger.tags("DEBUG").info("{filename} does not exists. skip.", filename)
        val numRecord = pcs.getShape.getShape()(0)
        val numDimension = pcs.getShape.getShape()(1)
        val timeIndex = pcs.get(NDIndex().addAllDim.addSliceDim(0, 1)) // 多分15秒間隔
        val timeRangePair = timeIndex.getMinMaxPair
        val detectWindow = Property.detectWindowByMinutes * 60
        val timeRange = Range(timeRangePair(0), timeRangePair(1), detectWindow) // detectWindow間隔
        val fixedSteptimeIndex = manager.create(timeRange.map(f => f.toDouble))
        npSave(Property.timeIndexFileName, fixedSteptimeIndex)

    def close = 
       manager.close
    def getMinOrMaxPcs(pcs: NDArray, axis: Int) =
        pcs.get(":, 0").stack(pcs.get(f":, ${axis}"), 1)
                              .concat(pcs.get(":, 3").addHDim(1), 1)
    /**
      * extract column 0(time), 1(min value), 3(z-axis)
      * */
    def getMinPcs(pcs: NDArray) = getMinOrMaxPcs(pcs, 1)
    /**
      * extract column 0(time), 2(max value), 3(z-axis)
      * */
    def getMaxPcs(pcs: NDArray) = getMinOrMaxPcs(pcs, 2)
    /**
      * load persistence diagram vector data from npy files.
      * NDArray is returned(not djl DataSet)
      */
    def loadSleepPdDataset(plan: String) =
        loadPdDataset(ActionTarget.sleep, plan)
    /**
      * load persistence diagram vector data from npy files.
      * NDArray is returned(not djl DataSet)
      */
    def loadActivePdDataset(plan: String) =
        loadPdDataset(ActionTarget.active, plan)

    //def choicePD(numpyArray: py.Dynamic, rev: String) =
    def choicePD(numpyArray: py.Dynamic) =
        val newNumpyArray = 
        //if rev == "rev01" then
        if Property.samplingMode == SamplingMode.random then
            // PDの2次元ベクトルはたくさんあるのでダウンサンプリング
            val index = np.random.choice(numpyArray.shape.bracketAccess(0)
                           , size=Property.pdLength, replace=false)
            numpyArray.bracketAccess(index)
        //else if rev == "rev02" then
        else if Property.samplingMode == SamplingMode.importance then
            import me.shadaj.scalapy.py.PyQuote
            // birth-deathの差が大きい順にソートして上から取るサンプリング
            val sortedNumpyArray 
                = py.Dynamic.global.sorted(numpyArray, key=py"lambda x: x[1]-x[0]", reverse=true)
            py"${sortedNumpyArray}[0: ${Property.pdLength}]"
            //Logger.tags("NOTICE").debug("sampled={}", newNumpyArray.shape)
        else
            np.zero()
        newNumpyArray

    // 2次元PDベクトルをフラットにした1次元ベクトルを並べた2次元ベクトル
    def loadPdDataset(label: ActionTarget, plan: String) =
        // 時間インデックスを取り出すためにオリジナルデータを一旦読む
        //val pcs = loadPcDataset(label, plan)
        var numDataset = 0
        // ウィンドウ単位でPDをロードする
        val detectWindow = Property.detectWindowByMinutes * 60
        // NNの入力サイズの長さの1次元ベクトルを用意
        var dataset = manager.create(Shape(0, Property.inputSize), DataType.FLOAT32)
        //for startIndex <- getStepIndexFromPcs(pcs) do
        for startIndex <- getTimeIndexRange do
            indexToTime(startIndex, detectWindow)
            val filename = f"${getFilename(label, "pd", startIndex)}.npy"
            Logger.tag("INFO").info("filename to load={}", filename)
            if npExists(filename) then
                Logger.tags("NOTICE", "INFO").info("{} exists. loading...", filename)
                // この段階では，(datalength, 2)の2次元ベクトル
                val numpyArray = npLoad(filename).astype(np.float32)
                Logger.tags("NOTICE", "INFO")
                         .info("loaded PD data={}()should be 2D", numpyArray.shape) // (57, 2)
                val shape = (numpyArray.shape.bracketAccess(0).as[Double], numpyArray.shape.bracketAccess(1).as[Double])
                Logger.tags("NOTICE", "INFo").info("NDArrayed shape={}", shape)
                if shape._1 > Property.pdLength then
                    //val newNumpyArray = choicePD(numpyArray, "rev02")
                    val newNumpyArray = choicePD(numpyArray)

                    val pd = manager.create(newNumpyArray.as[Seq[Seq[Double]]]).toType(DataType.FLOAT32, false)
                    Logger.tags("NOTICE", "INFO").info("loaded pd={}", pd.get("0:3, :"))
                    Logger.tags("NOTICE", "INFO").info("loaded pd size={}", pd.getShape)
                    // (32, 2)をフラットにするので(1, 64)
                    val mlpInputPd = pd.reshape(1, pd.size)
                    //Logger.tags("NOTICE", "DEBUG").debug("input pd={}", mlpInputPd.get("0:3"))
                    //Logger.tags("NOTICE", "DEBUG").debug("input pd size={}", mlpInputPd.getShape)
                    dataset = dataset.concat(mlpInputPd)
                    Logger.tags("NOTICE", "INFO").info("current dataset = {}", dataset.getShape)
                else
                    Logger.tags("NOTICE", "DEBUG")
                                 .debug("sampling number is too small {}<{}"
                                        , shape(0), Property.pdLength)
                    Logger.tags("NOTICE", "DEBUG")
                                  .debug("sampling procedure should be skipped for {}.", shape)
            else
                Logger.tags("NOTICE", "INFO").warn("{} does not exists", filename)
        Logger.tags("NOTICE", "INFO").info("final dataset = {}", dataset.getShape)
        dataset
    def loadSleepPcDataset(plan: String = "A") =
            loadPcDataset(ActionTarget.sleep, plan)
    def loadActivePcDataset(plan: String = "A") =
            loadPcDataset(ActionTarget.active, plan)
    def loadPcDataset(target: ActionTarget, plan: String) =
        // ダウンサンプリングしたmax-min睡眠時心拍データ
        // 24時間分のロード
        val pcs = loadDailyData(target, plan)
        pcs
    def createSleepPDDataset(plan: String = "A") =
        createPDDataset(ActionTarget.sleep, plan)
    def createActivePDDataset(plan: String = "A") =
        createPDDataset(ActionTarget.active, plan)
    def createUnknownPDDataset(plan: String = "A") =
        createPDDataset(ActionTarget.unknown, plan)
    /**
      * 単純にpcsのtimeレコードの先頭から，detectWindow*60を加算してったRangeオブジェクト
      * */
    def getFixedStepIndexFromPcs(pcs: NDArray) =
        /** planA/Bのコード
        val timeRange = pcs.get(":, 0").getMinMaxPair
          */
        Logger.tags("DEBUG").info("pcs shape={}", pcs.getShape.getShape.toSeq)
        Logger.tags("DEBUG").info("これtimeRangeを求めるデータであってる？{}", pcs.get(NDIndex().addAllDim.addIndices(0).addIndices(0)).get("0:3"))
        Logger.tags("DEBUG").info("これtimeRangeを求めるデータであってる？{}", pcs.get(":, 0, 0").get("0:3"))
        //val timeRange = pcs.get(NDIndex().addAllDim.addIndices(0)).getMinMaxPair
        // Fix: 2023/2/25 05/40
        // loadDailyDataでembeddingされたpcを考えていたから，3次元になっている
        val timeRange = pcs.get(NDIndex().addAllDim.addIndices(0).addIndices(0)).getMinMaxPair
        val detectWindow = Property.detectWindowByMinutes * 60
        Logger.tags("DEBUG").info("start={}, end={}", timeRange(0), timeRange(1))
        Range(timeRange(0), timeRange(1), detectWindow)
    /**
      * timeIndexFileのtimeレコードの先頭から，detectWindow*60を加算してったRangeオブジェクト
      * */
    def getTimeIndexRange =
        /** planA/Bのコード
        val timeRange = pcs.get(":, 0").getMinMaxPair
          */
        val timeRangeNp = npLoad(s"${Property.timeIndexFileName}.npy") // ToDo: NDArrayにする
        Logger.tags("NOTICE", "INFO").info("timeIndexFile={}",
            timeRangeNp
        )

        val timeRange = manager.create(timeRangeNp.as[Seq[Double]])
                           .toType(DataType.FLOAT32, false)
                           .getMinMaxPair
        val detectWindow = Property.detectWindowByMinutes * 60
        Logger.tags("DEBUG").info("start={}, end={}", timeRange(0), timeRange(1))
        Range(timeRange(0), timeRange(1), detectWindow)
    /**
      * timeStampのtimeレコードの先頭から，detectWindow*60を加算してったRangeオブジェクト
      * */
    def getTimeIndexRange(timeStamp: NDArray) =
        val timeRange = timeStamp
                           .getMinMaxPair
        val detectWindow = Property.detectWindowByMinutes * 60
        Logger.tags("DEBUG").info("start={}, end={}", timeRange(0), timeRange(1))
        Range(timeRange(0), timeRange(1), detectWindow)



    def getNearestTimeIndex(targetTime: String) =
        val dateTime = DateTime.parse(targetTime)
        Logger.tags("NOTICE").info("dateTime={}", dateTime)
        val hour = dateTime.getHourOfDay
        val minute = dateTime.getMinuteOfHour
        val second = dateTime.getSecondOfMinute
        //Logger.tags("NOTICE").info("hour={}, minute={}, second={}", hour, minute, second)
        val target = second + minute * 60 + hour * 60 * 60
        Logger.tags("NOTICE").info("target second={}", target)
        val timeIndexRange = getTimeIndexRange
        val indexCandidate = timeIndexRange.sliding(2)
                                    .filter(f => f(0)<= target && target < f(1))
                                    .toSeq
        Logger.tags("NOTICE").info("indexCandidate={}", indexCandidate)
        // とりあえず区間の下限を返してみよう
        indexCandidate.toSeq(0)(0)

    def getTargetSlotFromPcs(pcs: NDArray, targetTime: String) =
        Logger.tags("NOTICE").info("input={}", pcs)
        Logger.tags("NOTICE").info("input={}", pcs.get("0:4, :, :"))
        val timeIndex = getNearestTimeIndex(targetTime)
        val timeColumn = NDIndex().addAllDim.addAllDim.addSliceDim(0, 1)
        Logger.tags("NOTICE").info("time column={}", pcs.get(timeColumn).get("0:3"))
        Logger.tags("NOTICE").info("time column eq={}", pcs.get(timeColumn).eq(timeIndex).get("0:3"))
        Logger.tags("NOTICE").info("time column eq={}", pcs.get(timeColumn).eq(timeIndex).get("0:3").uniq)
        pcs.get(pcs.get(timeColumn) // time column
                                 .eq(timeIndex))
//    def displayTimeIndexRange =
    def createPDDataset(target: ActionTarget, plan: String) =
        val pcs = loadPcDataset(target, plan)
        Logger.tags("NOTICE", "DEBUG").debug("pcs({})={}, shape={}"
                                          , target, pcs.get("0:3, :"), pcs.getShape.getShape.toSeq)
        val timeLength = pcs.getShape.get(0)
        if timeLength > 0 then        
            //Logger.debug("pcs.shape={}", pcs.getShape)
            //val sleeppcsMax = sleeppcs.get(":, (0, 1, 3)")
            /**** planA/Bのコード ****
            val pcsMax = pcs.get(":, 0").stack(pcs.get(":, 1"), 1)
                                  .concat(pcs.get(":, 3").addHDim(1), 1)
            //Logger.debug("pcsMax={}", pcsMax.get("0:3, :"))
            //Logger.debug("pcsMax.shape={}", pcsMax.getShape)
            //val sleeppcsMin = sleeppcs.get(":, (0, 2, 3)")
            val pcsMin = pcs.get(":, 0").stack(pcs.get(":, 2"), 1)
                                  .concat(pcs.get(":, 3").addHDim(1), 1)
             * */
            val timeStamp = pcs.get(NDIndex().addAllDim
                                          .addIndices(0)
                                          .addIndices(0)).reshape(timeLength, 1)
            Logger.tags("NOTICE", "DEBUG").debug("t={}?, shape={}", timeStamp.get(NDIndex().addSliceDim(0, 3)), timeStamp.getShape)
            val maxValue = pcs.get(NDIndex().addAllDim
                                          .addAllDim
                                          .addIndices(1)).reshape(timeLength, -1)
            Logger.tags("NOTICE", "DEBUG").debug("max={}?, shape={}", maxValue.get(NDIndex().addSliceDim(0, 3)), maxValue.getShape)
            val pcsMax = timeStamp.concat(maxValue, 1) // Shape(timeLength, 4)


            Logger.tags("NOTICE").debug("pcsMax={}, shape={}", pcsMax.get("0:3, :"), pcsMax.getShape)
            //Logger.debug("pcsMax.shape={}", pcsMax.getShape)
            //val sleeppcsMin = sleeppcs.get(":, (0, 2, 3)")
            val pcsMin = pcs.get(NDIndex().addAllDim
                                          .addAllDim
                                          .addIndices(0))
            // ウィンドウ単位でPDを作成する
            val detectWindow = Property.detectWindowByMinutes * 60
            //for startIndex <- getStepIndexFromPcs(pcsMax) do
            //for startIndex <- getTimeIndexRange do
            val indexSet = getTimeIndexRange.toSet
            //var numDataset = 0
            //var numRecord = 0
            val numDataset = indexSet.foldLeft(scala.collection.mutable.Map.empty[Int, Int])
                                 ((f, g) => f + (g -> 0))
            val numRecord = indexSet.foldLeft(scala.collection.mutable.Map.empty[Int, Int])
                                 ((f, g) => f + (g -> 0))
            //indexSet.par.foreach{startIndex =>
            indexSet.par.foreach{startIndex =>
                //Logger.info("STARTINDEX={}", startIndex)
                indexToTime(startIndex, detectWindow)
                //split_pc = sleeppcs_max[start_index: start_index + detect_window]
                // time embeddingなどで0-axisはソートされていない状態
                val timeColumnRange = NDIndex().addAllDim.addIndices(0)
                // ここで探索をかけるので，startIndexは実際にデータに存在するとは限らない
                val splitPc = pcsMax.get((pcsMax.get(timeColumnRange) >= startIndex) 
                                   * (pcsMax.get(timeColumnRange) < startIndex + detectWindow))
                if splitPc.getShape.get(0) < 5 then
                    Logger.tags("NOTICE","DEBUG")
                    .debug("target data for startIndex={} not found or too small. skip.", startIndex)
                    Logger.tags("NOTICE","DEBUG")
                    .debug("target data={}", splitPc)
                else
                    Logger.tags("NOTICE","DEBUG")
                    .debug("target data for startIndex={} found.", startIndex)
                    //Logger.tags("DEBUG").debug("splitPC={}", splitPc)
                    Logger.tags("DEBUG").debug("in plan {}, splitPC={}", plan, splitPc.get("0:4, :"))

                    /* ****** これはplanA/Bのコード******
                    //np.set_printoptions(threshold=np.inf, suppress=True)
                    //Logger.debug("slplitted-{}={}", startIndex, splitPc)
                    //Logger.debug("splitted-pc-{}.shape={}", startIndex, splitPc.getShape)
                    // z軸の値が全部あるかの確認．Plan Aは日数(Property.interval)，Plan BはProperty.delayWindow
                    val zindex = splitPc.get(":, 2").uniq
                    val zSize = plan match 
                                 case "A" => Property.interval.toPeriod.getDays + 1
                                 case "B" => Property.delayWindow
                    //save_pcpdpng("sleep", split_pc, base_name, start_index)
                    //if len(split_pc) > 0: # JSIAM
                    //Logger.debug(splitPc.size)
                    //if splitPc.size() > 90 && zindex.size == Property.interval.toPeriod.getDays + 1 then // FIT

                    if splitPc.size() > 3 && zindex.size == zSize then
                        drawPcpd(label, splitPc)
                        numDataset += 1
                    //return sleeppd1    
                    else
                        Logger.tag("NOTICE").warn("splitPc.size={}, zindex.size({})->{}", splitPc.size(), zindex.size,
                                                            zSize)
                     * *****/

                    plan match
                        case "A" =>
                            val pcs = splitPc.get(NDIndex().addAllDim.addSliceDim(0, 3))
                            Logger.tags("NOTICE").debug("pcs={}", pcs)
                            savePcs(target, startIndex, splitPc) // (all, time, max, z)
                            numDataset(startIndex) += 1
                            numRecord(startIndex) += splitPc.getShape.get(0).toInt
                            val filename = getFilename(target, "pd", startIndex)
                            val pd1 = getPd(f"${filename}-pcs", splitPc)
                            if pd1.isDefined then
                                val pd = pd1.get
                                savePd(target, startIndex, pd)
                                drawPd(target, startIndex, pd)
                                savePdHist(target, startIndex, pd)
                                drawPcpd(target, startIndex, splitPc, pd)
                            else
                                Logger.tags("DEBUG").info("pd does not found.", "")
                        case "C" =>
                            // (all, 0)を取る(all, 1, 2, 3)にする
                            val pcsWithoutTime = splitPc.get(NDIndex().addAllDim.addSliceDim(1, 4))
                            Logger.tags("NOTICE").debug("pcsWithoutTime={}", pcsWithoutTime)
                            savePcs(target, startIndex, pcsWithoutTime)
                            numDataset(startIndex) += 1
                            numRecord(startIndex) += splitPc.getShape.get(0).toInt
                            val filename = getFilename(target, "pd", startIndex)
                            val pd1 = getPd(f"${filename}-pcs", pcsWithoutTime)
                            if pd1.isDefined then
                                val pd = pd1.get
                                savePd(target, startIndex, pd)
                                drawPd(target, startIndex, pd)
                                savePdHist(target, startIndex, pd)
                                drawPcpd(target, startIndex, pcsWithoutTime, pd)
                            else
                                Logger.tags("DEBUG").info("pd does not found.", "")



            }
            val numDatasetSum = numDataset.values.sum
            val numRecordSum = numRecord.values.sum
            Logger.tags("NOTICE", "DEBUG").info("collected dataset for {} = {}", target, numDatasetSum)
            Logger.tags("NOTICE", "DEBUG")
                       .info("collected record(time base) for {} = {}", target, numRecordSum)
    // 2023-02-17/heartrate-sampled-sleep-pd-27315.npy
    def createPDDatasetAt(target: ActionTarget, plan: String, dateTimeStr: String): NDArray =
        // loadDailyDataはsleep/activeしか対応してないので，extractで
        val pcs = extractDailyData(target, plan)
        Logger.tags("NOTICE", "INFO").debug("extracted pcs({})={}, shape={}"
                                          , target, pcs.get("0:3, :"), pcs.getShape.getShape.toSeq)
        val timeLength = pcs.getShape.get(0)
        if timeLength > 0 then        
            //Logger.debug("pcs.shape={}", pcs.getShape)
            //val sleeppcsMax = sleeppcs.get(":, (0, 1, 3)")
            /**** planA/Bのコード ****
            val pcsMax = pcs.get(":, 0").stack(pcs.get(":, 1"), 1)
                                  .concat(pcs.get(":, 3").addHDim(1), 1)
            //Logger.debug("pcsMax={}", pcsMax.get("0:3, :"))
            //Logger.debug("pcsMax.shape={}", pcsMax.getShape)
            //val sleeppcsMin = sleeppcs.get(":, (0, 2, 3)")
            val pcsMin = pcs.get(":, 0").stack(pcs.get(":, 2"), 1)
                                  .concat(pcs.get(":, 3").addHDim(1), 1)
             * */
            val timeStamp = pcs.get(NDIndex().addAllDim
                                          .addIndices(0)
                                          .addIndices(0)).reshape(timeLength, 1)
            Logger.tags("NOTICE", "DEBUG").debug("t={}?, shape={}", timeStamp.get(NDIndex().addSliceDim(0, 3)), timeStamp.getShape)

            Logger.tags("NOTICE").info("targetTime={}", dateTimeStr)
            val targetIndex = getNearestTimeIndex(dateTimeStr)
            Logger.tags("NOTICE").info("targetIndex={}", targetIndex)
            // ウィンドウ単位でPDを作成する
            val detectWindow = Property.detectWindowByMinutes * 60

            val extractTimeStampNDIndex = timeStamp.eq(targetIndex) + 
                                          timeStamp.gt(targetIndex) * 
                                          timeStamp.lt(targetIndex + detectWindow)
            Logger.tags("NOTICE", "INFO").info("extract NDIndex={}, shape={}", extractTimeStampNDIndex.get("0:30"), extractTimeStampNDIndex.getShape)

            val extractTimeStamp = timeStamp.get(extractTimeStampNDIndex).reshape(-1, 1)
            Logger.tags("NOTICE", "INFO").debug("extract timeStamp={}?, shape={}", extractTimeStamp.get(NDIndex().addSliceDim(0, 3)), extractTimeStamp.getShape)

            val maxValue = pcs.get(NDIndex().addAllDim
                                          .addAllDim
                                          .addIndices(1)).reshape(timeLength, -1)
            Logger.tags("NOTICE", "DEBUG").debug("max={}?, shape={}", maxValue.get(NDIndex().addSliceDim(0, 3)), maxValue.getShape)
            val extract2dindex =                                           extractTimeStampNDIndex
                                           .concat(extractTimeStampNDIndex, 1)
                                           .concat(extractTimeStampNDIndex, 1)

            Logger.tags("NOTICE", "INFO").info("extract2dindex={}?, shape={}", extract2dindex.get(NDIndex().addSliceDim(0, 3)), extract2dindex.getShape)
            val extractMaxValue = maxValue.get(extract2dindex
                                       ).reshape(Shape(extractTimeStamp.getShape.get(0), 
                                                       maxValue.getShape.get(1)))
        
            Logger.tags("NOTICE", "INFO").info("extract max={}?, shape={}", extractMaxValue.get(NDIndex().addSliceDim(0, 3)), extractMaxValue.getShape)

            //val pcsMax = timeStamp.concat(maxValue, 1) // Shape(timeLength, 4)
            val pcsMax = extractTimeStamp.concat(extractMaxValue, 1) // Shape(xxx, 4)


            Logger.tags("NOTICE", "INFO").info("maybe found: pcsMax={}, shape={}", pcsMax.get("0:3, :"), pcsMax.getShape)
            //Logger.debug("pcsMax.shape={}", pcsMax.getShape)
            //val sleeppcsMin = sleeppcs.get(":, (0, 2, 3)")
            val pcsMin = pcs.get(NDIndex().addAllDim
                                          .addAllDim
                                          .addIndices(0))
            //for startIndex <- getStepIndexFromPcs(pcsMax) do
            //for startIndex <- getTimeIndexRange do
            val indexSet = getTimeIndexRange(extractTimeStamp).toSet
            //var numDataset = 0
            //var numRecord = 0
            val numDataset = indexSet.foldLeft(scala.collection.mutable.Map.empty[Int, Int])
                                 ((f, g) => f + (g -> 0))
            val numRecord = indexSet.foldLeft(scala.collection.mutable.Map.empty[Int, Int])
                                 ((f, g) => f + (g -> 0))
            //indexSet.par.foreach{startIndex =>
            var resultPd = manager.create(Shape(0, 2))
                           .toType(DataType.FLOAT32, false)
            indexSet.foreach{startIndex =>
                //Logger.info("STARTINDEX={}", startIndex)
                indexToTime(startIndex, detectWindow)
                //split_pc = sleeppcs_max[start_index: start_index + detect_window]
                // time embeddingなどで0-axisはソートされていない状態
                val timeColumnRange = NDIndex().addAllDim.addIndices(0)
                // ここで探索をかけるので，startIndexは実際にデータに存在するとは限らない
                val splitPc = pcsMax.get((pcsMax.get(timeColumnRange) >= startIndex) 
                                   * (pcsMax.get(timeColumnRange) < startIndex + detectWindow))
                if splitPc.getShape.get(0) < 5 then
                    Logger.tags("NOTICE","DEBUG")
                    .debug("target data for startIndex={} not found or too small. skip.", startIndex)
                    Logger.tags("NOTICE","DEBUG")
                    .debug("target data={}", splitPc)
                else
                    Logger.tags("NOTICE","DEBUG")
                    .debug("target data for startIndex={} found.", startIndex)
                    //Logger.tags("DEBUG").debug("splitPC={}", splitPc)
                    Logger.tags("DEBUG").debug("in plan {}, splitPC={}", plan, splitPc.get("0:4, :"))

                    /* ****** これはplanA/Bのコード******
                    //np.set_printoptions(threshold=np.inf, suppress=True)
                    //Logger.debug("slplitted-{}={}", startIndex, splitPc)
                    //Logger.debug("splitted-pc-{}.shape={}", startIndex, splitPc.getShape)
                    // z軸の値が全部あるかの確認．Plan Aは日数(Property.interval)，Plan BはProperty.delayWindow
                    val zindex = splitPc.get(":, 2").uniq
                    val zSize = plan match 
                                 case "A" => Property.interval.toPeriod.getDays + 1
                                 case "B" => Property.delayWindow
                    //save_pcpdpng("sleep", split_pc, base_name, start_index)
                    //if len(split_pc) > 0: # JSIAM
                    //Logger.debug(splitPc.size)
                    //if splitPc.size() > 90 && zindex.size == Property.interval.toPeriod.getDays + 1 then // FIT

                    if splitPc.size() > 3 && zindex.size == zSize then
                        drawPcpd(label, splitPc)
                        numDataset += 1
                    //return sleeppd1    
                    else
                        Logger.tag("NOTICE").warn("splitPc.size={}, zindex.size({})->{}", splitPc.size(), zindex.size,
                                                            zSize)
                     * *****/

                    plan match
                        case "A" =>
                            val pcs = splitPc.get(NDIndex().addAllDim.addSliceDim(0, 3))
                            Logger.tags("NOTICE").debug("pcs={}", pcs)
                            savePcs(target, startIndex, splitPc) // (all, time, max, z)
                            numDataset(startIndex) += 1
                            numRecord(startIndex) += splitPc.getShape.get(0).toInt
                            val filename = getFilename(target, "pd", startIndex)
                            val pd1 = getPd(f"${filename}-pcs", splitPc)
                            if pd1.isDefined then
                                val pd = pd1.get
                                savePd(target, startIndex, pd)
                                drawPd(target, startIndex, pd)
                                savePdHist(target, startIndex, pd)
                                drawPcpd(target, startIndex, splitPc, pd)
                            else
                                Logger.tags("DEBUG").info("pd does not found.", "")
                        case "C" =>
                            // (all, 0)を取る(all, 1, 2, 3)にする
                            val pcsWithoutTime = splitPc.get(NDIndex().addAllDim.addSliceDim(1, 4))
                            Logger.tags("NOTICE").debug("pcsWithoutTime={}", pcsWithoutTime)
                            savePcs(target, startIndex, pcsWithoutTime)
                            numDataset(startIndex) += 1
                            numRecord(startIndex) += splitPc.getShape.get(0).toInt
                            val filename = getFilename(target, "pd", startIndex)
                            val pd1 = getPd(f"${filename}-pcs", pcsWithoutTime)
                            if pd1.isDefined then
                                val pd = pd1.get
                                          .toType(DataType.FLOAT32, false)
                                Logger.tags("NOTICE", "INFO").info("obtaind PD={}", pd)


                                resultPd = resultPd.concat(pd)
                                Logger.tags("NOTICE", "INFO").info("stacked PD={}", resultPd)
                                savePd(target, startIndex, pd)
                                drawPd(target, startIndex, pd)
                                savePdHist(target, startIndex, pd)
                                drawPcpd(target, startIndex, pcsWithoutTime, pd)
                            else
                                Logger.tags("DEBUG").info("pd does not found.", "")



            }
            val numDatasetSum = numDataset.values.sum
            val numRecordSum = numRecord.values.sum
            Logger.tags("NOTICE", "DEBUG").info("collected dataset for {} = {}", target, numDatasetSum)
            Logger.tags("NOTICE", "DEBUG")
                       .info("collected record(time base) for {} = {}", target, numRecordSum)
            resultPd
        else
            manager.create(Array(0.0))
    def indexToTimeString(index: Int): String = 
        val day = (index / (60*60*24)).toInt
        val index2 = index % (60*60*24)
        val minutes = (index2 / 60).toInt
        val second = index2 - minutes * 60
        val hour = (minutes / 60).toInt
        val minute = minutes - hour * 60
        return f"${day} ${hour}%02d:${minute}%02d:${second}%02d"

    def indexToTime(index: Int, window: Int) =
        val start = indexToTimeString(index)
        val `end` = indexToTimeString(index + window)
        Logger.tags("DEBUG").debug("start index={}, window={}, end index={}, as time String; {}--{}",
                      index, window, index + window, start, `end`)
    def pyList(x: Seq[Double]) =
        py.Dynamic.global.list(x.toPythonProxy)
//    def pyList(x: py.Any) =
//        py.Dynamic.global.list(x)
    def pyDict(`type`: String = "", b: Double = -0.1) =
    //def pyDict(`type`: String = "") =
    //    py.Dynamic.global.dict(`type` = `type`, b = b)
        val dic = py.Dynamic.global.dict(`type` = `type`)
        if b != -0.1 then
            dic.update(b = b)
        dic
    /**
      * extract persistence diagram from the point cloud data in the NDArray
      * and save in the file ${basename}.pdgm
      * */
    def getPd(basename: String, pcs: NDArray) =
        val filtrationFile = f"${basename}.pdgm"
        Logger.tags("NOTICE").debug("input pcs={}", pcs.get("0:5, :"))
        hc.PDList.from_alpha_filtration(pcs.toNumpy(np), 
                                    save_to=filtrationFile,
                                    save_boundary_map=true)
        val pdlist = hc.PDList(filtrationFile)
        val pd1 = pdlist.dth_diagram(1)
        //print("histogram", pd1.histogram())
        //print("pair", pd1.pairs())
        val births = pd1.births.as[Seq[Double]].toArray
        val deaths = pd1.deaths.as[Seq[Double]].toArray
        Logger.tags("NOTICE").info("births={}, len={}", births.toSeq.take(3), births.toSeq.size)
        Logger.tags("NOTICE").info("deaths={}, len={}", deaths.toSeq.take(3), deaths.toSeq.size)
        val pairs = 
            if births.length > 0 && deaths.length > 0 then
                           Option(manager.create(births.zip(deaths).map(f => Array(f._1, f._2))))

            else
                           None
        //Logger.info("paris={}", pairs.get("-3:, :"))

        pairs
    def getHist(arr: NDArray, bins: Int = 50, binarization: Boolean = false) =
        val hist = np.histogram2d(arr.get(":, 0").toNumpy(np), arr.get(":, 1").toNumpy(np), bins=bins, density=false)
        //Logger.info("hist={}", hist)
        //Logger.info("hist={}", hist.bracketAccess(0))
        hist
    def drawPdHist(hist: me.shadaj.scalapy.py.Dynamic, filename: String = "figure.png", title: String = "") =
        val H = hist.bracketAccess(0)
        val xedges = hist.bracketAccess(1)
        val yedges = hist.bracketAccess(2)
        //if binarize then
        //    H = binarize(H, 0.001)
        val extent = py.Dynamic.global.list(Seq(xedges.bracketAccess(0), xedges.bracketAccess(-1), yedges.bracketAccess(0), yedges.bracketAccess(-1)).toPythonProxy)
        plt.imshow(H.T, origin="lower", interpolation="nearest", extent=extent, cmap="tab20")
        plt.title(title)
        plt.savefig(filename, dpi=96)
        //if is_env_notebook():
        //    plt.show()


    def getFloatFromNDArray(x: NDArray): Double =
        x.getDataType match
            case DataType.FLOAT32 => x.getFloat().toDouble
            case DataType.FLOAT64 => x.getDouble()

    //def getPdScatter(title: String, pd: NDArray) =
    def getPdScatter(target: ActionTarget, startIndex: Int, pd: NDArray) =
        val titleString = f"${target} pd-${startIndex}"
        val x = pd.get(":, 0")
        val y = pd.get(":, 1")
        val xmin = getFloatFromNDArray(x.min)
        val xmax = getFloatFromNDArray(x.max)
        val ymin = getFloatFromNDArray(y.min)
        val ymax = getFloatFromNDArray(y.max)
        val size = pd.getShape.size
        //Logger.info("xmin: {}", xmin)
        //Logger.info("xmax: {}", xmax)
        //Logger.info("count: {}", size)
        /*
        #value=pd.histogram((xmin, xmax), size).values
        #print("value", value)
        #fig, ax = plt.subplots()
        #cax=ax.imshow(value, cmap="Paired", origin="lower")
        #cbar = fig.colorbar(cax)
        #print(cbar)
        #plt.savefig("{}-pd.png".format(base_name))
         */
        val axisrange = (xmin.min(ymin), xmax.max(ymax))
        val scatter = go.Scatter(x=x.toNumpy(np), y=y.toNumpy(np), name=titleString, mode="markers",
                                 showlegend = false)
        (scatter, axisrange)
    //def drawPd(title: String, basename: String, pd: NDArray) =
    def drawPd(target: ActionTarget, startIndex: Int, pd: NDArray) =
        val titleString = f"${target} pd-${startIndex}"
        val filename = getFilename(target, "pd", startIndex)
        val (scatter, axisrange) = getPdScatter(target, startIndex, pd)
        val data = py.Dynamic.global.list(Seq(scatter).toPythonProxy)
        //Logger.info("Scatter: {}", scatter)
        //Logger.info("data: {}",py.Dynamic.global.list(Seq(scatter).toPythonProxy))
        val fig = go.Figure(
            data=data,
            layout=py.Dynamic.global.dict(
                            scene=py.Dynamic.global.dict(
                                xaxis=py.Dynamic.global.dict(visible=true), 
                                yaxis=py.Dynamic.global.dict(visible=true)),
                        height=400, width=400)
        )
        fig.update_xaxes(range=axisrange)
        fig.update_yaxes(range=axisrange)
        fig.update_layout(title=titleString)
        //if is_env_notebook():
        //    fig.show()
        fig.write_image(f"${filename}.png")
    //def savePdHist(titleString: String, filename: String, pd: NDArray) =
    def savePdHist(target: ActionTarget, startIndex: Int, pd: NDArray) =
        val titleString = f"${target} hist-${startIndex}"
        val filename = getFilename(target, "hist", startIndex)
        //val titleString = f"${dirName}. sampled-pf-pd-histogram(${startIndex})"
        //var filename = f"${dirName}/${baseName}-hist-${startIndex}.png"
        //val hist = getHist(pd, title = titleString, filename = filename)
        val hist = getHist(pd)
        //Logger.info("hist:{}",hist)
        //Logger.info("hist.size:{}",hist.bracketAccess(0).shape)
        drawPdHist(hist, title = titleString, filename = f"${filename}.png")
        Logger.tag("DEBUG").info("Saving {}", f"${filename}.npy")
        npSave(f"${filename}.npy", hist.bracketAccess(0))

    /**
      * @param target: ActionTarget
      * @param state: String
      * @param startIndex: Int
      * @return "yyyy-MM-dd/heartrate-sampled-${target}-${state}-${startIndex}"
      * */
    def getFilename(target: ActionTarget, state: String, startIndex: Int) =
        val baseName = f"heartrate-sampled-${target}"
        val day = (startIndex / (60*60*24)).toInt
        val targetDate = Property.startDateTime + day.days
        val dirName = targetDate.toString("yyyy-MM-dd")
        var filename = f"${dirName}/${baseName}-${state}-${startIndex}"
        filename

    def savePcs(target: ActionTarget, startIndex: Int, pcs: NDArray) = 
        val baseName = f"heartrate-sampled-${target}"
        val titleString = f"${target} pc-${startIndex}"
        val filename = getFilename(target, "pc", startIndex)
        Logger.tags("NOTICE", "DEBUG").info("Saving {}", filename)
        //np.save(filename, pcs.toNumpy(np))
        npSave(filename, pcs)
    def pcsExists(target: ActionTarget, startIndex: Int) =
        val baseName = f"heartrate-sampled-${target}"
        val filename = getFilename(target, "pc", startIndex)
        File(filename).exists
    /**
      * 出力例: 2023-02-17/heartrate-sampled-sleep-pd-27315.npy
      * */
    def savePd(target: ActionTarget, startIndex: Int, pd: NDArray) = 
        //val titleString = f"${target} pc-${startIndex}"
        val filename = getFilename(target, "pd", startIndex)
        Logger.tags("NOTICE", "DEBUG").info("Saving {}", filename)
        //np.save(filename, pd.toNumpy(np))
        npSave(filename, pd)
    def pdExists(target: ActionTarget, startIndex: Int) = 
        val titleString = f"${target} pc-${startIndex}"
        val filename = getFilename(target, "pd", startIndex)
        File(filename).exists

    def drawPcpd(target: ActionTarget, startIndex: Int, pcs: NDArray, pd: NDArray) =
        Logger.tags("NOTICE").info("STARTINDEX in drawPdpd={}, pcs={}, pd={}", startIndex, 
                pcs.get("0:3, :"), pd.get("0:3, :"))
        //filename = f"${dirName}/${baseName}-pc-${startIndex}"
        //draw_pcs(title_string, filename, pcs)
        //from plotly.subplots import make_subplots
        val subplots = py.module("plotly.subplots")
        //Logger.debug(pyDict(`type`="scene", b=0.1))
        //Logger.debug(pyDict(`type` = "xy"))
        //Logger.debug(py.Dynamic.global.list(Seq((pyDict(`type`="scene", b=0.1), pyDict(`type` = "xy"))).toPythonProxy))
        val specs = py.Dynamic.global.list(Seq(py.Dynamic.global.list((pyDict(`type`="scene", b=0.0), pyDict(`type` = "xy")))).toPythonProxy)
        //Logger.debug("specs={}",specs)
        val fig = subplots.make_subplots(rows=1, cols=2,
                           ///specs=[[{"type": "scene", "b": 0.1}, {"type": "xy"}]],
                           specs = specs,
                           column_widths= pyList(Seq(50, 40)),
                           //row_heights=[600],
                           //insets=[{"cell":(1,1), "l": 0.5}, {"cell":(1, 2), "w": 0.1}]
                           )
        fig.add_trace(p3d.PointCloud(pcs.toNumpy(np), color="red"), row=1, col=1)
        //fig.update_layout(margin=py.Dynamic.global.dict(l=65, r=50, b=65, t=90), row=1, col=1)
        //fig = go.Figure(
        //        data=[p3d.PointCloud(pcs, color="red")],
        //        layout=dict(scene=dict(xaxis=dict(visible=True), yaxis=dict(visible=True), zaxis=dict(visible=True)))
        //    )
        //fig.update_layout(title=title_string, autosize=False,
        //                      width=500, height=500,
        //                      margin=dict(l=65, r=50, b=65, t=90), row=1, col=1)

        //fig.show()
        //fig.write_image("{}-pc.png".format(base_name))
        //val (scatter, axisrange) = getPdScatter(titleString, pd)
        val (scatter, axisrange) = getPdScatter(target, startIndex, pd)
        fig.add_trace(
            scatter, row=1, col=2
        )
        val titleString = getFilename(target, "pcpd", startIndex)
        fig.update_layout(width=700, height=400, title = py.Dynamic.global.dict(text = titleString))
        fig.update_xaxes(range=axisrange, row=1, col=2)
        fig.update_yaxes(range=axisrange, row=1, col=2, scaleanchor = "x", scaleratio = 1)
        //if is_env_notebook():
        //    fig.show()
        val filename = getFilename(target, "pcpd", startIndex)
        fig.write_image(f"${sessionName}/${filename}.png")

    def getClusteringTarget(target: ActionTarget, topn_labels: Int = 10, topn_clusters: Int = 2, reverse: Boolean =true) =
        val dirName = Property.startDateTime.toString("yyyy-MM-dd")
        val baseName = f"heartrate-sampled-${target}"
        Logger.tags("NOTICE").info("target={}", target)
        Logger.tags("NOTICE").info("dirName={}", dirName)
        val l = File(f"${sessionName}/${dirName}").glob(f"${baseName}-hist-*.npy").toArray
        Logger.tags("NOTICE").info("original files: {}", l.toSeq)
        val simmatrix = manager.zeros(Shape(l.size, l.size), DataType.FLOAT32)
        // ここでは，パスにセッションディレクトリが既に含まれいるから，npLoadは使わない
        for i <- Range(0, l.size.toInt - 1) do
            val target1 = manager.create(np.load(l(i).pathAsString).as[Seq[Seq[Double]]])
            for j <- Range(0, l.size.toInt - 1) do
                val target2 = manager.create(np.load(l(j).pathAsString).as[Seq[Seq[Double]]])

                //target1 = binarize(target1, 0.001)
                //target2 = binarize(target2, 0.001)
                val sim = target1.jfip(target2)
                simmatrix.set(NDIndex(i,j), 1.0 - sim)
        Logger.tags("NOTICE", "INFO").info("simmatrix: {}", simmatrix)
        val cluster = py.module("sklearn.cluster")
        val targetFiles = ListBuffer.empty[File]
        if l.size > 0 then
            val model = cluster.AgglomerativeClustering(affinity="precomputed", linkage="complete", n_clusters=5).fit(simmatrix.toNumpy(np))

            Logger.info("cluster: {}", model.labels_)
      
            val labels_counts = np.unique(model.labels_, return_counts = true)
            val (labels, counts) = (labels_counts.bracketAccess(0).as[Seq[Int]],labels_counts.bracketAccess(1).as[Seq[Int]])
            Logger.info("labels={}", labels)
            Logger.info("counts={}", counts)
            val countedLabels = labels.zip(counts).sortBy((f, g) => g).reverse
            // INFO: List((0,13), (1,4), (4,2), (3,1), (2,1))
            Logger.info(countedLabels)
            val targetClusters = countedLabels.take(3)
            Logger.info("target={}", targetClusters)
            for i <- targetClusters do
                Logger.info(i)
                val file = l(i._1.toInt)
                Logger.info(file)
                targetFiles.append(file)
        Logger.tags("NOTICE").info("final target files={}", targetFiles)
        targetFiles.toSeq
/*
        counted_labels = sorted(zip(labels, counts), key=itemgetter(1), reverse=reverse)
        print("sorted counted labels", counted_labels)
        target_clusters = counted_labels[:topn_clusters]
        print("target_clusters", target_clusters)
        target_files = []
        for i in target_clusters:
            for j, label in enumerate(model.labels_):
                if i[0] == label:
                    target_files.append(l[j])
        return target_files[:topn_labels]
 */
    def createSimirarityMatrix(l: Seq[better.files.File]) =
        val dirName = Property.startDateTime.toString("yyyy-MM-dd")
        //base_name = "heartrate-sampled-{}".format(target)
        //l = glob.glob("{}/sleep-hist-*.npy".format(dir_name))
        //l += glob.glob("{}/test-hist-*.npy".format(dir_name))
        //l += glob.glob("{}/active-hist-*.npy".format(dir_name))
        val simmatrix = manager.zeros(Shape(l.size, l.size), DataType.FLOAT32)
        Logger.tags("NOTICE", "DEBUG").debug("simmatrix slot={}", simmatrix)
        // 引数で渡されるのはgetClusteringTargetの戻り値で，セッションディレクトリが含まれているからnpLoadは使わない
        for i <- Range(0, l.size) do
            val target1 = manager.create(np.load(l(i).path.toString).as[Seq[Seq[Double]]])
            for j <- Range(0, l.size) do
                val target2 = manager.create(np.load(l(j).path.toString).as[Seq[Seq[Double]]])
                val sim = target1.jfip(target2)
                simmatrix.set(NDIndex(i.toLong,j.toLong),sim)
        print(simmatrix)
        plt.imshow(simmatrix.toNumpy(np))
        val title = f"${Property.startDateTime.toString("yyyy-MM-dd")} sampled-pf-pd-histogram-simmatrix"
        val graphFilename = f"${dirName}/simmatrix.png"
        plt.title(title)
        plt.savefig(graphFilename, dpi=96)
        val npyFilename = f"${dirName}/simmatrix.npy"
        //np.save(npyFilename, simmatrix.toNumpy(np))
        npSave(npyFilename, simmatrix)
        
    def checkSimirarityMatrix =
        val dirName = Property.startDateTime.toString("yyyy-MM-dd")
        val npyFilename = f"${dirName}/simmatrix.npy"
        Logger.tags("DEBUG").debug("check file={}", npyFilename)
        File(npyFilename).exists

    /**
    # 0 -- len(pc) - delay_window
    # 1 -- len(pc) - delay_window + 1
    # 2 -- len(pc) - delay_window + 2
    # 3 -- len(pc) - delay_window + 3
    # 4 -- len(pc) - delay_window + 4
    # という遅延埋込ポイントクラウドを作る
    # サイズがdelay_windowだけ小さくなる
      * */
    def createEmbedding(pcRaw: NDArray) =
        val pc = pcRaw.toType(DataType.FLOAT32, false)
        // logger.debug("input=%s", pc.shape)
        //Logger.debug("input={}", pc.getShape)
        //Logger.debug("input={}", pc)
        val dataSize = pc.getShape.get(1)
        val blockSize = pc.getShape.get(0) - Property.delayWindow
        val totalBlockSize = blockSize * Property.delayWindow
        val pcs = manager.create(Shape(totalBlockSize, dataSize + 1), DataType.FLOAT32) // append z-axis
        for j <- Range(0, Property.delayWindow) do
            //val pc1 = pc.get(f"${j}: ${-Property.delayWindow + j}, :")
            val pc1 = pc.get(NDIndex().addSliceDim(j, j + blockSize)
                                      .addAllDim).toType(DataType.FLOAT32, false)
            //pc1[:, 0] = pc[0: -delay_window, 0] # 時間は最初のに合わせる
            //Logger.debug("block={}", pc1.getShape)
            //Logger.debug("block={}", pc1)
            if pc1.getShape.get(0) == pc.getShape.get(0) - Property.delayWindow then
                pc1.set(NDIndex().addAllDim.addIndices(0), 
                            pc.get(NDIndex().addSliceDim(0, blockSize).addIndices(0)))
                //Logger.debug("prepared block={}", pc1.getShape)
                //Logger.debug("prepared block={}", pc1)
                val z = manager.full(Shape(blockSize), j.toFloat).toType(DataType.FLOAT32, false) // z-axis
                //Logger.debug("prepared z={}", z.getShape)
                //Logger.debug("prepared z={}", z)
                //pcs.stapcs, np.insert(pc1, 3, z, axis=1) , axis=0)
                val index = j * blockSize
                if pcs.getShape.get(0) > pc1.getShape.get(0) &&
                   pcs.getShape.get(1) == pc1.getShape.get(1) + 1 then
                    pcs.set(NDIndex().addSliceDim(index, index + blockSize)
                                     .addSliceDim(0, dataSize), pc1)
                if pcs.getShape.get(0) > z.getShape.get(0) then
                    pcs.set(NDIndex().addSliceDim(index, index + blockSize)
                                     .addIndices(dataSize + 0), z)
            else
                Logger.warn("pc1.getShape.get(0)({}) != pc.getShape.get(0) - Property.delayWindow ({})",
                             pc1.getShape.get(0), pc.getShape.get(0) - Property.delayWindow)       
        //Logger.debug("output={}", pcs.getShape)
        //Logger.debug("output={}", pcs.get("0:3, :"))
        //Logger.debug("output={}", pcs.get("-3:, :"))
        pcs
    /**
    # f(0), f(1), f(2)
    # f(1), f(2), f(3)
    # f(2), f(3), f(4)
    # という遅延埋込ポイントクラウドを作る
    # サイズが2だけだけ小さくなる
    # 最終的に時間で行動識別をするので，時間情報は消せない
      * 入力
      * [[t0, f0, g0], [t1, f1, g1], [t2, f2, g2]...
      * 出力
      * [ [[t0, f0, g0], [t1, f1, g1], [t2, f2, g2]], 
      *   [[t1, f1, g1], [t2, f2, g2], [t3, f3, g3]], 
      * ]
      * */
    def createSlidingWindowEmbedding(pcRaw: NDArray, timeDelay: Int = 2) =
        val pc = pcRaw.toType(DataType.FLOAT32, false)
        Logger.tags("NOTICE").debug("in raw={}", pc)
        val timeDelay = 2
        val dimension = 3
        // logger.debug("input=%s", pc.shape)
        //Logger.debug("input={}", pc.getShape)
        //Logger.debug("input={}", pc)
        val dataSize = pc.getShape.get(1)  // time + data such as min, max
        //val blockSize = pc.getShape.get(0) - Property.delayWindow
        // 例で言うf(2)の2=5 - 3 - 1 + 1
        // ここまでループしながら，f(t+delta), f(t+2delta), f(t+3delta)を切り出す
        // 3次元は固定でもいい
        val blockSize = pc.getShape.get(0) - dimension - timeDelay + 1
        Logger.tags("NOTICE", "DEBUG").debug("blockSize={}", blockSize)
        //val totalBlockSize = blockSize * Property.delayWindow
        //val pcs = manager.create(Shape(totalBlockSize, dataSize + 1), DataType.FLOAT32) // append z-axis
        
        val pcs = manager.create(Shape(blockSize, dimension, dataSize), DataType.FLOAT32) // remove time
        //for j <- Range(0, Property.delayWindow) do
        for j <- Range(0, blockSize.toInt) do
            //val pc1 = pc.get(f"${j}: ${-Property.delayWindow + j}, :")
            //val pc1 = pc.get(NDIndex().addSliceDim(j, j + blockSize)
            //                         .addAllDim).toType(DataType.FLOAT32, false)
            val pc1 = pc.get(NDIndex().addSliceDim(j, j + dimension * timeDelay, timeDelay)
                                     .addAllDim).toType(DataType.FLOAT32, false)
            //Logger.tags("NOTICE").debug("in={}", pc1)
            //Logger.debug("block={}", pc1.getShape)
            //Logger.tags("NOTICE").debug("block={}", pc1)
            if true then
                //pc1.set(NDIndex().addAllDim.addIndices(0), 
                //            pc.get(NDIndex().addSliceDim(0, blockSize).addIndices(0)))
                //pc1[:, 0] = pc[0: -delay_window, 0] # 時間は最初のに合わせる
                //Logger.debug("prepared block={}", pc1.getShape)
                //Logger.debug("prepared block={}", pc1)
                //val z = manager.full(Shape(blockSize), j.toFloat).toType(DataType.FLOAT32, false) // z-axis
                //Logger.debug("prepared z={}", z.getShape)
                //Logger.debug("prepared z={}", z)
                //pcs.stapcs, np.insert(pc1, 3, z, axis=1) , axis=0)
                /*
                val index = j * blockSize
                if pcs.getShape.get(0) > pc1.getShape.get(0) &&
                   pcs.getShape.get(1) == pc1.getShape.get(1) + 1 then
                    pcs.set(NDIndex().addSliceDim(index, index + blockSize)
                                     .addSliceDim(0, dataSize), pc1)
                if pcs.getShape.get(0) > z.getShape.get(0) then
                    pcs.set(NDIndex().addSliceDim(index, index + blockSize)
                                     .addIndices(dataSize + 0), z)
                 */
                val embeddingValue = pc1.get(NDIndex().addAllDim
                                          .addAllDim)
                                          .reshape(Shape(1, dimension, dataSize))
                //Logger.tags("NOTICE").debug("embedding block={}", embeddingValue)
                //Logger.tags("NOTICE").debug("dimension={}", dimension)
                //Logger.tags("NOTICE").debug("j={}", j)
                pcs.set(NDIndex().addIndices(j).addSliceDim(0, dimension)
                         .addSliceDim(0, dataSize),
                         embeddingValue)
                //Logger.tags("NOTICE").debug("new pcs={}", pcs)
            else
                Logger.warn("pc1.getShape.get(0)({}) != pc.getShape.get(0) - Property.delayWindow ({})",
                             pc1.getShape.get(0), pc.getShape.get(0) - Property.delayWindow)       
        //Logger.debug("output={}", pcs.getShape)
        //Logger.debug("output={}", pcs.get("0:3, :"))
        //Logger.debug("output={}", pcs.get("-3:, :"))
        pcs
    def createSupervisedData(plan: String) =
        createSleepPDDataset(plan)
        createActivePDDataset(plan)
    //def createTestData(plan: String) =


object PersistenceDiagramTransform:

    @main
    def createSupervisedData() =
        Logger.tags("NOTICE", "INFO").info("=============== Start ==================", "")
        val pdt = PersistenceDiagramTransform()
        //pdt.setSession("ut session")
        //pdt.setSession("ipsj2023-rev01")
        //pdt.setSession("ipsj2023-rev02")
//        pdt.loadPdDataset("sleep", "B")
        pdt.createSupervisedData("C")
        Logger.tags("NOTICE", "INFO").info("=============== End ==================", "")

    @main
    def importSupervisedDataFrom(oldSessionName: String) =
        Logger.tags("NOTICE", "INFO").info("=============== Start ==================", "")
        val pdt = PersistenceDiagramTransform()
        val oldFC = File(oldSessionName)
        val newFC = File(Property.sessionName)
        Logger.tags("NOTICE", "INFO").info("oldFC(oldSessionName)={}", oldFC)
        Logger.tags("NOTICE", "INFO").info("newFC(Property.SessionName)={}", newFC)
        //val targetDirs = oldFC.globRegex("^\\d\\d\\d\\d\\-\\d\\d\\-\\d\\d$".r) // yyyy-mm-dd
        val targetDirs = oldFC.globRegex("\\d\\d\\d\\d-\\d\\d-\\d\\d$".r).toList // yyyy-mm-dd
        //val targetDirs = oldFC.glob("*") // yyyy-mm-dd
        Logger.tags("NOTICE", "INFO").info("target dir list={}", targetDirs)
        targetDirs.foreach{f =>
            f.createDirectoryIfNotExists(true)
            val npyFiles = f.glob("*.npy").toList // hearrate-sampled-*-*-*.npy
            Logger.tags("NOTICE", "INFO").info("target file list={}", npyFiles)
            npyFiles.foreach{g => // /mnt/disks/sdb/home2/tetsu.sato/FitbitWebAPI/ipsj2023-rev02/2022-09-08/heartrate-sampled-active-hist-667800.npy
                Logger.tags("NOTICE", "INFO").info("g.path is {}, g.name is {}, g.parent is {}", g.path, g.name, g.parent)
                Logger.tags("NOTICE", "INFO").info("g.parent is {}, g.parent.name is {}", g.parent, g.parent.name)
                val dateDirName = g.parent.name
                val destDir = File(s"${newFC}/${dateDirName}")
                Logger.tags("NOTICE", "INFO").info("Duplicate copy from {}({}) to dir {}", g, g.path, 
                               destDir)
                destDir.createDirectoryIfNotExists(true)
                g.copyToDirectory(destDir)(File.LinkOptions.default,File.CopyOptions.apply(true))
            }
        }
        
        //pdt.createSupervisedData("C")
        Logger.tags("NOTICE", "INFO").info("=============== End ==================", "")



    @main
    def createSimilarityMatrixData() =
        Logger.tags("NOTICE", "INFO").info("=============== Start ==================", "")
        val pdt = PersistenceDiagramTransform()
        if pdt.checkSimirarityMatrix then
            Logger.tags("NOTICE").info("Simirarity matrix file is exists. skip.", "")
        else
            //pdt.createSupervisedData("C")
            var l = scala.collection.mutable.ArrayBuffer.empty[File]
            val simMatrixSleepData = pdt.getClusteringTarget(ActionTarget.sleep, topn_clusters=2, topn_labels=5)
            val simMatrixActiveData = pdt.getClusteringTarget(ActionTarget.active, topn_clusters=2, topn_labels=5)
            Logger.tags("INFO").info("# of sleep data file for creating simirarity matrix = {}", simMatrixSleepData.size)
            Logger.tags("INFO").info("# of active data file for creating simirarity matrix = {}", simMatrixActiveData.size)
            l ++= pdt.getClusteringTarget(ActionTarget.sleep, topn_clusters=2, topn_labels=5)
            l ++= pdt.getClusteringTarget(ActionTarget.active, topn_clusters=2, topn_labels=5)
            pdt.createSimirarityMatrix(l.toSeq)
        Logger.tags("NOTICE", "INFO").info("=============== End ==================", "")

