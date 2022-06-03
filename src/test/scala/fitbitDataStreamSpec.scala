package org.example
import org.scalatest._
import org.scalatest.funsuite.AnyFunSuite
import io.circe.syntax._
import com.github.psambit9791.jdsp.misc.{Plotting, UtilMethods}
import com.github.psambit9791.jdsp.transform.DiscreteFourier
import better.files._
import com.github.nscala_time.time.Imports._




class FitbitDataStreamSpec extends AnyFunSuite:
/*
    test("class object should be created"){
        val fb = FitbitDataStream("src/main/resources/tetsu.sato.toml")
        assert(fb != null) 
        fb.close
    }

    test("request should be authorized"){
        val fb = FitbitDataStream("src/main/resources/tetsu.sato.toml")
        assert(fb != null) 
        //fb.requestAuthorization
        val ret = fb.getAccessToken
        println(ret)
        fb.close
        
    }

 */
/*
    test("request should be created"){
        val fb = FitbitDataStream("src/main/resources/tetsu.sato.toml")
        assert(fb != null) 
        val ret = fb.requestAuthorization
        println(ret)
    }
 */
    test("getRefreshToken should refresh tokens"){
        val fb = FitbitDataStream("src/main/resources/tetsu.sato.toml")
        assert(fb != null) 
        try
          fb.introspect
          val ret = fb.getRefreshToken
          println(ret)
          fb.introspect
        finally
          println("closing DB...")
          fb.close
    }

    test("sleep should obtain sleep data"){
        val fb = FitbitDataStream("src/main/resources/tetsu.sato.toml")
        assert(fb != null) 
        try
          val ret = fb.sleep("2022-04-04")
          println(ret)

        finally
          println("closing DB...")
          fb.close
    }
    test("sleep should obtain sleep data between two dates"){
        val fb = FitbitDataStream("src/main/resources/tetsu.sato.toml")
        assert(fb != null) 
        try
          val ret = fb.sleep("2022-04-04", "2022-04-06")
          println(ret)
        finally
          println("closing DB...")
          fb.close
    }
    test("heart should obtain heart rate data"){
        val fb = FitbitDataStream("src/main/resources/tetsu.sato.toml")
        assert(fb != null) 
        try
          val ret = fb.heart("2022-01-01", "1d")
          println(ret)
          val heart = Heart(ret)
          //println(heart.`activities-heart`(0).dateTime)
          //println(heart.`activities-heart`(0).value.heartRateZones.toSeq)
          //println(heart.`activities-heart-intraday`)
          //println(heart.`activities-heart-intraday`.dataset.toSeq)
          val dataset = heart.`activities-heart-intraday`.dataset.toSeq
          val dataSeries = scala.collection.mutable.ArrayBuffer.empty[Long]
          val file = "heart_0401.csv".toFile

          val sw = DateTime.parse("2022-04-01T00:00:00.000-09:00")
      //          dataset.filter(f => f.time.startsWith("16:") || f.time.startsWith("15:")|| f.time.startsWith("14:")).zipWithIndex.foreach{f =>
          val fmt = DateTimeFormat.forPattern("HH:mm:ss")
          val timeSeries = (0 until 24*60).toList.map(f => (sw + f.minutes).toString(fmt))
          println(s"timeSeries=${timeSeries}")
          val complemented = timeSeries.map{f =>
              val target = dataset.filter(g => g.time == f)
              target.length match
                  case 0 => Dataset(f, 0L)
                  case 1 => target(0)
                  case _ => Dataset("error", 0L)
          }
          println(s"complemented=${complemented}")
          (0 +: complemented).zip(complemented :+ 0).slice(1, complemented.length).foreach{f =>
          //dataset.zipWithIndex.foreach{f =>
          //dataSeries += f._1.value
               dataSeries += f._1.asInstanceOf[Dataset].value
//              file.appendLine(s"${f._1.time.substring(0, 1)} ${f._2.toDouble} ${f._1.value.toDouble}")
               file.appendLine(s"${f._1.asInstanceOf[Dataset].time.substring(0, 5)} ${f._1.asInstanceOf[Dataset].value.toDouble} ${f._2.asInstanceOf[Dataset].value.toDouble}")

          }
          
          //val ui = new UI
          //ui.flush(dataSeries.toArray)
          val width = 600
          val height = 500
          val title = "Heart Rate"
          val x_axis = "Time"
          val y_axis = "Signal"
          val fig = new Plotting(width, height, title, x_axis, y_axis)
          fig.initialisePlot()
          val signal1 = dataSeries.map(f => f.toDouble).toArray
          val time = UtilMethods.linspace(0, 1, signal1.length, false)
          fig.addSignal("Signal 1", time, signal1, true)
          fig.plot

          val fig2 = new Plotting(width, height, "FFT", "frequency", "magnitude")
          fig2.initialisePlot
          val ft = new DiscreteFourier(signal1)
          ft.transform
          val out = ft.getMagnitude(false)
          fig2.addSignal("FFT", time, out, true)
          fig2.plot
          println(out.toSeq)

        finally
          println("closing DB...")
          fb.close
    }
    test("heartIntra should obtain detailed heart rate data"){
        val fb = FitbitDataStream("src/main/resources/tetsu.sato.toml")
        assert(fb != null) 
        try
          val ret = fb.heartIntraday("2022-03-01", "1sec")
          println(ret)
          val heart = Heart(ret)
          val dataset = heart.`activities-heart-intraday`.dataset.toSeq
          val dataSeries = scala.collection.mutable.ArrayBuffer.empty[Long]
          val file = "heart_0401.csv".toFile

          val sw = DateTime.parse("2022-03-01T00:00:00.000-09:00")
      //          dataset.filter(f => f.time.startsWith("16:") || f.time.startsWith("15:")|| f.time.startsWith("14:")).zipWithIndex.foreach{f =>
          val fmt = DateTimeFormat.forPattern("HH:mm:ss")
          val timeSeries = (0 until 24*60*60).toList.map(f => (sw + f.second).toString(fmt))
          println(s"timeSeries=${timeSeries}")
          val complemented = timeSeries.map{f =>
              val target = dataset.filter(g => g.time == f)
              target.length match
                  case 0 => Dataset(f, 0L)
                  case 1 => target(0)
                  case _ => Dataset("error", 0L)
          }
          println(s"complemented=${complemented}")
          (0 +: complemented).zip(complemented :+ 0).slice(1, complemented.length).foreach{f =>
          //dataset.zipWithIndex.foreach{f =>
          //dataSeries += f._1.value
               dataSeries += f._1.asInstanceOf[Dataset].value
//              file.appendLine(s"${f._1.time.substring(0, 1)} ${f._2.toDouble} ${f._1.value.toDouble}")
               file.appendLine(s"${f._1.asInstanceOf[Dataset].time.substring(0, 5)} ${f._1.asInstanceOf[Dataset].value.toDouble} ${f._2.asInstanceOf[Dataset].value.toDouble}")

          }
          
          //val ui = new UI
          //ui.flush(dataSeries.toArray)
          val width = 600
          val height = 500
          val title = "Heart Rate"
          val x_axis = "Time"
          val y_axis = "Signal"
          val fig = new Plotting(width, height, title, x_axis, y_axis)
          fig.initialisePlot()
          val signal1 = dataSeries.map(f => f.toDouble).toArray
          val time = UtilMethods.linspace(0, 1, signal1.length, false)
          fig.addSignal("Signal 1", time, signal1, true)
          fig.plot

          val fig2 = new Plotting(width, height, "FFT", "frequency", "magnitude")
          fig2.initialisePlot
          val ft = new DiscreteFourier(signal1)
          ft.transform
          val out = ft.getMagnitude(false)
          fig2.addSignal("FFT", time, out, true)
          fig2.plot
          println(out.toSeq)

        finally
          println("closing DB...")
          fb.close
    }
    test("heartIntraTime should obtain detailed heart rate data"){
        val fb = FitbitDataStream("src/main/resources/tetsu.sato.toml")
        assert(fb != null) 
        val targetDate = "2022-03-01"
        val startTimeString = "00:00"
        val endTimeString = "01:00"
        val startDateTimeString = s"${targetDate}T${startTimeString}"
        val endDateTimeString = s"${targetDate}T${endTimeString}"
        try
/**
          val ret = fb.heartIntraday(targetDate, "1sec", startTimeString, endTimeString)
          println(ret)
          val heart = ActivityHeartTime(ret)
          val dataset = heart.`activities-heart-intraday`.dataset.toSeq
          val dataSeries = scala.collection.mutable.ArrayBuffer.empty[Long]
          val file = "heart_0401.csv".toFile

          val fmt = DateTimeFormat.forPattern("HH:mm:ss")
          val startDateTime = DateTime.parse(startDateTimeString)
          val endDateTime = DateTime.parse(endDateTimeString)
          val timeSeries = startDateTime.toInterval(endDateTime).toStringTimeSeries(fmt)
          println(s"timeSeries=${timeSeries}")
          val complemented = timeSeries.map{f =>
              val target = dataset.filter(g => g.time == f)
              target.length match
                  case 0 => Dataset(f, 59L)
                  case 1 => target(0)
                  case _ => Dataset("error", 0L)
          }
          println(s"complemented=${complemented}")
          (0 +: complemented).zip(complemented :+ 0).slice(1, complemented.length).foreach{f =>
          //dataset.zipWithIndex.foreach{f =>
          //dataSeries += f._1.value
               dataSeries += f._1.asInstanceOf[Dataset].value
//              file.appendLine(s"${f._1.time.substring(0, 1)} ${f._2.toDouble} ${f._1.value.toDouble}")
               file.appendLine(s"${f._1.asInstanceOf[Dataset].time.substring(0, 5)} ${f._1.asInstanceOf[Dataset].value.toDouble} ${f._2.asInstanceOf[Dataset].value.toDouble}")

          }
  * */          
          val dataSeries = fb.getActivityHeartIntradayDataSeries(targetDate, startTimeString, endTimeString)

          //val ui = new UI
          //ui.flush(dataSeries.toArray)
          val width = 600
          val height = 500
          val title = "Heart Rate"
          val x_axis = "Time"
          val y_axis = "Signal"
          val fig = new Plotting(width, height, title, x_axis, y_axis)
          fig.initialisePlot()
          val signal1 = dataSeries.map(f => f.toDouble).toArray
          val time = UtilMethods.linspace(0, 1, signal1.length, false)
          fig.addSignal("Signal 1", time, signal1, true)
          fig.plot

          val fig2 = new Plotting(width, height, "FFT", "frequency", "magnitude")
          fig2.initialisePlot
          val ft = new DiscreteFourier(signal1)
          ft.transform
          val out = ft.getMagnitude(false)
          fig2.addSignal("FFT", time, out, true)
          fig2.plot
          println(out.toSeq)

        finally
          println("closing DB...")
          fb.close
    }

    test("access token should be introspected"){
        val fb = FitbitDataStream("src/main/resources/tetsu.sato.toml")
        assert(fb != null) 
        try
          val ret = fb.introspect
          println(ret)
        finally
          println("closing DB...")
          fb.close
        
    }

    test("UTF test"){
      case class user(氏名: String, 候補者ID: Int)
      val user1 = user(候補者ID = 123, 氏名 = "佐藤")
      println(user1)        
    }

