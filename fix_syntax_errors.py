#!/usr/bin/env python3
"""
Fix the syntax errors in the 34 tool files.
The issue is that the closing brace is in the wrong place.
"""

import re
from pathlib import Path

def fix_syntax(content):
    """
    Fix the pattern where properties block is closed with }, followed by putJsonArray("required")
    This causes putJsonArray to be outside the buildJsonObject.

    Pattern to fix:
        },


                    putJsonArray("required") {

    Should be:
        }

            putJsonArray("required") {
    """

    # Fix the specific pattern where }, appears before putJsonArray("required")
    # and putJsonArray is outside the buildJsonObject
    pattern = r'(\s+)\},(\s+)putJsonArray\("required"\)\s*\{'

    def replace_func(match):
        indent = match.group(1)
        # Return proper closing of properties and start of required array
        return f'{indent}}}\n\n{indent}putJsonArray("required") {{'

    content = re.sub(pattern, replace_func, content)

    # Now we need to ensure the buildJsonObject is properly closed
    # Pattern: after the putJsonArray required block closes, we need to close buildJsonObject
    # Look for the pattern:
    #     }
    #         }
    #     )
    # Which should be:
    #     }
    # }
    # ),

    # This is tricky. Let me use a different approach - find where buildJsonObject starts
    # and make sure it has proper closing

    # Actually, let me check if there's a hanging Tool.Input structure
    # The pattern should be:
    # inputSchema = Tool.Input(
    #     properties = buildJsonObject {
    #         ...
    #     }
    # ),

    # Replace pattern where we have:
    #                     }
    #                 }
    #             )
    # With:
    #             }
    #         }
    #     ),

    pattern2 = r'(\s+)\}(\s+)\}(\s+)\)'
    def replace_func2(match):
        # Find the right indentation
        # Should be uniform decrease in indentation
        return '            }\n        }\n    ),'

    content = re.sub(pattern2, replace_func2, content)

    return content

def main():
    tools_dir = Path("/Users/jp17463/DoorayMCP/src/main/kotlin/com/bifos/dooray/mcp/tools")

    # List of 34 files that have syntax errors
    failing_tools = [
        "GetProjectPostsTool.kt",
        "CreateProjectPostTool.kt",
        "SetProjectPostWorkflowTool.kt",
        "UpdateProjectPostTool.kt",
        "CreatePostCommentTool.kt",
        "GetPostCommentsTool.kt",
        "UpdatePostCommentTool.kt",
        "DeletePostCommentTool.kt",
        "SearchMembersTool.kt",
        "SendDirectMessageTool.kt",
        "GetSimpleChannelsTool.kt",
        "GetChannelTool.kt",
        "SendChannelMessageTool.kt",
        "GetCalendarDetailTool.kt",
        "GetCalendarEventsTool.kt",
        "GetCalendarEventDetailTool.kt",
        "CreateCalendarEventTool.kt",
        "GetDrivesTool.kt",
        "GetDriveFilesTool.kt",
        "UploadFileFromPathTool.kt",
        "UploadFileTool.kt",
        "DownloadFileTool.kt",
        "GetFileMetadataTool.kt",
        "UpdateFileTool.kt",
        "MoveFileToTrashTool.kt",
        "DeleteFileTool.kt",
        "CreateFolderTool.kt",
        "CopyFileTool.kt",
        "MoveFileTool.kt",
        "CreateSharedLinkTool.kt",
        "GetSharedLinksTool.kt",
        "GetSharedLinkDetailTool.kt",
        "UpdateSharedLinkTool.kt",
        "DeleteSharedLinkTool.kt",
    ]

    print(f"\n{'='*70}")
    print(f"Fixing Syntax Errors in 34 Tool Files")
    print(f"{'='*70}\n")

    fixed_count = 0
    error_count = 0

    for filename in failing_tools:
        file_path = tools_dir / filename

        if not file_path.exists():
            print(f"❌ {filename}: File not found")
            error_count += 1
            continue

        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                original_content = f.read()

            # Apply fix
            new_content = fix_syntax(original_content)

            if new_content != original_content:
                with open(file_path, 'w', encoding='utf-8') as f:
                    f.write(new_content)
                print(f"✅ {filename}: Fixed")
                fixed_count += 1
            else:
                print(f"⏭️  {filename}: No changes needed")

        except Exception as e:
            print(f"❌ {filename}: Error - {e}")
            error_count += 1

    print(f"\n{'='*70}")
    print(f"Summary:")
    print(f"  ✅ Fixed: {fixed_count}")
    print(f"  ❌ Errors: {error_count}")
    print(f"  📊 Total: {len(failing_tools)}")
    print(f"{'='*70}\n")

    return fixed_count > 0

if __name__ == "__main__":
    import sys
    success = main()
    sys.exit(0 if success else 1)
