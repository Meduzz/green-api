package se.chimps.green.api.system

import akka.actor.ActorRef
import se.chimps.green.api.workers.Worker

trait SystemDelegate {
	def initWorker(worker:Worker):ActorRef
}
