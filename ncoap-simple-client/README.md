## A simple Client with nCoAP

This MAVEN module contains  a simple example application, to showcase how to use the protocol implementation for
clients. The client is configured using command line parameters. For a complete list of supported parameters use

`java -jar ncoap-simple-client-1.8.3-SNAPSHOT.one-jar.jar --help`

on the command line after compiling this Maven module or program parameter `--help` in your IDE.

### Example 1

To send a single request to `coap://example.org:5683/test` one can start the client using the following
program parameters (command line or IDE) parameters:

`--host example.org --port 5683 --path /test --non --duration 20`

This will cause a non-confirmable CoAP request sent to the resource.  Afterwards the client awaits either a single
response or 20 seconds to pass (whatever happens first). Then the application is shut down.

### Example 2

To start the observation of `coap://example.org:5683/obs` one can start the client using the following
program parameters:

`--host example.org --port 5683 --path /obs --observing --maxUpdates 5 --duration 90`

This will cause a confirmable CoAP request with the observing option set to be sent to the resource and either await 5
update notifications or 90 seconds to pass (whatever happens first). If one of this shutdown criteria is satisfied,
the application is shut down after another delay of 10 seconds.

The 10 seconds are to (possibly) enable a graceful abortion of the running observation (depends on the update interval
of the observed resource).

**Note:** The parameter `--duration` has a default value of 60, i.e. the client shuts down at the latest after 60
seconds (and another 10 seconds as mentioned above) if no other value was explicitly set.
