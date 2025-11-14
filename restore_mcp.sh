#!/bin/bash
# Claude Code MCP 설정 복원 스크립트

CONFIG_DIR="$HOME/.config/claude-code"

echo "🔄 Restoring Claude Code MCP configuration to empty state..."
cp "$CONFIG_DIR/mcp_config_empty.json" "$CONFIG_DIR/mcp_config.json"
echo "✅ Restored! Claude Code will have no MCP servers configured."
echo "📝 Restart Claude Code for changes to take effect."
