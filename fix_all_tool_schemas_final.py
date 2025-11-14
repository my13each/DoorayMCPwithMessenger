#!/usr/bin/env python3
"""
Fix all Tool.Input schemas to wrap properties correctly for JSON Schema draft 2020-12.

Changes from:
  Tool.Input(
    properties = buildJsonObject {
      putJsonObject("field") { ... }
    },
    required = listOf("field")
  )

To:
  Tool.Input(
    properties = buildJsonObject {
      put("type", "object")
      putJsonObject("properties") {
        putJsonObject("field") { ... }
      }
      putJsonArray("required") {
        add("field")
      }
    }
  )
"""
import re
from pathlib import Path
import sys

def fix_tool_schema(content: str) -> str:
    """Fix Tool.Input schema in a single file."""

    # Pattern to match Tool.Input(...) with properties and optional required
    # This is a complex multi-line pattern
    pattern = r'(Tool\.Input\(\s*properties\s*=\s*buildJsonObject\s*\{)(.*?)(\}\s*(?:,\s*required\s*=\s*listOf\((.*?)\))?\s*\))'

    def replace_schema(match):
        prefix = match.group(1)  # "Tool.Input(properties = buildJsonObject {"
        properties_content = match.group(2)  # The properties fields
        suffix_part = match.group(3)  # "}, required = listOf(...)" or "})"
        required_fields = match.group(4)  # "field1", "field2"  or None

        # Extract required fields if present
        required_list = []
        if required_fields:
            # Parse required fields from listOf("field1", "field2")
            required_list = [f.strip().strip('"') for f in required_fields.split(',')]

        # Build new structure
        new_content = f'''{prefix}
                put("type", "object")
                putJsonObject("properties") {{
{properties_content}
                }}'''

        if required_list:
            required_entries = '\n'.join(f'                    add("{field}")' for field in required_list)
            new_content += f'''
                putJsonArray("required") {{
{required_entries}
                }}'''

        new_content += '\n            }'

        # Add closing parenthesis
        new_content += '\n        )'

        return new_content

    # Apply the pattern with DOTALL flag to match across multiple lines
    result = re.sub(pattern, replace_schema, content, flags=re.DOTALL)

    return result


def process_file(file_path: Path) -> bool:
    """Process a single tool file. Returns True if modified."""
    try:
        content = file_path.read_text(encoding='utf-8')
        original = content

        # Skip if already has the correct pattern
        if 'put("type", "object")' in content and 'putJsonObject("properties")' in content:
            print(f"⏭️  Skipped: {file_path.name} (already fixed)")
            return False

        modified = fix_tool_schema(content)

        if modified != original:
            file_path.write_text(modified, encoding='utf-8')
            print(f"✅ Fixed: {file_path.name}")
            return True
        else:
            print(f"⚠️  No changes: {file_path.name}")
            return False

    except Exception as e:
        print(f"❌ Error processing {file_path.name}: {e}")
        return False


def main():
    tools_dir = Path(__file__).parent / "src/main/kotlin/com/bifos/dooray/mcp/tools"

    if not tools_dir.exists():
        print(f"Error: Tools directory not found: {tools_dir}")
        sys.exit(1)

    # Get all Tool files except SendDirectMessageTool (already fixed)
    tool_files = list(tools_dir.glob("*Tool.kt"))
    tool_files = [f for f in tool_files if f.name != "SendDirectMessageTool.kt"]

    print(f"Found {len(tool_files)} tool files to process\n")

    modified_count = 0
    for tool_file in sorted(tool_files):
        if process_file(tool_file):
            modified_count += 1

    print(f"\n{'='*60}")
    print(f"Summary: Modified {modified_count} out of {len(tool_files)} files")
    print(f"{'='*60}")


if __name__ == "__main__":
    main()
