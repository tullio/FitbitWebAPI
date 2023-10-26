package org.example
import org.scalactic.*
import org.scalactic.Tolerance.*
import org.scalatest.*
import org.scalatest.flatspec.*
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.*
import org.tinylog.Logger
import ai.djl.ndarray.{NDArrays, NDManager} 
import ai.djl.ndarray.index.NDIndex
import org.nd4j.nativeblas.Nd4jCpu.draw_bounding_boxes
import ai.djl.ndarray.types.{Shape, DataType}
import scala.jdk.CollectionConverters._

class NDArrayExtensionSpec extends AnyFunSuite:
    test("Variance covariance matrix should be calculated"){
       val manager = NDManager.newBaseManager()
       val xy = manager.create(Array(Array(40.0, 80.0, 90.0), Array(80.0, 90.0, 100.0)))
       assert(xy.matMul(xy.transpose).toDoubleArray === Array(16100.0, 19400.0, 19400.0, 24500.0))
       val xyMean = xy.mean(Array(1), true)
       Logger.info(xyMean)
       assert(xyMean.toDoubleArray === Array(70.0, 90.0))
       Logger.info(xyMean.repeat(1, xy.getShape.get(1))) 
       val diff = xy - xyMean.repeat(1, xy.getShape.get(1))
       assert(diff.toDoubleArray === Array(-30.0, 10.0, 20.0, -10.0, 0.0, 10.0))
       assert(diff.matMul(diff.transpose) === Array(1400.0, 500.0, 500.0, 200.0))
       assert(xy.cov() === Array(1400.0, 500.0, 500.0, 200.0))
       Logger.info(xy.cov(true))
    }
    test("Inner product should be calculated"){
       val manager = NDManager.newBaseManager()
       val x = manager.create(Array(1.0, 2.0, 3.0))
       val y = manager.create(Array(4.0, 5.0, 6.0))
       Logger.info(x.dot(y).getShape)
       Logger.info(x.dot(y))
       assert(x.dot(y) === 32.0)
    }
    test("HStack should be calculated"){
       val manager = NDManager.newBaseManager()
       val x = manager.create(Array(1.0, 2.0))
       val y = manager.create(Array(Array(3.0, 4.0), Array(5.0, 6.0)))
       Logger.tags("NOTICE").info(x)
       Logger.tags("NOTICE").info(y)
       //Logger.tags("NOTICE").info(x.reshape(2, 1).stack(y, 0))
       Logger.tags("NOTICE").info(x.reshape(2, 1).concat(y, 1))
       //assert(x.dot(y) === 32.0)
    }
    test("Frobenius inner product should be calculated"){
       val manager = NDManager.newBaseManager()
       val x = manager.create(Array(Array(1.0, 2.0), Array(3.0, 4.0)))
       val y = manager.create(Array(Array(5.0, 6.0), Array(7.0, 8.0)))
       assert(x.fip(y) === 1.0*5.0+2.0*6.0+3.0*7.0+4.0*8.0)
    }
    test("Jaccard Frobenius inner product should be calculated"){
       val manager = NDManager.newBaseManager()
       val x = manager.create(Array(Array(1.0, 2.0), Array(3.0, 4.0)))
       val y = manager.create(Array(Array(5.0, 6.0), Array(7.0, 8.0)))
       val z = manager.create(Array(Array(-1.0, -2.0), Array(-3.0, -4.0)))
       assert(x.jfip(x) === 1.0)
       assert(x.jfip(y) === 0.6862745098039216)
       assert(x.jfip(z) === -1.0)
    }
    test("Regularize should be calculated"){
       val manager = NDManager.newBaseManager()
       val x = manager.create(Array(Array(1.0, 2.0), Array(3.0, 4.0)))
                      .toType(DataType.FLOAT32, false)
       Logger.tags("NOTICE").info(x)
       Logger.tags("NOTICE").info(x.regularizeVertically(10))
       Logger.tags("NOTICE").info(x.regularizeHolizontally(10))
    }
    test("Sample should be calculated"){
       val manager = NDManager.newBaseManager()
       val x = manager.create(Array(Array(1.0, 2.0, 3.0, 4.0))).transpose
                      .toType(DataType.FLOAT32, false)
       Logger.tags("NOTICE").info(x)
       Logger.tags("NOTICE").info(x.sample(0.5))
    }
    test("Get and take should be compared"){
       val manager = NDManager.newBaseManager()
       val x = manager.create(Array(1.0, 2.0, 3.0, 4.0))
       Logger.tags("NOTICE").info(x)
       val indexArray = manager.create(Array(0))
       Logger.tags("NOTICE").info("get({})={}", indexArray.toArray.toSeq, x.get(indexArray))
       Logger.tags("NOTICE").info("take({})={}", indexArray.toArray.toSeq, x.take(indexArray))
       Logger.tags("NOTICE").info("===================", "")
       indexArray.set(Array(1))
       Logger.tags("NOTICE").info("get({})={}", indexArray.toArray.toSeq, x.get(indexArray))
       Logger.tags("NOTICE").info("take({})={}", indexArray.toArray.toSeq, x.take(indexArray))

       Logger.tags("NOTICE").info("===================", "")
       val indexArray2 = manager.create(Array(0, 1))
       Logger.tags("NOTICE").info("get({})={}", indexArray2.toArray.toSeq, x.get(indexArray2))
       Logger.tags("NOTICE").info("take({})={}", indexArray2.toArray.toSeq, x.take(indexArray2))
       Logger.tags("NOTICE").info("===================", "")
       indexArray2.set(Array(1, 2))
       Logger.tags("NOTICE").info("get({})={}", indexArray2.toArray.toSeq, x.get(indexArray2))
       Logger.tags("NOTICE").info("take({})={}", indexArray2.toArray.toSeq, x.take(indexArray2))


    }
    test("NDIndex should be explaind"){
       val manager = NDManager.newBaseManager()
       Logger.tags("NOTICE").info(NDIndex().addAllDim().getIndices.asScala.map(f => f.getRank))
       Logger.tags("NOTICE").info(NDIndex().addAllDim().addAllDim().getIndices.asScala.map(f => f.getRank))
       Logger.tags("NOTICE").info(NDIndex().addIndices(0).getIndices.asScala.map(f => f.getRank))
       Logger.tags("NOTICE").info(NDIndex().addIndices(0, 1).getIndices.asScala.map(f => f.getRank))
       Logger.tags("NOTICE").info(NDIndex().addPickDim(manager.create(Array(0))).getIndices.asScala.map(f => f.getRank))
       Logger.tags("NOTICE").info(NDIndex().addPickDim(manager.create(Array(0, 1))).getIndices.asScala.map(f => f.getRank))
    }
    test("How to obtain one row"){
       val manager = NDManager.newBaseManager()
       val x = manager.create(Array(Array(1.0, 2.0), Array(3.0, 4.0)))
       Logger.tags("NOTICE").info(x)
       val indexArray = NDIndex().addSliceDim(1, 2).addAllDim()
       Logger.tags("NOTICE").info("indexArray={}", indexArray.getIndices.asScala)
       Logger.tags("NOTICE").info("slice={}", x.get(indexArray))
    }
