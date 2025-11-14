#!/usr/bin/env python3
import os
import glob
import re

def fix_indentation(file_path):
    """Fix indentation after putJsonObject removal"""
    with open(file_path, 'r', encoding='utf-8') as f:
        lines = f.readlines()

    # Skip excluded files
    basename = os.path.basename(file_path)
    if basename in ['GetWikisTool.kt', 'UpdateWikiPageTool.kt']:
        return False

    fixed = False
    new_lines = []
    in_input_schema = False
    schema_indent = 0

    for i, line in enumerate(lines):
        # Detect start of inputSchema
        if 'inputSchema' in line and 'Tool.Input' in line:
            in_input_schema = True
            # Find the indentation of buildJsonObject
            if i + 2 < len(lines) and 'buildJsonObject' in lines[i + 2]:
                # Get the indentation before buildJsonObject
                match = re.match(r'(\s*)buildJsonObject', lines[i + 2])
                if match:
                    schema_indent = len(match.group(1))

        # Fix excessive indentation in inputSchema section
        if in_input_schema:
            # Check for lines with excessive indentation (more than expected)
            match = re.match(r'(\s+)putJsonObject\("', line)
            if match:
                current_indent = len(match.group(1))
                # If indentation is way too much (like 28 spaces), fix it
                if current_indent > schema_indent + 10:
                    # Reduce to proper level (schema_indent + 4)
                    proper_indent = ' ' * (schema_indent + 4)
                    line = proper_indent + line.lstrip()
                    fixed = True

            # Check if we're exiting inputSchema
            if line.strip().startswith('),') or (line.strip() == '),' and 'outputSchema' in lines[i + 1] if i + 1 < len(lines) else False):
                in_input_schema = False

        new_lines.append(line)

    if fixed:
        with open(file_path, 'w', encoding='utf-8') as f:
            f.writelines(new_lines)
        print(f"Fixed indentation in {basename}")
        return True

    return False

def main():
    tools_dir = '/Users/jp17463/DoorayMCP/src/main/kotlin/com/bifos/dooray/mcp/tools'
    tool_files = glob.glob(os.path.join(tools_dir, '*Tool.kt'))

    fixed_count = 0
    for file_path in sorted(tool_files):
        if fix_indentation(file_path):
            fixed_count += 1

    print(f"\nTotal files with indentation fixed: {fixed_count}")

if __name__ == '__main__':
    main()
