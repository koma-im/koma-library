package koma.network.client.okhttp

import mu.KotlinLogging
import okhttp3.Dns
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress

private val logger = KotlinLogging.logger {}

object Dns {
    val system = Dns.SYSTEM
    val preferV4 = object : Dns {
        /**
         * Returns the IPv4 addresses first, which will be tried first
         */
        override fun lookup(hostname: String): List<InetAddress> {
            val addrs = system.lookup(hostname)
            addrs.sortedBy { a -> addressPriority(a) }
            logger.debug { "Dns answers sorted into $addrs" }
            return addrs
        }
    }
    val onlyV4 = object : Dns {
        /**
         * Use only IPv4
         */
        override fun lookup(hostname: String): List<InetAddress> {
            val addrs = system.lookup(hostname)
                    .filter { it is Inet4Address }
            logger.debug { "Filtered Ipv4 Dns answers: $addrs" }
            return addrs
        }
    }
}

/**
 * IPv4 first, IPv6 second
 */
private fun addressPriority(address: InetAddress): Int {
    return when (address) {
        is Inet4Address -> 4
        is Inet6Address -> 6
        else -> 8
    }
}
