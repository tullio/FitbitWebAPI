package org.example

import com.github.nscala_time.time.Imports._
import me.shadaj.scalapy.py
import me.shadaj.scalapy.py.Module
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

extension(x: NDArray)
  def ===(y: Double):Boolean =
      x.toDoubleArray.toSeq(0) == y
  def ===(y: Array[Double]):Boolean =
      x.toDoubleArray.toSeq == y.toSeq
  def ===(y: Array[Array[Double]]):Boolean =
      x === y.map(f => f.toSeq).toSeq
  def ===(y: Seq[Seq[Double]]):Boolean =
      //Logger.debug(x.toArray)
      //Logger.debug(y.flatten.toArray)
      if x.getShape.get(0) == y.length &&
         x.getShape.get(1) == y(0).length &&
         x.toArray.sameElements(y.flatten.toArray) then
          true
      else
          false

  def hoge(y: Number) =
      (y.asInstanceOf[Int]+1).toString
  def +(y: NDArray): NDArray =
      x.add(y)
  def -(y: NDArray): NDArray =
      x.sub(y)
  def *(y: NDArray): NDArray =
      x.mul(y)
  def +(y: Double): NDArray =
      x.add(y)
  def -(y: Double): NDArray =
      x.sub(y)
  def *(y: Double): NDArray =
      x.mul(y)
  def /(y: Double): NDArray =
      x.div(y)
  def >(y: Double): NDArray =
      x.gt(y)
  def >=(y: Double): NDArray =
       x.gt(y).add(x.eq(y))
  def <(y: Double): NDArray =
      x.lt(y)
  def <=(y: Double): NDArray =
       x.lt(y).add(x.eq(y))
  def >(y: Int): NDArray =
      x.gt(y)
  def >=(y: Int): NDArray =
       x.gt(y).add(x.eq(y))
  def <(y: Int): NDArray =
      x.lt(y)
  def <=(y: Int): NDArray =
       x.lt(y).add(x.eq(y))
  def uniq =
      val manager = x.getManager
      //manager.create(x.sort.toDoubleArray.distinct)
      manager.create(x.toType(DataType.FLOAT64, false).sort.toDoubleArray.distinct)
  def addHDim(d: Int) =
      val newShape = x.getShape.add(d)
      x.reshape(newShape)
  def getMinMaxPair =
      (x.min().getDouble().toInt, x.max().getDouble().toInt)
  def toNumpy(np: Module) =
      //np.array(x.toDoubleArray.toPythonProxy).reshape(x.getShape.getShape.toPythonProxy) 
      np.array(x.toType(DataType.FLOAT64, false).toDoubleArray.toPythonProxy)
               .reshape(x.getShape.getShape.toPythonProxy)
  def cov(bias: Boolean = false) =
      val mean = x.mean(Array(1), true)
      val diff = x - mean.repeat(1, x.getShape.get(1))
      var covMat = diff.matMul(diff.transpose)
      if bias then
          covMat = covMat.div(x.getShape.get(1))
      covMat
  def fip(y: NDArray) =
      x.mul(y).sum.toDoubleArray.apply(0)
  def jfip(y: NDArray) =
      val f = x.cov()
      val g = y.cov()
      val jfip = 2.0*x.fip(y)/(x.fip(x)+y.fip(y))
      jfip

