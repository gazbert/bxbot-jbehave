# BX-bot JBehave

## What is BX-bot JBehave?
BX-bot [JBehave](https://jbehave.org/) is a suite of BDD acceptance tests for [BX-bot's REST API](https://github.com/gazbert/bxbot).

## Usage
You'll need [openjdk-11-jdk](http://openjdk.java.net/projects/jdk/11/) or 
[Oracle JDK 11](https://www.oracle.com/technetwork/java/javase/downloads/jdk11-downloads-5066655.html) installed on your machine.
 
1. Start an instance of [BX-bot](https://github.com/gazbert/bxbot) and note the REST API base URI and user credentials.
1. Update the [./src/test/resources/restapi-config.properties](./src/test/resources/restapi-config.properties) if you need to.
From the project root, run `./mvnw clean install`
1. Take a look at the JBehave test reports in the `./target/jbehave/view/report.html` file after the build completes.

## Issue & Change Management
Issues and new features are managed using the project [Issue Tracker](https://github.com/gazbert/bxbot-jbehave/issues) -
submit bugs here.

You are welcome to take on new features or fix bugs! See [here](CONTRIBUTING.md) for how to get involved. 

For help and general questions about BX-bot, check out the [Gitter](https://gitter.im/BX-bot/Lobby) channel.

