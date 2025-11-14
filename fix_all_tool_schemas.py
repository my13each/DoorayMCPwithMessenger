#!/usr/bin/env python3
"""
모든 Tool 파일에서 Tool.Input의 이중 중첩 문제를 수정하는 스크립트

변경사항:
1. put("type", "object") 줄 제거
2. 외부 putJsonObject("properties") { 제거
3. 해당 닫는 괄호 } 제거
"""

import re
import glob
from pathlib import Path

def fix_tool_schema(content: str) -> tuple[str, bool]:
    """
    Tool.Input의 이중 중첩 문제를 수정

    Before:
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                put("type", "object")
                putJsonObject("properties") {
putJsonObject("name") {
                    ...
                }
                }
            }),

    After:
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                putJsonObject("name") {
                    ...
                }
            }),
    """

    original_content = content

    # 1. put("type", "object") 줄 제거
    content = re.sub(
        r'\n\s+put\("type", "object"\)\s*\n',
        '\n',
        content
    )

    # 2. putJsonObject("properties") { 줄 제거 (buildJsonObject 직후에 오는 것)
    # buildJsonObject { 다음에 오는 putJsonObject("properties") { 를 찾아서 제거
    content = re.sub(
        r'(buildJsonObject\s*\{\s*)\n\s+putJsonObject\("properties"\)\s*\{\s*\n',
        r'\1\n',
        content
    )

    # 3. 닫는 괄호 } 제거 (putJsonObject("properties")에 대응하는 것)
    # }  <- putJsonObject("properties")의 닫는 괄호
    # }  <- buildJsonObject의 닫는 괄호
    # 패턴: 들여쓰기가 같은 두 개의 } 중 첫 번째 제거
    content = re.sub(
        r'(\s+)\}\s*\n\s+\}\s*\n(\s+)\]\)',
        r'\1}\n\2]),',
        content
    )

    # 대체 패턴: } 두 개가 연속으로 나오는 경우
    content = re.sub(
        r'(\s+)\}\s*\n\s+\}\s*\n(\s+)\)\),',
        r'\1}\n\2}),',
        content
    )

    return content, (content != original_content)

def process_file(file_path: Path) -> bool:
    """파일을 처리하고 변경 여부를 반환"""
    print(f"Processing: {file_path.name}")

    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()

    new_content, changed = fix_tool_schema(content)

    if changed:
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(new_content)
        print(f"  ✅ Modified")
        return True
    else:
        print(f"  ⏭️  No changes needed")
        return False

def main():
    # 모든 Tool 파일 찾기
    tool_files = sorted(glob.glob('src/main/kotlin/com/bifos/dooray/mcp/tools/*Tool.kt'))

    print(f"\n=== Found {len(tool_files)} tool files ===\n")

    modified_count = 0
    for file_path in tool_files:
        if process_file(Path(file_path)):
            modified_count += 1

    print(f"\n=== Summary ===")
    print(f"Total files: {len(tool_files)}")
    print(f"Modified: {modified_count}")
    print(f"Unchanged: {len(tool_files) - modified_count}")

if __name__ == '__main__':
    main()
