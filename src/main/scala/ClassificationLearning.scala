package org.example
/*
import com.github.nscala_time.time.Imports._
import scala.annotation.targetName
import scala.collection.mutable.ListBuffer
import ai.djl.nn.SequentialBlock
 */
import better.files._
import me.shadaj.scalapy.py
import me.shadaj.scalapy.py.SeqConverters


import scala.jdk.CollectionConverters._
import ai.djl.ndarray.{NDArrays, NDArray, NDManager, NDList} 
import ai.djl.ndarray.index.NDIndex
import ai.djl.ndarray.types.{Shape, DataType}

import ai.djl.Device

import ai.djl.nn.{Blocks, Activation}
import ai.djl.nn.core.Linear
import ai.djl.nn.*
import ai.djl.nn.norm.Dropout
import ai.djl.nn.norm.BatchNorm
import ai.djl.nn.recurrent.LSTM
import ai.djl.Model
import ai.djl.training.DefaultTrainingConfig
import ai.djl.training.listener.SaveModelTrainingListener
import ai.djl.training.evaluator.Accuracy
import ai.djl.training.listener.TrainingListener
import ai.djl.training.loss.Loss
import ai.djl.training.dataset.ArrayDataset
import ai.djl.training.EasyTrain
import java.util.function.Function
import ai.djl.training.util.ProgressBar
import ai.djl.translate.{Translator, TranslatorContext}
import ai.djl.metric.Metrics

import ai.djl.inference.Predictor

import org.tinylog.Logger
import java.nio.file.Paths
import org.nd4j.linalg.api.ops.impl.loss.SoftmaxCrossEntropyLoss
import org.nd4j.linalg.api.ops.impl.transforms.custom.SoftMax
import ai.djl.training.dataset.RandomAccessDataset



class ClassificationLearning(mode: LearningMode):
    //val pdDimension = 2 // fix
    //val pdLength = 256 // try
    //val inputSize = pdDimension * pdLength
    val outputSize = 1 // クラスインデックスじゃなくても，アクティブ度とかでもいいかも？←イイね
    val batchSize = 32
    val block = new SequentialBlock

    def getBlock =
        block.add(Blocks.batchFlattenBlock(Property.inputSize))
        //block.add(Blocks.batchFlattenBlock())
        //block.add(Blocks.batchFlattenBlock(2))
        //block.add(BatchNorm.builder().build())

        block.add(Linear.builder().setUnits(1024).build())
        block.add(Activation.relu: Function[NDList, NDList])
//        block.add(Dropout.builder().optRate(0.2).build())
        block.add(Linear.builder().setUnits(1024).build())
//        block.add(BatchNorm.builder().build())
        block.add(Activation.relu: Function[NDList, NDList])
//        block.add(Dropout.builder().optRate(0.2).build())
//        block.add(BatchNorm.builder().build())
//        block.add(Linear.builder().setUnits(512).build())
//        block.add(Activation.relu: Function[NDList, NDList])
//        block.add(Dropout.builder().optRate(0.2).build())
        block.add(Linear.builder().setUnits(outputSize).build())
//        block.addSingleton(nd => nd.softmax(1))
        block

    def getTraingingConfig(outputDir: String = "log") =
        val listener = new SaveModelTrainingListener(outputDir)
        listener.setSaveModelCallback(
            trainer => {
                val result = trainer.getTrainingResult
                val model = trainer.getModel
                val accuracy = result.getValidateEvaluation("Accuracy").toDouble
                model.setProperty("Accuracy", f"${accuracy}%.5f")
                model.setProperty("Loss", f"${result.getValidateLoss().toDouble}%.5f")
            }
        )

        val config = new DefaultTrainingConfig(Loss.l2Loss)
//        val config = new DefaultTrainingConfig(Loss.softmaxCrossEntropyLoss("loss", 1, -1, false, false))
            .addEvaluator(new Accuracy())
            //.addTrainingListeners(TrainingListener.Defaults.logging(outputDir)(0))
            .addTrainingListeners(TrainingListener.Defaults.logging(outputDir): _*)
            .addTrainingListeners(listener)
        config

    def prepare(plan: String): Unit =
        import Property.*
        val pdt = PersistenceDiagramTransform()
        val model = Model.newInstance("mlp")

        var learningData: NDArray = null
        var learningLabel: NDArray = null
        var sleepData: NDArray = null
        var activeData: NDArray = null

        if pdt.npExists(learningDataFile) then
            Logger.tags("NOTICE", "INFO").info(
                                     """Learning data file already exists in the session directory({}).
                                       |Nothing to Do.
                                     """.stripMargin, 
                                     learningDataFile)
            return

        else
            Logger.tags("NOTICE").info(
                                     """Learning data file does not exist in the session directory({}).
                                       |Lets creating
                                     """.stripMargin, 
                                     learningDataFile)
            plan match
                case "C" =>
                    Logger.tags("INFO").info("plan is C. {}", plan)
                    Logger.tags("INFO").info("sleepData={}", sleepData)
                    // 2次元PDベクトルをフラットにした1次元ベクトルを並べた2次元ベクトル
                    sleepData = pdt.loadSleepPdDataset("C")
                                 .regularizeHolizontally(Property.pdRegularizationUnit.toFloat)
                    Logger.tags("INFO").info("now sleepData={}", sleepData)
                    Logger.tags("INFO").info("returned sleep data shape={}", sleepData.getShape)
                    // 2次元PDベクトルをフラットにした1次元ベクトルを並べた2次元ベクトル
                    activeData = pdt.loadActivePdDataset("C")
                                 .regularizeHolizontally(Property.pdRegularizationUnit.toFloat)
            Logger.tags("INFO").info("input sleep data shape={}", sleepData.getShape)

            if sleepData.getShape.get(0) > activeData.getShape.get(0) then
               val ratio = activeData.getShape.get(0).toDouble/sleepData.getShape.get(0).toDouble
               sleepData = sleepData.sample(ratio)
               Logger.tags("INFO").info("sampled sleepData.shape={}", sleepData.getShape)
               Logger.tags("INFO").info("activeData.shape={}", activeData.getShape)

            //// ratio=3.0だから，3倍に水増ししてる？
            //sleepData = sleepData.sample(3.0, true)
            //activeData = activeData.sample(3.0, true)
            npSave(sleepPdDataFile, sleepData, pdt)
            npSave(activePdDataFile, activeData, pdt)


            val sleepLabels = pdt.manager.full(Shape(sleepData.getShape.get(0), 1), 0.0f, DataType.FLOAT32)
            //val sleepLabels = pdt.manager.create(Array.fill(sleepData.getShape.get(0).toInt)(Array(1.0, 0.0))).toType(DataType.FLOAT32, false) // not require for SoftmaxCrossEntropyLoss with sparse_label = true(default)
            //val sleepLabels = pdt.manager.full(Shape(sleepData.getShape.get(0)), 0, DataType.UINT8)
            Logger.tags("NOTICE", "INFO").info("sleep data shape={}", sleepData.getShape)
            Logger.tags("NOTICE", "INFO").info("sleep data(regularized)={}", sleepData.get("0:3,0:5"))
            Logger.tags("NOTICE", "INFO").info("labels={}", sleepLabels.getShape)
            Logger.tags("NOTICE", "INFO").info("labels={}", sleepLabels.get("0:3,:"))
            val activeLabels = pdt.manager.full(Shape(activeData.getShape.get(0), 1), 1.0f, DataType.FLOAT32)
            //val activeLabels = pdt.manager.create(Array.fill(activeData.getShape.get(0).toInt)(Array(0.0, 1.0))).toType(DataType.FLOAT32, false)
            //val activeLabels = pdt.manager.full(Shape(activeData.getShape.get(0)), 1, DataType.UINT8)
            Logger.tags("NOTICE", "INFO").info("active data shape={}", activeData.getShape)
            Logger.tags("NOTICE", "INFO").info("active data(regularized)={}", activeData.get("0:3,0:5"))
            Logger.tags("DEBUG").debug("concat labels={}", sleepLabels.concat(activeLabels).getShape)
            Logger.tags("DEBUG").debug("concat labels={}", sleepLabels.concat(activeLabels))

            learningData = sleepData.concat(activeData)
            learningLabel = sleepLabels.concat(activeLabels)
            pdt.npSave(learningDataFile, learningData)
            pdt.npSave(learningLabelFile, learningLabel)


    def train: Unit =
        import Property.*
        val pdt = PersistenceDiagramTransform()
        val model = Model.newInstance("mlp")

        var learningData: NDArray = null
        var learningLabel: NDArray = null
        var sleepData: NDArray = null
        var activeData: NDArray = null

        //if File(learningDataFile).exists then
        if npExists(learningDataFile, pdt) then
            learningData = npLoad(learningDataFile, pdt)
            learningLabel = npLoad(learningLabelFile, pdt)
            sleepData = npLoad(sleepPdDataFile, pdt)
            activeData = npLoad(activePdDataFile, pdt)
        else
            Logger.tags("NOTICE", "INFO").info(
                                     """Learning data file does not exists({}).
                                       |Without learning data, learning cannot proceed.
                                       |Exit.
                                     """.stripMargin, 
                                     learningDataFile)
            return

        // learningData is the data combined from the active and sleep data
        // The active and sleep data is the persistence diagram data
        // So, time data is eliminated.
        val dataset = new ArrayDataset.Builder()
               .setData(learningData)
               .optLabels(learningLabel)
               .setSampling(batchSize, true)
               .build
        dataset.prepare(new ProgressBar())

        val trainAndTest = dataset.randomSplit(4, 4, 2)
        val sessionModelFile = File(s"${sessionName}/${modelFile.toFile.getName}").path
        if sessionModelFile.toFile.exists then
            Logger.tags("NOTICE", "INFO").info(
                                     """Model file already exists({}).
                                       |Nothing to Do.
                                     """.stripMargin, 
                                     sessionModelFile)
            return
        else
            val block = getBlock
            Logger.tags("NOTICE", "INFO").info("model={}", model)
            model.setBlock(block)
            model.setDataType(DataType.FLOAT32)

            val trainerConfig = getTraingingConfig("log")
            val trainer = model.newTrainer(trainerConfig)
            //trainer.setMetrics(new Metrics())


            //trainer.initialize(new Shape(Property.inputSize, outputSize))
            trainer.initialize(new Shape(1, Property.inputSize))

            Logger.tags("INFO").info("sample forward(prediction) from sleep = {}", trainer.forward(NDList(sleepData.get(NDIndex().addIndices(0).addAllDim))).get(0))
            Logger.tags("INFO").info("sample forward from active = {}", trainer.forward(NDList(activeData.get(NDIndex().addIndices(0).addAllDim))).get(0))

            val epoch = 5400
            //val epoch = 7400
            //trainAndTest(0).prepare(new ProgressBar())
            //trainAndTest(1).prepare(new ProgressBar())

/*
            Logger.tags("INFO").info("epoch = {}", epoch)
            Logger.tags("NOTICE").debug("train={}", trainAndTest(0).size)
            Logger.tags("NOTICE", "DEBUG").debug("train data shape={}", trainAndTest(0).getData(pdt.manager).asScala.iterator.next.getData.getShapes.toSeq)
            Logger.tags("NOTICE", "DEBUG").debug("train data={}", trainAndTest(0).get(pdt.manager, 0).getData.get(0).get("0:5"))
            Logger.tags("NOTICE", "DEBUG").debug("train label shape={}", trainAndTest(0).getData(pdt.manager).asScala.iterator.next.getLabels.getShapes.toSeq)
            Logger.tags("NOTICE", "DEBUG").debug("train label={}", trainAndTest(0).getData(pdt.manager).asScala.iterator.next.getLabels.get(0).get("0:5"))
            Logger.tags("NOTICE").debug("test={}", trainAndTest(1).size)
            Logger.tags("NOTICE").debug("trainer model={}", trainer.getModel)
            Logger.tags("NOTICE").debug("trainer evaluator={}", trainer.getEvaluators.asScala.toSeq)
 */
            EasyTrain.fit(trainer, epoch, trainAndTest(0), trainAndTest(1))
            Logger.tags("NOTICE", "INFO").info("trainer result={}", trainer.getTrainingResult)

            Logger.tags("NOTICE", "INFO").info("Saving model...({})", sessionModelFile)
            model.save(sessionModelFile, "classificationModel")

    def prediction: Unit =
        import Property.*
        val pdt = PersistenceDiagramTransform()
        val model = Model.newInstance("mlp")

        var learningData: NDArray = null
        var learningLabel: NDArray = null
        var sleepData: NDArray = null
        var activeData: NDArray = null


        //val translator = new Translator[NDList, Float](){
        val translator = new Translator[NDList, NDArray](){
               override def processInput(ctx: TranslatorContext, input: NDList) =
                 //NDList(pdt.manager.create(input))
                 input
               override def processOutput(ctx: TranslatorContext, list: NDList) =
                 list.singletonOrThrow
                 //list.singletonOrThrow.getFloat(0L)
            }

        if npExists(learningDataFile, pdt) then
            learningData = npLoad(learningDataFile, pdt)
            learningLabel = npLoad(learningLabelFile, pdt)
            sleepData = npLoad(sleepPdDataFile, pdt)
            activeData = npLoad(activePdDataFile, pdt)
        else
            Logger.tags("NOTICE", "INFO", "ERROR")
                  .error(
                      """Learning data file{} does not found.
                        |Learning data and labels are used for evaluation and test.
                        |and evaluation.
                      """.stripMargin
                      , learningDataFile)
            return

        val sessionModelFile = File(s"${sessionName}/${modelFile.toFile.getName}").path

        //if File(s"${sessionName}/${modelFile.toFile.getName}").exists then
        if sessionModelFile.toFile.exists then
            Logger.tags("NOTICE", "INFO").info("Model file found({})", sessionModelFile)
            val block = getBlock
            model.setBlock(block)
            model.setDataType(DataType.FLOAT32)
            model.load(sessionModelFile)
            Logger.tags("NOTICE", "INFO").info("loaded model={}", model)
        else
            Logger.tags("NOTICE", "INFO", "ERROR")
                  .error(
                      """Model file{} does not exist.
                        |Model file contains network structure information.
                        |Without it, prediction cannot be proceed.
                      """.stripMargin
                      , sessionModelFile)
            return

        val predictor = model.newPredictor(translator)

        Logger.tags("NOTICE", "INFO").info("original sleep.shape={}", sleepData.getShape.getShape.toSeq)
        Logger.tags("NOTICE", "INFO").info("original sleep={}", sleepData)
        Logger.tags("NOTICE", "INFO").info("==== test for first 10 samples from sleepData and activeData", "")
        for i <- Range(0, 10) do
            //val sleep = sleepData.get(NDIndex().addIndices(i).addAllDim)
            val sleep = sleepData.get(NDIndex().addSliceDim(i, i+1).addAllDim)
            Logger.tags("INFO").info("input sleep={}", sleep.getShape)
            Logger.tags("INFO").info("input sleep={}", sleep)
            if sleep.size > 0 then
                Logger.tags("NOTICE", "INFO").info("score for sleep data=>{}",
                                      predictor.predict(NDList(sleep)).getFloat())
            //val active = activeData.get(NDIndex().addIndices(i).addAllDim)
            val active = activeData.get(NDIndex().addSliceDim(i, i+1).addAllDim)
            //Logger.tags("NOTICE").info("input active={}", active.getShape)
            if active.size > 0 then
                Logger.tags("NOTICE", "INFO").info("score for active data=>{}",
                                          predictor.predict(NDList(active)).getFloat())


        // learningData is the data combined from the active and sleep data
        // The active and sleep data is the persistence diagram data
        // So, time data is eliminated.
        val dataset = new ArrayDataset.Builder()
               .setData(learningData)
               .optLabels(learningLabel)
               .setSampling(batchSize, true)
               .build
        dataset.prepare(new ProgressBar())




        val trainAndTest = dataset.randomSplit(4, 4, 2)


        val validationData = trainAndTest(1)
        Logger.tags("INFO").info("======== validation data ========= n = {}", validationData.size.toInt)
        dataStatistics(validationData, pdt.manager, predictor)

        val testData = trainAndTest(2)
        Logger.tags("INFO").info("======== test data ========= n = {}", testData.size.toInt)
        dataStatistics(testData, pdt.manager, predictor)
/*
        Logger.tags("DEBUG").debug("test data={}", testData)
        Logger.tags("DEBUG").debug("test data size={}", testData.size)
        for i <- testData.getData(pdt.manager).asScala do
            val dataList = i.getData
            val labelList = i.getLabels
            Logger.tags("DEBUG").debug("Test Batch size={}", i.getSize)
            Logger.tags("DEBUG").debug("data size={}", dataList.size)
            Logger.tags("DEBUG").debug("data={}", dataList)
            Logger.tags("DEBUG").debug("label size={}", labelList.size)
            Logger.tags("DEBUG").debug("label={}", labelList)
            val labelarrays = labelList.iterator.asScala
            val ndarrays = dataList.iterator.asScala
            for ndarray <- ndarrays do
                val labelarray = labelarrays.next
                for j <- Range(0, ndarray.getShape.get(0).toInt) do
                    val row = ndarray.get(NDIndex().addIndices(j).addAllDim)
                    val label = labelarray.get(NDIndex().addIndices(j).addAllDim)
                    Logger.tags("DEBUG").debug("label={}", label)
                    Logger.tags("DEBUG").debug("predict={}", predictor.predict(NDList(row)))
 */
        val trainData = trainAndTest(0)
        Logger.tags("INFO").info("======== train data ========= n = {}", trainData.size.toInt)
        dataStatistics(trainData, pdt.manager, predictor)
/*
        Logger.tags("DEBUG").debug("train data={}", trainData)
        Logger.tags("DEBUG").debug("train data size={}", trainData.size)
        for i <- trainData.getData(pdt.manager).asScala do
            val dataList = i.getData
            val labelList = i.getLabels
            Logger.tags("DEBUG").debug("Train Batch size={}", i.getSize)
            Logger.tags("DEBUG").debug("data size={}", dataList.size)
            Logger.tags("DEBUG").debug("data={}", dataList)
            Logger.tags("DEBUG").debug("label size={}", labelList.size)
            Logger.tags("DEBUG").debug("label={}", labelList)
            val labelarrays = labelList.iterator.asScala
            val ndarrays = dataList.iterator.asScala
            for ndarray <- ndarrays do
                val labelarray = labelarrays.next
                for j <- Range(0, ndarray.getShape.get(0).toInt) do
                    val row = ndarray.get(NDIndex().addIndices(j).addAllDim)
                    val label = labelarray.get(NDIndex().addIndices(j).addAllDim)
                    Logger.tags("DEBUG").debug("label={}", label)
                    Logger.tags("DEBUG").debug("predict={}", predictor.predict(NDList(row)))
 */

    def directPrediction(target: ActionTarget, dateTimeStr: String): Double =
        import Property.*
        val pdt = PersistenceDiagramTransform()
        val model = Model.newInstance("mlp")

        var learningData: NDArray = null
        var learningLabel: NDArray = null
        var sleepData: NDArray = null
        var activeData: NDArray = null


        //val translator = new Translator[NDList, Float](){
        val translator = new Translator[NDList, NDArray](){
               override def processInput(ctx: TranslatorContext, input: NDList) =
                 //NDList(pdt.manager.create(input))
                 input
               override def processOutput(ctx: TranslatorContext, list: NDList) =
                 list.singletonOrThrow
                 //list.singletonOrThrow.getFloat(0L)
            }

        if npExists(learningDataFile, pdt) then
            learningData = npLoad(learningDataFile, pdt)
            learningLabel = npLoad(learningLabelFile, pdt)
            sleepData = npLoad(sleepPdDataFile, pdt)
            activeData = npLoad(activePdDataFile, pdt)
        else
            Logger.tags("NOTICE", "INFO", "ERROR")
                  .error(
                      """Learning data file{} does not found.
                        |Learning data and labels are used for evaluation and test.
                        |and evaluation.
                      """.stripMargin
                      , learningDataFile)
            return -100.0

        val sessionModelFile = File(s"${sessionName}/${modelFile.toFile.getName}").path

        //if File(s"${sessionName}/${modelFile.toFile.getName}").exists then
        if sessionModelFile.toFile.exists then
            Logger.tags("NOTICE", "INFO").info("Model file found({})", sessionModelFile)
            val block = getBlock
            model.setBlock(block)
            model.setDataType(DataType.FLOAT32)
            model.load(sessionModelFile)
            Logger.tags("NOTICE", "INFO").info("loaded model={}", model)
        else
            Logger.tags("NOTICE", "INFO", "ERROR")
                  .error(
                      """Model file{} does not exist.
                        |Model file contains network structure information.
                        |Without it, prediction cannot be proceed.
                      """.stripMargin
                      , sessionModelFile)
            return -100.0

        val predictor = model.newPredictor(translator)

        val pd = pdt.createPDDatasetAt(target, "C", dateTimeStr)
        val targetIndex = pdt.getNearestTimeIndex(dateTimeStr)
        Logger.tags("NOTICE", "INFO").info("pd={}, index={}", pd, targetIndex)

        val choicedPdRaw = pdt.manager.create(pdt.choicePD(pd.toNumpy(pdt.np))
                                    .as[Seq[Seq[Double]]])
        Logger.tags("NOTICE", "INFO").info("choiced by indicated algorithm({}) pd={}", 
                        Property.samplingMode, choicedPdRaw)
        val choicedPd = choicedPdRaw
                                    .reshape(1, Property.pdLength * 2) // 2d to 1d
                                  .toType(DataType.FLOAT32, false)
                                 .regularizeHolizontally(Property.pdRegularizationUnit.toFloat)
        Logger.tags("NOTICE", "INFO").info("choiced pd={}", choicedPd)
        Logger.tags("NOTICE", "INFO").info("score for THIS data=>{}",
                                      predictor.predict(NDList(choicedPd)).getFloat())

        pdt.savePd(target, targetIndex, choicedPdRaw)
        

/*
            //val sleep = sleepData.get(NDIndex().addIndices(i).addAllDim)
            val sleep = sleepData.get(NDIndex().addSliceDim(i, i+1).addAllDim)
            Logger.tags("INFO").info("input sleep={}", sleep.getShape)
            Logger.tags("INFO").info("input sleep={}", sleep)
            if sleep.size > 0 then
                Logger.tags("NOTICE", "INFO").info("score for sleep data=>{}",
                                      predictor.predict(NDList(sleep)).getFloat())
            //val active = activeData.get(NDIndex().addIndices(i).addAllDim)
            val active = activeData.get(NDIndex().addSliceDim(i, i+1).addAllDim)
            //Logger.tags("NOTICE").info("input active={}", active.getShape)
            if active.size > 0 then
                Logger.tags("NOTICE", "INFO").info("score for active data=>{}",
                                        predictor.predict(NDList(active)).getFloat())
 */
        0.1

    def npExists(fileName: String, pdt: PersistenceDiagramTransform): Boolean =
         pdt.npExists(fileName)

    def npLoad(fileName: String, pdt: PersistenceDiagramTransform): NDArray =
         val npObje =  pdt.npLoad(fileName)
         val ndarray = pdt.manager.create(npObje.as[Seq[Seq[Double]]])
                                        .toType(DataType.FLOAT32, false)
         ndarray
    def npSave(fileName: String, data: NDArray, pdt: PersistenceDiagramTransform) =
         pdt.npSave(fileName, data)

    /**
      * Following types as statistical data are displayed:
      *   - k: 
      *   - good: A numbef of data that the difference between a label value and a prediction value is less than 0.5
      *   - ratio: good / k
      *   - goodMap: good and ratio data of each label(=0 and 1)
      * The data is consisted from PD data of the active and sleep data
      * So, time information is eliminated.
      * @param data: RandomAccessDataset -- randomSplit ted data 
      * @param manager: NDManager
      * @param predictor: Predictor[NDList, NDArray]
      * */
    def dataStatistics(data: RandomAccessDataset, manager: NDManager, predictor: Predictor[NDList, NDArray]) =
        var k = 0
        val datasetSize = data.size.toInt
        Logger.tags("NOTICE", "INFO", "DEBUG").info("n={}", datasetSize)
        val batchIter = data.getData(manager).asScala.iterator
        val trainLabelMap = scala.collection.mutable.HashMap.empty[Float, Int]
        val predictLabelMap = scala.collection.mutable.HashMap.empty[Float, Int]
        val labelCountMap = scala.collection.mutable.HashMap.empty[Float, Int]
        var good = 0
        val goodMap = scala.collection.mutable.HashMap.empty[Float, Int]
        while batchIter.hasNext do
            val batch = batchIter.next
            val dataListIter = batch.getData.iterator
            val labelListIter = batch.getLabels.iterator
            while dataListIter.hasNext do
                val dataArray = dataListIter.next
                val labelArray = labelListIter.next
                for j <- Range(0, dataArray.getShape.get(0).toInt) do
                    //val index = NDIndex().addIndices(j).addAllDim
                    val index = NDIndex().addSliceDim(j, j+1).addAllDim
                    val label = labelArray.get(index).getFloat()
                    val row = dataArray.get(index)
                    Logger.tags("NOTICE", "INFO").info("data in a batch={}", row)
                    val predict = predictor.predict(NDList(row)).getFloat()
                    Logger.tags("NOTICE", "INFO").info("{}: label={}, row max={}, predict={}", k, label, row.max.getFloat(), predict)
                    labelCountMap.update(label, labelCountMap.getOrElse(label, 0) + 1)
                    if Math.abs(label - predict) < 0.5 then
                        good += 1
                        goodMap.update(label, goodMap.getOrElse(label, 0) + 1)
                    k += 1
        Logger.tags("NOTICE", "INFO", "DEBUG").info("k={}, good={}, ratio={}", k, good, good.toDouble/k)
        Logger.tags("NOTICE", "INFO", "DEBUG").info("goodMap={}", goodMap)
        Logger.tags("NOTICE", "INFO", "DEBUG").info("labelCountMap={}", labelCountMap)
        for i <- labelCountMap.keySet do
             Logger.tags("NOTICE", "INFO", "DEBUG").info("Acuracy {} = {}", i, goodMap(i).toDouble/labelCountMap(i))

object ClassificationLearning:
    @main
    def executePreparation =
        Logger.tags("NOTICE", "INFO", "DEBUG").info("=============== Start ==================", "")
        val cl = ClassificationLearning(LearningMode.normal)
        //cl.setSession("ut session")
        //cl.setSession("ipsj2023-rev01")
        //cl.setSession("ipsj2023-rev02")
        cl.prepare("C")
        Logger.tags("NOTICE", "INFO", "DEBUG").info("=============== End ==================", "")

    @main
    def executeTrain =
        Logger.tags("NOTICE", "INFO", "DEBUG").info("=============== Start ==================", "")
        val cl = ClassificationLearning(LearningMode.normal)
        //cl.setSession("ut session")
        //cl.setSession("ipsj2023-rev01")
        //cl.setSession("ipsj2023-rev02")
        cl.train
        Logger.tags("NOTICE", "INFO", "DEBUG").info("=============== End ==================", "")

    @main
    def executePrediction =
        Logger.tags("NOTICE", "INFO", "DEBUG").info("=============== Start ==================", "")
        val cl = ClassificationLearning(LearningMode.normal)
        //cl.setSession("ipsj2023-rev01")
        //cl.setSession("ipsj2023-rev02")
        //cl.setSession("ut session")
        cl.prediction
        Logger.tags("NOTICE", "INFO", "DEBUG").info("=============== End ==================", "")

    @main
    def executeDirectPrediction =
        Logger.tags("NOTICE", "INFO", "DEBUG").info("=============== Start ==================", "")
        val cl = ClassificationLearning(LearningMode.normal)
        //cl.setSession("ipsj2023-rev01")
        //cl.setSession("ipsj2023-rev02")
        //cl.setSession("ut session")
        //cl.directPrediction(ActionTarget.unknown, "2022-09-01T16:30")
        cl.directPrediction(ActionTarget.unknown, "2022-09-01T23:30")
        Logger.tags("NOTICE", "INFO", "DEBUG").info("=============== End ==================", "")
