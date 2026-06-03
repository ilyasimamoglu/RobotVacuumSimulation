package vacuumsim.controller;

import vacuumsim.model.Robot;
import vacuumsim.model.Room;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * ============================================================================
 * SINIF: Pathfinder (Yön Bulucu ve Yapay Zeka)
 * GÖREVİ: Robotun beyni olarak çalışır. Haritayı okur, algoritmaları işletir
 * ve BFS ile en kısa eve dönüş rotasını çizer.
 * MVC Uyum: Arayüz renklerine değil, doğrudan Room modeline bağlıdır.
 * ============================================================================
 */
public class Pathfinder {

    private Robot robot;
    private Room oda;
    private int donusSayici = 0; // Spiral sıkışmasını önlemek için dönüş sayacı

    public Pathfinder(Robot robot, Room oda) {
        this.robot = robot;
        this.oda = oda;
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
        // Önümüz açık ve henüz temizlenmemiş/ziyaret edilmemiş bir zeminse ilerle
        if (onumAcikMi() && onumdekiKareZiyaretEdilmediMi()) {
            ileriGit();
            donusSayici = 0; // Başarıyla ilerlediğimiz için sayacı sıfırla
        } else {
            sagaDon();
            donusSayici++;

            // Eğer 4 kez üst üste döndüysek, etrafımızdaki tüm yollar ya duvar ya da zaten temizlenmiştir.
            // Sıkışmayı önlemek için geçici olarak rastgele hareket modunu tetikleyip sarmaldan dışarı çıkıyoruz.
            if (donusSayici >= 4) {
                hareketRastgele();
                donusSayici = 0;
            }
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

        int satirSayisi = oda.getSatirSayisi();
        int sutunSayisi = oda.getSutunSayisi();

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
                    Room.HucreTuru tur = oda.getHucreTuru(yeniX, yeniY);

                    // Orası mobilya (ENGEL) DEĞİLSE listeye ekle
                    if (!ziyaretEdildi[yeniY][yeniX] && tur != Room.HucreTuru.ENGEL) {
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

    /**
     * Robotun bulunduğu konumdan, odadaki en yakın henüz temizlenmemiş/ziyaret edilmemiş
     * hücreye giden en kısa rotayı bulur.
     */
    public List<int[]> bfsEnYakinTemizlenmemisBul() {
        int baslangicX = robot.getX();
        int baslangicY = robot.getY();

        int satirSayisi = oda.getSatirSayisi();
        int sutunSayisi = oda.getSutunSayisi();

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

            // Eğer ulaştığımız hücre henüz temizlenmemiş veya kirliyse, hedefimizi bulduk!
            Room.HucreTuru gecerliTur = oda.getHucreTuru(x, y);
            if (gecerliTur == Room.HucreTuru.TEMIZ || gecerliTur == Room.HucreTuru.TOZ ||
                gecerliTur == Room.HucreTuru.SIVI || gecerliTur == Room.HucreTuru.LEKE) {
                gecerliYol.remove(0); // Başlangıç koordinatını rotadan çıkar
                return gecerliYol;
            }

            for (int[] yon : yonler) {
                int yeniX = x + yon[1];
                int yeniY = y + yon[0];

                if (yeniX >= 0 && yeniX < sutunSayisi && yeniY >= 0 && yeniY < satirSayisi) {
                    Room.HucreTuru tur = oda.getHucreTuru(yeniX, yeniY);

                    // Mobilya (ENGEL) olmayan ve ziyaret edilmeyen hücreleri kuyruğa ekle
                    if (!ziyaretEdildi[yeniY][yeniX] && tur != Room.HucreTuru.ENGEL) {
                        ziyaretEdildi[yeniY][yeniX] = true;
                        List<int[]> yeniYol = new ArrayList<>(gecerliYol);
                        yeniYol.add(new int[]{yeniX, yeniY});
                        kuyruk.add(yeniYol);
                    }
                }
            }
        }
        return new ArrayList<>(); // Tüm oda tamamen temizlendiyse boş liste döner
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

        if (hX < 0 || hX >= oda.getSutunSayisi() || hY < 0 || hY >= oda.getSatirSayisi()) return false;
        return oda.getHucreTuru(hX, hY) != Room.HucreTuru.ENGEL;
    }

    private boolean sagimAcikMi() {
        int hX = robot.getX(), hY = robot.getY();

        if (robot.getYon() == Robot.YON.KUZEY) hX++;
        else if (robot.getYon() == Robot.YON.GUNEY) hX--;
        else if (robot.getYon() == Robot.YON.DOGU) hY++;
        else if (robot.getYon() == Robot.YON.BATI) hY--;

        if (hX < 0 || hX >= oda.getSutunSayisi() || hY < 0 || hY >= oda.getSatirSayisi()) return false;
        return oda.getHucreTuru(hX, hY) != Room.HucreTuru.ENGEL;
    }

    private boolean onumdekiKareZiyaretEdilmediMi() {
        int[] hedef = hedefKoordinatiBul();
        if (hedef[0] >= 0 && hedef[0] < oda.getSutunSayisi() && hedef[1] >= 0 && hedef[1] < oda.getSatirSayisi()) {
            Room.HucreTuru tur = oda.getHucreTuru(hedef[0], hedef[1]);
            // Ziyaret edilmemiş / kirli hücreleri temiz kabul etmeyip buralara girmesini sağlıyoruz
            return tur == Room.HucreTuru.TEMIZ || tur == Room.HucreTuru.TOZ || tur == Room.HucreTuru.SIVI || tur == Room.HucreTuru.LEKE;
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