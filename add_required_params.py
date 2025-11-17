#!/usr/bin/env python3
import re
from pathlib import Path

def extract_required_fields(content):
    """Extract required field names from Tool.Input properties"""
    required_fields = []
    
    # Find all putJsonObject blocks
    pattern = r'putJsonObject\("([^"]+)"\)\s*\{([^}]+)\}'
    matches = re.findall(pattern, content, re.DOTALL)
    
    for field_name, field_content in matches:
        # Check if description mentions "(필수)" or has no "default" or "선택"
        if '(필수)' in field_content or '필수' in field_content:
            required_fields.append(field_name)
        elif 'default' not in field_content.lower() and '선택' not in field_content:
            # Might be required, but be conservative
            pass
    
    return required_fields

def fix_file(file_path):
    """Add required parameter to Tool.Input"""
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Check if already has required
    if re.search(r'required\s*=', content):
        return False, "Already has required"
    
    # Extract required fields
    required_fields = extract_required_fields(content)
    
    # Find the Tool.Input closing
    # Pattern: properties = buildJsonObject { ... }\n        )
    pattern = r'(properties\s*=\s*buildJsonObject\s*\{[^}]*\})\s*\)'
    
    if not re.search(pattern, content, re.DOTALL):
        # Try to match multiline with nested braces
        # This is complex, let's use a simpler approach
        # Find: "properties = buildJsonObject {"
        # Then count braces until we find the matching close
        pass
    
    # Simpler approach: find "}\n        )" after "properties = buildJsonObject"
    # Replace with "},\n        required = xxx\n    )"
    
    if required_fields:
        required_str = f'listOf({", ".join(f\'"{f}\'' for f in required_fields)})'
    else:
        required_str = 'emptyList()'
    
    # Find the pattern more carefully
    # After "inputSchema = Tool.Input(" we have "properties = buildJsonObject { ... }"
    # We need to add ", required = xxx" before the closing ")"
    
    # Let's match the specific pattern
    lines = content.split('\n')
    new_lines = []
    in_input_schema = False
    brace_count = 0
    
    for i, line in enumerate(lines):
        if 'properties = buildJsonObject {' in line:
            in_input_schema = True
            brace_count = line.count('{') - line.count('}')
        elif in_input_schema:
            brace_count += line.count('{') - line.count('}')
            if brace_count == 0 and '}' in line:
                # This is the closing brace of buildJsonObject
                # Next line should be "        )" or similar
                new_lines.append(line.rstrip())
                # Add required parameter
                indent = ' ' * 8
                new_lines.append(indent + '},')
                new_lines.append(indent + f'required = {required_str}')
                in_input_schema = False
                continue
        
        new_lines.append(line)
    
    new_content = '\n'.join(new_lines)
    
    if new_content == content:
        return False, "No changes"
    
    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(new_content)
    
    return True, f"Added required = {required_str}"

# List of files to fix
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
    "MoveFileTool.kt",
    "MoveFileToTrashTool.kt",
    "SearchMembersTool.kt",
    "UpdateFileTool.kt",
    "UploadFileTool.kt",
]

tools_dir = Path("/Users/jp17463/DoorayMCP/src/main/kotlin/com/bifos/dooray/mcp/tools")
fixed_count = 0

for filename in files_to_fix:
    file_path = tools_dir / filename
    if not file_path.exists():
        print(f"❌ {filename}: File not found")
        continue
    
    success, message = fix_file(file_path)
    if success:
        print(f"✅ {filename}: {message}")
        fixed_count += 1
    else:
        print(f"⏭️  {filename}: {message}")

print(f"\n📊 Fixed {fixed_count}/{len(files_to_fix)} files")
