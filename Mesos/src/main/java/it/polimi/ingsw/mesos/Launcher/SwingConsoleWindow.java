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

public final class SwingConsoleWindow {
    private final JTextArea outputArea;
    private final JTextField inputField;
    private final PipedOutputStream inputWriter;

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

    private void append(String text) {
        outputArea.append(text);
        outputArea.setCaretPosition(outputArea.getDocument().getLength());
    }

    @FunctionalInterface
    public interface ConsoleProgram {
        void run() throws Exception;
    }

    private static final class TextAreaOutputStream extends OutputStream {
        private final JTextArea textArea;
        private final StringBuilder buffer;

        private TextAreaOutputStream(JTextArea textArea) {
            this.textArea = textArea;
            this.buffer = new StringBuilder();
        }

        @Override
        public synchronized void write(int value) {
            buffer.append((char) value);
            flush();
        }

        @Override
        public synchronized void write(byte[] bytes, int offset, int length) {
            buffer.append(new String(bytes, offset, length, StandardCharsets.UTF_8));
            flush();
        }

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
