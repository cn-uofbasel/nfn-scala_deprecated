package filterAccess

/**
 * Created by Claudio Marxer <marxer@claudio.li>
 *
 * Classes to which JSON maps
 *
 */
package object json {

  // Access Channel
  case class UserLevel(name: String, level: Int)
  case class Permissions(content: String, permissions: List[UserLevel])

  // Key Channel
  // TODO

  // Content Channel
  case class TrackPoint(x:Int, y:Int, z:Int) {
    def +(s:TrackPoint):TrackPoint = new TrackPoint(this.x+s.x, this.y+s.y, this.z+s.z)
    def -(s:TrackPoint):TrackPoint = new TrackPoint(this.x-s.x, this.y-s.y, this.z-s.z)
  }
  case class Track(content:String, trace: List[TrackPoint])

}
