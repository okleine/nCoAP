## A simple Server with nCoAP

This MAVEN module contains a simple server hosting the two Web Services:

* `/simple` (not observable)
* `/utc-time` (observable, updates every 5 seconds)

both of which support content formats `text/plain` (no. 0) and  `application/xml` (no. 40). To start the server simply
type

`java -jar ncoap-simple-server-1.8.3-SNAPSHOT.one-jar.jar`

after running the Maven target "package", i.e. `mvn package`, in a terminal or your IDE.