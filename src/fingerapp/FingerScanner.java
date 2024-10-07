package fingerapp;

import com.digitalpersona.uareu.Engine;
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
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class FingerScanner extends javax.swing.JFrame 
{
    private Reader reader;
    private boolean scanning = true;
    
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
                    verifyFingerprint();
                } catch (SQLException ex) {
                    Logger.getLogger(FingerScanner.class.getName()).log(Level.SEVERE, null, ex);
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
    
    private int getAktifKategori()
    {
        int id = 0;

        try {
            Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/absensi_kece", "root", "");
            Statement statement = connection.createStatement();
            String sql = "SELECT id_kategori FROM absen_aktif";
            ResultSet resultSet = statement.executeQuery(sql);
            if (resultSet.next()) {
                id = resultSet.getInt("id_kategori");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return id;
    }
    
    private void verifyFingerprint() throws SQLException {
        try {
            // Pindai sidik jari baru
            Reader.CaptureResult captureResult = reader.Capture(Fid.Format.ANSI_381_2004, Reader.ImageProcessing.IMG_PROC_DEFAULT, 500, -1);

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
                
                // Buat template dari hasil scan baru
                Fmd fmdCaptured = UareUGlobal.GetEngine().CreateFmd(fid, Fmd.Format.ANSI_378_2004);

                // Ambil template dari database (contoh: ambil dari user tertentu)
                List<UserFingerprint> userFingerprintList = getAllFingerprintTemplatesFromDatabase();
                
                for (UserFingerprint userFingerprint : userFingerprintList) {
                    int score = UareUGlobal.GetEngine().Compare(
                        fmdCaptured,    // FMD yang baru dipindai
                        0,              // Posisi fitur pertama (biasanya 0)
                        userFingerprint.getFingerprintTemplate(),      // FMD dari database
                        0               // Posisi fitur pertama dari FMD di database
                    );
                    
                    int threshold = Engine.PROBABILITY_ONE / 100000; // Sesuaikan threshold ini

                    // Jika skor cukup rendah, itu berarti ada kecocokan
                    if (score < threshold) {
                        
                        String insertQuery = "INSERT INTO absen (id_kategori, id_siswa, status, baru) VALUES (?,?,?,?)";
                        String updateQuery = "UPDATE absen SET baru = 0";
                        String checkQuery = "SELECT id_siswa FROM absen WHERE DATE(waktu_absen) = DATE(NOW()) AND id_siswa = " + userFingerprint.getId();
                        
                        try(
                            Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/absensi_kece", "root", "");
                            PreparedStatement pstmtcek = connection.prepareStatement(checkQuery)
                        ) {
                            
                            ResultSet resultSet = pstmtcek.executeQuery();

                            if (!resultSet.next()) {
                                try (
                                PreparedStatement pstmtnew = connection.prepareStatement(updateQuery)
                                ) {
                                    pstmtnew.executeUpdate();

                                    try (
                                        PreparedStatement pstmt = connection.prepareStatement(insertQuery)
                                    ) {
                                        pstmt.setInt(1, getAktifKategori());
                                        pstmt.setInt(2, userFingerprint.getId());
                                        pstmt.setString(3, "Berhasil");
                                        pstmt.setInt(4, 1);
                                        pstmt.executeUpdate();

                                    } catch (SQLException e) {
                                        e.printStackTrace();
                                        JOptionPane.showMessageDialog(this, "Gagal.");
                                    }
                                }
                            }   
                        }
                        
                        nama.setText(userFingerprint.getNamaUser());
                        break;
                    }else{
                        nama.setText("finger print tidak ditemukan!");
                    }
                }
            } else {
            }
        } catch (UareUException e) {
            e.printStackTrace();
        }
    }
    
    public List<UserFingerprint> getAllFingerprintTemplatesFromDatabase() throws SQLException, UareUException {
        
        List<UserFingerprint> userFingerprintList = new ArrayList<>();

        Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/absensi_kece", "root", "");

        try {
            // SQL untuk mengambil semua template sidik jari
            String sql = "SELECT id_siswa, nama_siswa, fid FROM siswa JOIN fid USING(id_siswa)";
            PreparedStatement statement = connection.prepareStatement(sql);

            // Eksekusi query
            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                int idSiswa = resultSet.getInt("id_siswa");
                String namaUser = resultSet.getString("nama_siswa");
                byte[] fingerprintTemplate = resultSet.getBytes("fid");

                // Konversi byte array menjadi objek FMD
                Fmd fmd = UareUGlobal.GetImporter().ImportFmd(
                    fingerprintTemplate, 
                    Fmd.Format.ANSI_378_2004, 
                    Fmd.Format.ANSI_378_2004
                );

                // Tambahkan FMD ke list
                UserFingerprint userFingerprint = new UserFingerprint(idSiswa, namaUser, fmd);
                userFingerprintList.add(userFingerprint);
            }

        } finally {
            connection.close();  // Tutup koneksi database
        }

        return userFingerprintList;  // Return list of FMD
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

    
    public FingerScanner() {  
        initializeReader();
        startAutoCapture();
        initComponents();
    }


    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        imageLabel = new javax.swing.JLabel();
        nama = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("FingerPrint App");
        setResizable(false);

        imageLabel.setBackground(new java.awt.Color(255, 51, 51));

        nama.setFont(new java.awt.Font("Tahoma", 0, 24)); // NOI18N
        nama.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);

        jButton1.setText("Register");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(23, 23, 23)
                        .addComponent(imageLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 241, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(75, 75, 75)
                        .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 136, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(22, Short.MAX_VALUE))
            .addComponent(nama, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(38, 38, 38)
                .addComponent(imageLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 275, Short.MAX_VALUE)
                .addGap(18, 18, 18)
                .addComponent(nama, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(13, 13, 13))
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        closeReader();
        FingerRegister sc = new FingerRegister();
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
            java.util.logging.Logger.getLogger(FingerScanner.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(FingerScanner.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(FingerScanner.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(FingerScanner.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new FingerScanner().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel imageLabel;
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel nama;
    // End of variables declaration//GEN-END:variables
}
