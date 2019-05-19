package se.chimps.green.api.system

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorRef, Props}
import akka.cluster.Cluster
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import akka.cluster.routing.{ClusterRouterPool, ClusterRouterPoolSettings}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings, ShardRegion}
import akka.routing.RoundRobinPool
import se.chimps.green.api.workers.{PooledWorker, ShardProxyWorker, ShardedWorker, Worker}

import scala.concurrent.duration.Duration

class AppActor(id:String, name:String, version:String) extends Actor {
	val cluster = Cluster(context.system)
	val mediator = DistributedPubSub(context.system).mediator

	var actors:Map[String, ActorType] = Map()

	override def receive:Receive = {
		case s:ShardProxyWorker => sender() ! shardProxyActor(s.shardRegion())
		case s:ShardedWorker => sender() ! shardedActor(s.props, s.name, s.extractEntityId, s.extractShardId)
		case p:PooledWorker => sender() ! clusterPoolActor(p.props, p.name, p.count)
		case w:Worker => sender() ! localActor(w.props, w.name)
		case Ping => {
			sendAlive(cluster.selfAddress.hostPort.split("@")(1), cluster.selfRoles.toSeq)
		}
	}

	override def preStart():Unit = {
		context.system.scheduler.schedule(Duration(15, TimeUnit.SECONDS), Duration(15, TimeUnit.SECONDS), self, Ping)(context.dispatcher)
	}

	private def sendAlive(node:String, roles:Seq[String]): Unit = {
		val evt = AppAlive(id, name, version, node, roles, actors)
		mediator ! Publish("app.alive", evt)
	}


	override def postStop():Unit = {
		mediator ! AppShutdown(id)
	}

	private def localActor(props:Props, name:String):ActorRef = {
		val actor = context.actorOf(props, name)

		actors = actors ++ Map(name -> ActorType("local", 1, actor.path.toSerializationFormat))
		actor
	}

	private def clusterPoolActor(props:Props, name:String, count:Int):ActorRef = {
		val actor = context.actorOf(
			ClusterRouterPool(
				RoundRobinPool(count),
				ClusterRouterPoolSettings(100, 10, false, "compute"))
				.props(props),
			name)

		actors = actors ++ Map(name -> ActorType("pooled", count, actor.path.toSerializationFormat))
		actor
	}

	private def shardedActor(props:Props,
	                         name:String,
	                         extractEntityId:ShardRegion.ExtractEntityId,
	                         extractShardId: ShardRegion.ExtractShardId):ActorRef = {

		val actor = ClusterSharding(context.system)
			.start(name,
				props,
				ClusterShardingSettings(context.system).withRole("compute"),
				extractEntityId,
				extractShardId)

		actors = actors ++ Map(name -> ActorType("sharded", 1, actor.path.toSerializationFormat))
		actor
	}

	private def shardProxyActor(shardRegion:String):ActorRef = {

		val actor = ClusterSharding(context.system)
			.shardRegion(shardRegion)

		actors = actors ++ Map(name -> ActorType("proxy", 1, actor.path.toSerializationFormat))
		actor
	}

}

case object Ping
