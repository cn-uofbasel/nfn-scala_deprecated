package nfn.service.GPX.helpers

import ccn.packet.CCNName

/**
 * Created by Claudio Marxer <marxer@claudio.li>
 *
 */
object GPXConfig {

  val data_prefix = CCNName("/ndn/ch/unibas/NDNfit/hidden".substring(1).split("/").toList, None)

  val GPXDistanceComputerName = CCNName(???)
  val GPXDistanceAggregatorName = CCNName(???)

}
