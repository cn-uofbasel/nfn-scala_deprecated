
nfn-scala
=========

nfn-scala is a NFN service layer implementation in Scala, interfacing and depending on CCN-lite. 
This project provides a asynchronous compute environment, an API to interface with NFN, a DSL to program the lambda calculus and other things. 

##Installation

The main dependencies are Java JDK 7 and sbt 0.13. 
Certain ccn-lite functionality is directly used by nfn-scala. It is either executed with the command line tools (cli) or natively (jni).
Currently only CCNB and NDNTLV formats are supported with cli and CCNB with jni. 
We suggest using cli as long as performance is not important because it is less fragile (but slower).

### ccn-lite

Refer to the installation information in [CCN-lite](https://github.com/cn-uofbasel/ccn-lite), but in short the following should work.

1. Set the ccn-lite env: `export CCNL_PATH="<path/to/ccnlite"` (don't forget to add it to your  bash profile if you want it to persist)
2. Ubuntu: `sudo apt-get install libssl-dev`

   OSX: `brew install openssl` (assuming the [homebrew](http://brew.sh) packet manager is installed)

3. Ubuntu: `make clean && make all` in the `$CCNL_PATH` root directory

   OSX: `make clean && make -f BSDmakefile` in the `$CCNL_PATH` root directory

### nfn-scala

Ubuntu:

1. `sudo apt-get install openjdk-7-jdk`
2. Set the java env: `export JAVA_HOME=/usr/lib/jvm/java-7-openjdk-amd64` (don't forget to add it to your bash profile if you want it to persist)
3. Follow [instructions](http://www.scala-sbt.org/0.13.2/docs/Getting-Started/Setup.html) to install sbt

OSX:
JDK 7 should be available, otherwise it can be downloaded directly from [oracle](http://www.oracle.com/technetwork/java/javase/downloads/jdk7-downloads-1880260.html). 

1. `brew install openssl`
2. `brew install sbt`
3. `export JAVA_HOME="/Library/Java/JavaVirtualMachines/jdk1.7.0_07.jdk/Contents/Home/"`
4. `make clean && make -f BSDmakefile` in the `ccn-lite` root directory

##Running NFN
### Starting sbt
Set the `NFN_SCALA` variable with `export NFN_SCALA="<path/to/nfn-scala/"` and change to this directory.
If you only want to use nfn-scala in the cli mode, you can just type `sbt`.
For the jni mode run the `start_sbt.sh` script, this will launch sbt with the linked dynamic library (not yet compiled) in the classpath. 
To compile this library, run the sbt command `compileJniNativelib`.

### Running the project
There are some runnables in the nfn-scala-experiments project. Type `project nfn-scala-experiments` and then `run` in the sbt console. 
Choose from a list of runnable applications, e.g. `evaluation.PaperExperiment` which runs the currently selected expriment (change in source code).

## Visualization
To replay and visualize the most recently run NFN program, change to the directory `./omnetreplay`. 
An installation of [OMNeT++](http://www.omnetpp.org) is required (we used Version 4.4.1, but other versions should work as well). 
Now you should be able to run the `make.sh` script which compiles and runs everything. 
From then on the simulation can be directly started with `./omentreplay`.

## Issues
- The project is not yet setup to be easily used in an actual network (there is only the `LocalNode` wrapper which executes everything on localhost). However, all the functionality (except the hack for large data files) is prepared because communication happens over sockets.
- Timeouts are not fully implemented when using NACKS.
