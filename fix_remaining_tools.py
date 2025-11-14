#!/usr/bin/env python3
"""
Script to automatically fix the Tool.Input double-nesting issue in remaining tool files.
"""

import re
import sys

def fix_tool_schema(file_path):
    """Fix the schema in a single file"""
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()

    # Pattern to match the problematic structure
    # Look for: inputSchema = Tool.Input(\n            properties = buildJsonObject {\n                put("type", "object")\n                putJsonObject("properties") {

    # Find the inputSchema block
    pattern = r'(inputSchema\s*=\s*Tool\.Input\(\s*properties\s*=\s*buildJsonObject\s*\{\s*)put\("type",\s*"object"\)\s*putJsonObject\("properties"\)\s*\{'

    if re.search(pattern, content):
        # Remove the double nesting lines
        content = re.sub(pattern, r'\1', content)

        # Now we need to find and remove the corresponding closing brace before putJsonArray or })
        # This is trickier - we need to find the last } before putJsonArray("required") or before })\s*,?\s*outputSchema

        # Strategy: Find inputSchema block, then look for the extra } before putJsonArray or })
        # We'll do this in multiple passes

        # First pass: already done above (removed opening lines)

        # Second pass: Find and remove the extra } before putJsonArray("required")
        # Pattern: }\s*}\s*putJsonArray("required") -> }\s*putJsonArray("required")
        content = re.sub(r'(\s+)\}\s*\}\s*(putJsonArray\("required"\))', r'\1\2', content)

        # Third pass: Find and remove extra } before }) at end of inputSchema
        # Pattern: }\s*}\s*\)\) -> }\s*\)\)
        content = re.sub(r'(\s+)\}\s*\}\s*(\)\s*\),\s*outputSchema)', r'\1}\2', content)

        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(content)

        return True

    return False

def main():
    files = [
        "/Users/jp17463/DoorayMCP/src/main/kotlin/com/bifos/dooray/mcp/tools/GetPostCommentsTool.kt",
        "/Users/jp17463/DoorayMCP/src/main/kotlin/com/bifos/dooray/mcp/tools/GetProjectPostsTool.kt",
        "/Users/jp17463/DoorayMCP/src/main/kotlin/com/bifos/dooray/mcp/tools/GetProjectPostTool.kt",
        "/Users/jp17463/DoorayMCP/src/main/kotlin/com/bifos/dooray/mcp/tools/GetProjectsTool.kt",
        "/Users/jp17463/DoorayMCP/src/main/kotlin/com/bifos/dooray/mcp/tools/GetSharedLinkDetailTool.kt",
        "/Users/jp17463/DoorayMCP/src/main/kotlin/com/bifos/dooray/mcp/tools/GetSharedLinksTool.kt",
        "/Users/jp17463/DoorayMCP/src/main/kotlin/com/bifos/dooray/mcp/tools/GetSimpleChannelsTool.kt",
        "/Users/jp17463/DoorayMCP/src/main/kotlin/com/bifos/dooray/mcp/tools/GetWikiPagesTool.kt",
        "/Users/jp17463/DoorayMCP/src/main/kotlin/com/bifos/dooray/mcp/tools/GetWikiPageTool.kt",
        "/Users/jp17463/DoorayMCP/src/main/kotlin/com/bifos/dooray/mcp/tools/MoveFileTool.kt",
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
    for file_path in files:
        try:
            if fix_tool_schema(file_path):
                print(f"Fixed: {file_path}")
                fixed.append(file_path)
            else:
                print(f"No changes needed: {file_path}")
        except Exception as e:
            print(f"Error processing {file_path}: {e}", file=sys.stderr)

    print(f"\nTotal files fixed: {len(fixed)}")
    return 0 if len(fixed) > 0 else 1

if __name__ == "__main__":
    sys.exit(main())
