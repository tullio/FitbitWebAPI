package org.example
import org.scalatest._
import org.scalatest.funsuite.AnyFunSuite
import com.github.psambit9791.jdsp.misc.{Plotting, UtilMethods}
import com.github.psambit9791.jdsp.transform.DiscreteFourier
import scala.math.{Pi, sin}
import java.security.Permission

sealed case class ExitException(status: Int) extends SecurityException("System.exit() is not allowed") {
}
sealed class NoExitSecurityManager extends SecurityManager {
  override def checkPermission(perm: Permission): Unit = {}

  override def checkPermission(perm: Permission, context: Object): Unit = {}

  override def checkExit(status: Int): Unit = {
    super.checkExit(status)
//    throw ExitException(status)
  }
}


class jdspSpec extends AnyFunSuite
               with BeforeAndAfterAll:
    override def beforeAll() = {
        System.setSecurityManager(new NoExitSecurityManager())
    }
    test("lines-points should be plotted"){

        val width = 600
        val height = 500
        val title = "Sample Figure"
        val x_axis = "Time"
        val y_axis = "Signal"
        val fig = new Plotting(width, height, title, x_axis, y_axis)
        fig.initialisePlot()
        val signal1 = Array(2.0, 4.0, 2.0, 3.0, 1.0)
        val signal2 = Array(3.4, 6.7, 2.2, 1.6, 3.6)
        val time = Array(0.0, 1.0, 2.0, 3.0, 4.0)
        fig.addSignal("Signal 1", time, signal1, true)
        fig.addSignal("Signal 2", time, signal2, false)
        try
          fig.plot
        catch
          case e: ExitException => println(e)
    }
    test("simple sine curve should be ffted"){
        val width = 600
        val height = 500
        val title = "Sample Figure"
        val x_axis = "Time"
        val y_axis = "Signal"
        val fig = new Plotting(width, height, title, x_axis, y_axis)
        fig.initialisePlot()
        val time = UtilMethods.linspace(0.0, 4.0*Pi, 100, true)
        val signal1 = time.map(f => sin(f) + 1.5*sin(2.0*f))
        fig.addSignal("Signal 1", time, signal1, true)
//        fig.plot
        println(s"time.length=${time.length}")
        println(s"signal1.length=${signal1.length}")
        val ft = new DiscreteFourier(signal1)
        ft.transform
        val out = ft.getMagnitude(false)
        println(s"out.length=${out.length}")
        println(out.toSeq)
        fig.addSignal("FFT", time, out, true)
        fig.plot
    }
