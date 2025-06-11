Home Climate Control Legacy: FAQ
==
Psst! Did you read the [BIG FAT WARNING](./index.md#big-fat-warning)?

---
## FAQ

**Q:** What exactly does this FAQ cover?  
**A:** The legacy version of the code available at [last-imperative-maintenance branch](https://github.com/home-climate-control/dz/tree/last-imperative-maintenance?tab=readme-ov-file#home-climate-control-aka-dz).

**Q:** Will DZ run on Raspberry Pi Model A?  
**A:** Haven't tried, probably no.

**Q:** Will DZ run on Raspberry Pi Model B?  
**A:** Haven't tried, probably yes.

**Q:** Will DZ run on Raspberry Pi Model B+?  
**A:** Yes, it runs reliably. However, even though it runs just fine, it's painfully slow to tinker with remotely - forget running anything over X Server forwarding, it'll take minutes to just refresh the screen once. For this reason, JMX control is out of question if JConsole is started on Pi itself, and remote invocation still needs to be figured out.

**Q:** Will DZ run on Raspberry Pi 2 Model B?  
**A:** Absolutely. It runs comfortably with full configuration, including RRD databases and JMX. The best way to run the Swing Console is to use a VNC server, X Server forwarding is much slower.

**Q:** Will DZ run on Raspberry Pi 3 Model B?  
**A:** Yes. In fact, you can just take an SD card from Pi 2 Model B if you had one working already, plug it into Pi 3B, and it will work. However, keep in mind that Pi 3B will run about 10-12°C hotter than Pi 2, and it requires a 2.5A power supply vs. 1.8A for Pi 2, so if heat dissipation at your location is a problem, you may want to use Pi 2 and not Pi 3B. Nevertheless, even with ambient temperature reaching 31°C Pi 3B is unlikely to heat above 72°C which is below the temperature slowdown threshold.

**Q:** Will DZ run on Raspberry Pi 3 Model A+?  
**A:** Yes, but not as well as on Pi 3 Model B or even Pi 2 Model B. Model A+ has half the memory of models 2B and 3B, and that's the bottleneck for most things Java.

**Q:** Should I use Pi 2 or Pi 3B?  
**A:** If you have Pi 2 already, use it. If you have Pi 3B already, use it and see if it overheats (unlikely). If you need to buy one and know you will not use it for anything other than running DZ, consider buying Pi 2 (less heat dissipation, smaller power supply). If you know you'll be experimenting, get a Pi 3B with an adequate power supply.

**Q:** Should I use Pi 4B?  
**A:** Only if you plan to be actively working on DZ development, or plan to run other services on it as well. It is an overkill for a long term dedicated DZ installation, and will generate more heat than you'd like.

**Q:** How do I build DZ on Raspberry Pi?  
**A:** Here is [HOWTO: Build DZ on Raspberry Pi](./build.md).

**Q:** How do I run DZ on Raspberry Pi?  
**A:** Here is [HOWTO: Run DZ on Raspberry Pi](./run.md).

**Q:** Does it make sense to overclock Raspberry Pi to run DZ?  
**A:** No.

**Q:** What size SD card is enough?  
**A:** Fresh system and DZ build take [2.9GB](./build.md). 4GB would probably be too small, 8GB would be enough ([pack-and-clear-logs](https://github.com/home-climate-control/dz/tree/last-imperative-maintenance/dz3-shell/pack-and-clear-logs) script with default settings stabilizes disk consumption at about 5.4GB), and 16GB would be comfortable. However, any SD card size below 32GB is no longer practical - price per storage unit is too high.

**Q:** Any recommendations about SD card vendor selection?  
**A:** SanDisk cards keep dying. Samsung cards keep working. Current SD card of choice is [MB-ME32GA/AM](https://www.amazon.com/gp/product/B06XWN9Q99).

**Q:** What Java runtime needs to be used?  
**A:** Use Oracle Java 8 (~it will get pulled in as a side effect of [building DZ on Raspberry Pi](./build.md)~ not anymore (as of Raspbian Stretch). **FIXME:** update build script to pull Oracle JDK instead of OpenJDK). **Do NOT use OpenJDK**, it is known to segfault randomly, sometimes after many hours of uptime, and works about (I kid you not) ten times slower.

**Q:** How do I specify which Java runtime to use?  
**A**: One of ways that work is to add a `export JAVA_HOME=/usr/lib/jvm/jdk-8-oracle-arm-vfp-hflt/` line to `$HOME/.bashrc` (and then run `source $HOME/.bashrc` so it takes effect in the current session). Another way is to use [alternatives](http://www.unix.com/man-page/all/8/alternatives/) or [galternatives](http://www.unix.com/man-page/debian/1/galternatives/) - this is more permanent, but with system-wide effect.

**Q:** Which distribution is the best to use for DZ?  
**A:** It shouldn't matter - DZ will run as long as dependencies are satisfied. However, FAQs here will not cover distributions other than Raspbian.

**Q:** Which Raspbian versions DZ runs on?  
**A:** DZ is known to reliably run for years on Raspbian 7 (Wheezy) and 8 (Jessie), and is currently running on Raspbian 9 (Stretch) without any problems.

**Q:** Which Raspbian version should I use to run DZ?  
**A:** Run it on whatever you currently have. If you don't have anything or are creating a new image, Stretch Lite is the best choice if you have previous UNIX/Raspbian experience, and Stretch Desktop is the best choice if you don't.
