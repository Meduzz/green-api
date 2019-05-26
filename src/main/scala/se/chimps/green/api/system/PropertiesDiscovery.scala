package se.chimps.green.api.system

import se.chimps.green.spi.Discovery

class PropertiesDiscovery extends Discovery {
	override def lookup(id:String):Seq[(String, Int)] = {
		val prop = System.getenv("JOIN")

			if (prop != null) {
				prop.split(",")
					.map(addr => {
						val Array(host:String, port:String) = addr.split(":")
						(host, port.toInt)
					})
			} else {
				Seq()
			}
	}

	override def register(id:String, service:String, ip:String, port:Int):Unit = {}

	override def deregister(id:String):Unit = {}
}
