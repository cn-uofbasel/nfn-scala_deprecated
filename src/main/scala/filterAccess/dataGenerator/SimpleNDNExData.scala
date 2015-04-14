package filterAccess.dataGenerator

import filterAccess.json._
import filterAccess.crypto.Helpers.symKeyGenerator

/**
 * Created by Claudio Marxer <marxer@claudio.li>
 *
 * Sample data generator (JSON format) for NDNExSetup (and possibly other runnables).
 *
 * Content Channel
 *  Track with 6 track points
 *
 * Key Channel
 *  AES-256 Keys for access level 1, 2, 3
 *
 * Permission Channel
 *  Certain Permissions for 3 different users
 *
 */
object SimpleNDNExData {

  // -----------------------------------------------------------------------------
  // ==== CONTENT CHANNEL ========================================================
  // -----------------------------------------------------------------------------

  /**
   * Generate content data (track with 6 track points)
   *
   * @param    name   Raw data name
   * @param    i      Parameter to influence coordinates of track (deterministic, different i generate different tracks)
   * @return          Content data (track) as String (JSON Object)
   */
  def generateTrackJSON(name: String, i: Int = 0): String = {
    ContentChannelBuilder.buildTrack(
      List(
        TrackPoint(3 + i, 4 + i * 2, 6 + i),
        TrackPoint(4 + i, 4 + i, 6 + i * 3),
        TrackPoint(4 + i * 2, 5 + i, 6 + i),
        TrackPoint(4 + i, 6 + i * 4, 7 + i),
        TrackPoint(6 + i * 2, 6 + i, 5 + i * 2),
        TrackPoint(5 + i, 6 + i, 5 + i * 3)
      ),
      name
    )
  }


  /**
   * Same data as generateTrackJSON but in Array[Byte] (track with 6 track points)
   *
   * @param    name   Raw data name
   * @param    i      Parameter to influence coordinates of track (deterministic, different i generate most likely different tracks)
   * @return          Content data (track) as Array[Byte]
   */
  def generateTrack(name: String, i: Int = 0): Array[Byte] =
    generateTrackJSON(name, i).getBytes


  // -----------------------------------------------------------------------------
  // ==== KEY CHANNEL ============================================================
  // -----------------------------------------------------------------------------

  /**
   * Generate key data (AES-256 Keys for access level 1, 2, 3)
   *
   * @param    name     Raw data name
   * @param    i        Parameter to influence keys (deterministic, different i generate most likely different tracks)
   * @return            Key data as String (JSON Object)
   */
  def generateKeysJSON(name: String, i: Int): String = {
    KeyChannelBuilder.buildKeys(
      Map(
        AccessLevel(0) -> LevelKey(symKeyGenerator((99111111 * i).toString)),   // Symmetric key to encrypt/decrypt permission data
        AccessLevel(1) -> LevelKey(symKeyGenerator((99222222 * i).toString)),   // Symmetric key to encrypt/decrypt unfiltered data
        AccessLevel(2) -> LevelKey(symKeyGenerator((99333333 * i).toString))    // Symmetric key to encrypt/decrypt filtered data of first access level
      ),
      name
    )
  }


  /**
   * Returns same data as generateKeysJSON but in Array[Byte] (AES-256 Keys for access level 1, 2, 3)
   *
   * @param    name   Raw data name
   * @return          Key data as Array[Byte]
   */
  def generateKeys(name: String, i: Int): Array[Byte] =
    generateKeysJSON(name, i).getBytes


  // -----------------------------------------------------------------------------
  // ==== PERMISSION CHANNEL =====================================================
  // -----------------------------------------------------------------------------

  /**
   * Generate permission data (certain permissions for 3 different users)
   *
   * @param    name   Raw data name
   * @return          Permission data as String (JSON Object)
   */
  def generatePermissionsJSON(name: String): String = {
    AccessChannelBuilder.buildPermissions(
      List(
        UserLevel("MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAIkI4jaasIJnSpB12KBQeqlkMx+/H1nZ1MI85JfeI4w/eOiPLog5if71TUyuf6Qy/dPVqTA/a5zPawDJE3nyykMCAwEAAQ==", 1),  // permission to access unfiltered data
        UserLevel("MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAJpoF3jlUz9OOFgvEtraFMuaOuA211Ck3UHuHToMys65tT7PqvY87VNdOflJN1oTqqIuy3b8Hn4r45duJFc9N+MCAwEAAQ==", 0),  // permission to access permission data
        UserLevel("MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAJpoF3jlUz9OOFgvEtraFMuaOuA211Ck3UHuHToMys65tT7PqvY87VNdOflJN1oTqqIuy3b8Hn4r45duJFc9N+MCAwEAAQ==", 1),  // permission to access unfiltered data
        UserLevel("MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAJpoF3jlUz9OOFgvEtraFMuaOuA211Ck3UHuHToMys65tT7PqvY87VNdOflJN1oTqqIuy3b8Hn4r45duJFc9N+MCAwEAAQ==", 2),  // permission to access data on first filter level
        UserLevel("MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAI/KObiAb04130QaeKcE5QWVw/42b5uLaiO8jGDKFMn+Zefxx42rOSkwniJYKJWOqx6kzm4u7Dpma6J6/QVzjB8CAwEAAQ==", 1)   // permission to access unfiltered data
      ),
      name
    )
  }


  /**
   * Same data as generateKPermissionsJSON but in Array[Byte] (certain permissions for 3 different users)
   *
   * @param    name   Raw data name
   * @return          Permission data as Array[Byte]
   */
  def generatePermissions(name: String): Array[Byte] =
    generatePermissionsJSON(name).getBytes

}
