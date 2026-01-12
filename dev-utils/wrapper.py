import sys
import requests
import json

# Configuration: Point this to your running Java/Spring server
HTTP_SERVER_URL = "http://localhost:9000/mcp/v1/chinook/"

def log(message):
    """Logs to stderr so it doesn't interfere with the MCP protocol on stdout."""
    sys.stderr.write(f"BRIDGE-LOG: {message}\n")
    sys.stderr.flush()

def main():
    log("Starting MCP Stdio-to-HTTP Bridge...")

    try:
        # Read from stdin line by line (standard for most MCP clients)
        for line in sys.stdin:
            if not line.strip():
                continue

            payload = line.strip()
            log(f"Received Request: {payload[:100]}...")

            try:
                # Forward to your Java server
                response = requests.post(
                    HTTP_SERVER_URL,
                    data=payload,
                    headers={'Content-Type': 'application/json'},
                    timeout=30 # Allow time for DB queries
                )

                if response.status_code == 200:
                    # Write the response back to stdout for Claude to read
                    sys.stdout.write(response.text + "\n")
                    sys.stdout.flush()
                else:
                    log(f"Server Error: {response.status_code} - {response.text}")

            except requests.exceptions.RequestException as e:
                log(f"HTTP Connection Error: {str(e)}")

    except KeyboardInterrupt:
        log("Bridge shutting down.")

if __name__ == "__main__":
    main()
