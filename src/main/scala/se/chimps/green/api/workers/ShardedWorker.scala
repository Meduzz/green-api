package se.chimps.green.api.workers

import akka.cluster.sharding.ShardRegion

trait ShardedWorker extends Worker {

	def extractEntityId:ShardRegion.ExtractEntityId

	def extractShardId: ShardRegion.ExtractShardId
}
