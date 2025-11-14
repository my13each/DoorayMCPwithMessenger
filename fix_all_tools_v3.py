#!/usr/bin/env python3
"""
모든 Tool 파일에서 Tool.Input 이중 중첩 문제를 수정
SearchMembersTool과 GetWikisTool의 수정 패턴을 기반으로 함
"""

import re
import glob
from pathlib import Path

def fix_tool_schema_v3(content: str, filename: str) -> tuple[str, bool]:
    """
    Tool.Input의 이중 중첩 문제를 수정
    """
    original = content

    # SearchMembersTool은 이미 수정됨
    if 'SearchMembersTool' in filename:
        return content, False

    # GetWikisTool도 이미 수정됨
    if 'GetWikisTool' in filename:
        return content, False

    # 패턴 1: put("type", "object") 줄 제거
    content = re.sub(
        r'\n\s+put\("type",\s*"object"\)\s*\n',
        '\n',
        content,
        flags=re.MULTILINE
    )

    # 패턴 2: putJsonObject("properties") { 줄 제거
    # buildJsonObject 또는 properties = 다음에 오는 putJsonObject("properties") { 제거
    content = re.sub(
        r'(buildJsonObject\s*\{)\s*\n\s*putJsonObject\("properties"\)\s*\{\s*\n',
        r'\1\n',
        content
    )

    # 패턴 3: 닫는 괄호 } 하나 제거
    # putJsonObject("properties")에 대응하는 닫는 괄호 제거
    # }  <- putJsonObject("properties")의 닫는 괄호
    # }  <- buildJsonObject의 닫는 괄호
    # 두 개의 } 중 첫 번째를 제거
    content = re.sub(
        r'\n(\s+)\}\s*\n\s+\}\)',
        r'\n\1})',
        content
    )

    return content, (content != original)

def process_file(file_path: Path) -> tuple[bool, str]:
    """파일을 처리하고 변경 여부 및 결과 반환"""
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()

    new_content, changed = fix_tool_schema_v3(content, file_path.name)

    if changed:
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(new_content)
        return True, "Modified"
    else:
        return False, "Unchanged"

def main():
    # 모든 Tool 파일 찾기
    tool_files = sorted(glob.glob('src/main/kotlin/com/bifos/dooray/mcp/tools/*Tool.kt'))

    print(f"\n=== Processing {len(tool_files)} tool files ===\n")

    modified_count = 0
    results = []

    for file_path in tool_files:
        path = Path(file_path)
        changed, status = process_file(path)

        if changed:
            modified_count += 1
            print(f"✅ {path.name}: {status}")
        else:
            print(f"⏭️  {path.name}: {status}")

        results.append((path.name, status))

    print(f"\n=== Summary ===")
    print(f"Total files: {len(tool_files)}")
    print(f"Modified: {modified_count}")
    print(f"Unchanged: {len(tool_files) - modified_count}")

    return modified_count

if __name__ == '__main__':
    exit(0 if main() > 0 else 1)
