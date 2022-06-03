package org.example
import org.scalatest._
import org.scalatest.funsuite.AnyFunSuite
import scala.swing._
import java.awt.{Color, Point}

class GraphicsSpec extends AnyFunSuite:
    test("points should be plotted as swing objects"){
        val ui = new UI
        val xy = Array(Point(10,10), Point(10, 20), Point(20,20))
        ui.flush(xy)
    }
/*
    test("points should be plotted as matplotlib objects"){
        val ui = new UI
        val xy = Array(10,10, 10, 20, 20,20)
        ui.flush(xy)
    }
 */
