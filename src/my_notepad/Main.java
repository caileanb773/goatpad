package my_notepad;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import javax.swing.undo.UndoManager;

public class Main {

	private static final int SUCCESS = 0;
	private static final int FAIL = -1;
	private static boolean isModified = false;
	private static UndoManager undoMgr = new UndoManager();

	public static void main(String[] args) {

		JFrame frame = new JFrame();
		Main m = new Main();

		// Text input area
		JTextArea input = new JTextArea();
		input.setEditable(true);
		input.setLayout(new BorderLayout());
		input.setBorder(new EmptyBorder(5,5,5,5));
		input.setFont(new Font("Consolas", Font.PLAIN, 14));
		JScrollPane inputScr = new JScrollPane(input);

		// Menu bar
		JMenuBar mMenuBar = new JMenuBar();

		// File
		JMenu mOptFile = new JMenu("File");
		JMenuItem mSave = new JMenuItem("Save");
		JMenuItem mOpen = new JMenuItem("Open");
		JMenuItem mExit = new JMenuItem("Exit");
		JMenuItem mNew = new JMenuItem("New");
		mOptFile.add(mNew);
		mOptFile.add(mSave);
		mOptFile.add(mOpen);
		mOptFile.add(mExit);

		// Edit
		JMenu mEdit = new JMenu("Edit");
		JMenuItem mDateTime  = new JMenuItem("Date/Time");
		JMenuItem mCopy = new JMenuItem("Copy");
		JMenuItem mCut = new JMenuItem("Cut");
		JMenuItem mPaste = new JMenuItem("Paste");
		JMenuItem mUndo = new JMenuItem("Undo");
		JMenuItem mRedo = new JMenuItem("Redo");
		mEdit.add(mCopy);
		mEdit.add(mCut);
		mEdit.add(mPaste);
		mEdit.add(mUndo);
		mEdit.add(mRedo);
		mEdit.add(mDateTime);

		//
		JMenu mTools = new JMenu("Tools");
		JMenuItem mWCount = new JMenuItem("Word Count");
		mTools.add(mWCount);

		// Help
		JMenu mOptHelp = new JMenu("Help");
		JMenuItem mAbout = new JMenuItem("About");
		mOptHelp.add(mAbout);

		// Construct mBar
		mMenuBar.add(mOptFile);
		mMenuBar.add(mEdit);
		mMenuBar.add(mTools);
		mMenuBar.add(mOptHelp);

		// Add input to frame
		frame.setJMenuBar(mMenuBar);
		frame.add(inputScr);

		// Action Events
		mSave.addActionListener(_ -> m.saveFile(frame, input));
		mOpen.addActionListener(_ -> m.checkBeforeOpen(frame, input));
		mExit.addActionListener(_ -> m.checkBeforeExit(frame, input));
		mUndo.addActionListener(_ -> m.undo());
		mRedo.addActionListener(_ -> m.redo());
		mCopy.addActionListener(_ -> input.copy());
		mCut.addActionListener(_ -> input.cut());
		mPaste.addActionListener(_ -> input.paste());
		mDateTime.addActionListener(_ -> m.writeDateTime(input));
		mAbout.addActionListener(_ -> m.printAbout(frame));
		mNew.addActionListener(_ -> m.checkBeforeNewFile(frame, input));
		mWCount.addActionListener(_ -> m.displayWordCount(frame, input));

		// Setup undo/redo functionality
		Main.setupUndoFunc(input, Main.undoMgr);

		input.getDocument().addDocumentListener(new DocumentListener() {
			public void insertUpdate(DocumentEvent e) { isModified = true; 
			System.out.println("isModified is now: " + isModified);}
			public void removeUpdate(DocumentEvent e) { isModified = true; 
			System.out.println("isModified is now: " + isModified);}
			public void changedUpdate(DocumentEvent e) { isModified = true; 
			System.out.println("isModified is now: " + isModified);}
		});

		input.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK), "save");
		input.getActionMap().put("save", new AbstractAction() {
			private static final long serialVersionUID = 1L;
			@Override
			public void actionPerformed(ActionEvent e) {
				m.saveFile(frame, input);
			}
		});

		input.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK), "open");
		input.getActionMap().put("open", new AbstractAction() {
			private static final long serialVersionUID = 1L;
			@Override
			public void actionPerformed(ActionEvent e) {
				m.checkBeforeOpen(frame, input);
			}
		});

		// Icon
		ImageIcon icon = new ImageIcon("asset/icon.png");
		frame.setIconImage(icon.getImage());
		frame.setVisible(true);
		frame.setResizable(true);
		frame.setTitle("Goatpad - Untitled");
		frame.pack();
		frame.setSize(400,400);
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				m.checkBeforeExit(frame, input);
			}
		});

	}

	private static void setupUndoFunc(JTextArea area, UndoManager undoMgr) {
		Document doc = area.getDocument();
		doc.addUndoableEditListener(e -> undoMgr.addEdit(e.getEdit()));
		area.getInputMap().put(KeyStroke.getKeyStroke("ctrl Z"), "undoAction");
		area.getActionMap().put("undoAction", new AbstractAction() {
			private static final long serialVersionUID = 1L;
			@Override
			public void actionPerformed(ActionEvent e) {
				if (undoMgr.canUndo()) {
					undoMgr.undo();
				}
			}
		});

		area.getInputMap().put(KeyStroke.getKeyStroke("ctrl Y"), "redoAction");
		area.getActionMap().put("redoAction", new AbstractAction() {
			private static final long serialVersionUID = 1L;
			@Override
			public void actionPerformed(ActionEvent e) {
				if (undoMgr.canRedo()) {
					undoMgr.redo();
				}
			}
		});
	}

	public void undo() {
		if (Main.undoMgr.canUndo()) {
			Main.undoMgr.undo();
		}
	}

	public void redo() {
		if (Main.undoMgr.canRedo()) {
			Main.undoMgr.redo();
		}
	}

	public void printAbout(JFrame frame) {
		StringBuilder sb = new StringBuilder();
		String temp = "";

		try (BufferedReader reader = new BufferedReader(new FileReader("asset/credits.txt"))) {
			while ((temp = reader.readLine()) != null) {
				sb.append(temp).append("\n");
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			makeErrorDialog(frame, "Credits.txt not found.");
		} catch (IOException e) {
			e.printStackTrace();
		}
		JDialog d = new JDialog(frame, "About", true);
		JLabel about = new JLabel("<html><pre>" + sb.toString() + "</pre></html>");
		about.setBorder(new EmptyBorder(5,10,5,10));
		d.setSize(300,400);
		d.add(about, BorderLayout.CENTER);
		d.pack();
		d.setLocationRelativeTo(null);
		d.setVisible(true);
	}

	public void writeDateTime(JTextArea area) {
		Date curr = new Date();
		area.insert(curr.toString(), area.getCaretPosition());
	}

	public void newFile(JFrame frame, JTextArea area) {
		frame.setTitle("Goatpad - Untitled");
		area.setText("");
		isModified = false;
	}

	public void checkBeforeOpen(JFrame frame, JTextArea area) {
		if (isModified) {
			int choice = JOptionPane.showConfirmDialog(
					frame,
					"You have unsaved changes.\nSave before opening new file?",
					"Warning",
					JOptionPane.YES_NO_CANCEL_OPTION);
			if (choice == JOptionPane.YES_OPTION) {
				if (saveFile(frame, area) == FAIL){
					return;
				}
			} else if (choice == JOptionPane.CANCEL_OPTION
					|| choice == JOptionPane.CLOSED_OPTION) {
				return;
			} else {
				openFile(frame, area);
			}
		} else {
			openFile(frame, area);
		}
	}

	public void checkBeforeNewFile(JFrame frame, JTextArea area) {
		if (isModified == true) {
			int choice = JOptionPane.showConfirmDialog(
					frame,
					"You have unsaved changes.\nSave before making new file?",
					"Warning",
					JOptionPane.YES_NO_CANCEL_OPTION);
			if (choice == JOptionPane.YES_OPTION) {
				if (saveFile(frame, area) == FAIL){
					return;
				}
			} else if (choice == JOptionPane.CANCEL_OPTION
					|| choice == JOptionPane.CLOSED_OPTION) {
				return;
			} else {
				newFile(frame, area);
			}
		} else {
			newFile(frame, area);
		}
	}

	public void checkBeforeExit(JFrame frame, JTextArea area) {
		if (isModified) {
			int choice = JOptionPane.showConfirmDialog(
					frame,
					"You have unsaved changes.\nSave before exiting?",
					"Warning",
					JOptionPane.YES_NO_CANCEL_OPTION);
			if (choice == JOptionPane.YES_OPTION) {
				if (saveFile(frame, area) == FAIL) {
					return;
				} else {
					frame.dispose();
				}
			} else if (choice == JOptionPane.CANCEL_OPTION
					|| choice == JOptionPane.CLOSED_OPTION) {
				return;
			} else if (choice == JOptionPane.NO_OPTION) {
				frame.dispose();
			}
			newFile(frame, area);
		} else {
			frame.dispose();
		}
	}

	public int saveFile(JFrame frame, JTextArea area) {
		JFileChooser fileChooser = new JFileChooser();
		int opt = fileChooser.showSaveDialog(frame);
		int confirm = 0;

		if (opt == JFileChooser.APPROVE_OPTION) {
			File file = fileChooser.getSelectedFile();

			if (file.exists()) {
				confirm = JOptionPane.showConfirmDialog(
						frame,
						"The file already exists. Do you want to overwrite it?",
						"Confirm Overwrite",
						JOptionPane.YES_NO_OPTION);
			}

			if (confirm != JOptionPane.YES_OPTION) {
				return FAIL;
			}
			try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))){
				writer.write(area.getText());
			} catch (IOException e) {
				JOptionPane.showMessageDialog(frame, "Error saving file.", "Error", JOptionPane.ERROR_MESSAGE);
				return FAIL;
			}
			isModified = false;
			System.out.println("isModified is now: " + isModified);
			frame.setTitle("Goatpad - " + file.getName());
			return SUCCESS;
		}
		return FAIL;
	}

	public int openFile(JFrame frame, JTextArea area) {
		JFileChooser fileChooser = new JFileChooser();
		int opt = fileChooser.showOpenDialog(frame);

		if (opt == JFileChooser.APPROVE_OPTION) {
			File file = fileChooser.getSelectedFile();
			try (BufferedReader reader = new BufferedReader (new FileReader(file))){
				area.setText("");
				String line;
				while ((line = reader.readLine()) != null) {
					area.append(line + "\n");
				}
			} catch (IOException e) {
				JOptionPane.showMessageDialog(frame, "Error opening file.", "Error", JOptionPane.ERROR_MESSAGE);
				return FAIL;
			}
			frame.setTitle("Goatpad - " + file.getName());
			isModified = false;
			System.out.println("isModified is now " + isModified);
			return SUCCESS;
		}
		System.out.println("openFile returned FAIL after try/catch block.");
		return FAIL;
	}

	public int wordCount(JTextArea area) {
		String content = area.getText();
		if (content.trim().isEmpty()) return 0;
		String[] words = content.trim().split("\\s+");
		return words.length;
	}

	public void displayWordCount(JFrame frame, JTextArea area) {
		JDialog d = new JDialog(frame, "Word Count", true);
		d.setLayout(new FlowLayout());
		int count = wordCount(area);
		JLabel text = new JLabel("The document contains " + count + " words.");
		JButton ok = new JButton("Ok");
		ok.addActionListener(_ -> d.dispose());
		d.add(text);
		d.add(ok);
		d.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					d.dispose();
				}
			}
		});
		ok.addActionListener(_ -> {
			d.dispose();
		});

		d.setFocusable(true);
		d.setFocusableWindowState(true);
		d.pack();
		d.setLocationRelativeTo(null);
		d.setVisible(true);
	}

	public String getFileName(JFrame frame, String prompt) {
		AtomicReference<String> fName = new AtomicReference<>(null);

		JDialog d = new JDialog(frame, prompt, true);
		d.setLayout(new FlowLayout());
		JButton ok = new JButton("OK");
		JButton canc = new JButton("Cancel");
		JTextField in = new JTextField(10);
		d.add(new JLabel(prompt));
		d.add(in);
		d.add(ok);
		d.add(canc);

		in.addActionListener(_ -> {
			if (!in.getText().trim().isEmpty()) {
				fName.set(in.getText().trim());
				d.dispose();
			} else {
				makeErrorDialog(frame, "Input field left blank.");
			}
		});

		ok.addActionListener(_ -> {
			if (!in.getText().trim().isEmpty()) {
				fName.set(in.getText().trim());
				d.dispose();
			} else {
				makeErrorDialog(frame, "Input field left blank.");
			}
		});

		canc.addActionListener(_ -> {
			fName.set(null);
			d.dispose();
		});

		d.pack();
		d.setLocationRelativeTo(null);
		d.setVisible(true);

		return fName.get();
	}

	public int saveFromTextArea(JFrame frame, JTextArea area, String fileName) {
		String contents = area.getText();

		try (BufferedWriter writer = new BufferedWriter(new FileWriter("tests/" + fileName))) {
			writer.write(contents);
		} catch (IOException e) {
			e.printStackTrace();
			makeErrorDialog(frame, "The input/output stream was interrupted.");
			return FAIL;
		}
		return SUCCESS;
	}

	public int writeToTextArea(JFrame frame, JTextArea area, String fileName) {
		if (fileName == null) {
			return FAIL;
		}

		area.setText("");
		try (BufferedReader reader = new BufferedReader(new FileReader("tests/" + fileName))){
			String line = reader.readLine();
			while (line != null) {
				area.append(line + "\n");
				line = reader.readLine();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			makeErrorDialog(frame, "Could not find that file.");
			return FAIL;
		} catch (IOException e) {
			e.printStackTrace();
			makeErrorDialog(frame, "The input/output stream was interrupted.");
			return FAIL;
		}
		return SUCCESS;
	}

	public void pasteFromClipBoard(JTextArea area) {
		String str = getClipBoard();
		area.insert(str, area.getCaretPosition());
	}

	private String getClipBoard(){
		try {
			return (String)Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
		} catch (HeadlessException e) {
			e.printStackTrace();            
		} catch (UnsupportedFlavorException e) {
			e.printStackTrace();            
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "";
	}

	public void makeErrorDialog(JFrame frame, String text) {
		JDialog d = new JDialog(frame, "Error!", true);
		d.setLayout(new FlowLayout());
		d.add(new JLabel(text));
		JButton ok = new JButton("OK");
		d.add(ok);
		d.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					d.dispose();
				}
			}
		});
		ok.addActionListener(_ -> {
			d.dispose();
		});

		d.setFocusable(true);
		d.setFocusableWindowState(true);
		d.pack();
		d.setLocationRelativeTo(null);
		d.setVisible(true);
	}

}
