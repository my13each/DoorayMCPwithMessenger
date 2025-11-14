#!/usr/bin/env python3
"""
Comprehensive script to fix Tool.Input double-nesting in all remaining tool files.
"""

import re

def fix_tool_file(filepath):
    """Fix a single tool file"""
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    original_content = content

    # Check if this file needs fixing
    if 'put("type", "object")' not in content:
        return False, "Already fixed or doesn't need fixing"

    # Pattern 1: Remove `put("type", "object")` line
    content = re.sub(r'(\s+)put\("type",\s*"object"\)\s*\n', '', content)

    # Pattern 2: Remove `putJsonObject("properties") {` line
    content = re.sub(r'(\s+)putJsonObject\("properties"\)\s*\{\s*\n', '', content)

    # Pattern 3: Remove extra closing brace before putJsonArray("required")
    # This is the } that closes putJsonObject("properties")
    content = re.sub(r'(\s+)\}\s*\n(\s+)putJsonArray\("required"\)', r'\2putJsonArray("required")', content)

    # Pattern 4: Remove extra closing brace before }) at end of inputSchema (for files without putJsonArray)
    # Match: spaces + } + newline + spaces + } + ) + ),
    content = re.sub(r'(\s+)\}\s*\n(\s+)\}\s*(\)\s*\),?\s*\n\s*outputSchema)', r'\2}\3', content)

    if content != original_content:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)
        return True, "Fixed"

    return False, "No changes made (unexpected pattern)"

def main():
    files_to_fix = [
        "/Users/jp17463/DoorayMCP/src/main/kotlin/com/bifos/dooray/mcp/tools/GetProjectPostsTool.kt",
        "/Users/jp17463/DoorayMCP/src/main/kotlin/com/bifos/dooray/mcp/tools/GetProjectPostTool.kt",
        "/Users/jp17463/DoorayMCP/src/main/kotlin/com/bifos/dooray/mcp/tools/GetSharedLinkDetailTool.kt",
        "/Users/jp17463/DoorayMCP/src/main/kotlin/com/bifos/dooray/mcp/tools/GetSharedLinksTool.kt",
        "/Users/jp17463/DoorayMCP/src/main/kotlin/com/bifos/dooray/mcp/tools/GetSimpleChannelsTool.kt",
        "/Users/jp17463/DoorayMCP/src/main/kotlin/com/bifos/dooray/mcp/tools/GetWikiPagesTool.kt",
        "/Users/jp17463/DoorayMCP/src/main/kotlin/com/bifos/dooray/mcp/tools/GetWikiPageTool.kt",
        "/Users/jp17463/DoorayMCP/src/main/kotlin/com/bifos/dooray/mcp/tools/MoveFileToTrashTool.kt",
        "/Users/jp17463/DoorayMCP/src/main/kotlin/com/bifos/dooray/mcp/tools/SendChannelMessageTool.kt",
        "/Users/jp17463/DoorayMCP/src/main/kotlin/com/bifos/dooray/mcp/tools/SendDirectMessageTool.kt",
        "/Users/jp17463/DoorayMCP/src/main/kotlin/com/bifos/dooray/mcp/tools/SetProjectPostDoneTool.kt",
        "/Users/jp17463/DoorayMCP/src/main/kotlin/com/bifos/dooray/mcp/tools/SetProjectPostWorkflowTool.kt",
        "/Users/jp17463/DoorayMCP/src/main/kotlin/com/bifos/dooray/mcp/tools/UpdateFileTool.kt",
        "/Users/jp17463/DoorayMCP/src/main/kotlin/com/bifos/dooray/mcp/tools/UpdatePostCommentTool.kt",
        "/Users/jp17463/DoorayMCP/src/main/kotlin/com/bifos/dooray/mcp/tools/UpdateProjectPostTool.kt",
        "/Users/jp17463/DoorayMCP/src/main/kotlin/com/bifos/dooray/mcp/tools/UpdateSharedLinkTool.kt",
        "/Users/jp17463/DoorayMCP/src/main/kotlin/com/bifos/dooray/mcp/tools/UpdateWikiPageTool.kt",
        "/Users/jp17463/DoorayMCP/src/main/kotlin/com/bifos/dooray/mcp/tools/UploadFileFromPathTool.kt",
        "/Users/jp17463/DoorayMCP/src/main/kotlin/com/bifos/dooray/mcp/tools/UploadFileTool.kt",
    ]

    fixed = []
    failed = []

    for filepath in files_to_fix:
        try:
            was_fixed, message = fix_tool_file(filepath)
            if was_fixed:
                print(f"✓ {filepath.split('/')[-1]}: {message}")
                fixed.append(filepath)
            else:
                print(f"- {filepath.split('/')[-1]}: {message}")
        except Exception as e:
            print(f"✗ {filepath.split('/')[-1]}: Error - {e}")
            failed.append((filepath, str(e)))

    print(f"\n=== Summary ===")
    print(f"Fixed: {len(fixed)}")
    print(f"Failed: {len(failed)}")

    if failed:
        print("\nFailed files:")
        for filepath, error in failed:
            print(f"  - {filepath.split('/')[-1]}: {error}")

    return 0 if len(failed) == 0 else 1

if __name__ == "__main__":
    exit(main())
