#!/usr/bin/env python3
import re
import os
import glob

base_path = "/Users/jp17463/DoorayMCP/src/main/kotlin/com/bifos/dooray/mcp/tools/"

# Get all tool files
tool_files = glob.glob(os.path.join(base_path, "*Tool.kt"))

def extract_required_fields(content):
    """Extract required fields from the putJsonArray("required") block"""
    pattern = r'putJsonArray\("required"\)\s*\{([^}]+)\}'
    match = re.search(pattern, content, re.DOTALL)
    if match:
        adds = re.findall(r'add\("([^"]+)"\)', match.group(1))
        return adds
    return []

def fix_schema(content):
    """Fix the schema structure by removing wrapper and moving required to parameter"""

    # Extract required fields first
    required_fields = extract_required_fields(content)

    # Pattern to match the entire inputSchema block
    # Match: inputSchema = Tool.Input(\n            properties = buildJsonObject {\n                put("type", "object")\n                putJsonObject("properties") { ... }\n                putJsonArray("required") { ... }\n            }\n        ),

    pattern = r'(inputSchema = Tool\.Input\(\s+properties = buildJsonObject \{\s+)put\("type", "object"\)\s+putJsonObject\("properties"\) \{(.*?)\s+\}\s+putJsonArray\("required"\) \{[^}]+\}\s+\}(\s+\),)'

    def replacer(match):
        properties_content = match.group(2)

        if required_fields:
            required_list = ', '.join([f'"{f}"' for f in required_fields])
            return match.group(1) + properties_content + '\n            },\n            required = listOf(' + required_list + ')\n        ),'
        else:
            return match.group(1) + properties_content + '\n            }\n        ),'

    new_content = re.sub(pattern, replacer, content, flags=re.DOTALL)
    return new_content

def remove_unused_imports(content):
    """Remove putJsonArray and add imports if they're not used elsewhere"""
    # Check if putJsonArray or add are used outside of inputSchema
    # Simple check: if they appear only once each (in imports), remove them
    if content.count('putJsonArray') == 1 and content.count('\nadd') == 1:
        content = re.sub(r'import kotlinx\.serialization\.json\.putJsonArray\n', '', content)
        content = re.sub(r'import kotlinx\.serialization\.json\.add\n', '', content)
        content = re.sub(r'import kotlinx\.serialization\.json\.JsonPrimitive\n', '', content)
    return content

fixed_count = 0
error_count = 0

for filepath in tool_files:
    filename = os.path.basename(filepath)

    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()

        # Skip if already fixed (check if it has the old pattern)
        if 'put("type", "object")' not in content or 'putJsonObject("properties")' not in content:
            print(f"⏭️  Skipped (already fixed): {filename}")
            continue

        new_content = fix_schema(content)

        if new_content != content:
            new_content = remove_unused_imports(new_content)
            with open(filepath, 'w', encoding='utf-8') as f:
                f.write(new_content)
            print(f"✅ Fixed: {filename}")
            fixed_count += 1
        else:
            print(f"⚠️  No changes: {filename}")

    except Exception as e:
        print(f"❌ Error in {filename}: {e}")
        error_count += 1

print(f"\n✅ Fixed {fixed_count} files")
if error_count > 0:
    print(f"❌ {error_count} errors")
