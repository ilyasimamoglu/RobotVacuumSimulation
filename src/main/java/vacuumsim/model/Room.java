package vacuumsim.model;

/**
 * Odanın fiziksel boyutlarını, hücre durumlarını ve toplam alanını tutan Model sınıfı.
 */
public class Room {
    
    // Hücre durumlarını temsil eden Enum. Arayüzden bağımsız olarak modelin durumunu tanımlar.
    public enum HucreTuru {
        TEMIZ,          // Temiz zemin (başlangıç durumu - antik beyaz)
        TOZ,            // Tozlu zemin
        SIVI,           // Sıvı dökülmüş zemin
        LEKE,           // Lekeli zemin
        ENGEL,          // Mobilya/duvar engeli
        SARJ_ISTASYONU, // Şarj istasyonu
        TEMIZLENDI      // Süpürge tarafından temizlenmiş/ziyaret edilmiş alan (beyaz)
    }

    private int sutunSayisi; // Genişlik (X ekseni)
    private int satirSayisi; // Yükseklik (Y ekseni)
    private HucreTuru[][] izgara; // Hücrelerin durumunu tutan matris
    private HucreTuru[][] baslangicIzgarasi; // Orijinal yerleşimi korumak için yedek matris

    public Room(int sutunSayisi, int satirSayisi) {
        this.sutunSayisi = sutunSayisi;
        this.satirSayisi = satirSayisi;
        this.izgara = new HucreTuru[satirSayisi][sutunSayisi];
        this.baslangicIzgarasi = new HucreTuru[satirSayisi][sutunSayisi];
        
        // Izgarayı başlangıçta temiz zeminle dolduruyoruz
        for (int satir = 0; satir < satirSayisi; satir++) {
            for (int sutun = 0; sutun < sutunSayisi; sutun++) {
                izgara[satir][sutun] = HucreTuru.TEMIZ;
                baslangicIzgarasi[satir][sutun] = HucreTuru.TEMIZ;
            }
        }
        // Şarj istasyonunu başlangıç konumu (0, 0) olarak ayarlıyoruz
        izgara[0][0] = HucreTuru.SARJ_ISTASYONU;
        baslangicIzgarasi[0][0] = HucreTuru.SARJ_ISTASYONU;
    }

    public int getSutunSayisi() { return sutunSayisi; }
    public int getSatirSayisi() { return satirSayisi; }

    // Toplam metrekareyi (kare sayısını) otomatik hesaplar
    public int getToplamAlan() { return sutunSayisi * satirSayisi; }

    // Belirli bir koordinattaki hücre türünü döner (X: sutun, Y: satir)
    public HucreTuru getHucreTuru(int x, int y) {
        if (x >= 0 && x < sutunSayisi && y >= 0 && y < satirSayisi) {
            return izgara[y][x];
        }
        return HucreTuru.ENGEL; // Harita dışını engel olarak kabul ediyoruz
    }

    // Belirli bir koordinattaki hücre türünü günceller (X: sutun, Y: satir)
    public void setHucreTuru(int x, int y, HucreTuru tur) {
        if (x >= 0 && x < sutunSayisi && y >= 0 && y < satirSayisi) {
            izgara[y][x] = tur;
            // Robotun bıraktığı temizlik izleri (TEMIZLENDI) hariç,
            // elle yerleştirilen kir ve engelleri başlangıç durumuna yedekliyoruz.
            if (tur != HucreTuru.TEMIZLENDI) {
                baslangicIzgarasi[y][x] = tur;
            }
        }
    }

    // Tüm hücre durumlarını başlangıçtaki temiz haline döndürür (Haritayı tamamen boşaltır)
    public void sifirla() {
        for (int satir = 0; satir < satirSayisi; satir++) {
            for (int sutun = 0; sutun < sutunSayisi; sutun++) {
                izgara[satir][sutun] = HucreTuru.TEMIZ;
                baslangicIzgarasi[satir][sutun] = HucreTuru.TEMIZ;
            }
        }
        izgara[0][0] = HucreTuru.SARJ_ISTASYONU; // Şarj istasyonu yerinde kalır
        baslangicIzgarasi[0][0] = HucreTuru.SARJ_ISTASYONU;
    }

    // Sadece süpürülmüş yolları sıfırlar, kirleri ve mobilyaları/engelleri korur (Orijinal senaryoyu geri yükler)
    public void rotayiSifirla() {
        for (int satir = 0; satir < satirSayisi; satir++) {
            for (int sutun = 0; sutun < sutunSayisi; sutun++) {
                izgara[satir][sutun] = baslangicIzgarasi[satir][sutun];
            }
        }
        izgara[0][0] = HucreTuru.SARJ_ISTASYONU;
    }

    // Sadece yerdeki tüm kirleri (Toz, Sıvı, Leke) kaldırır. Mobilyaları (engelleri) korur.
    public void kirleriTemizle() {
        for (int satir = 0; satir < satirSayisi; satir++) {
            for (int sutun = 0; sutun < sutunSayisi; sutun++) {
                HucreTuru tur = izgara[satir][sutun];
                if (tur == HucreTuru.TOZ || tur == HucreTuru.SIVI || tur == HucreTuru.LEKE) {
                    izgara[satir][sutun] = HucreTuru.TEMIZ;
                }
                
                HucreTuru baslangicTur = baslangicIzgarasi[satir][sutun];
                if (baslangicTur == HucreTuru.TOZ || baslangicTur == HucreTuru.SIVI || baslangicTur == HucreTuru.LEKE) {
                    baslangicIzgarasi[satir][sutun] = HucreTuru.TEMIZ;
                }
            }
        }
    }
}