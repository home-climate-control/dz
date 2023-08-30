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

When completed successfully, it will create an executable script in `${project_root}./dz3-spring/build/install/dz/bin/dz`. The next step would be to [create the configuration](../configuration/index.md) and run the script with its path as an argument.
