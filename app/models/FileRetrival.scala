package models;

import scala.collection.immutable.List
import scala.collection.mutable.ListBuffer

import org.apache.commons.vfs2._
import org.apache.commons.vfs2.auth.StaticUserAuthenticator
import org.apache.commons.vfs2.impl.DefaultFileSystemConfigBuilder

object FileRetrival {

   def getFilePathsFromFTP(ftpSite: String, userName: String, password: String): List[String] = {
      var filePaths: ListBuffer[String] = new ListBuffer[String]()
      try {
         val authenticator: StaticUserAuthenticator = new StaticUserAuthenticator(ftpSite, userName, password)
         val opts: FileSystemOptions = new FileSystemOptions()
         DefaultFileSystemConfigBuilder.getInstance().setUserAuthenticator(opts, authenticator)
         var fileObj = VFS.getManager().resolveFile("ftp://" + ftpSite, opts)
         filePaths = getFilePathsFromRoot(fileObj)
         return filePaths.toList
      } catch {
         case ex: FileSystemException => {
            return new ListBuffer[String]().toList
         }
      }
   }

   def getFilePathsFromLocalDir(dirPath: String): List[String] = {
      var filePaths: ListBuffer[String] = new ListBuffer[String]()
      try {
         var fileObj = VFS.getManager().resolveFile("file://" + dirPath)
         filePaths = getFilePathsFromRoot(fileObj)
         return filePaths.toList
      } catch {
         case ex: FileSystemException => {
            println("Error when attempting to access directory at: " + dirPath)
            println(ex)
            return new ListBuffer[String]().toList
         }
      }
   }

   def getFilePathsFromRoot(fileObj: FileObject): ListBuffer[String] = {
      val filePaths = new ListBuffer[String]()

      println(fileObj.getName().getPath())
      if (fileObj.getType() == FileType.FOLDER) {
         var children = fileObj.getChildren()
         for (child: FileObject <- children) {
            filePaths.appendAll(getFilePathsFromRoot(child))
         }
      } else {
         filePaths += fileObj.getName().getPath()
      }
      return filePaths
   }
}
