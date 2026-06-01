package vacuumsim.model;

/**
 * Robotun şarj olduğu istasyonun koordinatlarını tutan Model sınıfı.
 */
public class ChargingStation {
    private int x;
    private int y;

    // İstasyonun kurulacağı yeri belirliyoruz (Bizimki sol üstte, yani 0,0)
    public ChargingStation(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() { return x; }
    public int getY() { return y; }
}