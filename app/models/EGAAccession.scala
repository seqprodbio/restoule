package models

import play.api.db.slick.Config.driver.simple._

case class EGAAccession(id: Option[Int], resourceType: String, accession: String, refname: String, releaseName: String, created: java.sql.Timestamp)

class EGAAccessionTable(tag: Tag) extends Table[EGAAccession](tag, "ega_accession") {
   def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
   def resourceType = column[String]("resource_type", O.NotNull)
   def accession = column[String]("accession", O.NotNull)
   def refname = column[String]("refname", O.NotNull)
   def releaseName = column[String]("release_name", O.NotNull)
   def created = column[java.sql.Timestamp]("created_tmstp", O.NotNull)

   def * = (id.?, resourceType, accession, refname, releaseName, created) <> (EGAAccession.tupled, EGAAccession.unapply)
}