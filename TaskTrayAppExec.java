import java.awt.AWTException;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Hashtable;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

class TaskTrayAppExec extends JPanel {

	/**
	  * シャープ(#)
	  */
	static final String SHARP = "#";

	/**
	  * 空文字
	  */
	static final String BLANK = "";

	/**
	  * カンマ
	  */
	static final String COMMA = ",";

	/**
	  * イコール
	  */
	static final String EQUAL = "=";

	/**
	  * 復号化方式
	  */
	static final String AES = "AES";

	/**
	  * エンコード(コマンドプロンプト用)
	  */
	private static final String ENCODE = "MS932";  

	/**
	  * エンコード(ファイル用)
	  */
	private static final Charset CHARSET = StandardCharsets.UTF_8;

	/**
	  * 2重起動チェック(チェックする)
	  */
	private static final int DBL_CHK_DO = 1;

	/**
	  * アイコン名
	  */
	private static final String ICON_NAME = "menuinicrypteditor.png";

	/**
	  * プロパティファイル名
	  */
	private static final String RESOUCE_BUNDLE = "menuinicrypteditor";

	/**
	  * Props(INIファイル名)
	  */
	private static final String INI_FILE_PATH = "inifilepath";

	/**
	  * Props(強制終了フラグファイル名)
	  */
	private static final String FORCE_EXIT_FILE_PATH = "forceexitfilepath";

	/**
	  * Props(復号化対象キー(前方一致文字))
	  */
	static final String CRYPTKEY_STARTWITHS_KEY = "cryptKeyStartWiths";

	/**
	  * 復号化キー最大長(16バイト固定)
	  */
	static final byte[] CRYPTKEY_BASE   = "1111111111111111".getBytes(CHARSET);

	/**
	  *  メニューIDX(アナログ時計)
	  */
	static final int MENU_ITEMS_IDX_ANALOG_WATCH = 0;

	/**
	  *  デフォルトで表示するアイテム数(時計、reload、暗号復号化キー設定、exit)
	  */
	static final int MENU_ITEMS_DEFAULT_ITEMS_CNT = 4;

	/**
	  *  JFrame
	  */
	private static JFrame frame = null;

	/**
	  *  アナログ時計表示フラグ
	  */
	private static boolean showFlg = false;

	/**
	  *  MenuItem
	  */
	public MenuItem[] menuItems = null;

	/**
	  *  メニュー表示用データ
	  */
	public ClassMenuData[] menudatas =  null;

	/**
	  * INIファイル復号化対象キー(※前方一致)※カンマ区切り複数可能
	  */
	static String[] CRYPTKEY_STARTWITHS  = null;

	/**
	  * 編集中ファイルパス
	  */
	static String FILENAME = "";

	/**
	  * AES暗号復号化キー
	  */
	static String ENCRYPT_KEY = "";

	/**
	 * メイン処理
	 */
	public static void main(String[] args) {

		// 初期表示で、暗号復号化キー設定ダイアログを表示する。
		initShowInputDialog();

		TaskTrayAppExec app = new TaskTrayAppExec();
		app.showtray(false);
	}

	/**
	  * 初期表示で、暗号復号化キー設定ダイアログを表示する。
	  */
	private static void initShowInputDialog(){
		String str = ENCRYPT_KEY;
		if (str == null) {
			str = BLANK;
		}

		String title = "暗号復号化キー(16バイト以内)";
		int MAX_RETRY = 10;
		int retry = 0;
		for (retry = 0; retry < MAX_RETRY; retry++) {
			str = JOptionPane.showInputDialog(null, title, str, JOptionPane.INFORMATION_MESSAGE);
			if (str == null) {
				// 取消
				break;
			}else if (isBlank(str)) {
				str = BLANK;
				title = "暗号復号化キー(再入力 16バイト以内)　未入力 (" + retry + "/" + MAX_RETRY + ")";
			} else {
				byte[] data = str.getBytes(CHARSET);
				if (data.length > 16) {
					str = BLANK;
					title = "暗号復号化キー(再入力 16バイト以内)　桁数オーバ (" + retry + "/" + MAX_RETRY + ")";
				} else {
					break;
				}
			}
		}
		if (retry > MAX_RETRY) {
            JOptionPane.showConfirmDialog(null, "リトライ回数が" + MAX_RETRY + "回を超えました。再度設定はメニューから実施してください。", "Info", JOptionPane.CLOSED_OPTION);
		}
		if (!isBlank(str)){
			ENCRYPT_KEY = str;
		}
	}

	/**
	 * システムトレイ表示。
	 * param subFlg サブ呼出しフラグ
	 */
	private void showtray(boolean subFlg) {
		SystemTray tray = SystemTray.getSystemTray();
		PopupMenu popup = new PopupMenu();
		Image image = Toolkit.getDefaultToolkit().createImage( ClassLoader.getSystemResource(ICON_NAME));
		TrayIcon icon = new TrayIcon(image, "lanch apps", popup);
		icon.setImageAutoSize(true);

		// メニュー表示設定ファイル読込み
		menudatas =  readInitFile();
		// メニュー表示設定ファイルなし
		if ((menudatas == null) || (menudatas.length == 0)) {
			// (アナログ時計、暗号復号化キー設定、reload、exit)のみ表示。
			menuItems = new MenuItem[MENU_ITEMS_DEFAULT_ITEMS_CNT];
		} else {
			// (アナログ時計、暗号復号化キー設定、reload、exit)＋メニュー表示設定ファイル内容表示。
			menuItems = new MenuItem[MENU_ITEMS_DEFAULT_ITEMS_CNT + menudatas.length];
		}

		// メニュー1つ目：アナログ時計。
		menuItems[MENU_ITEMS_IDX_ANALOG_WATCH] = new MenuItem("■[ analog watch ]");
		menuItems[MENU_ITEMS_IDX_ANALOG_WATCH].addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {

			}
		});
		// メニュー2つ目～末尾から4つ目：メニュー表示設定ファイル内容。
		if (menuItems.length > MENU_ITEMS_DEFAULT_ITEMS_CNT) {
			for (int i = 0; i < menudatas.length; i++) {
				menuItems[i + 1] = new MenuItem("□[ " + menudatas[i].getName() + " ]");
				menuItems[i + 1].addActionListener(new ActionListenerForMenuDatas(i));
			}
		}
		// メニュー末尾から3つ目：暗号復号化キー設定。
		menuItems[menuItems.length - 3] = new MenuItem("■[ 暗号復号化キー再設定 ]");
		menuItems[menuItems.length - 3].addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// 初期表示で、暗号復号化キー設定ダイアログを表示する。
				initShowInputDialog();
			}
		});
		// メニュー末尾から2つ目：Reload設定(メニュー表示設定ファイル再読込み)。
		menuItems[menuItems.length - 2] = new MenuItem("■[ Reload ]");
		menuItems[menuItems.length - 2].addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// 現在のリスナーを全削除
				for (int i = 0; i < menuItems.length; i++) {
					ActionListener[] listeners = menuItems[i].getActionListeners();
					for (ActionListener listener: listeners){
						menuItems[i].removeActionListener(listener);
					}
					menuItems[i] =null;
				}
				// 変数の初期化
				if (frame != null) {
					frame.setVisible(false);
					showFlg = false;
					frame.dispose();
				}
				menuItems = null;
				menudatas =  null;

				// メニュー表示設定ファイル再読込み)。
				if (!subFlg) {
					tray.remove(icon);
					showtray(true);
				}
			}
		});
		// メニュー末尾：Exit設定。
		menuItems[menuItems.length - 1] = new MenuItem("■[ Exit ]");
		menuItems[menuItems.length - 1].addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				tray.remove(icon);
				System.exit(0);
			}
		});
		for (MenuItem menudata : menuItems ) {
			popup.add(menudata );
		}		

		try {
			// システムトレイに追加★今ひとつの動き。。。
			tray.add(icon); 
		} catch (AWTException e) {
			e.printStackTrace();
		}
	}

//	/**
//	 * アナログ時計表示。
//	 */
//	private static void showclock() {
//		showFlg = true;
//		if (frame == null) {
//			frame = new JFrame("アナログ時計");
//			TaskTrayAppExec clockPanel = new TaskTrayAppExec();
//			// タイトルバーを消す
//			frame.setUndecorated(true);
//			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//			frame.setSize(400, 400);
//			frame.setLayout(new BorderLayout());
//			frame.add(clockPanel, BorderLayout.CENTER);
//			//タスクバー非表示
//			frame.setType(javax.swing.JFrame.Type.UTILITY) ;
//		}
//		frame.setVisible(true);
//	}

	/**
	 * メニュー表示設定ファイル読込み。
	 */
	private ClassMenuData[] readInitFile() {
		ResourceBundle bundle = null;
		try {
			 bundle = ResourceBundle.getBundle(RESOUCE_BUNDLE);
		} catch (MissingResourceException e) {
			 System.out.println("プロパティファイル「" + RESOUCE_BUNDLE + "」がありません。");
			 return null;
		}

		// INIファイル
		String iniFilePath = "";
		try {
			 iniFilePath = bundle.getString(INI_FILE_PATH);
		} catch (MissingResourceException e) {
			 System.out.println("プロパティファイル「" + iniFilePath + "」がありません。");
			 return null;
		}

		// 復号化対象キー読込み
		try {
			String cryptKeyStartWiths = bundle.getString(CRYPTKEY_STARTWITHS_KEY);
			if (!isBlank(cryptKeyStartWiths)) {
				CRYPTKEY_STARTWITHS = cryptKeyStartWiths.split(COMMA);
			}
		} catch (MissingResourceException e) {
			System.out.println("プロパティファイル「" + RESOUCE_BUNDLE + "」にキー「" + CRYPTKEY_STARTWITHS_KEY  + "」がありません。");
			return null;
		}

		// INIファイル読込み
		List<ClassMenuData> wklist = iniFileRead(iniFilePath);
		if (wklist == null) {
			 return null;
		} else if (wklist.size() == 0) {
			 return null;
		} else {
			// メニュー表示用データ設定
			 ClassMenuData[] ret = new ClassMenuData[wklist.size()];
			 for (int i = 0; i < wklist.size(); i++) {
				 ret[i] = wklist.get(i);
			}
			return ret;
		}
	}

	/**
	 * 2重起動チェック
	 */
	private static boolean dblchk(String execpath) {
		String imagename = getImagine(execpath);
		String cmdstr = "tasklist /fi \"IMAGENAME eq \"" + imagename + " /fo csv";
		System.out.println("入力:" + cmdstr);
		boolean ret = execCommand(cmdstr);
		System.out.println("戻り値：" + ret);
		return !ret;
	}

	/**
	 * 実行中プロセスチェック
	 * @param cmdstr 実行中プロセスチェックコマンド
	 */
	public static boolean execCommand(String cmdstr)  {
		InputStream in = null;
		BufferedReader br = null;

		boolean ret = false;
		try {
			Runtime runtime = Runtime.getRuntime();
			Process process = runtime.exec(cmdstr);
			in = process.getInputStream();
			br = new BufferedReader(new InputStreamReader(in, ENCODE));
			//コマンドプロンプト標準出力1つ目取得
			String stdout = "";
			while ((stdout = br.readLine()) != null) {
				ret = true;
				break;
			}
			//情報: 指定された条件に一致するタスクは実行されていません。
			if (stdout.startsWith("情報: 指定された条件に一致するタスク")) {
				ret = false;
			}
			//コマンドプロンプト戻り値判定
			int iret = process.waitFor();
			//エラーの場合、判断しない。
			if (iret  != 0) {
				ret = false;
			}
		} catch (IOException e) {
			System.out.println("エラーです。");
		} catch (InterruptedException e) {
			System.out.println("エラーです。");
		} finally {
			try {
				if (br != null) {
					br.close();
				}
				if (in != null) {
					in.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return ret;
	}

	/**
	 * 実行文字列からパスを除いたexe名取得。
	 */
	private static String getImagine(String execpath) {
		String chkstr = execpath;
		//実行文字列がダブルクォーテーションで囲まれている場合
		if (execpath.startsWith("\"")) {
			chkstr = chkstr.substring(1);
			//次のダブルクォーテーション手前までをexe名とする。
			int index = chkstr.indexOf("\"");
			//フルパス実行ファイル名
			chkstr = chkstr.substring(1, index);
		} else {
			String[]  strarray = chkstr.split(" ");
			//フルパス実行ファイル名
			chkstr = strarray[0];
		}
		//アプリ名を返す。
		File file= new File(chkstr);
		return file.getName();
	}

	/**
	  * INIファイル読込み
	  */
	public List<ClassMenuData> iniFileRead(String filePath) {
		if (isBlank(filePath)) {
			return null;
		}

		File file = new File(filePath);
		if (!file.exists()) {
			System.out.println("INIファイル(" + file.getPath() + ")がありません。");
			return null;
		}
		BufferedReader br = null;
		try{
			br = new BufferedReader(new FileReader(file));
			List<String> list = new ArrayList<String>();
			String s;
			while ((s = br.readLine()) != null){
				list.add(s);
			}
			// TRIM
			list = listTrim(list);
			// 復号化
			if (!isBlank(ENCRYPT_KEY)) {
				list = decrypto(list);
			}
			// メニュー表示用データ設定
			Hashtable<String, String> hashName = new Hashtable<String, String>();
			Hashtable<String, String> hashPath = new Hashtable<String, String>();
			Hashtable<String, String> hashDblRunOkFlg = new Hashtable<String, String>();
			String regexKeyName = "^name[0-9]+";
			String regexKeyPath = "^path[0-9]+";
			String regexKeyDblRunOkFlg = "^dblRunOkFlg[0-9]+";
			Pattern p1 = Pattern.compile(regexKeyName);
			Pattern p2 = Pattern.compile(regexKeyPath);
			Pattern p3 = Pattern.compile(regexKeyDblRunOkFlg);
			int maxKeyNum = 0;
			int line = 0;
			for (String str: list) {
				line++;
				str = trimTabSpace(str);
				if (str.startsWith(SHARP)) {
					continue;
				}
				String[] wkarray = str.split(EQUAL);
				// プロパティファイル形式でない。
				if (wkarray.length < 2) {
					System.out.println(line + "行目 プロパティファイル形式でありません(記号「=」なし)。");
					continue;
				}
				String key = trimTabSpace(wkarray[0]);
				if (key.endsWith("\\h")) {
					System.out.println(line + "行目 KEY項目の後ろに全角空白あり。");
					continue;
				}
				String value = trimTabSpace(wkarray[1]);
				if (value.startsWith("\\h")) {
					System.out.println(line + "行目 記号「=」の次に全角空白あり。");
					continue;
				}
				Matcher m1 = p1.matcher(key);
				if(m1.find()) {
					String keyNum = key.substring(4);
					hashName.put(keyNum, value);
					int wkInt = Integer.valueOf(keyNum).intValue();
					if (maxKeyNum < wkInt) {
						maxKeyNum = wkInt;
					}
				}
				Matcher m2 = p2.matcher(key);
				if(m2.find()) {
					String keyNum = key.substring(4);
					hashPath.put(keyNum, value);
				}
				Matcher m3 = p3.matcher(key);
				if(m3.find()) {
					String keyNum = key.substring(11);
					hashDblRunOkFlg.put(keyNum, value);
				}
			}
			List<ClassMenuData> ret = new ArrayList<ClassMenuData>();
			for (int i = 1; i <= maxKeyNum; i++) {
				String valueName = hashName.get(String.valueOf(i));
				String valuePath = hashPath.get(String.valueOf(i));
				String valuehashDblRunOkFlg = hashDblRunOkFlg.get(String.valueOf(i));
				if (valuehashDblRunOkFlg == null) {
					valuehashDblRunOkFlg = "0";
				}
				if ((valueName != null) && (valuePath != null)) {
					ClassMenuData menudate = new ClassMenuData();
					menudate.setName(valueName);
					menudate.setPath(valuePath);
					menudate.setDblRunOkFlg(Integer.parseInt(valuehashDblRunOkFlg));
					ret.add(menudate);
				}
			}
			return ret;
		} catch(NoSuchPaddingException e) {
			e.printStackTrace();
			System.out.println("NoSuchPaddingExceptionエラーが発生しました。INIファイル読込みに失敗しました。");
			return null;
		} catch(NoSuchAlgorithmException e) {
			e.printStackTrace();
			System.out.println("NoSuchAlgorithmExceptionエラーが発生しました。INIファイル読込みに失敗しました。");
			return null;
		} catch(BadPaddingException e) {
			e.printStackTrace();
			System.out.println("暗号復号化キーの値が間違っています。再度、設定後、INIファイル読込みを行ってください。");
			return null;
		} catch(IllegalBlockSizeException e) {
			e.printStackTrace();
			System.out.println("IllegalBlockSizeExceptionエラーが発生しました。INIファイル読込みに失敗しました。");
			return null;
		} catch(InvalidKeyException e) {
			e.printStackTrace();
			System.out.println("InvalidKeyExceptionエラーが発生しました。INIファイル読込みに失敗しました。");
			return null;
		} catch(IOException e) {
			e.printStackTrace();
			System.out.println("IOExceptionエラーが発生しました。INIファイル読込みに失敗しました。");
			return null;
		} finally {
			try {
				if (br != null) {
					br.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	  * 復号化
	  */
	public static List<String> decrypto(List<String> list) throws NoSuchPaddingException, NoSuchAlgorithmException, BadPaddingException, IllegalBlockSizeException, InvalidKeyException {
		List<String> ret = new ArrayList<String>();

		// 16バイトに成形
		byte[] wksecretKeyBytes = ENCRYPT_KEY.getBytes();
		byte[] secretKeyBytes = CRYPTKEY_BASE.clone();
		for (int i = 0; i < wksecretKeyBytes.length; i++) {
			secretKeyBytes[i] = wksecretKeyBytes[i];
		}
		// データ
		for (String line : list) {
			if (isTarget(line)) {
				int pos = line.indexOf(EQUAL);
				String itemname = line.substring(0, pos + 1);
				String originalData = line.substring( pos + 1);
				byte[] data = originalData.getBytes(CHARSET);

				//decrypt
				byte[] decryptBytes = Base64.getDecoder().decode(new String(data, CHARSET));
				SecretKeySpec secretKeySpec = new SecretKeySpec(secretKeyBytes, AES);
				Cipher cipher = Cipher.getInstance(AES);
				cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
				byte[] originalBytes = cipher.doFinal(decryptBytes);
				ret.add(itemname + new String(originalBytes, CHARSET));
			} else {
				ret.add(line);
			}
		}
		return ret;
	}

	/**
	 * 復号化対象判定。
	 */
	private static boolean isTarget(String str) {
		if (isBlank(str)) {
			return false;
		}
		if (trimTabSpace(str).startsWith(SHARP)) {
			return false;
		}
		// 復号化対象キー(前方一致文字)※複数設定確認
		if (isBlank(CRYPTKEY_STARTWITHS)) {
			return false;
		}
		// 復号化対象キー(前方一致文字)ごとに復号化対象化を判定。
		for (String strStartWith : CRYPTKEY_STARTWITHS) {
			String[] wkarray = str.split(EQUAL);
			// プロパティファイル形式でない。
			if (wkarray.length < 2) {
				return false;
			}
			// キー項目なので先頭で判断
			if (wkarray[0].startsWith(strStartWith)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Null空文字判定。
	 */
	public static boolean isBlank(String str) {
		if (str == null) {
			return true;
		} else if (str.length() == 0) {
			return true;
		}
		return false;
	}

	/**
	 * Null空文字判定。
	 */
	public static boolean isBlank(String[] strarray) {
		if (strarray == null) {
			return true;
		} else if (strarray.length == 0) {
			return true;
		}
		return false;
	}

	/**
	 * 前後タブ半角空白除去
	 */
	public static String trimTabSpace(String str) {
		if (isBlank(str)) {
			return str;
		}else {
			str = str.trim();
			str = str.replaceFirst("^[\\t ]+", "").replaceFirst("[\\t ]+$", "");
			return str;
		}
	}

	/**
	  * 不要空行削除
	  */
	public static List<String> listTrim(List<String> list) {
		List<String> ret = new ArrayList<String>();
		// データ
		for (String line : list) {
			if (trimTabSpace(line).length() > 0) {
				ret.add(line);
			}
		}
		return ret;
	}

	/**
	 * ポップアップ表示設定ファイル内容格納データクラス。
	 */
	private class ClassMenuData {
		// ポップアップ表示名
		private String name;
		// パス
		private String path;
		// 2重起動フラグ(0:2重起動しても良い、1:2重起動抑止)

		private int dblRunOkFlg;

		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public String getPath() {
			return path;
		}
		public void setPath(String path) {
			this.path = path;
		}
		public int getDblRunOkFlg() {
			return dblRunOkFlg;
		}
		public void setDblRunOkFlg(int dblRunOkFlg) {
			this.dblRunOkFlg = dblRunOkFlg;
		}
	}

	/**
	 * 明示的にアクションリスナーを定義。
	 */
	private class ActionListenerForMenuDatas implements ActionListener {
		/**
		 * メニュー配列INDEX
		 */
		private int menuarrayidx;

		/**
		 * コンストラクタ
		 * @param arrayindex メニュー配列INDEX
		 */
		public ActionListenerForMenuDatas(int menuarrayidx) {
			this.menuarrayidx = menuarrayidx;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (menudatas == null) {
				return;
			}
			if ((menuarrayidx < 0) || (menuarrayidx > (menudatas.length - 1))) {
				return;
			}
			String execpath = menudatas[menuarrayidx].getPath();
			String[] Command = { "cmd", "/c", execpath };
			Runtime runtime = Runtime.getRuntime(); 

			// 2重起動チェックする。
			boolean doexec = true;
			if (DBL_CHK_DO == menudatas[menuarrayidx ].getDblRunOkFlg()) {
				doexec = dblchk(execpath);
			}
			// 実行OK
			if (doexec) {
				try {
					runtime.exec(Command);
				} catch (IOException e2) {
					e2.printStackTrace();
				}
			}
		}
	}
}
