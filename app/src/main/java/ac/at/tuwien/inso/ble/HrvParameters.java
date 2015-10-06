package ac.at.tuwien.inso.ble;

import java.io.Serializable;

/**
 * Created by manu on 06.08.2015.
 */
public class HrvParameters implements Serializable {

    public static final String DELIMITER = ";";
    private final double meanHr;
    private final double sdnn;
    private final double rmssd;
    private final double pnn50;

    public HrvParameters(double meanHr, double sdnn, double rmssd, double pnn50) {
        this.meanHr = meanHr;
        this.sdnn = sdnn;
        this.rmssd = rmssd;
        this.pnn50 = pnn50;
    }

    public HrvParameters(String s) {
        String[] split = s.split(DELIMITER);
        this.meanHr = Double.valueOf(split[0]);
        this.sdnn = Double.valueOf(split[1]);
        this.rmssd = Double.valueOf(split[2]);
        this.pnn50 = Double.valueOf(split[3]);
    }

    public double getMeanHr() {
        return meanHr;
    }

    public double getSdnn() {
        return sdnn;
    }

    public double getRmssd() {
        return rmssd;
    }

    public double getPnn50() {
        return pnn50;
    }

    @Override
    public String toString() {
        return meanHr + DELIMITER + sdnn + DELIMITER + rmssd + DELIMITER + pnn50;
    }
}
