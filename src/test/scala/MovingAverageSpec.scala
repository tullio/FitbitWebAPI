package org.example
import org.scalatest._
import org.scalatest.funsuite.AnyFunSuite
import io.circe.syntax._
import com.github.psambit9791.jdsp.misc.{Plotting, UtilMethods}
import com.github.psambit9791.jdsp.transform.DiscreteFourier
import better.files._
import com.github.nscala_time.time.Imports._
import org.tinylog.Logger



class MovingAverageSpec extends AnyFunSuite:
    test("MovingAverage should be calculated"){
        val queue = MovingAverage(5)
        assert(queue != null) 
        queue += 1.0
        assert(queue.value == 1.0)
        queue += 2.0
        assert(queue.value == 1.5)
        queue += 3.0
        assert(queue.value == 2.0)
        queue += 2.0
        assert(queue.value == 2.0)
        queue += 2.0
        assert(queue.value == 2.0)
        queue += 2.0
        assert(queue.value == 2.2)

    }
