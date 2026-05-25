package vacuumsim.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;

//Scene Builder'da hazırlayacağımız arayüzdeki butonlara ("Başlat", "Duraklat" vb.)
// basıldığında çalışacak ana köprü sınıfımızdır.
public class SimulationController {

    // ARAÇLAR Elmanları
    @FXML private ComboBox <String> cmbKirTuru;
    @FXML private Button btnKirEkle;
    @FXML private  Button btnMobilyaEkle;
    // ROBOT Hızı
    @FXML private Slider sldHiz;
    // Temizlik Algoritması
    // Togglegroup bizi if else hamalığından kurtartır sürekli hangi algoritma seçildi diye kontrol etmemize gerek kalmaz
    @FXML private ToggleGroup algoGroup;
    // Robot Durum Elmanları
    @FXML private Label lblYon;
    @FXML private Label lblBatarya;
    @FXML private Label lblKonum;
    // Kontrol Elmanları
    @FXML private Button btnBaslat;
    @FXML private Button btnDuraklat;
    @FXML private Button btnSifirla;
    @FXML private Button btnIstasyonaDon;
    // oyun alanı "pane"
    @FXML private Pane ortaOyunAlani;

    //ortaOyunAlani (grid) 2'D matris ile tasarlıyoruz
    private final int SUTUN_SAYISI = 25;        // X ekseni: Soldan sağa kaç adet kare olacağı
    private final int SATIR_SAYISI = 19;        // Y ekseni: Yukarıdan aşağıya kaç adet kare olacağı
    private final double HUCRE_BOYUTU = 35.0;   // Ekrandaki kare boyutu
    private javafx.scene.shape.Rectangle[][] hucreler;   // Bu bizim 2 Boyutlu Matrisimiz

    // ortaoyunAlani kareleri oluşturduğumuz metot
    private void PaneGrid(){
        hucreler = new javafx.scene.shape.Rectangle[SATIR_SAYISI][SUTUN_SAYISI]; // 15 satırlık 20 stunluk bir yer ayırdk
        for (int satir = 0; satir < SATIR_SAYISI; satir++) //satırları gezmek için
        {
            for (int sutun = 0; sutun < SUTUN_SAYISI; sutun++) // sütünleri gezmek için
            {
                // karenin genişliği ve yüksekliği
                javafx.scene.shape.Rectangle kare = new javafx.scene.shape.Rectangle(HUCRE_BOYUTU, HUCRE_BOYUTU);
                kare.setFill(javafx.scene.paint.Color.web("#faebd7")); // karenin rengini ayarlmak için
                kare.setStroke(javafx.scene.paint.Color.LIGHTGRAY); // karenin kenarlarının gözükmesi için
                kare.setX(sutun * HUCRE_BOYUTU); //burası önemli kareleri yanyana çizmek için sütün ile hücreyi çarpıyoruz
                kare.setY(satir * HUCRE_BOYUTU);  //burası önemli kareleri alt alta çizmek için satir ile hücreyi çarpıyoruz
                hucreler[satir][sutun] = kare; // oluşan kareyi saklıyoruz
                ortaOyunAlani.getChildren().add(kare); //oluşturduğumuz kareyi getchildrene ekliyoruzki ekranda gözüksün karemiz

            }

            }
            }
         // ÖNEMLİ: Bu metodun adı KESİNLİKLE değiştirilemez!
         // JavaFX, arayüzdeki butonları koda bağlama işlemi bittikten hemen sonra
         // otomatik olarak "initialize" isimli metodu arar ve programı buradan başlatır.
         @FXML
         public void initialize() {
        cmbKirTuru.getItems().addAll("Toz", "Sıvı", "Leke");// kir ekle menüye seçenekleri ekliyoruz
        cmbKirTuru.getSelectionModel().selectFirst(); // Varsayılan olarak ilk sıradaki seçili gelsin

        System.out.println("Sistem Hazır");
        // Önemli !
        // initialize metodu arayüz ekranı bilgisayarda ilk yüklendiği an 1 kereye mahsus otomatik çalışır.
        // Kullanıcı daha hiçbir butona basmadan odanın ızgarası ekrana çizilsin diye bu metodu burada tetikliyoruz.
        PaneGrid();
        }

    @FXML
    public void baslatTiklandi() {
        // Hangi algoritmanın seçili olduğunu buluyoruz
        RadioButton secilenButon = (RadioButton) algoGroup.getSelectedToggle(); // burda cast yaptık
        String secilenAlgoritma = secilenButon.getText();
        System.out.println("Simülasyon BAŞLATILDI. Algoritma: " + secilenAlgoritma);
    }

    @FXML
    public void duraklatTiklandi() {
        System.out.println("Simülasyon DURAKLATILDI.");
    }

    @FXML
    public void sifirlaTiklandi() {
        System.out.println("Simülasyon SIFIRLANDI.");
    }

    @FXML
    public void istasyonaDonTiklandi() {
        System.out.println("Robot şarj istasyonuna ÇAĞRILDI.");
    }

    @FXML
    public void kirEkleTiklandi() {
        System.out.println("Odaya eklenecek kir: " + cmbKirTuru.getValue());
    }

    @FXML
    public void mobilyaEkleTiklandi() {
        System.out.println("Mobilya ekleme modu aktif.");
    }
}



