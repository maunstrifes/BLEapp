package ac.at.tuwien.inso.ble.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * All variables in this class are lazily initialized.
 */
public class BaseCalculator {

    private final LimitedList<Double> heartRate;
    private List<Double> intervals;
    private List<Double> differences; //absolute
    private long sumRr;
    private long sumHr;
    private double meanHr;
    private double meanRr;
    private double sdnn;
    private double rmssd;
    private double pnn50;

    public BaseCalculator(LimitedList<Double> heartRate) {
        this.heartRate = heartRate;
    }

    public LimitedList<Double> getHeartRate() {
        return heartRate;
    }

    public List<Double> getDifferences() {
        if (differences == null) {
            differences = new ArrayList<Double>(heartRate.size());
            double last = 0;
            for (Double rr : getIntervals()) {
                if (last != 0) {
                    differences.add(Math.abs(rr - last));
                }
                last = rr;
            }
        }
        return differences;
    }

    public double getSumHr() {
        if (sumHr == 0) {
            for (Double hr : heartRate) {
                sumHr += hr;
            }
        }
        return sumHr;
    }

    public double getSumRr() {
        if (sumRr == 0) {
            for (Double rr : getIntervals()) {
                sumRr += rr;
            }
        }
        return sumRr;
    }

    public double getMeanHr() {
        if (meanHr == 0) {
            meanHr = getSumHr() / heartRate.size();
        }
        return meanHr;
    }

    public double getMeanRr() {
        if (meanRr == 0) {
            meanRr = getSumRr() / getIntervals().size();
        }
        return meanRr;
    }

    public double getSdnn() {
        if (sdnn == 0) {
            double mean = getMeanRr();
            double temp = 0;
            for (double rr : getIntervals()) {
                temp += (mean - rr) * (mean - rr);
            }
            sdnn = temp / getIntervals().size();
        }
        return sdnn;
    }

    public double getRmssd() {
        if (rmssd == 0) {
            for (Double diff : getDifferences()) {
                rmssd += Math.pow(diff, 2);
            }
            rmssd = Math.sqrt(rmssd) / (differences.size() - 1);
        }
        return rmssd;
    }

    public double getPnn50() {
        if (pnn50 == 0) {
            int i = 0;
            for (Double diff : getDifferences()) {
                if (diff > 50) {
                    i++;
                }
            }
            pnn50 = i / getDifferences().size();
        }
        return pnn50;
    }

    public List<Double> getIntervals() {
        if (intervals == null) {
            intervals = new ArrayList<Double>(heartRate.size());
            for (Double hr : heartRate) {
                intervals.add((1 / hr) * 1000 * 60);
            }
        }
        return intervals;
    }
}