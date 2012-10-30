# Launching Cloudera Manager with Whirr

Follow these instructions to start a cluster on EC2 running Cloudera Manager.
Cloudera Manager allows you to install, run, and manage a Hadoop cluster.

This method uses Whirr to start a cluster with
 * one node running the Cloudera Manager Admin Console, and
 * a user-selectable number of nodes for the Hadoop cluster itself

Once Whirr has started the cluster, you use Cloudera Manager in the usual way.

Note that you can omit the CDH client node if you want to run programs entirely
from Hue.

It is not currently possible to launch Hadoop programs from your local machine
that use the cluster, due to the way Cloudera Manager manages host addresses.
To work around this limitation you can add a Gateway role which installs CDH
client components on a node in the cloud to run programs from.

## Install Whirr

Run the following commands from you local machine.

### Set your AWS credentials as environment variables:
```bash
export AWS_ACCESS_KEY_ID=...
export AWS_SECRET_ACCESS_KEY=...
```

### Install Whirr:

Install CDH repositotries, eg for CDH4:

https://ccp.cloudera.com/display/CDH4DOC/CDH4+Installation#CDH4Installation-InstallingCDH4

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

## Install the Whirr Cloudera Manager Service Plugin

Download the Whirr CM plugun source, build and install into the lib directory of your Whirr installation.

```bash
git clone https://github.com/cloudera/whirr-cm.git
cd whirr-cm
mvn clean install -Dmaven.test.skip=true
cp -rvf target/whirr-cm-*.jar $WHIRR_HOME/lib
```

## Get your whirr-cm configuration

A sample Whirr EC2 CM config is available here: 

```bash
curl -O https://raw.github.com/cloudera/whirr-cm/master/cm-ec2.properties
```

If you would like to upload a CM License as part of the instalation (Cloudera can provide this if you do not
have one), place the license in a file "cm-license.txt" on the whirr classpath (eg in $WHIRR_HOME/conf), eg

```bash
mv -v eval_acme_20120925_cloudera_enterprise_license.txt $WHIRR_HOME/conf/cm-license.txt
```

If you would like to upload a CM Configuration as part of the instalation, place the config in a file called
"cm-config.json" on the whirr classpath (eg in $WHIRR_HOME/conf). The format of this file should match the JSON
as downloaded from the CM UI, eg

```bash
curl -O https://raw.github.com/cloudera/whirr-cm/master/cm-config.json
mv -v cm-config.json $WHIRR_HOME/conf/cm-config.json
```

## Launch a Cloudera Manager Cluster

The following command will start a cluster with 5 Hadoop nodes. To change this
number edit the cm-ec2.properties file. Edit the same file if you don't want to
launch a CDH client node.

```bash
whirr launch-cluster --config cm-ec2.properties
```

Whirr will report progress to the console as it runs. The command will exit when
the cluster is ready to be used.

## Configure the Hadoop cluster

The next step is to run the Cloudera Manager Admin Console -- at the URL printed
by the Whirr command -- to install and configure Hadoop, using the instructions
at

https://ccp.cloudera.com/display/FREE4DOC/Automated+Installation+of+Cloudera+Manager+and+CDH

The output of the Whirr command includes settings for the cluster hosts
and the authentication method to be used while running the Cloudera Manager
Admin Console.

## Use the cluster

Once the Hadoop cluster is up and running you can use it via Hue (the URL
is printed by the launch cluster command), or from a CDH gateway machine. In
the latter case, follow these instructions to add a gateway role

https://ccp.cloudera.com/display/FREE4DOC/Adding+Role+Instances

Then SSH to the gateway machine. Now you can interact with the cluster,
e.g. to list files in HDFS:

```bash
hadoop fs -ls /tmp
```

## Shutdown the cluster

Finally, when you want to shutdown the cluster, run the following command. Note
that all data and state stored on the cluster will be lost.

```bash
whirr destroy-cluster --config cm-ec2.properties
```
