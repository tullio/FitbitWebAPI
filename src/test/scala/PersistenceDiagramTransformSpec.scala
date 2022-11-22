package org.example
import org.scalactic.*
import org.scalactic.Tolerance.*
import org.scalatest.*
import org.scalatest.flatspec.*
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.*
import org.tinylog.Logger
import ai.djl.ndarray.{NDArrays, NDManager} 
import ai.djl.ndarray.types.{Shape, DataType}
import ai.djl.ndarray.index.NDIndex
import org.nd4j.nativeblas.Nd4jCpu.draw_bounding_boxes

class PersistenceDiagramTransformSpec extends AnyFunSuite:
    test("PDT object should be loaded"){
        val pdt = PersistenceDiagramTransform()
        assert(pdt != null)

        val pcs = pdt.loadDailyData("heartrate-sampled-active")
        Logger.info(pcs.get("1:3, :"))
        val maxPcs = pdt.getMaxPcs(pcs)
        Logger.info(maxPcs.get("1:3, :"))
        val pd = pdt.getPd("test", maxPcs)
        Logger.info(pd)
        pdt.drawPcpd("test", maxPcs)
        //pdt.createSleepDataset
        pdt.close
    }
    test("Clustering should be executed"){
        val pdt = PersistenceDiagramTransform()
        assert(pdt != null)
        val sim = pdt.getClusteringTarget("sleep")
        pdt.createSimirarityMatrix(sim)
        pdt.close
    }
    test("loadDailyData should be executed"){
        val pdt = PersistenceDiagramTransform()
        assert(pdt != null)
        val pcsA = pdt.loadDailyData("heartrate-sampled-sleep", "A")
        Logger.info(pcsA.get("0:3, :"))
        val pcsB = pdt.loadDailyData("heartrate-sampled-sleep", "B")
        Logger.info(pcsB.get("0:10, :"))
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
        val dummyTime = x.get(NDIndex().addSliceDim(0, 3).addIndices(0)).toType(DataType.FLOAT64, false)
        for i <- Range(0, x.getShape.get(0).toInt, 3) do
            val localIndex = i/3
            val dummyData = x.get(NDIndex().addSliceDim(localIndex, localIndex + 3).addIndices(1))
                            .toType(DataType.FLOAT64, false)
            assert(z.get(NDIndex().addSliceDim(i, i + 3).addIndices(0)) === dummyTime)
            assert(z.get(NDIndex().addSliceDim(i, i + 3).addIndices(1)) === dummyData)
            val zIndex = pdt.manager.full(Shape(3), localIndex.toFloat)
                            .toType(DataType.FLOAT64, false)
            assert(z.get(NDIndex().addSliceDim(i, i + 3).addIndices(2)) === zIndex)
        pdt.close
    }
