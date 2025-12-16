# DoorayMCP プロジェクト概要

## プロジェクトの目的

DoorayMCPは、NHN Dooray統合のためのMCP (Model Context Protocol) サーバー実装です。Claude（AI）がDoorayサービスと対話できるように、stdin/stdoutを介した標準化されたMCPプロトコルで48のツールを提供します。

### 主な機能カテゴリ
- **Wiki**: Wiki空間、ページの取得、作成、更新、コメント管理
- **Project**: タスク管理（作成、更新、ステータス変更、コメント、ワークログ）
- **Messenger**: メンバー検索、ダイレクトメッセージ、チャンネル管理、メッセージ送信
- **Calendar**: カレンダー、イベントの取得、作成、管理
- **Drive**: ドライブ管理、ファイルアップロード/ダウンロード、共有リンク、フォルダ操作

## 技術スタック

### 言語とビルドシステム
- **言語**: Kotlin 2.1.20
- **JVM**: Java 21
- **ビルドツール**: Gradle 8.10+
- **プラグイン**: 
  - Shadow plugin（fat JAR生成用）
  - Kotlin serialization plugin

### フレームワークとライブラリ
- **MCP SDK**: io.modelcontextprotocol:kotlin-sdk:0.7.7
- **HTTP クライアント**: Ktor 3.1.1
  - ktor-client-content-negotiation（コンテンツネゴシエーション）
  - ktor-serialization-kotlinx-json（JSONシリアライゼーション）
  - ktor-client-logging（HTTPロギング）
- **ロギング**: Logback 1.5.18（stderrのみに出力、stdoutはMCPプロトコル用）
- **テスティング**:
  - JUnit Platform
  - kotlinx-coroutines-test 1.10.1
  - ktor-client-mock（HTTPモッキング）
  - mockk 1.13.10（モッキングフレームワーク）

### デプロイメント
- **Docker**: マルチステージビルド
  - ビルダー: gradle:8.10-jdk21
  - ランタイム: eclipse-temurin:21-jre-alpine
  - 非rootユーザー: dooray:dooray (UID/GID 1000)
  - **注**: 現在はAMD64のみサポート（ARM64は一時的に無効化）

## システム要件
- Darwin (macOS) または Linux
- JDK 21以上
- Docker（コンテナ実行の場合）

## 環境変数

| 変数名 | 必須 | 説明 | デフォルト |
|--------|------|------|-----------|
| DOORAY_API_KEY | ✓ | Dooray API認証キー | - |
| DOORAY_BASE_URL | ✓ | Dooray APIベースURL | - |
| DOORAY_ENABLED_CATEGORIES | - | ツールカテゴリフィルタ（カンマ区切り: wiki,project,messenger,calendar,drive） | 全カテゴリ有効 |
| DOORAY_LOG_LEVEL | - | 一般ログレベル（DEBUG/INFO/WARN/ERROR） | WARN |
| DOORAY_HTTP_LOG_LEVEL | - | HTTPクライアントログレベル | WARN |
| CI | - | "true"の場合、統合テストを自動除外 | - |

## プロジェクト規模
- ツール数: 48個
- ソースファイル: 約60個のKotlinファイル
- テストファイル: 7個（ユニットテスト + 統合テスト）
