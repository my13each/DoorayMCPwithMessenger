#!/usr/bin/env python3
"""
Fix remaining 31 tools with the verified pattern.
Based on successful manual fixes: GetDrivesTool, SearchMembersTool, GetSimpleChannelsTool
"""

import re
from pathlib import Path

def count_braces(text):
    """Count net open braces to find matching closing brace"""
    count = 0
    for char in text:
        if char == '{':
            count += 1
        elif char == '}':
            count -= 1
    return count

def find_matching_brace(content, start_pos):
    """Find the position of the matching closing brace"""
    count = 1  # We start after the opening brace
    pos = start_pos
    while pos < len(content) and count > 0:
        if content[pos] == '{':
            count += 1
        elif content[pos] == '}':
            count -= 1
        pos += 1
    return pos - 1 if count == 0 else -1

def transform_input_schema(content):
    """
    Transform Tool.Input from old format to new format.

    Pattern:
    OLD:
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                CONTENT_HERE
            },
            required = listOf(...) OR emptyList()
        ),

    NEW:
        inputSchema = Tool.Input(
                    properties = buildJsonObject {
                        put("type", JsonPrimitive("object"))

                        putJsonObject("properties") {
                            CONTENT_HERE
                        }

                        putJsonArray("required") {
                            add(JsonPrimitive("field1"))
                            OR
                            // No required parameters
                        }
                    }
                ),
    """

    # Find inputSchema = Tool.Input(
    input_pattern = r'(\s+)(inputSchema\s*=\s*Tool\.Input\s*\(\s*)'
    match = re.search(input_pattern, content)

    if not match:
        return content, False

    base_indent = match.group(1)
    input_start = match.group(2)
    start_pos = match.end()

    # Find properties = buildJsonObject {
    props_pattern = r'properties\s*=\s*buildJsonObject\s*\{'
    props_match = re.search(props_pattern, content[start_pos:])

    if not props_match:
        return content, False

    # Find the matching closing brace for buildJsonObject
    brace_start = start_pos + props_match.end()
    brace_end = find_matching_brace(content, brace_start)

    if brace_end == -1:
        return content, False

    # Extract properties content
    properties_content = content[brace_start:brace_end]

    # Find required parameter after the closing brace
    after_brace = content[brace_end:]
    required_pattern = r'\s*\},\s*required\s*=\s*(listOf\([^)]*\)|emptyList\(\))\s*\)'
    required_match = re.search(required_pattern, after_brace)

    if not required_match:
        return content, False

    required_value = required_match.group(1)

    # Extract required fields
    required_fields = []
    if 'listOf' in required_value:
        fields = re.findall(r'"([^"]+)"', required_value)
        required_fields = fields

    # Build required array content
    if required_fields:
        required_lines = '\n'.join([
            f'{base_indent}                        add(JsonPrimitive("{field}"))'
            for field in required_fields
        ])
    else:
        required_lines = f'{base_indent}                        // No required parameters'

    # Build new schema
    new_schema = (
        f'{base_indent}{input_start}\n'
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
        f'{base_indent}            )'
    )

    # Find the end position of the old schema
    old_end = brace_end + required_match.end()

    # Replace the old schema with new schema
    new_content = content[:match.start()] + new_schema + content[old_end:]

    return new_content, True

def main():
    tools_dir = Path("/Users/jp17463/DoorayMCP/src/main/kotlin/com/bifos/dooray/mcp/tools")

    # All 31 remaining files
    files_to_fix = [
        "GetProjectPostsTool.kt",
        "CreateProjectPostTool.kt",
        "SetProjectPostWorkflowTool.kt",
        "UpdateProjectPostTool.kt",
        "CreatePostCommentTool.kt",
        "GetPostCommentsTool.kt",
        "UpdatePostCommentTool.kt",
        "DeletePostCommentTool.kt",
        "SendDirectMessageTool.kt",
        "GetChannelTool.kt",
        "SendChannelMessageTool.kt",
        "GetCalendarDetailTool.kt",
        "GetCalendarEventsTool.kt",
        "GetCalendarEventDetailTool.kt",
        "CreateCalendarEventTool.kt",
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
    print(f"Fixing 31 Remaining Tools - Final Verified Pattern")
    print(f"{'='*70}\n")

    fixed_count = 0
    skipped_count = 0
    error_count = 0

    for filename in files_to_fix:
        file_path = tools_dir / filename

        if not file_path.exists():
            print(f"❌ {filename}: Not found")
            error_count += 1
            continue

        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                original = f.read()

            new_content, changed = transform_input_schema(original)

            if changed:
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
    print(f"Total: {len(files_to_fix)}")
    print(f"{'='*70}\n")

    return error_count == 0

if __name__ == "__main__":
    import sys
    success = main()
    sys.exit(0 if success else 1)
