package vacuumsim.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
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
    @FXML private Button btnKirEkle, btnMobilyaEkle, btnBaslat, btnDuraklat, btnSifirla, btnIstasyonaDon;
    @FXML private Slider sldHiz;
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

        // 1. Ressamı (View) Kur ve Haritayı Çizdir
        gridView = new RoomGridView(ortaOyunAlani, oda, 35.0);
        for (int satir = 0; satir < oda.getSatirSayisi(); satir++) {
            for (int sutun = 0; sutun < oda.getSutunSayisi(); sutun++) {
                Rectangle kare = gridView.hucreOlustur(satir, sutun);

                kare.setOnMouseClicked(event -> {
                    if (aktifMod == EditModu.KIR_EKLE) {
                        String secilenKir = cmbKirTuru.getValue();
                        if (secilenKir.equals("Toz")) kare.setFill(Color.web("#b2bec3"));
                        else if (secilenKir.equals("Sıvı")) kare.setFill(Color.web("#74b9ff"));
                        else if (secilenKir.equals("Leke")) kare.setFill(Color.web("#a29bfe"));
                    } else if (aktifMod == EditModu.MOBILYA_EKLE) {
                        kare.setFill(Color.web("#2d3436"));
                    }
                });
            }
        }

        // 2. Modelleri, Beyni ve Motoru Kur
        robot = new Robot(istasyon.getX(), istasyon.getY());
        pathfinder = new Pathfinder(robot, gridView.getHucreler(), oda.getSutunSayisi(), oda.getSatirSayisi());
        gridView.robotuCiz(robot);

        // Motora "Her saniye bu ekraniGuncelle metodunu çalıştır" diyoruz.
        motor = new SimulationEngine(robot, pathfinder, gridView.getHucreler(), istasyon, this::ekraniGuncelle);

        // Algoritma seçimini motora bağla
        algoGroup.selectedToggleProperty().addListener((obs, eski, yeni) -> {
            if (yeni != null) motor.setAktifAlgoritma(((RadioButton) yeni).getText());
        });

        // Hız çubuğu
        sldHiz.valueProperty().addListener((obs, eski, yeni) -> {
            motor.setHiz(Math.max(yeni.doubleValue() / 10.0, 0.2));
        });

        ekraniGuncelle();
    }

    // --- EKRAN GÜNCELLEME (Sıfır Matematik) ---
    private void ekraniGuncelle() {
        lblBatarya.setText("%" + robot.getBatarya());
        lblKonum.setText("(" + robot.getX() + ", " + robot.getY() + ")");
        lblYon.setText(robot.getYon().toString());

        lblGecenSure.setText(motor.getFormatliSure());

        int alan = oda.getToplamAlan();
        lblToplamAlan.setText("Toplam Alan: " + alan + " m²");
        lblTemizlenenAlan.setText("Temizlenen: " + motor.getTemizlenenKareSayisi() + " m² (" + motor.getTemizlemeYuzdesi(alan) + "%)");
        lblKalanAlan.setText("Kalan: " + motor.getKalanAlan(alan) + " m²");
        lblToplananToz.setText("Toplanan Kir: " + motor.getToplananKirSayisi());

        gridView.robotuGuncelle(robot);
    }

    // --- BUTON KOMUTLARI ---
    @FXML public void baslatTiklandi() { motor.baslat(); }
    @FXML public void duraklatTiklandi() { motor.duraklat(); }
    @FXML public void kirEkleTiklandi() { aktifMod = EditModu.KIR_EKLE; }
    @FXML public void mobilyaEkleTiklandi() { aktifMod = EditModu.MOBILYA_EKLE; }
    @FXML
    public void istasyonaDonTiklandi() {
        System.out.println("İstasyona Dön tuşuna basıldı! Rota çiziliyor...");
        motor.istasyonaDon();
        motor.baslat(); // EĞER OYUN DURDURULDUYSA VEYA ŞARJ BİTTİYSE MOTORU ZORLA ÇALIŞTIR!
    }

    @FXML
    public void sifirlaTiklandi() {
        motor.sifirla();
        gridView.tahtayiSifirla();
        gridView.robotuGuncelle(robot);
        sldHiz.setValue(50.0);
        ekraniGuncelle();
    }
}