#!/bin/bash

# MCP 서버를 실행하고 tools/list 요청을 보낸 후, 17번째 도구의 스키마만 추출

cat <<'JSONRPC' | DOORAY_API_KEY="test" DOORAY_BASE_URL="https://api.dooray.com" timeout 10 /opt/homebrew/Cellar/openjdk@21/21.0.8/libexec/openjdk.jdk/Contents/Home/bin/java -jar build/libs/dooray-mcp-server-0.2.14-all.jar 2>/dev/null | python3 -c "
import json
import sys

lines = sys.stdin.readlines()
for line in lines:
    try:
        data = json.loads(line)
        if 'result' in data and 'tools' in data['result']:
            tools = data['result']['tools']
            if len(tools) > 16:
                tool17 = tools[16]  # 0-based index
                print(f\"Tool 17: {tool17['name']}\")
                print(json.dumps(tool17.get('inputSchema', {}), indent=2))
            break
    except:
        pass
"
JSONRPC

{"jsonrpc": "2.0", "id": 1, "method": "initialize", "params": {"protocolVersion": "2024-11-05", "capabilities": {}, "clientInfo": {"name": "test", "version": "1.0"}}}
{"jsonrpc": "2.0", "id": 2, "method": "tools/list", "params": {}}
JSONRPC

chmod +x /Users/jp17463/DoorayMCP/check_tool_17.sh
