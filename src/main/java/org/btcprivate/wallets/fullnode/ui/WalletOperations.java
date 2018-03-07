package org.btcprivate.wallets.fullnode.ui;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;


import org.btcprivate.wallets.fullnode.daemon.BTCPClientCaller;
import org.btcprivate.wallets.fullnode.daemon.BTCPClientCaller.*;
import org.btcprivate.wallets.fullnode.daemon.BTCPInstallationObserver;
import org.btcprivate.wallets.fullnode.util.BackupTracker;
import org.btcprivate.wallets.fullnode.util.Log;
import org.btcprivate.wallets.fullnode.util.StatusUpdateErrorReporter;
import org.btcprivate.wallets.fullnode.util.Util;


/**
 * Provides miscellaneous operations for the wallet file.
 *
 * @author Ivan Vaklinov <ivan@vaklinov.com>
 */
public class WalletOperations {
    private BTCPWalletUI parent;
    private JTabbedPane tabs;
    private DashboardPanel dashboard;
    private SendCashPanel sendCash;
    private AddressesPanel addresses;

    private BTCPInstallationObserver installationObserver;
    private BTCPClientCaller clientCaller;
    private StatusUpdateErrorReporter errorReporter;
    private BackupTracker backupTracker;


    public WalletOperations(BTCPWalletUI parent,
                            JTabbedPane tabs,
                            DashboardPanel dashboard,
                            AddressesPanel addresses,
                            SendCashPanel sendCash,

                            BTCPInstallationObserver installationObserver,
                            BTCPClientCaller clientCaller,
                            StatusUpdateErrorReporter errorReporter,
                            BackupTracker backupTracker)
            throws IOException, InterruptedException, WalletCallException {
        this.parent = parent;
        this.tabs = tabs;
        this.dashboard = dashboard;
        this.addresses = addresses;
        this.sendCash = sendCash;

        this.installationObserver = installationObserver;
        this.clientCaller = clientCaller;
        this.errorReporter = errorReporter;

        this.backupTracker = backupTracker;
    }

    public void showPrivateKey() {
        if (this.tabs.getSelectedIndex() != 1) {
            JOptionPane.showMessageDialog(
                    this.parent,
                    "Please select an address in the \"My Addresses\" tab " +
                            "to view its private key.",
                    "Select an Address", JOptionPane.INFORMATION_MESSAGE);
            this.tabs.setSelectedIndex(1);
            return;
        }

        String address = this.addresses.getSelectedAddress();

        if (address == null) {
            JOptionPane.showMessageDialog(
                    this.parent,
                    "Please select an address from the table " +
                            "to view its private key.",
                    "Select an Address", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        try {
            // Check for encrypted wallet
            final boolean bEncryptedWallet = this.clientCaller.isWalletEncrypted();
            if (bEncryptedWallet) {
                PasswordDialog pd = new PasswordDialog((JFrame) (this.parent));
                pd.setVisible(true);

                if (!pd.isOKPressed()) {
                    return;
                }

                this.clientCaller.unlockWallet(pd.getPassword());
            }

            boolean isZAddress = Util.isZAddress(address);

            String privateKey = isZAddress ?
                    this.clientCaller.getZPrivateKey(address) : this.clientCaller.getTPrivateKey(address);

            // Lock the wallet again
            if (bEncryptedWallet) {
                this.clientCaller.lockWallet();
            }

            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(new StringSelection(privateKey), null);

            JOptionPane.showMessageDialog(
                    this.parent,
                    (isZAddress ? "Z (Private)" : "T (Transparent)") + " address:\n" +
                            address + "\n" +
                            "has private key:\n" +
                            privateKey + "\n\n" +
                            "The private key has also been copied to the clipboard.",
                    "Private Key Info", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception ex) {
            this.errorReporter.reportError(ex, false);
        }
    }


    public void importSinglePrivateKey() {
        try {
            SingleKeyImportDialog kd = new SingleKeyImportDialog(this.parent, this.clientCaller, this.sendCash, this.tabs);
            kd.setVisible(true);

        } catch (Exception ex) {
            this.errorReporter.reportError(ex, false);
        }
    }
}