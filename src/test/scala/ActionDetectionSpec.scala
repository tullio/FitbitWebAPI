package org.example
import com.example.pf.Tensor
import org.scalactic.*
import org.scalactic.Tolerance.*
import org.scalatest.*
import org.scalatest.flatspec.*
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.*
import com.github.nscala_time.time.Imports._
import org.example

class ActionDetectionSpec extends AnyFunSuite:
  test("Difference between two DateTime Objects should be calculated") {
      val start = DateTime.parse("2022-01-01T00:00:00")
      val end = start.plusDays(1)
      println(start)
      println(end)
      assert(calculateSeconds(start, end).getSeconds == 60*60*24)
  }
