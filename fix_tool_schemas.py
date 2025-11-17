#!/usr/bin/env python3
"""
Fix all MCP tools to use JSON Schema draft 2020-12 format (PR #1 approach)
Add 'required' parameter to Tool.Input constructor
"""

import re
from pathlib import Path
from typing import Tuple

def fix_tool_file(file_path: Path) -> Tuple[bool, str]:
    """Fix a single tool file to add required parameter to Tool.Input"""

    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()

    # Pattern to match Tool.Input(...) with properties = buildJsonObject { }
    # We need to handle multiline carefully
    pattern = r'(inputSchema\s*=\s*Tool\.Input\(\s*properties\s*=\s*buildJsonObject\s*\{)'

    if not re.search(pattern, content):
        return False, "No Tool.Input with buildJsonObject found"

    # Check if already has required parameter
    if re.search(r'Tool\.Input\([^)]*required\s*=', content):
        return False, "Already has required parameter"

    # Find the closing of buildJsonObject and add required parameter
    # This is complex because of nested braces
    lines = content.split('\n')
    new_lines = []
    in_input_schema = False
    brace_count = 0
    input_start_line = -1

    for i, line in enumerate(lines):
        if 'inputSchema = Tool.Input(' in line:
            in_input_schema = True
            input_start_line = i

        if in_input_schema:
            # Count braces
            brace_count += line.count('{') - line.count('}')

            # Check if this is the closing line of buildJsonObject
            if brace_count == 0 and 'properties = buildJsonObject' in content[content.find('inputSchema = Tool.Input('):content.find('inputSchema = Tool.Input(') + content[content.find('inputSchema = Tool.Input('):].find(line) + len(line)]:
                # This is the closing brace of buildJsonObject
                new_lines.append(line.rstrip())

                # Add required parameter
                indent = ' ' * (len(line) - len(line.lstrip()))
                new_lines.append(indent[:-4] + '},')
                new_lines.append(indent[:-4] + 'required = emptyList()')

                in_input_schema = False
                continue

        new_lines.append(line)

    # Simpler approach: use regex with careful pattern
    # Find: Tool.Input(\n            properties = buildJsonObject { ... }
    # Replace with: Tool.Input(\n            properties = buildJsonObject { ... }\n        },\n        required = emptyList()

    # Let's use a different approach
    # Find the pattern and replace the closing ) with },\n required = emptyList()\n)

    original_pattern = r'(inputSchema\s*=\s*Tool\.Input\(\s*properties\s*=\s*buildJsonObject\s*\{(?:[^{}]|\{[^{}]*\})*\})\s*\)'

    def replace_func(match):
        inner_content = match.group(1)
        return inner_content + '\n        },\n        required = emptyList()\n    )'

    new_content = re.sub(original_pattern, replace_func, content)

    if new_content == content:
        return False, "Pattern not matched correctly"

    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(new_content)

    return True, "Fixed"

def main():
    tools_dir = Path("/Users/jp17463/DoorayMCP/src/main/kotlin/com/bifos/dooray/mcp/tools")

    if not tools_dir.exists():
        print(f"Error: Tools directory not found: {tools_dir}")
        return

    tool_files = list(tools_dir.glob("*.kt"))
    print(f"Found {len(tool_files)} tool files")

    fixed_count = 0
    skipped_count = 0

    for tool_file in sorted(tool_files):
        fixed, message = fix_tool_file(tool_file)

        if fixed:
            print(f"✅ {tool_file.name}: {message}")
            fixed_count += 1
        else:
            print(f"⏭️  {tool_file.name}: {message}")
            skipped_count += 1

    print(f"\n📊 Summary:")
    print(f"   Fixed: {fixed_count}")
    print(f"   Skipped: {skipped_count}")
    print(f"   Total: {len(tool_files)}")

if __name__ == "__main__":
    main()
