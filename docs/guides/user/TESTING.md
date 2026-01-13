# TESTING

```shell
--- To initialize an MCP session, use the following curl command:
curl -v http://192.168.109.36:9001/mcp/v1/ \
-H "Content-Type: application/json" \
-H "Accept: text/event-stream,application/json" \
-d '{
  "jsonrpc": "2.0", 
  "id": 1, 
  "method": "initialize", 
  "params": {
    "protocolVersion": "2024-11-05",
    "capabilities": {},
    "clientInfo": {"name": "curl-test", "version": "1.0"}
  }
}'

--- After getting the Mcp-Session-Id from the response headers, use it in the next ack request:

curl -v http://192.168.109.36:9001/mcp/v1/chinook/ \
-H "Content-Type: application/json" \
-H "Accept: text/event-stream,application/json" \
-H "Mcp-Session-Id: 7bce45e6-cc6d-4af8-b3a0-e311580fc099" \
-d '{
  "jsonrpc": "2.0",
  "method": "notifications/initialized"
}'

--- To list available tools, use the following curl command:

curl -v http://192.168.109.36:9001/mcp/v1/blog/ \
-H "Content-Type: application/json" \
-H "Accept: text/event-stream,application/json" \
-H "Mcp-Session-Id: 7bce45e6-cc6d-4af8-b3a0-e311580fc099" \
-d '{
  "jsonrpc": "2.0",
  "id": 2,
  "method": "tools/list"
}'

--- to call a tool, use the following curl command:
curl -v -N -X POST http://192.168.109.36:9001/mcp/v1/chinook/ \
-H "Content-Type: application/json" \
-H "Accept: text/event-stream,application/json" \
-H "Mcp-Session-Id: ef89f26a-cc5b-4a64-af64-6ca100ec65c6" \
-d '{
    "jsonrpc": "2.0",
    "id": "1",
    "method": "tools/call",
    "params": {
      "name": "create_author",
      "arguments": {
        "username": "Radioheadpatate",
        "email": "d@gmail.com"
      }
    }
}'
```

# StdIO

Inialization Request:
```json
{"jsonrpc":"2.0","id":"1","method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"test-client","version":"1.0.0"}}}
```

---

Complete handshake response:
```json
{"jsonrpc":"2.0","method":"notifications/initialized","params":{}}
```

---
List tools request:
```json
{"jsonrpc":"2.0","id":"2","method":"tools/list","params":{}}
```

---
List resources:
```json
{"jsonrpc":"2.0","id":"3","method":"resources/list","params":{}}
```

---
List prompts:
```json
{"jsonrpc":"2.0","id":"4","method":"prompts/list","params":{}}
```

---
list artists:
```json
{"jsonrpc":"2.0","id":"5","method":"resources/read","params":{"uri":"list_artists","arguments":{}}}
```

---
Subscribe to notifications (optional)
```json
{"jsonrpc":"2.0","id":"8","method":"resources/subscribe","params":{"uri":"chinook://schemas/artist.json"}}
```

---

# Testing wit Inspector
```shell
DANGEROUSLY_OMIT_AUTH=true npx @modelcontextprotocol/inspector java -jar /home/hchaibi/workspace/idea-projects/cheshire-framework/cheshire-blog-app/target/blog-app-1.0-SNAPSHOT.jar
npx @modelcontextprotocol/inspector java -jar cheshire-blog-app/target/blog-app-1.0-SNAPSHOT.jar blog-mcp-stdio.yaml
```

--- To initialize an MCP session, use the following request in MCP Inspector:
# Testing with Claude

logs %USERPROFILE%\AppData\Roaming\Claude\logs\

# TROUBLE_SHOUTING_DEBUG_INFOS:
---

### Fixing paths in JettyMcpModule

``` java
.mcpEndpoint("/")
context.setContextPath(baseUrl);
context.addServlet(new ServletHolder(transport), "/*");
```

---

### Using MCP Inspector not wokring as expected ...

TODO: BE EXPLORED

---

### Curl command returns 301 Moved Permanently instead of expected response with initialization result.

```shell
hchaibi@HC:~/workspace/idea-projects$ curl -v -X POST http://192.168.109.36:9001/mcp/v1 \
-H "Content-Type: application/json" \
-H "Accept: text/event-stream, application/json" \
-d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"curl-test","version":"1.0"}}}'
Note: Unnecessary use of -X or --request, POST is already inferred.
*   Trying 192.168.109.36:9000...
* Connected to 192.168.109.36 (192.168.109.36) port 9000 (#0)
> POST /mcp/v1 HTTP/1.1
> Host: 192.168.109.36:9000
> User-Agent: curl/7.81.0
> Content-Type: application/json
> Accept: text/event-stream, application/json
> Content-Length: 156
> 
* Mark bundle as not supporting multiuse
< HTTP/1.1 301 Moved Permanently
< Server: Jetty(12.1.5)
< Date: Mon, 05 Jan 2026 05:57:48 GMT
< Location: /mcp/v1/
< Content-Length: 0
< 
* Connection #0 to host 192.168.109.36 left intact
```

The issue here is that the request is being redirected from `/mcp/v1` to `/mcp/v1/` due to the missing trailing slash. To fix this, ensure that the URL you are using in the curl command includes the trailing slash.

TODO:
ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
context.setContextPath("/mcp/v1");

// Disable the automatic redirect from /mcp/v1 to /mcp/v1/
context.setRedirectWelcome(false);

// Ensure the servlet mapping handles the empty path relative to the context
context.addServlet(new ServletHolder(transport), "/*");

---

### MCP Transport is strictly enforcing content negotiation.

```shell
hchaibi@HC:~/workspace/idea-projects$ curl -v -X POST http://192.168.109.36:9001/mcp/v1/ -H "Content-Type: application/json" -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{}}'
Note: Unnecessary use of -X or --request, POST is already inferred.
*   Trying 192.168.109.36:9000...
* Connected to 192.168.109.36 (192.168.109.36) port 9000 (#0)
> POST /mcp/v1/ HTTP/1.1
> Host: 192.168.109.36:9000
> User-Agent: curl/7.81.0
> Accept: */*
> Content-Type: application/json
> Content-Length: 58
> 
* Mark bundle as not supporting multiuse
< HTTP/1.1 400 Bad Request
< Server: Jetty(12.1.5)
< Date: Mon, 05 Jan 2026 06:07:58 GMT
< Content-Type: application/json;charset=utf-8
< Transfer-Encoding: chunked
< 
{
  "cause" : null,
  "stackTrace" : [ {
    "classLoaderName" : "app",
    "moduleName" : null,
    "moduleVersion" : null,
    "methodName" : "doPost",
    "fileName" : "HttpServletStreamableServerTransportProvider.java",
    "lineNumber" : 403,
    "className" : "io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider",
    "nativeMethod" : false
    .....
    
        "classLoaderName" : null,
    "moduleName" : "java.base",
    "moduleVersion" : "21.0.9",
    "methodName" : "run",
    "fileName" : "VirtualThread.java",
    "lineNumber" : 329,
    "className" : "java.lang.VirtualThread",
    "nativeMethod" : false
  } ],
  "jsonRpcError" : null,
  "message" : "text/event-stream required in Accept header; application/json required in Accept header",
  "suppressed" : [ ],
  "localizedMessage" : "text/event-stream required in Accept header; application/json required in Accept header"
* Connection #0 to host 192.168.109.36 left intact
```

*"message" : "text/event-stream required in Accept header; application/json required in Accept header",*

---

### MCP Transport is not accepting requests without Accept header.

```shell
hchaibi@HC:~/workspace/idea-projects$ curl -v http://192.168.109.36:9001/mcp/v1/ \
-H "Content-Type: application/json" \
-H "Accept: text/event-stream,application/json" \
-d '{
  "jsonrpc": "2.0", 
  "id": 1, 
  "method": "initialize", 
  "params": {
    "protocolVersion": "2024-11-05",
    "capabilities": {},
    "clientInfo": {"name": "curl-test", "version": "1.0"}
  }
}'
*   Trying 192.168.109.36:9000...
* Connected to 192.168.109.36 (192.168.109.36) port 9000 (#0)
> POST /mcp/v1/ HTTP/1.1
> Host: 192.168.109.36:9000
> User-Agent: curl/7.81.0
> Content-Type: application/json
> Accept: text/event-stream, application/json
> Content-Length: 200
> 
* Mark bundle as not supporting multiuse
< HTTP/1.1 200 OK
< Server: Jetty(12.1.5)
< Date: Mon, 05 Jan 2026 06:28:44 GMT
< Content-Type: application/json;charset=utf-8
< Mcp-Session-Id: 7b1115a2-cb69-4f58-88d7-d2e3e42d82ab
< Transfer-Encoding: chunked
< 
{
  "jsonrpc" : "2.0",
  "id" : 1,
  "result" : {
    "protocolVersion" : "2024-11-05",
    "capabilities" : {
      "logging" : { },
      "resources" : {
        "subscribe" : true,
        "listChanged" : true
      },
      "tools" : {
        "listChanged" : true
      }
    },
    "serverInfo" : {
      "name" : "cheshire-mcp-jetty",
      "version" : "1.0.0"
    }
  }
* Connection #0 to host 192.168.109.36 left intact
```

The issue here is that the MCP Transport requires the Accept header to determine the response format. To fix this, ensure that the Accept header is included in your requests.
note: params include capabilities and clientInfo, TODO: check the MCP specification.
note: the above calls include the Accept header, so they should work fine.
note: going forward the session id should be included in the Mcp-Session-Id header for subsequent requests after initialization.

---

### MCP Transport requires to call notifications/initialized after initialization.

```shell
hchaibi@HC:~/workspace/idea-projects$ curl -v http://192.168.109.36:9001/mcp/v1/ \
-H "Content-Type: application/json" \
-H "Accept: text/event-stream,application/json" \
-H "Mcp-Session-Id: 7b1115a2-cb69-4f58-88d7-d2e3e42d82ab" \
-d '{
  "jsonrpc": "2.0",
  "method": "notifications/initialized"
}'
*   Trying 192.168.109.36:9000...
* Connected to 192.168.109.36 (192.168.109.36) port 9000 (#0)
> POST /mcp/v1/ HTTP/1.1
> Host: 192.168.109.36:9000
> User-Agent: curl/7.81.0
> Content-Type: application/json
> Accept: text/event-stream,application/json
> Mcp-Session-Id: 7b1115a2-cb69-4f58-88d7-d2e3e42d82ab
> Content-Length: 63
> 
* Mark bundle as not supporting multiuse
< HTTP/1.1 202 Accepted
< Server: Jetty(12.1.5)
< Date: Mon, 05 Jan 2026 06:38:53 GMT
< Content-Length: 0
< 
* Connection #0 to host 192.168.109.36 left intact
```

---

### List tools after initialization returns empty list.

```shell
curl -v http://192.168.109.36:9001/mcp/v1/ \
-H "Content-Type: application/json" \
-H "Accept: text/event-stream,application/json" \
-H "Mcp-Session-Id: 7b1115a2-cb69-4f58-88d7-d2e3e42d82ab" \
-d '{
  "jsonrpc": "2.0",
  "id": 2,
  "method": "tools/list"
}'
```

### Make sure app logs or anything else are not polluting the System.out - MCP Transport in standard I/O mode will scream if the System.in and System.out are not reserved for MCP communication.

