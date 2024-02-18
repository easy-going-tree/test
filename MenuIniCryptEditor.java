import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

class MenuIniCryptEditor {
	public static void main(String args[]){
		JTextArea jtextarea = new JTextArea();
		jtextarea.setFont(new Font(Font.DIALOG_INPUT, Font.PLAIN, 11));

		//初期処理
		init();

		MenuIniCryptEditorEditorMenu menu = new MenuIniCryptEditorEditorMenu(jtextarea);
		MenuIniCryptEditorEditFace face = new MenuIniCryptEditorEditFace(jtextarea, menu);
		// 初期表示で、暗号復号化キー設定ダイアログを表示する。
		menu.menuEncryptKeySet.initShowInputDialog();
		// 初期表示でiniファイルを読み込んで画面に表示。
		menu.menuFileMenu.fileRead();
	}

	private static void init() {
		ResourceBundle bundle = null;
		try {
			bundle = ResourceBundle.getBundle(MenuIniCryptEditorEditorUtil.PROPNAME);
		} catch (MissingResourceException e) {
			JOptionPane.showConfirmDialog(null, "プロパティファイル「" + MenuIniCryptEditorEditorUtil.PROPNAME + "」がありません。", "エラー", JOptionPane.CLOSED_OPTION);
			return;
		}

		try {
			String iniFilePath = bundle.getString(MenuIniCryptEditorEditorUtil.INI_FILE_PATH_KEY);
			if (!MenuIniCryptEditorEditorUtil.isBlank(iniFilePath)) {
				MenuIniCryptEditorEditorUtil.FILENAME_FROM_PROP = iniFilePath;
			}
		} catch (MissingResourceException e) {
			JOptionPane.showConfirmDialog(null, "プロパティファイル「" + MenuIniCryptEditorEditorUtil.PROPNAME + "」にキー「" + MenuIniCryptEditorEditorUtil.INI_FILE_PATH_KEY + "」がありません。", "エラー", JOptionPane.CLOSED_OPTION);
			return;
		}
		try {
			String cryptKeyStartWiths = bundle.getString(MenuIniCryptEditorEditorUtil.CRYPTKEY_STARTWITHS_KEY);
			if (!MenuIniCryptEditorEditorUtil.isBlank(cryptKeyStartWiths)) {
				MenuIniCryptEditorEditorUtil.CRYPTKEY_STARTWITHS = cryptKeyStartWiths.split(MenuIniCryptEditorEditorUtil.CONMA);
			}
		} catch (MissingResourceException e) {
			JOptionPane.showConfirmDialog(null, "プロパティファイル「" + MenuIniCryptEditorEditorUtil.PROPNAME + "」にキー「" + MenuIniCryptEditorEditorUtil.CRYPTKEY_STARTWITHS_KEY  + "」がありません。", "エラー", JOptionPane.CLOSED_OPTION);
			return;
		}
	}
}

/**
  * 画面表示クラス
  */
class MenuIniCryptEditorEditFace extends JFrame {
	MenuIniCryptEditorEditFace(JTextArea jtextarea, MenuIniCryptEditorEditorMenu menu){
		super("暗号化復号化テキストエディッタ");
		Container container = getContentPane();
		container.setLayout(new BorderLayout());
		container.add(menu, BorderLayout.NORTH);
		JScrollPane sp = new JScrollPane(jtextarea);
		container.add(sp, BorderLayout.CENTER);
		setSize(800, 600);
		setVisible(true);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
	}
}

/**
  * メニューバークラス
  */
class MenuIniCryptEditorEditorMenu extends JMenuBar {
	public MenuIniCryptEditorEncryptKeySet menuEncryptKeySet;
	public MenuIniCryptEditorFileMenu menuFileMenu;

	MenuIniCryptEditorEditorMenu(JTextArea jtextarea){
		super();
		menuEncryptKeySet = new MenuIniCryptEditorEncryptKeySet(jtextarea);
		menuFileMenu = new MenuIniCryptEditorFileMenu(jtextarea);
		add(menuEncryptKeySet);
		add(menuFileMenu);
	}
}

/**
  * メニューバー（ファイル）クラス
  */
class MenuIniCryptEditorFileMenu extends JMenu implements ActionListener {
	JTextArea jtextarea;
	JMenuItem jmenuitemOpen;
	JMenuItem jmenuitemOverWrite;
	JMenuItem jmenuitemExit;
	MenuIniCryptEditorFileMenu(JTextArea jtextarea){
		super("ファイル");
		this.jtextarea = jtextarea;
		jmenuitemOpen = new JMenuItem("開く");
		jmenuitemOverWrite = new JMenuItem("上書き保存");
		jmenuitemExit = new JMenuItem("終了");
		jmenuitemOpen.addActionListener(this);
		jmenuitemOverWrite.addActionListener(this);
		jmenuitemExit.addActionListener(this);
		add(jmenuitemOpen);
		add(jmenuitemOverWrite);
		add(jmenuitemExit);
	}

	public void fileRead(){
		MenuIniCryptEditorEditFileAccess fileaccess = new MenuIniCryptEditorEditFileAccess();
		fileaccess.jmenu = this;
		fileaccess.fileRead(jtextarea, MenuIniCryptEditorEditorUtil.FILENAME_FROM_PROP);
	}

	public void actionPerformed(ActionEvent e){
		MenuIniCryptEditorEditFileAccess fileaccess = new MenuIniCryptEditorEditFileAccess();
		fileaccess.jmenu = this;
		Object obj = e.getSource();
		if (obj == jmenuitemOpen){
			fileaccess.fileOpen(jtextarea);
		}else if (obj == jmenuitemOverWrite){
			if (!MenuIniCryptEditorEditorUtil.FILENAME_FROM_PROP.equals(MenuIniCryptEditorEditorUtil.BLANK) && MenuIniCryptEditorEditorUtil.NEW_FLG){
			// 新規
				// 事前チェック
				if (!MenuIniCryptEditorCheck.beforeSaveCheck()) {
					return;
				}
				MenuIniCryptEditorEditorUtil.FILENAME = MenuIniCryptEditorEditorUtil.FILENAME_FROM_PROP;
				fileaccess.overWrite(jtextarea);
			} else if (MenuIniCryptEditorEditorUtil.FILENAME.equals(MenuIniCryptEditorEditorUtil.BLANK)){
				JOptionPane.showConfirmDialog(null, "iniファイルを読み込んでいません。iniファイルを開いてください。", "エラー", JOptionPane.CLOSED_OPTION);
			}else{
			// 更新
				// 事前チェック
				if (!MenuIniCryptEditorCheck.beforeSaveCheck()) {
					return;
				}
				fileaccess.overWrite(jtextarea);
			}
		}else if (obj == jmenuitemExit){
			System.exit(0);
		}
	}
}

/**
  * メニューバー（暗号復号化キー設定）クラス
  */
class MenuIniCryptEditorEncryptKeySet extends JMenu implements ActionListener {
	JTextArea jtextarea;
	JMenuItem menuEncryptKeySet;

	MenuIniCryptEditorEncryptKeySet(JTextArea jtextarea){
		super("暗号復号化キー");
		this.jtextarea = jtextarea;
		menuEncryptKeySet = new JMenuItem("暗号復号化キー");
		menuEncryptKeySet.addActionListener(this);
		add(menuEncryptKeySet);
	}

	public void actionPerformed(ActionEvent e){
		Object obj = e.getSource();
		if (obj == menuEncryptKeySet){
			String str = MenuIniCryptEditorEditorUtil.ENCRYPT_KEY;
			if (str == null) {
				str = MenuIniCryptEditorEditorUtil.BLANK;
			}
			str = JOptionPane.showInputDialog(this, "暗号復号化キー", str);
			if (!MenuIniCryptEditorEditorUtil.isBlank(str)){
				MenuIniCryptEditorEditorUtil.ENCRYPT_KEY = str;
			}
		}
	}

	/**
	  * 初期表示で、暗号復号化キー設定ダイアログを表示する。
	  */
	public void initShowInputDialog(){
		String str = MenuIniCryptEditorEditorUtil.ENCRYPT_KEY;
		if (str == null) {
			str = MenuIniCryptEditorEditorUtil.BLANK;
		}

		String title = "暗号復号化キー(16バイト以内)";
		int MAX_RETRY = 10;
		int retry = 0;
		for (retry = 0; retry < MAX_RETRY; retry++) {
			str = JOptionPane.showInputDialog(this, title, str, JOptionPane.INFORMATION_MESSAGE);
			if (MenuIniCryptEditorEditorUtil.isBlank(str)) {
				str = MenuIniCryptEditorEditorUtil.BLANK;
				title = "暗号復号化キー(再入力 16バイト以内)　未入力 (" + retry + "/" + MAX_RETRY + ")";
			} else {
				byte[] data = str.getBytes(MenuIniCryptEditorEditorUtil.CHARSET);
				if (data.length > 16) {
					str = MenuIniCryptEditorEditorUtil.BLANK;
					title = "暗号復号化キー(再入力 16バイト以内)　桁数オーバ (" + retry + "/" + MAX_RETRY + ")";
				} else {
					break;
				}
			}
		}
		if (retry > MAX_RETRY) {
            JOptionPane.showConfirmDialog(null, "リトライ回数が" + MAX_RETRY + "回を超えました。再度設定はメニューから実施してください。", "エラー", JOptionPane.CLOSED_OPTION);
		}

		if (str != null){
			if (str.length() > 0){
				MenuIniCryptEditorEditorUtil.ENCRYPT_KEY = str;
			}
		}
	}
}

/**
  * ファイルアクセスクラス
  */
class MenuIniCryptEditorEditFileAccess {
	/**
	  * ファイル読込み（イベント経由無）
	  */
	public JMenu jmenu = null;

	/**
	  * ファイル読込み（イベント経由無）
	  */
	public void fileRead(JTextArea jtextarea, String filePath) {
		if (MenuIniCryptEditorEditorUtil.isBlank(filePath)) {
			return;
		}

		jtextarea.setText(MenuIniCryptEditorEditorUtil.BLANK);
		File f = new File(filePath);
		BufferedReader br = null;
		try{
			br = new BufferedReader(new FileReader(f));

			List<String> list = new ArrayList<String>();
			String s;
			while ((s = br.readLine()) != null){
				list.add(s);
			}

			// 復号化
			if (!MenuIniCryptEditorEditorUtil.isBlank(MenuIniCryptEditorEditorUtil.ENCRYPT_KEY)) {
				list = MenuIniCryptEditorCrypt.decrypto(list);
			}

			// 画面表示
			for (String str: list){
				jtextarea.append(str + '\n');
			}
		} catch(NoSuchPaddingException e) {
			e.printStackTrace();
			jtextarea.setText(MenuIniCryptEditorEditorUtil.BLANK);
            JOptionPane.showConfirmDialog(null, "NoSuchPaddingExceptionエラーが発生しました。INIファイル読込みに失敗しました。", "エラー", JOptionPane.CLOSED_OPTION);
			return;
		} catch(NoSuchAlgorithmException e) {
			e.printStackTrace();
			jtextarea.setText(MenuIniCryptEditorEditorUtil.BLANK);
            JOptionPane.showConfirmDialog(null, "NoSuchAlgorithmExceptionエラーが発生しました。INIファイル読込みに失敗しました。", "エラー", JOptionPane.CLOSED_OPTION);
			return;
		} catch(BadPaddingException e) {
			e.printStackTrace();
			jtextarea.setText(MenuIniCryptEditorEditorUtil.BLANK);
            JOptionPane.showConfirmDialog(null, "暗号復号化キーの値が間違っています。再度、設定後、INIファイル読込みを行ってください。", "エラー", JOptionPane.CLOSED_OPTION);
			return;
		} catch(IllegalBlockSizeException e) {
			e.printStackTrace();
			jtextarea.setText(MenuIniCryptEditorEditorUtil.BLANK);
            JOptionPane.showConfirmDialog(null, "IllegalBlockSizeExceptionエラーが発生しました。INIファイル読込みに失敗しました。", "エラー", JOptionPane.CLOSED_OPTION);
			return;
		} catch(InvalidKeyException e) {
			e.printStackTrace();
			jtextarea.setText(MenuIniCryptEditorEditorUtil.BLANK);
            JOptionPane.showConfirmDialog(null, "InvalidKeyExceptionエラーが発生しました。INIファイル読込みに失敗しました。", "エラー", JOptionPane.CLOSED_OPTION);
			return;
		} catch(IOException e) {
			e.printStackTrace();
			jtextarea.setText(MenuIniCryptEditorEditorUtil.BLANK);
            JOptionPane.showConfirmDialog(null, "IOExceptionエラーが発生しました。INIファイル読込みに失敗しました。", "エラー", JOptionPane.CLOSED_OPTION);
			return;
		} finally {
			try {
				if (br != null) {
					br.close();
				}
			} catch (Exception e) {}
		}
		MenuIniCryptEditorEditorUtil.FILENAME = f.getPath();
	}

	/**
	  * ファイルオープン
	  */
	public void fileOpen(JTextArea jtextarea){
		File dir = new File(MenuIniCryptEditorEditorUtil.FILENAME_FROM_PROP);
		JFileChooser fc = new JFileChooser(dir);
		FileFilter filter = new FileNameExtensionFilter("iniファイル", "ini");
		fc.addChoosableFileFilter(filter);
		fc.setAcceptAllFileFilterUsed(false);
		int selected = fc.showOpenDialog(jmenu);
		if (selected == JFileChooser.CANCEL_OPTION){
			return;
		}else if (selected == JFileChooser.ERROR_OPTION){
			return;
		}

		String backupText = jtextarea.getText();
		jtextarea.setText(MenuIniCryptEditorEditorUtil.BLANK);
		File file = fc.getSelectedFile();
		if (file == null) {
			return;
		}

		BufferedReader br = null;
		try{
			br = new BufferedReader(new FileReader(file));

			List<String> list = new ArrayList<String>();
			String s;
			while ((s = br.readLine()) != null){
				list.add(s);
			}

			// 復号化
			if (!MenuIniCryptEditorEditorUtil.isBlank(MenuIniCryptEditorEditorUtil.ENCRYPT_KEY)) {
				list = MenuIniCryptEditorCrypt.decrypto(list);
			}

			// 画面表示
			for (String str: list){
				jtextarea.append(str + '\n');
			}
		} catch(NoSuchPaddingException e) {
			e.printStackTrace();
			jtextarea.setText(backupText);
            JOptionPane.showConfirmDialog(null, "NoSuchPaddingExceptionエラーが発生しました。INIファイル読込みに失敗しました。", "エラー", JOptionPane.CLOSED_OPTION);
			return;
		} catch(NoSuchAlgorithmException e) {
			e.printStackTrace();
			jtextarea.setText(backupText);
            JOptionPane.showConfirmDialog(null, "NoSuchAlgorithmExceptionエラーが発生しました。INIファイル読込みに失敗しました。", "エラー", JOptionPane.CLOSED_OPTION);
			return;
		} catch(BadPaddingException e) {
			e.printStackTrace();
			jtextarea.setText(backupText);
            JOptionPane.showConfirmDialog(null, "暗号復号化キーの値が間違っています。再度、設定後、INIファイル読込みを行ってください。", "エラー", JOptionPane.CLOSED_OPTION);
			return;
		} catch(IllegalBlockSizeException e) {
			e.printStackTrace();
			jtextarea.setText(backupText);
            JOptionPane.showConfirmDialog(null, "IllegalBlockSizeExceptionエラーが発生しました。INIファイル読込みに失敗しました。", "エラー", JOptionPane.CLOSED_OPTION);
			return;
		} catch(InvalidKeyException e) {
			e.printStackTrace();
			jtextarea.setText(backupText);
            JOptionPane.showConfirmDialog(null, "InvalidKeyExceptionエラーが発生しました。INIファイル読込みに失敗しました。", "エラー", JOptionPane.CLOSED_OPTION);
			return;
		} catch(IOException e) {
			e.printStackTrace();
			jtextarea.setText(backupText);
            JOptionPane.showConfirmDialog(null, "IOExceptionエラーが発生しました。INIファイル読込みに失敗しました。", "エラー", JOptionPane.CLOSED_OPTION);
			return;
		} finally {
			try {
				if (br != null) {
					br.close();
				}
			} catch (Exception e) {}
		}
		
		MenuIniCryptEditorEditorUtil.FILENAME = file.getPath();
	}

	/**
	  * ファイル上書き保存
	  */
	public void overWrite(JTextArea jtextarea){
		File file = new File(MenuIniCryptEditorEditorUtil.FILENAME);
		PrintWriter pw = null;
		try{
			pw = new PrintWriter(new FileWriter(file, false));
			// エディッタ内容
			String str = jtextarea.getText();
			String wkarray[] = str.split("\n");
			List<String> list = new ArrayList<String>();
			for (String line : wkarray) {
				list.add(line);
			}

			// 不要空行削除
			list = MenuIniCryptEditorCrypt.listTrim(list);

			// チェック処理
			if (!MenuIniCryptEditorCheck.check(jtextarea, list)) {
				return;
			}

			// 暗号化
			if (!MenuIniCryptEditorEditorUtil.isBlank(MenuIniCryptEditorEditorUtil.ENCRYPT_KEY)) {
				list = MenuIniCryptEditorCrypt.encrypto(list);
			}

			// ファイル出力
			for (String line : list) {
				pw.println(line);
			}
		} catch(NoSuchPaddingException e) {
			e.printStackTrace();
            JOptionPane.showConfirmDialog(null, "NoSuchPaddingExceptionエラーが発生しました。INIファイル書込みに失敗しました。", "エラー", JOptionPane.CLOSED_OPTION);
			return;
		} catch(NoSuchAlgorithmException e) {
			e.printStackTrace();
            JOptionPane.showConfirmDialog(null, "NoSuchAlgorithmExceptionエラーが発生しました。INIファイル書込みに失敗しました。", "エラー", JOptionPane.CLOSED_OPTION);
			return;
		} catch(BadPaddingException e) {
			e.printStackTrace();
            JOptionPane.showConfirmDialog(null, "BadPaddingExceptionエラーが発生しました。INIファイル書込みに失敗しました。", "エラー", JOptionPane.CLOSED_OPTION);
			return;
		} catch(IllegalBlockSizeException e) {
			e.printStackTrace();
            JOptionPane.showConfirmDialog(null, "IllegalBlockSizeExceptionエラーが発生しました。INIファイル書込みに失敗しました。", "エラー", JOptionPane.CLOSED_OPTION);
			return;
		} catch(InvalidKeyException e) {
			e.printStackTrace();
            JOptionPane.showConfirmDialog(null, "InvalidKeyExceptionエラーが発生しました。INIファイル書込みに失敗しました。", "エラー", JOptionPane.CLOSED_OPTION);
			return;
		} catch(IOException e) {
			e.printStackTrace();
            JOptionPane.showConfirmDialog(null, "IOExceptionエラーが発生しました。INIファイル書込みに失敗しました。", "エラー", JOptionPane.CLOSED_OPTION);
			return;
		} finally {
			try {
				if (pw != null) {
					pw.close();
				}
			} catch (Exception e) {}
		}
	}
}

/**
  * チェッククラス
  */
class MenuIniCryptEditorCheck {
	/**
	  * 保存前事前チェック
	  */
	public static boolean beforeSaveCheck() {
		if (MenuIniCryptEditorEditorUtil.isBlank(MenuIniCryptEditorEditorUtil.ENCRYPT_KEY)) {
			JOptionPane.showConfirmDialog(null, "暗号復号化キーが未設定です。暗号復号化キーを設定してください。", "エラー", JOptionPane.CLOSED_OPTION);
			return false;
		}
		return true;
	}
	/**
	  * 形式チェック
	  */
	public static boolean check(JTextArea jtextarea, List<String> list) {
		int line = 0;
		for (String str: list) {
			line++;
			if (MenuIniCryptEditorEditorUtil.trimTabSpace(str).startsWith(MenuIniCryptEditorEditorUtil.SHARP)) {
				continue;
			}
			String[] wkarray = str.split(MenuIniCryptEditorEditorUtil.EQUAL);
			// プロパティファイル形式でない。
			if (wkarray.length < 2) {
				JOptionPane.showConfirmDialog(null, line + "行目 プロパティファイル形式でありません(記号「=」なし)。", "エラー", JOptionPane.CLOSED_OPTION);
				return false;
			}
			String key = MenuIniCryptEditorEditorUtil.trimTabSpace(wkarray[0]);
			if (key.endsWith("\\h")) {
				JOptionPane.showConfirmDialog(null, line + "行目 KEY項目の後ろに全角空白あり。", "エラー", JOptionPane.CLOSED_OPTION);
				return false;
			}
			String value = MenuIniCryptEditorEditorUtil.trimTabSpace(wkarray[1]);
			if (value.startsWith("\\h")) {
				JOptionPane.showConfirmDialog(null, line + "行目 記号「=」の次に全角空白あり。", "エラー", JOptionPane.CLOSED_OPTION);
				return false;
			}
		}

		return true;
	}
}

/**
  * 暗号復号化クラス
  */
class MenuIniCryptEditorCrypt {
	/**
	  * 暗号化
	  */
	public static List<String> encrypto(List<String> list) throws NoSuchPaddingException, NoSuchAlgorithmException, BadPaddingException, IllegalBlockSizeException, InvalidKeyException {
		List<String> ret = new ArrayList<String>();

		// 16バイトに成形
		byte[] wksecretKeyBytes = MenuIniCryptEditorEditorUtil.ENCRYPT_KEY.getBytes();
		byte[] secretKeyBytes = MenuIniCryptEditorEditorUtil.CRYPTKEY_BASE;
		for (int i = 0; i < wksecretKeyBytes.length; i++) {
			secretKeyBytes[i] = wksecretKeyBytes[i];
		}

		// データ
		for (String line : list) {
			if (isTarget(line)) {
				int pos = line.indexOf("=");
				String itemname = line.substring(0, pos + 1);
				String originalData = line.substring( pos + 1);
				byte[] data = originalData.getBytes(MenuIniCryptEditorEditorUtil.CHARSET);

				//encrypt
				SecretKeySpec secretKeySpec = new SecretKeySpec(secretKeyBytes, MenuIniCryptEditorEditorUtil.AES);
				Cipher cipher = Cipher.getInstance(MenuIniCryptEditorEditorUtil.AES);
				cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
				byte[] encryptBytes = cipher.doFinal(data);
				byte[] encryptBytesBase64 = Base64.getEncoder().encode(encryptBytes);
				ret.add(itemname + new String(encryptBytesBase64, MenuIniCryptEditorEditorUtil.CHARSET));
			} else {
				ret.add(line);
			}
		}
		return ret;
	}

	/**
	  * 復号化
	  */
	public static List<String> decrypto(List<String> list) throws NoSuchPaddingException, NoSuchAlgorithmException, BadPaddingException, IllegalBlockSizeException, InvalidKeyException {
		List<String> ret = new ArrayList<String>();

		// 16バイトに成形
		byte[] wksecretKeyBytes = MenuIniCryptEditorEditorUtil.ENCRYPT_KEY.getBytes();
		byte[] secretKeyBytes = MenuIniCryptEditorEditorUtil.CRYPTKEY_BASE;
		for (int i = 0; i < wksecretKeyBytes.length; i++) {
			secretKeyBytes[i] = wksecretKeyBytes[i];
		}

		// データ
		for (String line : list) {
			if (isTarget(line)) {
				int pos = line.indexOf("=");
				String itemname = line.substring(0, pos + 1);
				String originalData = line.substring( pos + 1);
				byte[] data = originalData.getBytes(MenuIniCryptEditorEditorUtil.CHARSET);

				//decrypt
				byte[] decryptBytes = Base64.getDecoder().decode(new String(data, MenuIniCryptEditorEditorUtil.CHARSET));
				SecretKeySpec secretKeySpec = new SecretKeySpec(secretKeyBytes, MenuIniCryptEditorEditorUtil.AES);
				Cipher cipher = Cipher.getInstance(MenuIniCryptEditorEditorUtil.AES);
				cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
				byte[] originalBytes = cipher.doFinal(decryptBytes);
				ret.add(itemname + new String(originalBytes, MenuIniCryptEditorEditorUtil.CHARSET));
			} else {
				ret.add(line);
			}
		}
		return ret;
	}

	/**
	  * 不要空行削除
	  */
	public static List<String> listTrim(List<String> list) {
		List<String> ret = new ArrayList<String>();
		// データ
		for (String line : list) {
			if (MenuIniCryptEditorEditorUtil.trimTabSpace(line).length() > 0) {
				ret.add(line);
			}
		}
		return ret;
	}

	/**
	 * 対象かどうか。
	 */
	private static boolean isTarget(String str) {
		System.out.println("str;" + str);
		if (MenuIniCryptEditorEditorUtil.isBlank(str)) {
			return false;
		}
		if (MenuIniCryptEditorEditorUtil.trimTabSpace(str).startsWith(MenuIniCryptEditorEditorUtil.SHARP)) {
			return false;
		}
		// プロパティファイルより読み込んだINIファイル暗号化復号化キー前方一致文字※複数
		if (MenuIniCryptEditorEditorUtil.isBlank(MenuIniCryptEditorEditorUtil.CRYPTKEY_STARTWITHS)) {
			return false;
		}
		for (String strStartWith : MenuIniCryptEditorEditorUtil.CRYPTKEY_STARTWITHS) {
			String[] wkarray = str.split(MenuIniCryptEditorEditorUtil.EQUAL);
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
}

/**
  * Utilクラス
  */
class MenuIniCryptEditorEditorUtil {

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
	static final String CONMA = ",";
	/**
	  * イコール
	  */
	static final String EQUAL = "=";

	/**
	  * 暗号復号化方式
	  */
	static final String AES = "AES";

	/**
	  * INIファイル文字コード
	  */
	static final Charset CHARSET = StandardCharsets.UTF_8;

	/**
	  * プロパティファイル
	  */
	static final String PROPNAME = "menuinicrypteditor";

	/**
	  * プロパティファイル記載のINIファイルパスキー
	  */
	static final String INI_FILE_PATH_KEY = "inifilepath";

	/**
	  * プロパティファイル記載のINIファイル暗号化復号化キー
	  */
	static final String CRYPTKEY_STARTWITHS_KEY = "cryptKeyStartWiths";

	/**
	  * プロパティファイル記載のINIファイル暗号化復号化キー(size;16, 24, 32)
	  */
	static final byte[] CRYPTKEY_BASE   = "1111111111111111".getBytes(CHARSET);

	/**
	  * プロパティファイルより読み込んだ対象INIファイル
	  */
	static String FILENAME_FROM_PROP = "";

	/**
	  * プロパティファイルより読み込んだINIファイル暗号化復号化キー前方一致文字※複数
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
	  * 新規フラグ
	  */
	static boolean NEW_FLG = false;


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
}
