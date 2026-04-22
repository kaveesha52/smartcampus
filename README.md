# Smart Campus - Sensor & Room Management API

A RESTful API built with **JAX-RS (Jersey 2.39)** and an embedded **Grizzly HTTP server**, implementing full room and sensor management for the University of Westminster's Smart Campus initiative.

---

## Table of Contents

1. [API Design Overview](#api-design-overview)
2. [Technology Stack](#technology-stack)
3. [Project Structure](#project-structure)
4. [Build & Run Instructions](#build--run-instructions)
5. [API Endpoints Reference](#api-endpoints-reference)
6. [Sample curl Commands](#sample-curl-commands)
7. [Report: Question Answers](#report-question-answers)

---

## API Design Overview

The API follows **REST architectural principles** with a versioned base path of `/api/v1`. It models three core entities - `Room`, `Sensor`, and `SensorReading` - with a logical hierarchy that mirrors the physical campus:

```
/api/v1
  /rooms
    /{roomId}
  /sensors
    /{sensorId}
      /readings
```

Key design decisions:
- **In-memory storage** using `ConcurrentHashMap` and `CopyOnWriteArrayList` for thread-safe, database-free operation.
- **Sub-Resource Locator pattern** for nested reading history, keeping resource classes focused and maintainable.
- **Exception Mappers** for every error scenario - no raw stack traces ever reach the client.
- **Request/Response logging filter** for full API observability without polluting business logic.
- **HATEOAS links** (`_links`) in all POST responses to guide clients to related resources.

---

## Technology Stack

| Component        | Technology                           |
|-----------------|--------------------------------------|
| Language         | Java 11+                             |
| JAX-RS Impl.     | Jersey 2.39.1                        |
| HTTP Server      | Grizzly2 (embedded, no Tomcat needed)|
| JSON             | Jackson (via jersey-media-json-jackson) |
| Build Tool       | Maven 3.x                            |
| Packaging        | Executable fat JAR (Maven Shade)     |

> **Important:** No Spring Boot, no database (SQL/NoSQL). Pure JAX-RS with in-memory data structures as required.

---

## Project Structure

```
smart-campus-api/
├── pom.xml
└── src/main/java/com/smartcampus/
    ├── Main.java                          
    ├── SmartCampusApplication.java        
    ├── model/
    │   ├── Room.java
    │   ├── Sensor.java
    │   ├── SensorReading.java
    │   └── ApiError.java                  
    ├── store/
    │   └── DataStore.java                 
    ├── resource/
    │   ├── DiscoveryResource.java        
    │   ├── RoomResource.java              
    │   ├── SensorResource.java            
    │   └── SensorReadingResource.java     
    ├── exception/
    │   ├── RoomNotEmptyException.java
    │   ├── LinkedResourceNotFoundException.java
    │   └── SensorUnavailableException.java
    ├── mapper/
    │   ├── RoomNotEmptyExceptionMapper.java          
    │   ├── LinkedResourceNotFoundExceptionMapper.java 
    │   ├── SensorUnavailableExceptionMapper.java      
    │   └── GlobalExceptionMapper.java                 
    └── filter/
        └── LoggingFilter.java             
```

---

## Build & Run Instructions

### Prerequisites

- Java 11 or higher (`java -version`)
- Maven 3.6+ (`mvn -version`)

### Step 1 - Clone the repository

```bash
git clone https://github.com/YOUR_USERNAME/smart-campus-api.git
cd smart-campus-api
```

### Step 2 - Build the fat JAR

```bash
mvn clean package
```

This produces `target/smart-campus-api-1.0.0.jar` - a self-contained executable with all dependencies bundled.

### Step 3 - Start the server

```bash
java -jar target/smart-campus-api-1.0.0.jar
```

The server starts on **port 8080**. You should see:

```
INFO: Smart Campus API started.
INFO: Endpoints available at: http://localhost:8080/api/v1
INFO: Press CTRL+C to stop.
```

### Step 4 - Verify it's running

```bash
curl http://localhost:8080/api/v1
```

Press `CTRL+C` to stop the server.

---

## API Endpoints Reference

| Method | Endpoint                                    | Description                                      | Status |
|--------|---------------------------------------------|--------------------------------------------------|--------|
| GET    | `/api/v1`                                   | Discovery — API metadata + hypermedia links      | 200    |
| GET    | `/api/v1/rooms`                             | List all rooms                                   | 200    |
| POST   | `/api/v1/rooms`                             | Create a new room                                | 201    |
| GET    | `/api/v1/rooms/{roomId}`                    | Get room details                                 | 200/404|
| DELETE | `/api/v1/rooms/{roomId}`                    | Delete room (blocked if sensors present)         | 204/409|
| GET    | `/api/v1/sensors`                           | List all sensors (optional `?type=` filter)      | 200    |
| GET    | `/api/v1/sensors/{sensorId}`               | Get sensor details                               | 200/404|
| POST   | `/api/v1/sensors`                           | Register new sensor (validates roomId)           | 201/422|
| GET    | `/api/v1/sensors/{sensorId}/readings`      | Get reading history for a sensor                 | 200    |
| POST   | `/api/v1/sensors/{sensorId}/readings`      | Append a new reading (blocked if MAINTENANCE)    | 201/403|

---

## Sample curl Commands

### 1. Discover the API

```bash
curl -s http://localhost:8080/api/v1 | python3 -m json.tool
```

### 2. Create a new room

```bash
curl -s -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"id":"CS-101","name":"Computer Science Lecture Theatre","capacity":120}' \
  | python3 -m json.tool
```

### 3. List all rooms

```bash
curl -s http://localhost:8080/api/v1/rooms | python3 -m json.tool
```

### 4. Register a new sensor linked to a room

```bash
curl -s -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"CO2-002","type":"CO2","status":"ACTIVE","currentValue":400.0,"roomId":"CS-101"}' \
  | python3 -m json.tool
```

### 5. Filter sensors by type

```bash
curl -s "http://localhost:8080/api/v1/sensors?type=Temperature" | python3 -m json.tool
```

### 6. Post a sensor reading

```bash
curl -s -X POST http://localhost:8080/api/v1/sensors/TEMP-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value":23.4}' \
  | python3 -m json.tool
```

### 7. Get reading history for a sensor

```bash
curl -s http://localhost:8080/api/v1/sensors/TEMP-001/readings | python3 -m json.tool
```

### 8. Attempt to delete a room with sensors 

```bash
curl -s -X DELETE http://localhost:8080/api/v1/rooms/LIB-301 | python3 -m json.tool
```

### 9. Attempt to post a reading to a maintance sensor 

```bash
curl -s -X POST http://localhost:8080/api/v1/sensors/OCC-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value":15.0}' \
  | python3 -m json.tool
```

### 10. Attempt to register a sensor with a non-existent roomId (expect 422)

```bash
curl -s -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"GHOST-001","type":"Temperature","status":"ACTIVE","currentValue":0.0,"roomId":"DOES-NOT-EXIST"}' \
  | python3 -m json.tool
```

---

## Report: Question Answers

---

### Part 1.1 - JAX-RS Resource Lifecycle & Concurrency

**Q:** Explain the default lifecycle of a JAX-RS Resource class. Is a new instance created per request or is it a singleton? How does this impact in-memory data management?

**Answer:**

By default, JAX-RS uses a **per-request lifecycle** for resource classes: the runtime instantiates a brand-new object for every incoming HTTP request and discards it once the response is sent. This is the default behaviour defined in the JAX-RS specification and is the safest choice because each request operates on an isolated object with no shared mutable state on the resource class itself.

However, this creates a critical challenge for in-memory APIs: if data were stored as instance fields on the resource class (e.g. `private Map<String, Room> rooms = new HashMap<>()`), every request would start with an empty map and any data written by a previous request would be permanently lost. The API would be stateless in the worst sense — incapable of retaining any data at all.

To solve this, this implementation uses a **centralised `DataStore` class with `static` fields backed by `ConcurrentHashMap` and `CopyOnWriteArrayList`**. Static fields exist at the class level in the JVM, not the instance level, so they persist across the entire lifetime of the application regardless of how many resource instances are created or destroyed.

The choice of `ConcurrentHashMap` over `HashMap` is deliberate and essential. In a real server environment, the HTTP container manages a **thread pool**: multiple requests execute simultaneously on different threads. If two threads attempt to write to a plain `HashMap` concurrently — for example, two POST requests registering sensors at the same moment — the result is a **race condition**: corrupted internal state, lost entries, or `ConcurrentModificationException`. `ConcurrentHashMap` uses **segment-level locking** (in Java 8+, lock striping on individual buckets) to allow safe concurrent reads and writes without requiring the entire map to be locked, giving high throughput without data corruption. `CopyOnWriteArrayList` similarly ensures thread-safe list iteration by creating a fresh copy of the underlying array on every write — ideal for the sensor reading lists where reads far outnumber writes.

In this project, the resource classes themselves hold no mutable state at all. They are pure **stateless handlers** that delegate all reads and writes to the shared `DataStore`. This means that even if Jersey creates a new `RoomResource` instance for every request (the default behaviour), data integrity is guaranteed because the data lives outside those instances entirely.

---

### Part 1.2 - HATEOAS and Hypermedia Design

**Q:** Why is the provision of Hypermedia (HATEOAS) considered a hallmark of advanced RESTful design? How does it benefit client developers compared to static documentation?

**Answer:**

HATEOAS — Hypermedia as the Engine of Application State — is the principle that REST API responses should include **links to related resources and available actions**, allowing clients to navigate the API dynamically rather than relying on externally memorised URLs.

Roy Fielding, who defined REST in his doctoral dissertation, described HATEOAS as a mandatory constraint of the REST architectural style. An API that does not include hypermedia links is, strictly speaking, not fully RESTful — it is merely an HTTP-based RPC service.

The practical benefit for client developers is profound. With a static-documentation approach, a developer must read external docs to know that creating a sensor requires a POST to `/api/v1/sensors` with a JSON body containing a `roomId`. If the URL structure changes — say, to `/api/v2/sensors` — every client breaks silently and requires manual updates. With HATEOAS, the Discovery endpoint (`GET /api/v1`) returns the authoritative URL for the sensors collection in the response body. Clients follow the link provided; they do not hardcode it. URL changes propagate automatically.

Furthermore, HATEOAS communicates **available actions in context**. A POST response that creates a sensor and immediately returns `_links` containing `self`, `readings`, and `room` tells the client exactly what it can do next without requiring a separate documentation lookup. This is particularly powerful for automated systems and IoT clients that must make decisions programmatically.

In this API, every POST response includes a `_links` object, and the Discovery endpoint provides a complete resource map — a machine-readable sitemap of the entire API. This follows the Richardson Maturity Model Level 3, the highest level of REST maturity.

---

### Part 2.1 - Returning Full Objects vs IDs in List Responses

**Q:** When returning a list of rooms, what are the implications of returning only IDs versus returning the full room objects?

**Answer:**

This is a classic **network efficiency vs. client convenience** trade-off with implications for both bandwidth and architectural coupling.

**Returning only IDs** minimises the initial response payload. For a campus with thousands of rooms, returning just `["LIB-301", "LAB-102", "AUD-001"]` is extremely lightweight. However, this forces the client into the **N+1 problem**: to display even a basic room listing page, the client must make one additional GET request per room ID to fetch its name, capacity, and sensor list. For 500 rooms, that is 501 HTTP requests. On mobile networks or in IoT environments with limited bandwidth budgets, this pattern is disastrous for performance and battery life.

**Returning full room objects** costs more bandwidth per request but eliminates the N+1 problem entirely. A client can render a complete rooms dashboard from a single HTTP call. For this API's use case — facilities managers needing to see room names, capacities, and sensor counts at a glance — returning full objects is clearly superior.

The best production approach is often a **projection pattern**: the client specifies the fields it needs via a query parameter (e.g. `?fields=id,name`) and the server returns only those fields. This gives the bandwidth efficiency of ID-only responses while avoiding unnecessary round trips. This API returns full objects by default, which is the right choice for an administrative dashboard scenario where all fields are routinely needed.

---

### Part 2.2 - Idempotency of DELETE

**Q:** Is the DELETE operation idempotent in your implementation? Justify by describing what happens if the same DELETE request is sent multiple times.

**Answer:**

Yes, the DELETE operation in this implementation is **idempotent**, consistent with the HTTP specification (RFC 7231).

Idempotency means that sending the same request multiple times produces the same **server state** as sending it once - it does not mean every response is identical.

In this implementation:
- **First DELETE** on `/api/v1/rooms/CS-101`: the room exists, has no sensors, and is successfully removed. The server returns `204 No Content`.
- **Second DELETE** on `/api/v1/rooms/CS-101`: the room no longer exists. The server returns `404 Not Found` with a JSON error body.
- **Third (and any subsequent) DELETE**: identical to the second - `404 Not Found`.

After the first DELETE, the server state is "room CS-101 does not exist." Every subsequent DELETE finds the same state. The **observable effect on the server is identical** - the room remains absent. This satisfies idempotency: multiple identical requests leave the server in the same final state as a single request.

The 409 Conflict case (room has sensors) is also idempotent: sending the same DELETE on a room that always has sensors will always return 409, and the server state never changes. The operation is consistently refused.

The only non-idempotent HTTP method in this API is POST, which creates a new resource on every successful call.

---

### Part 3.1 - @Consumes and Media Type Mismatch

**Q:** We use `@Consumes(MediaType.APPLICATION_JSON)` on the POST method. What happens if a client sends data in a different format such as text/plain or application/xml?

**Answer:**

The `@Consumes` annotation tells the JAX-RS runtime to **only dispatch the request to this method if the incoming `Content-Type` header matches `application/json`**. If a client sends a POST with `Content-Type: text/plain` or `Content-Type: application/xml`, JAX-RS performs content negotiation **before the method is invoked**.

The runtime compares the request's `Content-Type` against all `@Consumes`-annotated methods on the resource. If no method matches, JAX-RS automatically returns **HTTP 415 Unsupported Media Type** with no involvement from application code. The resource method body is never executed.

This has two important implications. First, it is a security and robustness feature: malformed or unexpected input formats are rejected at the framework level before deserialization is attempted, preventing Jackson from trying to parse arbitrary text as JSON and potentially throwing unhandled exceptions. Second, it enforces a strict contract: the API declares its accepted format explicitly, and clients that do not honour this contract receive a clear, standards-compliant error code.

If the `@Consumes` annotation were absent, JAX-RS would attempt to deserialize any content type, which could result in a `400 Bad Request` from Jackson, or in ambiguous routing where multiple methods match the same path. The annotation also enables **content negotiation**: a resource could declare multiple overloaded methods with different `@Consumes` values (e.g. one for JSON, one for XML) and JAX-RS would route to the correct one automatically based on the `Content-Type` header.

---

### Part 3.2 - @QueryParam vs Path Segment for Filtering

**Q:** You used `@QueryParam` for filtering sensors by type. Contrast this with embedding the type in the URL path (e.g. `/api/v1/sensors/type/CO2`). Why is the query parameter approach superior for filtering?

**Answer:**

The core REST principle that distinguishes these two approaches is that **path segments identify resources, while query parameters modify the representation of a collection**.

A path like `/api/v1/sensors/type/CO2` implies that `type` and `CO2` are hierarchical components of a unique resource identifier. This is semantically incorrect: there is no single "CO2 sensors" resource — there is a sensors collection that can be viewed through a CO2 filter. Embedding the filter in the path also creates several practical problems:

**Combinatorial explosion**: if filtering by multiple fields is ever needed (e.g. type AND status), path-based design produces ugly URLs like `/api/v1/sensors/type/CO2/status/ACTIVE`. With query parameters, this is simply `?type=CO2&status=ACTIVE` - clean, conventional, and extensible.

**Caching semantics**: HTTP caches and CDNs treat path segments as resource identifiers. `/api/v1/sensors` and `/api/v1/sensors?type=CO2` are understood to be the same base resource with a filtered view. Path-based designs confuse caches into thinking they are entirely different resources, degrading cache hit rates.

**Optionality**: `@QueryParam` parameters are optional by design. The same endpoint `GET /api/v1/sensors` serves both filtered and unfiltered requests. Path-based filtering requires separate route definitions for the filtered and unfiltered cases, adding routing complexity.

**REST convention**: the HTTP specification and REST community convention clearly treat query strings as the mechanism for search, filtering, pagination, and sorting - all operations that narrow or transform a collection view without identifying a new resource.

For these reasons, `@QueryParam` is the semantically correct, cache-friendly, and extensible approach for all filtering and search operations on collections.

---

### Part 4.1 - Sub-Resource Locator Pattern Benefits

**Q:** Discuss the architectural benefits of the Sub-Resource Locator pattern. How does delegating to separate classes help manage complexity compared to defining every nested path in one controller?

**Answer:**

The Sub-Resource Locator pattern is a structural design technique in JAX-RS where a resource method carries only a `@Path` annotation - no HTTP verb - and returns an instance of another class that handles the nested path. JAX-RS resolves the locator at runtime and dispatches the sub-request to the returned object.

In this API, `SensorResource` declares:
```java
@Path("/{sensorId}/readings")
public SensorReadingResource getReadingResource(@PathParam("sensorId") String sensorId) {
    return new SensorReadingResource(sensorId);
}
```

This delegates all `/sensors/{sensorId}/readings` operations to `SensorReadingResource`, which focuses entirely on reading management logic.

The architectural benefits are substantial:

**Single Responsibility Principle**: each resource class has one job. `SensorResource` manages sensor registration and querying; `SensorReadingResource` manages reading history. Neither class is burdened with the other's concerns.

**Scalability of codebase**: in a real campus API, sensors might have sub-resources for calibration records, maintenance logs, firmware updates, and alert thresholds. Without the locator pattern, a single `SensorResource` class would grow to hundreds of methods across dozens of concerns - an unmaintainable monolith. With the pattern, each concern lives in its own file.

**Context propagation**: the `sensorId` path parameter is captured by the locator and passed into the sub-resource constructor. The sub-resource always knows which sensor it is operating on without needing to re-parse path parameters in every method.

**Independent testing**: `SensorReadingResource` can be unit-tested in complete isolation from `SensorResource` by constructing it directly with a known `sensorId`.

**Runtime flexibility**: JAX-RS can apply filters, interceptors, and injections to the sub-resource independently, enabling fine-grained security or logging policies per resource type.

---

### Part 5.2 - Why HTTP 422 over 404 for Missing References

**Q:** Why is HTTP 422 Unprocessable Entity often considered more semantically accurate than 404 Not Found when the issue is a missing reference inside a valid JSON payload?

**Answer:**

The distinction lies in **what was not found**: the request URL, or a resource referenced inside the request body.

**HTTP 404 Not Found** means the server cannot locate the resource identified by the **request URI itself**. If a client requests `GET /api/v1/rooms/GHOST-999` and that room does not exist, 404 is correct — the URI identifies a resource that does not exist on the server.

**HTTP 422 Unprocessable Entity** means the server received and understood the request — the URI is valid, the HTTP method is appropriate, the `Content-Type` is correct, and the JSON is syntactically well-formed — but the **semantic content of the payload is invalid** and the instruction cannot be carried out. The request is understood but unprocessable.

In the case of `POST /api/v1/sensors` with `"roomId": "DOES-NOT-EXIST"`, the request URI `/api/v1/sensors` is perfectly valid and found. The JSON is syntactically correct. But the `roomId` field contains a reference to a room that does not exist in the system. The payload fails a **domain-level integrity check**, not a routing check. Returning 404 here would be misleading: it would imply to the client that `/api/v1/sensors` was not found, causing confusion. Returning 422 precisely communicates: "I found your endpoint, I parsed your JSON, but the content you provided has a semantic error."

This distinction is especially important for automated clients and IoT systems that use HTTP status codes programmatically. A 404 triggers "retry a different URL" logic; a 422 triggers "fix the payload" logic. Using the wrong code sends the wrong signal and causes incorrect automated behaviour. HTTP 422 is defined in RFC 4918 (WebDAV) and is widely adopted in REST APIs precisely for this foreign-key validation scenario.

---

### Part 5.4 - Cybersecurity Risks of Exposing Stack Traces

**Q:** From a cybersecurity standpoint, explain the risks of exposing internal Java stack traces to external API consumers. What specific information could an attacker gather?

**Answer:**

Exposing raw Java stack traces to API consumers is a serious **information disclosure vulnerability**, classified under OWASP Top 10 as A05:2021 — Security Misconfiguration. A stack trace is a diagnostic tool intended for developers; in the hands of an attacker it becomes a reconnaissance asset.

Specifically, an attacker can extract the following from a Java stack trace:

**Internal package and class structure**: fully-qualified class names like `com.smartcampus.store.DataStore.getRooms(DataStore.java:47)` reveal the internal architecture of the application. An attacker learns whether the codebase follows MVC patterns, uses DAO layers, or has identifiable service classes — all useful for targeted attacks.

**Third-party library names and versions**: stack traces typically include frames from dependency libraries, e.g. `org.hibernate.engine.jdbc.spi.SqlExceptionHelper.logExceptions(SqlExceptionHelper.java:137)`. This immediately reveals that Hibernate is in use and often reveals the version. The attacker can then look up known CVEs (Common Vulnerabilities and Exposures) for that exact version and craft an exploit accordingly.

**Database and ORM details**: exceptions from database layers reveal table names, column names, and query structures that would otherwise be completely invisible to an external caller.

**Server file system paths**: Java exceptions from file I/O operations often include absolute paths like `/var/app/smartcampus/config/database.properties`, revealing the deployment directory structure.

**Business logic and control flow**: the sequence of method calls in a stack trace reveals the code path that triggered the error, allowing an attacker to understand the application's internal decision-making and look for exploitable branches.

**Null pointer and index exceptions**: these reveal assumptions the code makes about data - e.g. `NullPointerException` at a line accessing `sensor.getRoomId()` tells an attacker that sending a sensor payload with a null `roomId` causes a crash, potentially exploitable as a denial-of-service vector.

The `GlobalExceptionMapper` in this API addresses all of these risks by catching every unhandled `Throwable`, logging the full stack trace **server-side only** (accessible to developers via server logs), and returning nothing more than a generic `500 Internal Server Error` message to the client. The client receives zero information about the internal failure beyond the fact that it occurred.

---

### Part 5.5 - JAX-RS Filters vs Manual Logging

**Q:** Why is it advantageous to use JAX-RS filters for cross-cutting concerns like logging, rather than manually inserting Logger.info() statements inside every resource method?

**Answer:**

A **cross-cutting concern** is a behaviour that applies uniformly across many parts of an application - logging, authentication, CORS headers, rate limiting, and compression are classic examples. Implementing these concerns by manually inserting code into every resource method violates fundamental software engineering principles and creates serious long-term problems.

**DRY (Don't Repeat Yourself)**: with manual logging, every resource method must contain the same Logger.info() boilerplate. Across 15+ endpoints, this means 30+ logging statements (one for request entry, one for response). Every statement must be written, tested, and maintained individually. A JAX-RS filter centralises this in a single class that executes automatically for every request and response — zero repetition.

**Guaranteed completeness**: a developer writing a new resource method might forget to add logging. With a filter, logging is guaranteed for every endpoint that passes through the JAX-RS pipeline, including new ones added in the future. It is impossible to accidentally skip.

**Separation of concerns**: resource methods should contain business logic - creating rooms, registering sensors. They should not contain infrastructure concerns like logging, timing, or security checks. Filters enforce this separation cleanly. A `RoomResource` that handles only room logic is far easier to read, test, and maintain than one interspersed with logging statements.

**Centralised modification**: if the logging format needs to change - for example, to add a correlation ID or switch to structured JSON logging - a filter requires a single edit in one class. Manual logging requires finding and updating every Logger call across every resource file, with the risk of missing some.

**Pre/Post execution access**: a filter has access to both the `ContainerRequestContext` (before business logic runs) and `ContainerResponseContext` (after it returns). This makes it possible to log the complete round-trip — method, URI, duration, and status code - in a way that individual resource methods cannot do because a method cannot observe its own returned status code from within its own body.

These advantages apply equally to any cross-cutting concern: authentication filters, CORS filters, rate-limiting filters, and request ID injection filters all benefit from the same architectural separation that makes JAX-RS filter chains a cornerstone of professional API design.


