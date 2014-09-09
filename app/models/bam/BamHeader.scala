package models.bam

import htsjdk.samtools.SamReader
import htsjdk.samtools.SamReaderFactory
import scala.io
import java.io.File
import scala.collection.JavaConverters._
import scala.io.Source

object BamHeader {

  /**
   * Returns the sample name from the bam header.
   */
  def getSampleName(filename: String): String = {
    val file = new File(filename)
    val reader = SamReaderFactory.makeDefault().open(file)
    val readgroups = reader.getFileHeader().getReadGroups().asScala
    var samplename = ""
    readgroups.foreach { f =>
      samplename = f.getSample() // The last value of sample will be returned.
    }
    samplename
  }

}