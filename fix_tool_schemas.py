#!/usr/bin/env python3
import os
import re
import glob

def fix_tool_schema(file_path):
    """Fix the JSON schema structure in a Tool file"""
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()

    original_content = content

    # Skip if already fixed (check for the pattern that shouldn't exist)
    if 'put("type", "object")' not in content:
        print(f"Skipping {os.path.basename(file_path)} - already fixed")
        return False

    # Find the inputSchema section
    # Pattern: inputSchema = Tool.Input( properties = buildJsonObject { put("type", "object") putJsonObject("properties") {
    pattern = r'(inputSchema\s*=\s*Tool\.Input\(\s*properties\s*=\s*buildJsonObject\s*\{\s*)put\("type",\s*"object"\)\s*putJsonObject\("properties"\)\s*\{'

    if re.search(pattern, content):
        # Remove put("type", "object") and putJsonObject("properties") {
        new_content = re.sub(pattern, r'\1', content)

        # Now we need to find and remove the closing } for putJsonObject("properties")
        # This is tricky because we need to find the right closing brace

        # Find all positions where we have the pattern
        matches = list(re.finditer(pattern, content))
        if matches:
            # Process from the end to maintain correct positions
            for match in reversed(matches):
                start_pos = match.end()

                # Find the matching closing brace for putJsonObject("properties")
                # We need to count braces after putJsonObject("properties") {
                brace_count = 1
                i = start_pos
                while i < len(content) and brace_count > 0:
                    if content[i] == '{':
                        brace_count += 1
                    elif content[i] == '}':
                        brace_count -= 1
                    i += 1

                # Now we're at the position after the closing }, we need to remove it
                # But we need to be careful - there might be more content after
                # Let's look for the pattern: } followed by optional whitespace and }
                # Actually, let's look for: }\n followed by optional whitespace and }
                end_pattern_start = i

                # Look for the next non-whitespace/newline character
                while i < len(content) and content[i] in ' \n\t':
                    i += 1

                # Check if it's a closing brace (closing putJsonObject("properties"))
                if i < len(content) and content[i] == '}':
                    # This is the closing brace we want to remove
                    # Remove from end_pattern_start-1 (the }) to i (inclusive)
                    # But let's be more careful and only remove one }
                    pass

        # Let's try a different approach - use a more specific regex
        # Look for the pattern where we have properties closing followed by buildJsonObject closing
        pattern2 = r'(\s+)\}\s*\}\s*\)\s*,'

        # First let's count how many such patterns we have
        matches2 = list(re.finditer(pattern2, content))

        # Actually, let's be smarter - let's parse properly
        # Find inputSchema = Tool.Input(
        # Then find buildJsonObject {
        # Then remove put("type", "object") and putJsonObject("properties") {
        # And the corresponding }

    # Actually, let's use a simpler line-by-line approach
    lines = content.split('\n')
    new_lines = []
    skip_next_properties_close = False
    inside_input_schema = False
    brace_depth = 0
    removed_properties = False

    for i, line in enumerate(lines):
        # Track if we're inside inputSchema
        if 'inputSchema' in line and 'Tool.Input' in line:
            inside_input_schema = True
            brace_depth = 0

        if inside_input_schema:
            # Count braces to track depth
            brace_depth += line.count('{') - line.count('}')

            # Skip the line with put("type", "object")
            if 'put("type", "object")' in line:
                continue

            # Skip the line with putJsonObject("properties") {
            if 'putJsonObject("properties")' in line and not removed_properties:
                removed_properties = True
                skip_next_properties_close = True
                continue

            # Skip one closing } after we removed putJsonObject("properties")
            if skip_next_properties_close and line.strip() == '}':
                skip_next_properties_close = False
                continue

            if brace_depth <= 0:
                inside_input_schema = False
                removed_properties = False

        new_lines.append(line)

    new_content = '\n'.join(new_lines)

    # Write back if changed
    if new_content != content:
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(new_content)
        print(f"Fixed {os.path.basename(file_path)}")
        return True
    else:
        print(f"No changes needed for {os.path.basename(file_path)}")
        return False

def main():
    tools_dir = '/Users/jp17463/DoorayMCP/src/main/kotlin/com/bifos/dooray/mcp/tools'
    tool_files = glob.glob(os.path.join(tools_dir, '*Tool.kt'))

    fixed_count = 0
    for file_path in sorted(tool_files):
        if fix_tool_schema(file_path):
            fixed_count += 1

    print(f"\nFixed {fixed_count} files")

if __name__ == '__main__':
    main()
