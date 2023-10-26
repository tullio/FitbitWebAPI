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

class ClassificationLearningSpec extends AnyFunSuite:
    test("Object should be created"){
        val cl = ClassificationLearning(LearningMode.normal)
        assert(cl != null)
    }
    test("Prepare should be created"){
        val cl = ClassificationLearning(LearningMode.normal)
        assert(cl != null)

        cl.prepare("C")
    }
    test("Train should be created"){
        val cl = ClassificationLearning(LearningMode.normal)
        assert(cl != null)

        cl.train
    }
    test("Predict should be created"){
        val cl = ClassificationLearning(LearningMode.normal)
        assert(cl != null)

        cl.prediction
    }
    test("directPredict should be created"){
        val cl = ClassificationLearning(LearningMode.normal)
        assert(cl != null)

        cl.directPrediction(ActionTarget.unknown, "2022-09-01T0:00")
    }


