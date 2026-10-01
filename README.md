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

# オプション: ツールカテゴリフィルタリング 🆕
export DOORAY_ENABLED_CATEGORIES="wiki,project"  # 有効にするカテゴリをカンマ区切りで指定
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

#### ツールカテゴリフィルタリング 🆕

`DOORAY_ENABLED_CATEGORIES`環境変数で、使用するツールのカテゴリを制限できます。

**利用可能なカテゴリ:**

- `wiki` - Wiki関連ツール (5個)
- `project` - プロジェクト・タスク・コメント関連ツール (11個)
- `messenger` - メッセンジャー関連ツール (17個)
- `calendar` - カレンダー関連ツール (5個)
- `drive` - ドライブ・ファイル・共有リンク関連ツール (20個)

**使用例:**

```bash
# Wikiとプロジェクトツールのみ有効化
export DOORAY_ENABLED_CATEGORIES="wiki,project"

# メッセンジャーのみ有効化
export DOORAY_ENABLED_CATEGORIES="messenger"

# すべてのツールを有効化（デフォルト）
export DOORAY_ENABLED_CATEGORIES=""  # または変数を設定しない
```

**メリット:**

- 不要なツールを非表示にして、Claudeの応答精度を向上
- 使用するツールを限定してMCPサーバーの起動を高速化
- 複数のプロファイルを使い分けて効率的に作業

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
docker pull my13each/dooray-mcp:latest

# 環境変数と一緒に実行
docker run -e DOORAY_API_KEY="your_api_key" \
           -e DOORAY_BASE_URL="https://api.dooray.com" \
           my13each/dooray-mcp:latest
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
        "-v", "/Users/{username}/Downloads:/host/Downloads:rw",
        "-v", "/Users/{username}/Downloads:/home/claude:rw",
        "-v", "/tmp:/tmp:rw",
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

### カテゴリフィルタリング設定例 🆕

特定のツールカテゴリのみを使用したい場合：

```json
{
  "mcpServers": {
    "dooray-wiki": {
      "command": "docker",
      "args": [
        "run", "--platform", "linux/amd64", "-i", "--rm",
        "-e", "DOORAY_API_KEY",
        "-e", "DOORAY_BASE_URL",
        "-e", "DOORAY_ENABLED_CATEGORIES",
        "my13each/dooray-mcp:latest"
      ],
      "env": {
        "DOORAY_API_KEY": "{Your Dooray API Key}",
        "DOORAY_BASE_URL": "https://api.dooray.com",
        "DOORAY_ENABLED_CATEGORIES": "wiki,project"
      }
    },
    "dooray-messenger": {
      "command": "docker",
      "args": [
        "run", "--platform", "linux/amd64", "-i", "--rm",
        "-e", "DOORAY_API_KEY",
        "-e", "DOORAY_BASE_URL",
        "-e", "DOORAY_ENABLED_CATEGORIES",
        "my13each/dooray-mcp:latest"
      ],
      "env": {
        "DOORAY_API_KEY": "{Your Dooray API Key}",
        "DOORAY_BASE_URL": "https://api.dooray.com",
        "DOORAY_ENABLED_CATEGORIES": "messenger"
      }
    }
  }
}
```

> 💡 **ヒント**: 複数のMCPサーバーを登録することで、用途に応じて使い分けることができます。

> 📁 **ファイルアップロード機能**: `-v`オプションでDesktopとDownloadsフォルダをマウントすることで、`dooray_drive_upload_file_from_path`ツールを使用してローカルファイルをDoorayドライブにアップロードできます。
>
> **マウント設定の説明:**
> - `/host/Desktop:ro` - Desktopフォルダを読み取り専用でマウント
> - `/host/Downloads:rw` - Downloadsフォルダを読み書き可能でマウント
> - `/home/claude:rw` - Claude Desktopの作業ディレクトリ（Claudeがファイル生成時に使用）
> - `/tmp:rw` - 一時ファイルディレクトリ（Claudeが一時ファイル作成時に使用）
>
> **重要:** `/home/claude`と`/tmp`のマウントにより、Claudeが生成したExcel、CSV等のファイルを直接アップロード可能です。
>
> `{username}`は実際のユーザー名に置き換えてください。
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
        "-v", "/Users/{username}/Downloads:/host/Downloads:rw",
        "-v", "/Users/{username}/Downloads:/home/claude:rw",
        "-v", "/tmp:/tmp:rw",
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
git clone https://github.com/my13each/DoorayMCPwithMessenger.git
cd DoorayMCPwithMessenger

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

## 使用可能なツール（合計58個）

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

**🚀 v0.2.28の最適化:**
- **担当者情報の追加軽量化**: 名前とIDのみ含む（ワークフロー/type/null値を除外）
- **トークン使用量削減**: 担当者1人あたり約80%削減（例: 3人の場合 ~600→~120トークン）

**🚀 v0.2.27の最適化:**
- **応答の軽量化**: 担当者のみ含む（参照者/作成者は除外）
- **高速レスポンス**: 出力トークン制限を回避し、大量データにも対応

**📋 返却される情報:**
- 基本情報: ID、タイトル、タスク番号、ステータス、優先度、期限
- 担当者: 名前とID（organizationMemberId, name）のみ

**📝 応答例:**
```json
{
  "assignees": [
    {"organizationMemberId": "1234567890", "name": "山田太郎"},
    {"organizationMemberId": "0987654321", "name": "佐藤花子"}
  ]
}
```

**💡 詳細情報が必要な場合は `dooray_project_get_post` を使用してください。**

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

### メッセンジャー関連ツール（17個）

#### 20. dooray_messenger_search_members

Dooray組織のメンバーを検索します。名前、メール（カンマ区切りで最大10個）、ユーザーコード、IDプロバイダーのユーザーID（社員番号など、`id_provider_user_id`）で検索できます。

#### 21. dooray_messenger_send_direct_message

特定メンバーに1対1ダイレクトメッセージを送信します。

#### 22. dooray_messenger_get_member 🆕

メンバーID（organizationMemberId）からメンバーの詳細情報（名前、英語名、ユーザーコード、メールなど）を取得します。メッセージの送信者が誰かを確認する時に使います。

#### 23. dooray_messenger_get_channels

アクセス可能なメッセンジャーチャンネル一覧を取得します。最近N ヶ月以内に更新されたチャンネルのみフィルタリングして大容量結果を防ぐことができます。

#### 24. dooray_messenger_get_simple_channels

簡易チャンネル一覧を取得します。チャンネル検索用でID、タイトル、タイプ、ステータス、更新日時、参加者数のみ含み、すべてのチャンネルを安全に取得できます。

#### 25. dooray_messenger_get_channel

特定チャンネルの詳細情報を取得します。チャンネルIDを通じて該当チャンネルのすべてのメンバー、設定などの詳細情報を確認できます。

#### 26. dooray_messenger_get_channel_logs 🆕

チャンネルの**メッセージを取得**します（最新から最大 `size` 件）。

**パラメータ:**
- `channelId`: チャンネルID（必須）
- `size`: 取得件数（1〜1000、デフォルト: 50）
- `include_sender_names`: 送信者名（`senderName`）を付けるか（デフォルト: true）

**応答の特徴:**
- メッセージは `seq` の昇順（古い → 新しい）
- ボットのメッセージは `sender.type=app`、`sender.app.appId`
- 返信（`type=REPLY`）は `text` に本文だけを入れ、元のJSONは `rawText` に残します
- 添付ファイルがある場合は `file`（`file.id` でダウンロード可能）

> ⚠️ **制限:**
> - 公式ドキュメントに載っていないAPIです（動作確認済み: 2026-10-01）
> - ページングに対応していないため、最新1000件より古いメッセージは取得できません
> - スレッドはスレッドチャンネルIDを `channelId` に渡せば読めますが、親メッセージにスレッド情報が無いため、IDは `create_thread` 系ツールの応答からしか分かりません

#### 27. dooray_messenger_create_channel

新しいメッセンジャーチャンネルを作成します。（privateまたはdirectタイプ対応）

#### 28. dooray_messenger_send_channel_message

メッセンジャーチャンネルにメッセージを送信します。

**✨ メンション機能対応:**
- **特定ユーザーメンション**: `[@ユーザー名](dooray://組織ID/members/メンバーID "member")`
- **チャンネル全体メンション**: `[@Channel](dooray://組織ID/channels/チャンネルID "channel")`

**🎯 インラインメンション位置保持（v0.2.30）:**

Claudeが文章内に自然に配置したメンションは**元の位置に保持**されます。

```
✅ 正しい動作:
入力: "1時間後に [@具成珉](dooray://...) と会議が準備されています"
出力: "1時間後に [@具成珉](dooray://...) と会議が準備されています"

❌ 以前の動作:
出力: "[@具成珉](dooray://...)\n1時間後に と会議が準備されています"  ← メンションが先頭に移動してしまい、文章が不自然
```

**📌 メンション方法:**
1. **自然な文章内メンション（推奨）**: Claudeが文脈に合わせてメンションを配置
   - 例: "整理した資料を [@金哲秀](dooray://...) に送りました。[@李英姫](dooray://...) も確認お願いします〜"
   - メンションは文中の元の位置に保持されます ✅

2. **パラメータによるメンション追加**: `mention_members`パラメータ使用時
   - メンションはメッセージの**先頭**に追加されます
   - テキストに既にメンション形式が含まれている場合は重複を自動的に防ぎます

> 💡 スレッドチャンネルID（`create_thread` の応答の `threadId`）を `channel_id` に指定すると、そのスレッドに続けて送信できます。

#### 29. dooray_messenger_join_channel

メッセンジャーチャンネルに**メンバーを追加**します。既存のチャンネルに新しいメンバーを招待する際に使用します。複数のメンバーを一度に追加することが可能です。

**パラメータ:**
- `channel_id`: メンバーを追加するチャンネルのID（必須）
- `member_ids`: 追加するメンバーIDの配列（必須）

#### 30. dooray_messenger_leave_channel

メッセンジャーチャンネルから**メンバーを削除**します。チャンネルからメンバーを退出させる際に使用します。複数のメンバーを一度に削除することが可能です。

**パラメータ:**
- `channel_id`: メンバーを削除するチャンネルのID（必須）
- `member_ids`: 削除するメンバーIDの配列（必須）

#### 31. dooray_messenger_create_thread

メッセンジャーチャンネルに**スレッドを作成し、最初のメッセージを送信**します。特定のメッセージに対する返信や関連する会話を整理する際に便利です。スレッド機能により、チャンネル内の会話を構造化して管理できます。

**パラメータ:**
- `channel_id`: スレッドを作成するチャンネルのID（必須）
- `text`: チャンネルに送るメッセージ（必須）。このメッセージにスレッドが付きます
- `thread_text`: スレッドの最初のメッセージ（オプション）。省略するとDoorayが `#` を入れます 🆕
- `message_type`: メッセージタイプ（オプション、デフォルト: "text"）

**応答:**
- `threadId`: スレッドチャンネルのID（`send_channel_message` / `get_channel_logs` に使えます）
- `logId`: スレッド内に送信されたメッセージのログID

#### 32. dooray_messenger_update_message

既存のメッセンジャーメッセージを**編集**します。送信済みのメッセージの内容を修正する際に使用します。メッセージの履歴は保持され、編集されたことが記録されます。

**パラメータ:**
- `channel_id`: メッセージがあるチャンネルのID（必須）
- `log_id`: 編集するメッセージのログID（必須）
- `text`: 新しいメッセージ内容（必須）
- `message_type`: メッセージタイプ（オプション、デフォルト: "text"）

#### 33. dooray_messenger_delete_message

メッセンジャーメッセージを**削除**します。不要なメッセージや誤って送信したメッセージを削除する際に使用します。削除されたメッセージは復元できません。

**パラメータ:**
- `channel_id`: メッセージがあるチャンネルのID（必須）
- `log_id`: 削除するメッセージのログID（必須）

#### 34. dooray_messenger_reply_message 🆕

特定のメッセージ（`log_id`）に**返信**します。元のメッセージを引用した形で同じチャンネルに送信されます。

**パラメータ:**
- `channel_id`: チャンネルID（必須）
- `log_id`: 返信先メッセージのログID（必須）
- `text`: 返信内容（必須）

#### 35. dooray_messenger_create_thread_from_message 🆕

既存のメッセージ（`log_id`）に**スレッドを作成**してメッセージを送信します。新しいメッセージとスレッドを同時に作る場合は `create_thread` を使います。

**パラメータ:**
- `channel_id`: チャンネルID（必須）
- `log_id`: スレッドを付けるメッセージのログID（必須）
- `text`: スレッドに送るメッセージ（必須）

**応答:** `sentChannelId` がスレッドチャンネルIDです。

#### 36. dooray_messenger_download_file 🆕

メッセージの**添付ファイルをダウンロード**してローカルに保存します。

**パラメータ:**
- `channel_id`: チャンネルID（必須）
- `file_id`: 添付ファイルID（`get_channel_logs` の `file.id`、必須）
- `save_dir`: 保存先フォルダ（オプション、デフォルト: Downloads。Docker実行時は `/host/Downloads` = ホストの `~/Downloads`）

テキストファイル（50KB以下）は内容も応答に含まれます。

> ⚠️ メッセンジャーの添付ファイルはアップロードから約2週間で期限切れになります。

### 📅 カレンダー関連ツール（5個）

#### 37. dooray_calendar_list

Doorayでアクセス可能なカレンダー一覧を取得します。カレンダーIDを確認したり、使用可能なカレンダーを確認する際に使用します。

#### 38. dooray_calendar_detail

特定のカレンダーの詳細情報を取得します。カレンダーメンバー一覧、権限情報（👑所有者、🤝委任者、✏️編集者など）、委任情報を確認できます。

#### 39. dooray_calendar_events

指定された期間のカレンダーイベント（予定）一覧を取得します。特定の日付や期間の予定を確認する際に使用します。timeMin、timeMaxパラメータでISO 8601形式の日時を指定し、特定のカレンダーのみをフィルタリングすることも可能です。

**新機能**: postType（参加者フィルタ）とcategory（カテゴリフィルタ）パラメータで詳細フィルタリングが可能
- postType: `toMe`（自分宛て）、`toCcMe`（自分宛て+参照）、`fromToCcMe`（すべて関連）
- category: `general`（一般予定）、`post`（タスク）、`milestone`（マイルストーン）

#### 40. dooray_calendar_event_detail

特定のカレンダーイベント（予定）の詳細情報を取得します。👑主催者、✅参加者、📋参照者の詳細情報と参加状況（参加/不参加/未定/未確認）を確認できます。会議の参加者を詳しく確認する際に役立ちます。

#### 41. dooray_calendar_create_event

新しいカレンダーイベント（予定）を作成します。会議、約束などの予定を登録する際に使用します。タイトル、内容、開始時間、終了時間、場所、参加者、参照者などを設定でき、終日予定オプションにも対応しています。

### 💾 ドライブ関連ツール（20個）

#### 42. dooray_drive_list

Doorayでアクセス可能なドライブ一覧を取得します。利用可能なドライブのIDと名前、権限情報を確認できます。

**🆕 フィルタリング機能（v0.2.21）:**
- `project_id`: 特定プロジェクトのドライブのみ取得
- `type`: `private`（個人ドライブ）または `project`（プロジェクトドライブ）
- `scope`: `own`（自分のドライブ）または `all`（すべてのドライブ）
- `state`: `active`（アクティブ）または `inactive`（非アクティブ）

#### 43. dooray_drive_get_detail

特定のドライブの詳細情報を取得します。ドライブタイプ（個人/プロジェクト）、メンバー一覧、役割などを確認できます。

**🆕 新機能（v0.2.22）:** ドライブメンバー管理と権限確認が可能になりました。

#### 44. dooray_drive_list_files

特定のドライブの**ファイルとフォルダ一覧**を取得します。parent_idを指定してフォルダを階層別に探索することができます。各ファイルの詳細情報（サイズ、作成日時、更新日時、MIME タイプ、作成者など）を含んでいます。

**🆕 フィルタリング機能（v0.2.23）:**
- `type`: `folder`（フォルダのみ）または `file`（ファイルのみ）
- `sub_types`: サブタイプフィルタ（カンマ区切り）
  - フォルダ: `root`, `trash`, `users`
  - ファイル: `etc`, `doc`, `photo`, `movie`, `music`, `zip`

#### 45. dooray_drive_get_changes

**ドライブ内の変更履歴を取得**します。ファイル/フォルダの作成、更新、削除の履歴を追跡できます。変更タイプ（updated/deleted）、リビジョン番号、ファイル情報を確認できます。

**🆕 新機能（v0.2.25）:**
- `latest_revision`: 変更履歴の基準点（デフォルト: 0）
- `file_id`: 特定ファイル以降の変更のみ取得
- `size`: 取得件数（デフォルト: 20、最大: 200）

**📌 使用例:**
- リビジョン管理: 最後に確認したリビジョン以降の変更のみ取得
- 同期機能: 変更内容を追跡してファイル同期実装
- 監査ログ: ドライブ内のすべての変更履歴を記録

#### 46. dooray_drive_upload_file_from_path ⭐**優先使用**

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

#### 47. dooray_drive_upload_file 🔄**フォールバック**

Base64エンコードされたファイルをアップロードします。**`dooray_drive_upload_file_from_path`が失敗した場合のバックアップ方法です。**

⚠️ **使用制限:**
- 小さなファイル（10KB未満推奨）専用
- 大きなファイルはClaudeのメッセージ長制限（約200K文字）に達します

📌 **使用すべき場合:**
- `dooray_drive_upload_file_from_path`でファイルが見つからない場合
- 既にBase64エンコード済みのデータがある場合
- ファイルパスが利用できない特殊なケース

#### 48. dooray_drive_download_file

ドライブから**ファイルをダウンロード**します。指定したファイルの内容をBase64でエンコードして返します。テキストファイル、画像、PDF等あらゆる形式のファイルをダウンロードできます。

#### 49. dooray_drive_get_file_metadata

ドライブ **ファイルの詳細メタ情報**を取得します。ファイルのバージョン、リビジョン、作成者、最終更新者、注釈情報、親フォルダ経路、お気に入り状態などを確認できます。

#### 50. dooray_drive_rename_file

ドライブ内の**ファイルまたはフォルダの名前を変更**します。ファイルの拡張子変更やフォルダ名の修正が可能です。

**🆕 新機能（v0.2.24）:**
- PUT /drive/v1/drives/{drive-id}/files/{file-id}?media=meta APIを使用
- ファイル名変更: `document.txt` → `report.txt`
- 拡張子変更: `image.png` → `image.jpg`
- フォルダ名変更: `old_folder` → `new_folder`

#### 51. dooray_drive_update_file

既存ドライブファイルを**新しいバージョンで更新**します。Base64でエンコードされた新しい内容で既存ファイルを上書きし、バージョン管理機能を利用できます。

#### 52. dooray_drive_move_file_to_trash

ドライブファイルを**ゴミ箱に移動**します。ゴミ箱に移動されたファイルは復元または永久削除が可能です。

#### 53. dooray_drive_delete_file

**ゴミ箱にあるファイルを永久削除**します。永久削除されたファイルは復元不可能です。

#### 54. dooray_drive_create_folder

**ドライブに新しいフォルダを作成**します。親フォルダID、フォルダ名を指定してフォルダを作成できます。

#### 55. dooray_drive_copy_file

**ドライブファイルを別の場所にコピー**します。同じドライブ内または別のドライブへのコピーをサポートします。

#### 56. dooray_drive_move_file

**ドライブファイルを別のフォルダに移動**します。ファイルの場所を変更する際に使用します。

### 🔗 ドライブ共有リンク関連ツール（5個）

#### 57. dooray_drive_create_shared_link

**ファイルの共有リンクを作成**します。共有範囲（組織内/外部含む）と有効期限を指定できます。

📌 **共有範囲オプション:**
- `member`: ゲストを除く組織内ユーザー
- `memberAndGuest`: 組織内すべてのユーザー（メンバー+ゲスト）
- `memberAndGuestAndExternal`: 内外部問わず誰でも

📌 **権限**: プロジェクト管理者と作成者のみ作成可能

#### 58. dooray_drive_get_shared_links

**ファイルに作成されたすべての共有リンクを取得**します。管理者はすべてのリンクを、一般ユーザーは自分が作成したリンクのみ確認できます。有効なリンクまたは期限切れリンクをフィルタリングして取得可能です。

#### 59. dooray_drive_get_shared_link_detail

**特定の共有リンクの詳細情報を取得**します。リンクID、作成日時、有効期限、作成者情報、実際の共有リンクURL、共有範囲を確認できます。

#### 60. dooray_drive_update_shared_link

**特定の共有リンクを更新**します。有効期限と共有範囲を変更できます。

#### 61. dooray_drive_delete_shared_link

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

### タスク一覧取得 🆕

```json
{
  "name": "dooray_project_list_posts",
  "arguments": {
    "project_id": "your_project_id",
    "size": 100,
    "post_workflow_classes": ["working", "registered"]
  }
}
```

**応答例（経量化）:**
```json
{
  "success": true,
  "data": [
    {
      "id": "123456789",
      "subject": "バグ修正: ログイン処理の改善",
      "taskNumber": "PROJ-456",
      "workflowClass": "working",
      "workflow": { "id": "workflow_id", "name": "進行中" },
      "assignees": [
        {
          "type": "member",
          "member": { "id": "member_id", "name": "田中太郎" }
        }
      ],
      "priority": "high",
      "dueDate": "2025-12-01T09:00:00+09:00",
      "createdAt": "2025-11-20T10:00:00+09:00",
      "updatedAt": "2025-11-27T15:30:00+09:00"
    }
  ],
  "message": "📋 プロジェクト業務一覧を正常に照会しました"
}
```

**ページング例（追加データ取得）:**

APIは1回あたり最大100件まで取得可能です。それ以上のデータが必要な場合は`page`パラメータを使用してください。

```json
// 1ページ目（0〜99件目）
{
  "name": "dooray_project_list_posts",
  "arguments": {
    "project_id": "your_project_id",
    "page": 0,
    "size": 100
  }
}

// 2ページ目（100〜199件目）
{
  "name": "dooray_project_list_posts",
  "arguments": {
    "project_id": "your_project_id",
    "page": 1,
    "size": 100
  }
}

// 3ページ目（200〜299件目）
{
  "name": "dooray_project_list_posts",
  "arguments": {
    "project_id": "your_project_id",
    "page": 2,
    "size": 100
  }
}
```

> 💡 **ヒント**: `page`を0から順に増やしながら、データが返されなくなるまで繰り返し取得できます。

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

**インラインメンション（v0.2.30）- 推奨方法:**

Claudeに自然な文章でメンションを含めるよう依頼すると、文脈に合った位置にメンションが配置されます。

```json
{
  "name": "dooray_messenger_send_channel_message",
  "arguments": {
    "channel_id": "channel_id_here",
    "text": "整理した資料を [@金哲秀](dooray://1708537451674140147/members/3926605175248762314 \"member\") に送りました。[@李英姫](dooray://1708537451674140147/members/3926605175248762315 \"member\") も確認お願いします〜"
  }
}
```

**送信されるメッセージ:**
```
整理した資料を [@金哲秀](dooray://1708537451674140147/members/3926605175248762314 "member") に送りました。[@李英姫](dooray://1708537451674140147/members/3926605175248762315 "member") も確認お願いします〜
```

> 💡 **ポイント**: メンションが文章の途中に自然に配置され、元の位置に保持されます。以前のバージョンではメンションがすべて先頭に移動していましたが、v0.2.30からは文脈を保持したまま送信されます。

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

### チャンネルメンバー追加

```json
{
  "name": "dooray_messenger_join_channel",
  "arguments": {
    "channel_id": "channel_id_here",
    "member_ids": ["member_id_1", "member_id_2", "member_id_3"]
  }
}
```

### チャンネルメンバー削除

```json
{
  "name": "dooray_messenger_leave_channel",
  "arguments": {
    "channel_id": "channel_id_here",
    "member_ids": ["member_id_to_remove"]
  }
}
```

### スレッド作成とメッセージ送信

```json
{
  "name": "dooray_messenger_create_thread",
  "arguments": {
    "channel_id": "channel_id_here",
    "text": "新しいスレッドを開始します。\nこのトピックについて話し合いましょう。",
    "message_type": "text"
  }
}
```

**応答例:**
```json
{
  "success": true,
  "data": {
    "channelId": "channel_id_here",
    "threadId": "thread_12345",
    "logId": "log_67890",
    "sentText": "新しいスレッドを開始します。\nこのトピックについて話し合いましょう。",
    "timestamp": 1703145600000
  },
  "message": "スレッドが成功的に生成され、メッセージが送信されました。"
}
```

### メッセージ編集

```json
{
  "name": "dooray_messenger_update_message",
  "arguments": {
    "channel_id": "channel_id_here",
    "log_id": "log_id_here",
    "text": "訂正: 正しい内容はこちらです。",
    "message_type": "text"
  }
}
```

### メッセージ削除

```json
{
  "name": "dooray_messenger_delete_message",
  "arguments": {
    "channel_id": "channel_id_here",
    "log_id": "log_id_here"
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

### 💾 ドライブ一覧取得（フィルタリング付き）🆕

```json
{
  "name": "dooray_drive_list",
  "arguments": {
    "project_id": "project_id_here",
    "type": "project",
    "scope": "all",
    "state": "active"
  }
}
```

### 💾 ドライブ詳細取得 🆕

```json
{
  "name": "dooray_drive_get_detail",
  "arguments": {
    "drive_id": "drive_id_here"
  }
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

### 💾 ドライブファイル一覧取得（フィルタリング付き）🆕

```json
{
  "name": "dooray_drive_list_files",
  "arguments": {
    "drive_id": "drive_id_here",
    "type": "file",
    "sub_types": "doc,photo",
    "parent_id": "folder_id_here",
    "size": 50
  }
}
```

### 💾 ドライブ変更履歴取得 🆕

```json
{
  "name": "dooray_drive_get_changes",
  "arguments": {
    "drive_id": "drive_id_here",
    "latest_revision": "100",
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

### 💾 ファイル名変更 🆕

```json
{
  "name": "dooray_drive_rename_file",
  "arguments": {
    "drive_id": "drive_id_here",
    "file_id": "file_id_here",
    "new_name": "new_filename.txt"
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

| 変数名                      | 説明                                                               | 必須       | デフォルト |
| --------------------------- | ------------------------------------------------------------------ | ---------- | ---------- |
| DOORAY_API_KEY              | Dooray API キー                                                    | 必須       | -          |
| DOORAY_BASE_URL             | Dooray API Base URL                                                | 必須       | -          |
| DOORAY_ENABLED_CATEGORIES   | 有効にするツールカテゴリ (wiki,project,messenger,calendar,drive) | オプション | すべて     |
| DOORAY_LOG_LEVEL            | 一般ログレベル (DEBUG,INFO,WARN,ERROR)                            | オプション | WARN       |
| DOORAY_HTTP_LOG_LEVEL       | HTTPログレベル (DEBUG,INFO,WARN,ERROR)                            | オプション | WARN       |

## ライセンス

このプロジェクトはオープンソースであり、自由にご利用いただけます。

## 貢献

プロジェクトに貢献したい場合は、issueを登録するかpull requestを送ってください。

## 📚 参考資料

- [Dooray API](https://helpdesk.dooray.com/share/pages/9wWo-xwiR66BO5LGshgVTg/2939987647631384419)
- [Kotlin MCP Server サンプル](https://github.com/modelcontextprotocol/kotlin-sdk/blob/main/samples/weather-stdio-server/src/main/kotlin/io/modelcontextprotocol/sample/server/McpWeatherServer.kt)
- [Model Context Protocol](https://modelcontextprotocol.io/introduction)
