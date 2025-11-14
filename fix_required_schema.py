#!/usr/bin/env python3
"""
Fix Tool.Input schema by moving 'required' array from properties to Tool.Input constructor parameter.
"""
import re
import sys
from pathlib import Path

def fix_tool_file(file_path: Path) -> bool:
    """Fix a single tool file. Returns True if modified."""
    content = file_path.read_text()
    original_content = content

    # Pattern to match the putJsonArray("required") block and extract required fields
    # This pattern needs to handle multi-line required blocks
    pattern = r'(\s+)putJsonArray\("required"\)\s*\{([^}]+)\}'

    matches = list(re.finditer(pattern, content))
    if not matches:
        return False

    for match in reversed(matches):  # Process in reverse to maintain positions
        indent = match.group(1)
        required_block = match.group(2)

        # Extract all required field names from add(JsonPrimitive("...")) statements
        field_pattern = r'add\(JsonPrimitive\("([^"]+)"\)\)'
        fields = re.findall(field_pattern, required_block)

        if not fields:
            continue

        # Create the required list parameter
        required_list = ', '.join(f'"{field}"' for field in fields)
        required_param = f',\n{indent}required = listOf({required_list})'

        # Remove the putJsonArray("required") block
        content = content[:match.start()] + content[match.end():]

        # Find the closing of Tool.Input(...) and add required parameter before it
        # Look for }),\n or }\n) pattern after the match position
        # We need to find the closing of buildJsonObject { ... }
        # Pattern: look for the next "})," or "})" after the properties block

        # Find the next }),  after where we removed the required block
        # This should be the end of buildJsonObject
        search_start = match.start()

        # Look for the pattern: "}\n" + indent + ")," which closes buildJsonObject and Tool.Input properties
        closing_pattern = r'(\n' + re.escape(indent[:-4]) + r'\}\n' + re.escape(indent[:-4]) + r'\))(,)'
        closing_match = re.search(closing_pattern, content[search_start:search_start+500])

        if closing_match:
            # Insert before the ")," that closes Tool.Input properties parameter
            insert_pos = search_start + closing_match.start(2)
            content = content[:insert_pos] + required_param + '\n' + indent[:-4] + content[insert_pos:]
        else:
            print(f"Warning: Could not find closing pattern in {file_path}")
            continue

    if content != original_content:
        file_path.write_text(content)
        return True

    return False


def main():
    tools_dir = Path(__file__).parent / "src/main/kotlin/com/bifos/dooray/mcp/tools"

    if not tools_dir.exists():
        print(f"Error: Tools directory not found: {tools_dir}")
        sys.exit(1)

    # Get all tool files that contain putJsonArray("required")
    import subprocess
    result = subprocess.run(
        ['grep', '-l', 'putJsonArray("required")', str(tools_dir / '*.kt')],
        shell=True,
        capture_output=True,
        text=True
    )

    tool_files = [
        tools_dir / f"{name}.kt"
        for name in [
            "CreateCalendarEventTool",
            "CreateChannelTool",
            "CreatePostCommentTool",
            "CreateProjectPostTool",
            "CreateSharedLinkTool",
            "CreateWikiPageTool",
            "DeletePostCommentTool",
            "DeleteSharedLinkTool",
            "GetCalendarDetailTool",
            "GetCalendarEventDetailTool",
            "GetCalendarEventsTool",
            "GetChannelTool",
            "GetPostCommentsTool",
            "GetProjectPostsTool",
            "GetProjectPostTool",
            "GetSharedLinkDetailTool",
            "GetSharedLinksTool",
            "GetWikiPagesTool",
            "GetWikiPageTool",
            "SearchMembersTool",
            "SendChannelMessageTool",
            "SetProjectPostDoneTool",
            "SetProjectPostWorkflowTool",
            "UpdatePostCommentTool",
            "UpdateProjectPostTool",
            "UpdateSharedLinkTool",
            "UpdateWikiPageTool",
            "UploadFileFromPathTool",
        ]
    ]

    modified_count = 0
    for tool_file in tool_files:
        if tool_file.exists():
            if fix_tool_file(tool_file):
                print(f"✓ Fixed: {tool_file.name}")
                modified_count += 1
            else:
                print(f"- Skipped: {tool_file.name} (no changes needed)")
        else:
            print(f"✗ Not found: {tool_file.name}")

    print(f"\nModified {modified_count} files")


if __name__ == "__main__":
    main()
