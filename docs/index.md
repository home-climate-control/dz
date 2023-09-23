Home Climate Control: Docs
==

# FAQ

**Q:** Nothing works!
: A: [Check the configuration](./configuration/index.md), and check the logs. HCC is permissive (will completely stop operating only on unrecoverable errors), and verbose. Logs often contain links to documentation on how to correct a particular problem.

# State of Affairs
* Rock stable, but ages old, [imperative branch](https://github.com/home-climate-control/dz/tree/last-imperative-maintenance).
    * Benefits: it works.
    * Disadvantages:
        * The configuration is **very** complex
        * It's getting a bit long in the tooth
        * There will be no new features in this branch, only critical fixes
    * When to choose: when you want something very reliable and don't mind time learning the ropes.
* Bleeding edge, a bit less stable, but with more features, [reactive overhaul](https://github.com/home-climate-control/dz/tree/reactive).
    * Benefits:
        * Drastically simplified configuration (YAML instead of XML)
        * Can now be run in a [Docker container](./build/index.md#docker)
        * Better device support
        * Special note: economizer support, $$$ saved
        * The configuration is [documented](./configuration/index.md) and code assist is supported by modern IDEs (thanks to [Spring Configuration Metadata](https://docs.spring.io/spring-boot/docs/current/reference/html/configuration-metadata.html))
        * Drastically improved startup time
        * More ways to run (standalone, SpringBoot, Quarkus, Docker, k8s)
        * Improved platform instrumentation (Micrometer to InfluxDb)
        * Improved system instrumentation ("instrument cluster", documentation coming)
    * Disadvantages:
        * Some subsystems (1-Wire, XBee, shell) have not yet been ported over from `reactive`
        * Needs Java 17
        * Has not yet been verified to run on Raspberry Pi 3B
        * Bleeding edge
    * When to choose: if you are just approaching this project. There is a bit of rocket science involved, by the time you get all the dependencies lined up and operational, the project will reach the stable state and `imperative` will be retired for good.

# Getting Deeper
* [Build](./build/index.md)
* [Configuration Reference](./configuration/index.md)
* [Instrument Cluster](./instrument-cluster/index.md)
