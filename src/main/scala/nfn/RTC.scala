package nfn

import ccn.packet.CCNName
import com.typesafe.scalalogging.slf4j.Logging

// Request to computation
case class RTC(computeName: CCNName, request: String, params: List[String]) extends Logging {

}
