package vacuumsim.controller;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
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
import javafx.util.Duration;
import vacuumsim.model.Robot;

public class SimulationController {

    // --- HARİTA EDİTÖRÜ ELEMANLARI ---
    @FXML private ComboBox<String> cmbKirTuru;
    @FXML private Button btnKirEkle;
    @FXML private Button btnMobilyaEkle;

    // --- AYARLAR ELEMANLARI ---
    @FXML private Slider sldHiz;
    @FXML private ToggleGroup algoGroup; // Togglegroup bizi if-else hamallığından kurtarır

    // --- ROBOT DURUM ELEMANLARI (SOL PANEL) ---
    @FXML private Label lblYon;
    @FXML private Label lblBatarya;
    @FXML private Label lblKonum;

    // --- KONTROL ELEMANLARI ---
    @FXML private Button btnBaslat;
    @FXML private Button btnDuraklat;
    @FXML private Button btnSifirla;
    @FXML private Button btnIstasyonaDon;

    // --- OYUN ALANI (TUVAL) ---
    @FXML private Pane ortaOyunAlani;

    // --- ALT BİLGİ ÇUBUĞU ELEMANLARI (Senin kodunda eksik olan yer burasıydı) ---
    @FXML private Label lblToplamAlan;
    @FXML private Label lblTemizlenenAlan;
    @FXML private Label lblKalanAlan;
    @FXML private Label lblGecenSure;
    @FXML private Label lblToplananToz;

    // --- ODA IZGARASI (GRID) DEĞİŞKENLERİ ---
    private final int SUTUN_SAYISI = 21;        // X ekseni (Genişlik)
    private final int SATIR_SAYISI = 18;        // Y ekseni (Yükseklik)
    private final double HUCRE_BOYUTU = 35.0;   // Ekrandaki her bir karenin piksel boyutu
    private Rectangle[][] hucreler;             // 2 Boyutlu matrisimiz

    // --- SİMÜLASYON MOTORU DEĞİŞKENLERİ ---
    private Robot robot;
    private Timeline oyunDongusu;
    private int gecenSaniye = 0;   // Kronometre için saniye sayacı
    private javafx.scene.shape.Circle gorselRobot; // Ekranda göreceğimiz fiziksel robotumuz
    private int temizlenenKareSayisi = 0;
    private enum EditModu { BOSTA, KIR_EKLE, MOBILYA_EKLE }
    private EditModu aktifMod = EditModu.BOSTA;
    private boolean istasyonaDonuyor = false; // Robotun eve dönme modunda olup olmadığını tutacak

    // INITIALIZE: PROGRAMIN BAŞLANGIÇ NOKTASI

    @FXML
    public void initialize() {
        cmbKirTuru.getItems().addAll("Toz", "Sıvı", "Leke");
        cmbKirTuru.getSelectionModel().selectFirst();

        // Önce tahtayı çiz
        odaIzgarasiniCiz();

        // Robotu (0,0) konumunda yarat ve döngüyü hazırda beklet
        robot = new Robot(0, 0);
        robotuEkranaEkle();
        ekraniGuncelle();
        oyunDongusunuKur();
        // --- HIZ ÇUBUĞUNU (SLIDER) OYUN MOTORUNA BAĞLAMA ---
        sldHiz.valueProperty().addListener((observable, eskiDeger, yeniDeger) -> {
            // Slider varsayılan olarak 0 ile 100 arasında.
            // 50 değerini normal hız (1.0x) kabul ediyoruz.
            // Çubuğu en sağa (100'e) çekersek hız çarpanı 100 / 10.0 = 10x hıza kadar çıkacak!
            double hizCarpani = yeniDeger.doubleValue() / 10.0;

            // Sıfıra çekildiğinde robotun tamamen donup kalmasını engellemek için taban hız koyuyoruz
            if (hizCarpani < 0.2) hizCarpani = 0.2;

            if (oyunDongusu != null) {
                oyunDongusu.setRate(hizCarpani); // Motorun zaman akış hızını anlık olarak değiştir
            }
        });


    }


    // ODANIN KARELERİNİ OLUŞTURAN METOT

    private void odaIzgarasiniCiz() {
        hucreler = new Rectangle[SATIR_SAYISI][SUTUN_SAYISI];

        // Tahtanın boyutunu kilitliyoruz ki pencere büyüdüğünde çirkinleşmesin
        double tamGenislik = SUTUN_SAYISI * HUCRE_BOYUTU;
        double tamYukseklik = SATIR_SAYISI * HUCRE_BOYUTU;
        ortaOyunAlani.setPrefSize(tamGenislik, tamYukseklik);
        ortaOyunAlani.setMinSize(tamGenislik, tamYukseklik);
        ortaOyunAlani.setMaxSize(tamGenislik, tamYukseklik);

        for (int satir = 0; satir < SATIR_SAYISI; satir++) {
            for (int sutun = 0; sutun < SUTUN_SAYISI; sutun++) {

                Rectangle kare = new Rectangle(HUCRE_BOYUTU, HUCRE_BOYUTU);
                kare.setFill(Color.web("#faebd7"));
                kare.setStroke(Color.LIGHTGRAY);
                kare.setX(sutun * HUCRE_BOYUTU);
                kare.setY(satir * HUCRE_BOYUTU);

                hucreler[satir][sutun] = kare;
                ortaOyunAlani.getChildren().add(kare);
                // --- KAREYE TIKLAMA OLAYI ---
                kare.setOnMouseClicked(event -> {
                    if (aktifMod == EditModu.KIR_EKLE) {
                        String secilenKir = cmbKirTuru.getValue();
                        if (secilenKir.equals("Toz")) kare.setFill(Color.web("#b2bec3"));
                        else if (secilenKir.equals("Sıvı")) kare.setFill(Color.web("#74b9ff"));
                        else if (secilenKir.equals("Leke")) kare.setFill(Color.web("#a29bfe"));
                    }
                    else if (aktifMod == EditModu.MOBILYA_EKLE) {
                        kare.setFill(Color.web("#2d3436")); // Duvar / Mobilya rengi
                    }
                });
            }
        }
    }


    // SİMÜLASYON ZAMANLAYICISI (GAME LOOP)

    private void oyunDongusunuKur() {
        // Her 1 saniyede bir çalışacak motor
        oyunDongusu = new Timeline(new KeyFrame(Duration.seconds(1), event -> {

            if (robot.getBatarya() > 0) {
                robot.sarjTuket(1);
                gecenSaniye++;      // Kronometreyi 1 saniye artır
                //  Kalp her attığında robotu da 1 kare yürütüyoruz:
                robotuIleriTasit();
                yeriTemizle();

                ekraniGuncelle();   // Değişiklikleri ekrana yansıt
            } else {
                oyunDongusu.stop();
                System.out.println("ŞARJ BİTTİ! Robot olduğu yerde kaldı.");
            }
        }));
        oyunDongusu.setCycleCount(Timeline.INDEFINITE); // Sonsuza dek dön
    }


    // DİNAMİK EKRAN GÜNCELLEME MOTORU

    private void ekraniGuncelle() {
        // 1. Sol Panel
        lblBatarya.setText("%" + robot.getBatarya());
        lblKonum.setText("(" + robot.getX() + ", " + robot.getY() + ")");
        lblYon.setText(robot.getYon().toString());

        // 2. Alt Çubuk - Süre formatlama (Örn: 02:05)
        int dakika = gecenSaniye / 60;
        int saniye = gecenSaniye % 60;
        String formatliSure = String.format("Geçen Süre: %02d:%02d", dakika, saniye);
        lblGecenSure.setText(formatliSure);

        int toplamAlanM2 = SUTUN_SAYISI * SATIR_SAYISI;
        int temizlemeYuzdesi = (int) Math.round(((double) temizlenenKareSayisi / toplamAlanM2) * 100);

        lblToplamAlan.setText("Toplam Alan: " + toplamAlanM2 + " m²");
        lblTemizlenenAlan.setText("Temizlenen: " + temizlenenKareSayisi + " m² (" + temizlemeYuzdesi + "%)");
        lblKalanAlan.setText("Kalan: " + (toplamAlanM2 - temizlenenKareSayisi) + " m²");
        lblToplananToz.setText("Toz: 0");
    }
    // ROBOTU FİZİKSEL OLARAK EKRANA EKLEYEN METOT
    private void robotuEkranaEkle() {
        // Robotumuzu bir daire yapıyoruz. Yarıçapı hücre boyutunun yarısından biraz küçük olsun ki karenin içine tam otursun.
        gorselRobot = new javafx.scene.shape.Circle((HUCRE_BOYUTU / 2) - 4);
        gorselRobot.setFill(Color.web("#e17055")); // Şık bir turuncu renk
        gorselRobot.setStroke(Color.web("#2d3436")); // Siyah bir dış çerçeve
        gorselRobot.setStrokeWidth(2);

        // Robotu başlangıç (X,Y) hücresinin tam merkezine oturtma matematiği
        gorselRobot.setCenterX((robot.getX() * HUCRE_BOYUTU) + (HUCRE_BOYUTU / 2));
        gorselRobot.setCenterY((robot.getY() * HUCRE_BOYUTU) + (HUCRE_BOYUTU / 2));

        ortaOyunAlani.getChildren().add(gorselRobot); // Sahaya fırlat!
    }
    // ROBOTUN BEYNİ (ANA HAREKET YÖNETİCİSİ)
    private void robotuIleriTasit() {
        // Arayüzde hangi algoritma seçili onu buluyoruz
        RadioButton secilenButon = (RadioButton) algoGroup.getSelectedToggle();
        String secilenAlgoritma = secilenButon.getText();

        // Seçilen algoritmaya göre ilgili metodu çalıştır
        if (secilenAlgoritma.equals("Rastgele")) {
            hareketRastgele();
        } else if (secilenAlgoritma.equals("Spiral")) {
            hareketSpiral();
        } else if (secilenAlgoritma.equals("Duvar Takibi")) {
            hareketDuvarTakibi();
        }

        // Hangi algoritma çalışırsa çalışsın, en son Görsel Robotu yeni (X,Y) konumuna kaydır
        gorselRobot.setCenterX((robot.getX() * HUCRE_BOYUTU) + (HUCRE_BOYUTU / 2));
        gorselRobot.setCenterY((robot.getY() * HUCRE_BOYUTU) + (HUCRE_BOYUTU / 2));
    }
    // ALGORİTMA 1: RASTGELE HAREKET
    // Önü açıksa dümdüz git. Duvara veya mobilyaya gelirse rastgele başka yöne dön.

    // ALGORİTMA 1: RASTGELE HAREKET (Geliştirilmiş Sekme)
    private void hareketRastgele() {
        if (onumAcikMi()) {
            ileriGit();
        } else {
            // Önü kapalıysa %50 ihtimalle Sağa, %50 ihtimalle Sola dönsün (Çarpıp sekme efekti)
            if (Math.random() > 0.5) {
                sagaDon();
            } else {
                solaDon();
            }
        }
    }
    // ALGORİTMA 2: SİPRAL HAREKET
    // Önü açık olduğu sürece DÜZ git. Duvara veya temizlenmiş(BEYAZ) bir yere gelirse SAĞA dön.
    // Bu sayede dışarıdan içeriye doğru mükemmel bir kare spiral çizecektir!

    private void hareketSpiral() {
        if (onumAcikMi() && !onumdekiKareTemizMi()) {
            ileriGit();
        } else {
            sagaDon();
        }
    }
    // ALGORİTMA 3: DUVAR TAKİBİ
    // Dümdüz git, duvara (engeli) çarpınca SOLA dön ve duvarı sağında tutarak çevreyi dolaş.
    // =========================================================================
    private void hareketDuvarTakibi() {
        if (onumAcikMi()) {
            ileriGit();
        } else {
            solaDon();
        }
    }
    // ROBOTUN SENSÖRLERİ VE FİZİKSEL YARDIMCI METOTLARI
    // =========================================================================

    // Robotun 1 adım sonrasındaki hedef X ve Y koordinatını hesaplar
    private int[] hedefKoordinatiBul() {
        int hedefX = robot.getX();
        int hedefY = robot.getY();
        if (robot.getYon() == Robot.YON.KUZEY) hedefY--;
        else if (robot.getYon() == Robot.YON.GUNEY) hedefY++;
        else if (robot.getYon() == Robot.YON.DOGU) hedefX++;
        else if (robot.getYon() == Robot.YON.BATI) hedefX--;
        return new int[]{hedefX, hedefY};
    }
    // Hedeflenen karede duvar (harita sınırı) veya mobilya var mı kontrol eder
    private boolean onumAcikMi() {
        int[] hedef = hedefKoordinatiBul();
        int hX = hedef[0];
        int hY = hedef[1];

        // 1. Sınır Kontrolü (Harita dışına çıkıyor mu?)
        if (hX < 0 || hX >= SUTUN_SAYISI || hY < 0 || hY >= SATIR_SAYISI) return false;

        // 2. İleride Mobilya Eklendiğinde siyah (#2d3436) renk varsa orası duvardır, geçemez
        Color renk = (Color) hucreler[hY][hX].getFill();
        if (renk.equals(Color.web("#2d3436"))) return false;

        return true; // Önü tertemiz açık!
    }
    // Spiral algoritması için önündeki karenin daha önce temizlenip temizlenmediğini sorar
    private boolean onumdekiKareTemizMi() {
        int[] hedef = hedefKoordinatiBul();
        if (hedef[0] >= 0 && hedef[0] < SUTUN_SAYISI && hedef[1] >= 0 && hedef[1] < SATIR_SAYISI) {
            Color renk = (Color) hucreler[hedef[1]][hedef[0]].getFill();
            return renk.equals(Color.web("#ffffff")); // Beyazsa temizdir
        }
        return false;
    }
    // Yönüne göre robotun koordinatını 1 birim değiştirir
    private void ileriGit() {
        int[] hedef = hedefKoordinatiBul();
        robot.setX(hedef[0]);
        robot.setY(hedef[1]);
    }
    // Robotu olduğu yerde 90 derece Sağa döndürür
    private void sagaDon() {
        if (robot.getYon() == Robot.YON.KUZEY) robot.setYon(Robot.YON.DOGU);
        else if (robot.getYon() == Robot.YON.DOGU) robot.setYon(Robot.YON.GUNEY);
        else if (robot.getYon() == Robot.YON.GUNEY) robot.setYon(Robot.YON.BATI);
        else if (robot.getYon() == Robot.YON.BATI) robot.setYon(Robot.YON.KUZEY);
    }

    // Robotu olduğu yerde 90 derece Sola döndürür
    private void solaDon() {
        if (robot.getYon() == Robot.YON.KUZEY) robot.setYon(Robot.YON.BATI);
        else if (robot.getYon() == Robot.YON.BATI) robot.setYon(Robot.YON.GUNEY);
        else if (robot.getYon() == Robot.YON.GUNEY) robot.setYon(Robot.YON.DOGU);
        else if (robot.getYon() == Robot.YON.DOGU) robot.setYon(Robot.YON.KUZEY);
    }
    private void yeriTemizle() {
        int rx = robot.getX();
        int ry = robot.getY();
        Rectangle bulunduguKare = hucreler[ry][rx];
        Color suAnkiRenk = (Color) bulunduguKare.getFill();

        // Eğer renk Beyaz (Temiz) veya Siyah (Duvar) DEĞİLSE, orayı beyaza boyayıp sayacı artır
        if (!suAnkiRenk.equals(Color.web("#ffffff")) && !suAnkiRenk.equals(Color.web("#2d3436"))) {
            bulunduguKare.setFill(Color.web("#ffffff"));
            temizlenenKareSayisi++;
        }
    }

    // BUTON TIKLAMA AKSİYONLARI

    @FXML
    public void baslatTiklandi() {
        RadioButton secilenButon = (RadioButton) algoGroup.getSelectedToggle();
        String secilenAlgoritma = secilenButon.getText();

        oyunDongusu.play();
        System.out.println("Simülasyon BAŞLATILDI. Algoritma: " + secilenAlgoritma);
    }

    @FXML
    public void duraklatTiklandi() {
        oyunDongusu.pause();
        System.out.println("Simülasyon DURAKLATILDI.");
    }

    @FXML
    public void sifirlaTiklandi() {
        oyunDongusu.stop();
        robot.fullSajYap();
        robot.setX(0);
        robot.setY(0);
        gecenSaniye = 0;        // Süreyi de sıfırla
        temizlenenKareSayisi = 0;
        // Tahtayı tamamen eski zemin rengine çevir
        for (int i = 0; i < SATIR_SAYISI; i++) {
            for (int j = 0; j < SUTUN_SAYISI; j++) {
                hucreler[i][j].setFill(Color.web("#faebd7"));
            }
        }
        // Sıfırlanınca görsel robotu da (0,0) noktasına (sol üste) geri ışınla
        gorselRobot.setCenterX((0 * HUCRE_BOYUTU) + (HUCRE_BOYUTU / 2));
        gorselRobot.setCenterY((0 * HUCRE_BOYUTU) + (HUCRE_BOYUTU / 2));
        // Sıfırlayınca hız motorunu da normal (Slider 50'deyken) ayarlarına geri çek
        sldHiz.setValue(50.0);
        if (oyunDongusu != null) {
            oyunDongusu.setRate(5.0); // 50 / 10.0 = 5.0 başlangıç hızı
        }

        ekraniGuncelle();
        System.out.println("Simülasyon SIFIRLANDI.");
    }

    @FXML
    public void istasyonaDonTiklandi() { System.out.println("İstasyona dönülüyor."); }
    @FXML
    public void kirEkleTiklandi() { aktifMod = EditModu.KIR_EKLE; }
    @FXML
    public void mobilyaEkleTiklandi() { aktifMod = EditModu.MOBILYA_EKLE; }
}
