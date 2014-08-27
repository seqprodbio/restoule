package models.lims

import scala.io.Source

import spray.json._
import spray.json.DefaultJsonProtocol._

/**
 * Retrieve samples in the JSON format from pinery and convert them to case classes.
 */

case class Attribute(name: String, value: JsValue)
case class Status(name: String, state: String)
case class PreparationKit(name: String, description: Option[JsValue])
case class Sample(archived: Option[Boolean], attributes: Option[Either[Seq[Attribute], Attribute]], children: Option[Either[Seq[String], String]], concentration: Option[Float],
  created_by_url: Option[String], created_date: Option[String], description: Option[JsValue],
  id: Option[Int], modified_by_url: Option[String], modified_date: Option[String], name: Option[JsValue], parents: Option[Either[Seq[String], String]],
  project_name: Option[String], sample_type: Option[String], status: Option[Status],
  storage_location: Option[JsValue], tube_barcode: Option[JsValue], url: Option[String], preparation_kit: Option[PreparationKit],
  volume: Option[Float])
case class SampleWrapper(sample: Sample)

object MyJsonProtocol extends DefaultJsonProtocol {
  implicit val attributeFormat = jsonFormat2(Attribute)
  implicit val statusFormat = jsonFormat2(Status)
  implicit val preparationKitFormat = jsonFormat2(PreparationKit)
  implicit val sampleFormat = jsonFormat20(Sample)
  implicit val sampleWrapperFormat = jsonFormat1(SampleWrapper)
}

object LimsJson {

  import MyJsonProtocol._

  def getSamplesFromUrl(): Seq[SampleWrapper] = {
    val samplesText = Source.fromURL("https://pinery.hpc.oicr.on.ca:8443/pinery/samples")
    val jsonAst = samplesText.mkString.parseJson
    jsonAst.convertTo[Seq[SampleWrapper]]
  }

}