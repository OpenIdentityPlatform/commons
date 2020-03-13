# Open Identity Platform Commons

[![Build Status](https://travis-ci.org/OpenIdentityPlatform/commons.svg)](https://travis-ci.org/OpenIdentityPlatform/commons)

## How-to build

- git clone --recursive <https://github.com/OpenIdentityPlatform/commons.git>
- mvn clean install -f commons

On macOS use `JAVA_HOME="/Library/Java/JavaVirtualMachines/jdk1.8.0_241.jdk/Contents/Home/" mvn clean install`. Otherwise it will not built under default Java 12 environment.
