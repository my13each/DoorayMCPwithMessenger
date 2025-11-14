#!/usr/bin/env python3
"""
모든 Tool 파일에서 Tool.Input() 래퍼를 제거하는 스크립트

Before:
    inputSchema = Tool.Input(
        properties = buildJsonObject {
            ...
        }
    ),

After:
    inputSchema = buildJsonObject {
        ...
    },
"""

import re
import glob
from pathlib import Path

def remove_tool_input_wrapper(content: str) -> tuple[str, bool]:
    """
    Tool.Input(properties = ...) 패턴을 찾아서 제거

    Returns:
        (수정된 내용, 변경 여부)
    """

    # Tool.Input( 으로 시작하는 패턴 찾기
    pattern = r'inputSchema\s*=\s*Tool\.Input\(\s*properties\s*=\s*(buildJsonObject\s*\{)'

    # Tool.Input() 래퍼 제거
    new_content = re.sub(
        pattern,
        r'inputSchema = \1',
        content
    )

    # 변경이 있었는지 확인
    if new_content != content:
        # Tool.Input()의 닫는 괄호 ")," 제거
        # inputSchema = buildJsonObject { ... } 다음에 오는 }),를 }), 로 변경
        new_content = re.sub(
            r'(\s+\}\)\s*)\),(\s*outputSchema)',
            r'\1,\2',
            new_content
        )
        return new_content, True

    return content, False

def process_file(file_path: Path) -> bool:
    """
    파일을 처리하고 변경 여부를 반환
    """
    print(f"Processing: {file_path.name}")

    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()

    new_content, changed = remove_tool_input_wrapper(content)

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
    tool_files = glob.glob('src/main/kotlin/com/bifos/dooray/mcp/tools/*Tool.kt')
    tool_files.sort()

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
