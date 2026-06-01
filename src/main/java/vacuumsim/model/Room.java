package vacuumsim.model;

/**
 * Odanın fiziksel boyutlarını ve toplam alanını hesaplayan Model sınıfı.
 */
public class Room {
    private int sutunSayisi; // Genişlik (X ekseni)
    private int satirSayisi; // Yükseklik (Y ekseni)

    public Room(int sutunSayisi, int satirSayisi) {
        this.sutunSayisi = sutunSayisi;
        this.satirSayisi = satirSayisi;
    }

    public int getSutunSayisi() { return sutunSayisi; }
    public int getSatirSayisi() { return satirSayisi; }

    // Toplam metrekareyi (kare sayısını) otomatik hesaplar
    public int getToplamAlan() { return sutunSayisi * satirSayisi; }
}