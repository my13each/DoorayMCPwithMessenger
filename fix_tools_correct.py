#!/usr/bin/env python3
"""
Correctly fix Tool.Input double-nesting by:
1. Removing put("type", "object")
2. Removing putJsonObject("properties") {
3. Properly closing buildJsonObject after all properties and required array
"""

import re
import glob
from pathlib import Path


def fix_tool_schema_correct(content: str, filename: str) -> tuple[str, bool]:
    """
    Fix Tool.Input double-nesting issue
    """
    original = content

    # Skip already fixed tools
    if 'SearchMembersTool' in filename or 'GetWikisTool' in filename:
        return content, False

    # Pattern to match the problematic structure
    # inputSchema = Tool.Input(
    #     properties = buildJsonObject {
    #         put("type", "object")
    #         putJsonObject("properties") {
    #             ... properties ...
    #         }
    #         putJsonArray("required") { ... }  # Optional
    #     }),

    # Step 1: Remove put("type", "object") line
    content = re.sub(
        r'\n\s+put\("type",\s*"object"\)\s*\n',
        '\n',
        content
    )

    # Step 2: Find and remove putJsonObject("properties") { and its closing }
    # We need to be careful to find the right closing brace

    # Match the pattern starting from inputSchema
    pattern = re.compile(
        r'(inputSchema\s*=\s*Tool\.Input\(\s*\n'
        r'\s*properties\s*=\s*buildJsonObject\s*\{\s*\n)'
        r'\s*putJsonObject\("properties"\)\s*\{\s*\n',
        re.MULTILINE
    )

    content = pattern.sub(r'\1', content)

    # Step 3: Find the closing } for putJsonObject("properties")
    # This is the tricky part - we need to find the } that's right before
    # either putJsonArray("required") or the closing } of buildJsonObject

    # Pattern: Find }
    #          followed by optional whitespace and newline
    #          followed by either:
    #            - putJsonArray("required")
    #            - }), which closes buildJsonObject and Tool.Input

    # Case 1: } before putJsonArray
    content = re.sub(
        r'\n(\s+)\}\s*\n\s+putJsonArray\("required"\)',
        r'\n\1putJsonArray("required")',
        content
    )

    # Case 2: } before closing buildJsonObject (no required array)
    # This pattern matches:
    #   }  <- closing a putJsonObject for a property
    #   }  <- closing putJsonObject("properties") - THIS ONE TO REMOVE
    #   }), <- closing buildJsonObject and Tool.Input
    content = re.sub(
        r'\n(\s+)\}\s*\n\s+\}\s*\n\s+\}\),',
        r'\n\1}\n\1}),',
        content
    )

    return content, (content != original)


def process_file(file_path: Path) -> tuple[bool, str]:
    """Process a single file"""
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()

    new_content, changed = fix_tool_schema_correct(content, file_path.name)

    if changed:
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(new_content)
        return True, "Modified"
    else:
        return False, "Unchanged"


def main():
    tool_files = sorted(glob.glob('src/main/kotlin/com/bifos/dooray/mcp/tools/*Tool.kt'))

    print(f"\n=== Processing {len(tool_files)} tool files ===\n")

    modified_count = 0

    for file_path in tool_files:
        path = Path(file_path)
        changed, status = process_file(path)

        if changed:
            modified_count += 1
            print(f"✅ {path.name}: {status}")
        else:
            print(f"⏭️  {path.name}: {status}")

    print(f"\n=== Summary ===")
    print(f"Total files: {len(tool_files)}")
    print(f"Modified: {modified_count}")
    print(f"Unchanged: {len(tool_files) - modified_count}")

    return modified_count


if __name__ == '__main__':
    exit(0 if main() >= 0 else 1)
