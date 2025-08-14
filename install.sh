#!/bin/bash

# Dooray MCP with Messenger - Installation Script
# GitHub Enterprise Release: https://github.nhnent.com/sungmin-koo/DoorayMCPwithMessanger

set -e

# 설정
GITHUB_URL="https://github.nhnent.com/sungmin-koo/DoorayMCPwithMessanger"
RELEASE_TAG="v1.0.0"
JAR_NAME="dooray-mcp-server-0.2.1-all.jar"
INSTALL_DIR="$HOME/dooray-mcp-messenger"
CLAUDE_CONFIG_DIR="$HOME/Library/Application Support/Claude"

echo "🚀 Dooray MCP with Messenger 설치 시작..."

# 설치 디렉터리 생성
echo "📁 설치 디렉터리 생성: $INSTALL_DIR"
mkdir -p "$INSTALL_DIR"

# JAR 파일 다운로드 (GitHub Enterprise에서)
echo "⬇️ JAR 파일 다운로드 중..."
DOWNLOAD_URL="$GITHUB_URL/releases/download/$RELEASE_TAG/$JAR_NAME"
echo "다운로드 URL: $DOWNLOAD_URL"

# curl을 사용하여 다운로드 (인증이 필요할 수 있음)
if curl -L -f -o "$INSTALL_DIR/$JAR_NAME" "$DOWNLOAD_URL"; then
    echo "✅ JAR 파일 다운로드 완료"
else
    echo "❌ JAR 파일 다운로드 실패"
    echo "GitHub Enterprise 접근 권한을 확인하거나 수동으로 다운로드해주세요:"
    echo "$DOWNLOAD_URL"
    exit 1
fi

# Java 21 확인
echo "☕ Java 21 확인 중..."
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | head -n1 | awk -F '"' '{print $2}' | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -ge 21 ]; then
        echo "✅ Java $JAVA_VERSION 발견"
        JAVA_CMD="java"
    else
        echo "⚠️ Java 21이 필요합니다. 현재 버전: $JAVA_VERSION"
    fi
else
    echo "⚠️ Java가 설치되지 않았습니다."
fi

# Homebrew Java 21 확인
if [ -f "/opt/homebrew/opt/openjdk@21/bin/java" ]; then
    echo "✅ Homebrew Java 21 발견"
    JAVA_CMD="/opt/homebrew/opt/openjdk@21/bin/java"
elif [ -z "$JAVA_CMD" ]; then
    echo "❌ Java 21을 찾을 수 없습니다. 다음 명령으로 설치해주세요:"
    echo "brew install openjdk@21"
    exit 1
fi

# Claude Desktop 설정 생성
echo "⚙️ Claude Desktop 설정 준비 중..."

cat << EOF

🎉 설치 완료!

Claude Desktop 설정에 다음을 추가하세요:
($CLAUDE_CONFIG_DIR/claude_desktop_config.json)

{
  "mcpServers": {
    "dooray-mcp-messenger": {
      "command": "$JAVA_CMD",
      "args": [
        "-jar",
        "$INSTALL_DIR/$JAR_NAME"
      ],
      "env": {
        "DOORAY_API_KEY": "[YOUR_API_KEY]",
        "DOORAY_BASE_URL": "https://api.dooray.com"
      }
    }
  }
}

📝 설정 후 Claude Desktop을 재시작하세요.

🔧 메신저 기능:
- 멤버 검색
- 다이렉트 메시지 전송
- 채널 목록 조회
- 채널 메시지 조회
- 채널 메시지 전송

EOF