package models.lims

import scala.io.Source

import spray.json._
import spray.json.DefaultJsonProtocol._

/**
 * Retrieve samples in the JSON format from pinery and convert them to case classes.
 */
case class Attribute(name: String, value: String)
case class Status(name: String, state: String)
case class PreparationKit(name: String, description: Option[JsValue])
case class Sample(archived: Option[Boolean], attributes: Option[Seq[Attribute]], children: Option[Either[Seq[String], String]], concentration: Option[Float],
  created_by_url: Option[String], created_date: Option[String], description: Option[String],
  id: Int, modified_by_url: Option[String], modified_date: Option[String], name: String, parents: Option[Either[Seq[String], String]],
  project_name: Option[String], sample_type: Option[String], status: Option[Status],
  storage_location: Option[String], tube_barcode: Option[String], url: String, preparation_kit: Option[PreparationKit],
  volume: Option[Float])

object MyJsonProtocol extends DefaultJsonProtocol {
  implicit val attributeFormat = jsonFormat2(Attribute)
  implicit val statusFormat = jsonFormat2(Status)
  implicit val preparationKitFormat = jsonFormat2(PreparationKit)
  implicit val sampleFormat = jsonFormat20(Sample)
}

object LimsJson {

  type Id = Int
  type Url = String

  import MyJsonProtocol._

  def getSamplesFromUrl(): Seq[Sample] = {
    val samplesText = Source.fromURL("https://pinery.hpc.oicr.on.ca:8443/pinery/samples")
    //    val samplesText = Source.fromFile("/Users/tdebat/Work/pinery/samples.json")
    val jsonAst = samplesText.mkString.parseJson
    jsonAst.convertTo[Seq[Sample]]
  }

  def getSampleMap(samples: Seq[Sample]): Map[Url, Sample] = {
    def getSampleMap0(samples_0: Seq[Sample], acc: Map[Url, Sample]): Map[Url, Sample] = {
      samples_0 match {
        case Nil => acc
        case s :: ss => getSampleMap0(ss, acc + (s.url -> s))
      }
    }
    getSampleMap0(samples, Map())
  }

  /**
   * The JSON list of parent or child urls can
   */
  def getEither(i: Option[Either[Seq[String], String]]): Option[String] = {
    i match {
      case None => None
      case Some(x) => x match {
        case Left(y) => Some(y.head) // Return first item. Not common to have multiple parents
        case Right(y) => Some(y)
      }
    }
  }

  def getDonorSample(sample: Sample, sampleMap: Map[Url, Sample]): Option[Sample] = {
    def getDonorSample0(parentUrl: Option[Url]): Option[Sample] = {
      parentUrl match {
        case None => None
        case Some(u) => sampleMap.get(u) match {
          case Some(x) if x.sample_type.getOrElse("") == "Identity" => Some(x)
          case Some(x) => getDonorSample0(getEither(x.parents))
        }
      }
    }
    getDonorSample0(getEither(sample.parents))
  }

  def getDonorSampleSet(samples: Seq[Sample], sampleMap: Map[Url, Sample]): Set[Sample] = {
    def getDonorSampleSet0(samples0: Seq[Sample], acc: Set[Sample]): Set[Sample] = {
      samples0 match {
        case x :: xs => getDonorSample(x, sampleMap) match {
          case Some(y) => getDonorSampleSet0(xs, acc + y)
          case None => getDonorSampleSet0(xs, acc)
        }
        case Nil => acc
      }
    }
    getDonorSampleSet0(samples, Set())
  }

  def getSampleByName(name: String, samples: Seq[Sample]): Seq[Sample] = {
    def getSampleByName0(samples0: Seq[Sample], acc: List[Sample]): Seq[Sample] = {
      samples0 match {
        case x :: xs if x.name == name => getSampleByName0(xs, x :: acc)
        case x :: xs => getSampleByName0(xs, acc)
        case Nil => acc
      }
    }
    getSampleByName0(samples, Nil)
  }

  // Must be of type Library Seq
  def getLibraryByName(name: String, samples: Seq[Sample]): Seq[Sample] = {
    def getSampleByName0(samples0: Seq[Sample], acc: List[Sample]): Seq[Sample] = {
      samples0 match {
        case x :: xs if x.name == name && x.sample_type.getOrElse("").endsWith("Library Seq") => getSampleByName0(xs, x :: acc)
        case x :: xs => getSampleByName0(xs, acc)
        case Nil => acc
      }
    }
    getSampleByName0(samples, Nil)
  }

  def getAttribute(attributeName: String, sample: Sample, sampleMap: Map[Url, Sample]): Option[Attribute] = {
    def getAttribute(attributes: Seq[Attribute]): Option[Attribute] = {
      attributes match {
        case a :: as => if (a.name == attributeName) Some(a) else getAttribute(as)
        case Nil => None
      }
    }
    def getDonorSample0(parentUrl: Option[Url]): Option[Attribute] = {
      parentUrl match {
        case None => None
        case Some(u) => sampleMap.get(u) match {
          case Some(x) => x.attributes match {
            case Some(attributes) => getAttribute(attributes)
            case None => getDonorSample0(getEither(x.parents))
          }
          case None => None // This means there was a url but no sample. This shouldn't happen.
        }
      }
    }
    getDonorSample0(getEither(sample.parents))
  }

}