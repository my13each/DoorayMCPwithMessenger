#!/usr/bin/env python3
import os
import glob
import re

def fix_tool_file(file_path):
    """Fix the JSON schema in a Kotlin Tool file"""
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()

    original_content = content

    # Skip if explicitly excluded
    basename = os.path.basename(file_path)
    if basename in ['GetWikisTool.kt', 'UpdateWikiPageTool.kt']:
        print(f"Skipping {basename} - explicitly excluded")
        return False

    # Pattern: Find inputSchema = Tool.Input( ... properties = buildJsonObject {
    # Then within that, find put("type", "object") followed by putJsonObject("properties") {
    # We want to remove these two lines and one matching closing brace

    # Use a regex to find and replace the pattern
    # This pattern matches:
    # - put("type", "object")
    # - optional whitespace/newlines
    # - putJsonObject("properties") {
    pattern = r'(\s+)put\("type",\s*"object"\)\s*\n\s*putJsonObject\("properties"\)\s*\{\s*\n'

    if re.search(pattern, content):
        # Remove put("type", "object") and putJsonObject("properties") {
        content = re.sub(pattern, r'\1', content)

        # Now find and remove the extra closing brace
        # Look for a lone } on a line that closes the removed putJsonObject("properties")
        # This is tricky - we need to find the right closing brace

        # Let's be more careful - find where properties closing happens before })
        # Pattern: Look for }\n followed by spaces/tabs and }\n followed by })
        # Actually, simpler: after we remove putJsonObject("properties") {, we need to remove one }
        # that appears before the final })

        #Let me try a different approach - find } } pattern where first } closes properties
        # and second } closes buildJsonObject
        pattern2 = r'(\s+)\}\s*\n(\s+)\}\s*\n(\s*)\)\s*,'

        # Count closing braces - if we find a sequence like  }\n  }\n), we can remove one }
        # But only if it's in the inputSchema section

        # Actually, let's be specific: look for a} followed by whitespace, then another }, then )
        # where the line with first } is at a specific indentation level
        # Look for:  "    }" (16 spaces) or "                }" followed by "            }" followed by "),"

        # Let's try to match the exact pattern from the files:
        #                 }
        #             }),
        # vs correct:
        #             }),

        # So we want to remove one } that appears right before }),

        # Match: (spaces)}(newline)(spaces)}(newline)(spaces)),
        pattern_close = r'(\s+)\}(\s*\n\s+)\}\s*\n(\s*)\)\s*,'

        # Check if this pattern exists
        if re.search(pattern_close, content):
            # Remove the first } in this pattern
            content = re.sub(pattern_close, r'\2}\n\3),', content)

        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(content)

        print(f"Fixed {basename}")
        return True
    else:
        return False

def main():
    tools_dir = '/Users/jp17463/DoorayMCP/src/main/kotlin/com/bifos/dooray/mcp/tools'
    tool_files = glob.glob(os.path.join(tools_dir, '*Tool.kt'))

    fixed_count = 0
    for file_path in sorted(tool_files):
        if fix_tool_file(file_path):
            fixed_count += 1

    print(f"\nTotal fixed: {fixed_count} files")

if __name__ == '__main__':
    main()
