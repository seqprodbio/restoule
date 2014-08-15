package models;

import scala.io.Source
import scala.collection.immutable.List
import scala.collection.mutable.ListBuffer

import org.apache.commons.vfs2._
import org.apache.commons.vfs2.auth.StaticUserAuthenticator
import org.apache.commons.vfs2.impl.DefaultFileSystemConfigBuilder
import org.apache.commons.vfs2.provider.ftp.FtpFileSystemConfigBuilder;

object FileRetrival {

   def getFilePathsFromFTP(ftpSite: String, userName: String, password: String): List[String] = {
      var filePaths: ListBuffer[String] = new ListBuffer[String]()
      try {
         val authenticator: StaticUserAuthenticator = new StaticUserAuthenticator(ftpSite, userName, password)
         val opts: FileSystemOptions = new FileSystemOptions()
         DefaultFileSystemConfigBuilder.getInstance().setUserAuthenticator(opts, authenticator)
         FtpFileSystemConfigBuilder.getInstance().setDataTimeout(opts, new Integer(600));
         var fileObj = VFS.getManager().resolveFile("ftp://" + ftpSite, opts)
         filePaths = getFilePathsFromRoot(fileObj)
         return filePaths.toList
      } catch {
         case ex: FileSystemException => {
            return new ListBuffer[String]().toList
         }
      }
   }

   def getFileContentsFromFTP(ftpSite: String, userName: String, password: String, filePath: String): String = {
      try {
         val authenticator: StaticUserAuthenticator = new StaticUserAuthenticator(ftpSite, userName, password)
         val opts: FileSystemOptions = new FileSystemOptions()
         DefaultFileSystemConfigBuilder.getInstance().setUserAuthenticator(opts, authenticator)
         FtpFileSystemConfigBuilder.getInstance().setDataTimeout(opts, new Integer(600));
         var fileObj = VFS.getManager().resolveFile("ftp://" + ftpSite + filePath, opts)
         val inputStream = fileObj.getContent().getInputStream()
         return Source.fromInputStream(inputStream).mkString
      } catch {
         case ex: FileSystemException => {
            return ""
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

   def getFileContentsFromLocal(dirPath: String): String = {
      try {
         var fileObj = VFS.getManager().resolveFile("file://" + dirPath)
         val inputStream = fileObj.getContent().getInputStream()
         return Source.fromInputStream(inputStream).mkString
      } catch {
         case ex: FileSystemException => {
            return ""
         }
      }
   }

   def getFilePathsFromRoot(fileObj: FileObject): ListBuffer[String] = {
      val filePaths = new ListBuffer[String]()
      var successful = false
      while (!successful) {
         successful = true
         try {
            println(fileObj.getName().getPath())
            if (fileObj.getType() == FileType.FOLDER) {
               var children = fileObj.getChildren()
               for (child: FileObject <- children) {
                  filePaths.appendAll(getFilePathsFromRoot(child))
               }
            } else {
               filePaths += fileObj.getName().getPath()
            }
         } catch {
            case ex: FileSystemException => {
               println("Timed out when attempting to get children of " + fileObj.getName().getPath())
               successful = false
            }
         }
      }
      return filePaths
   }
}
