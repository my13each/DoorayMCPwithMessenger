#!/usr/bin/env python3
"""
Properly fix the 34 tool schemas with correct transformation.
Based on the successful manual fixes of the other 11 files.
"""

import re
from pathlib import Path

def transform_tool_input(content):
    """
    Transform from OLD SDK 0.6.0 format:
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                putJsonObject("field1") { ... }
            },
            required = listOf("field1")
        ),

    To NEW SDK 0.7.7 format:
        inputSchema = Tool.Input(
                    properties = buildJsonObject {
                        put("type", JsonPrimitive("object"))

                        putJsonObject("properties") {
                            putJsonObject("field1") { ... }
                        }

                        putJsonArray("required") {
                            add(JsonPrimitive("field1"))
                        }
                    }
                ),
    """

    # Regex to match the entire Tool.Input block
    # Group 1: Leading whitespace/indent
    # Group 2: properties content (everything between buildJsonObject { and })
    # Group 3: required parameter (listOf(...) or emptyList())
    pattern = r'(\s+)inputSchema\s*=\s*Tool\.Input\(\s*properties\s*=\s*buildJsonObject\s*\{(.*?)\}\s*,\s*required\s*=\s*((?:listOf\([^)]*\)|emptyList\(\)))\s*\)'

    def replace_func(match):
        base_indent = match.group(1)
        properties_content = match.group(2)
        required_param = match.group(3)

        # Extract required fields from listOf("field1", "field2")
        required_fields = []
        if 'listOf' in required_param:
            fields_match = re.findall(r'"([^"]+)"', required_param)
            required_fields = fields_match

        # Build required array content
        if required_fields:
            required_lines = '\n'.join([
                f'{base_indent}                    add(JsonPrimitive("{field}"))'
                for field in required_fields
            ])
        else:
            required_lines = f'{base_indent}                    // No required parameters'

        # Build the new structure
        new_input = (
            f'{base_indent}inputSchema = Tool.Input(\n'
            f'{base_indent}                properties = buildJsonObject {{\n'
            f'{base_indent}                    put("type", JsonPrimitive("object"))\n'
            f'\n'
            f'{base_indent}                    putJsonObject("properties") {{\n'
            f'{properties_content}\n'
            f'{base_indent}                    }}\n'
            f'\n'
            f'{base_indent}                    putJsonArray("required") {{\n'
            f'{required_lines}\n'
            f'{base_indent}                    }}\n'
            f'{base_indent}                }}\n'
            f'{base_indent}            ),'
        )

        return new_input

    # Apply transformation
    new_content = re.sub(pattern, replace_func, content, flags=re.DOTALL)

    return new_content

def fix_imports(content):
    """Ensure kotlinx.serialization.json.* import"""
    # Check if wildcard import already exists
    if 'import kotlinx.serialization.json.*' in content:
        return content

    # Find and replace specific imports with wildcard
    import_pattern = r'import kotlinx\.serialization\.json\.(buildJsonObject|jsonPrimitive|put|putJsonObject|jsonArray|jsonObject)\n'

    # Remove all specific kotlinx.serialization.json imports
    content = re.sub(import_pattern, '', content)

    # Add wildcard import after Tool import
    if 'import io.modelcontextprotocol.kotlin.sdk.Tool\n' in content:
        content = content.replace(
            'import io.modelcontextprotocol.kotlin.sdk.Tool\n',
            'import io.modelcontextprotocol.kotlin.sdk.Tool\nimport kotlinx.serialization.json.*\n'
        )

    return content

def main():
    tools_dir = Path("/Users/jp17463/DoorayMCP/src/main/kotlin/com/bifos/dooray/mcp/tools")

    # The 34 files to fix
    files_to_fix = [
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
    print(f"Fixing 34 Tool Schemas - Correct Transformation")
    print(f"{'='*70}\n")

    fixed_count = 0
    skipped_count = 0
    error_count = 0

    for filename in files_to_fix:
        file_path = tools_dir / filename

        if not file_path.exists():
            print(f"❌ {filename}: File not found")
            error_count += 1
            continue

        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                original_content = f.read()

            # Apply transformations
            new_content = transform_tool_input(original_content)
            new_content = fix_imports(new_content)

            if new_content != original_content:
                with open(file_path, 'w', encoding='utf-8') as f:
                    f.write(new_content)
                print(f"✅ {filename}")
                fixed_count += 1
            else:
                print(f"⏭️  {filename}: No changes")
                skipped_count += 1

        except Exception as e:
            print(f"❌ {filename}: {e}")
            error_count += 1

    print(f"\n{'='*70}")
    print(f"Summary: Fixed {fixed_count}, Skipped {skipped_count}, Errors {error_count}")
    print(f"{'='*70}\n")

    return error_count == 0

if __name__ == "__main__":
    import sys
    success = main()
    sys.exit(0 if success else 1)
