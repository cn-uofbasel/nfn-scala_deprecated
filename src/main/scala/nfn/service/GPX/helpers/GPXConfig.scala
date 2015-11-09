package nfn.service.GPX.helpers

import ccn.packet.CCNName

/**
 * Created by Claudio Marxer <marxer@claudio.li>
 *
 */
object GPXConfig {

  val raw_prefix = CCNName("/ndn/ch/unibas/NDNfit/hidden".substring(1).split("/").toList, None)

  val GPXDistanceComputerName = CCNName("nfn/node0/nfn_service_GPX_GPXDistanceComputer") // todo: adjust prefix to setup!
  val GPXDistanceAggregatorName = CCNName("nfn_service_GPX_GPXDistanceAggregator")
  val GPXOriginFilterName = CCNName("nfn_service_GPX_GPXOriginFilter")

}
