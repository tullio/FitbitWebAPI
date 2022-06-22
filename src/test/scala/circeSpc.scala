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
                   "shortData": [
                     {
                       "dateTime": "2020-02-21T00:10:30.000",
                       "level": "wake",
                       "seconds": 30
                     }
                    ],
                    "summary": {
                      "deep": {
                        "count": 5,
                        "minutes": 104,
                        "thirtyDayAvgMinutes": 69
                      },
                      "light": {
                        "count": 32,
                        "minutes": 205,
                        "thirtyDayAvgMinutes": 202
                      },
                      "rem": {
                        "count": 11,
                        "minutes": 75,
                        "thirtyDayAvgMinutes": 87
                      },
                      "wake": {
                        "count": 11,
                        "minutes": 75,
                        "thirtyDayAvgMinutes": 87
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
              ],
              "summary": {
                "stages": {
                  "deep": 104,
                  "light": 205,
                  "rem": 75,
                  "wake": 78
                },
                "totalMinutesAsleep": 384,
                "totalSleepRecords": 1,
                "totalTimeInBed": 462
              }
           }
          """.stripMargin
        println(s"decode=${decode[Sleep](json)}")
        val sleep = decode[Sleep](json).right.get
        assert(decode[Sleep](json) != null) 
        assert(sleep.sleep(0).dateOfSleep == "2020-01-27")

}
