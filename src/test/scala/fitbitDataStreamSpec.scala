package org.example
import org.scalatest._
import org.scalatest.funsuite.AnyFunSuite
import io.circe.syntax._
import com.github.psambit9791.jdsp.misc.{Plotting, UtilMethods}
import com.github.psambit9791.jdsp.transform.DiscreteFourier
import better.files._
import com.github.nscala_time.time.Imports._
import org.tinylog.Logger



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
        fb.introspect
        val ret = fb.getRefreshToken
        println(ret)
        fb.introspect
    }

    test("sleep should obtain sleep data"){
        val fb = FitbitDataStream("src/main/resources/tetsu.sato.toml")
        assert(fb != null) 
        val ret = fb.sleep("2022-04-04")
    }
    test("sleep should obtain sleep data between two dates"){
        val fb = FitbitDataStream("src/main/resources/tetsu.sato.toml")
        assert(fb != null) 
        val ret = fb.sleep("2022-04-04", "2022-04-06")
    }
    test("heart should obtain heart rate data"){
        val fb = FitbitDataStream("src/main/resources/tetsu.sato.toml")
        assert(fb != null) 
        val ret = fb.heart("2022-01-01", "1d")
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
        //println(s"timeSeries=${timeSeries}")
        val complemented = timeSeries.map{f =>
            val target = dataset.filter(g => g.time == f)
            target.length match
                case 0 => Dataset(f, 0L)
                case 1 => target(0)
                case _ => Dataset("error", 0L)
        }
        println(s"complemented=${complemented.take(10)}")
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
        //println(out.toSeq)

    }
    test("heartIntra should obtain detailed heart rate data"){
        val fb = FitbitDataStream("src/main/resources/tetsu.sato.toml")
        assert(fb != null) 
        val ret = fb.heartIntraday("2022-03-01", "1sec")
        //println(ret)
        val heart = Heart(ret)
        val dataset = heart.`activities-heart-intraday`.dataset.toSeq
        val dataSeries = scala.collection.mutable.ArrayBuffer.empty[Long]
        val file = "heart_0401.csv".toFile

        val sw = DateTime.parse("2022-03-01T00:00:00.000-09:00")
    //          dataset.filter(f => f.time.startsWith("16:") || f.time.startsWith("15:")|| f.time.startsWith("14:")).zipWithIndex.foreach{f =>
        val fmt = DateTimeFormat.forPattern("HH:mm:ss")
        val timeSeries = (0 until 24*60*60).toList.map(f => (sw + f.second).toString(fmt))
        //println(s"timeSeries=${timeSeries}")
        val complemented = timeSeries.map{f =>
            val target = dataset.filter(g => g.time == f)
            target.length match
                case 0 => Dataset(f, 0L)
                case 1 => target(0)
                case _ => Dataset("error", 0L)
        }
        //println(s"complemented=${complemented}")
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
        val signal1 = dataSeries.take(100).map(f => f.toDouble).toArray
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
        //println(out.toSeq)

    }
    test("heartIntraTime should obtain detailed heart rate data"){
        val fb = FitbitDataStream("src/main/resources/tetsu.sato.toml")
        assert(fb != null) 
        val targetDate = "2022-03-01"
        val startTimeString = "00:00"
        val endTimeString = "01:00"
        val startDateTimeString = s"${targetDate}T${startTimeString}"
        val endDateTimeString = s"${targetDate}T${endTimeString}"
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
        val dataSeries = fb.getActivityHeartIntradayDataSeries(targetDate, startTimeString, endTimeString).take(10).takeRight(3)
        assert(dataSeries == List(Left("00:00:07"), Right(Dataset("00:00:08",91)), Left("00:00:09")))
        //val ui = new UI
        //ui.flush(dataSeries.toArray)
    /**
        val width = 600
        val height = 500
        val title = "Heart Rate"
        val x_axis = "Time"
        val y_axis = "Signal"
        val fig = new Plotting(width, height, title, x_axis, y_axis)
        fig.initialisePlot()
        val signal1 = dataSeries.take(100).map{f =>
            f match
                case Right(data) =>  data.value.toDouble
                case Left(data) => 0.0
        }.toArray
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
        //println(out.toSeq)
      * */
    }

    test("access token should be introspected"){
        val fb = FitbitDataStream("src/main/resources/tetsu.sato.toml")
        assert(fb != null) 
        val ret = fb.introspect
        println(ret)
    }

    test("activityIntraday should obtain detailed activity data"){
        val fb = FitbitDataStream("src/main/resources/tetsu.sato.toml")
        assert(fb != null) 
        val targetDate = "2022-06-07"
        val startTimeString = "19:30"
        val endTimeString = "20:30"
        val startDateTimeString = s"${targetDate}T${startTimeString}"
        val endDateTimeString = s"${targetDate}T${endTimeString}"
        var ret = fb.activityIntraday("calories", targetDate, "1min", startTimeString, endTimeString)
        println(ret)
        ret = fb.activityIntraday("distance", targetDate, "1min", startTimeString, endTimeString)
        println(ret)
    }
    test("activityLogList should obtain activity log list"){
        val fb = FitbitDataStream("src/main/resources/tetsu.sato.toml")
        assert(fb != null) 
        val targetDate = "2022-03-01"
        val startTimeString = "00:00"
        val endTimeString = "01:00"
        val startDateTimeString = s"${targetDate}T${startTimeString}"
        val endDateTimeString = s"${targetDate}T${endTimeString}"
        var ret = fb.getActivityLogList(startDateTimeString)
        Logger.debug("ret={}",ret)
        Logger.debug("startTime={}", ret.activities(1))
        Logger.debug("originalStartTime={}", ret.activities(1).originalStartTime)
    }
    test("getSleepDataSeries should obtain detailed sleep data"){
        val fb = FitbitDataStream("src/main/resources/tetsu.sato.toml")
        assert(fb != null) 
        val targetDate = "2022-04-01"
        val startTimeString = "19:30"
        val endTimeString = "20:30"
        val startDateTimeString = s"${targetDate}T${startTimeString}"
        val endDateTimeString = s"${targetDate}T${endTimeString}"
        var ret = fb.getSleepDataSeries(targetDate)
        println(ret)
        Logger.debug("ret.sleep={}", ret.sleep.toSeq)
        Logger.debug("ret.sleep(0)={}", ret.sleep.toSeq(0))
        Logger.debug("ret.sleep(0).dateOfSleep={}", ret.sleep.toSeq(0).dateOfSleep)
        Logger.debug("ret.sleep(0).duration={}", ret.sleep.toSeq(0).duration)
        Logger.debug("ret.sleep(0).endTime={}", ret.sleep.toSeq(0).endTime)
        Logger.debug("ret.sleep(0).levels={}", ret.sleep.toSeq(0).levels)
        Logger.debug("ret.sleep(0).levels.data={}", ret.sleep.toSeq(0).levels.data.toSeq)
        Logger.debug("ret.summary={}", ret.summary)

    }
