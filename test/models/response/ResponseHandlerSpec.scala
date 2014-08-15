package models.response

import org.scalatest.Matchers
import org.scalatest.FlatSpec

class ResponseHandlerSpec extends FlatSpec with Matchers {

  val successfulResponse = """<?xml version="1.0" encoding="UTF-8"?><?xml-stylesheet type="text/xsl" href="receipt.xsl"?>
<RECEIPT receiptDate="2014-08-14T20:11:56.329+01:00" submissionFile="submission.xml" success="true">
   <EXPERIMENT accession="EGAX00001211476" alias="PCSI_0074_Ly_R_PE_367_EX" status="PRIVATE" />
   <RUN accession="EGAR00001223846" alias="PCSI_0074_Ly_R_PE_367_EX_110805_SN803_0063_AB01E5ACXX_4_TGACCA" status="PRIVATE" />
   <RUN accession="EGAR00001223847" alias="PCSI_0074_Ly_P_PE_367_EX_110805_SN803_0063_AB01E5ACXX_4_TGACCA" status="PRIVATE" />
   <SAMPLE accession="EGAN00001221879" alias="PCSI_0074_Ly_R" status="PRIVATE" />
   <SUBMISSION accession="EGA00001159601" alias="One Sample Test" />
   <MESSAGES>
      <INFO> Its ega submisison as PROTECT action is used</INFO>
      <INFO> PROTECT action for the following XML: sample.xml experiment.xml run.xml       </INFO>
      <INFO> Unable to get or issue BioSample accession for this sample ERS526871</INFO>
   </MESSAGES>
   <ACTIONS>ADD</ACTIONS>
   <ACTIONS>ADD</ACTIONS>
   <ACTIONS>ADD</ACTIONS>
   <ACTIONS>PROTECT</ACTIONS>
</RECEIPT>"""

  "my response handler" should "find four accessions" in {
    val result = ResponseHandler.responseAccessions(successfulResponse)
    result.success should be(true)
    result.studyAbstract.length should be(4)
  }

}