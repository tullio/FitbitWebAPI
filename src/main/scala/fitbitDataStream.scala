package org.example

import better.files.File
import com.electronwill.nightconfig.toml.TomlParser
import scala.jdk.CollectionConverters._
import org.apache.hc.client5.http.impl.classic.HttpClients
import sttp.client3._
import io.circe.parser.{parse => jsonParse, decode}
import io.circe._
import io.circe.syntax._
import java.util.Base64
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import org.mapdb._
import com.github.nscala_time.time.Imports._
import scala.annotation.meta.param

class FitbitDataStream(fileName: String):
    val res = File(fileName).contentAsString
    val tomlParser = TomlParser()
    val params = tomlParser.parse(res)
    val clientId = params.get("clientId").asInstanceOf[String]
    val userId = params.get("userId").asInstanceOf[String]
    val clientSecret = params.get("clientSecret").asInstanceOf[String]
    val code = params.get("code").asInstanceOf[String]
    val scopes = "activity heartrate location sleep"
    val responseType = "code"
    //val responseType = "token"
    val mapdbFile = params.get("mapdbFile").asInstanceOf[String]
    val db =   
             try
               DBMaker.fileDB(mapdbFile).make
             catch
               case e: org.mapdb.DBException.FileLocked => println(s"DBException: ${e}")
                         println("Copying original db...")
                         File(mapdbFile).copyTo(File(s"${mapdbFile}.bak"), true)
                         println("Deleting original db...")
                         File(mapdbFile).delete(true)
                         DBMaker.fileDB(mapdbFile).make
               case e: Throwable => println(e); DBMaker.fileDB("/tmp/db").make
//    println(s"a=${a.getClass}")
//    val db =               DBMaker.fileDB(mapdbFile).make
    val mapdb = db.hashMap("mapdb", Serializer.STRING, Serializer.STRING).createOrOpen
    //var accessToken = params.get("accessToken").asInstanceOf[String]
    //var refreshToken = params.get("refreshToken").asInstanceOf[String]
    var accessToken = mapdb.get("accessToken")
    var refreshToken = mapdb.get("refreshToken")
    var codeVerifier = "01234567890123456789012345678901234567890123456789"
    if accessToken == null then
        println("accessToken is empty")
        getAccessToken
    def close =
        db.close

    def introspect =
        val url = uri"https://api.fitbit.com/1.1/oauth2/introspect"
        val body = collection.mutable.Map[String, String]()
        body("token") = accessToken
        //body("token") = refreshToken
        println(s"request body=${body.asJson}")
        println(s"request body=${body.asJson.toString}")
        val req = basicRequest.auth.bearer(accessToken)
                   .header("Content-type", "application/x-www-form-urlencoded")
                   .body(body.toMap)
                   .post(url)
                   .response(asString)
        println(s"req=${req}")
        val backend = HttpURLConnectionBackend()
        val res = req.send(backend)
        println(res)
        if res.code.isSuccess then
          val doc = jsonParse(res.body.right.get).right.get
          val cur = doc.hcursor
          println(doc)
          val active = cur.downField("active").as[Boolean].right.get
          if active then
              val scope =  cur.downField("scope").as[String].right.get
              val clientId =  cur.downField("client_id").as[String].right.get
              val userId =  cur.downField("user_id").as[String].right.get
              val exp = cur.downField("exp").as[Long].right.get.toDateTime
              val iat = cur.downField("iat").as[Long].right.get.toDateTime
              println(s"token=${accessToken}")
              println(s"active=${active}")
              println(s"clientId=${clientId}")
              println(s"userId=${userId}")
              println(s"expiration date=${exp}")
              println(s"issued date=${iat}")
          else
              println(doc)
        else
          println(res)
    def getAccessToken =
        val backend = HttpURLConnectionBackend()
        //val basicToken = Base64.encodeString(s"${clientId}:${clientSecret}")
        val body = collection.mutable.Map[String, String]()
        body("client_id") = clientId
        body("code") = code
        body("code_verifier") = codeVerifier
        //body("grant_type") = "client_credentials"
        body("grant_type") = "authorization_code"
        println(body.toMap)
        //val url = uri"https://api.fitbit.com/oauth2/token?${body.toMap}"
        val url = uri"https://api.fitbit.com/oauth2/token"
        val req = basicRequest.auth.basic(clientId, clientSecret)
        //val req = basicRequest
                       .get(url)
                       .body(body.toMap)
                       .response(asString)
        println(s"req=${req}")
        val ret = req.send(backend)
        println(s"ret=${ret}")
        if ret.code.isSuccess then
            //println(s"ret.body=${ret.body}")
            val doc = jsonParse(ret.body.getOrElse("")).getOrElse(Json.Null)
            val cur = doc.hcursor
            accessToken = cur.downField("access_token").as[String].getOrElse("")
            println(s"access token=${accessToken}")
            refreshToken = cur.downField("refresh_token").as[String].getOrElse("")
            println(s"refresh token=${refreshToken}")
            mapdb.put("accessToken", accessToken)
            mapdb.put("refreshToken", refreshToken)
        else
            val doc = jsonParse(ret.body.left.get).right.get
            println(s"left.get=${doc}")
            val cur = doc.hcursor
            val errorType = cur.downField("errors").downArray
                             .downField("errorType").as[String].right.get
            val errorMessage = cur.downField("errors").downArray
                             .downField("message").as[String].right.get
            println(s"${errorType}:${errorMessage}")
            errorType match
                case "invalid_grant" => requestAuthorization



    def requestAuthorization =
        //val codeVerifier = for(i <- Range(0, 96)) "-"
        codeVerifier = "01234567890123456789012345678901234567890123456789"
        println(s"codeVerifier=${codeVerifier}")
        val utf8 = StandardCharsets.UTF_8
//        val c = new String(Base64.getEncoder.encode(generateHash(codeVerifier).getBytes(utf8)), utf8)
        val c = new String(Base64.getEncoder.encode(generateHash(codeVerifier)), utf8)
        println(s"c=${c}")
//        val codeChallenge = URLEncoder.encode(c, utf8)
        val codeChallenge = c.replace("+","-").replace("/", "_").replace("=","")
        println(s"codeChallenge=${codeChallenge}")

        val backend = HttpURLConnectionBackend()
        val body = collection.mutable.Map[String, String]()
        body("client_id") = clientId
        body("scope") = scopes
        body("code_challenge") = codeChallenge
        body("code_challenge_method") = "S256"
        body("response_type") = "code"
        val url = uri"https://api.fitbit.com/oauth2/authorize?${body.toMap}"
        val req = basicRequest
                       .get(url)
                       .response(asString)
        println(s"req=${req}")
        val ret = req.send(backend)
        //println(s"ret=${ret}")
        println(s"headers=${ret.headers}")
        println(s"request=${ret.request}")
        println(s"request.method=${ret.request.method}")
        println(s"request.uri(for obtaining code)=${ret.request.uri}")
        println(s"request.headers=${ret.request.headers}")
        //val ret2 = basicRequest.get(ret.request.uri).response(asString).send(backend)
        //println(s"ret2=${ret2}")
/*
        val basicToken = Base64.encodeString(s"${clientId}:${clientSecret}")
        val url = "https://api.fitbit.com/oauth2/token"
        val body = collection.mutable.Map[String, String]()
        body("grant_type") = "client_credentials"
        val req = Http(url)
                     .header("Authorization", s"Basic ${basicToken}")
                     .postForm(body.toSeq)        
         //println(s"req=${req}")
         val ret = req.asString
         //println(s"ret=${ret}")
         //println(s"ret.body=${ret.body}")
         val doc = jsonParse(ret.body).getOrElse(Json.Null)
         val cur = doc.hcursor
         val token = cur.downField("access_token").as[String].getOrElse("")
         //println(s"token=${cur.downField("access_token").as[String].getOrElse("")}")
         accessToken = token
         token
 */
    def heart(date: String, period: String) =
        val url = s"https://api.fitbit.com/1/user/${userId}/activities/heart/date/${date}/${period}.json"
        getApiResponse(url)
    /**
        Retrieves the heart rate intraday time series data on a specific date or 24 hour period. 
        @param date The date in the format yyyy-MM-dd or today.
        @param detailLevel Number of data points to include. Supported: 1sec | 1min
    **/
    def heartIntraday(date: String, detailLevel: String) =
        val url = s"https://api.fitbit.com/1/user/${userId}/activities/heart/date/${date}/1d/${detailLevel}.json"
        getApiResponse(url)
    /**
        Retrieves the heart rate intraday time series data on a specific date or 24 hour period. 
        @param date The date in the format yyyy-MM-dd or today.
        @param detailLevel Number of data points to include. Supported: 1sec | 1min
        @param startTime The start of the time period in the format HH:mm.
        @param endTime The end of the time period in the format HH:mm.
    **/
    def heartIntraday(date: String, detailLevel: String, startTime: String, endTime: String) =
        val url = s"https://api.fitbit.com/1/user/${userId}/activities/heart/date/${date}/1d/${detailLevel}/time/${startTime}/${endTime}.json"
        getApiResponse(url)

    /**
        @param targetDate target date string in the format yyyy-MM-dd
        @param startTimeString start time string in the format mm:ss
        @param endTimeString end time string in the format mm:ss
     **/
    def getActivityHeartIntradayDataSeries(targetDate: String, startTimeString: String, endTimeString: String) =
        val startDateTimeString = s"${targetDate}T${startTimeString}"
        val endDateTimeString = s"${targetDate}T${endTimeString}"
        val retJson = heartIntraday(targetDate, "1sec", startTimeString, endTimeString)
        val heart = ActivityHeartTime(retJson)
        val dataset = heart.`activities-heart-intraday`.dataset.toSeq
        val dataSeries = scala.collection.mutable.ArrayBuffer.empty[Long]
        val fmt = DateTimeFormat.forPattern("HH:mm:ss")
        val startDateTime = DateTime.parse(startDateTimeString)
        val endDateTime = DateTime.parse(endDateTimeString)
        val timeSeries = startDateTime.toInterval(endDateTime).toStringTimeSeries(fmt)
        val complemented = timeSeries.map{f =>
              val target = dataset.filter(g => g.time == f)
              target.length match
                  case 0 => Dataset(f, 59L)
                  case 1 => target(0)
                  case _ => Dataset("error", 0L)
          }
        (0 +: complemented).zip(complemented :+ 0).slice(1, complemented.length).foreach{f =>
               dataSeries += f._1.asInstanceOf[Dataset].value
               //file.appendLine(s"${f._1.asInstanceOf[Dataset].time.substring(0, 5)} ${f._1.asInstanceOf[Dataset].value.toDouble} ${f._2.asInstanceOf[Dataset].value.toDouble}")
          }
        dataSeries

    def sleep(date: String) =
        val url = s"https://api.fitbit.com/1.2/user/${userId}/sleep/date/${date}.json"
        getApiResponse(url)
    def sleep(fromDate: String, toDate: String) =
        val url = s"https://api.fitbit.com/1.2/user/${userId}/sleep/date/${fromDate}/${toDate}.json"
        getApiResponse(url)
    def getApiResponse(uriString: String): String =
        val uri = uri"$uriString"
        val req = basicRequest.auth.bearer(accessToken)
                   .get(uri)
                   .response(asString)
        println(s"req=${req}")
        val backend = HttpURLConnectionBackend()
        val res = req.send(backend)
        println(s"res=${res}")
        println(s"code=${res.code}")
        val ret = if res.code.isSuccess then
          val ret = res.body.getOrElse("")
          ret
        else
          val doc = jsonParse(res.body.left.get).right.get
          println(s"left.get=${doc}")
          val cur = doc.hcursor
          val errorType = cur.downField("errors").downArray
                             .downField("errorType").as[String].right.get
          val ret = errorType match
              case "expired_token" => println("token was expired. refresh.")
                                      getRefreshToken
                                      getApiResponse(uriString)
          ret
        ret

    def getRefreshToken =
        val url = uri"https://api.fitbit.com/oauth2/token"
        val body = collection.mutable.Map[String, String]()
        body("grant_type") = "refresh_token"
        body("refresh_token") = refreshToken
        body("client_id") = clientId
        //val req = basicRequest.header("authorization", s"Basic ${accessToken}")
        val req = basicRequest.auth.basic(clientId, clientSecret)
                   .post(url)
                   .body(body.toMap)
                   .response(asString)
        println(s"req=${req}")
        val backend = HttpURLConnectionBackend()
        val res = req.send(backend)
        if res.code.isSuccess then
          println("refresh success")
          println(res)
          println(res.body.right.get)
          val doc = jsonParse(res.body.right.get).right.get
          val cur = doc.hcursor
          accessToken = cur.downField("access_token").as[String].right.get
          refreshToken = cur.downField("refresh_token").as[String].right.get
          mapdb.put("accessToken", accessToken)
          mapdb.put("refreshToken", refreshToken)
        else
          println(res)
          println("refresh failure")
    def generateHash(password: String) = 
        import java.security.MessageDigest
        val digest = MessageDigest.getInstance("SHA-256")
        val result = digest.digest(password.getBytes)
        println(result.toSeq)
        import java.math.BigInteger
        val hash = String.format("%040x", new BigInteger(1, result))
        println(s"hash=${hash}")
        hash
        result

/*
    def authorize =
        val basicToken = Base64.encodeString(s"${clientId}:${clientSecret}")
        val json =
            s"""{ 
               "response_type": "${responseType}",
               "client_id": "${clientId}",
               "scope": "${scopes}"
             } """.stripMargin
        val json2 =
            s"""{ 
               "grant_type": "client_credentials"
             } """.stripMargin
        println(s"json2=${json2}")
         //val ret = HttpClient("").post("https://www.fitbit.com/oauth2/authorize", json).get
         //val req = Http("https://www.fitbit.com/oauth2/authorize")
         val req = Http("https://api.fitbit.com/oauth2/token")
//                 .header("Content-Type", "application/json")
                 .header("Authorization", s"Basic ${basicToken}")
                 .postForm(Seq("grant_type" -> "client_credentials"))
         //        .postData(json)
         println(s"req=${req}")
         val ret = req.asString
         println(s"res=${res}")
         ret
 */

