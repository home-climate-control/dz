Home Climate Control: Configuration Reference
==

## Variants

There are currently four ways to run the application (see [build](../build/index.md) for details):
* Standalone ("minimal")
* SpringBoot
* Quarkus
* Docker

The "minimal" variant is the only one that requires things to be spelled out exactly, the rest follow the usual Spring and Quarkus conventions (splitting configuration into many files, specifying active profiles, using YAML anchors, etc.).

> **NOTE:** YAML merging is not so smart. Verify the effective configuration, it is logged at debug level at the start of the application.

### YAML Anchors

Configuration file for HCC can be quite big (400+ lines is not unheard of), do take advantage of [YAML Anchors](https://yaml.org/spec/1.2.2/#3222-anchors-and-aliases) to make it stable.

## Infrastructure Specific
This part contains what you would usually provide for your standard Spring or Quarkus applications. For your convenience, basic
[localhost](../../app/hcc-springboot/src/main/resources/application-localhost.yaml) and
[docker](../../app/hcc-springboot/src/main/resources/application-docker.yaml) profiles are included into default configuration - feel free to override them as you see fit.
## Home Climate Control Specific

> **NOTE:** There are two source trees, [Spring specific](../../app/hcc-springboot/src/main/java/net/sf/dz3/runtime/config/HccRawRecordConfig.java) and
> [Quarkus specific](../../app/hcc-quarkus/src/main/java/net/sf/dz3/runtime/config/quarkus/HccRawInterfaceConfig.java) that directly map to configuration entries.
> They are expected to behave the same way (the reason two of them exist is very different conventions followed by Spring and Quarkus), if there's ever any discrepancy (the application works invoked with Spring and doesn't work invoked with Quarkus, or the other way around), please [file an issue](https://github.com/home-climate-control/dz/issues).

> **NOTE:** You can use your IDE to edit configuration files, this will give you code completion and extended documentation, thanks to [Spring Configuration Metadata](https://docs.spring.io/spring-boot/docs/current/reference/html/configuration-metadata.html), and "code walking" if you are using YAML anchors.

---
[^^^ Index](./index.md)
