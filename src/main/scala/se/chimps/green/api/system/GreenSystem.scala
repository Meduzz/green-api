package se.chimps.green.api.system

import java.util.concurrent.TimeUnit
import java.util.{ServiceLoader, UUID}

import akka.actor.{ActorRef, ActorSystem, Address, Props}
import akka.cluster.Cluster
import akka.pattern.ask
import akka.util.Timeout
import se.chimps.green.api.GreenApp
import se.chimps.green.api.workers.Worker
import se.chimps.green.spi.Discovery

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.Duration

class GreenSystem {
	private val system = ActorSystem("Green")
	private val id = UUID.randomUUID().toString
	private val cluster = Cluster(system)

	private var app:GreenApp = _
	private var actor:ActorRef = _

	import scala.collection.JavaConverters._
	private val discoveryLoader = ServiceLoader.load(classOf[Discovery])
		.iterator().asScala ++ Seq(new PropertiesDiscovery())

	def start(app:GreenApp):Unit = {
		if (this.app == null) {
			this.app = app

			if (cluster.settings.SeedNodes.isEmpty) {
				val clusterNodes = discoveryLoader.flatMap(d => d.lookup("ComputeModule"))

				if (clusterNodes.nonEmpty) {
					clusterNodes
						.foreach(a => {
							val addr = Address("akka.tcp", "Green", a._1, a._2)
							println(s"Joining: $addr.")
							cluster.join(addr)
						})
				} else {
					println("Found no cluster to join!")
					System.exit(1)
				}
			}

			actor = system.actorOf(Props(classOf[AppActor], id, app.name, app.version), app.name)
			app.start(new SystemDelegateImpl(actor))
		} else {
			println("System is already running!")
			System.exit(1)
		}
	}

	def stop():Unit = {
		app.stop()
		cluster.leave(cluster.selfAddress)
		system.terminate()
  		.map(_ => println("ActorSystem was shutdown."))(ExecutionContext.global)
	}
}

private class SystemDelegateImpl(actor:ActorRef) extends SystemDelegate {
	implicit val timeout = Timeout(2L, TimeUnit.SECONDS)

	override def initWorker(worker:Worker):ActorRef = {
		val futureActor = (actor ? worker).mapTo[ActorRef]
		Await.result(futureActor, Duration(2L, TimeUnit.SECONDS))
	}
}

case class AppAlive(id:String, name:String, version:String, node:String, roles:Seq[String], actors:Map[String, ActorType])
case class ActorType(typ:String, instances:Int, address:String)
case class AppShutdown(id:String)