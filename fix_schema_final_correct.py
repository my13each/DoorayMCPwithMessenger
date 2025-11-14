#!/usr/bin/env python3
"""
Final correct fix for Tool.Input double-nesting.
Removes EXACTLY 3 lines:
1. put("type", "object")
2. putJsonObject("properties") {
3. The } before putJsonArray("required") or before })
"""

import re

files_to_fix = [
    "GetProjectPostsTool.kt",
    "GetSharedLinkDetailTool.kt",
    "GetSharedLinksTool.kt",
    "GetWikiPagesTool.kt",
    "SetProjectPostDoneTool.kt",
    "SetProjectPostWorkflowTool.kt",
    "UpdateProjectPostTool.kt",
    "UpdateSharedLinkTool.kt",
    "UpdateWikiPageTool.kt",
    "UploadFileFromPathTool.kt",
]

base_path = "src/main/kotlin/com/bifos/dooray/mcp/tools/"

for filename in files_to_fix:
    filepath = base_path + filename

    with open(filepath, 'r', encoding='utf-8') as f:
        lines = f.readlines()

    new_lines = []
    i = 0
    while i < len(lines):
        line = lines[i]

        # Skip put("type", "object") lines
        if 'put("type", "object")' in line and 'putJsonObject("properties")' in lines[i+1] if i+1 < len(lines) else False:
            i += 1  # Skip this line
            continue

        # Skip putJsonObject("properties") { lines
        if 'putJsonObject("properties") {' in line:
            i += 1  # Skip this line
            continue

        # Check if this is a } before putJsonArray("required")
        if i+1 < len(lines) and line.strip() == '}' and 'putJsonArray("required")' in lines[i+1]:
            # Skip this closing brace
            i += 1
            continue

        # Otherwise keep the line
        new_lines.append(line)
        i += 1

    with open(filepath, 'w', encoding='utf-8') as f:
        f.writelines(new_lines)

    print(f"✓ Fixed {filename}")

print(f"\n=== Summary ===")
print(f"Fixed {len(files_to_fix)} files")
