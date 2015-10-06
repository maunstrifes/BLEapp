package ac.at.tuwien.inso.ble;

import java.io.Serializable;

/**
 * Valueholder for HRV Parameters
 */
public class HrvParameters implements Serializable {

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
}
