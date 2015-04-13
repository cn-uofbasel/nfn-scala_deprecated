package filterAccess.json

/**
 * Created by Claudio Marxer <marxer@claudio.li>
 *
 * Classes to which JSON maps
 *
 */

// Access Channel
case class AccessLevel(level:Int) {
  override def toString() = level.toString
}
case class UserLevel(name: String, level: Int)
case class Permissions(content: String, permissions: List[UserLevel])

// Key Channel
case class LevelKey(key:String) {
  override def toString() = key.toString
}
case class Keys(content: String, keys:Map[AccessLevel, LevelKey])

// Content Channel
case class TrackPoint(x:Int, y:Int, z:Int) {
  def +(s:TrackPoint):TrackPoint = new TrackPoint(this.x+s.x, this.y+s.y, this.z+s.z)
  def -(s:TrackPoint):TrackPoint = new TrackPoint(this.x-s.x, this.y-s.y, this.z-s.z)
}
case class Track(content:String, trace: List[TrackPoint])

