#!/usr/bin/env python3
"""
Fix the remaining 34 tool schemas that still have the old SDK 0.6.0 format.
Transforms from old format to JSON Schema draft 2020-12 compliant format for SDK 0.7.7.
"""

import re
from pathlib import Path

def transform_to_new_format(content):
    """
    Transform from OLD format:
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                putJsonObject("field1") { ... }
                ...
            },
            required = listOf("field1") OR required = emptyList()
        ),

    To NEW format:
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                put("type", JsonPrimitive("object"))
                putJsonObject("properties") {
                    putJsonObject("field1") { ... }
                    ...
                }
                putJsonArray("required") {
                    add(JsonPrimitive("field1"))
                }
            }
        ),
    """

    # Pattern to match old format inputSchema blocks
    # Captures: (indent)(properties block)(required clause)
    pattern = r'(\s+)(inputSchema\s*=\s*Tool\.Input\(\s*)(properties\s*=\s*buildJsonObject\s*\{)(.*?)(\}\s*,?\s*)(required\s*=\s*(?:listOf\(([^)]*)\)|emptyList\(\)))\s*(\))'

    def replace_block(match):
        base_indent = match.group(1)
        input_start = match.group(2)
        props_start = match.group(3)
        properties_content = match.group(4)
        props_end = match.group(5)
        required_clause = match.group(6)

        # Extract required fields
        required_fields = []
        if required_clause:
            # Extract fields from listOf("field1", "field2", ...)
            fields = re.findall(r'"([^"]+)"', required_clause)
            required_fields = fields

        # Build required array
        if required_fields:
            required_items = '\n'.join([
                f'{base_indent}                add(JsonPrimitive("{field}"))'
                for field in required_fields
            ])
        else:
            required_items = f'{base_indent}                // No required parameters'

        # Build new format
        new_block = (
            f'{base_indent}{input_start}{props_start}\n'
            f'{base_indent}            put("type", JsonPrimitive("object"))\n'
            f'\n'
            f'{base_indent}            putJsonObject("properties") {{\n'
            f'{properties_content}{props_end}\n'
            f'\n'
            f'{base_indent}            putJsonArray("required") {{\n'
            f'{required_items}\n'
            f'{base_indent}            }}\n'
            f'{base_indent}        }}\n'
            f'{base_indent}    )'
        )

        return new_block

    # Apply transformation
    new_content = re.sub(pattern, replace_block, content, flags=re.DOTALL)

    return new_content

def fix_imports(content):
    """Ensure proper imports"""
    if 'import kotlinx.serialization.json.*' not in content:
        # Replace specific imports with wildcard
        patterns = [
            r'import kotlinx\.serialization\.json\.buildJsonObject\s*\n',
            r'import kotlinx\.serialization\.json\.jsonPrimitive\s*\n',
            r'import kotlinx\.serialization\.json\.put\s*\n',
            r'import kotlinx\.serialization\.json\.putJsonObject\s*\n',
            r'import kotlinx\.serialization\.json\.jsonArray\s*\n',
            r'import kotlinx\.serialization\.json\.jsonObject\s*\n',
        ]

        for pattern in patterns:
            content = re.sub(pattern, '', content)

        # Add wildcard import after the package and other imports
        if 'import io.modelcontextprotocol.kotlin.sdk.Tool' in content:
            content = content.replace(
                'import io.modelcontextprotocol.kotlin.sdk.Tool',
                'import io.modelcontextprotocol.kotlin.sdk.Tool\nimport kotlinx.serialization.json.*'
            )

    return content

def main():
    tools_dir = Path("/Users/jp17463/DoorayMCP/src/main/kotlin/com/bifos/dooray/mcp/tools")

    # List of 34 files that failed validation
    failing_tools = [
        "GetProjectPostsTool.kt",
        "CreateProjectPostTool.kt",
        "SetProjectPostWorkflowTool.kt",
        "UpdateProjectPostTool.kt",
        "CreatePostCommentTool.kt",
        "GetPostCommentsTool.kt",
        "UpdatePostCommentTool.kt",
        "DeletePostCommentTool.kt",
        "SearchMembersTool.kt",
        "SendDirectMessageTool.kt",
        "GetSimpleChannelsTool.kt",
        "GetChannelTool.kt",
        "SendChannelMessageTool.kt",
        "GetCalendarDetailTool.kt",
        "GetCalendarEventsTool.kt",
        "GetCalendarEventDetailTool.kt",
        "CreateCalendarEventTool.kt",
        "GetDrivesTool.kt",
        "GetDriveFilesTool.kt",
        "UploadFileFromPathTool.kt",
        "UploadFileTool.kt",
        "DownloadFileTool.kt",
        "GetFileMetadataTool.kt",
        "UpdateFileTool.kt",
        "MoveFileToTrashTool.kt",
        "DeleteFileTool.kt",
        "CreateFolderTool.kt",
        "CopyFileTool.kt",
        "MoveFileTool.kt",
        "CreateSharedLinkTool.kt",
        "GetSharedLinksTool.kt",
        "GetSharedLinkDetailTool.kt",
        "UpdateSharedLinkTool.kt",
        "DeleteSharedLinkTool.kt",
    ]

    print(f"\n{'='*70}")
    print(f"Fixing Remaining 34 Tool Schemas")
    print(f"{'='*70}\n")

    fixed_count = 0
    error_count = 0

    for filename in failing_tools:
        file_path = tools_dir / filename

        if not file_path.exists():
            print(f"❌ {filename}: File not found")
            error_count += 1
            continue

        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                original_content = f.read()

            # Apply transformations
            new_content = transform_to_new_format(original_content)
            new_content = fix_imports(new_content)

            if new_content != original_content:
                with open(file_path, 'w', encoding='utf-8') as f:
                    f.write(new_content)
                print(f"✅ {filename}: Fixed")
                fixed_count += 1
            else:
                print(f"⏭️  {filename}: No changes needed")

        except Exception as e:
            print(f"❌ {filename}: Error - {e}")
            error_count += 1

    print(f"\n{'='*70}")
    print(f"Summary:")
    print(f"  ✅ Fixed: {fixed_count}")
    print(f"  ❌ Errors: {error_count}")
    print(f"  📊 Total: {len(failing_tools)}")
    print(f"{'='*70}\n")

    return fixed_count > 0

if __name__ == "__main__":
    import sys
    success = main()
    sys.exit(0 if success else 1)
