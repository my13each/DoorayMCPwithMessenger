#!/usr/bin/env python3
"""
Robustly fix Tool.Input double-nesting by carefully identifying and removing:
1. put("type", "object")
2. putJsonObject("properties") {
3. The matching closing }
"""

import re
from pathlib import Path
import sys


def fix_tool_file(content: str, filename: str) -> tuple[str, bool]:
    """
    Fix a single tool file's inputSchema structure
    """
    original = content

    # Skip already fixed files
    if filename in ['SearchMembersTool.kt', 'GetWikisTool.kt']:
        return content, False

    # Step 1: Remove put("type", "object") line
    content = re.sub(
        r'\n\s*put\("type",\s*"object"\)\s*\n',
        '\n',
        content
    )

    # Step 2: Remove putJsonObject("properties") { line
    # Match it carefully: after buildJsonObject { and optional whitespace/newlines
    content = re.sub(
        r'(properties\s*=\s*buildJsonObject\s*\{\s*\n)'
        r'\s*putJsonObject\("properties"\)\s*\{\s*\n',
        r'\1',
        content
    )

    # Step 3: Remove the corresponding closing }
    # This is tricky - we need to find the } that closes putJsonObject("properties")
    # It's either:
    #   a) Right before putJsonArray("required")
    #   b) Right before the } that closes buildJsonObject

    # Case a: } before putJsonArray
    content = re.sub(
        r'\n(\s+)\}\s*\n(\s+)putJsonArray\("required"\)',
        r'\n\2putJsonArray("required")',
        content
    )

    # Case b: Double } before })  (one for putJsonObject("properties"), one for buildJsonObject)
    # Pattern: }  }), where first } is putJsonObject("properties"), second } is buildJsonObject
    content = re.sub(
        r'\n(\s+)\}\s*\n\s*\}\s*\n\s*\)\s*,',
        r'\n\1}\n\1),',
        content
    )

    return content, (content != original)


def main():
    tools_dir = Path('src/main/kotlin/com/bifos/dooray/mcp/tools')

    if not tools_dir.exists():
        print(f"Error: Directory {tools_dir} not found")
        return 1

    tool_files = sorted(tools_dir.glob('*Tool.kt'))

    print(f"\n=== Processing {len(tool_files)} tool files ===\n")

    modified_files = []
    unchanged_files = []

    for tool_file in tool_files:
        content = tool_file.read_text(encoding='utf-8')
        new_content, changed = fix_tool_file(content, tool_file.name)

        if changed:
            tool_file.write_text(new_content, encoding='utf-8')
            modified_files.append(tool_file.name)
            print(f"✅ {tool_file.name}")
        else:
            unchanged_files.append(tool_file.name)
            print(f"⏭️  {tool_file.name}")

    print(f"\n=== Summary ===")
    print(f"Total: {len(tool_files)}")
    print(f"Modified: {len(modified_files)}")
    print(f"Unchanged: {len(unchanged_files)}")

    if modified_files:
        print(f"\nModified files:")
        for f in modified_files[:10]:  # Show first 10
            print(f"  - {f}")
        if len(modified_files) > 10:
            print(f"  ... and {len(modified_files) - 10} more")

    return 0


if __name__ == '__main__':
    sys.exit(main())
