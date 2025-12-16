# DoorayMCP 開発コマンド集

## ビルド関連

### Fat JAR のビルド
```bash
./gradlew clean shadowJar
```
すべての依存関係を含むfat JARを生成します。  
出力: `build/libs/dooray-mcp-server-{version}-all.jar`

## 実行関連

### ローカル実行（.envファイル使用）
```bash
./gradlew runLocal
```
- `.env`ファイルから環境変数を自動ロード
- 必要な環境変数: `DOORAY_API_KEY`, `DOORAY_BASE_URL`
- stdin/stdoutでMCPプロトコル通信

### Docker でのビルドと実行
```bash
# Docker イメージのビルド
docker build -t dooray-mcp:local --build-arg VERSION=0.2.1 .

# Docker コンテナの実行
docker run -e DOORAY_API_KEY="your_key" \
           -e DOORAY_BASE_URL="https://api.dooray.com" \
           dooray-mcp:local

# 公開イメージの取得と実行
docker pull my13each/dooray-mcp:latest
docker run -e DOORAY_API_KEY="your_key" \
           -e DOORAY_BASE_URL="https://api.dooray.com" \
           my13each/dooray-mcp:latest
```

## テスト関連

### すべてのテストを実行
```bash
./gradlew test
```
- ユニットテストと統合テストを実行
- CI環境（`CI=true`）では統合テストを自動除外

### CI環境でのテスト実行
```bash
CI=true ./gradlew test
```
統合テスト（`*IntegrationTest*`）を除外します。

## 検証関連

### ツールJSONスキーマの検証
```bash
./gradlew validateSchemas
```
すべてのツールのJSONスキーマが正しいかを検証します。

## システムユーティリティコマンド (Darwin/macOS)

### ファイル検索
```bash
find . -name "*.kt" -type f
```

### ディレクトリ一覧
```bash
ls -la
```

### ファイル内容検索
```bash
grep -r "pattern" src/
```

### Git 操作
```bash
# ステータス確認
git status

# 変更をステージング
git add .

# コミット
git commit -m "message"

# プッシュ
git push origin main
```

## ログレベル調整

### デバッグモードで実行
```bash
DOORAY_LOG_LEVEL=DEBUG DOORAY_HTTP_LOG_LEVEL=DEBUG ./gradlew runLocal
```

## 特定カテゴリのツールのみを有効化

```bash
# WikiとProjectツールのみ有効化
DOORAY_ENABLED_CATEGORIES=wiki,project ./gradlew runLocal
```

## トラブルシューティング

### クリーンビルド
```bash
./gradlew clean build
```

### Gradle キャッシュクリア
```bash
rm -rf ~/.gradle/caches/
./gradlew clean build --refresh-dependencies
```
