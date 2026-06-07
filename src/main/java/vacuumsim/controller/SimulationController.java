package vacuumsim.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;
import vacuumsim.model.Robot;
import vacuumsim.model.Room;
import vacuumsim.model.ChargingStation;
import vacuumsim.view.RoomGridView;

/**
 * ============================================================================
 * SINIF: SimulationController (Kontrolcü / Köprü)
 * GÖREVİ: MVC mimarisindeki "C" (Controller) katmanıdır.
 * Sadece bir köprü görevi görür. Kullanıcının arayüzdeki (UI) butonlara tıklamalarını
 * dinler, bu tıklamaları ilgili motorlara/beyinlere iletir ve oradan gelen
 * sonuçları alıp ekrandaki metinleri (Label) günceller.
 * İçerisinde matematiksel hesaplama veya karmaşık mantık barındırmaz.
 * ============================================================================
 */
public class SimulationController {

    @FXML private ComboBox<String> cmbKirTuru;
    @FXML private ComboBox<String> cmbOdaDuzeni;
    @FXML private Button btnKirEkle, btnMobilyaEkle, btnBaslat, btnDuraklat, btnSifirla, btnKirleriTemizle, btnRobotuBul;
    @FXML private Slider sldHiz;
    @FXML private Slider sldBatarya;
    @FXML private CheckBox chkSes;
    @FXML private ToggleGroup algoGroup;
    @FXML private Label lblYon, lblBatarya, lblKonum;
    @FXML private Label lblToplamAlan, lblTemizlenenAlan, lblKalanAlan, lblGecenSure, lblToplananToz;
    @FXML private Pane ortaOyunAlani;

    // --- OOP NESNELERİMİZ VE YÖNETİCİLER ---
    private Room oda = new Room(21, 18);
    private ChargingStation istasyon = new ChargingStation(0, 0);
    private Robot robot;
    private Pathfinder pathfinder;
    private RoomGridView gridView;
    private SimulationEngine motor;

    private enum EditModu { BOSTA, KIR_EKLE, MOBILYA_EKLE }
    private EditModu aktifMod = EditModu.BOSTA;

    @FXML
    public void initialize() {
        cmbKirTuru.getItems().addAll("Toz", "Sıvı", "Leke");
        cmbKirTuru.getSelectionModel().selectFirst();
 
        cmbOdaDuzeni.getItems().addAll("Boş Oda", "Oturma Odası", "Çok Odalı Daire", "Labirent", "Ulaşılamaz Alan");
        cmbOdaDuzeni.getSelectionModel().selectFirst();

        // 1. Ressamı (View) Kur ve Haritayı Çizdir
        gridView = new RoomGridView(ortaOyunAlani, oda, 35.0);
        for (int satir = 0; satir < oda.getSatirSayisi(); satir++) {
            for (int sutun = 0; sutun < oda.getSutunSayisi(); sutun++) {
                Rectangle kare = gridView.hucreOlustur(satir, sutun);

                final int finalSatir = satir;
                final int finalSutun = sutun;
                kare.setOnMouseClicked(event -> {
                    // Şarj istasyonu konumuna elle engel veya kir eklenmesini engelliyoruz
                    if (finalSatir == 0 && finalSutun == 0) return;

                    if (aktifMod == EditModu.KIR_EKLE) {
                        String secilenKir = cmbKirTuru.getValue();
                        Room.HucreTuru tur = Room.HucreTuru.TEMIZ;
                        if (secilenKir.equals("Toz")) tur = Room.HucreTuru.TOZ;
                        else if (secilenKir.equals("Sıvı")) tur = Room.HucreTuru.SIVI;
                        else if (secilenKir.equals("Leke")) tur = Room.HucreTuru.LEKE;

                        oda.setHucreTuru(finalSutun, finalSatir, tur);
                        gridView.hucreYenile(finalSatir, finalSutun, tur);
                    } else if (aktifMod == EditModu.MOBILYA_EKLE) {
                        oda.setHucreTuru(finalSutun, finalSatir, Room.HucreTuru.ENGEL);
                        gridView.hucreYenile(finalSatir, finalSutun, Room.HucreTuru.ENGEL);
                    }
                });
            }
        }

        // 2. Modelleri, Beyni ve Motoru Kur
        robot = new Robot(istasyon.getX(), istasyon.getY());
        pathfinder = new Pathfinder(robot, oda);
        gridView.robotuCiz(robot);

        // Motora "Her saniye bu ekraniGuncelle metodunu çalıştır" diyoruz.
        motor = new SimulationEngine(robot, pathfinder, oda, istasyon, this::ekraniGuncelle);

        // Algoritma seçimini motora bağla
        algoGroup.selectedToggleProperty().addListener((obs, eski, yeni) -> {
            if (yeni != null) motor.setAktifAlgoritma(((RadioButton) yeni).getText());
        });

        // Hız çubuğu
        sldHiz.valueProperty().addListener((obs, eski, yeni) -> {
            motor.setHiz(Math.max(yeni.doubleValue() / 10.0, 0.2));
        });
        motor.setHiz(Math.max(sldHiz.getValue() / 10.0, 0.2));

        // Batarya manuel ayar sürgüsü (PDF gereksinimi)
        sldBatarya.valueProperty().addListener((obs, eski, yeni) -> {
            int yeniBatarya = yeni.intValue();
            if (robot.getBatarya() != yeniBatarya) {
                robot.setBatarya(yeniBatarya);
                ekraniGuncelle();
            }
        });

        // Ses Efektleri CheckBox dinleyicisi
        chkSes.selectedProperty().addListener((obs, eski, yeni) -> {
            SesYonetici.setSesAcik(yeni);
        });
        SesYonetici.setSesAcik(chkSes.isSelected());

        ekraniGuncelle();
    }

    // --- EKRAN GÜNCELLEME (Sıfır Matematik) ---
    private void ekraniGuncelle() {
        lblBatarya.setText("%" + robot.getBatarya());
        sldBatarya.setValue(robot.getBatarya()); // Sürgüyü de güncelle
        lblKonum.setText("(" + robot.getX() + ", " + robot.getY() + ")");
        lblYon.setText(robot.getYon().toString());

        lblGecenSure.setText(motor.getFormatliSure());

        int alan = oda.getToplamAlan();
        lblToplamAlan.setText("Toplam Alan: " + alan + " m²");
        lblTemizlenenAlan.setText("Temizlenen: " + motor.getTemizlenenKareSayisi() + " m² (" + motor.getTemizlemeYuzdesi(alan) + "%)");
        lblKalanAlan.setText("Kalan: " + motor.getKalanAlan(alan) + " m²");
        lblToplananToz.setText("Toplanan Kir: " + motor.getToplananKirSayisi());

        gridView.robotuGuncelle(robot);
        gridView.tumTahtayiGuncelle(); // Arayüz karolarını modelin durumuna göre güncelle (MVC)
    }

    @FXML
    public void odaDuzeniDegisti() {
        String secilenDuzen = cmbOdaDuzeni.getValue();
        System.out.println("Oda düzeni değişti: " + secilenDuzen);
        
        motor.sifirla(); // Simülasyonu durdur ve sıfırla
        oda.odaDuzeniniYukle(secilenDuzen); // Modeli yeni düzenle yükle
        
        gridView.tahtayiSifirla(); // Arayüz izlerini ve hücreleri temizle/güncelle
        gridView.robotuGuncelle(robot); // Robotu (0,0) konumuna çek
        ekraniGuncelle();
    }
 
    // --- BUTON KOMUTLARI ---
    @FXML public void baslatTiklandi() { motor.baslat(); }
    @FXML public void duraklatTiklandi() { motor.duraklat(); }
    @FXML public void kirEkleTiklandi() { aktifMod = EditModu.KIR_EKLE; }
    @FXML public void mobilyaEkleTiklandi() { aktifMod = EditModu.MOBILYA_EKLE; }
    @FXML
    public void kirleriTemizleTiklandi() {
        System.out.println("Sadece kirler temizleniyor...");
        motor.kirleriTemizle();
        gridView.tumTahtayiGuncelle();
        ekraniGuncelle();
    }

    @FXML
    public void sifirlaTiklandi() {
        motor.sifirla();
        gridView.tahtayiSifirla();
        gridView.robotuGuncelle(robot);
        ekraniGuncelle();
    }

    @FXML
    public void rotayiSifirlaTiklandi() {
        motor.rotayiSifirla();
        gridView.tahtayiSifirla();
        gridView.robotuGuncelle(robot);
        ekraniGuncelle();
    }

    @FXML
    public void robotuBulTiklandi() {
        SesYonetici.oynatRobotuBul();
    }
}