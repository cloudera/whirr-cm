# Launching Cloudera Manager with Whirr

Follow these instructions to start a cluster on Amazon EC2 running Cloudera Manager (CM),
allowing you to install, run and manage a CDH cluster.

## Install Whirr

Run the following commands from you local machine.

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

## Install the Whirr Cloudera Manager Plugin

Download the Whirr CM plugin source, build and install into the lib directory of your Whirr installation.

```bash
git clone https://github.com/cloudera/whirr-cm.git
cd whirr-cm
mvn clean install -Dmaven.test.skip=true
cp -rvf target/whirr-cm-*.jar $WHIRR_HOME/lib
```

## Launch a Cloudera Manager Cluster

A sample Whirr CM CDH EC2 config is available here: 

```bash
curl -O https://raw.github.com/cloudera/whirr-cm/master/cm-ec2.properties
```

If you would like to upload a CM License as part of the installation (Cloudera can provide this if you do not
have one), place the license in a file "cm-license.txt" on the whirr classpath (eg in $WHIRR_HOME/conf), eg

```bash
mv -v eval_acme_20120925_cloudera_enterprise_license.txt $WHIRR_HOME/conf/cm-license.txt
```

The following command will start a cluster with 5 nodes, 1 CM server, 1 master and 3 slave nodes. To change the
cluster topology, edit the [cm-ec2.properties](https://raw.github.com/cloudera/whirr-cm/master/cm-ec2.properties) file.

```bash
whirr launch-cluster --config cm-ec2.properties
```

Whirr will report progress to the console as it runs and will exit once complete.

During the various phases of execution, the Whirr CM plugin will report the CM Web Console URL, eg pre-provision

```bash
Whirr Handler -----------------------------------------------------------------
Whirr Handler [CMClusterProvision] 
Whirr Handler -----------------------------------------------------------------
Whirr Handler 
Whirr Handler [CMClusterProvision] follow live at http://37.188.114.234:7180
```

and post-provision:

```bash
Whirr Handler [CMClusterProvision] CM AGENTS
Whirr Handler [CMClusterProvision]   ssh -o StrictHostKeyChecking=no -i /root/.ssh/whirr whirr@37.188.114.209
Whirr Handler [CMClusterProvision]   ssh -o StrictHostKeyChecking=no -i /root/.ssh/whirr whirr@37.188.114.210
Whirr Handler [CMClusterProvision]   ssh -o StrictHostKeyChecking=no -i /root/.ssh/whirr whirr@37.188.114.225
Whirr Handler [CMClusterProvision]   ssh -o StrictHostKeyChecking=no -i /root/.ssh/whirr whirr@37.188.114.234
Whirr Handler [CMClusterProvision] CM SERVER
Whirr Handler [CMClusterProvision]   http://37.188.114.234:7180
Whirr Handler [CMClusterProvision]   ssh -o StrictHostKeyChecking=no -i /root/.ssh/whirr whirr@37.188.114.234
```

You are able to log into the CM Web Console (or hosts) at any stage and observe proceedings, via the async, real time UI.

The default admin user credentials are: 

```bash
Username: admin 
Password: admin 
```

## Manage the CDH cluster with CM

The Whirr property 'whirr.cm.auto', as set in [cm-ec2.properties](https://raw.github.com/cloudera/whirr-cm/master/cm-ec2.properties):

```bash
# Switch to enable/disable the automatic setup of a cluster within Cloudera Manager,
# its initialization and launch, defaults to true
whirr.cm.auto=true
```

determines whether the Whirr CM plugin provisions, initialises and starts a new CDH cluster (true)
or merely provisions the CM Server and Agents to allow manual CDH cluster management through
the CM Web Console (false).

In either case, once Whirr has completed, the CM infrastructure will be deployed and ready to
use, fully documented here:

http://www.cloudera.com/content/support/en/documentation/manager-enterprise/cloudera-manager-enterprise-v4-latest.html

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

As well as listing services via the 'list-services' command, the Whirr CM plugin supports the
following commands:

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
providers security controls and host firewalls.

```bash
# List of client IP ranges that can access the CM and CDH web console and client servers
whirr.client-cidrs=0.0.0.0/0
```

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
