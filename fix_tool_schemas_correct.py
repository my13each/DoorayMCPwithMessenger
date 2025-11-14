#!/usr/bin/env python3
import re
import os
import glob

base_path = "/Users/jp17463/DoorayMCP/src/main/kotlin/com/bifos/dooray/mcp/tools/"
tool_files = glob.glob(os.path.join(base_path, "*Tool.kt"))

def fix_tool_input(content):
    """Fix Tool.Input to use correct constructor with properties and required parameters"""

    # Pattern to match current incorrect structure:
    # inputSchema = Tool.Input(
    #     properties = buildJsonObject {
    #         put("type", "object")
    #         putJsonObject("properties") {
    #             putJsonObject("field1") { ... }
    #             putJsonObject("field2") { ... }
    #         }
    #         putJsonArray("required") {
    #             add("field1")
    #             add("field2")
    #         }
    #         put("additionalProperties", false)
    #     }
    # )

    pattern = r'inputSchema = Tool\.Input\(\s*properties = buildJsonObject \{\s*put\("type", "object"\)\s*putJsonObject\("properties"\) \{(.*?)\}\s*putJsonArray\("required"\) \{(.*?)\}\s*put\("additionalProperties", false\)\s*\}\s*\),'

    def replacer(match):
        fields_content = match.group(1).strip()
        required_content = match.group(2).strip()

        # Extract required field names from add("fieldname") calls
        required_fields = re.findall(r'add\("([^"]+)"\)', required_content)
        required_list = ', '.join(f'"{field}"' for field in required_fields)

        return f'''inputSchema = Tool.Input(
            properties = buildJsonObject {{
{fields_content}
            }},
            required = listOf({required_list})
        ),'''

    new_content = re.sub(pattern, replacer, content, flags=re.DOTALL)
    return new_content

def remove_unused_imports(content):
    """Remove putJsonArray and add imports if they're no longer used in the file"""

    # Check if putJsonArray or add are still used outside of imports
    has_putJsonArray_usage = 'putJsonArray' in content.split('\n\n', 1)[1] if '\n\n' in content else False
    has_add_usage = '\nadd(' in content.split('\n\n', 1)[1] if '\n\n' in content else False

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

        # Check if needs fixing (has the old pattern)
        if 'putJsonObject("properties")' in content and 'putJsonArray("required")' in content:
            new_content = fix_tool_input(content)
            new_content = remove_unused_imports(new_content)

            if new_content != content:
                with open(filepath, 'w', encoding='utf-8') as f:
                    f.write(new_content)
                print(f"✅ Fixed: {filename}")
                fixed_count += 1
            else:
                print(f"⚠️  No changes: {filename}")
        else:
            print(f"⏭️  Skipped (already correct or different pattern): {filename}")
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
