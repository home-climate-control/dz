Home Climate Control: Docs
==

# FAQ

**Q:** What are my options here?  
**A:** As usual, the tradeoff is between functionality and stability:
* [Named releases](https://github.com/home-climate-control/dz/releases) - known stable checkpoints.
* [master branch](https://github.com/home-climate-control/dz/tree/master) - known stable, but a bit obsolete.
* [dev branch](https://github.com/home-climate-control/dz/tree/dev) - current development. Stable enough, with occasional flukes.
* [imperative branch](https://github.com/home-climate-control/dz/tree/last-imperative-maintenance) - rock stable, but ages old. Will not get new updates.

**Q:** How do I choose a stable revision?  
**A:** Look at [tags list](https://github.com/home-climate-control/dz/tags), any tag containing the word "stable" in it is what you're looking for.
Additionally, if you take a look at the output of `git tag -n`, it will usually mention the uptime of that particular revision before it was tagged.

**Q:** Nothing works!  
**A:** In approximately this order:

* [Check the configuration](./configuration/index.md).
  * Use YAML anchors, they help to keep it consistent.
  * Check if the order of configuration entries is the same as in the documentation.
  * Make sure you examine the configuration dump (logged at `debug` level) to see if the configuration you think you provided is the configuration that's actually been read.
* Check the logs. HCC is permissive (will completely stop operating only on unrecoverable errors), and verbose. Logs often contain links to documentation on how to correct a particular problem.
* Enable [InfluxDB connector](./configuration/influx.md). HCC emits a lot of data, and one picture is better than a thousand words.
* Enable [Console](./configuration/console.md) (incompatible with [running in Docker](./build/index.md#docker), but well worth it for troubleshooting). Its [instrument cluster](./instrument-cluster/index.md) might pinpoint problems immediately.

**Q:** HCC advertises itself on `127.0.1.1`, clients can't connect  
**A:** You're likely running Debian or derivative, and [this is what is going on](https://serverfault.com/questions/363095/why-does-my-hostname-appear-with-the-address-127-0-1-1-rather-than-127-0-0-1-in).
Remove `127.0.1.1` from `/etc/hosts`.

# Next Steps
* [Platform Support](./platform.md)
* [Hardware Support](hardware/index.md)
* [Build](./build/index.md)
* [Configuration Reference](./configuration/index.md)
* [Instrument Cluster](./instrument-cluster/index.md)
