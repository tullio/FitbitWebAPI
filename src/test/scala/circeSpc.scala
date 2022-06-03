package org.example
import org.scalatest._
import org.scalatest.funsuite.AnyFunSuite
import io.circe.parser.decode
import io.circe._
class circeSpec extends AnyFunSuite:
    test("sleep json object should be converted"){
        val jsonTest = 
          s"""
            {
              "sleep": [
                {
                  "dateOfSleep": "2020-01-27"
                }
               ]
            }
          """.stripMargin
        val json = 
          s"""
            {
              "sleep": [
                {
                  "dateOfSleep": "2020-01-27",
                  "duration": 4560000,
                  "efficiency": 86,
                  "endTime": "2020-01-27T00:17:30.000",
                  "infoCode": 2,
                  "isMainSleep": true,
                  "levels": {
                    "data": [
                      {
                        "dateTime": "2020-01-26T23:01:00.000",
                        "level": "restless",
                        "seconds": 360
                      },
                      {
                        "dateTime": "2020-01-26T23:07:00.000",
                        "level": "asleep",
                        "seconds": 1800
                      }
                    ],
                    "summary": {
                      "asleep": {
                        "count": 3,
                        "minutes": 65
                      },
                      "awake": {
                        "count": 0,
                        "minutes": 0
                      },
                      "restless": {
                        "count": 4,
                        "minutes": 11
                      }
                    }
                  },
                  "logId": 25647389515,
                  "minutesAfterWakeup": 0,
                  "minutesAsleep": 65,
                  "minutesAwake": 11,
                  "minutesToFallAsleep": 0,
                  "startTime": "2020-01-26T23:01:00.000",
                  "timeInBed": 76,
                  "type": "classic"
                }
              ] }
          """.stripMargin
        println(s"decode=${decode[Sleep](json)}")
        val sleep = decode[Sleep](json).right.get
        assert(decode[Sleep](json) != null) 
        assert(sleep.sleep(0).dateOfSleep == "2020-01-27")

}
