package org.example
import org.scalactic.*
import org.scalactic.Tolerance.*
import org.scalatest.*
import org.scalatest.flatspec.*
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.*
import org.tinylog.Logger
import ai.djl.ndarray.{NDArray, NDArrays, NDManager} 
import ai.djl.ndarray.types.{Shape, DataType}
import ai.djl.ndarray.index.NDIndex
import org.nd4j.nativeblas.Nd4jCpu.draw_bounding_boxes

class PersistenceDiagramTransformSpec extends AnyFunSuite:
    test("session should be set"){
        val pdt = PersistenceDiagramTransform()

        pdt.close
    }

    test("PDT object should be loaded"){
        val pdt = PersistenceDiagramTransform()

        assert(pdt != null)

        val pcs = pdt.loadDailyData(ActionTarget.active, "A")
        Logger.tags("NOTICE").info("Loaded data={}", pcs.get("1:3, :"))
        val maxPcs = pdt.getMaxPcs(pcs)
        Logger.tags("NOTICE").info("max record data set={}", maxPcs.get("1:3, :"))
        //val pd = pdt.getPd("test", maxPcs).get
        //Logger.info(pd)
        //val startIndex = pdt.getStepIndexFromPcs(maxPcs)(0)
        //pdt.drawPcpd(ActionTarget.test, startIndex, maxPcs, pd)
        //pdt.createSleepDataset
        pdt.close
    }
    test("Clustering should be executed"){
        val pdt = PersistenceDiagramTransform()

        assert(pdt != null)
        val sim = pdt.getClusteringTarget(ActionTarget.sleep)
        pdt.createSimirarityMatrix(sim)
        pdt.close
    }
    test("extractDailyData should be executed"){
        Logger.tags("NOTICE", "INFO").info("========== test start =============", "")
        val pdt = PersistenceDiagramTransform()

        assert(pdt != null)
        /**** planA/Bのコード ****
        val pcsA = pdt.loadDailyData("heartrate-sampled-sleep", "A")
        Logger.info(pcsA.get("0:3, :"))
        val pcsB = pdt.loadDailyData("heartrate-sampled-sleep", "B")
        Logger.info(pcsB.get("0:10, :"))
         * */
        val pcs = pdt.extractDailyData(ActionTarget.sleep, "C")
        // とりあえず見た目で判断
        Logger.tags("NOTICE").info(pcs.get("0:10, :"))

        val slot = pdt.getTargetSlotFromPcs(pcs, "2023-02-17T02:00")
        Logger.tags("NOTICE").info("slot={}", slot)
        pdt.close
        Logger.tags("NOTICE", "INFO").info("========== test end =============", "")
    }
    test("getTimeIndexRange should be executed"){
        Logger.tags("NOTICE", "INFO").info("========== test start =============", "")
        val pdt = PersistenceDiagramTransform()

        assert(pdt != null)
        val timeIndexRange = pdt.getTimeIndexRange
        Logger.tags("NOTICE", "INFO").info("timeIndexRange={}",
            timeIndexRange
        )
        Logger.tags("NOTICE", "INFO").info("timeIndexRange.take(5)={}",
            timeIndexRange.take(5)
        )
        Logger.tags("NOTICE", "INFO").info("timeIndexRange.take(5)={}",
          timeIndexRange.take(5).toArray.toSeq
        )

        pdt.close
        Logger.tags("NOTICE", "INFO").info("========== test end =============", "")
    }
    test("getNearestTimeIndex should be executed"){
        Logger.tags("NOTICE", "INFO").info("========== test start =============", "")
        val pdt = PersistenceDiagramTransform()
        assert(pdt != null)
        val timeIndex = pdt.getNearestTimeIndex("2023-02-17T02:00")
        Logger.tags("NOTICE", "INFO").info("timeIndex={}",
            timeIndex
        )

        pdt.close
        Logger.tags("NOTICE", "INFO").info("========== test end =============", "")
    }
    test("loadDailyData should be executed"){
        val pdt = PersistenceDiagramTransform()
        assert(pdt != null)
        /**** planA/Bのコード ****
        val pcsA = pdt.loadDailyData("heartrate-sampled-sleep", "A")
        Logger.info(pcsA.get("0:3, :"))
        val pcsB = pdt.loadDailyData("heartrate-sampled-sleep", "B")
        Logger.info(pcsB.get("0:10, :"))
         * */
        val pcsC = pdt.loadDailyData(ActionTarget.sleep, "C")
        Logger.tags("NOTICE").info(pcsC.get("0:10, :"))
        pdt.close
    }
    test("createDataset should be executed"){
        val pdt = PersistenceDiagramTransform()
        assert(pdt != null)
        pdt.createSleepPDDataset("C")
        pdt.createActivePDDataset("C")
        pdt.close
    }
    test("createPDDatasetAt should be executed"){
        val pdt = PersistenceDiagramTransform()
        assert(pdt != null)
        //pdt.createPDDatasetAt(ActionTarget.sleep, "C", "2023-02-17T02:00")
        val pcsC = pdt.loadDailyData(ActionTarget.sleep, "C") // dummy
        pdt.createPDDatasetAt(ActionTarget.unknown, "C", "2023-02-17T19:00")

        pdt.close
    }
    test("refreshTimeIndex should be executed"){
        val pdt = PersistenceDiagramTransform()
        //pdt.setSession("ut session")
        assert(pdt != null)
        pdt.refreshTimeIndex
        pdt.loadSleepPcDataset("C")
        pdt.loadActivePcDataset("C")

        pdt.close
    }

    test("createEmbedding should be executed"){
        val pdt = PersistenceDiagramTransform()
        assert(pdt != null)
        val x = pdt.manager.create(Shape(Property.delayWindow + 3, 2))
        for i <- Range(0, x.getShape.get(0).toInt) do
            x.set(NDIndex().addIndices(i).addIndices(0), i) // generate dummy time-like indices
            x.set(NDIndex().addIndices(i).addIndices(1), i) // generate dummy data
        Logger.info(x)
        val z = pdt.createEmbedding(x)
        Logger.info(z)
        val dummyTime = x.get(NDIndex().addSliceDim(0, 3).addIndices(0)).toType(DataType.FLOAT32, false)
        for i <- Range(0, x.getShape.get(0).toInt, 3) do
            val localIndex = i/3
            val dummyData = x.get(NDIndex().addSliceDim(localIndex, localIndex + 3)
                               .addIndices(1)).toType(DataType.FLOAT32, false)

            assert(z.get(NDIndex().addSliceDim(i, i + 3).addIndices(0)) === dummyTime)
            assert(z.get(NDIndex().addSliceDim(i, i + 3).addIndices(1)) === dummyData)
            val zIndex = pdt.manager.full(Shape(3), localIndex.toFloat)
                            .toType(DataType.FLOAT32, false)
            assert(z.get(NDIndex().addSliceDim(i, i + 3).addIndices(2)) === zIndex)
        pdt.close
    }
    test("createSlidingWindowEmbedding should be executed"){
        val pdt = PersistenceDiagramTransform()

        val timeDelay = 2
        val dimension = 3
        assert(pdt != null)
        val x = pdt.manager.create(Shape(7, 3))
        for i <- Range(0, x.getShape.get(0).toInt) do
            x.set(NDIndex().addIndices(i).addIndices(0), i) // generate dummy time-like indices
            x.set(NDIndex().addIndices(i).addIndices(1), i) // generate dummy data
            x.set(NDIndex().addIndices(i).addIndices(2), i*2) // generate dummy data
        Logger.tags("NOTICE").info("input={}", x)
        val z = pdt.createSlidingWindowEmbedding(x, timeDelay)
        Logger.tags("NOTICE").info("embedding={}", z)
        for i <- Range(0, x.getShape.get(0).toInt - 3 - timeDelay + 1) do
            val dummyTime = x.get(NDIndex().addSliceDim(i, i + 3 * timeDelay, timeDelay)
                               .addIndices(0)).toType(DataType.FLOAT32, false)
            val embedded0 = z.get(NDIndex().addIndices(i).addSliceDim(0, dimension).addIndices(0))
            assert(embedded0 === dummyTime)
            val dummyData1 = x.get(NDIndex().addSliceDim(i, i + 3 * timeDelay, timeDelay)
                               .addIndices(1)).toType(DataType.FLOAT32, false)

            val embedded1 = z.get(NDIndex().addIndices(i).addSliceDim(0, dimension).addIndices(1))
            assert(embedded1 === dummyData1)
            val dummyData2 = x.get(NDIndex().addSliceDim(i, i + 3 * timeDelay, timeDelay)
                               .addIndices(2)).toType(DataType.FLOAT32, false)
            val embedded2 = z.get(NDIndex().addIndices(i).addSliceDim(0, dimension).addIndices(2))
            assert(embedded2 === dummyData2)
        pdt.close
    }
    test("EngMTG0124"){
        def output(pc: NDArray) =
            Logger.tags("NOTICE").info("creating sliding window from {}", pc)
            val pdt = PersistenceDiagramTransform()
            val pc3d = pdt.createSlidingWindowEmbedding(pc)
            Logger.tags("NOTICE").info("results: {}", pc3d)
            val timeLength = pc3d.getShape.get(0)
            val timeStamp = pc3d.get(NDIndex().addAllDim
                                          .addIndices(0)
                                          .addIndices(0)).reshape(timeLength, 1)
            // Shape(timeLength, 3)
            val value = pc3d.get(NDIndex().addAllDim
                                          .addAllDim
                                          .addIndices(1)).reshape(timeLength, -1)
            val filtrationFile = "tmp.pdgm"
            Logger.tags("NOTICE").info("creating PD from {}", value)
            val pairs = pdt.getPd(filtrationFile, value)
            if pairs.isDefined then
                Logger.tags("NOTICE").info("pairs was defined...{}", pairs)
                val pairsNDArray = pairs.get
                val x = pairsNDArray.get(NDIndex().addAllDim.addIndices(0))
                val y = pairsNDArray.get(NDIndex().addAllDim.addIndices(1))
                pdt.plt.scatter(x.toNumpy(pdt.np), x.toNumpy(pdt.np))
                val figFile = "tmp.png"
                Logger.tags("NOTICE").info("saving...{}", figFile)
                pdt.plt.savefig("tmp.png")
            else
                Logger.tags("NOTICE").info("pairs was undefined...{}", pairs)

        def retrievePc(pc: NDArray, start: Int, stop: Int) =
            val timeColumnRange = NDIndex().addAllDim.addIndices(0)
            val range = (pc.get(timeColumnRange) >= start) *
                          (pc.get(timeColumnRange) < stop)
            pc.get(range)
        val pdt = PersistenceDiagramTransform()
        val timeDelay = 2
        val dimension = 3
        Logger.tags("NOTICE").info("creating ndarray...{}", "")
        // [time, value, ith number of target period]
        val pc = pdt.manager.create(
                      pdt.np.load("2022-09-01/heartrate-pd-2022-09-01.npy").T
                      .as[Seq[Seq[Double]]]
                     ).toType(DataType.FLOAT32, false)


    }
