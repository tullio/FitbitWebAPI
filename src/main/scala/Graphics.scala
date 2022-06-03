package org.example
import scala.swing._
import java.awt.{Color, Point}
//import com.github.sh0nk.matplotlib4j.Plot
import scala.jdk.CollectionConverters._

class UI extends Frame:
    title = "GUI"
    visible = true
    background = Color.PINK
    val pointRadius = 8
    contents = emptyPanel
    def flush(xy: Array[Point]) =
        contents = mainPanel(xy)
    def flush(y: Array[Long]) =
        val xy = y.zipWithIndex.map{f =>
           new Point(f._2, f._1.toInt)
        }
        contents = mainPanel(xy)

    def emptyPanel = new Panel {
        preferredSize = new Dimension(624, 240)
    }
    def mainPanel(xy: Array[Point]) = new Panel {
        val frameX = 624
        val frameY = 240
        preferredSize = new Dimension(frameX, frameY)

        override def paint(g: Graphics2D) =
            val xSeries = xy.foldLeft(Seq.empty[Int])((f, g) => f :+ g.x)
            val ySeries = xy.foldLeft(Seq.empty[Int])((f, g) => f :+ g.y)
            val xMin = xSeries.min
            val xMax = xSeries.max
            val yMin = ySeries.min
            val yMax = ySeries.max
            println(s"xmin=$xMin, xmax=$xMax, ymin=$yMin, ymax=$yMax")
            xy.foreach{ f =>
              val x = ((f.getX().toInt-xMin)/(xMax-xMin).toDouble*frameX).toInt
              val y = (frameY - (f.getY().toInt-yMin)/(yMax-yMin).toDouble*frameY).toInt
              println(s"normalize=${(f.getY().toInt-yMin)/(yMax-yMin).toDouble}")
              //println(s"xy=($x, $y)")
              pset(x, y, Color.GREEN)
            }
            def pset(x: Int, y: Int, c: Color) =
                g.setColor(c)
                g.fillOval(x, y, pointRadius, pointRadius)
    }
/*
class UI:
    val plt = Plot.create
    def flush(yData: Array[Int]): Unit =
        flush(yData.map(f => f.toLong))
    def flush(yData: Array[Long]): Unit =
        val (x, y) = yData.zipWithIndex.map{f =>
           (f._2.toDouble.asInstanceOf[java.lang.Double], f._1.toDouble.asInstanceOf[java.lang.Double])
        }.unzip
        plt.plot.add(x.toList.asJava, y.toList.asJava)
        plt.show
 */
