package vacuumsim.view;

import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import vacuumsim.model.Robot;
import vacuumsim.model.Room;

/**
 * ============================================================================
 * SINIF: RoomGridView (Görünüm / View Katmanı)
 * GÖREVİ: MVC mimarisindeki "V" (View) katmanıdır. Sadece ekrana çizim yapar.
 * Izgarayı (matrisi) oluşturur, hücreleri boyar ve robotu sahnede kaydırır.
 * İçerisinde hiçbir mantıksal karar, oyun döngüsü veya hesaplama barındırmaz.
 * Uygulamanın sadece "Ressam" görevini üstlenir.
 * ============================================================================
 */
public class RoomGridView {

    private Pane oyunAlani;
    private Room oda;
    private double hucreBoyutu;
    private Rectangle[][] hucreler;
    private Circle gorselRobot;

    public RoomGridView(Pane oyunAlani, Room oda, double hucreBoyutu) {
        this.oyunAlani = oyunAlani;
        this.oda = oda;
        this.hucreBoyutu = hucreBoyutu;
        this.hucreler = new Rectangle[oda.getSatirSayisi()][oda.getSutunSayisi()];

        double tamGenislik = oda.getSutunSayisi() * hucreBoyutu;
        double tamYukseklik = oda.getSatirSayisi() * hucreBoyutu;
        oyunAlani.setPrefSize(tamGenislik, tamYukseklik);
        oyunAlani.setMinSize(tamGenislik, tamYukseklik);
        oyunAlani.setMaxSize(tamGenislik, tamYukseklik);
    }

    public Rectangle hucreOlustur(int satir, int sutun) {
        Rectangle kare = new Rectangle(hucreBoyutu, hucreBoyutu);
        kare.setFill(Color.web("#faebd7"));
        kare.setStroke(Color.LIGHTGRAY);
        kare.setX(sutun * hucreBoyutu);
        kare.setY(satir * hucreBoyutu);

        hucreler[satir][sutun] = kare;
        oyunAlani.getChildren().add(kare);
        return kare;
    }

    public void robotuCiz(Robot robot) {
        gorselRobot = new Circle((hucreBoyutu / 2) - 4);
        gorselRobot.setFill(Color.web("#e17055"));
        gorselRobot.setStroke(Color.web("#2d3436"));
        gorselRobot.setStrokeWidth(2);

        robotuGuncelle(robot);
        oyunAlani.getChildren().add(gorselRobot);
    }

    public void robotuGuncelle(Robot robot) {
        gorselRobot.setCenterX((robot.getX() * hucreBoyutu) + (hucreBoyutu / 2));
        gorselRobot.setCenterY((robot.getY() * hucreBoyutu) + (hucreBoyutu / 2));
    }

    public void tahtayiSifirla() {
        for (int i = 0; i < oda.getSatirSayisi(); i++) {
            for (int j = 0; j < oda.getSutunSayisi(); j++) {
                hucreler[i][j].setFill(Color.web("#faebd7"));
            }
        }
    }

    public Rectangle[][] getHucreler() {
        return hucreler;
    }
}