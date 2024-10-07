/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fingerapp;

import com.digitalpersona.uareu.Fmd;

/**
 *
 * @author Adiatna Sukmana
 */
public class UserFingerprint {
    private int idSiswa;
    private String namaUser;  // Nama pengguna
    private Fmd fingerprintTemplate;  // Template sidik jari

    public UserFingerprint(int idSiswa, String namaUser, Fmd fingerprintTemplate) {
        this.idSiswa = idSiswa;
        this.namaUser = namaUser;
        this.fingerprintTemplate = fingerprintTemplate;
    }
    
    public Integer getId(){
        return idSiswa;
    }

    public String getNamaUser() {
        return namaUser;
    }

    public Fmd getFingerprintTemplate() {
        return fingerprintTemplate;
    }

    @Override
    public String toString() {
        return "User: " + namaUser;
    }
}