package org.example
import ai.djl.ndarray.{NDArrays, NDManager} 
import ai.djl.ndarray.NDArray
import scala.annotation.targetName

extension (manager: NDManager)
    @targetName("1D")
    def create(x: Seq[Double]): NDArray = 
        manager.create(x.toArray)
    @targetName("2D")
    def create(x: Seq[Seq[Double]]): NDArray =
        manager.create(x.toArray.map(f => f.toArray)) 
