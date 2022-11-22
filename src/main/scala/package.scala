package org.example
import org.tinylog.Logger
import com.github.psambit9791.jdsp.misc.{Plotting, UtilMethods}
import scala.jdk.CollectionConverters._
import com.github.nscala_time.time.Imports._
import org.joda.time.Seconds
import org.jetbrains.bio.npy._
import java.nio.file.Paths
import com.example.pf.DataStream
import scala.collection.mutable.ArrayBuffer
import com.example.pf.Tensor
import com.electronwill.nightconfig.core.CommentedConfig    
import com.electronwill.nightconfig.toml.TomlParser
import better.files.File 
import java.nio.file.NoSuchFileException
import java.lang.NullPointerException
import com.electronwill.nightconfig.core.io.ParsingException

def calculateSeconds(startDate: DateTime, endDate: DateTime) =
    val interval = Seconds.secondsBetween(startDate, endDate)
    interval

var params: CommentedConfig = _

def readProperties = 
    if params == null then 
       Logger.debug("params is null({}). try to read configuration...", params)
       val prop = scala.sys.props.get("property.file")    
       Logger.debug("prop={}", prop)
       val tomlParser = TomlParser() 
        params = prop match
            case Some(filename) =>
                 try
                     val resource =  File(filename).contentAsString
                     val params = tomlParser.parse(resource)
                     params
                 catch
                   case e: NoSuchFileException => Logger.error(e)
                                                  null
            case None =>
                 val resource = this.getClass.getClassLoader.getResourceAsStream("ActionDetection.toml") 
                 Logger.debug("resource={}", resource)
                 try
                     val params = tomlParser.parse(resource)  
                     params    
                 catch
                   case e: NoSuchFileException => Logger.error(e)
                                            null
                   case e: NullPointerException => Logger.error(e)
                                            null
                   case e: ParsingException => Logger.error(e)
                                            null
    else
        Logger.debug("params({}) is already set", params)
    params
