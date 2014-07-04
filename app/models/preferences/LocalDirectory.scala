package models.preferences

case class LocalDirectory(id: Long, directory: String, totalfiles: Int)

object LocalDirectory {
  var localDirectories = Set(
    LocalDirectory(1L, "/usr/local/ega", 33),
    LocalDirectory(2L, "/test/dir", 567),
    LocalDirectory(3L, "/home/user", 92))

  def findAll = localDirectories.toList.sortBy(_.directory)

  def findById(id: Long) = localDirectories.find(_.id == id)

  def add(localDirectory: LocalDirectory) {
    localDirectories = localDirectories + localDirectory
  }
}