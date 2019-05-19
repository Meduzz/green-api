# green-api
A new green api, for a better future.

## Concept

In green you run the compute module (basically an empty akka cluster) on your beefy hardware and run your "apps" on the tiny machines. Why is that? Well, your apps will spend most of their time waiting for IO, that is not very demanding. Your apps will hook into the compute modules and execute their heavy work there through actors.

## Compared to "Serverless"

Do you know what serverless is? Ofc you do. Did you ever try to compose lambdas to solve a complex problem? That's the tricky bit, right? You could use any of the many "flow-design" frameworks out there. But you'd end up writing more config than you have code in your lambdas. Here's where languages with functional idioms have a huge advantage.

Really this is what Green brings to the table. You are in full control of the "app", how it interacts with the world, which IO it waits for. Then it lets you break down the problems into nice chunks, and execute them on the compute modules. You'd be composing futures, which is simple yet very powerful!

## Nitty gritty

First off, your app needs a new dependency. In you ```build.sbt``` add:

> resolvers += "se.chimps.green" at "https://yamr.kodiak.se/maven"

And then that dependency:

> libraryDependencies += "se.chimps.green" %% "green-api" % "20190519"

Remember, you are in full control of the app. So you'll need a main function, but lets wait with that for now.

Start with creating a class that extends GreenApp like this:

    import se.chimps.green.api.GreenApp
    import se.chimps.green.api.system.SystemDelegate

    class ExampleApp extends GreenApp

This will force you to implement 2 functions:

    override def start(system:SystemDelegate):Unit

Here you can connect to nats and start consuming messages, or perhaps boot up a web server. That SystemDelegate param gives you the ability to boot up workers, which basically describes how to connect your actors to the system.

The second method:

    override def stop():Unit

This method is only for clean up purposes, you do clean up after your self, dont you?

Extending GreenApp also forces you to add some metadata like:

    val name:String = "MyFirstApp" // <- should not contain funky chars, since it's used on the actor path.
    val version:String = "1.2.3"

Lets finish of with a nice main function:

    import se.chimps.green.api.system.GreenSystem
    
    import scala.sys.ShutdownHookThread
    
    object Start {
	    def main(args:Array[String]):Unit = {
		    val green = new GreenSystem
		    green.start(new ExampleApp)
    
    		ShutdownHookThread {
    			green.stop()
    		}
    	}
    }
 
## wtf is Workers?

As I mentioned, workes tells the green-api how to connect your actors to the compute module (akka cluster).

Atm we offer a couple of flavours of these workers. All worker types are traits you implement.

### [Worker](https://github.com/Meduzz/green-api/blob/master/src/main/scala/se/chimps/green/api/workers/Worker.scala)
This actor will NOT be connected to the cluster, instead it will be created in the local actor system. Which might be usefull for simpler tasks, like listening in to cluster state or DistributedPubSub stuff.

### [PooledWorker](https://github.com/Meduzz/green-api/blob/master/src/main/scala/se/chimps/green/api/workers/PooledWorker.scala)
This worker will create a RoundRobinPool router in the cluster. Being a pool, means we at least in theory can scale it (up and down) simply by sending the appropriate message to the router.

### [ShardedWorker](https://github.com/Meduzz/green-api/blob/master/src/main/scala/se/chimps/green/api/workers/ShardedWorker.scala)
This worker will boot up cluster sharding with your actor as the target actor. This lets us split work over X shards in the akka cluster. It also requires you to provide a way to dig out the entity and a way to locate the correct shard.

#### Wait what?

By "dig out the entity", consider this:

You have an entity called Customer. The Customer entity has a property called Id which is unique for each Customer. In the sharded workload, each actor should only handle workloads for their Customer. So your "dig out entity function" would simply need to dig out that Customer.Id from each message going into the sharded cluster.

The same principle goes to locate the correct shard. First you need to descide how many shards you need. It could be number of compute modules * 2. Then each compute module would own 2 shards each. Your job is to make sure that traffic for the same Customer always goes to the same shard (because that's where the entity actor lives, ie your actor).

Still confused? Scroll through the akka [documentation](https://doc.akka.io/docs/akka/current/cluster-sharding.html).

### [ShardProxyWorker](https://github.com/Meduzz/green-api/blob/master/src/main/scala/se/chimps/green/api/workers/ShardProxyWorker.scala)
This worker lets you connect to an already existing sharded worker. Could be a handy way to connect 2 different apps.