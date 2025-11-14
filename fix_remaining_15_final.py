#!/usr/bin/env python3
import re
import os

files_to_fix = [
    "CreateFolderTool.kt",
    "DeleteFileTool.kt",
    "DownloadFileTool.kt",
    "GetCalendarsTool.kt",
    "GetChannelsTool.kt",
    "GetDriveFilesTool.kt",
    "GetDrivesTool.kt",
    "GetFileMetadataTool.kt",
    "GetProjectsTool.kt",
    "GetSimpleChannelsTool.kt",
    "GetWikisTool.kt",
    "MoveFileToTrashTool.kt",
    "MoveFileTool.kt",
    "UpdateFileTool.kt",
    "UploadFileTool.kt"
]

base_path = "/Users/jp17463/DoorayMCP/src/main/kotlin/com/bifos/dooray/mcp/tools/"

def extract_required_fields(content):
    """Extract required field names from MISSING_ error codes"""
    required = []
    # Pattern: code = "MISSING_FIELD_NAME"
    pattern = r'code\s*=\s*"MISSING_(\w+)"'
    matches = re.findall(pattern, content)
    for match in matches:
        # Convert DRIVE_ID to drive_id
        field_name = match.lower()
        if field_name not in required:
            required.append(field_name)
    return required

def fix_tool_file(filepath):
    """Fix a single tool file by adding required array"""
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    # Extract required fields
    required_fields = extract_required_fields(content)

    # Pattern to match the closing of properties block
    # We're looking for:
    #     }            <-- closes last field's putJsonObject
    #                 }            <-- closes putJsonObject("properties")
    #             }            <-- closes buildJsonObject
    #         ),           <-- closes Tool.Input

    # Find the pattern after the last putJsonObject inside properties
    pattern = re.compile(
        r'(putJsonObject\("[^"]+"\) \{[^\}]*\})\n(\s{12}\}\n)(\s{16}\}\n)(\s{12}\}\n)(\s{8}\),)',
        re.MULTILINE
    )

    # Simpler pattern - find end of properties block
    pattern = re.compile(
        r'(putJsonObject\("properties"\) \{(?:.*?))(\n\s{12}\}\n)(\s{16}\}\n)(\s{12}\}\n)(\s{8}\),)',
        re.MULTILINE | re.DOTALL
    )

    if required_fields:
        # Build required array
        required_str = '                putJsonArray("required") {\n'
        for field in required_fields:
            required_str += f'                    add("{field}")\n'
        required_str += '                }\n'

        # Replace: properties block + close properties + insert required + rest
        replacement = r'\1\2' + required_str + r'\3\4\5'
        new_content = pattern.sub(replacement, content)
    else:
        # No required fields - structure is OK as is
        return None

    if new_content != content:
        return new_content
    else:
        return None

for filename in files_to_fix:
    filepath = os.path.join(base_path, filename)

    if not os.path.exists(filepath):
        print(f"⚠️  Not found: {filename}")
        continue

    try:
        new_content = fix_tool_file(filepath)
        if new_content:
            with open(filepath, 'w', encoding='utf-8') as f:
                f.write(new_content)
            print(f"✅ Fixed: {filename}")
        else:
            print(f"ℹ️  No changes: {filename}")
    except Exception as e:
        print(f"❌ Error in {filename}: {e}")

print("\n✅ All files processed!")
