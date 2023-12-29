Home Climate Control: Build
==
## Quick Start
This should get you going:

```
git clone https://github.com/home-climate-control/dz.git && \
cd dz && \
git submodule init && \
git submodule update && \
./gradlew build installDist
```

When completed successfully, it will create a set of executable scripts and execution targets. To summarize,
### Standalone, or "minimal"
* To execute this variant: run the `${project_root}/xterdz3r-app-minimal/build/install/hcc/bin/hcc` script, having provided it with the literal configuration location to as an argument.
* This version will have stricter constraints on YAML configuration, in particular,
  * It doesn't support [YAML anchors]() (yet?)
  * It requires more stringent [Duration](https://docs.oracle.com/javase/8/docs/api/java/time/Duration.html#parse-java.lang.CharSequence-) syntax than other versions
  * It likely doesn't and will not support breaking the configuration into several _profiles_ (more about that in [configuration](./configuration/index.md) section)
  * It doesn't support automatic Micrometer instrumentation.
* When to use: if your configuration is stable, and you want to run the system on the smallest box possible.

### SpringBoot
* To execute this variant, you need to run `./gradlew bootRun --args='--spring.profiles.active=<list of active profiles>'` from the project root.
* The usual Spring YAML syntax conventions apply.
* When to use: if you are comfortable writing and working with Spring configurations, and wouldn't mind having the system instrumented.


### Quarkus
* To execute this variant, you need to run `QUARKUS_PROFILE=<list of active profiles> ./gradlew quarkusDev` from the project root.
* Configuration can probably be 100% interchangeable with the SpringBoot version.
* When to use: if you are comfortable with Quarkus more than you are with Spring.

### Docker
#### Local Image
* To execute this variant, you need to first build it with `./gradlew jibDockerBuild` (assuming you granted your build user permissions to run Docker tools) and then run the image with configuration, logs, and connector directories exposed as volumes. Details are coming soon.
* When to use: if you want to tinker with the code and still run it from Docker.

#### Public Image
Coming soon, keep checking [climategadgets/home-climate-control](https://hub.docker.com/repository/docker/climategadgets/home-climate-control/) collection.

### k8s
Coming soon.

## Further Steps
The next step would be to [create the configuration](../configuration/index.md).
