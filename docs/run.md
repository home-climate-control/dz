Home Climate Control Legacy: Run
==
Psst! Did you read the [BIG FAT WARNING](index.md#big-fat-warning)?

---
## Prerequisites
* [Build](./build.md)

## Install required tools

* `sudo apt-get install rrdtool`
* `sudo apt-get install librxtx-java`

## Install optional tools

* `sudo apt-get install munin-node` (you'll have to configure it with your other hosts)
* `sudo apt-get install tightvncserver` (faster to run the Swing console than X Server forwarding)

## Create the directory tree to run DZ in

* `mkdir $HOME/dz` #or whatever else you want to call it
* `export DZ=$HOME/dz`
* `mkdir $DZ/conf`# All configuration will go here
* `mkdir $DZ/rrd`# RRDTool created databases will go here

## Copy scripts

Now, copy the scripts from [dz3-shell](https://github.com/home-climate-control/dz/tree/last-imperative-maintenance/dz3-shell) tree here (to `$DZ`):

* `classpath-dev`
* `dz-jconsole`
* `dz-runner`
* `dz_graph_temperature.v2`
* `pack-and-clear-logs`
* `raise_priority`
* `take-memory-dump`
* `take-thread-stack-dump`

Now, apply the following diff to `dz-runner` (this will use the development classes compiled in the [previous step](./build.md)):

    diff --git a/dz3-shell/dz-runner b/dz3-shell/dz-runner
    index b4cd52b..11402a0 100755
    --- a/dz3-shell/dz-runner
    +++ b/dz3-shell/dz-runner
    @@ -4,12 +4,12 @@ export BASENAME=`dirname $0`
 
     # Following line should be uncommented if you want to run the distribution code
 
    -. ${BASENAME}/classpath
    +#. ${BASENAME}/classpath
 
     # Following line should be uncommented if you want to run the code produced locally on your box
     # by Maven build
 
    -#. ${BASENAME}/classpath-dev
    +. ${BASENAME}/classpath-dev
 
     # Comment out the next line if DZ blows out of memory for you - and don't forget to file a bug report.
     # See http://diy-zoning.blogspot.com/2010/01/memory-leak-hunt-aftermath.html for details

You can do it either by hand (correct three lines), or by saving the above clip to a file and running a [patch(1)](https://www.unix.com/man-page/all/1/patch/) command.

## Copy the sample configuration

From [dz3shell/conf](https://github.com/home-climate-control/dz/tree/last-imperative-maintenance/dz3-shell/conf) to `$DZ/conf`:

* `conf/log4j2.xml`
* `conf/raspberry-pi.xml`

## Configure

At this point, DZ should run (`cd $DZ && ./dz-runner raspberry-pi.xml` - note that the '`conf`' part is missing from the command line). This will start the DZ Swing Console with one thermostat reporting Raspberry Pi temperature (as reported by [vcgencmd](https://www.raspberrypi.org/documentation/raspbian/applications/vcgencmd.md)).
Next step, you'd want to provide your own configuration. Please proceed to [DZ Configuration Guide](./configuration/index.md).

## Take care of logs

Logs can take a lot of space, especially with debug enabled. Add the [pack-and-clear-logs](https://github.com/home-climate-control/dz/tree/last-imperative-maintenance/dz3-shell/pack-and-clear-logs) script to your [crontab](http://www.unix.com/man-page/all/5/crontab/). Don't forget to set `DZ_HOME` to your location either directly in the script, or in your environment.

## Need help?

There's a lot of people that are already using DZ and will be willing to help at [Home Climate Control forum](https://groups.google.com/forum/#!forum/home-climate-control).
