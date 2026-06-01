package vacuumsim.model;

import javafx.scene.paint.Color;

public class Dust extends Dirt {
    public Dust() {
        // Renk: Gri, Temizlenme Süresi: 1 saniye, Ekstra Batarya: 1 birim
        super(Color.web("#b2bec3"), 1, 1);
    }
}