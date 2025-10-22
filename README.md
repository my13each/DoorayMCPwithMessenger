# Dooray MCP Server

NHN Doorayサービス用のMCP（Model Context Protocol）サーバーです。

## 主要機能

- **Wikiの管理**: Wiki閲覧、作成、編集、参照者管理
- **タスク管理**: タスク閲覧、作成、編集、ステータス変更
- **コメント管理**: タスクコメントの作成、閲覧、編集、削除
- **メッセンジャー管理**: メンバー検索、ダイレクトメッセージ、チャンネル管理、チャンネルメッセージ送信
- **📅 カレンダー管理**: カレンダー閲覧、カレンダー詳細、イベント照会、イベント詳細、新しいイベント作成
- **💾 ドライブ管理**: ドライブ一覧取得、ファイル/フォルダ一覧取得、ファイルアップロード/ダウンロード、フォルダ作成、ファイルコピー/移動
- **🔗 ドライブ共有リンク**: 共有リンク作成、取得、更新、削除 - 組織内外のユーザーと安全にファイル共有
- **JSON応答**: 規格化されたJSON形式の応答
- **例外処理**: 一貫したエラー応答の提供
- **Docker対応**: マルチプラットフォームDockerイメージの提供

## クイックスタート

### 環境変数の設定

以下の環境変数を設定する必要があります：

```bash
export DOORAY_API_KEY="your_api_key"
export DOORAY_BASE_URL="https://api.dooray.com"

# オプション: ログレベル制御
export DOORAY_LOG_LEVEL="WARN"         # DEBUG, INFO, WARN, ERROR (デフォルト: WARN)
export DOORAY_HTTP_LOG_LEVEL="WARN"    # HTTPクライアントログ (デフォルト: WARN)
```

#### ログ設定

**一般ログ (`DOORAY_LOG_LEVEL`)**

- `WARN` (デフォルト): 警告とエラーのみログ出力 - **MCP通信の安定性のため推奨**
- `INFO`: 一般情報を含むログ出力
- `DEBUG`: 詳細なデバッグ情報を含む

**HTTPログ (`DOORAY_HTTP_LOG_LEVEL`)**

- `WARN` (デフォルト): HTTPエラーのみログ出力 - **MCP通信の安定性のため推奨**
- `INFO`: 基本的なリクエスト/レスポンス情報のみログ出力
- `DEBUG`: 詳細なHTTP情報をログ出力

> ⚠️ **重要**: MCPサーバーはstdin/stdoutを通じて通信するため、すべてのログは**stderr**に出力されます。ログレベルを上げてもプロトコル通信に影響はありませんが、パフォーマンスに影響する可能性があります。

### ローカル実行

```bash
# 依存関係のインストールとビルド
./gradlew clean shadowJar

# ローカル実行 (.envファイルを使用)
./gradlew runLocal

# または直接実行
java -jar build/libs/dooray-mcp-server-0.2.1-all.jar
```

### Docker実行

```bash
# Docker Hubからイメージを取得
docker pull bifos/dooray-mcp:latest

# 環境変数と一緒に実行
docker run -e DOORAY_API_KEY="your_api_key" \
           -e DOORAY_BASE_URL="https://api.dooray.com" \
           bifos/dooray-mcp:latest
```

## Claude Desktopでの使用方法

Claude Desktop（Claude Code）でMCPサーバーを使用するには、設定ファイルに以下のように追加してください。

### 設定ファイルの場所

**macOS**: `~/Library/Application Support/Claude/claude_desktop_config.json`  
**Windows**: `%APPDATA%\Claude\claude_desktop_config.json`  
**Linux**: `~/.config/Claude/claude_desktop_config.json`

### 基本設定（推奨）

```json
{
  "mcpServers": {
    "dooray-mcp": {
      "command": "docker",
      "args": [
        "run",
        "--platform", "linux/amd64",
        "-i",
        "--rm",
        "-v", "/Users/{username}/Desktop:/host/Desktop:ro",
        "-v", "/Users/{username}/Downloads:/host/Downloads:ro",
        "-e", "DOORAY_API_KEY",
        "-e", "DOORAY_BASE_URL",
        "my13each/dooray-mcp:latest"
      ],
      "env": {
        "DOORAY_API_KEY": "{Your Dooray API Key}",
        "DOORAY_BASE_URL": "https://api.dooray.com"
      }
    }
  }
}
```

> 📁 **ファイルアップロード機能**: `-v`オプションでDesktopとDownloadsフォルダをマウントすることで、`dooray_drive_upload_file_from_path`ツールを使用してローカルファイルをDoorayドライブにアップロードできます。`{username}`は実際のユーザー名に置き換えてください。
>
> **Windowsの場合**: `/Users/{username}/Desktop`の代わりに`C:\Users\{username}\Desktop`を使用し、パスは`C:/Users/{username}/Desktop:/host/Desktop:ro`のように`/`で記述してください。

### 常に最新版を使用（オプション）

最新アップデートをすぐに反映したい場合は、`--pull=always`オプションを追加してください：

```json
{
  "mcpServers": {
    "dooray-mcp": {
      "command": "docker",
      "args": [
        "run",
        "--platform", "linux/amd64",
        "--pull=always",
        "-i",
        "--rm",
        "-v", "/Users/{username}/Desktop:/host/Desktop:ro",
        "-v", "/Users/{username}/Downloads:/host/Downloads:ro",
        "-e", "DOORAY_API_KEY",
        "-e", "DOORAY_BASE_URL",
        "my13each/dooray-mcp:latest"
      ],
      "env": {
        "DOORAY_API_KEY": "{Your Dooray API Key}",
        "DOORAY_BASE_URL": "https://api.dooray.com"
      }
    }
  }
}
```

> ⚠️ **注意**: `--pull=always`オプションは、Claude起動時に毎回最新イメージをダウンロードするため、起動時間が長くなる可能性があります。

### ローカル実行設定（Dockerを使わない方法）

Dockerを使用せずに直接ローカルでMCPサーバーを実行したい場合の設定方法です。

#### 前提条件

- **Java 21以上**: OpenJDK 21またはOracle JDK 21以上が必要
- **Git**: ソースコードのクローンに必要

#### セットアップ手順

```bash
# 1. リポジトリのクローン
git clone https://github.com/sungmin-koo-ai/DoorayMCP.git
cd DoorayMCP

# 2. 環境変数ファイルの作成
cat > .env << EOF
DOORAY_API_KEY=your_api_key_here
DOORAY_BASE_URL=https://api.dooray.com
EOF

# 3. アプリケーションのビルド
./gradlew clean shadowJar

# 4. テスト実行（オプション）
./gradlew runLocal
```

#### Claude Desktop設定（ローカル版）

```json
{
  "mcpServers": {
    "dooray-mcp-local": {
      "command": "java",
      "args": [
        "-jar",
        "/path/to/DoorayMCP/build/libs/dooray-mcp-server-0.2.1-all.jar"
      ],
      "env": {
        "DOORAY_API_KEY": "{Your Dooray API Key}",
        "DOORAY_BASE_URL": "https://api.dooray.com"
      }
    }
  }
}
```

> 💡 **ヒント**: `/path/to/DoorayMCP/`部分は実際のプロジェクトパスに置き換えてください。

#### ローカル実行のメリット

- **高速起動**: Dockerイメージのプルが不要
- **開発効率**: ソースコードの修正と即座のテストが可能
- **デバッグ**: より詳細なログとデバッグ情報の取得
- **カスタマイズ**: 必要に応じてソースコードの修正が可能

#### トラブルシューティング

**Java のバージョン確認**
```bash
java -version
# java version "21.0.x" 以上が表示される必要があります
```

**JAVA_HOME の設定（必要に応じて）**
```bash
# macOS (Homebrew)
export JAVA_HOME=/opt/homebrew/opt/openjdk@21
export PATH=$JAVA_HOME/bin:$PATH

# Linux
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk
export PATH=$JAVA_HOME/bin:$PATH
```

### Dooray API Key発行方法

1. [Dooray管理者ページ](https://dooray.com)にログイン
2. **管理 > API管理**メニューに移動
3. **新しいAPI Key作成**をクリック
4. 必要な権限を設定後、作成
5. 生成されたAPI Keyを設定ファイルの`{Your Dooray API Key}`部分に入力

## 使用可能なツール（合計45個）

### Wiki関連ツール（8個）

#### 1. dooray_wiki_list_projects

Doorayでアクセス可能なWikiプロジェクト一覧を取得します。

#### 2. dooray_wiki_list_pages

特定のDooray Wikiプロジェクトのページ一覧を取得します。

#### 3. dooray_wiki_get_page

特定のDooray Wikiページの詳細情報を取得します。

#### 4. dooray_wiki_create_page

新しいWikiページを作成します。

#### 5. dooray_wiki_update_page

既存のWikiページを編集します。

#### 6. dooray_wiki_update_page_title

Wikiページのタイトルのみを編集します。

#### 7. dooray_wiki_update_page_content

Wikiページの内容のみを編集します。

#### 8. dooray_wiki_update_page_referrers

Wikiページの参照者を編集します。

### プロジェクト関連ツール（1個）

#### 9. dooray_project_list_projects

アクセス可能なプロジェクト一覧を取得します。

### タスク関連ツール（6個）

#### 10. dooray_project_list_posts

プロジェクトのタスク一覧を取得します。

#### 11. dooray_project_get_post

特定タスクの詳細情報を取得します。

#### 12. dooray_project_create_post

新しいタスクを作成します。

#### 13. dooray_project_update_post

既存のタスクを編集します。

#### 14. dooray_project_set_post_workflow

タスクのステータス（ワークフロー）を変更します。

#### 15. dooray_project_set_post_done

タスクを完了状態に変更します。

### タスクコメント関連ツール（4個）

#### 16. dooray_project_create_post_comment

タスクにコメントを作成します。

#### 17. dooray_project_get_post_comments

タスクのコメント一覧を取得します。

#### 18. dooray_project_update_post_comment

タスクコメントを編集します。

#### 19. dooray_project_delete_post_comment

タスクコメントを削除します。

### メッセンジャー関連ツール（7個）

#### 20. dooray_messenger_search_members

Dooray組織のメンバーを検索します。名前、メール、ユーザーコードなどで検索できます。

#### 21. dooray_messenger_send_direct_message

特定メンバーに1対1ダイレクトメッセージを送信します。

#### 22. dooray_messenger_get_channels

アクセス可能なメッセンジャーチャンネル一覧を取得します。最近N ヶ月以内に更新されたチャンネルのみフィルタリングして大容量結果を防ぐことができます。

#### 23. dooray_messenger_get_simple_channels

簡易チャンネル一覧を取得します。チャンネル検索用でID、タイトル、タイプ、ステータス、更新日時、参加者数のみ含み、すべてのチャンネルを安全に取得できます。

#### 24. dooray_messenger_get_channel

特定チャンネルの詳細情報を取得します。チャンネルIDを通じて該当チャンネルのすべてのメンバー、設定などの詳細情報を確認できます。

#### 25. dooray_messenger_create_channel

新しいメッセンジャーチャンネルを作成します。（privateまたはdirectタイプ対応）

#### 26. dooray_messenger_send_channel_message

メッセンジャーチャンネルにメッセージを送信します。**メンション機能対応**: 特定ユーザーメンション `[@ユーザー名](dooray://組織ID/members/メンバーID "member")` または チャンネル全体メンション `[@Channel](dooray://組織ID/channels/チャンネルID "channel")` が使用可能。テキストに既にメンション形式が含まれている場合は重複を自動的に防ぎます。

> ⚠️ **注意**: チャンネルメッセージの取得は、Dooray APIでセキュリティ上の理由により対応していません。

### 📅 カレンダー関連ツール（5個）

#### 25. dooray_calendar_list

Doorayでアクセス可能なカレンダー一覧を取得します。カレンダーIDを確認したり、使用可能なカレンダーを確認する際に使用します。

#### 26. dooray_calendar_detail

特定のカレンダーの詳細情報を取得します。カレンダーメンバー一覧、権限情報（👑所有者、🤝委任者、✏️編集者など）、委任情報を確認できます。

#### 27. dooray_calendar_events

指定された期間のカレンダーイベント（予定）一覧を取得します。特定の日付や期間の予定を確認する際に使用します。timeMin、timeMaxパラメータでISO 8601形式の日時を指定し、特定のカレンダーのみをフィルタリングすることも可能です。

**新機能**: postType（参加者フィルタ）とcategory（カテゴリフィルタ）パラメータで詳細フィルタリングが可能
- postType: `toMe`（自分宛て）、`toCcMe`（自分宛て+参照）、`fromToCcMe`（すべて関連）
- category: `general`（一般予定）、`post`（タスク）、`milestone`（マイルストーン）

#### 28. dooray_calendar_event_detail

特定のカレンダーイベント（予定）の詳細情報を取得します。👑主催者、✅参加者、📋参照者の詳細情報と参加状況（参加/不参加/未定/未確認）を確認できます。会議の参加者を詳しく確認する際に役立ちます。

#### 29. dooray_calendar_create_event

新しいカレンダーイベント（予定）を作成します。会議、約束などの予定を登録する際に使用します。タイトル、内容、開始時間、終了時間、場所、参加者、参照者などを設定でき、終日予定オプションにも対応しています。

### 💾 ドライブ関連ツール（17個）

#### 30. dooray_drive_list

Doorayでアクセス可能なドライブ一覧を取得します。利用可能なドライブのIDと名前、権限情報を確認できます。

#### 31. dooray_drive_list_files

特定のドライブの**ファイルとフォルダ一覧**を取得します。parent_idを指定してフォルダを階層別に探索することができます。各ファイルの詳細情報（サイズ、作成日時、更新日時、MIME タイプ、作成者など）を含んでいます。

#### 31. dooray_drive_upload_file_from_path ⭐**優先使用**

**ローカルファイルパスから直接ファイルをアップロード**します。すべてのファイルアップロードに推奨される方法です。

📌 **特徴:**
- ファイルパスを指定するだけで自動的にBase64エンコード（サーバー側で処理）
- **Claudeのメッセージ長制限を回避** - 大容量ファイルも問題なくアップロード可能
- MIMEタイプの自動検出（25種類以上対応）
- ファイルサイズ制限: 100MB
- Docker環境で自動パス変換対応（`/Users/{user}/Downloads` → `/host/Downloads`）

📋 **使用例:**
```
/Users/username/Downloads/report.xlsx をDoorayドライブにアップロードしてください
```

#### 32. dooray_drive_upload_file 🔄**フォールバック**

Base64エンコードされたファイルをアップロードします。**`dooray_drive_upload_file_from_path`が失敗した場合のバックアップ方法です。**

⚠️ **使用制限:**
- 小さなファイル（10KB未満推奨）専用
- 大きなファイルはClaudeのメッセージ長制限（約200K文字）に達します

📌 **使用すべき場合:**
- `dooray_drive_upload_file_from_path`でファイルが見つからない場合
- 既にBase64エンコード済みのデータがある場合
- ファイルパスが利用できない特殊なケース

#### 33. dooray_drive_download_file

ドライブから**ファイルをダウンロード**します。指定したファイルの内容をBase64でエンコードして返します。テキストファイル、画像、PDF等あらゆる形式のファイルをダウンロードできます。

#### 34. dooray_drive_get_file_metadata

ドライブ **ファイルの詳細メタ情報**を取得します。ファイルのバージョン、リビジョン、作成者、最終更新者、即座情報、親フォルダ経路、즐겨찾기状態などを確인할 수 있습니다。

#### 35. dooray_drive_update_file

既存ドライブファイルを**新しいバージョンで更新**します。Base64でエンコードされた新しい内容で既存ファイルを上書きし、バージョン管理機能を利用できます。

#### 36. dooray_drive_move_file_to_trash

ドライブファイルを**ゴミ箱に移動**します。ゴミ箱に移動されたファイルは復元または永久削除が可能です。

#### 37. dooray_drive_delete_file

**ゴミ箱にあるファイルを永久削除**します。永久削除されたファイルは復元不可能です。

#### 38. dooray_drive_create_folder

**ドライブに新しいフォルダを作成**します。親フォルダID、フォルダ名を指定してフォルダを作成できます。

#### 39. dooray_drive_copy_file

**ドライブファイルを別の場所にコピー**します。同じドライブ内または別のドライブへのコピーをサポートします。

#### 40. dooray_drive_move_file

**ドライブファイルを別のフォルダに移動**します。ファイルの場所を変更する際に使用します。

### 🔗 ドライブ共有リンク関連ツール（5個）

#### 41. dooray_drive_create_shared_link

**ファイルの共有リンクを作成**します。共有範囲（組織内/外部含む）と有効期限を指定できます。

📌 **共有範囲オプション:**
- `member`: ゲストを除く組織内ユーザー
- `memberAndGuest`: 組織内すべてのユーザー（メンバー+ゲスト）
- `memberAndGuestAndExternal`: 内外部問わず誰でも

📌 **権限**: プロジェクト管理者と作成者のみ作成可能

#### 42. dooray_drive_get_shared_links

**ファイルに作成されたすべての共有リンクを取得**します。管理者はすべてのリンクを、一般ユーザーは自分が作成したリンクのみ確認できます。有効なリンクまたは期限切れリンクをフィルタリングして取得可能です。

#### 43. dooray_drive_get_shared_link_detail

**特定の共有リンクの詳細情報を取得**します。リンクID、作成日時、有効期限、作成者情報、実際の共有リンクURL、共有範囲を確認できます。

#### 44. dooray_drive_update_shared_link

**特定の共有リンクを更新**します。有効期限と共有範囲を変更できます。

#### 45. dooray_drive_delete_shared_link

**特定の共有リンクを削除**します。削除されたリンクではファイルにアクセスできなくなり、削除操作は元に戻せません。

> 💡 **技術仕様**: Dooray Drive APIの307リダイレクト フローに対応。初期リクエストを `api.dooray.com` に送信し、307応答から `file-api.dooray.com` へのリダイレクトURLを取得して実際の作業を実行します。

## 使用例

### Wikiページ取得

```json
{
  "name": "dooray_wiki_list_projects",
  "arguments": {
    "page": 0,
    "size": 20
  }
}
```

### タスク作成

```json
{
  "name": "dooray_project_create_post",
  "arguments": {
    "project_id": "your_project_id",
    "subject": "新しいタスク",
    "body": "タスク内容",
    "to_member_ids": ["member_id_1", "member_id_2"],
    "priority": "high"
  }
}
```

### コメント作成

```json
{
  "name": "dooray_project_create_post_comment",
  "arguments": {
    "project_id": "your_project_id",
    "post_id": "your_post_id",
    "content": "コメント内容",
    "mime_type": "text/x-markdown"
  }
}
```

### メンバー検索

```json
{
  "name": "dooray_messenger_search_members",
  "arguments": {
    "name": "田中太郎",
    "size": 10
  }
}
```

### ダイレクトメッセージ送信

```json
{
  "name": "dooray_messenger_send_direct_message",
  "arguments": {
    "organization_member_id": "member_id_from_search",
    "text": "こんにちは！メッセージ送信テストです。"
  }
}
```

### 簡易チャンネル一覧取得

```json
{
  "name": "dooray_messenger_get_simple_channels",
  "arguments": {
    "recentMonths": 3,
    "size": 50
  }
}
```

### 特定チャンネル詳細取得

```json
{
  "name": "dooray_messenger_get_channel",
  "arguments": {
    "channelId": "channel_id_here"
  }
}
```

### チャンネル作成

```json
{
  "name": "dooray_messenger_create_channel",
  "arguments": {
    "type": "private",
    "title": "新プロジェクトチャンネル",
    "member_ids": ["member_id_1", "member_id_2"],
    "capacity": "50"
  }
}
```

### チャンネルメッセージ送信

**基本メッセージ:**
```json
{
  "name": "dooray_messenger_send_channel_message",
  "arguments": {
    "channel_id": "channel_id_from_list",
    "text": "チャンネルにメッセージを送信します。"
  }
}
```

**特定ユーザーメンション:**
```json
{
  "name": "dooray_messenger_send_channel_message",
  "arguments": {
    "channel_id": "channel_id_here",
    "text": "お疲れ様です。\n資料について、午後に整理してお共有する予定です。\nよろしくお願いいたします。",
    "mention_members": [
      {
        "id": "member_id_here", 
        "name": "田中太郎",
        "organizationId": "organization_id_here"
      }
    ]
  }
}
```

**送信されるメッセージ形式:**
```
[@田中太郎](dooray://organization_id_here/members/member_id_here "member")
お疲れ様です。
資料について、午後に整理してお共有する予定です。
よろしくお願いいたします。
```

**チャンネル全体メンション:**
```json
{
  "name": "dooray_messenger_send_channel_message",
  "arguments": {
    "channel_id": "channel_id_here", 
    "text": "重要なお知らせです。",
    "mention_all": true
  }
}
```

### 📅 カレンダー一覧取得

```json
{
  "name": "dooray_calendar_list",
  "arguments": {}
}
```

### 📅 カレンダー詳細取得

```json
{
  "name": "dooray_calendar_detail",
  "arguments": {
    "calendarId": "calendar_id_here"
  }
}
```

### 📅 カレンダーイベント取得（フィルタリング対応）

```json
{
  "name": "dooray_calendar_events",
  "arguments": {
    "timeMin": "2025-04-11T00:00:00+09:00",
    "timeMax": "2025-04-12T00:00:00+09:00",
    "calendars": "calendar_id_1,calendar_id_2",
    "postType": "toMe",
    "category": "general"
  }
}
```

### 📅 イベント詳細取得（参加者情報込み）

```json
{
  "name": "dooray_calendar_event_detail",
  "arguments": {
    "calendarId": "calendar_id_here",
    "eventId": "event_id_here"
  }
}
```

### 📅 新しいイベント作成

```json
{
  "name": "dooray_calendar_create_event",
  "arguments": {
    "calendarId": "calendar_id_here",
    "subject": "チームミーティング",
    "content": "週次進捗会議です。",
    "startedAt": "2025-04-11T14:00:00+09:00",
    "endedAt": "2025-04-11T15:00:00+09:00",
    "location": "会議室A",
    "wholeDayFlag": false
  }
}
```

### 💾 ドライブ一覧取得

```json
{
  "name": "dooray_drive_list",
  "arguments": {}
}
```

### 💾 ドライブファイル一覧取得

```json
{
  "name": "dooray_drive_list_files",
  "arguments": {
    "drive_id": "drive_id_here",
    "parent_id": "folder_id_here",
    "size": 50
  }
}
```

### 💾 ファイルアップロード (パスから) ⭐ **優先使用**

すべてのファイルアップロードに推奨される方法です。

```json
{
  "name": "dooray_drive_upload_file_from_path",
  "arguments": {
    "drive_id": "drive_id_here",
    "file_path": "/Users/username/Downloads/report.xlsx",
    "parent_id": "folder_id_here",
    "mime_type": "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
  }
}
```

> ✅ **推奨理由**:
> - Claudeのメッセージ長制限を回避
> - サーバー側でBase64エンコード処理
> - 大容量ファイル（画像、Excel、PDF等）も問題なく処理
> - Docker環境で自動パス変換対応

### 💾 ファイルアップロード (Base64) 🔄 **フォールバック**

Base64エンコード済みのファイルをアップロードします。`dooray_drive_upload_file_from_path`が失敗した場合のバックアップ方法です。

```json
{
  "name": "dooray_drive_upload_file",
  "arguments": {
    "drive_id": "drive_id_here",
    "file_name": "example.txt",
    "base64_content": "SGVsbG8gV29ybGQ=",
    "parent_id": "folder_id_here",
    "mime_type": "text/plain"
  }
}
```

> ⚠️ **注意事項**:
> - 小さなファイル（10KB未満推奨）専用
> - 大きなファイルはClaudeのメッセージ長制限に達します
> - 優先的に `dooray_drive_upload_file_from_path` を使用してください

### 💾 ファイルダウンロード

```json
{
  "name": "dooray_drive_download_file",
  "arguments": {
    "drive_id": "drive_id_here",
    "file_id": "file_id_here"
  }
}
```

### 💾 ファイルメタデータ取得

```json
{
  "name": "dooray_drive_get_file_metadata",
  "arguments": {
    "drive_id": "drive_id_here",
    "file_id": "file_id_here"
  }
}
```

### 💾 ファイル更新

```json
{
  "name": "dooray_drive_update_file",
  "arguments": {
    "drive_id": "drive_id_here",
    "file_id": "file_id_here",
    "file_name": "updated_example.txt",
    "base64_content": "VXBkYXRlZCBIZWxsbyBXb3JsZA==",
    "mime_type": "text/plain"
  }
}
```

### 💾 ファイルをゴミ箱に移動

```json
{
  "name": "dooray_drive_move_to_trash",
  "arguments": {
    "drive_id": "drive_id_here",
    "file_id": "file_id_here"
  }
}
```

### 💾 ファイル永久削除

```json
{
  "name": "dooray_drive_delete_file",
  "arguments": {
    "drive_id": "drive_id_here",
    "file_id": "file_id_here"
  }
}
```

### 💾 フォルダ作成

```json
{
  "name": "dooray_drive_create_folder",
  "arguments": {
    "drive_id": "drive_id_here",
    "parent_folder_id": "folder_id_here",
    "folder_name": "新しいフォルダ"
  }
}
```

### 💾 ファイルコピー

```json
{
  "name": "dooray_drive_copy_file",
  "arguments": {
    "drive_id": "source_drive_id_here",
    "file_id": "file_id_here",
    "destination_drive_id": "target_drive_id_here",
    "destination_folder_id": "target_folder_id_here"
  }
}
```

### 💾 ファイル移動

```json
{
  "name": "dooray_drive_move_file",
  "arguments": {
    "drive_id": "drive_id_here",
    "file_id": "file_id_here",
    "destination_folder_id": "target_folder_id_here"
  }
}
```

### 🔗 共有リンク作成

```json
{
  "name": "dooray_drive_create_shared_link",
  "arguments": {
    "drive_id": "drive_id_here",
    "file_id": "file_id_here",
    "scope": "memberAndGuestAndExternal",
    "expired_at": "2025-12-31T23:59:59+09:00"
  }
}
```

### 🔗 共有リンク一覧取得

```json
{
  "name": "dooray_drive_get_shared_links",
  "arguments": {
    "drive_id": "drive_id_here",
    "file_id": "file_id_here",
    "valid": true
  }
}
```

### 🔗 共有リンク詳細取得

```json
{
  "name": "dooray_drive_get_shared_link_detail",
  "arguments": {
    "drive_id": "drive_id_here",
    "file_id": "file_id_here",
    "link_id": "link_id_here"
  }
}
```

### 🔗 共有リンク更新

```json
{
  "name": "dooray_drive_update_shared_link",
  "arguments": {
    "drive_id": "drive_id_here",
    "file_id": "file_id_here",
    "link_id": "link_id_here",
    "expired_at": "2026-12-31T23:59:59+09:00",
    "scope": "memberAndGuest"
  }
}
```

### 🔗 共有リンク削除

```json
{
  "name": "dooray_drive_delete_shared_link",
  "arguments": {
    "drive_id": "drive_id_here",
    "file_id": "file_id_here",
    "link_id": "link_id_here"
  }
}
```

## 開発

### テスト実行

```bash
# すべてのテストを実行（環境変数がある場合）
./gradlew test

# CI環境では統合テストを自動除外
CI=true ./gradlew test
```

### ビルド

```bash
# JARビルド
./gradlew clean shadowJar

# Dockerイメージビルド
docker build -t dooray-mcp:local --build-arg VERSION=0.2.1 .
```

## Dockerマルチプラットフォームビルド

### 現在の状況

現在のDockerイメージは**AMD64のみ対応**しています。ARM64ビルドはQEMUエミュレーションでGradle依存関係ダウンロード段階で停止する問題があり、一時的に無効化されています。

### ARM64ビルド有効化

ARM64ビルドを再度有効化するには、`.github/workflows/docker-publish.yml`で以下の設定を変更してください：

```yaml
env:
  ENABLE_ARM64: true # falseからtrueに変更
```

### ARM64ビルド問題解決方法

1. **ネイティブARM64ランナー使用**（推奨）
2. **QEMUタイムアウト増加**
3. **Gradleキャッシュ最適化**
4. **依存関係事前ダウンロード**

現在は安定性のためAMD64のみビルドしており、ARM64対応は今後のアップデートで提供予定です。

## 環境変数

| 変数名          | 説明                | 必須 |
| --------------- | ------------------- | ---- |
| DOORAY_API_KEY  | Dooray API キー     | 必須 |
| DOORAY_BASE_URL | Dooray API Base URL | 必須 |

## ライセンス

このプロジェクトはオープンソースであり、自由にご利用いただけます。

## 貢献

プロジェクトに貢献したい場合は、issueを登録するかpull requestを送ってください。

## 📚 参考資料

- [Dooray API](https://helpdesk.dooray.com/share/pages/9wWo-xwiR66BO5LGshgVTg/2939987647631384419)
- [Kotlin MCP Server サンプル](https://github.com/modelcontextprotocol/kotlin-sdk/blob/main/samples/weather-stdio-server/src/main/kotlin/io/modelcontextprotocol/sample/server/McpWeatherServer.kt)
- [Model Context Protocol](https://modelcontextprotocol.io/introduction)
