Windows11で、新規ツールバーがなくなり、アプリ起動を不便に感じたため、
見よう見まねでタスクトレイにアプリの一覧メニューを表示するプログラムを作成しました。

ソースは、UTF-8でお願いします。

MenuIniCryptEditor.java
暗号化復号化エディッタです。
下記の「TaskTrayAppExec.java」で使用する設定ファイル「menuinicrypteditor.ini」専用です。　
※事前に「menuinicrypteditor.properties」のパスの修正が必要です。
起動すると、最初に暗号化復号化キーの入力が必要です。サンプルは、「123」です。
「menuinicrypteditor.ini」の(name、path、dblRunOkFlg)で１セットで、KEYに末尾連番1～が必要です。
※pathのみが暗号化対象です。
※dblRunOkFlg：0→2重起動OK、1→2重起動不可　です、
表示したいアプリとpathを設定します。
読み込み時、暗号化復号化キーで復号化して表示します。
上書き保存すると、暗号化復号化キーで暗号化して保存します。

TaskTrayAppExec.java
起動すると、最初に復号化キーの入力が必要です。サンプルは、「123」です。
その後、「menuinicrypteditor.properties」より、「menuinicrypteditor.ini」を読み込みタスクにメニューを作成します。
※事前に「menuinicrypteditor.properties」のパスの修正が必要です。
「menuinicrypteditor.ini」の(name、path、dblRunOkFlg)で１セットで、KEYに末尾連番1～が必要です。
※dblRunOkFlg：0→2重起動OK、1→2重起動不可　です、
セキュリティの観点から、pathは、AESで暗号化しています。※サンプルは、パスワード：「123」　※最大16桁
アイコン「menuinicrypteditor.png」はご自身で追加してください。
先頭メニュー「アナログ時計」は不詳のため、現在使用できません。
その他のメニュー「暗号復号化キー再設定」：AESの暗号を再設定できます。
その他のメニュー「reload」は「menuinicrypteditor.ini」の再読み込みです。
その他のメニュー「exit」は、終了です。

実行は自己責任でお願いします。
発生した事象に責任は持ちません。
