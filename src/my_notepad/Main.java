package my_notepad;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class Main {

	public static final int SUCCESS = 0;
	public static final int FAIL = -1;
	private static boolean isModified = false;

	public static void main(String[] args) {

		JFrame frame = new JFrame();

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
		JMenuItem mPaste = new JMenuItem("Paste");
		mEdit.add(mDateTime);
		mEdit.add(mPaste);

		// Help
		JMenu mOptHelp = new JMenu("Help");
		JMenuItem mAbout = new JMenuItem("About");
		mOptHelp.add(mAbout);

		// Construct mBar
		mMenuBar.add(mOptFile);
		mMenuBar.add(mEdit);
		mMenuBar.add(mOptHelp);

		// Add input to frame
		frame.setJMenuBar(mMenuBar);
		frame.add(inputScr);

		// Action Events
		mSave.addActionListener(_ -> saveFile(frame, "Enter a name for the file: ", input));
		mOpen.addActionListener(_ -> openFile(frame, "Enter the name of file to open: ", input));
		mExit.addActionListener(_ -> frame.dispose());
		mDateTime.addActionListener(_ -> writeDateTime(input));
		mPaste.addActionListener(_ -> pasteFromClipBoard(input));
		mAbout.addActionListener(_ -> printAbout(frame));
		mNew.addActionListener(_ -> checkBeforeNewFile(frame, input));

		input.getDocument().addDocumentListener(new DocumentListener() {
			public void insertUpdate(DocumentEvent e) { isModified = true; }
			public void removeUpdate(DocumentEvent e) { isModified = true; }
			public void changedUpdate(DocumentEvent e) { isModified = true; }
		});

		// Icon
		ImageIcon icon = new ImageIcon("asset/icon.png");
		frame.setIconImage(icon.getImage());

		frame.setVisible(true);
		frame.setResizable(true);
		frame.setTitle("Goatpad");
		frame.pack();
		frame.setSize(400,400);
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				checkBeforeExit(frame, input);
			}
		});

	}

	public static void printAbout(JFrame frame) {
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

	public static void writeDateTime(JTextArea area) {
		Date curr = new Date();
		area.insert(curr.toString(), area.getCaretPosition());
	}

	public static void newFile(JFrame frame, JTextArea area) {
		frame.setTitle("Goatpad");
		area.setText("");
	}

	public static void checkBeforeNewFile(JFrame frame, JTextArea area) {
		if (isModified) {
			int choice = JOptionPane.showConfirmDialog(
					frame,
					"You have unsaved changes.\nSave before making new file?",
					"Warning",
					JOptionPane.YES_NO_CANCEL_OPTION);
			if (choice == JOptionPane.YES_OPTION) {
				if (saveFile(frame, "Enter file name to save as: ", area) == FAIL){
					return;
				}
			} else if (choice == JOptionPane.CANCEL_OPTION
					|| choice == JOptionPane.NO_OPTION) {
				return;
			}
			newFile(frame, area);
		}
	}

	public static void checkBeforeExit(JFrame frame, JTextArea area) {
		if (isModified) {
			int choice = JOptionPane.showConfirmDialog(
					frame,
					"You have unsaved changes.\nSave before exiting?",
					"Warning",
					JOptionPane.YES_NO_CANCEL_OPTION);
			if (choice == JOptionPane.YES_OPTION) {
				if (saveFile(frame, "Enter file name to save as: ", area) == FAIL) {
					return;
				} else {
					frame.dispose();
				}
			} else if (choice == JOptionPane.CANCEL_OPTION) {
				return;
			} else if (choice == JOptionPane.NO_OPTION) {
				frame.dispose();
			}
			newFile(frame, area);
		}
	}

	public static int saveFile(JFrame frame, String prompt, JTextArea area) {
		String fName = getFileName(frame, prompt);
		if (fName == null || fName.isEmpty()) {
			return FAIL;
		}

		if (saveFromTextArea(frame, area, fName) == SUCCESS) {
			isModified = false;
			frame.setTitle("Goatpad - " + fName);
			return SUCCESS;
		} else {
			return FAIL;
		}
	}

	public static int openFile(JFrame frame, String prompt, JTextArea area) {
		String fName = getFileName(frame, prompt);
		if (writeToTextArea(frame, area, fName) == SUCCESS) {
			frame.setTitle("Goatpad - " + fName);
			isModified = false;
			return SUCCESS;
		} else {
			return FAIL;
		}
	}

	public static String getFileName(JFrame frame, String prompt) {
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

		in.addActionListener(_ ->{
			fName.set(in.getText());
			d.dispose();
		});

		ok.addActionListener(_ ->{
			fName.set(in.getText());
			d.dispose();
		});

		canc.addActionListener(_ -> {
			fName.set(null);
			d.dispose();
		});

		d.pack();
		d.setLocationRelativeTo(null);
		d.setVisible(true);

		try {
			if (fName.get() == null || fName.get().isEmpty())
				makeErrorDialog(frame, "Input field left blank.");
			return null;
		} catch (NullPointerException e) {
			e.printStackTrace();
		}
		System.out.println(fName.get());
		return fName.get();
	}

	public static int saveFromTextArea(JFrame frame, JTextArea area, String fileName) {
		String contents = area.getText();

		try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
			writer.write(contents);
		} catch (IOException e) {
			e.printStackTrace();
			makeErrorDialog(frame, "The input/output stream was interrupted.");
			return FAIL;
		}
		return SUCCESS;
	}

	public static int writeToTextArea(JFrame frame, JTextArea area, String fileName) {
		if (fileName == null) {
			return FAIL;
		}

		area.setText("");
		try (BufferedReader reader = new BufferedReader(new FileReader(fileName))){
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

	// TODO theres literally a method for this in JTextComponent
	public static void copyToClipBoard(String str) {

	}

	public static void pasteFromClipBoard(JTextArea area) {
		String str = getClipBoard();
		area.insert(str, area.getCaretPosition());
	}

	private static String getClipBoard(){
		try {
			return (String)Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
		} catch (HeadlessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();            
		} catch (UnsupportedFlavorException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();            
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "";
	}

	public static void makeErrorDialog(JFrame frame, String text) {
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
