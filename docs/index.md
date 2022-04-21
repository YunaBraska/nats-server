## Java Nats-Server

![logo](https://github.com/YunaBraska/nats-server/raw/main/src/test/resources/nats-java.png)

Nats Server for testing which contains the original [Nats server](https://github.com/nats-io/nats-server)

### Index

* [Variants](#variants)
* [Configuration Properties](#configuration-properties)
* [Configuration Priority Order](#configuration-priority)
* [Java Common Methods](#common-methods)
* Examples
  * [Java-Nats-Server](#java-nats-example)
  * [Java-Nats-Server (JUnit)](#java-nats-junit-example)
  * [Java-Nats-Server (Spring)](#java-nats-spring-example)
  * [Java-Nats-Streaming-Server](#java-nats-streaming-example) *([deprecation-notice](https://github.com/nats-io/nats-streaming-server#warning--deprecation-notice-warning))*
  * [Java-Nats-Streaming-Server (Spring)](#java-nats-streaming-example) *([deprecation-notice](https://github.com/nats-io/nats-streaming-server#warning--deprecation-notice-warning))*

### Variants

|Repositories                                                                                         | Framework        | Maven Central                                                                                                                                                                                                                                                                                                                                                                                                                             | Contains                                                                 |
|-----------------------------------------------------------------------------------------------------|------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------|
|[Java-Nats-Server](https://github.com/YunaBraska/nats-server)                                        | **Plain Java**   | [!["MVN Central"](https://img.shields.io/maven-central/v/berlin.yuna/nats-server?style=flat-square '"MVN Central"')](https://search.maven.org/artifact/berlin.yuna/nats-server) [!["Latest Change"](https://img.shields.io/github/last-commit/YunaBraska/nats-server?style=flat-square '"Latest Change"')](https://github.com/YunaBraska/nats-server/commits)                                                                             | [Original Nats Server](https://github.com/nats-io/nats-server)           |
|[Java-Nats-Server-JUnit](https://github.com/YunaBraska/nats-server-junit)                            | **JUnit 5**      | [!["MVN Central"](https://img.shields.io/maven-central/v/berlin.yuna/nats-server-junit?style=flat-square '"MVN Central"')](https://search.maven.org/artifact/berlin.yuna/nats-server-junit) [!["Latest Change"](https://img.shields.io/github/last-commit/YunaBraska/nats-server-junit?style=flat-square '"Latest Change"')](https://github.com/YunaBraska/nats-server-junit/commits)                                                     | [Java-Nats-Server](https://github.com/YunaBraska/nats-server)            |
|[Java-Nats-Server-Embedded](https://github.com/YunaBraska/nats-server-embedded)                      | **Spring Boot**  | [!["MVN Central"](https://img.shields.io/maven-central/v/berlin.yuna/nats-server-embedded?style=flat-square '"MVN Central"')](https://search.maven.org/artifact/berlin.yuna/nats-server-embedded) [!["Latest Change"](https://img.shields.io/github/last-commit/YunaBraska/nats-server-embedded?style=flat-square '"Latest Change"')](https://github.com/YunaBraska/nats-server-embedded/commits)                                         | [Java-Nats-Server](https://github.com/YunaBraska/nats-server)            |
|[Java-Nats-Streaming-Server](https://github.com/YunaBraska/nats-streaming-server)                    | **Plain Java**   | [!["MVN Central"](https://img.shields.io/maven-central/v/berlin.yuna/nats-streaming-server?style=flat-square '"MVN Central"')](https://search.maven.org/artifact/berlin.yuna/nats-streaming-server) [!["Latest Change"](https://img.shields.io/github/last-commit/YunaBraska/nats-streaming-server?style=flat-square '"Latest Change"')](https://github.com/YunaBraska/nats-streaming-server/commits)                                     | [Original Nats Streaming Server](https://github.com/nats-io/nats-streaming-server) *([deprecation-notice](https://github.com/nats-io/nats-streaming-server#warning--deprecation-notice-warning))* |
|[Java-Nats-Streaming-Server-Embedded](https://github.com/YunaBraska/nats-streaming-server-embedded)  | **Spring Boot**  | [!["MVN Central"](https://img.shields.io/maven-central/v/berlin.yuna/nats-streaming-server-embedded?style=flat-square '"MVN Central"')](https://search.maven.org/artifact/berlin.yuna/nats-streaming-server-embedded) [!["Latest Change"](https://img.shields.io/github/last-commit/YunaBraska/nats-streaming-server-embedded?style=flat-square '"Latest Change"')](https://github.com/YunaBraska/nats-streaming-server-embedded/commits) | [Java-Nats-Streaming-Server](https://github.com/YunaBraska/nats-streaming-server) *([deprecation-notice](https://github.com/nats-io/nats-streaming-server#warning--deprecation-notice-warning))* |

### Configuration properties

* Configs can be found
  under [NatsConfig](https://github.com/YunaBraska/nats-server/blob/main/src/main/java/berlin/yuna/natsserver/config/NatsConfig.java)
  or [NatsStreamingConfig](https://github.com/YunaBraska/nats-streaming-server/blob/main/src/main/java/berlin/yuna/natsserver/config/NatsStreamingConfig.java)
  * The available config keys are based on the default `NATS_VERSION` version of the current artefact
* The properties must start with the prefix **`NATS_`** *(e.g `NATS_CLUSTER_ID`)*
* `nats.properties` can be created optionally to configure the Nats Server

### Configuration priority

1) Custom Arguments
2) Java DSL config
3) Property File (default `nats.properties`)
4) Environment Variables (*1)
5) Default Config

* *1 configs must start with "NATS_" and the additional option
  from [NatsConfig](https://github.com/YunaBraska/nats-server/blob/main/src/main/java/berlin/yuna/natsserver/config/NatsConfig.java))*

### Common methods

#### Getter

| Name                                 | Description                                      |
|--------------------------------------|--------------------------------------------------|
| binaryFile                           | Path to binary file                              |
| downloadUrl                          | Download URL                                     |
| port                                 | port (-1 == not started && random port)          |
| pid                                  | process id (-1 == not started)                   |
| pidFile                              | Path to PID file                                 |
| config                               | Get config map                                   |
| getValue                             | Get resolved config for a key                    |
| getConfigFile                        | Get resolved config file if exists               |

#### Setter

| Name                                 | Description                                      |
|--------------------------------------|--------------------------------------------------|
| config(key, value)                   | Set specific config value                        |
| config(Map<key, value>)              | Set config map                                   |
| config(key, value...)                | Set config array                                 |

#### Others

| Name                                 | Description                                      |
|--------------------------------------|--------------------------------------------------|
| start                                | Starts the nats server                           |
| start(timeout)                       | Starts the nats server with custom timeout       |
| tryStart()                           | Starts the nats server (mode = RuntimeException) |
| stop()                               | Stops the nats server                            |
| stop(timeout)                        | Stops the nats server with custom timeout        |
| config(Map<key, value>)              | Set config map                                   |
| config(key, value...)                | Set config array                                 |

### Java Nats Example

[!["MVN Central"](https://img.shields.io/maven-central/v/berlin.yuna/nats-server?style=flat-square '"MVN Central"')](https://search.maven.org/artifact/berlin.yuna/nats-server) [!["Latest Change"](https://img.shields.io/github/last-commit/YunaBraska/nats-server?style=flat-square '"Latest Change"')](https://github.com/YunaBraska/nats-server/commits)

```java
public class MyNatsTest {

  public static void main(final String[] args) {
    final Nats nats = new Nats(4222) //-1 means random port
            .config(USER, "my_optional_user")
            .config(PASS, "my_optional_password")
            .config(NATS_BINARY_PATH, "optional/ready/to/use/nats/file")
            .config(NATS_DOWNLOAD_URL, "optional/nats/download/url")
            .config(NATS_CONFIG_FILE, "optional/config/file")
            .config(NATS_ARGS, "--optionalArg1=123\\,--optionalArg2=456")
            .config(NATS_VERSION, "v.1.0.0.optional")
            .config(NATS_SYSTEM, "optional_download_suffix")
            .start();
    nats.stop();
  }
}
```

### Java Nats Streaming Example

[!["MVN Central"](https://img.shields.io/maven-central/v/berlin.yuna/nats-streaming-server?style=flat-square '"MVN Central"')](https://search.maven.org/artifact/berlin.yuna/nats-streaming-server) [!["Latest Change"](https://img.shields.io/github/last-commit/YunaBraska/nats-streaming-server?style=flat-square '"Latest Change"')](https://github.com/YunaBraska/nats-streaming-server/commits)

**[Deprecation Notice](https://github.com/nats-io/nats-streaming-server#warning--deprecation-notice-warning)**

```java
public class MyNatsTest {

  public static void main(final String[] args) {
    final Nats nats = new Nats(4222) //-1 means random port
            .config(USER, "my_optional_user")
            .config(PASS, "my_optional_password")
            .config(NATS_BINARY_PATH, "optional/ready/to/use/nats/file")
            .config(NATS_DOWNLOAD_URL, "optional/nats/download/url")
            .config(NATS_CONFIG_FILE, "optional/config/file")
            .config(NATS_ARGS, "--optionalArg1=123\\,--optionalArg2=456")
            .config(NATS_VERSION, "v.1.0.0.optional")
            .config(NATS_SYSTEM, "optional_download_suffix")
            .start();
    nats.stop();
  }
}
```

### Java Nats JUnit Example

[!["MVN Central"](https://img.shields.io/maven-central/v/berlin.yuna/nats-server-junit?style=flat-square '"MVN Central"')](https://search.maven.org/artifact/berlin.yuna/nats-server-junit) [!["Latest Change"](https://img.shields.io/github/last-commit/YunaBraska/nats-server-junit?style=flat-square '"Latest Change"')](https://github.com/YunaBraska/nats-server-junit/commits)

* Port -1 = random port
* keepAlive = nats starts only one time in the whole test context

```java

@JUnitNatsServer(
        port = 4680,
        keepAlive = true,
        timeoutMs = 10000,
        configFile = "my.properties",
        downloadUrl = "https://example.com",
        binaryFile = "/tmp/natsserver",
        config = {"ADDR", "localhost"}
)
class NatsServerFirstTest {

  final NatsServer natsServer = getNatsServer();

  @Test
  void natsServerShouldStart() {
    assertThat(natsServer, is(notNullValue()));
  }
}
```

### Java Nats Spring Example

[!["MVN Central"](https://img.shields.io/maven-central/v/berlin.yuna/nats-server-embedded?style=flat-square '"MVN Central"')](https://search.maven.org/artifact/berlin.yuna/nats-server-embedded) [!["Latest Change"](https://img.shields.io/github/last-commit/YunaBraska/nats-server-embedded?style=flat-square '"Latest Change"')](https://github.com/YunaBraska/nats-server-embedded/commits)

* Supports spring config
* [Configuration Properties](#configuration-properties) are overwriting the spring properties

```java

@SpringBootTest
@RunWith(SpringRunner.class)
@EnableNatsServer(port = 4222, config = {"user", "admin", "pass", "admin"})
public class SomeTest {
    //[...]
}
```

```yaml
nats:
  server:
    hb_fail_count: 3
```

```properties
nats.server.hb_fail_count=3
```

### Java Nats Streaming Spring Example

[!["MVN Central"](https://img.shields.io/maven-central/v/berlin.yuna/nats-streaming-server-embedded?style=flat-square '"MVN Central"')](https://search.maven.org/artifact/berlin.yuna/nats-streaming-server-embedded) [!["Latest Change"](https://img.shields.io/github/last-commit/YunaBraska/nats-streaming-server-embedded?style=flat-square '"Latest Change"')](https://github.com/YunaBraska/nats-streaming-server-embedded/commits)

**[Deprecation Notice](https://github.com/nats-io/nats-streaming-server#warning--deprecation-notice-warning)**

* Supports spring config
* [Configuration Properties](#configuration-properties) are overwriting the spring properties

```java

@SpringBootTest
@RunWith(SpringRunner.class)
@EnableNatsStreamingServer(port = 4222, config = {"user", "admin", "pass", "admin"})
public class SomeTest {
    //[...]
}
```

```yaml
nats:
  streaming:
    server:
      hb_fail_count: 3
```

```properties
nats.streaming.server.hb_fail_count=3
```
