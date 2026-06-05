package vacuumsim.controller;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.SourceDataLine;
import java.io.File;

/**
 * ============================================================================
 * SINIF: SesYonetici (Ses Sentezleyici ve Çalar)
 * GÖREVİ: Simülasyonda gerçekleşen olaylara göre ses efektleri oynatır.
 * GLaDOS Ses Paketi yüklüyse (.wav dosyaları) onları çalar,
 * yoksa otomatik olarak Java'nın yerleşik ses sentezleyicisiyle bip melodileri üretir.
 * Tüm çalma işlemleri ana arayüzü dondurmaması için arka planda (Thread) asenkron çalışır.
 * 
 * GLaDOS Ses Paketi Kaynağı: https://github.com/arner/roborock-glados.git
 * ============================================================================
 */
public class SesYonetici {

    private static boolean sesAcik = true;
    private static boolean sesCaliniyor = false;
    private static final String SES_PAKETI_YOLU = "GLaDOS_Voice_Pack/default-v1/";

    public static void setSesAcik(boolean acik) {
        sesAcik = acik;
    }

    public static boolean isSesAcik() {
        return sesAcik;
    }

    public static boolean isSesCaliniyor() {
        return sesCaliniyor && sesAcik; // Ses kapalıysa kilit aktif olmasın
    }

    /**
     * Verilen dosya adındaki ses dosyasını oynatmaya çalışır.
     * Dosya bulunamaz veya hata oluşursa false döner (fallback tetiklenmesi için).
     */
    private static boolean dosyadanOynat(String dosyaAdi) {
        if (!sesAcik) return true; // Ses kapalıysa işlem tamamlanmış gibi kabul et

        File sesDosyasi = new File(SES_PAKETI_YOLU + dosyaAdi);
        if (!sesDosyasi.exists()) {
            System.out.println("Ses dosyası bulunamadı, fallback bip çalınacak: " + sesDosyasi.getPath());
            return false;
        }

        sesCaliniyor = true; // Konuşma başladı, hareketi kilitle
        new Thread(() -> {
            try (AudioInputStream audioStream = AudioSystem.getAudioInputStream(sesDosyasi)) {
                Clip clip = AudioSystem.getClip();
                clip.open(audioStream);
                clip.start();
                
                // Klip çalmaya başlayana ve bitene kadar iş parçacığını canlı tut
                Thread.sleep(100);
                int sayac = 0;
                while (clip.isRunning() && sayac < 100) {
                    Thread.sleep(100);
                    sayac++;
                }
                Thread.sleep(100);
                clip.close();
            } catch (Exception e) {
                System.err.println("Ses dosyası çalınırken hata oluştu: " + e.getMessage());
            } finally {
                sesCaliniyor = false; // Konuşma bitti, kilidi kaldır
            }
        }).start();

        return true;
    }

    /**
     * Belirli frekans ve sürede yapay bip sesi sentezler (Fallback mekanizması).
     */
    public static void bip(int frekans, int sureMs, double sesSeviyesi) {
        if (!sesAcik) return;

        new Thread(() -> {
            try {
                int orneklemeHizi = 8000;
                byte[] veri = new byte[sureMs * orneklemeHizi / 1000];
                for (int i = 0; i < veri.length; i++) {
                    double aci = i / (8000.0 / frekans) * 2.0 * Math.PI;
                    veri[i] = (byte) (Math.sin(aci) * 127.0 * sesSeviyesi);
                }

                AudioFormat format = new AudioFormat(8000f, 8, 1, true, false);
                try (SourceDataLine hat = AudioSystem.getSourceDataLine(format)) {
                    hat.open(format);
                    hat.start();
                    hat.write(veri, 0, veri.length);
                    hat.drain();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private static void bekle(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // =========================================================================
    // SİMÜLASYON OLAYLARINA GÖRE ÇALINACAK MELODİLER (GLaDOS veya Bip Fallback)
    // =========================================================================

    public static void oynatTemizlikBasladi() {
        boolean basarili = dosyadanOynat("start.wav");
        if (!basarili) {
            // Fallback: Do - Mi - Sol (Yükselen melodi)
            bip(523, 150, 0.5); // Do (C5)
            bekle(150);
            bip(659, 150, 0.5); // Mi (E5)
            bekle(150);
            bip(784, 250, 0.5); // Sol (G5)
        }
    }

    public static void oynatTemizlikDurduruldu() {
        boolean basarili = dosyadanOynat("pause.wav");
        if (!basarili) {
            // Fallback: Sol - Mi (Düşen melodi)
            bip(784, 150, 0.5); // Sol
            bekle(150);
            bip(659, 250, 0.5); // Mi
        }
    }

    public static void oynatSarjOluyor() {
        boolean basarili = dosyadanOynat("charging.wav");
        if (!basarili) {
            // Fallback: Sol - Do - Mi - Sol (Hızlı şarj melodisi)
            bip(392, 100, 0.5); // Sol
            bekle(100);
            bip(523, 100, 0.5); // Do
            bekle(100);
            bip(659, 100, 0.5); // Mi
            bekle(100);
            bip(784, 300, 0.5); // Sol
        }
    }

    public static void oynatSarjaDonuyor() {
        dosyadanOynat("home.wav");
        // Fallback ses üretilmesine gerek yok, oynatDusukBatarya veya oynatTemizlikDurduruldu çalacaktır
    }

    public static void oynatDusukBatarya() {
        boolean basarili = dosyadanOynat("no_power.wav");
        if (!basarili) {
            // Fallback: Kalın çift uyarı bipi
            bip(440, 200, 0.6); // La
            bekle(250);
            bip(440, 200, 0.6); // La
        }
    }

    public static void oynatTemizlikBitti() {
        boolean basarili = dosyadanOynat("finish.wav");
        if (!basarili) {
            // Fallback: Do-Re-Mi-Fa-Sol-La-Si-Do (Zafer melodisi)
            int[] notalar = {523, 587, 659, 698, 784, 880, 988, 1047};
            for (int nota : notalar) {
                bip(nota, 80, 0.4);
                bekle(90);
            }
        }
    }

    public static void oynatRobotuBul() {
        boolean basarili = dosyadanOynat("findme.wav");
        if (!basarili) {
            // Fallback: Radar / Konum bulucu yüksek sesli bipleyici
            for (int i = 0; i < 3; i++) {
                bip(1200, 100, 0.6);
                bekle(120);
            }
        }
    }

    /**
     * Vakumlama hissi veren hışırtı ses sentezi (gürültü/noise ve düşen frekans karışımı)
     * Kir temizlendiğinde çalınır.
     */
    public static void oynatKirVakumlandi() {
        if (!sesAcik) return;
        new Thread(() -> {
            try {
                int sureMs = 150;
                int orneklemeHizi = 8000;
                byte[] veri = new byte[sureMs * orneklemeHizi / 1000];
                for (int i = 0; i < veri.length; i++) {
                    double gecerliFrekans = 350.0 * (1.0 - (double) i / veri.length);
                    double aci = i / (8000.0 / gecerliFrekans) * 2.0 * Math.PI;
                    double gurultu = (Math.random() - 0.5) * 35;
                    double sinDalga = Math.sin(aci) * 50;
                    veri[i] = (byte) Math.max(-128, Math.min(127, (sinDalga + gurultu) * 0.35));
                }

                AudioFormat format = new AudioFormat(8000f, 8, 1, true, false);
                try (SourceDataLine hat = AudioSystem.getSourceDataLine(format)) {
                    hat.open(format);
                    hat.start();
                    hat.write(veri, 0, veri.length);
                    hat.drain();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
