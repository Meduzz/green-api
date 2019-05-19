package se.chimps.green.api.workers

trait PooledWorker extends Worker {
	/**
		* Initial number of workers to spawn
		* @return
		*/
	def count:Int
}
