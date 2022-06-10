package org.example
class MovingAverage(period: Int):
    val queue = new scala.collection.mutable.Queue[Double]
    def +=(v: Double) =
        if queue.length == period then
            queue.dequeue
        queue += v
    def value =
        queue.sum/queue.length
    def length =
        queue.length

