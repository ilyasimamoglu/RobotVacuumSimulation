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

    public boolean odadaTemizlenmemisAlanVarMi() {
        for (int y = 0; y < satirSayisi; y++) {
            for (int x = 0; x < sutunSayisi; x++) {
                HucreTuru tur = izgara[y][x];
                if (tur == HucreTuru.TEMIZ || tur == HucreTuru.TOZ || 
                    tur == HucreTuru.SIVI || tur == HucreTuru.LEKE) {
                    return true;
                }
            }
        }
        return false;
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

    /**
     * Önceden tanımlanmış hazır oda düzenlerini (Layouts) haritaya yükler.
     */
    public void odaDuzeniniYukle(String duzenIsmi) {
        sifirla(); // Önce haritayı tamamen boşaltıp temiz hale getiriyoruz
        
        if (duzenIsmi.equals("Oturma Odası")) {
            // TV Ünitesi (Üst tarafta)
            for (int x = 6; x <= 14; x++) {
                setHucreTuru(x, 1, HucreTuru.ENGEL);
            }
            // Yemek Masası ve Sandalyeler (Sol tarafta)
            for (int x = 2; x <= 4; x++) {
                for (int y = 3; y <= 5; y++) {
                    setHucreTuru(x, y, HucreTuru.ENGEL);
                }
            }
            // L Şeklinde Büyük Koltuk
            for (int x = 9; x <= 15; x++) {
                setHucreTuru(x, 10, HucreTuru.ENGEL);
            }
            for (int y = 11; y <= 14; y++) {
                setHucreTuru(15, y, HucreTuru.ENGEL);
            }
            // Orta Sehpa
            setHucreTuru(12, 12, HucreTuru.ENGEL);
            setHucreTuru(12, 13, HucreTuru.ENGEL);

            // Bölgesel Kirlerin Dağıtılması
            // Orta sehpa ve koltuk çevresi tozlar
            setHucreTuru(11, 12, HucreTuru.TOZ);
            setHucreTuru(11, 13, HucreTuru.TOZ);
            setHucreTuru(13, 12, HucreTuru.TOZ);
            setHucreTuru(13, 13, HucreTuru.TOZ);
            // Yemek masası altındaki yemek lekeleri ve sıvı döküntüleri
            setHucreTuru(2, 2, HucreTuru.LEKE);
            setHucreTuru(3, 2, HucreTuru.SIVI);
            setHucreTuru(4, 2, HucreTuru.LEKE);
            setHucreTuru(5, 3, HucreTuru.TOZ);
            setHucreTuru(5, 4, HucreTuru.TOZ);
            // Odanın kuytu köşelerindeki toz birikintileri
            setHucreTuru(20, 1, HucreTuru.TOZ);
            setHucreTuru(20, 2, HucreTuru.TOZ);
            setHucreTuru(20, 16, HucreTuru.TOZ);
            setHucreTuru(19, 16, HucreTuru.TOZ);
            setHucreTuru(1, 16, HucreTuru.TOZ);
            
        } else if (duzenIsmi.equals("Çok Odalı Daire")) {
            // Bölme Duvarlar (Odayı 3 bağımsız odaya ve koridorlara böler)
            // Dikey Duvar 1 (Sol Oda Bölmesi) - y=5'te kapı boşluğu var
            for (int y = 0; y <= 12; y++) {
                if (y != 5) {
                    setHucreTuru(7, y, HucreTuru.ENGEL);
                }
            }
            // Dikey Duvar 2 (Sağ Oda Bölmesi) - y=10'da kapı boşluğu var
            for (int y = 5; y <= 17; y++) {
                if (y != 10) {
                    setHucreTuru(14, y, HucreTuru.ENGEL);
                }
            }
            // Yatay Duvar (Sol Alt Oda Bölmesi) - x=3'te kapı boşluğu var
            for (int x = 0; x <= 7; x++) {
                if (x != 3) {
                    setHucreTuru(x, 12, HucreTuru.ENGEL);
                }
            }

            // Odaların İçindeki Mobilyalar
            // Oda 1 (Sol Üst - Yatak Odası dolap ve yatak)
            setHucreTuru(1, 2, HucreTuru.ENGEL);
            setHucreTuru(2, 2, HucreTuru.ENGEL);
            setHucreTuru(1, 3, HucreTuru.ENGEL);
            setHucreTuru(2, 3, HucreTuru.ENGEL);
            
            // Oda 2 (Sol Alt - Mutfak tezgahı)
            setHucreTuru(1, 15, HucreTuru.ENGEL);
            setHucreTuru(2, 15, HucreTuru.ENGEL);
            
            // Oda 3 (Sağ - Oturma Odası kanepesi)
            setHucreTuru(18, 5, HucreTuru.ENGEL);
            setHucreTuru(18, 6, HucreTuru.ENGEL);
            setHucreTuru(18, 7, HucreTuru.ENGEL);

            // Odalardaki Kirlerin Dağılımı
            setHucreTuru(3, 4, HucreTuru.TOZ);
            setHucreTuru(4, 5, HucreTuru.TOZ);
            setHucreTuru(2, 14, HucreTuru.SIVI); // Banyoda/mutfakta sıvı döküntüsü
            setHucreTuru(3, 15, HucreTuru.LEKE);
            setHucreTuru(10, 2, HucreTuru.TOZ);
            setHucreTuru(11, 3, HucreTuru.TOZ);
            setHucreTuru(17, 6, HucreTuru.TOZ);
            setHucreTuru(16, 7, HucreTuru.LEKE);
            
        } else if (duzenIsmi.equals("Labirent")) {
            // Kıvrımlı labirent duvarları (Robotun yılan gibi kıvrılmasını test etmek için)
            // Engel Sütunu 1 (Üstten sarkan)
            for (int y = 0; y <= 13; y++) {
                setHucreTuru(4, y, HucreTuru.ENGEL);
            }
            // Engel Sütunu 2 (Alttan yükselen)
            for (int y = 4; y <= 17; y++) {
                setHucreTuru(8, y, HucreTuru.ENGEL);
            }
            // Engel Sütunu 3 (Üstten sarkan)
            for (int y = 0; y <= 13; y++) {
                setHucreTuru(12, y, HucreTuru.ENGEL);
            }
            // Engel Sütunu 4 (Alttan yükselen)
            for (int y = 4; y <= 17; y++) {
                setHucreTuru(16, y, HucreTuru.ENGEL);
            }

            // Koridorlar boyunca serpilen kirler
            for (int y = 1; y <= 16; y += 2) {
                setHucreTuru(2, y, HucreTuru.TOZ);
                setHucreTuru(6, y, HucreTuru.TOZ);
                setHucreTuru(10, y, HucreTuru.TOZ);
                setHucreTuru(14, y, HucreTuru.TOZ);
                setHucreTuru(18, y, HucreTuru.TOZ);
            }
            setHucreTuru(6, 4, HucreTuru.SIVI);
            setHucreTuru(10, 13, HucreTuru.LEKE);
        } else if (duzenIsmi.equals("Ulaşılamaz Alan")) {
            // Sağ alt tarafta kapalı bir kutu oluşturuyoruz (izole alan)
            // Yatay duvar
            for (int x = 14; x <= 20; x++) {
                setHucreTuru(x, 9, HucreTuru.ENGEL);
            }
            // Dikey duvar
            for (int y = 9; y <= 17; y++) {
                setHucreTuru(14, y, HucreTuru.ENGEL);
            }
            
            // Kutunun içine kir yerleştir (Ulaşılamaz kirler)
            setHucreTuru(17, 13, HucreTuru.LEKE);
            setHucreTuru(18, 14, HucreTuru.TOZ);
            
            // Kutunun dışındaki serbest alana da normal temizlenebilir kirler yerleştir
            setHucreTuru(5, 5, HucreTuru.TOZ);
            setHucreTuru(8, 12, HucreTuru.SIVI);
            setHucreTuru(12, 4, HucreTuru.TOZ);
            setHucreTuru(2, 10, HucreTuru.LEKE);
        }
    }
}