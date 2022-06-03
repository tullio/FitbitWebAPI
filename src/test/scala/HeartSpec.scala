package org.example
import org.scalatest._
import org.scalatest.funsuite.AnyFunSuite
import io.circe.parser.decode
import io.circe._
class HeartSpec extends AnyFunSuite:
    test("heart rate object should be converted"){
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
              "activities-heart": [
                {
                  "dateTime": "2019-05-08",
                  "value": {
                    "customHeartRateZones": [
                      {
                        "caloriesOut": 1164.09312,
                        "max": 90,
                        "min": 30,
                        "minutes": 718,
                        "name": "Below"
                      },
                      {
                        "caloriesOut": 203.65344,
                        "max": 110,
                        "min": 90,
                        "minutes": 74,
                        "name": "Custom Zone"
                      }
                    ],
                    "heartRateZones": [
                      {
                        "caloriesOut": 979.43616,
                        "max": 86,
                        "min": 30,
                        "minutes": 626,
                        "name": "Out of Range"
                      },
                      {
                        "caloriesOut": 514.16208,
                        "max": 121,
                        "min": 86,
                        "minutes": 185,
                        "name": "Fat Burn"
                      }
                    ],
                    "restingHeartRate": 76
                  }
                }
              ],
              "activities-heart-intraday":
                      {"dataset":[{"time":"00:00:00","value":94},
                                  {"time":"00:01:00","value":97}],
                       "datasetInterval":1,
                       "datasetType":"minute"
                      }
            }
          """.stripMargin
        println(s"decode=${decode[Heart](json)}")
        //val heart = decode[Heart](json).right.get
        val heart = Heart(json)
        assert(decode[Heart](json) != null) 
        assert(heart.`activities-heart`(0).dateTime == "2019-05-08")

}
