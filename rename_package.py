#!/usr/bin/env python3
"""
Rename package from com.bifos to com.my13each in all Kotlin source files
"""

import os
import re
from pathlib import Path

def update_kotlin_file(file_path: Path):
    """Update package declarations and imports in a Kotlin file"""
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()

        original_content = content

        # Replace package declarations
        content = re.sub(r'package com\.bifos\.dooray\.mcp', 'package com.my13each.dooray.mcp', content)

        # Replace import statements
        content = re.sub(r'import com\.bifos\.dooray\.mcp', 'import com.my13each.dooray.mcp', content)

        # Only write if changed
        if content != original_content:
            with open(file_path, 'w', encoding='utf-8') as f:
                f.write(content)
            print(f"✅ Updated: {file_path}")
            return True
        else:
            print(f"⏭️  Skipped (no changes): {file_path}")
            return False
    except Exception as e:
        print(f"❌ Error processing {file_path}: {e}")
        return False

def main():
    src_dir = Path("/Users/jp17463/DoorayMCP/src")

    if not src_dir.exists():
        print(f"❌ Source directory not found: {src_dir}")
        return

    # Find all Kotlin files
    kt_files = list(src_dir.rglob("*.kt"))
    print(f"Found {len(kt_files)} Kotlin files\n")

    updated_count = 0
    for kt_file in sorted(kt_files):
        if update_kotlin_file(kt_file):
            updated_count += 1

    print(f"\n{'='*60}")
    print(f"Summary: Updated {updated_count} out of {len(kt_files)} files")
    print(f"{'='*60}")

if __name__ == "__main__":
    main()
