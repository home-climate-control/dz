Home Climate Control: Docs
==

# FAQ

**Q:** Nothing works!  
**A:** In approximately this order:

* [Check the configuration](./configuration/index.md).
  * Use YAML anchors, they help to keep it consistent.
  * Check if the order of configuration entries is the same as in the documentation.
  * Make sure you examine the configuration dump (logged at `debug` level) to see if the configuration you think you provided is the configuration that's actually been read.
* Check the logs. HCC is permissive (will completely stop operating only on unrecoverable errors), and verbose. Logs often contain links to documentation on how to correct a particular problem.
* Enable [InfluxDB connector](./configuration/influx.md). HCC emits a lot data, and one picture is better than a thousand words.
* Enable [Console](./configuration/console.md) (incompatible with [running in Docker](./build/index.md#docker), but well worth it for troubleshooting). Its [instrument cluster](./instrument-cluster/index.md) might pinpoint problems immediately.

# State of Affairs
* Bleeding edge, a bit less stable, but with more features, [reactive overhaul](https://github.com/home-climate-control/dz/milestone/12) is now the development mainline. Read everything about it [at the root](../README.md). Some facts not mentioned there for brevity:
    * Benefits:
        * Drastically improved startup time
        * More ways to run (standalone, SpringBoot, Quarkus, Docker, k8s)
        * Improved platform instrumentation (Micrometer to InfluxDb)
  * Disadvantages:
      * Some subsystems (1-Wire, XBee, shell) have not yet been ported over from `reactive`
      * Needs Java 17
      * Has not yet been verified to run on Raspberry Pi 3B
  * When to choose: if you are just approaching this project.
* Rock stable, but ages old, [imperative branch](https://github.com/home-climate-control/dz/tree/last-imperative-maintenance).
    * Benefits: it works.
    * Disadvantages:
        * The configuration is **very** complex
        * It's getting a bit long in the tooth
        * There will be no new features in this branch, only critical fixes
    * When to choose: when you want something very reliable and don't mind time learning the ropes. The 'reliable' advantage is quickly waning, though, so the only real reason to choose this would be if you want 1-Wire or XBee device support **right now**.

# Next Steps
* [Platform Support](./platform.md)
* [Hardware Support](hardware/index.md)
* [Build](./build/index.md)
* [Configuration Reference](./configuration/index.md)
* [Instrument Cluster](./instrument-cluster/index.md)
