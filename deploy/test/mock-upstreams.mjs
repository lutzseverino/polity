import { createServer } from "node:http";

function listen(port, service) {
  createServer((request, response) => {
    response.writeHead(200, { "content-type": "application/json" });
    response.end(
      JSON.stringify({
        headers: request.headers,
        method: request.method,
        service,
        url: request.url,
      }),
    );
  }).listen(port, "0.0.0.0");
}

listen(8081, "identity");
listen(8084, "polity");
