package filterAccess.json

/**
 * Created by Claudio Marxer <marxer@claudio.li>
 *
 * Classes to which JSON objects (content/permission/key channel) map.
 *
 */

// -----------------------------------------------------------------------------
// ==== PERMISSION CHANNEL =====================================================
// -----------------------------------------------------------------------------

case class AccessLevel(level:Int) {
  override def toString() = level.toString
}
case class UserLevel(name: String, level: Int)
case class Permissions(content: String, permissions: List[UserLevel])



// -----------------------------------------------------------------------------
// ==== KEY CHANNEL ============================================================
// -----------------------------------------------------------------------------

case class LevelKey(key:String) {
  override def toString() = key.toString
}
case class Keys(content: String, keys:Map[AccessLevel, LevelKey])



// -----------------------------------------------------------------------------
// ==== CONTENT CHANNEL ========================================================
// -----------------------------------------------------------------------------

case class TrackPoint(x:Int, y:Int, z:Int) {
  def +(s:TrackPoint):TrackPoint = new TrackPoint(this.x+s.x, this.y+s.y, this.z+s.z)
  def -(s:TrackPoint):TrackPoint = new TrackPoint(this.x-s.x, this.y-s.y, this.z-s.z)
  def distance:Int = Math.sqrt(x*x + y*y + z*z).toInt // distance to origin (floor)
  def distance(that:TrackPoint):Int = (this-that).distance // distance to another TrackPoint (floor)
}
case class Track(content:String, trace: List[TrackPoint]) {
  def length:Int = {
    var sum = 0
    for(i <- 0 to trace.length-2){
      sum = sum + trace(i+1).distance(trace(i))
    }
    sum
  }
}

