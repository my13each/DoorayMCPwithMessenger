#!/usr/bin/env python3
import subprocess
import json
import sys
import time

# MCP 서버를 실행하고 tools/list 요청을 보낸 후, 17번째 도구의 스키마를 확인

proc = subprocess.Popen(
    ['/opt/homebrew/Cellar/openjdk@21/21.0.8/libexec/openjdk.jdk/Contents/Home/bin/java',
     '-jar', 'build/libs/dooray-mcp-server-0.2.14-all.jar'],
    stdin=subprocess.PIPE,
    stdout=subprocess.PIPE,
    stderr=subprocess.PIPE,
    text=True,
    env={'DOORAY_API_KEY': 'test_key', 'DOORAY_BASE_URL': 'https://api.dooray.com'}
)

# Initialize 요청
init_request = {
    "jsonrpc": "2.0",
    "id": 1,
    "method": "initialize",
    "params": {
        "protocolVersion": "2024-11-05",
        "capabilities": {},
        "clientInfo": {"name": "test", "version": "1.0"}
    }
}

# tools/list 요청
tools_request = {
    "jsonrpc": "2.0",
    "id": 2,
    "method": "tools/list",
    "params": {}
}

try:
    # 요청 전송
    proc.stdin.write(json.dumps(init_request) + '\n')
    proc.stdin.write(json.dumps(tools_request) + '\n')
    proc.stdin.flush()

    # 응답 읽기
    time.sleep(2)

    for _ in range(10):  # 최대 10줄 읽기
        line = proc.stdout.readline()
        if not line:
            break

        try:
            data = json.loads(line)
            if 'result' in data and 'tools' in data['result']:
                tools = data['result']['tools']
                print(f"Total tools: {len(tools)}\n")

                if len(tools) > 16:
                    tool_16 = tools[15]  # 0-based index, 16번째 도구
                    tool_17 = tools[16]  # 17번째 도구

                    print("=== Tool #16 ===")
                    print(f"Name: {tool_16['name']}")
                    print(f"Input Schema:")
                    print(json.dumps(tool_16.get('inputSchema', {}), indent=2))

                    print("\n=== Tool #17 ===")
                    print(f"Name: {tool_17['name']}")
                    print(f"Input Schema:")
                    print(json.dumps(tool_17.get('inputSchema', {}), indent=2))
                break
        except json.JSONDecodeError:
            continue
except Exception as e:
    print(f"Error: {e}", file=sys.stderr)
finally:
    proc.terminate()
