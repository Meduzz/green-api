package se.chimps.green.api.workers

import akka.actor.Props

trait Worker {
	/**
		* Actor properties
		* @return
		*/
	def props:Props

	/**
		* Actor name, will have your app name prepended.
		* @return
		*/
	def name:String
}
