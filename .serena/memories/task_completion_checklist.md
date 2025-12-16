# タスク完了時のチェックリスト

コードの変更を完了したら、以下のステップを実行してください。

## 1. コードの検証

### ビルドチェック
```bash
./gradlew clean build
```
- ビルドエラーがないことを確認
- コンパイルが正常に完了することを確認

### ツールスキーマの検証
```bash
./gradlew validateSchemas
```
- 新しいツールを追加した場合は必須
- すべてのツールのJSONスキーマが正しいことを確認

## 2. テストの実行

### ユニットテストの実行
```bash
./gradlew test
```
- すべてのユニットテストがパスすることを確認
- 新しい機能には対応するテストを追加

### 統合テストの実行（ローカル環境のみ）
```bash
# 実際のDooray API認証情報が必要
# .envファイルに設定してから実行
./gradlew test
```
- 統合テストは実際のAPIを呼び出すため、ローカル環境でのみ実行
- CI環境では自動的にスキップされる

## 3. コードスタイルの確認

### Kotlinコードスタイル
- [ ] 4スペースインデント
- [ ] camelCase（変数、関数）、PascalCase（クラス）、UPPER_SNAKE_CASE（定数）
- [ ] 明示的な戻り値の型（パブリックAPI）
- [ ] nullable型の適切な使用

### ロギングの確認
- [ ] **重要**: `println()` を使用していないこと（stdoutを汚染しない）
- [ ] すべてのログは `logger.debug/info/warn/error()` を使用
- [ ] デバッグログは `logger.debug()` を使用

## 4. 新しいツールを追加した場合

### ツール登録の確認
- [ ] `DoorayMcpServer.kt` の `registerTool()` で登録済み
- [ ] 適切な `ToolCategory` を指定
- [ ] ツール数のコメントを更新（現在: 48個）

### ツール実装の確認
- [ ] `Tool` オブジェクトを定義（name、description、inputSchema）
- [ ] ハンドラー関数を実装（`suspend fun handle*(request: CallToolRequest): CallToolResult`）
- [ ] `DoorayClient` インターフェースにメソッド追加
- [ ] `DoorayHttpClient` にHTTP実装を追加
- [ ] 対応するデータ型を `types/` に追加

### ドキュメント更新
- [ ] `README.md` のツール一覧を更新
- [ ] 必要に応じて `CLAUDE.md` を更新

## 5. APIクライアントを変更した場合

### インターフェースとの整合性
- [ ] `DoorayClient` インターフェースのメソッドシグネチャが正しい
- [ ] `DoorayHttpClient` の実装が完全
- [ ] リトライロジックが適切に動作

### エラーハンドリング
- [ ] 適切な例外処理（`ToolException`, `CustomException`）
- [ ] エラーレスポンスが `DoorayApiErrorType` を使用

## 6. ローカルテスト

### ローカル実行テスト
```bash
./gradlew runLocal
```
- [ ] `.env` ファイルが正しく設定されている
- [ ] サーバーが正常に起動
- [ ] stdin/stdoutでMCPプロトコル通信が正常に動作

### 変更内容の手動テスト
- [ ] 変更した機能を実際にテスト
- [ ] エッジケースをテスト（null値、空文字列、無効なパラメータなど）

## 7. Dockerビルドテスト（オプション）

### Dockerイメージのビルド
```bash
docker build -t dooray-mcp:test --build-arg VERSION=test .
```
- [ ] Dockerイメージが正常にビルドされる
- [ ] マルチステージビルドが正常に動作

### Dockerコンテナのテスト
```bash
docker run -e DOORAY_API_KEY="your_key" \
           -e DOORAY_BASE_URL="https://api.dooray.com" \
           dooray-mcp:test
```
- [ ] コンテナが正常に起動
- [ ] MCP通信が正常に動作

## 8. Git コミット前の確認

### 変更内容の確認
```bash
git status
git diff
```
- [ ] 意図した変更のみがステージングされている
- [ ] 不要なファイル（`.DS_Store`、ビルド成果物など）が含まれていない

### コミットメッセージ
```bash
git commit -m "feat: add new tool for XXX"
# または
git commit -m "fix: correct XXX issue"
# または
git commit -m "docs: update README for XXX"
```
- [ ] 明確で説明的なコミットメッセージ
- [ ] 変更の種類を接頭辞で示す（feat、fix、docs、refactor、test、chore）

## 9. バージョン管理（リリース時のみ）

### バージョン更新
- [ ] `constants/VersionConst.kt` のバージョンを更新
- [ ] `gradle.properties` または `build.gradle.kts` のバージョンを更新
- [ ] `README.md` の変更履歴を更新

## 10. CI/CDの確認（プッシュ後）

### GitHub Actions
- [ ] CI/CDパイプラインが正常に完了
- [ ] テストがすべてパス
- [ ] Dockerイメージが正常にビルドされ公開される（mainブランチの場合）

## トラブルシューティング

### ビルドが失敗する場合
```bash
./gradlew clean
rm -rf build/
./gradlew build --refresh-dependencies
```

### テストが失敗する場合
```bash
# デバッグモードで実行
DOORAY_LOG_LEVEL=DEBUG ./gradlew test
```

### Dockerビルドが失敗する場合
```bash
# キャッシュなしでビルド
docker build --no-cache -t dooray-mcp:test .
```
