package org.example
import io.circe._
import io.circe.generic.semiauto._
import io.circe.parser.decode
case class Dataset(
    time: String,
    value: Long
    )
object Dataset:
  implicit val decoder: Decoder[Dataset] = deriveDecoder
