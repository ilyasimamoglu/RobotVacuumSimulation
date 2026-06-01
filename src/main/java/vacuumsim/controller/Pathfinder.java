package vacuumsim.controller;

import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import vacuumsim.model.Robot;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * ============================================================================
 * SINIF: Pathfinder (Yön Bulucu ve Yapay Zeka)
 * GÖREVİ: Robotun beyni olarak çalışır. Haritayı okur, algoritmaları işletir
 * ve BFS ile en kısa eve dönüş rotasını çizer.
 * ============================================================================
 */
public class Pathfinder {

    private Robot robot;
    private Rectangle[][] hucreler;
    private int sutunSayisi;
    private int satirSayisi;

    public Pathfinder(Robot robot, Rectangle[][] hucreler, int sutunSayisi, int satirSayisi) {
        this.robot = robot;
        this.hucreler = hucreler;
        this.sutunSayisi = sutunSayisi;
        this.satirSayisi = satirSayisi;
    }

    // =========================================================================
    // YAPAY ZEKA ALGORİTMALARI
    // =========================================================================

    public void hareketRastgele() {
        if (onumAcikMi()) {
            ileriGit();
        } else {
            if (Math.random() > 0.5) sagaDon();
            else solaDon();
        }
    }

    public void hareketSpiral() {
        if (onumAcikMi() && !onumdekiKareTemizMi()) {
            ileriGit();
        } else {
            sagaDon();
        }
    }

    public void hareketDuvarTakibi() {
        if (sagimAcikMi()) {
            sagaDon();
            ileriGit();
        } else if (onumAcikMi()) {
            ileriGit();
        } else {
            solaDon();
        }
    }

    // =========================================================================
    // BFS (BREADTH-FIRST SEARCH) EN KISA YOL ALGORİTMASI
    // =========================================================================

    /**
     * Robotun bulunduğu konumdan (0,0) noktasına duvarlara çarpmadan giden en kısa rotayı bulur.
     */
    public List<int[]> bfsIstasyonaDonRotaBul() {
        int baslangicX = robot.getX();
        int baslangicY = robot.getY();
        int hedefX = 0; // İstasyon X
        int hedefY = 0; // İstasyon Y

        // Yönler: Yukarı, Aşağı, Sağa, Sola
        int[][] yonler = {{-1, 0}, {1, 0}, {0, 1}, {0, -1}};

        boolean[][] ziyaretEdildi = new boolean[satirSayisi][sutunSayisi];
        Queue<List<int[]>> kuyruk = new LinkedList<>();

        List<int[]> baslangicYolu = new ArrayList<>();
        baslangicYolu.add(new int[]{baslangicX, baslangicY});
        kuyruk.add(baslangicYolu);
        ziyaretEdildi[baslangicY][baslangicX] = true;

        while (!kuyruk.isEmpty()) {
            List<int[]> gecerliYol = kuyruk.poll();
            int[] sonKonum = gecerliYol.get(gecerliYol.size() - 1);
            int x = sonKonum[0];
            int y = sonKonum[1];

            // Hedefe ulaştıysak yolu geri döndür
            if (x == hedefX && y == hedefY) {
                gecerliYol.remove(0); // Bulunduğumuz noktayı rotadan çıkar
                return gecerliYol;
            }

            // 4 yöne bakarak dalga dalga yayıl
            for (int[] yon : yonler) {
                int yeniX = x + yon[1];
                int yeniY = y + yon[0];

                if (yeniX >= 0 && yeniX < sutunSayisi && yeniY >= 0 && yeniY < satirSayisi) {
                    Color renk = (Color) hucreler[yeniY][yeniX].getFill();

                    // Orası mobilya (#2d3436) DEĞİLSE listeye ekle
                    if (!ziyaretEdildi[yeniY][yeniX] && !renk.equals(Color.web("#2d3436"))) {
                        ziyaretEdildi[yeniY][yeniX] = true;
                        List<int[]> yeniYol = new ArrayList<>(gecerliYol);
                        yeniYol.add(new int[]{yeniX, yeniY});
                        kuyruk.add(yeniYol);
                    }
                }
            }
        }
        return new ArrayList<>(); // Etrafı tamamen kapalıysa boş döner
    }

    // =========================================================================
    // SENSÖRLER VE FİZİKSEL YARDIMCI METOTLAR
    // =========================================================================

    private int[] hedefKoordinatiBul() {
        int hedefX = robot.getX();
        int hedefY = robot.getY();
        if (robot.getYon() == Robot.YON.KUZEY) hedefY--;
        else if (robot.getYon() == Robot.YON.GUNEY) hedefY++;
        else if (robot.getYon() == Robot.YON.DOGU) hedefX++;
        else if (robot.getYon() == Robot.YON.BATI) hedefX--;
        return new int[]{hedefX, hedefY};
    }

    private boolean onumAcikMi() {
        int[] hedef = hedefKoordinatiBul();
        int hX = hedef[0], hY = hedef[1];

        if (hX < 0 || hX >= sutunSayisi || hY < 0 || hY >= satirSayisi) return false;
        Color renk = (Color) hucreler[hY][hX].getFill();
        if (renk.equals(Color.web("#2d3436"))) return false;

        return true;
    }

    private boolean sagimAcikMi() {
        int hX = robot.getX(), hY = robot.getY();

        if (robot.getYon() == Robot.YON.KUZEY) hX++;
        else if (robot.getYon() == Robot.YON.GUNEY) hX--;
        else if (robot.getYon() == Robot.YON.DOGU) hY++;
        else if (robot.getYon() == Robot.YON.BATI) hY--;

        if (hX < 0 || hX >= sutunSayisi || hY < 0 || hY >= satirSayisi) return false;
        Color renk = (Color) hucreler[hY][hX].getFill();
        if (renk.equals(Color.web("#2d3436"))) return false;

        return true;
    }

    private boolean onumdekiKareTemizMi() {
        int[] hedef = hedefKoordinatiBul();
        if (hedef[0] >= 0 && hedef[0] < sutunSayisi && hedef[1] >= 0 && hedef[1] < satirSayisi) {
            Color renk = (Color) hucreler[hedef[1]][hedef[0]].getFill();
            return renk.equals(Color.web("#ffffff"));
        }
        return false;
    }

    private void ileriGit() {
        int[] hedef = hedefKoordinatiBul();
        robot.setX(hedef[0]);
        robot.setY(hedef[1]);
    }

    private void sagaDon() {
        if (robot.getYon() == Robot.YON.KUZEY) robot.setYon(Robot.YON.DOGU);
        else if (robot.getYon() == Robot.YON.DOGU) robot.setYon(Robot.YON.GUNEY);
        else if (robot.getYon() == Robot.YON.GUNEY) robot.setYon(Robot.YON.BATI);
        else if (robot.getYon() == Robot.YON.BATI) robot.setYon(Robot.YON.KUZEY);
    }

    private void solaDon() {
        if (robot.getYon() == Robot.YON.KUZEY) robot.setYon(Robot.YON.BATI);
        else if (robot.getYon() == Robot.YON.BATI) robot.setYon(Robot.YON.GUNEY);
        else if (robot.getYon() == Robot.YON.GUNEY) robot.setYon(Robot.YON.DOGU);
        else if (robot.getYon() == Robot.YON.DOGU) robot.setYon(Robot.YON.KUZEY);
    }
}