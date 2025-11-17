#!/usr/bin/env python3
"""
Fix all Dooray MCP tool schemas to be JSON Schema draft 2020-12 compliant.
This script safely transforms Tool.Input definitions from SDK 0.6.0 to 0.7.7 format.
"""
import re
from pathlib import Path

def fix_import(content):
    """Fix import statements to use kotlinx.serialization.json.*"""
    # Pattern for multiple specific imports
    pattern = r'import kotlinx\.serialization\.json\.(buildJsonObject|jsonPrimitive|put|putJsonArray|JsonPrimitive|putJsonObject|jsonArray|jsonObject)(\n\s*import kotlinx\.serialization\.json\..*)*'

    if 'import kotlinx.serialization.json.*' not in content:
        content = re.sub(
            pattern,
            'import kotlinx.serialization.json.*',
            content,
            flags=re.MULTILINE
        )
    return content

def extract_required_fields(required_str):
    """Extract field names from required = listOf(...) or emptyList()"""
    if 'emptyList()' in required_str or not required_str.strip():
        return []

    match = re.search(r'listOf\((.*?)\)', required_str, re.DOTALL)
    if match:
        items_str = match.group(1)
        # Extract quoted strings
        fields = re.findall(r'"([^"]+)"', items_str)
        return fields
    return []

def transform_schema_block(match):
    """Transform a single inputSchema block"""
    indent = match.group(1)
    properties_block = match.group(2)

    # Extract required parameter if exists
    required_match = re.search(r',\s*required\s*=\s*([^)]+)\s*\)', match.group(0))
    required_fields = []
    if required_match:
        required_fields = extract_required_fields(required_match.group(1))

    # Build required array
    if required_fields:
        required_items = '\n'.join([
            f'{indent}                    add(JsonPrimitive("{field}"))'
            for field in required_fields
        ])
    else:
        required_items = f'{indent}                    // No required parameters'

    # Construct new schema
    new_schema = f'''{indent}inputSchema = Tool.Input(
{indent}            properties = buildJsonObject {{
{indent}                put("type", JsonPrimitive("object"))
{indent}                putJsonObject("properties") {{
{properties_block}
{indent}                }}
{indent}                putJsonArray("required") {{
{required_items}
{indent}                }}
{indent}            }}
{indent}        ),'''

    return new_schema

def fix_tool_schema(content):
    """Fix the inputSchema definition"""
    # Pattern to match the entire inputSchema block
    # This captures from "inputSchema = Tool.Input(" to the closing "),"
    pattern = r'(\s+)inputSchema\s*=\s*Tool\.Input\(\s*properties\s*=\s*buildJsonObject\s*\{(.*?)\}(?:\s*,\s*required\s*=\s*[^)]+)?\s*\),'

    content = re.sub(pattern, transform_schema_block, content, flags=re.DOTALL)
    return content

def fix_file(file_path):
    """Fix a single Kotlin file"""
    print(f"  Processing: {file_path.name}")

    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            original_content = f.read()

        # Apply fixes
        content = fix_import(original_content)
        content = fix_tool_schema(content)

        if content != original_content:
            with open(file_path, 'w', encoding='utf-8') as f:
                f.write(content)
            print(f"    ✅ Fixed")
            return True
        else:
            print(f"    ⏭️  Already fixed or no match")
            return False

    except Exception as e:
        print(f"    ❌ Error: {e}")
        return False

def main():
    tools_dir = Path("/Users/jp17463/DoorayMCP/src/main/kotlin/com/bifos/dooray/mcp/tools")

    # Get all tool files
    tool_files = sorted(tools_dir.glob("*.kt"))

    print(f"\n{'='*70}")
    print(f"Dooray MCP Schema Fixer - JSON Schema draft 2020-12 Compliance")
    print(f"{'='*70}")
    print(f"Found {len(tool_files)} tool files\n")

    fixed_count = 0
    skipped_count = 0
    error_count = 0

    for tool_file in tool_files:
        result = fix_file(tool_file)
        if result is True:
            fixed_count += 1
        elif result is False:
            skipped_count += 1
        else:
            error_count += 1

    print(f"\n{'='*70}")
    print(f"Summary:")
    print(f"  ✅ Fixed: {fixed_count}")
    print(f"  ⏭️  Skipped: {skipped_count}")
    print(f"  ❌ Errors: {error_count}")
    print(f"  📊 Total: {len(tool_files)}")
    print(f"{'='*70}\n")

    return fixed_count > 0

if __name__ == "__main__":
    import sys
    success = main()
    sys.exit(0 if success else 1)
