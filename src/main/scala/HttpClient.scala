package org.example
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.client5.http.classic.methods.{HttpPost, HttpDelete}
import org.apache.hc.client5.http.fluent.Request
import org.apache.hc.core5.http.io.entity.StringEntity
import org.apache.hc.core5.http.message.BasicHeader

case class Response(code: Int, message: String, content: String)

class HttpClient(key: String):
    val httpclient = HttpClients.createDefault()
    def post(url: String, json: String) = {

        val httppost = new HttpPost(url)
        val requestEntity = new StringEntity(json)
        httppost.setEntity(requestEntity)
        //httppost.setHeader(new BasicHeader("api-key", key))
        httppost.setHeader(new BasicHeader("Content-Type", "application/json"))
        val response = httpclient.execute(httppost)
        println(s"response=${response}")
        try
          //val res = response.returnResponse
          val res = response.getEntity
          println(res)
          val len = res.getContentLength
          println(s"len=${len}")
          var buffer = new Array[Byte](len.toInt)
          res.getContent.read(buffer)
          val message = buffer.foldLeft("")((f, g) => f + g.toChar)
          //println(s"entity=${response.getEntity}, ${buffer.foldLeft("")((f, g) => f + g.toChar)}")
          //println(s"message=${response.getCode},${response.getReasonPhrase}")
          Some(Response(response.getCode, response.getReasonPhrase, message))
        catch
          case e: Exception => Some(Response(0, "", e.getMessage))
    }
    def delete(q: String) = {

        val httpdelete = new HttpDelete(q)
        httpdelete.setHeader(new BasicHeader("api-key", key))
        //httppost.setHeader(new BasicHeader("Content-Type", "application/json"))
        val response = httpclient.execute(httpdelete)
        try
          //val res = response.returnResponse
          val res = response.getEntity
          Some(Response(response.getCode, response.getReasonPhrase, ""))
        catch
          case e: Exception => Some(Response(0, "", e.getMessage))
    }

