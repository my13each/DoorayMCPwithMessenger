# DoorayMCP コードスタイルと規約

## 言語とスタイル

### Kotlin イディオム
- Kotlinのイディオマティックなスタイルを採用
- コルーチン使用（`suspend` 関数）
- 4スペースインデント（Kotlin標準）
- パブリックAPIには明示的な戻り値の型を指定

### 基本的なコーディング規約
```kotlin
// ✅ 良い例: 明示的な戻り値の型
suspend fun fetchWikiPage(pageId: String): WikiPageResponse {
    return client.getWikiPage(pageId)
}

// ✅ 良い例: データクラスとシリアライゼーションアノテーション
@Serializable
data class WikiPageResponse(
    val id: String,
    val title: String,
    val content: String? = null  // オプショナルパラメータはnullable型で
)

// ✅ 良い例: インターフェースベースの設計
interface DoorayClient {
    suspend fun getWikiPage(cloudId: String, pageId: String): WikiPageResponse
}
```

## 命名規約

### クラスとインターフェース
- **PascalCase** を使用: `DoorayHttpClient`, `ToolException`
- インターフェース名に接頭辞/接尾辞なし: `DoorayClient` (not `IDoorayClient`)

### 関数とメソッド
- **camelCase** を使用: `getWikiPage()`, `handleGetWikiPage()`
- 動詞で始める: `get`, `create`, `update`, `delete`, `handle`, `register`

### 定数
- **UPPER_SNAKE_CASE** を使用: `DOORAY_API_KEY`, `DOORAY_BASE_URL`
- `object` や `companion object` 内で定義

```kotlin
object EnvVariableConst {
    const val DOORAY_API_KEY = "DOORAY_API_KEY"
    const val DOORAY_BASE_URL = "DOORAY_BASE_URL"
}
```

### 変数
- **camelCase** を使用: `pageId`, `cloudId`, `toolCategory`

## データ型の扱い

### Nullable型の使用
```kotlin
// ✅ オプショナルパラメータはnullable型で表現
data class UpdateWikiPageRequest(
    val title: String? = null,      // 未指定の場合は既存のタイトルを保持
    val body: String,                 // 必須パラメータ
    val parentId: String? = null      // オプショナル
)
```

### kotlinx.serialization の使用
```kotlin
@Serializable
data class WikiPageResponse(
    @SerialName("id") val id: String,
    @SerialName("title") val title: String,
    @SerialName("body") val body: WikiPageBody? = null
)
```

## インターフェースベースの設計

### クライアントインターフェース
```kotlin
// DoorayClient.kt - インターフェース定義
interface DoorayClient {
    suspend fun getWikiPage(cloudId: String, pageId: String): WikiPageResponse
    suspend fun createWikiPage(cloudId: String, request: CreateWikiPageRequest): WikiPageResponse
}

// DoorayHttpClient.kt - Ktor HTTP実装
class DoorayHttpClient(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val apiKey: String
) : DoorayClient {
    override suspend fun getWikiPage(cloudId: String, pageId: String): WikiPageResponse {
        // Ktor HTTP実装
    }
}
```

## エラーハンドリング

### カスタム例外の使用
```kotlin
// ツール固有のエラー
class ToolException(message: String) : Exception(message)

// 一般的なカスタムエラー
class CustomException(message: String) : Exception(message)
```

### 統一されたエラーレスポンス
```kotlin
@Serializable
data class ErrorResponse(
    val error: String,
    val message: String
)

// ツールレスポンスのラップ
sealed class ToolResponse {
    data class Success(val data: JsonElement) : ToolResponse()
    data class Error(val error: ErrorResponse) : ToolResponse()
}
```

## ロギングの重要ルール

### ⚠️ 重要: stdoutの保護
```kotlin
// ❌ 絶対にNG: stdoutへの出力
println("Debug message")  // MCPプロトコルを破壊する！

// ✅ 正しい: stderrへのログ出力
logger.debug("Debug message")  // Logbackがstderrにリダイレクト
logger.info("Info message")
logger.warn("Warning message")
logger.error("Error message")
```

### ログレベルの使い分け
- **DEBUG**: 詳細なデバッグ情報（開発時のみ）
- **INFO**: 一般的な情報メッセージ
- **WARN**: 警告（デフォルト）
- **ERROR**: エラー状況

## ツール実装パターン

### 標準的なツール構造
```kotlin
// 1. Tool オブジェクト定義
val GetWikiPageTool = Tool(
    name = "dooray_wiki_get_page",
    description = "特定のWikiページの詳細情報を取得します",
    inputSchema = JsonObject(
        mapOf(
            "type" to JsonPrimitive("object"),
            "properties" to JsonObject(
                mapOf(
                    "cloudId" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Cloud ID")
                        )
                    ),
                    "pageId" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Page ID")
                        )
                    )
                )
            ),
            "required" to JsonArray(listOf(JsonPrimitive("cloudId"), JsonPrimitive("pageId")))
        )
    )
)

// 2. ハンドラー関数
suspend fun handleGetWikiPage(request: CallToolRequest): CallToolResult {
    try {
        // パラメータパース
        val cloudId = request.params.arguments["cloudId"]?.jsonPrimitive?.content
            ?: throw ToolException("cloudId is required")
        val pageId = request.params.arguments["pageId"]?.jsonPrimitive?.content
            ?: throw ToolException("pageId is required")
        
        // APIコール
        val response = client.getWikiPage(cloudId, pageId)
        
        // 成功レスポンス
        return CallToolResult(
            content = listOf(TextContent(Json.encodeToString(response))),
            isError = false
        )
    } catch (e: Exception) {
        // エラーレスポンス
        return CallToolResult(
            content = listOf(TextContent("Error: ${e.message}")),
            isError = true
        )
    }
}
```

## コルーチンの使用

### suspend関数
```kotlin
// ✅ 非同期I/O操作にはsuspendを使用
suspend fun getWikiPage(cloudId: String, pageId: String): WikiPageResponse {
    return httpClient.get("/wikis/$cloudId/pages/$pageId")
}

// ✅ コルーチンスコープでの実行
runBlocking {
    val page = client.getWikiPage(cloudId, pageId)
}
```

## ファイル構成の規約

### パッケージ構造
```
com.my13each.dooray.mcp
├── Main.kt                     # エントリーポイント
├── DoorayMcpServer.kt         # サーバー初期化
├── client/                     # HTTPクライアント
├── tools/                      # ツール実装
├── types/                      # データ型
├── constants/                  # 定数
├── utils/                      # ユーティリティ
└── exception/                  # 例外
```

## ドキュメンテーション

### KDoc コメント
```kotlin
/**
 * Dooray Wiki ページを取得します。
 *
 * @param cloudId クラウドインスタンスID
 * @param pageId WikiページID
 * @return Wikiページのレスポンス
 * @throws ToolException パラメータが不正な場合
 */
suspend fun getWikiPage(cloudId: String, pageId: String): WikiPageResponse
```

### インラインコメント
- 複雑なロジックにのみコメントを追加
- 自明なコードにはコメント不要

## バージョン管理

### バージョン定数
```kotlin
// constants/VersionConst.kt
object VersionConst {
    const val VERSION = "0.2.28"
}
```

### Gradleでのバージョン管理
```kotlin
// build.gradle.kts
version = project.findProperty("project.version") as String? ?: "0.1.5"
```
