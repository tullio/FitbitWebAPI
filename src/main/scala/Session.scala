package org.example

//import com.example.pf.DataStream.timeData
import com.github.nscala_time.time.Imports._
import me.shadaj.scalapy.py
import me.shadaj.scalapy.py.SeqConverters
import org.tinylog.Logger

import scala.jdk.CollectionConverters._
import better.files._
import me.shadaj.scalapy.py
import me.shadaj.scalapy.py.SeqConverters
import ai.djl.ndarray.{NDArrays, NDManager} 
import ai.djl.ndarray.NDArray
import ai.djl.ndarray.index.NDIndex
import ai.djl.ndarray.types.{Shape, DataType}
import scala.annotation.targetName
import ai.djl.Device
import scala.collection.mutable.ListBuffer
import org.example.ActionTarget
import scala.languageFeature.existentials

import scala.collection.parallel.CollectionConverters.*

class Session:
    var sessionName = ""
    def setSession(name: String) =
        sessionName = name.replaceAll(" ", "_")
        Logger.tags("NOTICE").info("path={}", File(sessionName))
        File(sessionName).createDirectoryIfNotExists

    
