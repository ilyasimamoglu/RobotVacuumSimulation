package vacuumsim.model;

import javafx.scene.paint.Color;

public class Liquid extends Dirt {
    public Liquid() {
        // Renk: Mavi, Temizlenme Süresi: 2 saniye, Ekstra Batarya: 3 birim
        super(Color.web("#74b9ff"), 2, 3);
    }
}