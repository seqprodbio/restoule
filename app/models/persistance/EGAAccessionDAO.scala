package models.persistance

import models.EGAAccession
import models.EGAAccessionTable
import play.api.db.slick.Config.driver.simple._
import models.SampleFile
import controllers.XMLGeneration

object EGAAccessionDAO {

  val egaAccessions = TableQuery[EGAAccessionTable]

  def getAllAccessions() = { implicit session: Session =>
    egaAccessions.list
  }

  def getAllSampleAccessions() = { implicit session: Session =>
    egaAccessions.filter(a => a.resourceType.equals("sample")).list
  }

  def getAllExperimentAccessions() = { implicit session: Session =>
    egaAccessions.filter(a => a.resourceType.equals("experiment")).list
  }

  def getAllRunAccessions() = { implicit session: Session =>
    egaAccessions.filter(a => a.resourceType.equals("run")).list
  }

  def getAccessionById(id: Int) = { implicit session: Session =>
    egaAccessions.filter(a => a.id === id).firstOption
  }

  def getAccessionByName(name: String) = { implicit session: Session =>
    egaAccessions.filter(a => a.refname.equals(name)).firstOption
  }

  def getSubmittedRunsFromRelease(releaseName: String) = { implicit session: Session =>
    egaAccessions.filter(a => a.releaseName.equals(releaseName) && a.resourceType.equals("run")).list
  }

  def createAccession(resourceType: String, refname: String, accession: String, releaseName: String) = { implicit session: Session =>
    val newAccession = new EGAAccession(None, resourceType, accession, refname, releaseName, new java.sql.Timestamp(System.currentTimeMillis()))
    egaAccessions.insert(newAccession)
  }

  def existsWithName(name: String) = { implicit session: Session =>
    egaAccessions.filter(a => a.refname === name).firstOption.isDefined
  }

  def sampleSubmitted(sampleName: SampleFile) = { implicit session: Session =>
    egaAccessions.filter(a => a.refname === XMLGeneration.runAlias(sampleName) && a.resourceType === "run").firstOption.isDefined
  }
}