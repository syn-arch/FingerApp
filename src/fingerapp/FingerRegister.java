package fingerapp;

import com.digitalpersona.uareu.Fid;
import com.digitalpersona.uareu.Fid.Fiv;
import com.digitalpersona.uareu.Fmd;
import com.digitalpersona.uareu.Reader;
import com.digitalpersona.uareu.ReaderCollection;
import com.digitalpersona.uareu.UareUException;
import com.digitalpersona.uareu.UareUGlobal;
import java.awt.image.BufferedImage;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class FingerRegister extends javax.swing.JFrame 
{
    private Reader reader;
    private boolean scanning = true;
    
    private int getIdSiswa()
    {
        int id = 0;

        try {
            Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/absensi_kece", "root", "");
            Statement statement = connection.createStatement();
            String sql = "SELECT id_siswa FROM siswa WHERE scan = 1 LIMIT 1";
            ResultSet resultSet = statement.executeQuery(sql);
            if (resultSet.next()) {
                id = resultSet.getInt("id_siswa");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return id;
    }
    
    private void saveFingerprintToDatabase(byte[] fingerprintData, int length) throws SQLException { 
        String insertQuery = "INSERT INTO fid (id_siswa, fid, length) VALUES (?,?,?)";
        
        try (
            Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/absensi_kece", "root", "");
            PreparedStatement pstmt = connection.prepareStatement(insertQuery)) {
            pstmt.setInt(1, getIdSiswa());
            pstmt.setBytes(2, fingerprintData);
            pstmt.setInt(3, length);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to save fingerprint to database.");
        }
    }
    
    private void initializeReader() {
        try {
            ReaderCollection readers = UareUGlobal.GetReaderCollection();
            readers.GetReaders();
            if (readers.size() == 0) {
                JOptionPane.showMessageDialog(this, "No fingerprint reader connected.");
                return;
            }
            reader = readers.get(0);
            reader.Open(Reader.Priority.EXCLUSIVE);
        } catch (UareUException e) {
            e.printStackTrace();
        }
    }

    private void startAutoCapture() {
        // Membuat thread baru untuk menjalankan pemindaian secara otomatis
        new Thread(() -> {
            while (scanning) {
                try {
                    captureFingerprint();
                } catch (SQLException ex) {
                    Logger.getLogger(FingerRegister.class.getName()).log(Level.SEVERE, null, ex);
                }
                try {
                    // Jeda sebentar sebelum pemindaian berikutnya
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void captureFingerprint() throws SQLException {
        try {
            // Mulai capture sidik jari
            Reader.CaptureResult captureResult = reader.Capture(
                    Fid.Format.ANSI_381_2004, 
                    Reader.ImageProcessing.IMG_PROC_DEFAULT, 
                    500, 
                    -1
            );

            if (captureResult.quality == Reader.CaptureQuality.GOOD) {
                Fid fid = captureResult.image;
                Fiv view = fid.getViews()[0];
                BufferedImage img = new BufferedImage(view.getWidth(), view.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
                img.getRaster().setDataElements(0, 0, view.getWidth(), view.getHeight(), view.getData());
                
                // Tampilkan gambar sidik jari di JFrame
                ImageIcon icon = new ImageIcon(img.getScaledInstance(imageLabel.getWidth(), imageLabel.getHeight(), BufferedImage.SCALE_SMOOTH));
                SwingUtilities.invokeLater(() -> {
                    imageLabel.setIcon(icon);
                    imageLabel.setText("");
                });
                
                  // Konversi gambar ke template FMD
                Fmd fmd = UareUGlobal.GetEngine().CreateFmd(
                    captureResult.image,                 // Gambar hasil pemindaian
                    Fmd.Format.ANSI_378_2004             // Format FMD yang diinginkan
                );
                
                // Dapatkan byte array dari FMD
                byte[] fingerprintTemplate = fmd.getData();
                int length = fmd.getData().length;

                // Simpan data sidik jari ke database
                saveFingerprintToDatabase(fingerprintTemplate, length);
                status.setText("Status : Berhasil");
            } else {
                // Jika kualitas sidik jari tidak bagus, tampilkan pesan
                SwingUtilities.invokeLater(() -> {
                    imageLabel.setText("Poor fingerprint quality. Try again.");
                    imageLabel.setIcon(null);
                });
            }
        } catch (UareUException e) {
            e.printStackTrace();
        }
    }


    public void closeReader() {
        try {
            scanning = false; // Menghentikan proses scanning
            if (reader != null) {
                reader.Close();
            }
        } catch (UareUException e) {
            e.printStackTrace();
        }
    }

    
    public FingerRegister() {  
        initializeReader();
        startAutoCapture();
        initComponents();
    }


    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        imageLabel = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();
        status = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("FingerPrint App");
        setResizable(false);

        imageLabel.setBackground(new java.awt.Color(255, 51, 51));

        jButton1.setText("Scan");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        status.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        status.setText("Status : ");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(22, 22, 22)
                .addComponent(imageLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 238, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(26, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(status, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jButton1, javax.swing.GroupLayout.DEFAULT_SIZE, 196, Short.MAX_VALUE))
                .addGap(39, 39, 39))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(26, 26, 26)
                .addComponent(imageLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 313, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(13, 13, 13)
                .addComponent(status, javax.swing.GroupLayout.PREFERRED_SIZE, 14, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(22, Short.MAX_VALUE))
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
    closeReader();
    FingerScanner sc = new FingerScanner();
    this.setVisible(false);
    sc.setVisible(true);
    }//GEN-LAST:event_jButton1ActionPerformed

    public static void main(String args[]) {
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(FingerRegister.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(FingerRegister.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(FingerRegister.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(FingerRegister.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new FingerRegister().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel imageLabel;
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel status;
    // End of variables declaration//GEN-END:variables
}
