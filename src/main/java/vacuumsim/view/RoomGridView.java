package vacuumsim.view;

import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Line;
import vacuumsim.model.Robot;
import vacuumsim.model.Room;

import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================================
 * SINIF: RoomGridView (Görünüm / View Katmanı)
 * GÖREVİ: MVC mimarisindeki "V" (View) katmanıdır. Sadece ekrana çizim yapar.
 * Izgarayı (matrisi) oluşturur, hücreleri boyar ve robotu sahnede kaydırır.
 * MVC Uyum: Artık renkleri doğrudan Room modeline göre belirler.
 * ============================================================================
 */
public class RoomGridView {

    private Pane oyunAlani;
    private Room oda;
    private double hucreBoyutu;
    private Rectangle[][] hucreler;
    private Circle gorselRobot;

    // Robotun arkasında bırakacağı hareket izi (path/trail) için değişkenler
    private List<Line> hareketIzleri = new ArrayList<>();
    private double sonRobotX = -1;
    private double sonRobotY = -1;

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
        kare.setStroke(Color.web("#1a202c")); // Koyu sınır çizgileri
        kare.setX(sutun * hucreBoyutu);
        kare.setY(satir * hucreBoyutu);

        hucreler[satir][sutun] = kare;
        oyunAlani.getChildren().add(kare);

        // Başlangıç rengini modeldeki hücre türüne göre çiziyoruz (Örn: şarj istasyonu)
        hucreYenile(satir, sutun, oda.getHucreTuru(sutun, satir));

        return kare;
    }

    // Modelden gelen hücre türüne göre arayüzdeki kareyi boyar
    public void hucreYenile(int satir, int sutun, Room.HucreTuru tur) {
        Rectangle kare = hucreler[satir][sutun];
        switch (tur) {
            case TEMIZ:
                kare.setFill(Color.web("#1e2530")); // Koyu lacivert/siyah (Süpürülmemiş)
                break;
            case TEMIZLENDI:
                kare.setFill(Color.web("#2d3748")); // Slate Gri/Mavi (Süpürülmüş)
                break;
            case TOZ:
                kare.setFill(Color.web("#57606f")); // Koyu Toz Rengi
                break;
            case SIVI:
                kare.setFill(Color.web("#0984e3")); // Canlı Neon Mavi
                break;
            case LEKE:
                kare.setFill(Color.web("#6c5ce7")); // Canlı Mor
                break;
            case ENGEL:
                kare.setFill(Color.web("#0c0f14")); // Çok koyu antrasit mobilya
                break;
            case SARJ_ISTASYONU:
                kare.setFill(Color.web("#00b894")); // Neon Turkuaz Şarj İstasyonu
                break;
        }
    }

    // Tüm ızgarayı modelin güncel durumuna göre yeniden çizer
    public void tumTahtayiGuncelle() {
        for (int satir = 0; satir < oda.getSatirSayisi(); satir++) {
            for (int sutun = 0; sutun < oda.getSutunSayisi(); sutun++) {
                hucreYenile(satir, sutun, oda.getHucreTuru(sutun, satir));
            }
        }
    }

    public void robotuCiz(Robot robot) {
        gorselRobot = new Circle((hucreBoyutu / 2) - 4);
        gorselRobot.setFill(Color.web("#e17055")); // Turuncu robot gövdesi
        gorselRobot.setStroke(Color.web("#ffffff")); // Beyaz parlama halkası
        gorselRobot.setStrokeWidth(2);

        sonRobotX = robot.getX();
        sonRobotY = robot.getY();

        robotuGuncelle(robot);
        oyunAlani.getChildren().add(gorselRobot);
    }

    public void robotuGuncelle(Robot robot) {
        double yeniCenterX = (robot.getX() * hucreBoyutu) + (hucreBoyutu / 2);
        double yeniCenterY = (robot.getY() * hucreBoyutu) + (hucreBoyutu / 2);

        // Eğer robot hareket ettiyse arkasında camgöbeği lazer bir iz bırak
        if (sonRobotX != -1 && sonRobotY != -1 && (sonRobotX != robot.getX() || sonRobotY != robot.getY())) {
            double eskiCenterX = (sonRobotX * hucreBoyutu) + (hucreBoyutu / 2);
            double eskiCenterY = (sonRobotY * hucreBoyutu) + (hucreBoyutu / 2);

            Line iz = new Line(eskiCenterX, eskiCenterY, yeniCenterX, yeniCenterY);
            iz.setStroke(Color.web("#00cec9")); // Neon Camgöbeği lazer izi
            iz.setStrokeWidth(2.5);
            iz.getStrokeDashArray().addAll(6.0, 6.0); // Kesikli çizgi
            iz.setOpacity(0.8); // Parlama hissi için opaklık artırıldı

            hareketIzleri.add(iz);

            // İz çizgisini robotun arkasında kalacak şekilde ekliyoruz
            int robotIndex = oyunAlani.getChildren().indexOf(gorselRobot);
            if (robotIndex != -1) {
                oyunAlani.getChildren().add(robotIndex, iz);
            } else {
                oyunAlani.getChildren().add(iz);
            }
        }

        sonRobotX = robot.getX();
        sonRobotY = robot.getY();

        gorselRobot.setCenterX(yeniCenterX);
        gorselRobot.setCenterY(yeniCenterY);
    }

    public void izleriTemizle() {
        for (Line iz : hareketIzleri) {
            oyunAlani.getChildren().remove(iz);
        }
        hareketIzleri.clear();
        sonRobotX = -1;
        sonRobotY = -1;
    }

    public void tahtayiSifirla() {
        izleriTemizle();
        tumTahtayiGuncelle();
    }

    public Rectangle[][] getHucreler() {
        return hucreler;
    }
}