package models.XMLCreators

case class SampleXMLData(sampleName: String, donorId: String)

case class ExperimentXMLData(libraryName: String, sampleName: String, libraryStrategy: String, librarySource: String, librarySelection: String, nominalLength: Int)

case class RunXMLData(alias: String, runDate: String, experimentRef: String, files: List[SampleFileData])

case class SampleFileData(filePath: String, fileType: String, checksumMethod: String, checksum: String, encryptedChecksum: String)

case class DatasetXMLData(alias: String, runRefs: List[RunReferenceData])

case class RunReferenceData(accession: String, refname: String)