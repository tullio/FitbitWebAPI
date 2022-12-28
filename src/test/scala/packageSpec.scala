package org.example
import breeze.plot.{Figure, Plot, plot}
import com.example.pf.model.{LinearGaussianObservationModel, LinearGaussianSystemModel}
import com.example.pf.Tensor
import org.scalactic.*
import org.scalactic.Tolerance.*
import org.scalatest.*
import org.scalatest.flatspec.*
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.*
import org.tinylog.Logger
import com.github.nscala_time.time.Imports._

class packageSpec extends AnyFunSuite:
    test("Basic function of readProperties should be constructed") {
        val prop = readProperties
        Logger.debug("prop = {}", prop)
        assert(prop != null)
    }
    test("Date-time information should be calculated") {
        val prop = readProperties
        val startDate = prop.get("StartDate").asInstanceOf[String]
        val startTime = prop.get("StartTime").asInstanceOf[String]
        val endDate = prop.get("EndDate").asInstanceOf[String]
        val endTime = prop.get("EndTime").asInstanceOf[String]
        val processStartDateObj = DateTime.parse(s"${startDate}T${startTime}")
        val processEndDateObj = DateTime.parse(s"${endDate}T${endTime}")
        val interval = new Interval(processStartDateObj, processEndDateObj)
        //Logger.debug("Duration Object={}", interval.toDuration.getStandardDays)
        //Logger.debug("Duration Object={}", interval.toDuration.getStandardHours)
        //Logger.debug("Period Object={}", interval.toPeriod.getDays)
        //Logger.debug("Period Object={}", interval.toPeriod.getHours)
        assert(interval.toDuration.getStandardDays == 24)
        assert(interval.toDuration.getStandardHours == 119)
    }
