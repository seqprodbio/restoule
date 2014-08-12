package models

import play.api.db.slick.Config.driver.simple._

case class SampleLIMSInfo(id: Option[Int], libraryName: String, donor: String, libraryStrategy: String, librarySource: String, librarySelection: String, created: java.sql.Timestamp)

class SampleLIMSInfoTable(tag: Tag) extends Table[SampleLIMSInfo](tag, "sample_lims_info") {
   def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
   def libraryName = column[String]("library_name", O.NotNull)
   def donor = column[String]("donor", O.NotNull)
   def libraryStrategy = column[String]("library_strategy", O.NotNull)
   def librarySource = column[String]("library_source", O.NotNull)
   def librarySelection = column[String]("library_selection", O.NotNull)
   def created = column[java.sql.Timestamp]("created_tmstp", O.NotNull)

   def * = (id.?, libraryName, donor, libraryStrategy, librarySource, librarySelection, created) <> (SampleLIMSInfo.tupled, SampleLIMSInfo.unapply)
}