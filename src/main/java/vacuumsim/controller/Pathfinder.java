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
    private boolean zigzagYonYukari = false; // false: aşağı gidiyor (GÜNEY), true: yukarı gidiyor (KUZEY)

    public Pathfinder(Robot robot, Room oda) {
        this.robot = robot;
        this.oda = oda;
    }

    // =========================================================================
    // YAPAY ZEKA TEMİZLİK ALGORİTMALARI
    // =========================================================================
 
    /**
     * 1. RASTGELE NAVİGASYON (Random Walk / Bounce)
     * Çevresini haritalandıramayan temel nesil robot süpürgelerin kullandığı mantıktır.
     * Robot düz bir çizgide ilerler; duvara veya mobilyaya çarptığında rastgele yön
     * (sağa veya sola 90 derece) değiştirerek temizliğe başka bir doğrultuda devam eder.
     */
    public void hareketRastgele() {
        if (onumAcikMi()) {
            ileriGit(); // Önü boşsa düz gitmeye devam et
        } else {
            // Engele çarptığında sağa veya sola rastgele dönerek sekme hareketi yap
            if (Math.random() > 0.5) {
                sagaDon();
            } else {
                solaDon();
            }
        }
    }
 
    /**
     * 2. SARMAL / SPİRAL TEMİZLİK (Spiral / Spot Cleaning)
     * Özellikle dökülen kirlerin olduğu yoğun bölgeleri dairesel/karesel genişleyerek
     * temizlemek için kullanılır.
     * Robot, önündeki kare hem açık hem de henüz "temizlenmemiş/ziyaret edilmemiş" ise
     * düz gider. Eğer önü kapalıysa veya önündeki kare zaten temizlenmişse sağa döner.
     * Bu sayede iç içe geçen karesel çizgilerle merkezden dışa doğru bir sarmal çizer.
     */
    public void hareketSpiral() {
        // Önümüz açık VE önümüzdeki hücre henüz süpürülmemiş bir zeminse düz ilerle
        if (onumAcikMi() && onumdekiKareZiyaretEdilmediMi()) {
            ileriGit();
            donusSayici = 0; // İlerleyebildiğimiz için dönüş sayacını sıfırla
        } else {
            // Önümüz kapalıysa veya önümüzdeki hücreyi zaten temizlediysek sarmalı büyütmek için sağa dön
            sagaDon();
            donusSayici++;
 
            // Eğer robot kendi etrafında 4 defa üst üste dönerse (yani etrafındaki 4 kare de
            // ya engelle kaplı ya da zaten temizlenmişse) sıkışmayı önlemek için geçici olarak
            // rastgele hareket modunu tetikler ve sarmal döngüsünden dışarı çıkar.
            if (donusSayici >= 4) {
                hareketRastgele();
                donusSayici = 0;
            }
        }
    }
 
    /**
     * 3. KENAR / DUVAR TAKİBİ (Wall Following)
     * Robotun odanın sınırlarını belirlemek ve mobilya kenarlarındaki tozları
     * temizlemek için kullandığı algoritmadır. 
     * Robot sağ tarafındaki sanal sensörle duvarı algılar.
     * - Sağ tarafı boşaldığında hemen sağa döner (duvarı sağında tutmaya devam etmek için).
     * - Sağ tarafı kapalıyken önü açıksa düz gider (duvar boyunca ilerler).
     * - Hem önü hem sağı kapalıysa köşeden kurtulmak için sola döner.
     */
    public void hareketDuvarTakibi() {
        if (sagimAcikMi()) {
            // Sağ taraf boşaldıysa duvarı takip etmek için sağa sap ve ilerle
            sagaDon();
            ileriGit();
        } else if (onumAcikMi()) {
            // Sağımız duvarla/engelle kaplıysa ve önümüz açıksa düz ilerle (duvarı takip et)
            ileriGit();
        } else {
            // Köşeye geldiysek (sağımız ve önümüz kapalıysa) sola dönerek kurtul
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

    /**
     * Robotun etrafındaki 4 bitişik hücrenin de ya engel (mobilya) ya da zaten temizlenmiş
     * (ziyaret edilmiş) olup olmadığını kontrol eder.
     * Eğer her taraf kapalıysa true döner, böylece robot başka bir temizlenmemiş alana sıçrayabilir.
     */
    public boolean etrafKapaliVeyaTemizlenmisMi() {
        int x = robot.getX();
        int y = robot.getY();
        int[][] yonler = {{-1, 0}, {1, 0}, {0, 1}, {0, -1}};
        for (int[] yon : yonler) {
            int komsuX = x + yon[1];
            int komsuY = y + yon[0];
            if (komsuX >= 0 && komsuX < oda.getSutunSayisi() && komsuY >= 0 && komsuY < oda.getSatirSayisi()) {
                Room.HucreTuru tur = oda.getHucreTuru(komsuX, komsuY);
                // Eğer komşu hücre henüz temizlenmemişse (TEMIZ, TOZ, SIVI veya LEKE ise) etraf kapalı değildir.
                if (tur == Room.HucreTuru.TEMIZ || tur == Room.HucreTuru.TOZ || 
                    tur == Room.HucreTuru.SIVI || tur == Room.HucreTuru.LEKE) {
                    return false;
                }
            }
        }
        return true;
    }

    public void reset() {
        donusSayici = 0;
        zigzagYonYukari = false;
    }

    public boolean yonAcikMi(Robot.YON yon) {
        int hX = robot.getX();
        int hY = robot.getY();
        if (yon == Robot.YON.KUZEY) hY--;
        else if (yon == Robot.YON.GUNEY) hY++;
        else if (yon == Robot.YON.DOGU) hX++;
        else if (yon == Robot.YON.BATI) hX--;

        if (hX < 0 || hX >= oda.getSutunSayisi() || hY < 0 || hY >= oda.getSatirSayisi()) return false;
        return oda.getHucreTuru(hX, hY) != Room.HucreTuru.ENGEL;
    }

    /**
     * 4. ZİKZAK NAVİGASYON (Boustrophedon / Zigzag Pattern)
     * Yeni nesil sistematik temizlik yapan süpürgelerin kullandığı tarama mantığıdır.
     * Robot odayı dikey kolonlar halinde (yukarı-aşağı) tarar.
     * Bir uca ulaştığında (örneğin kuzey veya güney duvarı), sağa (Doğuya) 1 adım kayar ve 
     * bu kez ters yönde taramaya başlar.
     * Eğer dikey yönde veya sağa kayarken engele (mobilyaya) çarparsa ve sıkışırsa,
     * BFS ile en yakın süpürülmemiş alana akıllıca rota çizer ve oradan zikzak yapmaya devam eder.
     */
    public void hareketZigzag() {
        Robot.YON dikeyYon = zigzagYonYukari ? Robot.YON.KUZEY : Robot.YON.GUNEY;
        
        // Eğer robot dikey doğrultuda değilse (örneğin ilk başta veya sağa kaydıktan sonra),
        // robotu tarayacağı dikey yöne döndür.
        if (robot.getYon() != Robot.YON.KUZEY && robot.getYon() != Robot.YON.GUNEY) {
            robot.setYon(dikeyYon);
            return;
        }

        if (onumAcikMi()) {
            ileriGit(); // Hatta önümüz açıksa ilerle
        } else {
            // Önümüz kapandıysa (duvar veya mobilya engeli varsa), sağa (DOĞU) 1 adım kay
            if (yonAcikMi(Robot.YON.DOGU)) {
                robot.setYon(Robot.YON.DOGU);
                ileriGit();
                
                // Sağa kaydıktan sonra dikey tarama yönünü tersine çevir
                zigzagYonYukari = !zigzagYonYukari;
            }
        }
    }

    public boolean zigzagSikistiMi() {
        Robot.YON dikeyYon = zigzagYonYukari ? Robot.YON.KUZEY : Robot.YON.GUNEY;
        boolean dikeyAcik = yonAcikMi(dikeyYon);
        boolean doguAcik = yonAcikMi(Robot.YON.DOGU);
        return !dikeyAcik && !doguAcik;
    }
}