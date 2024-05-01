import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SecureIt extends JFrame {
    private JProgressBar progressBar;
    private Timer timer;
    private JTextArea infoArea;

    public SecureIt() {
        setTitle("SecureIt");
        setSize(700, 150); // Increased height to accommodate the error message
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JLabel titleLabel = new JLabel("Secure Your Files");
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setFont(new Font("Helvetica", Font.BOLD, 24));

        JLabel keyLabel = new JLabel("Enter Key:");
        keyLabel.setHorizontalAlignment(SwingConstants.CENTER);
        keyLabel.setFont(new Font("Helvetica", Font.PLAIN, 12));

        JTextField keyField = new JTextField(10);
        JButton selectFileButton = new JButton("Select File");
        JButton clearButton = new JButton("Clear");

        JPanel inputPanel = new JPanel(new FlowLayout());
        inputPanel.add(keyLabel);
        inputPanel.add(keyField);
        inputPanel.add(selectFileButton);
        inputPanel.add(clearButton);

        infoArea = new JTextArea();
        infoArea.setEditable(false);
        infoArea.setLineWrap(true);

        JLabel constraintsLabel = new JLabel("Constraints: Key length between 8 and 32 characters, use a mix of uppercase, lowercase, digits, and special characters.");
        constraintsLabel.setFont(new Font("Helvetica", Font.PLAIN, 12));

        JPanel constraintsPanel = new JPanel();
        constraintsPanel.setLayout(new FlowLayout());
        constraintsPanel.add(constraintsLabel);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(inputPanel, BorderLayout.CENTER);
        mainPanel.add(constraintsPanel, BorderLayout.SOUTH);

        add(titleLabel, BorderLayout.NORTH);
        add(mainPanel, BorderLayout.CENTER);
        add(infoArea, BorderLayout.SOUTH);

        selectFileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String key = keyField.getText();
                if (!isValidKey(key)) {
                    infoArea.setText("Please enter the key following all the constraints.");
                    return;
                }

                JFileChooser fileChooser = new JFileChooser();
                int returnValue = fileChooser.showOpenDialog(null);
                if (returnValue == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    progressBar = new JProgressBar(0, 100);
                    progressBar.setStringPainted(true);

                    JDialog progressDialog = new JDialog();
                    progressDialog.setLayout(new BorderLayout());
                    progressDialog.add(progressBar, BorderLayout.CENTER);
                    progressDialog.setSize(300, 100);
                    progressDialog.setLocationRelativeTo(null);
                    progressDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
                    progressDialog.setModal(true);

                    int delay = 20; // 50 milliseconds
                    timer = new Timer(delay, new ActionListener() {
                        int progress = 0;

                        @Override
                        public void actionPerformed(ActionEvent e) {
                            progress++;
                            progressBar.setValue(progress);
                            if (progress >= 100) {
                                timer.stop();
                                progressDialog.dispose();
                            }
                        }
                    });
                    timer.start();

                    Thread operationThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                if (selectedFile.getName().startsWith("encrypted_")) {
                                    decrypt(selectedFile, key, infoArea); // Pass infoArea as a parameter
                                } else {
                                    encrypt(selectedFile, key);
                                    infoArea.setText("File encrypted successfully."); // Display encryption success message
                                }
                            } catch (Exception ex) {
                                infoArea.setText("Error: " + ex.getMessage());
                            }
                        }
                    });
                    operationThread.start();
                    progressDialog.setVisible(true);
                }
            }
        });

        clearButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                keyField.setText("");
                infoArea.setText("");
            }
        });
    }

    public static boolean isValidKey(String key) {
        // Check if key length is between 8 and 32 characters
        if (key.length() < 8 || key.length() > 32)
            return false;

        // Check if key contains a mix of uppercase, lowercase, digits, and special characters
        Pattern upperCase = Pattern.compile("[A-Z]");
        Pattern lowerCase = Pattern.compile("[a-z]");
        Pattern digit = Pattern.compile("[0-9]");
        Pattern special = Pattern.compile("[^A-Za-z0-9]");

        Matcher upperMatcher = upperCase.matcher(key);
        Matcher lowerMatcher = lowerCase.matcher(key);
        Matcher digitMatcher = digit.matcher(key);
        Matcher specialMatcher = special.matcher(key);

        return upperMatcher.find() && lowerMatcher.find() && digitMatcher.find() && specialMatcher.find();
    }

    public static void encrypt(File inputFile, String key) {
        try {
            FileInputStream fis = new FileInputStream(inputFile);
            byte[] data = new byte[fis.available()];
            fis.read(data);
            byte[] keyBytes = key.getBytes("UTF-8");
            int keyLength = keyBytes.length;
            for (int i = 0; i < data.length; i++) {
                data[i] = (byte) (data[i] ^ keyBytes[i % keyLength]);
            }
            FileOutputStream fos = new FileOutputStream(new File(inputFile.getParent(), "encrypted_" + inputFile.getName()));
            fos.write(data);
            fos.close();
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void decrypt(File inputFile, String key, JTextArea infoArea) {
        try {
            FileInputStream fis = new FileInputStream(inputFile);
            byte[] data = new byte[fis.available()];
            fis.read(data);
            byte[] keyBytes = key.getBytes("UTF-8");
            int keyLength = keyBytes.length;
            for (int i = 0; i < data.length; i++) {
                data[i] = (byte) (data[i] ^ keyBytes[i % keyLength]);
            }
            File outputFile = new File(inputFile.getParent(), inputFile.getName().replaceFirst("encrypted_", "decrypted_"));
            FileOutputStream fos = new FileOutputStream(outputFile);
            fos.write(data);
            fos.close();
            fis.close();

            // Delete the original encrypted file
            inputFile.delete();

            // Display decryption success message
            infoArea.setText("File decrypted successfully. Decrypted file: " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new SecureIt().setVisible(true);
            }
        });
    }
}

