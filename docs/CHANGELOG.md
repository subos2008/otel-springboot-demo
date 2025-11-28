Read down for latest updates.

We are getting traces going out of backend with the auto-instrumented `backend-agent` version:

![[Pasted image 20251127163715.png]]

## Camel

Added `backends/springboot-starter/camel-rest-app` - note this is using `camel-opentelemetry` already.

When running a `GET` *without* Camel we get a nice trace:

![[Pasted image 20251128173139.png]]
But **with** Camel we only get the incoming trace and not the outgoing trace:

![[Pasted image 20251128173225.png]]