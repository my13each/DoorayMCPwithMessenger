#!/usr/bin/env python3
import re

files = [
    "/Users/jp17463/DoorayMCP/src/main/kotlin/com/bifos/dooray/mcp/tools/GetProjectPostsTool.kt",
    "/Users/jp17463/DoorayMCP/src/main/kotlin/com/bifos/dooray/mcp/tools/GetSharedLinkDetailTool.kt",
    "/Users/jp17463/DoorayMCP/src/main/kotlin/com/bifos/dooray/mcp/tools/GetSharedLinksTool.kt",
    "/Users/jp17463/DoorayMCP/src/main/kotlin/com/bifos/dooray/mcp/tools/GetWikiPagesTool.kt",
    "/Users/jp17463/DoorayMCP/src/main/kotlin/com/bifos/dooray/mcp/tools/SetProjectPostDoneTool.kt",
    "/Users/jp17463/DoorayMCP/src/main/kotlin/com/bifos/dooray/mcp/tools/SetProjectPostWorkflowTool.kt",
    "/Users/jp17463/DoorayMCP/src/main/kotlin/com/bifos/dooray/mcp/tools/UpdateProjectPostTool.kt",
    "/Users/jp17463/DoorayMCP/src/main/kotlin/com/bifos/dooray/mcp/tools/UpdateSharedLinkTool.kt",
    "/Users/jp17463/DoorayMCP/src/main/kotlin/com/bifos/dooray/mcp/tools/UpdateWikiPageTool.kt",
    "/Users/jp17463/DoorayMCP/src/main/kotlin/com/bifos/dooray/mcp/tools/UploadFileFromPathTool.kt",
]

for filepath in files:
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    # Remove put("type", "object")
    content = re.sub(r'(\s+)put\("type",\s*"object"\)\s*\n', '', content)

    # Remove putJsonObject("properties") {
    content = re.sub(r'(\s+)putJsonObject\("properties"\)\s*\{\s*\n', '', content)

    # Remove extra } before putJsonArray("required")
    # Match: whitespace + } + newline + whitespace + putJsonArray("required")
    # Replace with: whitespace + putJsonArray("required")
    content = re.sub(r'(\s+)\}\s*\n(\s+)(putJsonArray\("required"\))', r'\2\3', content)

    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)

    print(f"Fixed: {filepath.split('/')[-1]}")

print("\nAll files fixed!")
