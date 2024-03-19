This is a release guide for repository owner. No need to read this except for the me.

いわゆる自分用メモってやつ

## 新バージョンリリース時の手順

ちゃんとこれPRでやれよ

1. バージョンを[build.gradle.kts](../app/build.gradle.kts)のmajor/minor/patchをいじって更新する
2. `fastlane android build_and_screengrab `を実行してスクリーンショットを更新する(アラーム画面だけは運要素あるので何回かやって確認してね)
3. `./gradlew -PnoSplits -PabiFilters=x86_64`で一度ビルドして、[VERSION_CODE](../VERSION_CODE)ファイルが正しく更新されることを確認する(値の下2桁がpatch*5+4)になってたらOK
4. {指定したバージョンコード(上のVERSION_CODE-4)}.txtって名前で[changelogs](../fastlane/metadata/android/en-US/changelogs)内に変更内容を書く(できればjaでも書く)
5. ここまででmainにマージ
6. `v{versionName}`でタグ付け(`git tag -s v{versionName}`)してプッシュ(マイナーバージョンの5刻みなルールはタグにも適用されるので注意)
7. ちょっとするとリリースが作成されるので`generate release notes`して内容確認しておわり(アセットに5種類のapkファイルがあるか確認して、ないやつは[該当Action](https://github.com/turtton/YtAlarm/actions/workflows/release.yml)のアーティファクトから引っぱってきて置いといてくれ)
8. たぶんFdroidの更新は勝手にやってくれる。だめそうなら[一回dockerでcheckupdates](https://gist.github.com/turtton/25b4e3a91703c6cbdebdeac2f754a51a)してちゃんと動きそうか確認してくれ(あとやってみてこの情報更新してくれ)

### 初期リリース時のメモ

- 申請時の[マージリクエスト](https://gitlab.com/fdroid/fdroiddata/-/merge_requests/12004)

まずこのプロジェクトはyt-dlpの実行ファイルを直接扱う関係でFdroidでは4つのabi(armabi/arm64/x86/x86_64)向けに分けてビルドする必要があった(なんか自分が申請送る1週間前ぐらいに[そういう方針になった](https://gitlab.com/fdroid/fdroiddata/-/issues/2809)らしい)。またFdroid君は一度のビルドで扱えるapkファイルが一個らしく4つ分のビルドタスクを書いて、それぞれバージョンコードも分ける必要もあった([不便です↑まん~](https://gitlab.com/fdroid/fdroiddata/-/merge_requests/12004#note_1155424407)って言われた)。

あとなんかこのプロジェクトはgithub上のビルドとFdroid側のビルドでAndroidManifest.xml, assets/dexopt/baseline.prof and classes2.dexに差異があるらしく、 Reproducible buildではないらしい。なんで？？？？([これ](https://gitlab.com/fdroid/fdroiddata/-/merge_requests/12004#note_1157082096)のことなんだけどこの解釈でいいよね？)
