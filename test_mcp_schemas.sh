#!/bin/bash

# MCP 서버 실행 및 tools/list 요청echo '{"jsonrpc": "2.0", "id": 1, "method": "tools/list", "params": {}}' | \
timeout 5 ./gradlew runLocal 2>/dev/null | \
grep -A 10000 '"result"' | \
head -500
