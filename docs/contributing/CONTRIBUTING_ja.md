# コントリビューションガイド([English](../../.github/CONTRIBUTING.md)/日本語)

お読みいただきありがとうございます。もし質問等ございましたら[Discussions](https://github.com/turtton/YtAlarm/discussions/categories/q-a)にてお願いします

## バグ報告/新機能提案

- 新しくIssueを立てる前に、既に同様の内容が報告されていないか確認してください
- 報告の際はテンプレートに沿って記述をお願いします(無視されている場合は内容を見ずに閉じます)
- 内容は英語、日本語どちらでもかまいません(日本語のほうがちょっとレスポンス早いかも)

1つのissueには1つのバグ、機能のみを記述するようお願いします

## 翻訳

### ドキュメント

新規での作成、既存ファイルの修正の提案どちらも歓迎しています  
以下は新規作成時のガイドとなります

- [docs](../.)ファイル内にcontributing/readme専用のフォルダが存在するためその中で作成してください。
- 各ファイルは対象の名前と[言語コード](https://www.loc.gov/standards/iso639-2/php/code_list.php)、必要に応じて[地域コード](https://www.iso.org/obp/ui/#iso:pub:PUB500001:en)を付けて作成してください(例:`README_en_uk.md`)
- 作成が完了したら`docs:`から始まるタイトルのPRを作成してください

### アプリケーション

準備中

## 開発

### 方針

- 特別な理由が無い限りJavaのコードは受けつけません
- コーディングに関しては[Android Kotlin Style Guide(日本語)](https://developer.android.com/kotlin/style-guide?hl=ja)に従って記述していただければ問題ないと思いますが、最終的にはktlint従ってください
- ライブラリ等を追加する際はライセンスに細心の注意を払ってください(クローズドソースなものが含まれていないことを確認してください)
- 特別な理由なしにmainなどの主要ブランチにコードを直接Pushすることは許されていません

### 開発を初める際には

- 作業したい内容と合ったIssueが存在する場合そこに作業を行う旨をコメントしてください。適切なIssueが存在しない場合は新たに作成してください
- 一度のPRで解決するIssueは最小限に留めてください

#### ブランチ名

変更内容を端的に表わしたものかissueの番号にしてください(例: `fix/88`,`feat/display_progress`)

#### コミットメッセージ

[ConventionalCommits](https://www.conventionalcommits.org/ja/v1.0.0/)に従って命名することをお勧めします

### プルリクエスト

- タイトルは[ConventionalCommits](https://www.conventionalcommits.org/ja/v1.0.0/)に従って作成してください
- テンプレートに沿って記述をお願いします(無視されている場合は内容を見ずに閉じます)

- 作業途中であっても作成していただいて構いません(進捗状況が追いやすくなるため)
  その際はDraftに設定するようお願いします

### 開発環境

本アプリケーションは[AndroidStudio](https://developer.android.com/studio)で開発されています。プロジェクトを読み込んだ際にコードスタイルが[`.editorconfig`](../../.editorconfig)によって上書きされていることをご確認ください。

他環境での開発は自由ですが、[`.editorconfig`](../../.editorconfig)に対応していることをご確認ください。また`.fleet`などの環境設定ファイルをコミットしないようお願いします。

### コード精査

`ktlint`および`Android Plugin for Gradle`を使用します。以下のgradleタスクより実行が可能です

- フォーマット: `formatKotlin`
- リント: `lintKotlin lint`
- 全体のチェック: `check`

