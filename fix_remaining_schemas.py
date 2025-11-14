#!/usr/bin/env python3
import re
import os
import glob

base_path = "/Users/jp17463/DoorayMCP/src/main/kotlin/com/bifos/dooray/mcp/tools/"
tool_files = glob.glob(os.path.join(base_path, "*Tool.kt"))

def fix_tool_input_comprehensive(content):
    """Fix all Tool.Input patterns comprehensively"""

    # Pattern 1: With required array
    pattern1 = r'inputSchema\s*=\s*Tool\.Input\(\s*properties\s*=\s*buildJsonObject \{\s*put\("type", "object"\)\s*putJsonObject\("properties"\) \{(.*?)\}\s*putJsonArray\("required"\) \{(.*?)\}\s*put\("additionalProperties", false\)\s*\}\s*\),'

    def replacer1(match):
        fields_content = match.group(1).strip()
        required_content = match.group(2).strip()

        required_fields = re.findall(r'add\("([^"]+)"\)', required_content)
        required_list = ', '.join(f'"{field}"' for field in required_fields)

        return f'''inputSchema = Tool.Input(
            properties = buildJsonObject {{
{fields_content}
            }},
            required = listOf({required_list})
        ),'''

    new_content = re.sub(pattern1, replacer1, content, flags=re.DOTALL)

    # Pattern 2: Without required array (optional params only or no params)
    pattern2 = r'inputSchema\s*=\s*Tool\.Input\(\s*properties\s*=\s*buildJsonObject \{\s*put\("type", "object"\)\s*putJsonObject\("properties"\) \{(.*?)\}\s*\}\s*\),'

    def replacer2(match):
        fields_content = match.group(1).strip()

        if not fields_content or '// ' in fields_content:
            # Empty properties or just comments
            return f'''inputSchema = Tool.Input(
            properties = buildJsonObject {{
{fields_content}
            }}
        ),'''
        else:
            # Has optional fields
            return f'''inputSchema = Tool.Input(
            properties = buildJsonObject {{
{fields_content}
            }}
        ),'''

    new_content = re.sub(pattern2, replacer2, new_content, flags=re.DOTALL)

    return new_content

def remove_unused_imports(content):
    """Remove putJsonArray and add imports if they're no longer used"""

    # Check if putJsonArray or add are still used outside of imports
    body_content = content.split('\n\n', 1)[1] if '\n\n' in content else content

    has_putJsonArray_usage = 'putJsonArray' in body_content
    has_add_usage = '\nadd(' in body_content

    if not has_putJsonArray_usage:
        content = re.sub(r'import kotlinx\.serialization\.json\.putJsonArray\n', '', content)

    if not has_add_usage:
        content = re.sub(r'import kotlinx\.serialization\.json\.add\n', '', content)

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
        if 'put("type", "object")' in content and 'putJsonObject("properties")' in content:
            new_content = fix_tool_input_comprehensive(content)
            new_content = remove_unused_imports(new_content)

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
