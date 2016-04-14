## A JAVA implementation of the CoAP

This implementation of the Constrained Application Protocol bases on the asynchronous and event-driven network
application framework [Netty](http://netty.io) (thats where the 'n' in nCoAP comes from). The nCoAP framework
currently covers

* the raw protocol ([RFC 7252](https://tools.ietf.org/html/rfc7252)),
* the observation of CoAP resources ([RFC 7641](https://tools.ietf.org/html/rfc7641)),
* the blockwise transfer ([draft 19](https://tools.ietf.org/html/draft-ietf-core-block-19)),
* the identification of endpoints with changing IPs
([draft 01](https://tools.ietf.org/html/draft-kleine-core-coap-endpoint-id-01)) , and
* the CoRE Link Format ([RFC 6690](https://tools.ietf.org/html/rfc6690)).

but without SSL (i.e. coaps). 

### Maven

The nCoAP project is organized in several maven modules, i.e.,

```xml
<groupId>de.uzl.itm</groupId>
<artifactId>ncoap-core</artifactId>
```

for the raw protocol implementation. For CoAP application development this is probably what you want.

#### Latest SNAPSHOT version (1.8.3-SNAPSHOT)

To use the latest bleeding edge version (SNAPSHOT) add the following to your pom.xml:

```xml
<repositories>
...
    <repository>
        <id>itm-maven-repository-snapshots</id>
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

Note, that for several reasons some interfaces changed since the latest stable version.

#### Latest stable release (1.8.2)

To use the latest stable release add the following to your pom.xml:

```xml
<repositories>
...
    <repository>
        <id>itm-maven-repository-releases</id>
        <name>ITM Maven Releases Repository</name>
        <url>https://maven.itm.uni-luebeck.de/content/repositories/releases</url>
    </repository>
...
</repositories>
```

...

```xml
<dependencies>
...
    <dependency>
        <groupId>de.uniluebeck.itm</groupId>
        <artifactId>ncoap-core</artifactId>
        <version>1.8.2</version>
    </dependency>
...
</dependencies>
```

The JavaDoc is available [here](http://media.itm.uni-luebeck.de/people/kleine/maven/ncoap-complete/1.8.2).

### Examples for Client and Server 

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