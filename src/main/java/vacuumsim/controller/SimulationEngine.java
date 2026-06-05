package vacuumsim.controller;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import vacuumsim.model.Robot;
import vacuumsim.model.Room;
import vacuumsim.model.ChargingStation;

import java.util.List;

/**
 * ============================================================================
 * SINIF: SimulationEngine (Simülasyon Motoru / Kalp)
 * GÖREVİ: Simülasyonun arka planındaki tüm zamanı ve matematiği yönetir.
 * MVC Uyum: Arayüzden (View) bağımsız olarak doğrudan Room modeli üzerinde çalışır.
 * ============================================================================
 */
public class SimulationEngine {
    private Robot robot;
    private Pathfinder pathfinder;
    private Room oda;
    private ChargingStation istasyon;

    private Timeline oyunDongusu;
    private int gecenSaniye = 0;
    private int temizlenenKareSayisi = 0;
    private int toplananKirSayisi = 0;
    private int adimSayaci = 0; // Bataryanın daha yavaş bitmesi için adım sayacı

    private vacuumsim.model.Dirt mevcutKir = null;
    private int kalanTemizlemeSuresi = 0;
    private boolean istasyonaDonuyor = false;
    private List<int[]> eveDonusRotasi = null;

    // Temizliğe/kaldığı yere geri dönme rotası
    private boolean temizligeDonuyor = false;
    private List<int[]> temizligeDonusRotasi = null;

    private Runnable arayuzGuncelle;
    private String aktifAlgoritma = "Rastgele";

    public SimulationEngine(Robot robot, Pathfinder pathfinder, Room oda, ChargingStation istasyon, Runnable arayuzGuncelle) {
        this.robot = robot;
        this.pathfinder = pathfinder;
        this.oda = oda;
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
                SesYonetici.oynatDusukBatarya();
                istasyonaDon();
            }

            adimSayaci++;
            // Bataryayı her adımda değil, her 4 adımda bir düşür (Ömrü 4 kat artırır)
            boolean sarjTuketilsinMi = (adimSayaci % 4 == 0);

            if (kalanTemizlemeSuresi > 0 && mevcutKir != null && !istasyonaDonuyor) {
                kalanTemizlemeSuresi--;
                gecenSaniye++;

                if (sarjTuketilsinMi) {
                    robot.sarjTuket(1 + mevcutKir.getEkstraBataryaTuketimi());
                }

                if (kalanTemizlemeSuresi == 0) {
                    // Temizleme bittiğinde modeli güncelle
                    oda.setHucreTuru(robot.getX(), robot.getY(), Room.HucreTuru.TEMIZLENDI);
                    temizlenenKareSayisi++;
                    toplananKirSayisi++;
                    mevcutKir = null;
                    SesYonetici.oynatKirVakumlandi(); // Kir temizlendiğinde vakum sesi çal
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
                
                // Eğer tüm oda temizlendiği için istasyona döndüysek simülasyonu durdur (Sonsuz döngüyü önler)
                List<int[]> kalanTemizlenmemis = pathfinder.bfsEnYakinTemizlenmemisBul();
                if (kalanTemizlenmemis == null || kalanTemizlenmemis.isEmpty()) {
                    oyunDongusu.stop();
                    System.out.println("Simülasyon Bitti: Tüm oda temizlendi ve robot şarj istasyonuna döndü.");
                    SesYonetici.oynatTemizlikBitti();
                } else {
                    SesYonetici.oynatSarjOluyor();
                }
            }
        } else if (temizligeDonuyor) {
            if (temizligeDonusRotasi != null && !temizligeDonusRotasi.isEmpty()) {
                int[] adim = temizligeDonusRotasi.remove(0);
                robot.setX(adim[0]);
                robot.setY(adim[1]);
            } else {
                temizligeDonuyor = false;
                temizligeDonusRotasi = null;
            }
        } else {
            // Eğer aktif algoritma Spiral veya Zikzak ise ve robot hareket edemeyecek şekilde sıkışmışsa,
            // veya etrafındaki tüm alanlar süpürülmüş/engelle kaplıysa, BFS ile en yakın
            // temizlenmemiş hücreye giden rotayı bulup oraya yürütelim.
            if (aktifAlgoritma.equals("Spiral") || aktifAlgoritma.equals("Zikzak")) {
                if (mevcutKir == null && kalanTemizlemeSuresi == 0) {
                    boolean sikisti = false;
                    if (aktifAlgoritma.equals("Spiral") && pathfinder.etrafKapaliVeyaTemizlenmisMi()) {
                        sikisti = true;
                    } else if (aktifAlgoritma.equals("Zikzak") && pathfinder.zigzagSikistiMi()) {
                        sikisti = true;
                    }

                    if (sikisti) {
                        temizligeDonusRotasi = pathfinder.bfsEnYakinTemizlenmemisBul();
                        if (temizligeDonusRotasi != null && !temizligeDonusRotasi.isEmpty()) {
                            temizligeDonuyor = true;
                            int[] adim = temizligeDonusRotasi.remove(0);
                            robot.setX(adim[0]);
                            robot.setY(adim[1]);
                            return;
                        } else {
                            // Eğer hiç temizlenmemiş alan kalmadıysa otomatik şarj istasyonuna geri dön
                            System.out.println("Tüm oda temizlendi! Şarj istasyonuna dönülüyor.");
                            istasyonaDon();
                            SesYonetici.oynatSarjaDonuyor();
                            // Zaten istasyondaysak simülasyonu hemen durdur (Batarya dalgalanmasını ve log tekrarını önler)
                            if (robot.getX() == istasyon.getX() && robot.getY() == istasyon.getY()) {
                                oyunDongusu.stop();
                                System.out.println("Simülasyon Bitti: Robot zaten şarj istasyonunda.");
                            }
                            return;
                        }
                    }
                }
            }

            if (aktifAlgoritma.equals("Rastgele")) pathfinder.hareketRastgele();
            else if (aktifAlgoritma.equals("Zikzak")) pathfinder.hareketZigzag();
            else if (aktifAlgoritma.equals("Spiral")) pathfinder.hareketSpiral();
            else if (aktifAlgoritma.equals("Duvar Takibi")) pathfinder.hareketDuvarTakibi();
        }
    }

    private void yeriTemizle() {
        int x = robot.getX();
        int y = robot.getY();
        Room.HucreTuru tur = oda.getHucreTuru(x, y);

        // Eğer hücre temizlenmemiş/normal zemin ise doğrudan TEMIZLENDI yap
        if (tur == Room.HucreTuru.TEMIZ) {
            oda.setHucreTuru(x, y, Room.HucreTuru.TEMIZLENDI);
            temizlenenKareSayisi++;
        } 
        // Eğer hücre kirli ise türüne göre kir nesnesini oluştur ve süreyi başlat
        else if (tur == Room.HucreTuru.TOZ || tur == Room.HucreTuru.SIVI || tur == Room.HucreTuru.LEKE) {
            if (tur == Room.HucreTuru.TOZ) mevcutKir = new vacuumsim.model.Dust();
            else if (tur == Room.HucreTuru.SIVI) mevcutKir = new vacuumsim.model.Liquid();
            else if (tur == Room.HucreTuru.LEKE) mevcutKir = new vacuumsim.model.Stain();

            if (mevcutKir != null) {
                kalanTemizlemeSuresi = mevcutKir.getTemizlenmeSuresi();
            }
        }
    }

    public void baslat() {
        oyunDongusu.play();
        SesYonetici.oynatTemizlikBasladi();
    }
    public void duraklat() {
        oyunDongusu.pause();
        SesYonetici.oynatTemizlikDurduruldu();
    }
    public void setHiz(double hizCarpani) { if (oyunDongusu != null) oyunDongusu.setRate(hizCarpani); }
    public void setAktifAlgoritma(String algo) { this.aktifAlgoritma = algo; }

    public void istasyonaDon() {
        istasyonaDonuyor = true; mevcutKir = null; kalanTemizlemeSuresi = 0;
        temizligeDonuyor = false; temizligeDonusRotasi = null; // Eski dönüş rotasını sıfırla (Işınlanmayı önler)
        eveDonusRotasi = pathfinder.bfsIstasyonaDonRotaBul();
    }

    public void sifirla() {
        oyunDongusu.stop();
        robot.fullSajYap();
        robot.setX(istasyon.getX());
        robot.setY(istasyon.getY());
        gecenSaniye = 0; temizlenenKareSayisi = 0; toplananKirSayisi = 0;
        mevcutKir = null; kalanTemizlemeSuresi = 0; istasyonaDonuyor = false; adimSayaci = 0;
        temizligeDonuyor = false; temizligeDonusRotasi = null;
        pathfinder.reset();
        oda.sifirla(); // Modeli de sıfırla
    }
 
    public void rotayiSifirla() {
        oyunDongusu.stop();
        robot.fullSajYap();
        robot.setX(istasyon.getX());
        robot.setY(istasyon.getY());
        gecenSaniye = 0; temizlenenKareSayisi = 0; toplananKirSayisi = 0;
        mevcutKir = null; kalanTemizlemeSuresi = 0; istasyonaDonuyor = false; adimSayaci = 0;
        temizligeDonuyor = false; temizligeDonusRotasi = null;
        pathfinder.reset();
        oda.rotayiSifirla(); // Modeli de sadece rotayı sıfırlayarak güncelle
    }

    public void kirleriTemizle() {
        oda.kirleriTemizle();
        mevcutKir = null;
        kalanTemizlemeSuresi = 0;
        toplananKirSayisi = 0;
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