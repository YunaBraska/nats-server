# nats-server

[Nats server](https://github.com/nats-io/nats-server) for testing which contains the
original [Nats server](https://github.com/nats-io/nats-server)

[![Build][build_shield]][build_link]
[![Maintainable][maintainable_shield]][maintainable_link]
[![Coverage][coverage_shield]][coverage_link]
[![Issues][issues_shield]][issues_link]
[![Commit][commit_shield]][commit_link]
[![Dependencies][dependency_shield]][dependency_link]
[![License][license_shield]][license_link]
[![Central][central_shield]][central_link]
[![Tag][tag_shield]][tag_link]
[![Javadoc][javadoc_shield]][javadoc_link]
[![Size][size_shield]][size_shield]
![Label][label_shield]

[build_shield]: https://github.com/YunaBraska/nats-server/workflows/JAVA_CI/badge.svg
[build_link]: https://github.com/YunaBraska/nats-server/actions?query=workflow%3AJAVA_CI
[maintainable_shield]: https://img.shields.io/codeclimate/maintainability/YunaBraska/nats-server?style=flat-square
[maintainable_link]: https://codeclimate.com/github/YunaBraska/nats-server/maintainability
[coverage_shield]: https://img.shields.io/codeclimate/coverage/YunaBraska/nats-server?style=flat-square
[coverage_link]: https://codeclimate.com/github/YunaBraska/nats-server/test_coverage
[issues_shield]: https://img.shields.io/github/issues/YunaBraska/nats-server?style=flat-square
[issues_link]: https://github.com/YunaBraska/nats-server/commits/master
[commit_shield]: https://img.shields.io/github/last-commit/YunaBraska/nats-server?style=flat-square
[commit_link]: https://github.com/YunaBraska/nats-server/issues
[license_shield]: https://img.shields.io/github/license/YunaBraska/nats-server?style=flat-square
[license_link]: https://github.com/YunaBraska/nats-server/blob/master/LICENSE
[dependency_shield]: https://img.shields.io/librariesio/github/YunaBraska/nats-server?style=flat-square
[dependency_link]: https://libraries.io/github/YunaBraska/nats-server
[central_shield]: https://img.shields.io/maven-central/v/berlin.yuna/nats-server?style=flat-square
[central_link]:https://search.maven.org/artifact/berlin.yuna/nats-server
[tag_shield]: https://img.shields.io/github/v/tag/YunaBraska/nats-server?style=flat-square
[tag_link]: https://github.com/YunaBraska/nats-server/releases
[javadoc_shield]: https://javadoc.io/badge2/berlin.yuna/nats-server/javadoc.svg?style=flat-square
[javadoc_link]: https://javadoc.io/doc/berlin.yuna/nats-server
[size_shield]: https://img.shields.io/github/repo-size/YunaBraska/nats-server?style=flat-square
[label_shield]: https://img.shields.io/badge/Yuna-QueenInside-blueviolet?style=flat-square
[gitter_shield]: https://img.shields.io/gitter/room/YunaBraska/nats-server?style=flat-square
[gitter_link]: https://gitter.im/nats-server/Lobby

### Family
* Nats plain Java
  * [Nats-Server](https://github.com/YunaBraska/nats-server)
  * [Nats-Streaming-Server](https://github.com/YunaBraska/nats-streaming-server)
* Nats for spring boot
  * [Nats-Server-Embedded](https://github.com/YunaBraska/nats-server-embedded)
  * [Nats-Streaming-Server-Embedded](https://github.com/YunaBraska/nats-streaming-server-embedded)

### Usage

```xml

<dependency>
    <groupId>berlin.yuna</groupId>
    <artifactId>nats-server</artifactId>
    <version>2.1.5</version>
</dependency>
```
[Get latest version][central_link]

### Common methods
#### Getter
| Name                                 | Description                                |
|--------------------------------------|--------------------------------------------|
| port                                 | Get configured port                        |
| pid                                  | Get process id                             |
| config                               | Get config map                             |
| source                               | Get download url                           |
| pidFile                              | Get file containing the process id         |
| natsPath                             | Get nats target path                       |

#### Setter
| Name                                 | Description                                |
|--------------------------------------|--------------------------------------------|
| port(port)                           | Sets specific port (-1 = random port)      |
| config(key, value)                   | Set specific config value                  |
| config(Map<key, value>)              | Set config map                             |
| config(key:value...)                 | Set config array as string                 |
| source(customUrl)                    | Sets custom nats download url              |

* All configurations are optional. (see all configs
  here: [NatsConfig](https://github.com/YunaBraska/nats-server/blob/master/src/main/java/berlin/yuna/natsserver/config/NatsConfig.java))
* Nats server default sources are described
  here: [NatsSourceConfig](https://github.com/YunaBraska/nats-server/blob/master/src/main/java/berlin/yuna/natsserver/config/NatsSourceConfig.java)

### Example

```java
public class MyNatsTest {

    public static void main(String[] args) {
        final Nats nats = new Nats()
                .source("http://myOwnCachedNatsServerVersion")
                .port(4222)
                .config(USER, "yuna")
                .config(PASS, "braska");
        nats.start();
        nats.stop();
    }
}
```

```
                                                             .,,.                                                             
                                                              ,/*.                                                            
                                                               *(/.                                                           
                                                               .(#*..                                                         
                                                               ,(#/..                                                         
                                                              ,(#(*..                                                         
                                                             ,/###/,,                                                         
                                                          ..*(#(**(((*                                                        
                                                         ,((#(/. ./##/.                                                       
                                                        ./##/,   ,(##/.                                                       
                                                        ,(((,   ./###*.                                                       
                                                        ,///.  ,(##//.                                                        
                                                         ,**,,/(#(*                                                           
                                                            ,(#(*.                                                            
                                                          ..*((*.                                                             
                                                          ,,((,                                                               
                                                          ..//.                                                               
                                                            .,.                                                               
                                                         .....,.........                                                      
                                            ..,**///(((((##############(((((((//*,..                                          
                                       .*//((#######################################((/*,.                                    
                                    ,/(###################################################/*..                                
                                .,,(########################################################((/.                              
                                ,((###########################################################(/.                             
                              .(#################################################################*                            
                             .(#.###############################################################*                           
                            ./#....###########################################################...,                          
                            .(#.....########################################################.....*                          
                            ,#.........##################################################.........#/.                         
                            *#...##.........##########################################........#...##(((//*,.                  
                            ,#..####...#............##########################..........#...####..........##/..               
                            ,#..####..###..............................................###..####...........##**               
                            .(#.#####.###....##..................................##...####.#####..#/,,,,/##..((.              
                             ,#..####..####..###....###..................####...###...###..####..#/.    .(#..((.              
                             ./#.#####.#####.####...####................#####..####.#####.#####.,    ./#..#//.              
                              ,#.#####.#####.#####..####................#####.####..##########..#/.    ,##.,,               
                               *#..######################..............#######################.#(.  .//#..##*                 
                                *##.#####################..............#####################..#(,.,/###..#(,                  
                                 **#######################............#####################...#####....##*.                   
                                   ,(#.#####################.........####################.........###//,                      
                                    *########################......######################..#######(/,..                       
                                     ,(#######################....########.############..#/,....                              
                                      ./###############.#####......#####.#############.##/**,,,.                              
                                        ,((#.###########..###......##...###########...#(/*********,.                          
                                         ,,(#.###########..............###########..###/************,..                       
                                            *(#.############.........###########..##//******************,.                    
                                              ,/#############......###########.##(/***********************,.                  
                                                .*((########.........#####.###/,...,,,,,,*******************,..               
                                                  ,,/########......#####.##((*.         ....,,,,*************,,.              
                                                     .,(##.............##(*.....,,,,,,,,,,,,,,,,,**************,              
                                                        .*###........##(/***********************************,..               
                                                            *(#...###/************************************,.                  
                                                             *(#.##//************************************,.                   
                                                              ./(*.,,********************************,,.                      
                                                                       .,************************,..                          
                                                                           ...,,,,******,,,,...                           
```
