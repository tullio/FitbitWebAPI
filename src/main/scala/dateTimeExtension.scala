package org.example
import com.github.nscala_time.time.Imports._
import org.joda.time.{DateTime, LocalTime}
import org.joda.time.Interval
import org.joda.time.format.DateTimeFormatter
import org.joda.time.Instant

extension (t: LocalTime)
    def toSec =
        t.getHourOfDay*60*60+t.getMinuteOfHour*60+t.getSecondOfMinute

extension (t: DateTime)
    def toSec =
        t.toInstant.getMillis/1000
    def toInterval(e: DateTime) = new Interval(t, e)

extension (t: Interval)
    def toStringTimeSeries(fmt: DateTimeFormatter) =
        //println(s"start:${t.getStart}")
        (t.getStart.toSec until t.getEnd.toSec).toList
           .map(f => (new LocalTime(f*1000)).toString(fmt))

