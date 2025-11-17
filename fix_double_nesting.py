#!/usr/bin/env python3
"""
Fix double-nesting issues in Tool schemas.
Removes the outer wrapping when there's a double-nested structure.
"""

import re
from pathlib import Path

def fix_double_nesting(content: str) -> tuple[str, bool]:
    """
    Fix double-nested inputSchema structure.

    Pattern to match:
    inputSchema = Tool.Input(
        properties = buildJsonObject {
            put("type", JsonPrimitive("object"))
            putJsonObject("properties") {
                put("type", JsonPrimitive("object"))    <- Remove this outer layer
                putJsonObject("properties") {           <- Remove this outer layer
                    ... actual properties ...
                }
                putJsonArray("required") { ... }         <- Remove this outer layer
            }
            putJsonArray("required") { ... }            <- Keep this (usually empty)
        }
    )
    """

    # Check if this file has double-nesting
    type_count = content.count('put("type", JsonPrimitive("object"))')
    if type_count <= 1:
        return content, False  # No double-nesting

    # Find the inputSchema section
    schema_pattern = r'(inputSchema = Tool\.Input\(\s*properties = buildJsonObject \{)(.*?)(^\s*\),)'
    match = re.search(schema_pattern, content, re.MULTILINE | re.DOTALL)

    if not match:
        return content, False

    prefix = match.group(1)
    schema_body = match.group(2)
    suffix = match.group(3)

    # Pattern to extract the inner properties block
    # We want to keep only the innermost properties block
    inner_pattern = r'put\("type", JsonPrimitive\("object"\)\)\s*putJsonObject\("properties"\) \{(.*?)put\("type", JsonPrimitive\("object"\)\)\s*putJsonObject\("properties"\) \{(.*?)\}\s*putJsonArray\("required"\) \{(.*?)\}\s*\}\s*putJsonArray\("required"\) \{'

    inner_match = re.search(inner_pattern, schema_body, re.DOTALL)

    if inner_match:
        # Extract the actual properties and required array
        actual_properties = inner_match.group(2).strip()
        inner_required = inner_match.group(3).strip()

        # Find the outer required array (the one we want to keep)
        outer_required_pattern = r'putJsonArray\("required"\) \{([^}]*)\}\s*\}\s*\)'
        outer_required_match = re.search(outer_required_pattern, schema_body[-200:])  # Search near end
        outer_required = outer_required_match.group(1).strip() if outer_required_match else "// No required parameters"

        # Reconstruct the schema with single nesting
        new_schema_body = f'''
            put("type", JsonPrimitive("object"))

            putJsonObject("properties") {{
                {actual_properties}
            }}

            putJsonArray("required") {{
                {inner_required}
            }}

        '''

        new_content = content[:match.start()] + prefix + new_schema_body + suffix + content[match.end():]
        return new_content, True

    return content, False


def main():
    # Files with double-nesting issues
    files_to_fix = [
        "CreateChannelTool.kt",
        "CreateWikiPageTool.kt",
        "GetCalendarsTool.kt",
        "GetChannelsTool.kt",
        "GetProjectPostTool.kt",
        "GetProjectsTool.kt",
        "GetWikiPagesTool.kt",
        "GetWikiPageTool.kt",
        "GetWikisTool.kt",
        "SetProjectPostDoneTool.kt",
        "UpdateWikiPageTool.kt",
    ]

    base_dir = Path("src/main/kotlin/com/bifos/dooray/mcp/tools")

    fixed_count = 0
    skipped_count = 0
    error_count = 0

    for filename in files_to_fix:
        file_path = base_dir / filename

        try:
            content = file_path.read_text(encoding='utf-8')
            new_content, was_fixed = fix_double_nesting(content)

            if was_fixed:
                file_path.write_text(new_content, encoding='utf-8')
                print(f"✅ Fixed: {filename}")
                fixed_count += 1
            else:
                print(f"⏭️  Skipped: {filename} (no double-nesting detected)")
                skipped_count += 1

        except Exception as e:
            print(f"❌ Error processing {filename}: {e}")
            error_count += 1

    print(f"\n{'='*60}")
    print(f"Summary: Fixed {fixed_count}, Skipped {skipped_count}, Errors {error_count}")
    print(f"{'='*60}")


if __name__ == "__main__":
    main()
