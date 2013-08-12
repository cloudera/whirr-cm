# Whirr Cloudera Manager Plugin

Follow these instructions to start a cluster on Amazon EC2 running Cloudera Manager (CM),
allowing you to install, run and manage a CDH cluster.

## Requirements

This plugin has dependencies on whirr and CM as per the pom, but remains backwards compatible with:

* whirr 0.8+
* CM 4.5+ (CM API 3+)

The plugin has been tested extensively on RHEL and Debian derivatives and ships with integrations tests targeting:

* CentOS 6.4
* Ubuntu LTS 12.04

## Install Whirr

Run the following commands from you local machine or edge node with access to your Cloud providers resources.
Note that the latter is often preferable, providing a cluster client edge node for using your cluster, catering for:

* network brownouts
* minimial latency
* private host/IP bindings


### Set your AWS credentials:
```bash
export WHIRR_CLOUD_PROVIDER_ID=...
export WHIRR_CLOUD_PROVIDER_KEY=...
```

### Install Whirr:

Install CDH repositories, eg for CDH4:

http://www.cloudera.com/content/support/en/documentation/cdh4-documentation/cdh4-documentation-v4-latest.html#CDH4Installation-InstallingCDH4

then install the whirr package and create some environment variables, eg for RHEL/CentOS:

```bash
yum install whirr
export WHIRR_HOME=/usr/lib/whirr
export PATH=$WHIRR_HOME/bin:$PATH
```

### Create a password-less SSH keypair for Whirr to use:

```bash
ssh-keygen -t rsa -P '' -f ~/.ssh/id_rsa_cm
```

## Install the Whirr Cloudera Manager Plugin (optional)

As of CDH 4.2, whirr ships with the whirr CM plugin, but in the event that you would like to replace this, you can follow
these instructions (take note of any files copied during the 'mvn dependency:copy-dependencies' command, this may indicate old and now stale versions remain and should be deleted from $WHIRR_HOME/lib)

```bash
git clone https://github.com/cloudera/whirr-cm.git
cd whirr-cm
mvn clean install -DskipTests
mvn dependency:copy-dependencies -DincludeScope=runtime -DoverWriteIfNewer=false -DoutputDirectory=$WHIRR_HOME/lib
rm -rf $WHIRR_HOME/lib/whirr-cdh-* $WHIRR_HOME/lib/whirr-cm-*
cp -rvf target/whirr-cm-*.jar $WHIRR_HOME/lib
```

## Launch a Cloudera Manager Cluster

A sample Whirr CM CDH EC2 config is available ([cm-ec2.properties](cm-ec2.properties)) which should be locally copied to your whirr client host: 

```bash
curl -O https://raw.github.com/cloudera/whirr-cm/master/cm-ec2.properties
```

If you would like to upload a CM License as part of the installation (Cloudera can provide this if you do not
have one), place the license in a file "cm-license.txt" on the whirr classpath (eg in $WHIRR_HOME/conf), eg

```bash
mv -v eval_acme_20120925_cloudera_enterprise_license.txt $WHIRR_HOME/conf/cm-license.txt
```

The following command will start a cluster with 7 nodes, 1 CM server, 3 master and 3 slave nodes. To change the
cluster topology, edit the cm-ec2.properties file.

```bash
whirr launch-cluster --config cm-ec2.properties
```

Whirr will report progress to the console as it runs and will exit once complete.

During the various phases of execution, the Whirr CM plugin will report the CM Web Console URL, e.g. pre-provision

```bash
Whirr Handler -----------------------------------------------------------------
Whirr Handler [CMClusterProvision] 
Whirr Handler -----------------------------------------------------------------
Whirr Handler 
Whirr Handler [CMClusterProvision] follow live at http://ec2-50-112-89-152.us-west-2.compute.amazonaws.com:7180
```

and post-provision:

```bash
Whirr Handler [CMClusterProvision] CM AGENTS
Whirr Handler [CMClusterProvision]   ssh -o StrictHostKeyChecking=no -i /root/.ssh/whirr whirr@37.188.114.209
Whirr Handler [CMClusterProvision]   ssh -o StrictHostKeyChecking=no -i /root/.ssh/whirr whirr@37.188.114.210
Whirr Handler [CMClusterProvision]   ssh -o StrictHostKeyChecking=no -i /root/.ssh/whirr whirr@37.188.114.225
Whirr Handler [CMClusterProvision]   ssh -o StrictHostKeyChecking=no -i /root/.ssh/whirr whirr@37.188.114.234
Whirr Handler [CMClusterProvision] CM SERVER
Whirr Handler [CMClusterProvision]   http://ec2-50-112-89-152.us-west-2.compute.amazonaws.com:7180
Whirr Handler [CMClusterProvision]   ssh -o StrictHostKeyChecking=no -i /root/.ssh/whirr whirr@37.188.114.234
```

You are able to log into the CM Web Console (or hosts) at any stage and observe proceedings, via the async, real time UI.

The default admin user credentials are: 

```bash
Username: admin 
Password: admin 
```

## Manage the CDH cluster with CM

The Whirr property 'whirr.cm.auto', as set in [cm-ec2.properties](https://raw.github.com/cloudera/whirr-cm/master/cm-ec2.properties),
determines whether the Whirr CM plugin provisions, initialises and starts a new CDH cluster (true)
or merely provisions the CM Server and Agents to allow manual CDH cluster management through
the CM Web Console (false).

In either case, once Whirr has completed, the CM infrastructure will be deployed and ready to
use, as documented [here](http://www.cloudera.com/content/support/en/documentation/manager-enterprise/cloudera-manager-enterprise-v4-latest.html).

Other Whirr properties can be used to affect the cluster provision process, refer to the example 
[cm-ec2.properties](https://raw.github.com/cloudera/whirr-cm/master/cm-ec2.properties) for more details.

You can have Whirr report the currently running cluster nodes at any time:

```bash
whirr list-cluster --config cm-ec2.properties
```

or query the Whirr CM plugin services:

```bash
whirr list-services --config cm-ec2.properties
```

As well as supporting all the standard whirr commands, the Whirr CM plugin provides and or intergrates to the following additional commands:

* init-cluster
* download-config 
* create-services
* start-services
* restart-services
* stop-services
* destroy-services
* launch-cluster
* clean-cluster
* destroy-cluster

Where appropriate, these commands can be filtered by role via the '--roles' command line switch,
bearing in mind that 'cm-server' is a mandatory role (all operations require it) and all lifecycle
commands operate on the parent of the role to ensure consistency. For example, to issue
a HDFS service start:

```bash
whirr start-services --roles cm-server,cm-cdh-datanode --config cm-ec2.properties
```

A custom CM cluster name can be provided to most of the commands via the '--cm-cluster-name' switch.
For example, to clean the "My Cluster" from CM:

```bash
whirr clean-cluster --cm-cluster-name "My Cluster" --config cm-ec2.properties
```

Full command documentation is available as part of Whirr, for example:

```bash
whirr
whirr help clean-cluster
```

## Use the CDH cluster

The host you have run Whirr from can be used as a CDH client, as long as it can see the cluster 
nodes (host,forward/reverse DNS) and has the same version of CDH installed (although not necessarily
via the same means, eg parcels/RPMs/.debs etc).

The 'whirr.client-cidrs' can be used to ensure the clients IP ranges are accepted through the cloud
providers security controls and host firewalls, see [cm-ec2.properties](cm-ec2.properties).

To download the CDH cluster client config to the whirr cluster working directory ($HOME/.whirr/whirr)

```bash
whirr download-config --config cm-ec2.properties
```

This will then allow the CDH clients to be executed, for example to list files in HDFS root dir:

```bash
hadoop --config $HOME/.whirr/whirr fs -ls /
```

Alternatively, you can interact with the cluster via a CM gateway node from within your cluster.


## Shutdown the cluster

Finally, when you want to shutdown the cluster, run the following command. Note
that all data and state stored on the cluster will be lost.

```bash
whirr destroy-cluster --config cm-ec2.properties
```

## Unit and Integration Tests

This project includes a full suite of unit tests, launchable via:

```bash
mvn clean test
```

Integration tests that run against Amazon EC2 are also available, run as so:

```bash
mvn clean verify -DskipUTs -DskipITs=false -Dwhirr.test.identity=$AWS_ACCESS_KEY -Dwhirr.test.credential=$AWS_SECRET_KEY
```

Be wary though, these tests will take some time to execute, so it is advisable to use individual test cases for iterative testing, eg:

```bash
mvn clean test -Dtest=CmServerCommandIT#testCreateServices -Dwhirr.test.identity=$AWS_ACCESS_KEY -Dwhirr.test.credential=$AWS_SECRET_KEY
mvn clean test -Dtest=CmServerCommandIT#testStartServices -Dwhirr.test.identity=$AWS_ACCESS_KEY -Dwhirr.test.credential=$AWS_SECRET_KEY
mvn clean test -Dtest=CmServerCommandIT#testServiceLifecycle -Dwhirr.test.identity=$AWS_ACCESS_KEY -Dwhirr.test.credential=$AWS_SECRET_KEY
```

You can optionally specify a target platform, CM, CM API and CDH version via the following system properties (defaulting to the parameters defined in the example configuration [cm-ec2.properties](cm-ec2.properties) and or the latest versions available):

* whirr.test.platform (centos | ubuntu)
* whirr.test.cm.version (cm$MAJOR_VERSION.$MINOR_VERSION.$INCREMENTAL_VERSION)
* whirr.test.cm.api.version (v$MAJOR_VERSION)
* whirr.test.cm.cdh.version (cdh$MAJOR_VERSION)

For example:

```bash
mvn clean test -Dtest=CmServerCommandIT#testCreateServices -Dwhirr.test.identity=$AWS_ACCESS_KEY -Dwhirr.test.credential=$AWS_SECRET_KEY -Dwhirr.test.platform=centos
mvn clean test -Dtest=CmServerCommandIT#testCreateServices -Dwhirr.test.identity=$AWS_ACCESS_KEY -Dwhirr.test.credential=$AWS_SECRET_KEY -Dwhirr.test.cm.version=cm4.5.0
mvn clean test -Dtest=CmServerCommandIT#testCreateServices -Dwhirr.test.identity=$AWS_ACCESS_KEY -Dwhirr.test.credential=$AWS_SECRET_KEY -Dwhirr.test.platform=centos -Dwhirr.test.cm.version=cm4.6.2 -Dwhirr.test.cm.api.version=v4 -Dwhirr.test.cm.cdh.version=cdh4
```

Note that 'whirr.test.cm.cdh.version' specifies only the major CDH version, both the minor and incremental versions are defaulted to the latest available from the list of parcel repos defined by 'whirr.cm.config.cm.remote_parcel_repo_urls', which defaults to the latest available.

The [CmServerSmokeSuiteIT](src/test/java/com/cloudera/whirr/cm/integration/CmServerSmokeSuiteIT.java) leverages a matrix of platforms and component versions to test all supported permutations, it ignores the command line switches.

```bash
mvn clean test -Dtest=CmServerSmokeSuiteIT -Dwhirr.test.identity=$AWS_ACCESS_KEY -Dwhirr.test.credential=$AWS_SECRET_KEY
```

The integration test frameworks sets up and tears down a cluster automatically as necessary, if you would like to launch and persist a cluster between integration tests for iterative testing sans the cluster bootstrap costs, a cluster can be launched as so (above platform and version system properties are also supported):

```bash
mvn exec:java -Dexec.mainClass="com.cloudera.whirr.cm.integration.BaseITServer\$ClusterBoostrap" -Dexec.classpathScope="test" -Dwhirr.test.identity=$AWS_ACCESS_KEY -Dwhirr.test.credential=$AWS_SECRET_KEY -Dlog4j.configuration=file:./target/test-classes/log4j.properties -Dwhirr.test.platform=centos
```

and then destroyed via:

```bash
mvn exec:java -Dexec.mainClass="com.cloudera.whirr.cm.integration.BaseITServer\$ClusterDestroy" -Dexec.classpathScope="test" -Dwhirr.test.identity=$AWS_ACCESS_KEY -Dwhirr.test.credential=$AWS_SECRET_KEY -Dlog4j.configuration=file:./target/test-classes/log4j.properties
```
