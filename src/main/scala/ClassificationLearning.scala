package org.example
/*
import com.github.nscala_time.time.Imports._
import me.shadaj.scalapy.py
import me.shadaj.scalapy.py.SeqConverters

import better.files._
import me.shadaj.scalapy.py
import me.shadaj.scalapy.py.SeqConverters
import scala.annotation.targetName
import scala.collection.mutable.ListBuffer
import ai.djl.nn.SequentialBlock
 */
import scala.jdk.CollectionConverters._
import ai.djl.ndarray.{NDArrays, NDManager, NDList} 
import ai.djl.ndarray.NDArray
import ai.djl.ndarray.index.NDIndex
import ai.djl.ndarray.types.{Shape, DataType}
import ai.djl.Device

import ai.djl.nn.{Blocks, Activation}
import ai.djl.nn.core.Linear
import ai.djl.nn.*
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

import org.tinylog.Logger



class ClassificationLearning:
    val pdDimension = 2 // fix
    val pdLength = 128 // try
    val inputSize = pdDimension * pdLength
    val outputSize = 1 // クラスインデックスじゃなくても，アクティブ度とかでもいいかも？←イイね
    val batchSize = 2
    val block = new SequentialBlock
    def getBlock =
        block.add(Blocks.batchFlattenBlock(inputSize))
        //block.add(Blocks.batchFlattenBlock())
        //block.add(Blocks.batchFlattenBlock(2))
        block.add(Linear.builder().setUnits(128).build())
        block.add(Activation.relu: Function[NDList, NDList])
        block.add(Linear.builder().setUnits(64).build())
        block.add(Activation.relu: Function[NDList, NDList])
        block.add(Linear.builder().setUnits(outputSize).build())
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
            .addEvaluator(new Accuracy())
            //.addTrainingListeners(TrainingListener.Defaults.logging(outputDir)(0))
            .addTrainingListeners(TrainingListener.Defaults.logging(): _*)
            .addTrainingListeners(listener)
        config

    def train =
        val pdt = PersistenceDiagramTransform()
        val sleepData = pdt.loadSleepPdDataset("B")
        val sleepLabels = pdt.manager.full(Shape(sleepData.getShape.get(0), 1), 0.0f, DataType.FLOAT32)
        Logger.tags("NOTICE", "INFO").info("sleep data shape={}", sleepData.getShape)
        Logger.tags("NOTICE").debug("data={}", sleepData.get("0:3,0:5"))
        Logger.tags("NOTICE").debug("labels={}", sleepLabels.getShape)
        Logger.tags("NOTICE").debug("labels={}", sleepLabels.get("0:3,:"))
        val activeData = pdt.loadActivePdDataset("B")
        val activeLabels = pdt.manager.full(Shape(activeData.getShape.get(0), 1), 1.0f, DataType.FLOAT32)
        Logger.tags("NOTICE", "INFO").info("active data shape={}", activeData.getShape)
        val dataset = new ArrayDataset.Builder()
               .setData(sleepData.concat(activeData))
               .optLabels(sleepLabels.concat(activeLabels))
               .setSampling(batchSize, false)
               .build
        dataset.prepare(new ProgressBar())
        val trainAndTest = dataset.randomSplit(8, 2)
        val block = getBlock
        val model = Model.newInstance("mlp")
        Logger.tags("NOTICE").debug("model={}", model)
        model.setBlock(block)
        model.setDataType(DataType.FLOAT32)

        val trainerConfig = getTraingingConfig("log")
        val trainer = model.newTrainer(trainerConfig)
        //trainer.setMetrics(new Metrics())


        trainer.initialize(new Shape(inputSize, outputSize))
        val epoch = 200
        //trainAndTest(0).prepare(new ProgressBar())
        //trainAndTest(1).prepare(new ProgressBar())

        Logger.tags("NOTICE").debug("train={}", trainAndTest(0).size)
        Logger.tags("NOTICE").debug("train data={}", trainAndTest(0).get(pdt.manager, 0).getData.getShapes.toSeq)
        Logger.tags("NOTICE").debug("train data={}", trainAndTest(0).get(pdt.manager, 0).getData.get(0).get("0:5"))
        Logger.tags("NOTICE").debug("train label={}", trainAndTest(0).get(pdt.manager, 0).getLabels.getShapes.toSeq)
        Logger.tags("NOTICE").debug("train label={}", trainAndTest(0).get(pdt.manager, 0).getLabels)
        Logger.tags("NOTICE").debug("test={}", trainAndTest(1).size)
        Logger.tags("NOTICE").debug("trainer model={}", trainer.getModel)
        Logger.tags("NOTICE").debug("trainer evaluator={}", trainer.getEvaluators.asScala.toSeq)

        EasyTrain.fit(trainer, epoch, trainAndTest(0), trainAndTest(1))
        Logger.tags("NOTICE").debug("trainer result={}", trainer.getTrainingResult)

        val translator = new Translator[NDList, Float](){
               override def processInput(ctx: TranslatorContext, input: NDList) =
                 //NDList(pdt.manager.create(input))
                 input
               override def processOutput(ctx: TranslatorContext, list: NDList) =
                 list.singletonOrThrow.getFloat(0L)
            }
        val predictor = model.newPredictor(translator)
        for i <- Range(0, 10) do
            Logger.tags("NOTICE").info("sleep=>{}",predictor.predict(NDList(sleepData.get(i))))
            Logger.tags("NOTICE").info("active=>{}",predictor.predict(NDList(activeData.get(i))))



object ClassificationLearning:
    @main
    def executeTrain =
        val cl = ClassificationLearning()
        cl.train
