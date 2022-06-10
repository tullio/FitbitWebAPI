package org.example

import org.mapdb._
import better.files.File
import org.tinylog.Logger

class MapDB(mapdbFile: String):

    var db: DB = _
    var mapdb: HTreeMap[String, String]  = _
    def open =
        db =   
                 try
                   DBMaker.fileDB(mapdbFile).make
                 catch
                   case e: org.mapdb.DBException.FileLocked => println(s"DBException: ${e}")
                             println("Copying original db...")
                             File(mapdbFile).copyTo(File(s"${mapdbFile}.bak"), true)
                             println("Deleting original db...")
                             File(mapdbFile).delete(true)
                             DBMaker.fileDB(mapdbFile).make
                   case e: Throwable => println(e)
                              //DBMaker.fileDB(mapdbFile).checksumHeaderBypass.fileDB("/tmp/db").make
                              DBMaker.fileDB(mapdbFile).checksumHeaderBypass.make
    //    println(s"a=${a.getClass}")
    //    val db =               DBMaker.fileDB(mapdbFile).make
        mapdb = db.hashMap("mapdb", Serializer.STRING, Serializer.STRING).createOrOpen
        Logger.debug("MapDB", "Opened.")
    def close =
        db.close
    def get(key: String) =
        mapdb.get(key)
    def put(key: String, value: String) =
        mapdb.put(key, value)
