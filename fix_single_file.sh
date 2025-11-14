#!/bin/bash
# 하나의 파일을 수정하는 스크립트

FILE="$1"

# 1. put("type", "object") 줄 삭제
sed -i.bak '/^[[:space:]]*put("type", "object")$/d' "$FILE"

# 2. putJsonObject("properties") { 줄 삭제
sed -i.bak '/^[[:space:]]*putJsonObject("properties")[[:space:]]*{$/d' "$FILE"

# 3. properties 닫는 } 찾아서 삭제 (putJsonArray("required") 바로 전의 })
# 또는 Tool.Input 닫기 전의 }
sed -i.bak '/^[[:space:]]*}[[:space:]]*$/,/^[[:space:]]*putJsonArray("required")/{/^[[:space:]]*}[[:space:]]*$/d;}' "$FILE"
sed -i.bak '/^[[:space:]]*}[[:space:]]*$/,/^[[:space:]]*})\),/{/^[[:space:]]*}[[:space:]]*$/N;s/\n[[:space:]]*}/\n            }/;}' "$FILE"

echo "Modified: $FILE"
rm -f "$FILE.bak"
