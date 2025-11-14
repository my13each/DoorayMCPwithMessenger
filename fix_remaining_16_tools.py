#!/usr/bin/env python3
import re
import os

# List of files that need fixing (16 files without putJsonArray("required"))
files_to_fix = [
    "CopyFileTool.kt",
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

# Manual mapping of required fields for each tool
required_fields = {
    "CopyFileTool.kt": ["drive_id", "file_id", "destination_drive_id", "destination_folder_id"],
    "CreateFolderTool.kt": ["drive_id", "parent_id", "name"],
    "DeleteFileTool.kt": ["drive_id", "file_id"],
    "DownloadFileTool.kt": ["drive_id", "file_id"],
    "GetCalendarsTool.kt": [],  # No required fields
    "GetChannelsTool.kt": [],  # No required fields
    "GetDriveFilesTool.kt": ["drive_id"],
    "GetDrivesTool.kt": [],  # No required fields
    "GetFileMetadataTool.kt": ["drive_id", "file_id"],
    "GetProjectsTool.kt": [],  # No required fields
    "GetSimpleChannelsTool.kt": [],  # No required fields
    "GetWikisTool.kt": [],  # No required fields
    "MoveFileToTrashTool.kt": ["drive_id", "file_id"],
    "MoveFileTool.kt": ["drive_id", "file_id", "destination_folder_id"],
    "UpdateFileTool.kt": ["drive_id", "file_id"],
    "UploadFileTool.kt": ["drive_id", "parent_id", "file_name", "file_content"]
}

for filename in files_to_fix:
    filepath = os.path.join(base_path, filename)

    if not os.path.exists(filepath):
        print(f"⚠️  File not found: {filepath}")
        continue

    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    # Pattern to find the closing of properties block followed by closing of buildJsonObject
    # We need to insert the required array between them
    # Pattern: find "            }\n                }\n            }\n        ),"
    # This is the pattern where properties closes, buildJsonObject closes, Tool.Input closes, then Tool closes

    pattern = r'(putJsonObject\("properties"\) \{[^}]*?)(            \})\s+(\})\s+(\})\s+(\),)'

    # More flexible pattern
    pattern = r'(putJsonObject\("properties"\) \{.*?)(^\s{12}\}\s*$)(^\s{16}\}\s*$)(^\s{12}\}\s*$)(^\s{8}\),)'

    # Even simpler: just find the pattern of closing braces at end of inputSchema
    # Look for: properties block end, then 3 closing braces
    old_pattern = re.compile(
        r'(putJsonObject\("properties"\) \{[^}]+?)\n(\s{12}\}\n)(\s{16}\}\n)(\s{12}\}\n)(\s{8}\),)',
        re.MULTILINE | re.DOTALL
    )

    required_list = required_fields.get(filename, [])

    if required_list:
        # Build the required array
        required_array = '                putJsonArray("required") {\n'
        for field in required_list:
            required_array += f'                    add("{field}")\n'
        required_array += '                }\n'

        # New content: properties block + its closing brace + required array + remaining closing braces
        replacement = r'\1\n\2' + required_array + r'\3\4\5'
    else:
        # No required fields, but still need to add empty required array for schema compliance
        # Actually, if there are no required fields, we don't need the array at all
        # Just keep the structure as is - the schema is valid without required array if all fields are optional
        print(f"ℹ️  {filename}: No required fields, structure is correct")
        continue

    new_content = old_pattern.sub(replacement, content)

    if new_content != content:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(new_content)
        print(f"✅ Fixed: {filename}")
    else:
        print(f"⚠️  No match found in: {filename}")

print("\n✅ Script completed!")
