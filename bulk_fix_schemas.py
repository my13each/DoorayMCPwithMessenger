#!/usr/bin/env python3
"""
Script to fix Tool.Input double-nesting issue in all remaining tool files.
"""
import re
import sys
from pathlib import Path

def fix_tool_schema(file_path):
    """Fix the double-nesting issue in a single tool file."""
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()

    original_content = content

    # Pattern 1: Remove put("type", "object") line
    content = re.sub(
        r'(\s+)put\("type", "object"\)\n',
        '',
        content
    )

    # Pattern 2: Remove putJsonObject("properties") { and its closing }
    # This is more complex - we need to find the matching brace

    # Find all occurrences of inputSchema = Tool.Input(
    pattern = r'(inputSchema\s*=\s*Tool\.Input\(\s*properties\s*=\s*buildJsonObject\s*\{)\s*putJsonObject\("properties"\)\s*\{'

    def fix_match(match):
        start_pos = match.end()
        # Find the content after putJsonObject("properties") {
        # We need to find the closing } for properties and remove it
        remaining = content[start_pos:]

        # Count braces to find the matching close
        brace_count = 1
        pos = 0
        while pos < len(remaining) and brace_count > 0:
            if remaining[pos] == '{':
                brace_count += 1
            elif remaining[pos] == '}':
                brace_count -= 1
            pos += 1

        if brace_count == 0:
            # Found the matching close brace
            # Now we need to check if the next line contains putJsonArray("required")
            # or if this is the end of buildJsonObject
            inner_content = remaining[:pos-1]  # Content between the braces (excluding closing })
            after_close = remaining[pos:]

            # Check if we need to keep something after the closing brace
            # Look for putJsonArray("required") or closing of buildJsonObject
            lines_after = after_close.split('\n')
            first_line = lines_after[0].strip() if lines_after else ''

            # If the first line after } is putJsonArray("required"), we need to include it
            # Otherwise, we're at the end
            if first_line.startswith('putJsonArray'):
                # Find where putJsonArray section ends
                remaining_after = after_close
                # Return the fixed version with properties block removed
                return match.group(1) + '\n' + inner_content + '\n' + remaining_after[:remaining_after.find('}', remaining_after.find('putJsonArray'))]
            else:
                # No required array, just close
                return match.group(1) + '\n' + inner_content

        return match.group(0)  # Return unchanged if we can't find matching brace

    # Simpler approach: use regex to match the entire structure
    # Match: inputSchema = Tool.Input(\n  properties = buildJsonObject {\n    putJsonObject("properties") {
    # Then find and remove that line and its matching }

    # Let's use a different approach - find and replace the specific pattern
    # Pattern: properties = buildJsonObject {\n<spaces>put("type", "object")\n<spaces>putJsonObject("properties") {

    pattern = r'(properties\s*=\s*buildJsonObject\s*\{\s*)(putJsonObject\("properties"\)\s*\{)'

    # First, remove put("type", "object") lines that were already done above

    # Now handle the putJsonObject("properties") { removal
    # We'll do this in multiple passes

    # Find instances where we have:
    # buildJsonObject {
    #   putJsonObject("properties") {
    #     putJsonObject("param1") ...

    # Strategy: Replace 'buildJsonObject {\n<indent>putJsonObject("properties") {' with 'buildJsonObject {'
    # and remove the corresponding closing '}\n<indent>}' before 'putJsonArray("required")' or '}'

    # Use a more surgical regex approach
    lines = content.split('\n')
    fixed_lines = []
    i = 0
    skip_next_properties_close = False
    properties_indent_to_remove = None

    while i < len(lines):
        line = lines[i]

        # Check if this line contains putJsonObject("properties") after buildJsonObject
        if 'putJsonObject("properties")' in line and i > 0 and 'buildJsonObject' in lines[i-1]:
            # Skip this line
            skip_next_properties_close = True
            # Capture the indentation level to know which } to remove
            properties_indent_to_remove = len(line) - len(line.lstrip())
            i += 1
            continue

        # Check if we need to skip a closing brace
        if skip_next_properties_close and line.strip() == '}':
            # Check indentation - we want to remove the } that closes putJsonObject("properties")
            current_indent = len(line) - len(line.lstrip())
            if current_indent == properties_indent_to_remove:
                # This is the closing brace for putJsonObject("properties"), skip it
                skip_next_properties_close = False
                properties_indent_to_remove = None
                i += 1
                continue

        fixed_lines.append(line)
        i += 1

    content = '\n'.join(fixed_lines)

    return content, original_content != content


def main():
    # Find all Tool.kt files that still have the issue
    tools_dir = Path(__file__).parent / "src/main/kotlin/com/bifos/dooray/mcp/tools"

    if not tools_dir.exists():
        print(f"Error: Directory not found: {tools_dir}")
        return 1

    fixed_count = 0
    skipped_count = 0

    for file_path in tools_dir.glob("*Tool.kt"):
        # Skip already fixed files
        if file_path.name in ['SearchMembersTool.kt', 'GetWikisTool.kt', 'CreateChannelTool.kt']:
            continue

        # Check if file needs fixing
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
            if 'put("type", "object")' not in content:
                skipped_count += 1
                continue

        print(f"Fixing {file_path.name}...")
        fixed_content, was_changed = fix_tool_schema(file_path)

        if was_changed:
            with open(file_path, 'w', encoding='utf-8') as f:
                f.write(fixed_content)
            fixed_count += 1
            print(f"  ✓ Fixed {file_path.name}")
        else:
            print(f"  - No changes needed for {file_path.name}")
            skipped_count += 1

    print(f"\n✅ Fixed {fixed_count} files")
    print(f"⏭️  Skipped {skipped_count} files (already fixed or no changes needed)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
