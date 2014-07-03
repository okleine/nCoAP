# nCoAP

## Java implementation of the CoAP protocol (draft 18)

see: http://tools.ietf.org/html/draft-ietf-core-coap-18

Draft-18 is the one to become an RFC at some time, i.e. there are (if any) only few and minor changes expected in the
future. This implementation currently supports the main protocol plus

* the core link format (see https://datatracker.ietf.org/doc/rfc6690/) and
* the observe extension (draft 12) (see http://tools.ietf.org/html/draft-ietf-core-observe-12).

## Maven

The nCoAP project is organized in several maven modules, i.e.

<groupId>de.uniluebeck.itm</groupId>
<artifactId>ncoap-core</artifactId>

for the raw protocol implementation. For CoAP application development this probably what you want.
To use the latest protocol implementation release add the following to your pom.xml

```
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

```
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

```
<groupId>de.uniluebeck.itm.ncoap</groupId>
<artifactId>ncoap-simple-client</artifactId>
```

and

```
<groupId>de.uniluebeck.itm.ncoap</groupId>
<artifactId>ncoap-simple-server</artifactId>
```

provide simple CoAP applications for both, client and server. There intention is to highlight, how easy it is to
write such applications using ncoap.


## Documentation

The documentation is available at http://media.itm.uni-luebeck.de/people/kleine/maven/ncoap-complete/1.8.1
