package vacuumsim.model;

import javafx.scene.paint.Color;

public class Obstacle {
    private Color renk;

    public Obstacle() {
        // Renk: Koyu Gri / Siyah (Duvar Rengi)
        this.renk = Color.web("#2d3436");
    }

    public Color getRenk() {
        return renk;
    }
}