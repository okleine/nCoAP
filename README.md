## nCoAP

### Java implementation of the CoAP protocol

This implementation currently covers

* the raw protocol ([RFC 7252](https://tools.ietf.org/html/rfc7252)),
* the observation of CoAP resources ([RFC 7252](https://tools.ietf.org/html/rfc7252)),
* the blockwise transfer ([draft 19](https://tools.ietf.org/html/draft-ietf-core-block-19)),
* the identification of endpoints with changing IPs
([draft 01](https://tools.ietf.org/html/draft-kleine-core-coap-endpoint-id-01)) , and
* the CoRE Link Format ([RFC 6690](https://tools.ietf.org/html/rfc6690)).


### Maven

The nCoAP project is organized in several maven modules, i.e.,

```xml
<groupId>de.uzl.itm</groupId>
<artifactId>ncoap-core</artifactId>
```

for the raw protocol implementation. For CoAP application development this is probably what you want.
To use the latest protocol implementation release add the following to your pom.xml

```xml
<repositories>
...
    <repository>
        <id>itm-maven-repository-releases</id>
        <name>ITM Maven Snapshots Repository</name>
        <url>https://maven.itm.uni-luebeck.de/content/repositories/snapshots</url>
    </repository>
...
</repositories>
```

...

```xml
<dependencies>
...
    <dependency>
        <groupId>de.uzl.itm</groupId>
        <artifactId>ncoap-core</artifactId>
        <version>1.8.3-SNAPSHOT</version>
    </dependency>
...
</dependencies>
```

The other models, i.e.,

```xml
<groupId>de.uzl.itm</groupId>
<artifactId>ncoap-simple-client</artifactId>
```

and

```xml
<groupId>de.uzl.itm</groupId>
<artifactId>ncoap-simple-server</artifactId>
```

provide simple CoAP applications for both, client and server. There intention is to highlight, how easy it is to
write such applications using ncoap.


### Documentation

The JavaDoc is available at http://media.itm.uni-luebeck.de/people/kleine/maven/ncoap-complete/1.8.3-SNAPSHOT
