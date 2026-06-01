package vacuumsim.model;

import javafx.scene.paint.Color;

/**
 * Tüm kir tipleri için Ana (Base) Sınıf.
 * OOP'nin Kalıtım (Inheritance) özelliğini kullanıyoruz.
 */
public abstract class Dirt {
    protected Color renk;             // Arayüzde görünecek renk
    protected int temizlenmeSuresi;   // Kaç saniyede temizleneceği
    protected int ekstraBataryaTuketimi; // Temizlerken harcayacağı fazladan batarya

    public Dirt(Color renk, int temizlenmeSuresi, int ekstraBataryaTuketimi) {
        this.renk = renk;
        this.temizlenmeSuresi = temizlenmeSuresi;
        this.ekstraBataryaTuketimi = ekstraBataryaTuketimi;
    }

    public Color getRenk() { return renk; }
    public int getTemizlenmeSuresi() { return temizlenmeSuresi; }
    public int getEkstraBataryaTuketimi() { return ekstraBataryaTuketimi; }
}