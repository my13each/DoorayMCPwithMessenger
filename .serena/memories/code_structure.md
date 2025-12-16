# DoorayMCP コード構造とアーキテクチャ

## ディレクトリ構造

```
DoorayMCP/
├── src/
│   ├── main/
│   │   ├── kotlin/com/my13each/dooray/mcp/
│   │   │   ├── Main.kt                      # エントリーポイント
│   │   │   ├── DoorayMcpServer.kt          # MCPサーバー初期化とツール登録
│   │   │   ├── client/
│   │   │   │   ├── DoorayClient.kt         # Dooray APIインターフェース
│   │   │   │   └── DoorayHttpClient.kt     # Ktor HTTP実装
│   │   │   ├── tools/                      # 48個のツール実装
│   │   │   │   ├── *WikiTool.kt            # Wiki関連ツール
│   │   │   │   ├── *ProjectPostTool.kt     # Project関連ツール
│   │   │   │   ├── *MessengerTool.kt       # Messenger関連ツール
│   │   │   │   ├── *CalendarTool.kt        # Calendar関連ツール
│   │   │   │   └── *DriveTool.kt           # Drive関連ツール
│   │   │   ├── types/                      # データ型定義
│   │   │   │   ├── WikiPageResponse.kt     # Wiki レスポンス型
│   │   │   │   ├── ProjectPostResponse.kt  # Project レスポンス型
│   │   │   │   ├── MessengerTypes.kt       # Messenger 型
│   │   │   │   ├── CalendarTypes.kt        # Calendar 型
│   │   │   │   ├── DriveTypes.kt           # Drive 型
│   │   │   │   ├── ToolResponseTypes.kt    # ツール共通レスポンス型
│   │   │   │   └── DoorayApiErrorType.kt   # エラー型
│   │   │   ├── constants/                  # 定数
│   │   │   │   ├── VersionConst.kt         # バージョン定数
│   │   │   │   ├── EnvVariableConst.kt     # 環境変数名
│   │   │   │   └── ToolCategory.kt         # ツールカテゴリEnum
│   │   │   ├── utils/
│   │   │   │   └── JsonUtils.kt            # JSONユーティリティ
│   │   │   └── exception/                  # カスタム例外
│   │   │       ├── ToolException.kt
│   │   │       └── CustomException.kt
│   │   └── resources/
│   │       └── logback.xml                 # ロギング設定（stderr専用）
│   └── test/
│       └── kotlin/com/my13each/dooray/mcp/
│           ├── tools/McpToolsUnitTest.kt
│           ├── client/dooray/*IntegrationTest.kt
│           └── util/TestUtil.kt
├── build.gradle.kts                        # Gradleビルド設定
├── settings.gradle.kts                     # Gradleプロジェクト設定
├── Dockerfile                              # Dockerイメージ定義
├── README.md                               # ユーザー向けドキュメント
├── CLAUDE.md                               # Claude Code向け指示
└── .github/workflows/                      # CI/CD設定
```

## アーキテクチャフロー

### MCPサーバー起動フロー

```
Main.kt
  └─ configureSystemLogging()              # すべてのログをstderrにリダイレクト
      └─ DoorayMcpServer.initServer()
          ├─ getEnv()                       # 環境変数検証（DOORAY_API_KEY, DOORAY_BASE_URL）
          ├─ DoorayHttpClient 作成          # Ktorベース HTTPクライアント（リトライロジック付き）
          ├─ Server (MCP SDK)               # プロトコル通信ハンドラー
          ├─ registerTool() (48回)          # DOORAY_ENABLED_CATEGORIES に基づき条件付き登録
          └─ StdioServerTransport           # stdin/stdoutトランスポート（blocking runBlocking）
```

### ツール実装パターン

各ツールは2つのコンポーネントから構成されます：

1. **Tool オブジェクト定義** (`*Tool.kt`)
   ```kotlin
   val GetWikiPageTool = Tool(
       name = "dooray_wiki_get_page",
       description = "特定のWikiページを取得",
       inputSchema = JsonObject(...)
   )
   ```

2. **ハンドラー関数**
   ```kotlin
   suspend fun handleGetWikiPage(request: CallToolRequest): CallToolResult {
       // 1. リクエストパラメータをパース
       // 2. DoorayClient メソッドを呼び出し
       // 3. レスポンスをツール結果にラップして返す
   }
   ```

### HTTP クライアント階層

```
Tools (48個)
  ↓ 呼び出し
DoorayClient (インターフェース)
  ↓ 実装
DoorayHttpClient (Ktor実装)
  ├─ ContentNegotiation + kotlinx.serialization
  ├─ リトライロジック
  ├─ 307リダイレクトサポート（Drive API用）
  └─ Base64エンコード/デコード（ファイル操作用）
```

## 重要な設計パターン

### 1. ロギングの衛生管理
- **重要**: MCPプロトコルはstdoutをJSON-RPCメッセージに使用
- **すべてのログはstderrに出力** (`Main.kt`でシステムプロパティ設定)
- デフォルトログレベル: `WARN`

### 2. カテゴリベースのツールフィルタリング
- ツールは5つのカテゴリに分類: `WIKI`, `PROJECT`, `MESSENGER`, `CALENDAR`, `DRIVE`
- 環境変数 `DOORAY_ENABLED_CATEGORIES` で有効化するツールを制御
- 未設定の場合: 全48ツールが登録される

### 3. エラーハンドリング
- カスタム例外: `ToolException`, `CustomException`
- 統一されたエラーレスポンス: `DoorayApiErrorType`
- ツールレスポンスは `ToolResponseTypes` でラップ（Success/Error）

### 4. HTTP 307リダイレクトハンドリング
- Drive操作は307リダイレクトが必要:
  - 初期リクエスト: `api.dooray.com`
  - リダイレクト先: `file-api.dooray.com`
- `DoorayHttpClient` でKtorの `followRedirects` を使用して実装

### 5. ファイルアップロード戦略
2つの方法が存在（優先順位あり）:
1. **dooray_drive_upload_file_from_path**（推奨）
   - ファイルパスから直接アップロード
   - サーバー側でBase64エンコード処理
   - Claudeのメッセージ長制限を回避
   - 最大100MBのファイルをサポート

2. **dooray_drive_upload_file**（フォールバック）
   - Base64エンコード済みコンテンツをアップロード
   - 小さいファイル（<10KB）向け
   - Claudeの約200K文字制限の対象

## 主要ファイルの役割

| ファイル | 役割 | 重要度 |
|---------|------|--------|
| `Main.kt` | エントリーポイント、ロギング設定 | ★★★ |
| `DoorayMcpServer.kt` | サーバー初期化、ツール登録（89-286行目） | ★★★ |
| `client/DoorayClient.kt` | 全Dooray APIメソッドのインターフェース | ★★★ |
| `client/DoorayHttpClient.kt` | Ktor HTTPクライアント実装 | ★★★ |
| `tools/*.kt` | 48個のツール定義とハンドラー | ★★★ |
| `types/*.kt` | リクエスト/レスポンスデータクラス（kotlinx.serialization） | ★★ |
| `constants/ToolCategory.kt` | ツールカテゴリEnum、パースロジック | ★★ |

## テスト戦略

- **ユニットテスト**: `McpToolsUnitTest.kt`
- **統合テスト**: `*IntegrationTest.kt`
  - 実際のDooray API認証情報が必要
  - CI環境（`CI=true`）では自動的に除外される
- **テストユーティリティ**: `TestUtil.kt`, `ClientStdio.kt`
