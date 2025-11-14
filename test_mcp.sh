#!/bin/bash
export JAVA_HOME=/opt/homebrew/Cellar/openjdk@21/21.0.8/libexec/openjdk.jdk/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH
export DOORAY_API_KEY="ajjt1imxmtj4:F1h8NZUlRLCAQO0k75TZWg"
export DOORAY_BASE_URL="https://api.dooray.com"

# MCP 초기화 요청
echo '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"test","version":"1.0.0"}}}' | java -jar build/libs/dooray-mcp-server-0.2.1-all.jar 2>/dev/null
