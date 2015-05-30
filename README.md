#nfn-scala

nfn-scala is a NFN service layer implementation written in Scala, interfacing and depending on CCN-Lite. 
This project provides an asynchronous compute environment, an CCN and NFN client library, a DSL to write lambda calculus programs. 
It can also be used to run CCN-Lite directly from Scala and to create a network topology.

##Installation

Installation is fairly straight forward, and boils down to an available Java JDK 7, sbt 0.13.5 and CCN-Lite.
If you want to install the JVM or sbt you could also test it out within a Docker container.
For a tutorial on that, refer to the docker tutorial in [CCN-lite](https://github.com/cn-uofbasel/ccn-lite/docs).


### JDK/sbt

#### Ubuntu

1. `sudo apt-get install openjdk-7-jdk`
2. Follow [instructions](http://www.scala-sbt.org/0.13.5/docs/Getting-Started/Setup.html) to install sbt

#### OSX
JDK 7 should be available, otherwise it can be downloaded directly from [oracle](http://www.oracle.com/technetwork/java/javase/downloads/jdk7-downloads-1880260.html). 
To install sbt you can use homebrew (`brew install sbt`).

### nfn-scala
You can either use sbt to compile the project (e.g. `sbt compile`) or you can use `sbt assembly`.
This command will build the jarfile `./target/scala-2.10/nfn-assembly-0.1-SNAPSHOT.jar` with all dependency (even Scala itself!)
which makes it easier to deploy it because you do not have to install sbt or scala (only JVM).

### ccn-lite

To install ccn-lite and its dependencies follow the information [here](https://github.com/cn-uofbasel/ccn-lite).
nfn-scala uses the commandline utilities of ccn-lite, therefore it needs to have the compiled utilities available.
There are two possibilities, either you have CCNL_HOME set to your custom installation or you clone the repository with `--recursive` and use the submodule in `ccn-lite-nfn`.
In general the latter is the easier option, because the version of ccn-lite is always compatible with your nfn-scala.
To compile ccn-lite (either in the submodule or, if not checked out, `$CCNL_HOME`), you can type `sbt compileCCNLite`.
Alternatively you can also build it manually, make sure that `USE_NFN=1` is set.

#### IDE
If you want to use IntelliJ or eclipse you can use the sbt tasks `gen-idea` or `eclipse`.

#### Uninstalling:
* Uninstall sbt (and remove `~/.sbt` if it still exists)
* Delete `~/.ivy2` (this will of course also delete all your cached Java jars if you are using ivy)

##Running nfn-scala

For a detailed tutorial on both running CCN-Lite as well as nfn-scala go to the [tutorial](https://github.com/cn-uofbasel/ccn-lite/blob/dev-master/doc/tutorial/tutorial.md).
In the following two very basic ways to use nfn-scala.

### Running a test project
There are some runnables classes in the project. To see them all you can simply use `sbt run` and you will presented a list with everything available.
Choose for example . `runnables.evaluation.PandocApp` which starts a nfn environment, sends a predefined request and prints the result.

### Starting a standalone compute server
There is a small startscript/program to start and configure a single nfn-node.
This time we are going to build a jar-file containing everything (even scala) with `sbt assembly`. This jar can be deployed on any JVCM.
To run it you can either use `scala ./target/scala-2.10/nfn-assembly-0.1-SNAPSHOT.jar -h` (make sure you have the correct Scala version) or `java -jar /target/scala-2.10/nfn-assembly-0.1-SNAPSHOT.jar -h`.
Running the above will print a help message. As you can see there are several options. The most important setting is if you want to start ccn-lite yourself or if you want nfn-scala to start it internally.
You do not have to worry about faces, they are setup automatically.
The prefix is the name under which all content and services are published.
If you want to use the default values (which starts CCN-Lite internally), you can run it with `sbt run /nfn/testnode`.
To test if it everything works, use send the following two interests to CCN-Lite:
```bash
ccn-lite-peek -u 127.0.0.1/9000 "/nfn/testnode/docs/tiny_md" | ccn-lite-pktdump
ccn-lite-peek -u 127.0.0.1/9000 "" "add 1 2" | ccn-lite-pktdump
ccn-lite-peek -u 127.0.0.1/9000 "" "/nfn/testnode/nfn_service_WordCount 'this string contains 5 words'" | ccn-lite-pktdump
```
<!---
## Visualization
To replay and visualize the most recently run NFN program, change to the directory `./omnetreplay`. 
An installation of [OMNeT++](http://www.omnetpp.org) is required (we used Version 4.4.1, but other versions should work as well). 
Now you should be able to run the `make.sh` script which compiles and runs everything. 
From then on the simulation can be directly started with `./omentreplay`.
-->

## Issues
- CCN-Lite command line interface is slow, but it is convenient and currently helps to test CCN-Lite itself. For performance, the JNI interface (or JNA) should be reintroduced.
- Compute Server currently only does exact match on names and not longest prefix match. 
This can result in issues, for example when computing on data from the testbed, which always adds a version number. 
Since there is no implementaiton for a version number in CCN-Lite or nfn-scala, exact matches in nfn-scala will fail.
- Timeouts are not fully implemented (especially when using NACKS).
