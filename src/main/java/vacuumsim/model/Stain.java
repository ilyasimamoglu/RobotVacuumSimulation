package vacuumsim.model;

import javafx.scene.paint.Color;

public class Stain extends Dirt {
    public Stain() {
        // Renk: Mor, Temizlenme Süresi: 4 saniye, Ekstra Batarya: 5 birim
        super(Color.web("#a29bfe"), 4, 5);
    }
}