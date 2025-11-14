#!/usr/bin/env python3
import re
import os
import glob

base_path = "/Users/jp17463/DoorayMCP/src/main/kotlin/com/bifos/dooray/mcp/tools/"
tool_files = glob.glob(os.path.join(base_path, "*Tool.kt"))

def fix_schema(content):
    """Fix schema to proper JSON Schema draft 2020-12 format"""

    # Pattern 1: Fix schemas that were just changed to wrong format
    # From: properties = buildJsonObject { putJsonObject("field") {...} }, required = listOf(...)
    # To: properties = buildJsonObject { put("type", "object") putJsonObject("properties") {...} putJsonArray("required") {...} put("additionalProperties", false) }

    pattern1 = r'inputSchema = Tool\.Input\(\s+properties = buildJsonObject \{\s+((?:.*?\n)*?)\s+\},\s+required = listOf\((.*?)\)\s+\),'

    def replacer1(match):
        properties_content = match.group(1).strip()
        required_items = match.group(2).strip()

        # Build required array
        required_list = []
        for item in required_items.split(','):
            item = item.strip().strip('"').strip("'")
            if item:
                required_list.append(item)

        required_block = '\n                putJsonArray("required") {\n'
        for item in required_list:
            required_block += f'                    add("{item}")\n'
        required_block += '                }'

        return f'''inputSchema = Tool.Input(
            properties = buildJsonObject {{
                put("type", "object")
                putJsonObject("properties") {{
{properties_content}
                }}
{required_block}
                put("additionalProperties", false)
            }}
        ),'''

    new_content = re.sub(pattern1, replacer1, content, flags=re.DOTALL)

    # Pattern 2: Fix schemas that already have type/properties but missing additionalProperties
    # Add additionalProperties: false before the closing brace
    pattern2 = r'(putJsonArray\("required"\) \{[^}]+\})\s+\}\s+\),\s+outputSchema'

    def replacer2(match):
        return match.group(1) + '\n                put("additionalProperties", false)\n            }\n        ),\n        outputSchema'

    new_content = re.sub(pattern2, replacer2, new_content, flags=re.DOTALL)

    return new_content

def add_missing_imports(content):
    """Add putJsonArray and add imports if missing"""
    imports_section = content.split('\n\n')[0]  # Get import section

    needs_putJsonArray = 'putJsonArray' in content and 'import kotlinx.serialization.json.putJsonArray' not in content
    needs_add = '\nadd(' in content and 'import kotlinx.serialization.json.add' not in content

    if needs_putJsonArray or needs_add:
        # Find the last kotlinx.serialization.json import
        import_pattern = r'(import kotlinx\.serialization\.json\.putJsonObject\n)'
        if needs_putJsonArray and needs_add:
            replacement = r'\1import kotlinx.serialization.json.putJsonArray\nimport kotlinx.serialization.json.add\n'
        elif needs_putJsonArray:
            replacement = r'\1import kotlinx.serialization.json.putJsonArray\n'
        elif needs_add:
            replacement = r'\1import kotlinx.serialization.json.add\n'
        else:
            return content

        content = re.sub(import_pattern, replacement, content)

    return content

fixed_count = 0
error_count = 0
skipped_count = 0

for filepath in tool_files:
    filename = os.path.basename(filepath)

    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()

        # Check if needs fixing
        if 'required = listOf(' in content or ('putJsonArray("required")' in content and 'additionalProperties' not in content):
            new_content = fix_schema(content)
            new_content = add_missing_imports(new_content)

            if new_content != content:
                with open(filepath, 'w', encoding='utf-8') as f:
                    f.write(new_content)
                print(f"✅ Fixed: {filename}")
                fixed_count += 1
            else:
                print(f"⚠️  No changes: {filename}")
        else:
            print(f"⏭️  Skipped (OK): {filename}")
            skipped_count += 1

    except Exception as e:
        print(f"❌ Error in {filename}: {e}")
        import traceback
        traceback.print_exc()
        error_count += 1

print(f"\n✅ Fixed: {fixed_count} files")
print(f"⏭️  Skipped: {skipped_count} files")
if error_count > 0:
    print(f"❌ Errors: {error_count} files")
