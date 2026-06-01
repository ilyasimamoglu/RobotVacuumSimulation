package vacuumsim.controller;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import vacuumsim.model.Robot;
import vacuumsim.model.ChargingStation;

import java.util.List;

/**
 * ============================================================================
 * SINIF: SimulationEngine (Simülasyon Motoru / Kalp)
 * GÖREVİ: Oyunun arka planındaki tüm zamanı ve matematiği yönetir.
 * ============================================================================
 */
public class SimulationEngine {
    private Robot robot;
    private Pathfinder pathfinder;
    private Rectangle[][] hucreler;
    private ChargingStation istasyon;

    private Timeline oyunDongusu;
    private int gecenSaniye = 0;
    private int temizlenenKareSayisi = 0;
    private int toplananKirSayisi = 0;
    private int adimSayaci = 0; // YENİ: Bataryanın daha yavaş bitmesi için adım sayacı

    private vacuumsim.model.Dirt mevcutKir = null;
    private int kalanTemizlemeSuresi = 0;
    private boolean istasyonaDonuyor = false;
    private List<int[]> eveDonusRotasi = null;

    private Runnable arayuzGuncelle;
    private String aktifAlgoritma = "Rastgele";

    public SimulationEngine(Robot robot, Pathfinder pathfinder, Rectangle[][] hucreler, ChargingStation istasyon, Runnable arayuzGuncelle) {
        this.robot = robot;
        this.pathfinder = pathfinder;
        this.hucreler = hucreler;
        this.istasyon = istasyon;
        this.arayuzGuncelle = arayuzGuncelle;

        oyunDongusu = new Timeline(new KeyFrame(Duration.seconds(1), event -> motorTiki()));
        oyunDongusu.setCycleCount(Timeline.INDEFINITE);
    }

    private void motorTiki() {
        if (robot.getBatarya() > 0) {

            // Eğer batarya %20 veya altına düştüyse acil dönüşü başlat
            if (robot.getBatarya() <= 20 && !istasyonaDonuyor) {
                System.out.println("UYARI: Batarya Kritik Seviyede! Otomatik istasyona dönülüyor.");
                istasyonaDon();
            }

            adimSayaci++;
            // Bataryayı her adımda değil, her 4 adımda bir düşür (Ömrü 4 kat artırır)
            // Eğer daha da yavaş bitsin istersen buradaki 4 sayısını 6 veya 8 yapabilirsin.
            boolean sarjTuketilsinMi = (adimSayaci % 4 == 0);

            if (kalanTemizlemeSuresi > 0 && mevcutKir != null && !istasyonaDonuyor) {
                kalanTemizlemeSuresi--;
                gecenSaniye++;

                if (sarjTuketilsinMi) {
                    robot.sarjTuket(1 + mevcutKir.getEkstraBataryaTuketimi());
                }

                if (kalanTemizlemeSuresi == 0) {
                    hucreler[robot.getY()][robot.getX()].setFill(Color.web("#ffffff"));
                    temizlenenKareSayisi++;
                    toplananKirSayisi++;
                    mevcutKir = null;
                }
            }
            else {
                if (sarjTuketilsinMi) {
                    robot.sarjTuket(1);
                }
                gecenSaniye++;
                robotuIleriTasit();
                if (!istasyonaDonuyor) yeriTemizle();
            }

            arayuzGuncelle.run();
        } else {
            oyunDongusu.stop();
            System.out.println("ŞARJ BİTTİ!");
        }
    }

    private void robotuIleriTasit() {
        if (istasyonaDonuyor) {
            if (eveDonusRotasi != null && !eveDonusRotasi.isEmpty()) {
                int[] adim = eveDonusRotasi.remove(0);
                robot.setX(adim[0]);
                robot.setY(adim[1]);
            } else {
                istasyonaDonuyor = false;
                robot.fullSajYap();
            }
        } else {
            if (aktifAlgoritma.equals("Rastgele")) pathfinder.hareketRastgele();
            else if (aktifAlgoritma.equals("Spiral")) pathfinder.hareketSpiral();
            else if (aktifAlgoritma.equals("Duvar Takibi")) pathfinder.hareketDuvarTakibi();
        }
    }

    private void yeriTemizle() {
        Rectangle bulunduguKare = hucreler[robot.getY()][robot.getX()];
        Color renk = (Color) bulunduguKare.getFill();

        if (renk.equals(Color.web("#faebd7"))) {
            bulunduguKare.setFill(Color.web("#ffffff"));
            temizlenenKareSayisi++;
        } else if (!renk.equals(Color.web("#ffffff")) && !renk.equals(Color.web("#2d3436"))) {
            if (renk.equals(Color.web("#b2bec3"))) mevcutKir = new vacuumsim.model.Dust();
            else if (renk.equals(Color.web("#74b9ff"))) mevcutKir = new vacuumsim.model.Liquid();
            else if (renk.equals(Color.web("#a29bfe"))) mevcutKir = new vacuumsim.model.Stain();

            if (mevcutKir != null) kalanTemizlemeSuresi = mevcutKir.getTemizlenmeSuresi();
        }
    }

    public void baslat() { oyunDongusu.play(); }
    public void duraklat() { oyunDongusu.pause(); }
    public void setHiz(double hizCarpani) { if (oyunDongusu != null) oyunDongusu.setRate(hizCarpani); }
    public void setAktifAlgoritma(String algo) { this.aktifAlgoritma = algo; }

    public void istasyonaDon() {
        istasyonaDonuyor = true; mevcutKir = null; kalanTemizlemeSuresi = 0;
        eveDonusRotasi = pathfinder.bfsIstasyonaDonRotaBul();
    }

    public void sifirla() {
        oyunDongusu.stop();
        robot.fullSajYap();
        robot.setX(istasyon.getX());
        robot.setY(istasyon.getY());
        gecenSaniye = 0; temizlenenKareSayisi = 0; toplananKirSayisi = 0;
        mevcutKir = null; kalanTemizlemeSuresi = 0; istasyonaDonuyor = false; adimSayaci = 0;
        setHiz(5.0);
    }

    public int getTemizlenenKareSayisi() { return temizlenenKareSayisi; }
    public int getToplananKirSayisi() { return toplananKirSayisi; }

    public String getFormatliSure() {
        return String.format("Geçen Süre: %02d:%02d", gecenSaniye / 60, gecenSaniye % 60);
    }

    public int getTemizlemeYuzdesi(int toplamAlan) {
        if (toplamAlan == 0) return 0;
        return (int) Math.round(((double) temizlenenKareSayisi / toplamAlan) * 100);
    }

    public int getKalanAlan(int toplamAlan) {
        return toplamAlan - temizlenenKareSayisi;
    }
}