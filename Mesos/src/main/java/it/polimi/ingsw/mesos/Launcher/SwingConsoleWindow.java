package it.polimi.ingsw.mesos.Launcher;

import java.awt.BorderLayout;
import java.awt.Font;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Simple Swing-backed console for running console programs without a terminal.
 */
public final class SwingConsoleWindow {
    private final JTextArea outputArea;
    private final JTextField inputField;
    private final PipedOutputStream inputWriter;

    /**
     * Creates the window and redirects standard streams to it.
     */
    private SwingConsoleWindow(String title, PipedInputStream inputReader, PipedOutputStream inputWriter) {
        this.outputArea = new JTextArea();
        this.inputField = new JTextField();
        this.inputWriter = inputWriter;

        outputArea.setEditable(false);
        outputArea.setLineWrap(true);
        outputArea.setWrapStyleWord(true);
        outputArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        inputField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));

        inputField.addActionListener(event -> submitInput());

        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        content.add(new JScrollPane(outputArea), BorderLayout.CENTER);
        inputField.setBorder(BorderFactory.createTitledBorder("Type here, then press Enter"));
        content.add(inputField, BorderLayout.SOUTH);

        JFrame frame = new JFrame(title);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setContentPane(content);
        frame.setSize(900, 620);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        SwingUtilities.invokeLater(() -> {
            frame.toFront();
            inputField.requestFocusInWindow();
        });

        System.setIn(inputReader);
        PrintStream consoleStream = new PrintStream(new TextAreaOutputStream(outputArea), true, StandardCharsets.UTF_8);
        System.setOut(consoleStream);
        System.setErr(consoleStream);
    }

    /**
     * Opens a Swing console and runs the program on a background thread.
     *
     * @param title window title
     * @param program program to run
     * @throws Exception if Swing initialization fails
     */
    public static void open(String title, ConsoleProgram program) throws Exception {
        PipedInputStream inputReader = new PipedInputStream();
        PipedOutputStream inputWriter = new PipedOutputStream(inputReader);

        SwingUtilities.invokeAndWait(() -> new SwingConsoleWindow(title, inputReader, inputWriter));

        Thread programThread = new Thread(() -> {
            try {
                program.run();
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }, title.toLowerCase().replace(' ', '-') + "-main");
        programThread.start();
    }

    /**
     * Sends the current input field contents to the redirected standard input.
     */
    private void submitInput() {
        String input = inputField.getText();
        inputField.setText("");
        append("> " + input + System.lineSeparator());
        try {
            inputWriter.write((input + System.lineSeparator()).getBytes(StandardCharsets.UTF_8));
            inputWriter.flush();
        } catch (IOException exception) {
            append("Unable to send input: " + exception.getMessage() + System.lineSeparator());
        }
    }

    /**
     * Appends text and keeps the caret at the end.
     */
    private void append(String text) {
        outputArea.append(text);
        outputArea.setCaretPosition(outputArea.getDocument().getLength());
    }

    @FunctionalInterface
    public interface ConsoleProgram {
        void run() throws Exception;
    }

    /**
     * Output stream that forwards text to a Swing text area.
     */
    private static final class TextAreaOutputStream extends OutputStream {
        private final JTextArea textArea;
        private final StringBuilder buffer;

        /**
         * Creates an output stream bound to a text area.
         */
        private TextAreaOutputStream(JTextArea textArea) {
            this.textArea = textArea;
            this.buffer = new StringBuilder();
        }

        /**
         * Writes one byte as a character and flushes it to Swing.
         */
        @Override
        public synchronized void write(int value) {
            buffer.append((char) value);
            flush();
        }

        /**
         * Writes UTF-8 bytes and flushes them to Swing.
         */
        @Override
        public synchronized void write(byte[] bytes, int offset, int length) {
            buffer.append(new String(bytes, offset, length, StandardCharsets.UTF_8));
            flush();
        }

        /**
         * Moves buffered output to the text area on the Swing event thread.
         */
        @Override
        public synchronized void flush() {
            if (buffer.isEmpty()) {
                return;
            }
            String text = buffer.toString();
            buffer.setLength(0);
            SwingUtilities.invokeLater(() -> {
                textArea.append(text);
                textArea.setCaretPosition(textArea.getDocument().getLength());
            });
        }
    }
}
