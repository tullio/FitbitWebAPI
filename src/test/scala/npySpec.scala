package org.example
import org.scalatest._
import org.scalatest.funsuite.AnyFunSuite
import io.circe.syntax._
import com.github.psambit9791.jdsp.misc.{Plotting, UtilMethods}
import com.github.psambit9791.jdsp.transform.DiscreteFourier
import better.files._
import com.github.nscala_time.time.Imports._
import org.tinylog.Logger
import org.jetbrains.bio.npy._
import java.nio.file.Paths

class npySpec extends AnyFunSuite:
    test("MovingAverage should be calculated"){
        val values = Array(1, 2, 3)
        NpyFile.write(Paths.get("npytest1.npy"), values)
        assert(0==0)
    }
