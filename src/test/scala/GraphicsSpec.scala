package org.example
import org.scalatest._
import org.scalatest.funsuite.AnyFunSuite
import scala.swing._
import java.awt.{Color, Point}
import breeze.plot.{Figure, Plot, plot}

class GraphicsSpec extends AnyFunSuite:
    test("points should be plotted as swing objects"){
        val ui = new UI
        val xy = Array(Point(10,10), Point(10, 20), Point(20,20))
        ui.flush(xy)
    }
    test("breeze should be performed"){
        val f = Figure(s"Particle filter")
        f.width = 1480
        f.height = 740
        val p0 = f.subplot(3, 2, 1)

        p0 += plot(Array(0.0, 1.0, 2.0), Array(0.0, 1.0, 4.0), name = "Input", style = '+', colorcode = "255,0,0")
    }
/*
    test("points should be plotted as matplotlib objects"){
        val ui = new UI
        val xy = Array(10,10, 10, 20, 20,20)
        ui.flush(xy)
    }
 */
