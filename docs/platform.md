Home Climate Control: Platform Support
==

## Current Development Tree

No clear answer much beyond "it runs on UNIX", and
* it takes great pains to build on Raspberry Pi 3 Model B+, you need to stop all non-essential services, [give it lots of swap space](https://pimylifeup.com/raspberry-pi-swap-file/), and build it without running test cases for good measure;
* it builds on Raspberry Pi 5 without any precautions, however, you will have to [make one extra step](https://github.com/home-climate-control/dz/issues/305) if you want to run it in a Docker container.
* It definitely works in a Docker container.

Stay tuned, more to come. Meanwhile, see [Build](./build/index.md).

## Legacy
The [legacy branch](https://github.com/home-climate-control/dz/tree/last-imperative-maintenance) effortlessly works on Raspberry Pi 3 Model B.

---
[^^^ Index](./index.md)
