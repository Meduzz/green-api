package se.chimps.green.api

import se.chimps.green.api.system.SystemDelegate

trait GreenApp {
	def name:String
	def version:String

	def start(system:SystemDelegate):Unit
	def stop():Unit
}
