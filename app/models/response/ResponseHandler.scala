package models.response

import scala.xml._

object ResponseHandler {

  /**
   * Parses the EGA xml response passed in as a string. Returns the list of accessions.
   */
  def responseAccessions(response: String): RestouleResponse = {
    var accessions: List[Accession] = List()
    var errors: List[String] = List()
    var restouleResponse = RestouleResponse(false, accessions, errors)
    val xml = XML.loadString(response)
    val success = (xml \\ "RECEIPT" \ "@success").toString()

    if (success == "true") {
      (xml \\ "RUN").foreach { xml => accessions = accessions :+ RunAccession((xml \ "@alias").toString(), (xml \ "@accession").toString()) }
      (xml \\ "SAMPLE").foreach { xml => accessions = accessions :+ SampleAccession((xml \ "@alias").toString(), (xml \ "@accession").toString()) }
      (xml \\ "EXPERIMENT").foreach { xml => accessions = accessions :+ ExperimentAccession((xml \ "@alias").toString(), (xml \ "@accession").toString()) }
      restouleResponse = RestouleResponse(true, accessions, errors)
    } else {
      (xml \\ "ERROR").foreach{ xml => errors = errors :+ xml.text}
      restouleResponse = RestouleResponse(false, accessions, errors)
    }
    restouleResponse
  }

}

trait Accession {
  def alias: String
  def accession: String
}

case class SampleAccession(alias: String, accession: String) extends Accession
case class ExperimentAccession(alias: String, accession: String) extends Accession
case class RunAccession(alias: String, accession: String) extends Accession

case class RestouleResponse(success: Boolean, studyAbstract: Seq[Accession], errors: List[String])
