import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.*;
import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.util.Comparator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

/**
 * ARemi Pro Application
 *
 * This application monitors Wii homebrew devices and allows you to create and edit them.
 * Devices display only their public fields (ID, Name, AppID, and Status)
 * while sensitive fields (Saved CPN and Security Key) remain hidden from the UI.
 *
 * The login screen provides options to log in or create a new account.
 * Accounts are stored in "accounts.json".
 *
 * The main window now contains a menu bar at the top with the left-side menus:
 *   File, View, Tools, and Settings.
 * The File menu includes new functions: New Device, Open Devices File, and Save Devices File.
 * The View menu includes functions such as Refresh, Sort Devices by Name, and Sort Devices by AppID.
 * The Tools menu also contains:
 *   - Import FIFO Player File (JavaScript SRC file)
 * Settings still contains the previously added items, and an Exit item is placed on the far right.
 */
public class ARemiPro {

    // File path for the accounts JSON file.
    private static final String ACCOUNTS_FILE = "accounts.json";

    // Map of accounts: username -> password.
    public static Map<String, String> accounts = new HashMap<>();

    // Load accounts when the class loads.
    static {
        loadAccounts();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable(){
            public void run() {
                new LoginFrame().setVisible(true);
            }
        });
    }

    // Loads accounts from ACCOUNTS_FILE if it exists; otherwise, creates default accounts.
    private static void loadAccounts() {
        File file = new File(ACCOUNTS_FILE);
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                Gson gson = new Gson();
                Type type = new TypeToken<HashMap<String, String>>() {}.getType();
                accounts = gson.fromJson(reader, type);
                if (accounts == null) {
                    accounts = new HashMap<>();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        } else {
            // Default accounts if file not found.
            accounts.put("admin", "admin123");
            accounts.put("user", "password");
            accounts.put("test", "test123");
            saveAccounts();
        }
    }

    // Saves the accounts map to ACCOUNTS_FILE.
    private static void saveAccounts() {
        try (FileWriter writer = new FileWriter(ACCOUNTS_FILE)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(accounts, writer);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * The LoginFrame presents a login interface with username and password fields
     * along with buttons for Login and Create Account.
     */
    static class LoginFrame extends JFrame {
        public LoginFrame() {
            super("ARemi Pro - Login");
            setSize(350, 250);
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setLocationRelativeTo(null);
            initializeUI();
        }

        private void initializeUI() {
            // Panel for credentials.
            JPanel fieldsPanel = new JPanel(new GridLayout(2, 2, 10, 10));
            JLabel userLabel = new JLabel("Username:");
            JTextField userField = new JTextField();
            JLabel passLabel = new JLabel("Password:");
            JPasswordField passField = new JPasswordField();
            fieldsPanel.add(userLabel);
            fieldsPanel.add(userField);
            fieldsPanel.add(passLabel);
            fieldsPanel.add(passField);

            // Panel for buttons.
            JPanel buttonsPanel = new JPanel();
            JButton loginButton = new JButton("Login");
            JButton createAccountButton = new JButton("Create Account");
            buttonsPanel.add(loginButton);
            buttonsPanel.add(createAccountButton);

            setLayout(new BorderLayout());
            add(fieldsPanel, BorderLayout.CENTER);
            add(buttonsPanel, BorderLayout.SOUTH);

            // Login action.
            loginButton.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent e) {
                    String username = userField.getText().trim();
                    String password = new String(passField.getPassword());
                    if (accounts.containsKey(username) && accounts.get(username).equals(password)) {
                        SwingUtilities.invokeLater(new Runnable(){
                            public void run() {
                                new ARemiProFrame(username).setVisible(true);
                            }
                        });
                        dispose();
                    } else {
                        JOptionPane.showMessageDialog(LoginFrame.this,
                          "Invalid login credentials", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });

            // Create Account action.
            createAccountButton.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent e) {
                    JPanel createPanel = new JPanel(new GridLayout(3, 2, 10, 10));
                    JLabel newUserLabel = new JLabel("Username:");
                    JTextField newUserField = new JTextField();
                    JLabel newPassLabel = new JLabel("Password:");
                    JPasswordField newPassField = new JPasswordField();
                    JLabel confirmPassLabel = new JLabel("Confirm Password:");
                    JPasswordField confirmPassField = new JPasswordField();

                    createPanel.add(newUserLabel);
                    createPanel.add(newUserField);
                    createPanel.add(newPassLabel);
                    createPanel.add(newPassField);
                    createPanel.add(confirmPassLabel);
                    createPanel.add(confirmPassField);

                    int result = JOptionPane.showConfirmDialog(LoginFrame.this, createPanel,
                        "Create New Account", JOptionPane.OK_CANCEL_OPTION);
                    if (result == JOptionPane.OK_OPTION) {
                        String newUsername = newUserField.getText().trim();
                        String newPassword = new String(newPassField.getPassword());
                        String confirmPassword = new String(confirmPassField.getPassword());
                        if (newUsername.isEmpty() || newPassword.isEmpty()) {
                            JOptionPane.showMessageDialog(LoginFrame.this,
                              "Username and password cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                        if (!newPassword.equals(confirmPassword)) {
                            JOptionPane.showMessageDialog(LoginFrame.this,
                              "Passwords do not match.", "Error", JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                        if (accounts.containsKey(newUsername)) {
                            JOptionPane.showMessageDialog(LoginFrame.this,
                              "Username already exists.", "Error", JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                        accounts.put(newUsername, newPassword);
                        saveAccounts();
                        JOptionPane.showMessageDialog(LoginFrame.this,
                          "Account created successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    }
                }
            });
        }
    }

    /**
     * The ARemiProFrame is the main application window.
     * It displays a table of devices (with non‑sensitive data) and allows creation/editing.
     * The window contains a menu bar at the top with the left‑side menus:
     *   File, View, Tools, and Settings.
     *
     * The File menu now has:
     *    - New Device
     *    - Open Devices File
     *    - Save Devices File
     *
     * The View menu now has:
     *    - Refresh
     *    - Sort Devices by Name
     *    - Sort Devices by AppID
     *
     * The Tools menu contains the new item:
     *    - Import FIFO Player File (JavaScript SRC file)
     *
     * The Settings menu contains previously added items.
     * After the left‑side menus, horizontal glue pushes an Exit item to the far right.
     */
    static class ARemiProFrame extends JFrame {
        private DefaultTableModel tableModel;
        private JTable deviceTable;
        // List holding full device information.
        private List<Device> devices = new ArrayList<>();
        private int deviceCounter = 0;
        private String username;
        private Gson gson;

        public ARemiProFrame(String username) {
            super("ARemi Pro");
            this.username = username;
            gson = new GsonBuilder().setPrettyPrinting().create();
            setSize(800, 500);
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setLocationRelativeTo(null);
            initializeUI();
        }

        private void initializeUI() {
            // Create the menu bar with left‑side menus.
            JMenuBar menuBar = new JMenuBar();

            // File menu.
            JMenu fileMenu = new JMenu("File");
            JMenuItem newDeviceFileItem = new JMenuItem("New Device");
            newDeviceFileItem.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent e) {
                    createNewDevice();
                }
            });
            JMenuItem openDevicesFileItem = new JMenuItem("Open Devices File");
            openDevicesFileItem.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent e) {
                    openDevicesFile();
                }
            });
            JMenuItem saveDevicesFileItem = new JMenuItem("Save Devices File");
            saveDevicesFileItem.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent e) {
                    saveDevicesFile();
                }
            });
            fileMenu.add(newDeviceFileItem);
            fileMenu.add(openDevicesFileItem);
            fileMenu.add(saveDevicesFileItem);

            // View menu.
            JMenu viewMenu = new JMenu("View");
            JMenuItem refreshViewItem = new JMenuItem("Refresh");
            refreshViewItem.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent e) {
                    refreshTable();
                }
            });
            JMenuItem sortByNameItem = new JMenuItem("Sort Devices by Name");
            sortByNameItem.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent e) {
                    Collections.sort(devices, Comparator.comparing(Device::getName));
                    refreshTable();
                }
            });
            JMenuItem sortByAppIDItem = new JMenuItem("Sort Devices by AppID");
            sortByAppIDItem.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent e) {
                    Collections.sort(devices, Comparator.comparing(Device::getAppId));
                    refreshTable();
                }
            });
            viewMenu.add(refreshViewItem);
            viewMenu.add(sortByNameItem);
            viewMenu.add(sortByAppIDItem);

            // Tools menu.
            JMenu toolsMenu = new JMenu("Tools");
            JMenuItem importFifoPlayerItem = new JMenuItem("Import FIFO Player File (JavaScript SRC file)");
            importFifoPlayerItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    JFileChooser fileChooser = new JFileChooser();
                    int result = fileChooser.showOpenDialog(ARemiProFrame.this);
                    if (result == JFileChooser.APPROVE_OPTION) {
                        File file = fileChooser.getSelectedFile();
                        if (!file.getName().toLowerCase().endsWith(".js")) {
                            JOptionPane.showMessageDialog(ARemiProFrame.this,
                                    "Please select a valid JavaScript SRC file (.js).", 
                                    "Invalid File", JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                        try {
                            new String(Files.readAllBytes(file.toPath()));
                            // (You can process the file content as needed.)
                            JOptionPane.showMessageDialog(ARemiProFrame.this,
                                    "FIFO Player File imported successfully.\nFile path: " + file.getPath(),
                                    "Import FIFO Player File", JOptionPane.INFORMATION_MESSAGE);
                        } catch (IOException ex) {
                            JOptionPane.showMessageDialog(ARemiProFrame.this,
                                    "Error reading file: " + ex.getMessage(),
                                    "File Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            });
            toolsMenu.add(importFifoPlayerItem);

            // Settings menu.
            JMenu settingsMenu = new JMenu("Settings");
            JMenuItem changePasswordItem = new JMenuItem("Change Password");
            JMenuItem preferencesItem = new JMenuItem("Preferences");
            JMenuItem aboutItem = new JMenuItem("About");
            settingsMenu.add(changePasswordItem);
            settingsMenu.add(preferencesItem);
            settingsMenu.add(aboutItem);
            settingsMenu.addSeparator();
            JMenuItem logoutItem = new JMenuItem("Logout");
            JMenuItem linkWRmailItem = new JMenuItem("Link WRmail");
            JMenuItem importDevicesItem = new JMenuItem("Import Devices");
            JMenuItem exportAllDevicesItem = new JMenuItem("Export All Devices");
            JMenuItem exportSelectedDevicesItem = new JMenuItem("Export Devices (Selects devices)");
            settingsMenu.add(logoutItem);
            settingsMenu.add(linkWRmailItem);
            settingsMenu.add(importDevicesItem);
            settingsMenu.add(exportAllDevicesItem);
            settingsMenu.add(exportSelectedDevicesItem);

            // Add all left-side menus.
            menuBar.add(fileMenu);
            menuBar.add(viewMenu);
            menuBar.add(toolsMenu);
            menuBar.add(settingsMenu);

            // Add horizontal glue to push the Exit item to the far right.
            menuBar.add(Box.createHorizontalGlue());
            JMenuItem exitItem = new JMenuItem("Exit");
            exitItem.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent e) {
                    System.exit(0);
                }
            });
            menuBar.add(exitItem);
            setJMenuBar(menuBar);

            // -- Settings menu actions --
            changePasswordItem.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent e) {
                    JPanel pwdPanel = new JPanel(new GridLayout(3, 2, 10, 10));
                    pwdPanel.add(new JLabel("Old Password:"));
                    JPasswordField oldField = new JPasswordField();
                    pwdPanel.add(oldField);
                    pwdPanel.add(new JLabel("New Password:"));
                    JPasswordField newField = new JPasswordField();
                    pwdPanel.add(newField);
                    pwdPanel.add(new JLabel("Confirm New Password:"));
                    JPasswordField confirmField = new JPasswordField();
                    pwdPanel.add(confirmField);

                    int result = JOptionPane.showConfirmDialog(ARemiProFrame.this, pwdPanel,
                            "Change Password", JOptionPane.OK_CANCEL_OPTION);
                    if (result == JOptionPane.OK_OPTION) {
                        String oldPass = new String(oldField.getPassword());
                        String newPass = new String(newField.getPassword());
                        String confirmPass = new String(confirmField.getPassword());
                        if (!accounts.get(username).equals(oldPass)) {
                            JOptionPane.showMessageDialog(ARemiProFrame.this,
                                    "Old password is incorrect.", "Error", JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                        if (newPass.isEmpty() || !newPass.equals(confirmPass)) {
                            JOptionPane.showMessageDialog(ARemiProFrame.this,
                                    "New passwords do not match or are empty.", "Error", JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                        accounts.put(username, newPass);
                        saveAccounts();
                        JOptionPane.showMessageDialog(ARemiProFrame.this,
                                "Password changed successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    }
                }
            });

            preferencesItem.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent e) {
                    JOptionPane.showMessageDialog(ARemiProFrame.this,
                            "Preferences not implemented yet.", "Preferences", JOptionPane.INFORMATION_MESSAGE);
                }
            });

            aboutItem.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent e) {
                    JOptionPane.showMessageDialog(ARemiProFrame.this,
                            "ARemi Pro v1.0\nDeveloped by petrofizkulture\nFor monitoring and managing Wii homebrew devices.",
                            "About", JOptionPane.INFORMATION_MESSAGE);
                }
            });

            logoutItem.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent e) {
                    int confirm = JOptionPane.showConfirmDialog(ARemiProFrame.this,
                            "Are you sure you want to logout?", "Logout", JOptionPane.YES_NO_OPTION);
                    if (confirm == JOptionPane.YES_OPTION) {
                        dispose();
                        SwingUtilities.invokeLater(new Runnable(){
                            public void run() {
                                new LoginFrame().setVisible(true);
                            }
                        });
                    }
                }
            });

            linkWRmailItem.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent e) {
                    JOptionPane.showMessageDialog(ARemiProFrame.this,
                            "Link WRmail feature is not implemented yet.",
                            "Link WRmail", JOptionPane.INFORMATION_MESSAGE);
                }
            });

            importDevicesItem.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent e) {
                    openDevicesFile();
                }
            });

            exportAllDevicesItem.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent e) {
                    JFileChooser fileChooser = new JFileChooser();
                    int result = fileChooser.showSaveDialog(ARemiProFrame.this);
                    if (result == JFileChooser.APPROVE_OPTION) {
                        File file = fileChooser.getSelectedFile();
                        try (FileWriter writer = new FileWriter(file)) {
                            writer.write(gson.toJson(devices));
                            JOptionPane.showMessageDialog(ARemiProFrame.this,
                                    "All devices exported successfully.",
                                    "Export All Devices", JOptionPane.INFORMATION_MESSAGE);
                        } catch (IOException ex) {
                            JOptionPane.showMessageDialog(ARemiProFrame.this,
                                    "Error writing file: " + ex.getMessage(),
                                    "File Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            });

            exportSelectedDevicesItem.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent e) {
                    int[] selectedRows = deviceTable.getSelectedRows();
                    if (selectedRows.length == 0) {
                        JOptionPane.showMessageDialog(ARemiProFrame.this,
                                "No devices selected.", "Export Devices", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    List<Device> selectedDevices = new ArrayList<>();
                    for (int row : selectedRows) {
                        int deviceId = (int) tableModel.getValueAt(row, 0);
                        for (Device dev : devices) {
                            if (dev.getId() == deviceId) {
                                selectedDevices.add(dev);
                                break;
                            }
                        }
                    }
                    JFileChooser fileChooser = new JFileChooser();
                    int result = fileChooser.showSaveDialog(ARemiProFrame.this);
                    if (result == JFileChooser.APPROVE_OPTION) {
                        File file = fileChooser.getSelectedFile();
                        try (FileWriter writer = new FileWriter(file)) {
                            writer.write(gson.toJson(selectedDevices));
                            JOptionPane.showMessageDialog(ARemiProFrame.this,
                                    "Selected devices exported successfully.",
                                    "Export Devices", JOptionPane.INFORMATION_MESSAGE);
                        } catch (IOException ex) {
                            JOptionPane.showMessageDialog(ARemiProFrame.this,
                                    "Error writing file: " + ex.getMessage(),
                                    "File Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            });

            // -- Main Panel: Device Table and Operation Buttons --
            JPanel panel = new JPanel(new BorderLayout());
            String[] columnNames = {"Device ID", "Name", "AppID", "Status"};
            tableModel = new DefaultTableModel(columnNames, 0);
            deviceTable = new JTable(tableModel);
            JScrollPane scrollPane = new JScrollPane(deviceTable);
            panel.add(scrollPane, BorderLayout.CENTER);

            JPanel controlPanel = new JPanel();
            JButton createDeviceButton = new JButton("Create New Device");
            JButton editDeviceButton = new JButton("Edit Device");
            controlPanel.add(createDeviceButton);
            controlPanel.add(editDeviceButton);
            panel.add(controlPanel, BorderLayout.SOUTH);
            add(panel);

            // Action for creating a new device via the control button.
            createDeviceButton.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent e) {
                    createNewDevice();
                }
            });

            // Action for editing a device via the control button.
            editDeviceButton.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent e) {
                    int selectedRow = deviceTable.getSelectedRow();
                    if (selectedRow < 0) {
                        JOptionPane.showMessageDialog(ARemiProFrame.this,
                            "Please select a device to edit.", "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    int deviceId = (int) tableModel.getValueAt(selectedRow, 0);
                    Device selectedDevice = null;
                    for (Device dev : devices) {
                        if (dev.getId() == deviceId) {
                            selectedDevice = dev;
                            break;
                        }
                    }
                    if (selectedDevice == null) {
                        JOptionPane.showMessageDialog(ARemiProFrame.this,
                            "Selected device not found.", "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    // Request secret (CPN or Security Key) before allowing edit.
                    String secret = JOptionPane.showInputDialog(ARemiProFrame.this,
                            "Enter the CPN or Security Key to access this device:");
                    if (secret == null) return;
                    if (!(secret.equals(selectedDevice.getSavedCPN()) || secret.equals(selectedDevice.getSecurityKey()))) {
                        JOptionPane.showMessageDialog(ARemiProFrame.this,
                            "Incorrect secret. Access denied.", "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    JTextField nameField = new JTextField(selectedDevice.getName());
                    JTextField appIdField = new JTextField(selectedDevice.getAppId());
                    JPanel editPanel = new JPanel(new GridLayout(2, 2, 10, 10));
                    editPanel.add(new JLabel("Name:"));
                    editPanel.add(nameField);
                    editPanel.add(new JLabel("AppID:"));
                    editPanel.add(appIdField);

                    int editResult = JOptionPane.showConfirmDialog(ARemiProFrame.this,
                        editPanel, "Edit Device", JOptionPane.OK_CANCEL_OPTION);
                    if (editResult == JOptionPane.OK_OPTION) {
                        selectedDevice.setName(nameField.getText().trim());
                        selectedDevice.setAppId(appIdField.getText().trim());
                        tableModel.setValueAt(selectedDevice.getName(), selectedRow, 1);
                        tableModel.setValueAt(selectedDevice.getAppId(), selectedRow, 2);
                        saveDevicesToFile();
                        JOptionPane.showMessageDialog(ARemiProFrame.this,
                            "Device updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    }
                }
            });
        }

        // Helper method: Opens a dialog to create a new device.
        private void createNewDevice() {
            JTextField nameField = new JTextField();
            JTextField appIdField = new JTextField();
            JTextField savedCPNField = new JTextField();
            JTextField securityKeyField = new JTextField();

            JPanel devicePanel = new JPanel(new GridLayout(4, 2, 10, 10));
            devicePanel.add(new JLabel("Name:"));
            devicePanel.add(nameField);
            devicePanel.add(new JLabel("AppID:"));
            devicePanel.add(appIdField);
            devicePanel.add(new JLabel("Saved CPN:"));
            devicePanel.add(savedCPNField);
            devicePanel.add(new JLabel("Security Key:"));
            devicePanel.add(securityKeyField);

            int result = JOptionPane.showConfirmDialog(this, devicePanel,
                        "Enter Device Details", JOptionPane.OK_CANCEL_OPTION);
            if(result == JOptionPane.OK_OPTION) {
                String name = nameField.getText().trim();
                String appId = appIdField.getText().trim();
                String savedCPN = savedCPNField.getText().trim();
                String securityKey = securityKeyField.getText().trim();
                if(savedCPN.length() == 8) {
                    Device newDevice = new Device(++deviceCounter, name, appId, savedCPN, securityKey, "Active");
                    devices.add(newDevice);
                    tableModel.addRow(new Object[]{newDevice.getId(), newDevice.getName(), newDevice.getAppId(), newDevice.getStatus()});
                    saveDevicesToFile();
                    JOptionPane.showMessageDialog(this, "Device created successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                }
                else {
                    JOptionPane.showMessageDialog(this, "Invalid Saved CPN. It must be exactly 8 characters.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }

        // Helper method: Opens a file chooser to import (open) a devices file.
        private void openDevicesFile() {
            JFileChooser fileChooser = new JFileChooser();
            int result = fileChooser.showOpenDialog(this);
            if(result == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                try(FileReader reader = new FileReader(file)) {
                    java.lang.reflect.Type deviceListType = new TypeToken<List<Device>>() {}.getType();
                    List<Device> importedDevices = new Gson().fromJson(reader, deviceListType);
                    if(importedDevices != null) {
                        for (Device dev : importedDevices) {
                            // Assign new unique ID.
                            Device newDevice = new Device(++deviceCounter,
                                    dev.getName(), dev.getAppId(), dev.getSavedCPN(), dev.getSecurityKey(), dev.getStatus());
                            devices.add(newDevice);
                            tableModel.addRow(new Object[]{newDevice.getId(), newDevice.getName(), newDevice.getAppId(), newDevice.getStatus()});
                        }
                        saveDevicesToFile();
                        JOptionPane.showMessageDialog(this, "Devices imported successfully.", "Import Devices", JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this, "Error reading file: " + ex.getMessage(), "File Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }

        // Helper method: Opens a file chooser to save (export) all devices.
        private void saveDevicesFile() {
            JFileChooser fileChooser = new JFileChooser();
            int result = fileChooser.showSaveDialog(this);
            if(result == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                try(FileWriter writer = new FileWriter(file)) {
                    writer.write(gson.toJson(devices));
                    JOptionPane.showMessageDialog(this, "Devices saved successfully.", "Save Devices File", JOptionPane.INFORMATION_MESSAGE);
                } catch(IOException ex) {
                    JOptionPane.showMessageDialog(this, "Error writing file: " + ex.getMessage(), "File Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }

        // Helper method: Refreshes the device table to reflect the in-memory list.
        private void refreshTable() {
            tableModel.setRowCount(0);
            for (Device device : devices) {
                tableModel.addRow(new Object[]{device.getId(), device.getName(), device.getAppId(), device.getStatus()});
            }
        }

        // Saves the complete list of devices to "devices.json".
        private void saveDevicesToFile() {
            String json = gson.toJson(devices);
            try (FileWriter writer = new FileWriter("devices.json")) {
                writer.write(json);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this,
                    "Error writing to devices.json: " + ex.getMessage(),
                    "File Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }
    }

    /**
     * The Device class encapsulates the data for a Wii homebrew device.
     * Public fields (ID, Name, AppID, Status) are shown in the UI,
     * while sensitive fields (Saved CPN and Security Key) remain hidden.
     */
    static class Device {
        private int id;
        private String name;
        private String appId;
        private String savedCPN;
        private String securityKey;
        private String status;

        public Device(int id, String name, String appId, String savedCPN, String securityKey, String status) {
            this.id = id;
            this.name = name;
            this.appId = appId;
            this.savedCPN = savedCPN;
            this.securityKey = securityKey;
            this.status = status;
        }

        public int getId() {
            return id;
        }
        public String getName() {
            return name;
        }
        public String getAppId() {
            return appId;
        }
        public String getSavedCPN() {
            return savedCPN;
        }
        public String getSecurityKey() {
            return securityKey;
        }
        public String getStatus() {
            return status;
        }
        public void setName(String name) {
            this.name = name;
        }
        public void setAppId(String appId) {
            this.appId = appId;
        }
        public void setStatus(String status) {
            this.status = status;
        }
    }
}
