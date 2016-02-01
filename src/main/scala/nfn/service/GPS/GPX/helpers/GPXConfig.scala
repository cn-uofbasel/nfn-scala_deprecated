package nfn.service.GPS.GPX.helpers

import ccn.packet.CCNName

/**
 * Created by Claudio Marxer <marxer@claudio.li>
 *
 */
object GPXConfig {

  // prefix of raw data
  val raw_prefix = CCNName("/ndn/ch/unibas/NDNfit/Joe/internal".substring(1).split("/").toList, None)

  // names of services
  val service_node_prefix = "/ndn/ch/unibas/NDNfit".substring(1)
  val GPXDistanceComputerName = CCNName(service_node_prefix + "/nfn_service_GPX_GPXDistanceComputer")
  val GPXDistanceAggregatorName = CCNName(service_node_prefix + "/nfn_service_GPX_GPXDistanceAggregator")
  val GPXOriginFilterName = CCNName(service_node_prefix + "/nfn_service_GPX_GPXOriginFilter")

}