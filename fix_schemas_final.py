#!/usr/bin/env python3
"""
This script fixes the JSON schema structure in Kotlin Tool files.
It removes the redundant `put("type", "object")` and `putJsonObject("properties")` wrapper.
"""

import os
import glob
import re

def fix_schema(content):
    """Fix the schema by removing the redundant nesting"""
    # Pattern 1: Match the opening of the bad structure
    # We want to find: buildJsonObject {\n...put("type", "object")\n...putJsonObject("properties") {
    pattern_open = r'(buildJsonObject\s*\{)\s*put\("type",\s*"object"\)\s*putJsonObject\("properties"\)\s*\{'

    # Check if pattern exists
    if not re.search(pattern_open, content, re.MULTILINE):
        return content, False

    # Replace the opening
    content = re.sub(pattern_open, r'\1', content, flags=re.MULTILINE)

    # Pattern 2: Find and remove the extra closing brace
    # After removing the opening, we need to find the matching closing brace for putJsonObject("properties")
    # This should be a `}` that appears before `})` or `}\n...)`

    # Look for the pattern:  }\n(spaces)})\n OR }\n(spaces)}\n(spaces)),
    # We want to remove the first `}`
    pattern_close = r'\}(\s*)\}(\s*)\)\s*,'

    # Count occurrences to decide if we should replace
    matches = list(re.finditer(pattern_close, content))
    if matches:
        # Replace only the first occurrence after buildJsonObject (in inputSchema section)
        content = re.sub(pattern_close, r'}\2),', content, count=1)

    return content, True

def process_file(file_path):
    """Process a single Kotlin tool file"""
    basename = os.path.basename(file_path)

    # Skip already fixed files
    if basename in ['GetWikisTool.kt', 'UpdateWikiPageTool.kt']:
        return False

    with open(file_path, 'r', encoding='utf-8') as f:
        original_content = f.read()

    fixed_content, changed = fix_schema(original_content)

    if changed and fixed_content != original_content:
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(fixed_content)
        print(f"✓ Fixed {basename}")
        return True

    return False

def main():
    tools_dir = '/Users/jp17463/DoorayMCP/src/main/kotlin/com/bifos/dooray/mcp/tools'
    tool_files = sorted(glob.glob(os.path.join(tools_dir, '*Tool.kt')))

    fixed_count = 0
    for file_path in tool_files:
        if process_file(file_path):
            fixed_count += 1

    print(f"\n{fixed_count} files fixed successfully")

if __name__ == '__main__':
    main()
