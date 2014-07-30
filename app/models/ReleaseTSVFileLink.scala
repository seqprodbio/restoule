package models

import play.api.db.slick.Config.driver.simple._

case class ReleaseTSVFileLink(id: Option[Int], releaseId: Int, tsvFileId: Int, created: java.sql.Timestamp)

class ReleaseTSVFileLinkTable(tag: Tag) extends Table[ReleaseTSVFileLink](tag, "release_tsv_file_link") {
   val releases = TableQuery[ReleaseTable]
   val tsvFiles = TableQuery[TSVFileTable]

   def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
   def releaseId = column[Int]("release_id", O.NotNull)
   def tsvFileId = column[Int]("tsv_file_id", O.NotNull)
   def created = column[java.sql.Timestamp]("created_tstmp", O.NotNull)

   def * = (id.?, releaseId, tsvFileId, created) <> (ReleaseTSVFileLink.tupled, ReleaseTSVFileLink.unapply)

   def release = foreignKey("RELEASE_FK", releaseId, releases)(_.id)
   def tsvFile = foreignKey("TSV_FILE_FK", tsvFileId, tsvFiles)(_.id)
}
