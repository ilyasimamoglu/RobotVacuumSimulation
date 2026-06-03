package vacuumsim.model;

public class Robot {
// enum kullanma sebebimiz seçenklerden sadece bir tanesini seçebilmek için
 public enum YON{
     KUZEY,GUNEY,DOGU,BATI
 }

    private int x;           // X koordinatı (Sütun)
    private int y;           // Y koordinatı (Satır)
    private int batarya;     // Batarya yüzdesi (0-100)
    private YON yon;         // Robotun şu an baktığı yön

    public Robot(int baslangicX , int baslangicY){
        this.x = baslangicX;
        this.y = baslangicY;
        this.batarya=100;
        this.yon = YON.DOGU;
    }

    public int getX(){
        return x;
    }
    public void setX(int x){
        this.x= x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getBatarya() {
        return batarya;
    }
    public YON getYon() {
        return yon;
    }

    public void setYon(YON yon) {
        this.yon = yon;
    }

    // Batarya değerini elle değiştirmek için setter
    public void setBatarya(int batarya) {
        if (batarya < 0) {
            this.batarya = 0;
        } else if (batarya > 100) {
            this.batarya = 100;
        } else {
            this.batarya = batarya;
        }
    }

    // Batarya düşürme metodu
    public void sarjTuket(int miktar){
        this.batarya-=miktar;
        if (this.batarya < 0) {
            this.batarya = 0; // Batarya eksiye düşemez!
        }
    }

    // Robot şarj istasyonuna dönünce çağıracağımız metot
    public void fullSajYap() {
        this.batarya = 100;
    }



}
