## nCoAP

### Java implementation of the CoAP protocol (RFC 7252)

See: https://datatracker.ietf.org/doc/rfc7252/

This implementation currently supports the main protocol according to RFC 7252 (without SSL) plus

* the core link format (see https://datatracker.ietf.org/doc/rfc6690/) and
* the observe extension (draft 14) (see http://tools.ietf.org/html/draft-ietf-core-observe-14).

### Maven

The nCoAP project is organized in several maven modules, i.e.,

```xml
<groupId>de.uniluebeck.itm</groupId>
<artifactId>ncoap-core</artifactId>
```

for the raw protocol implementation. For CoAP application development this is probably what you want.
To use the latest protocol implementation release add the following to your pom.xml

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
        <version>1.8.1</version>
    </dependency>
...
</dependencies>
```

The other models, i.e.,

```xml
<groupId>de.uniluebeck.itm</groupId>
<artifactId>ncoap-simple-client</artifactId>
```

and

```xml
<groupId>de.uniluebeck.itm</groupId>
<artifactId>ncoap-simple-server</artifactId>
```

provide simple CoAP applications for both, client and server. There intention is to highlight, how easy it is to
write such applications using ncoap.


### Documentation

The documentation is available at http://media.itm.uni-luebeck.de/people/kleine/maven/ncoap-complete/1.8.2
