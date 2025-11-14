#!/usr/bin/env python3
"""
Fix remaining putJsonObject("properties") issues where it's on the same line or with extra whitespace.
"""
import re
from pathlib import Path

def fix_file(file_path):
    """Fix a single file by removing putJsonObject("properties") wrapper."""
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()

    original = content

    # Pattern: Find buildJsonObject { followed by putJsonObject("properties") {
    # This pattern handles cases where they might be on the same line or have weird whitespace

    # First, normalize: find all cases where putJsonObject("properties") appears after buildJsonObject
    # and remove it along with its matching closing brace

    lines = content.split('\n')
    result_lines = []
    i = 0

    while i < len(lines):
        line = lines[i]

        # Check if this line has buildJsonObject { followed immediately or on next line by putJsonObject("properties")
        if 'buildJsonObject' in line and '{' in line:
            # Check if putJsonObject("properties") is on the same line
            if 'putJsonObject("properties")' in line:
                # Remove putJsonObject("properties") { from this line
                modified_line = line.replace('putJsonObject("properties") {', '').replace('putJsonObject("properties"){', '')
                result_lines.append(modified_line)

                # Now we need to find and remove the matching closing brace
                # Track brace depth to find the right one
                brace_depth = 1  # We're inside buildJsonObject already
                i += 1
                while i < len(lines) and brace_depth > 0:
                    current_line = lines[i]

                    # Count opening and closing braces
                    opens = current_line.count('{')
                    closes = current_line.count('}')
                    brace_depth += opens - closes

                    # If we're closing the properties block (brace_depth == 1 and line is just "}")
                    if brace_depth == 1 and current_line.strip() == '}':
                        # Skip this closing brace - it closes putJsonObject("properties")
                        i += 1
                        continue

                    result_lines.append(current_line)
                    i += 1
                continue

        # Check if next line is putJsonObject("properties")
        if i + 1 < len(lines) and 'putJsonObject("properties")' in lines[i + 1]:
            result_lines.append(line)
            # Skip the putJsonObject("properties") line
            i += 2

            # Find and remove matching closing brace
            brace_depth = 1
            while i < len(lines) and brace_depth > 0:
                current_line = lines[i]
                opens = current_line.count('{')
                closes = current_line.count('}')
                brace_depth += opens - closes

                if brace_depth == 0 and current_line.strip() == '}':
                    # Skip this closing brace
                    i += 1
                    continue

                result_lines.append(current_line)
                i += 1
            continue

        result_lines.append(line)
        i += 1

    content = '\n'.join(result_lines)

    if content != original:
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(content)
        return True
    return False

def main():
    tools_dir = Path("src/main/kotlin/com/bifos/dooray/mcp/tools")

    # Find all files that still have the issue
    import subprocess
    result = subprocess.run(
        ["bash", "-c",
         'for f in $(find src/main/kotlin/com/bifos/dooray/mcp/tools -name "*Tool.kt"); do '
         'if grep -q \'putJsonObject("properties")\' "$f"; then echo "$f"; fi; done'],
        capture_output=True,
        text=True
    )

    files_to_fix = [f.strip() for f in result.stdout.strip().split('\n') if f.strip()]

    print(f"Found {len(files_to_fix)} files to fix")

    fixed_count = 0
    for file_path in files_to_fix:
        print(f"Fixing {Path(file_path).name}...")
        if fix_file(file_path):
            fixed_count += 1
            print(f"  ✓ Fixed")
        else:
            print(f"  - No changes")

    print(f"\n✅ Fixed {fixed_count} files")

if __name__ == "__main__":
    main()
