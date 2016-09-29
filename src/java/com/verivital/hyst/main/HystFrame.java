package com.verivital.hyst.main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.DefaultCaret;

import com.verivital.hyst.passes.TransformationPass;
import com.verivital.hyst.printers.ToolPrinter;

@SuppressWarnings("serial")
public class HystFrame extends JFrame implements ActionListener, WindowListener, DocumentListener
{
	JTabbedPane tabs = new JTabbedPane();

	HintTextField xmlTf = new HintTextField("(Select model .xml file)", 25);
	JButton xmlButton = new JButton("Set Model Path (.xml)");

	HintTextField cfgTf = new HintTextField("(Select model .cfg file)", 25);
	JButton cfgButton = new JButton("Set Config Path (.cfg)");

	HintTextField outTf = new HintTextField("(stdout)", 25);
	JButton outButton = new JButton("Set Output Path");

	JRadioButton printNormal = new JRadioButton("Normal Printing", true);
	JRadioButton printVerbose = new JRadioButton("Verbose Printing");
	JRadioButton printDebug = new JRadioButton("Debug (and Verbose) Printing");

	boolean skipDocumentEvents = false;
	ArrayList<String> printerParams;
	JComboBox<String> printerBox;
	String TOOL_PARAM_HELP = "(tool parameters)";
	HintTextField printerParamTf = new HintTextField(TOOL_PARAM_HELP, 15);
	JButton printerButton = new JButton("Param Help");

	ArrayList<JComboBox<String>> passBoxes = new ArrayList<JComboBox<String>>();
	ArrayList<HintTextField> passParams = new ArrayList<HintTextField>();
	ArrayList<JButton> passButtons = new ArrayList<JButton>();

	JPanel passPanel = new JPanel();
	JButton addPassButton = new JButton("Add Another Transformation Pass");

	JButton runButton = new JButton("Convert");

	JTextArea outputArea = new JTextArea();
	StringBuffer outputBuffer = new StringBuffer();

	private Vector<String> passNames;
	private ToolPrinter[] printers;
	private ArrayList<TransformationPass> availablePasses = new ArrayList<TransformationPass>();

	final static private JFileChooser fileChooser = new JFileChooser();
	final private static String GUISTATE_FILENAME = ".hyst.xml";
	private static boolean shouldUpdate = false;

	public HystFrame(ToolPrinter[] printers, TransformationPass[] passes)
	{
		this.printers = printers;
		availablePasses.addAll(Arrays.asList(passes));

		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setTitle("Hyst: Hybrid Model Transformation and Translation Tool");
		addWindowListener(this);

		passNames = new Vector<String>();
		passNames.add("(Select Transformation Pass)");
		passNames.addAll(extractPassNames(passes));

		tabs.add(makeOptionsPanel(), "Options");

		tabs.add(makeOutputPanel(), "Output");

		getContentPane().add(tabs);

		loadFromGuiState(GuiState.load(GUISTATE_FILENAME));

		// default button
		JRootPane rootPane = SwingUtilities.getRootPane(runButton);
		rootPane.setDefaultButton(runButton);

		KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
		manager.addKeyEventDispatcher(new KeyEventDispatcher()
		{
			@Override
			public boolean dispatchKeyEvent(KeyEvent e)
			{
				boolean rv = false;

				if (e.getKeyCode() == KeyEvent.VK_Q && e.isControlDown())
				{
					dispose();
					rv = true;
				}

				return rv;
			}

		});

		pack();
		setLocationRelativeTo(null);
	}

	private void loadFromGuiState(GuiState gs)
	{
		xmlTf.setText(gs.getXml());
		cfgTf.setText(gs.getCfg());
		outTf.setText(gs.getOutput());

		int debugMode = gs.getDebugMode();

		if (debugMode == GuiState.NORMAL)
			printNormal.setSelected(true);
		else if (debugMode == GuiState.VERBOSE)
			printVerbose.setSelected(true);
		else if (debugMode == GuiState.DEBUG)
			printDebug.setSelected(true);

		ArrayList<Integer> passIndex = gs.getPassIndex();
		ArrayList<String> params = gs.getPassParam();
		int numPasses = passIndex.size();

		if (numPasses > 20)
		{
			System.err.println("Truncating numPasses to 20.");
			numPasses = 20;
		}

		for (int i = 1; i < numPasses; ++i)
			addPass();

		for (int i = 0; i < numPasses; ++i)
		{
			int pi = passIndex.get(i);
			String param = params.get(i);

			if (pi < passBoxes.get(i).getItemCount())
				passBoxes.get(i).setSelectedIndex(pi);

			passParams.get(i).setText(param);
		}

		int pi = gs.getPrinterIndex();
		printerParams = gs.getPrinterParams();

		if (pi < printerBox.getItemCount())
		{
			while (printerParams.size() > printers.length)
				printerParams.remove(printerParams.size() - 1);

			while (printerParams.size() < printers.length)
				printerParams.add("");

			printerBox.setSelectedIndex(pi);
		}
		else
			Hyst.logError("Saved GUI printer index was out of bounds.");
	}

	private JPanel makeOutputPanel()
	{
		JPanel rv = new JPanel();

		rv.setLayout(new BorderLayout());

		outputArea.setLineWrap(false);
		outputArea.setEditable(false);
		DefaultCaret caret = (DefaultCaret) outputArea.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

		// arg, this seems harder than it needs to be
		outputArea.setEnabled(false);
		Color trColor = outputArea.getBackground();
		outputArea.setEnabled(true);

		// setBackground doesn't seem to react to textResource colors
		Color color = new Color(trColor.getRed(), trColor.getGreen(), trColor.getBlue());
		outputArea.setBackground(color);

		JScrollPane sp = new JScrollPane(outputArea);

		rv.add(sp, BorderLayout.CENTER);

		return rv;
	}

	public void addOutput(final String s)
	{
		outputBuffer.append(s + "\n");

		if (shouldUpdate == false)
		{
			shouldUpdate = true;

			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					shouldUpdate = false;
					outputArea.setText(outputBuffer.toString());
				}
			});
		}
	}

	private JPanel makeOptionsPanel()
	{
		JPanel rv = new JPanel();

		rv.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(5, 5, 5, 5);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1;

		xmlTf.setEnabled(false);
		rv.add(xmlTf, c);
		c.gridx = 1;
		rv.add(xmlButton, c);
		xmlButton.addActionListener(this);
		c.gridx = 0;
		c.gridy++;

		cfgTf.setEnabled(false);
		rv.add(cfgTf, c);
		c.gridx = 1;
		rv.add(cfgButton, c);
		cfgButton.addActionListener(this);
		c.gridx = 0;
		c.gridy++;

		outTf.setEnabled(false);
		rv.add(outTf, c);
		c.gridx = 1;
		rv.add(outButton, c);
		outButton.addActionListener(this);
		c.gridx = 0;
		c.gridy++;

		Vector<String> printerNames = extractPrinterNames(printers);

		// printer panel (3 parts)
		JPanel printerPanel = new JPanel();
		printerPanel.setLayout(new GridBagLayout());

		GridBagConstraints c2 = new GridBagConstraints();
		c2.insets = new Insets(5, 5, 5, 5);

		c2.gridx = 0;
		c2.gridy = 0;
		c2.fill = GridBagConstraints.HORIZONTAL;
		c2.weightx = 0;

		printerBox = new JComboBox<String>(printerNames);
		printerPanel.add(printerBox, c2);
		printerBox.addActionListener(this);

		c2.gridx = 1;
		c2.weightx = 1;
		printerPanel.add(printerParamTf, c2);
		printerParamTf.getDocument().addDocumentListener(this);

		c2.gridx = 2;
		c2.weightx = 0;

		printerButton.addActionListener(this);
		printerPanel.add(printerButton, c2);

		c.gridwidth = 2;
		rv.add(printerPanel, c);
		c.gridy++;

		// printer type (normal, verbose, debug)
		JPanel p = new JPanel();
		p.setLayout(new FlowLayout());

		ButtonGroup bg = new ButtonGroup();
		bg.add(printNormal);
		bg.add(printVerbose);
		bg.add(printDebug);

		p.add(printNormal);
		p.add(Box.createRigidArea(new Dimension(15, 0)));
		p.add(printVerbose);
		p.add(Box.createRigidArea(new Dimension(15, 0)));
		p.add(printDebug);

		rv.add(p, c);
		c.gridy++;

		passPanel.setLayout(new GridBagLayout());
		rv.add(passPanel, c);
		c.gridy++;

		c.fill = GridBagConstraints.NONE;
		rv.add(addPassButton, c);
		addPassButton.addActionListener(this);
		c.gridy++;

		c.insets = new Insets(15, 5, 5, 5);
		rv.add(runButton, c);
		runButton.addActionListener(this);

		// add a single pass for width considerations
		addPass();

		return rv;
	}

	private ToolPrinter getReleasePrinter(int desiredIndex)
	{
		ToolPrinter rv = null;
		int index = 0;

		for (ToolPrinter tp : printers)
		{
			if (tp.isInRelease() && index++ == desiredIndex)
			{
				rv = tp;
				break;
			}
		}

		return rv;
	}

	private Vector<String> extractPrinterNames(ToolPrinter[] printers)
	{
		Vector<String> rv = new Vector<String>(printers.length);

		for (ToolPrinter tp : printers)
		{
			if (tp.isInRelease())
			{
				String param = tp.getCommandLineFlag();

				rv.add(tp.getToolName() + " " + param);
			}
		}

		return rv;
	}

	private ArrayList<String> extractPassNames(TransformationPass[] passes)
	{
		ArrayList<String> rv = new ArrayList<String>();

		for (TransformationPass tp : passes)
		{
			String s = tp.getName();

			rv.add(s);
		}

		return rv;
	}

	private String getPath(boolean open, String prevPath)
	{
		String rv = null;

		// System.out.println("Setting file to " + prevPath);

		if (prevPath.length() > 0)
			fileChooser.setSelectedFile(new File(prevPath));

		// first get the list of files
		int fileChooserRv = 0;

		if (open)
			fileChooserRv = fileChooser.showOpenDialog(this);
		else
			fileChooserRv = fileChooser.showSaveDialog(this);

		if (fileChooserRv == JFileChooser.APPROVE_OPTION)
			rv = fileChooser.getSelectedFile().getAbsolutePath();

		return rv;
	}

	private void addPass()
	{
		JComboBox<String> cb = new JComboBox<String>(passNames);
		HintTextField tf = new HintTextField("(pass parameters)", 15);

		GridBagConstraints c = new GridBagConstraints();
		c.weightx = 0;
		c.insets = new Insets(5, 5, 5, 5);
		c.fill = GridBagConstraints.HORIZONTAL;

		c.gridx = 0;
		c.gridy = passBoxes.size();

		passPanel.add(cb, c);

		c.gridx = 1;
		c.weightx = 1;
		passPanel.add(tf, c);

		c.gridx = 2;
		c.weightx = 0;
		JButton b = new JButton("Param Help");

		b.addActionListener(this);
		passPanel.add(b, c);

		pack();

		passBoxes.add(cb);
		passParams.add(tf);
		passButtons.add(b);
	}

	// hint text field
	// from
	// http://stackoverflow.com/questions/1738966/java-jtextfield-with-input-hint
	class HintTextField extends JTextField implements FocusListener
	{
		private final String hint;
		private boolean showingHint;
		private Color orig;

		public HintTextField(final String hint, int len)
		{
			super(hint, len);
			this.hint = hint;
			this.showingHint = true;
			orig = super.getForeground();
			super.setForeground(Color.gray);
			super.addFocusListener(this);
		}

		public void updateHint()
		{
			if (this.getText().isEmpty())
			{
				super.setText(hint);
				showingHint = true;
				super.setForeground(Color.gray);
			}
		}

		@Override
		public void focusGained(FocusEvent e)
		{
			if (this.getText().isEmpty())
			{
				super.setText("");
				showingHint = false;
				super.setForeground(orig);
			}
		}

		@Override
		public void focusLost(FocusEvent e)
		{
			if (this.getText().isEmpty())
			{
				super.setText(hint);
				showingHint = true;
				super.setForeground(Color.gray);
			}
		}

		@Override
		public void setText(String s)
		{
			if (s.length() == 0)
			{
				super.setText(hint);
				showingHint = true;
				super.setForeground(Color.gray);
			}
			else
			{
				super.setText(s);
				showingHint = false;
				super.setForeground(orig);
			}
		}

		@Override
		public String getText()
		{
			return showingHint ? "" : super.getText();
		}
	}

	private String[] makeConversionArgs()
	{
		ArrayList<String> rv = new ArrayList<String>();

		// printer flag
		ToolPrinter printer = getSelectedToolPrinter();

		rv.add(Hyst.FLAG_TOOL);
		rv.add(printer.getCommandLineFlag());
		rv.add(printerParamTf.getText().trim());

		// passes flags
		boolean printedPassesFlag = false;

		for (int i = 0; i < passBoxes.size(); ++i)
		{
			int selectedIndex = passBoxes.get(i).getSelectedIndex();

			if (selectedIndex != 0)
			{
				if (!printedPassesFlag)
				{
					printedPassesFlag = true;
					rv.add(Hyst.FLAG_PASSES);
				}

				TransformationPass tp = availablePasses.get(selectedIndex - 1);
				rv.add(tp.getCommandLineFlag());

				rv.add(passParams.get(i).getText());
			}
		}

		// params

		// verbose/debug print mode flag
		if (printDebug.isSelected())
			rv.add(Hyst.FLAG_DEBUG);
		else if (printVerbose.isSelected())
			rv.add(Hyst.FLAG_VERBOSE);

		// output file
		String out = outTf.getText();

		if (out.length() > 0)
		{
			rv.add(Hyst.FLAG_OUTPUT);
			rv.add(out);
		}

		// input files
		rv.add(Hyst.FLAG_INPUT);

		// input model xml
		rv.add(xmlTf.getText());

		// input model cfg
		String cfg = cfgTf.getText();

		if (cfg.length() > 0)
			rv.add(cfg);

		return rv.toArray(new String[rv.size()]);
	}

	private ToolPrinter getSelectedToolPrinter()
	{
		ToolPrinter rv = null;
		int selectedIndex = printerBox.getSelectedIndex();
		int count = 0;

		for (ToolPrinter tp : printers)
		{
			if (!tp.isInRelease())
				continue;

			if (selectedIndex == count++)
			{
				rv = tp;
				break;
			}
		}

		if (rv == null)
		{
			JOptionPane.showMessageDialog(null, "Error: Could not find selected printer.");
			rv = printers[0];
		}

		return rv;
	}

	private void saveGuiState()
	{
		GuiState gs = new GuiState();

		gs.setXml(xmlTf.getText());
		gs.setCfg(cfgTf.getText());
		gs.setOutput(outTf.getText());

		int pi = printerBox.getSelectedIndex();

		gs.setPrinterParams(printerParams);

		int mode = GuiState.NORMAL;

		if (printVerbose.isSelected())
			mode = GuiState.VERBOSE;
		else if (printDebug.isSelected())
			mode = GuiState.DEBUG;

		gs.setDebugMode(mode);

		gs.setPrinterIndex(pi);

		for (int i = 0; i < passBoxes.size(); ++i)
		{
			int index = passBoxes.get(i).getSelectedIndex();

			if (index != 0)
			{
				String param = passParams.get(i).getText();

				gs.getPassIndex().add(index);
				gs.getPassParam().add(param);
			}
		}

		GuiState.save(gs, GUISTATE_FILENAME);
	}

	/**
	 * Update the extension of the output file to match the tool (if there is an output file)
	 * 
	 * @param printer
	 *            the tool printer that was selected
	 */
	private void updateOutputExtension(ToolPrinter printer)
	{
		String ext = printer.getExtension();

		if (ext == null)
			ext = ".model";

		String cur = outTf.getText();

		if (cur.length() > 0)
		{
			// if there was an output
			int last = cur.lastIndexOf('.');

			if (last != -1)
				cur = cur.substring(0, last);

			outTf.setText(cur + ext);
		}
	}

	@Override
	public void windowActivated(WindowEvent e)
	{
	}

	@Override
	public void windowClosed(WindowEvent e)
	{
		saveGuiState();
	}

	@Override
	public void windowDeactivated(WindowEvent arg0)
	{
	}

	@Override
	public void windowDeiconified(WindowEvent arg0)
	{
	}

	@Override
	public void windowIconified(WindowEvent arg0)
	{
	}

	@Override
	public void windowOpened(WindowEvent arg0)
	{
	}

	@Override
	public void windowClosing(WindowEvent arg0)
	{
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == runButton)
		{
			String model = xmlTf.getText();

			if (model.length() == 0)
				JOptionPane.showMessageDialog(null, "You must select a model file.", "Error",
						JOptionPane.ERROR_MESSAGE);
			else
			{
				// clear the output
				outputBuffer.setLength(0);
				outputArea.setText(outputBuffer.toString());

				tabs.setSelectedIndex(1);

				final String[] args = makeConversionArgs();
				addOutput("Converting using command line: " + Hyst.makeSingleArgument(args) + "\n");

				new Thread()
				{
					public void run()
					{
						int code = Hyst.runWithArguments(args);
						String s = "\nConversion completed with exit code " + code + ": "
								+ Hyst.ExitCode.values()[code];

						addOutput(s);
					}
				}.start();

				// finally, save the gui file
				saveGuiState();
			}
		}
		else if (e.getSource() == xmlButton)
		{
			String s = getPath(true, xmlTf.getText());

			if (s != null)
			{
				setInputPath(s);
			}
		}
		else if (e.getSource() == cfgButton)
		{
			String s = getPath(true, cfgTf.getText());

			if (s != null)
				cfgTf.setText(s);
		}
		else if (e.getSource() == outButton)
		{
			String s = getPath(false, outTf.getText());

			if (s != null)
				outTf.setText(s);
			else
			{
				outTf.setText("");
				outTf.updateHint();
			}
		}
		else if (e.getSource() == printerButton)
		{
			ToolPrinter tp = getSelectedToolPrinter();
			String text = tp.getParamHelp().trim();

			if (text.length() == 0)
				text = "No parameters for this tool.";

			JOptionPane.showMessageDialog(this, text, "Tool Parameter Help",
					JOptionPane.INFORMATION_MESSAGE);
		}
		else if (e.getSource() == addPassButton)
			addPass();
		else if (e.getSource() == printerBox)
			printerBoxSelected();
		else
		{
			for (int i = 0; i < passButtons.size(); ++i)
			{
				if (e.getSource() == passButtons.get(i))
				{
					int index = passBoxes.get(i).getSelectedIndex();
					String msg = "Select a transformation pass from the drop-down list.";
					String title = "Transformation Pass Param Help";

					if (index > 0)
					{
						--index;
						TransformationPass pass = availablePasses.get(index);

						title = pass.getName() + " Help";

						msg = pass.getName() + " " + pass.getCommandLineFlag() + "\n\n";
						msg += "Param: " + pass.getParamHelp();

						String helpText = pass.getLongHelp();

						if (helpText != null)
							msg += "\n\n" + helpText;
					}

					JOptionPane.showMessageDialog(this, msg, title,
							JOptionPane.INFORMATION_MESSAGE);
					break;
				}
			}
		}
	}

	/**
	 * The printer box selection changed. Update the GUI.
	 */
	private void printerBoxSelected()
	{
		int pi = printerBox.getSelectedIndex();

		if (printerParams.size() > pi)
		{
			skipDocumentEvents = true;
			printerParamTf.setText(printerParams.get(pi));
			skipDocumentEvents = false;

			ToolPrinter tp = getReleasePrinter(pi);
			updateOutputExtension(tp);
		}
	}

	/**
	 * The gui was started with a given path, populate the text fields appropriately
	 * 
	 * @param filename
	 *            the filename that was provided
	 */
	public void guiLoad(String filename)
	{
		setInputPath(filename);

		String modelName = filename;
		int last = filename.lastIndexOf('.');

		if (last != -1)
			modelName = filename.substring(0, last);

		String output = modelName + "_converted.xml";
		outTf.setText(output);

		// update extension based on the selected printer
		printerBoxSelected();
	}

	private void setInputPath(String s)
	{
		xmlTf.setText(s);

		if (s.contains(".xml"))
		{
			String cfgPath = s.replace(".xml", ".cfg");

			if (new File(cfgPath).exists())
				cfgTf.setText(cfgPath);
		}
	}

	public void printerParamUpdated()
	{
		if (skipDocumentEvents == false)
		{
			int pi = printerBox.getSelectedIndex();
			String text = printerParamTf.getText();

			if (!text.equals(TOOL_PARAM_HELP))
				printerParams.set(pi, text);
		}
	}

	@Override
	public void changedUpdate(DocumentEvent arg0)
	{
		printerParamUpdated();
	}

	@Override
	public void insertUpdate(DocumentEvent arg0)
	{
		printerParamUpdated();
	}

	@Override
	public void removeUpdate(DocumentEvent arg0)
	{
		printerParamUpdated();
	}
}
