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
    private double sumRr;
    private long sumHr;
    private double meanHr;
    private double meanRr;
    private double sdnn;
    private double rmssd;
    private double pnn50;

    public BaseCalculator(LimitedList<Double> heartRate) {
        this.heartRate = heartRate;
    }

    /**
     * Returns the successive differences of the RR-Intervals
     */
    private List<Double> getDifferences() {
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

    /**
     * Returns the sum of heart beats (in bpm)
     */
    protected double getSumHr() {
        if (sumHr == 0) {
            for (Double hr : heartRate) {
                sumHr += hr;
            }
        }
        return sumHr;
    }

    /**
     * Returns the sum of RR intervals
     */
    protected double getSumRr() {
        if (sumRr == 0) {
            for (Double rr : getIntervals()) {
                sumRr += rr;
            }
        }
        return sumRr;
    }

    /**
     * Returns the mean heart rate
     */
    public double getMeanHr() {
        if (meanHr == 0) {
            meanHr = getSumHr() / heartRate.size();
        }
        return meanHr;
    }

    /**
     * Returns the mean RR-Interval
     */
    protected double getMeanRr() {
        if (meanRr == 0) {
            meanRr = getSumRr() / getIntervals().size();
        }
        return meanRr;
    }

    /**
     * Returns the Standard Deviation of the RR-Intervals
     */
    public double getSdnn() {
        if (sdnn == 0) {
            double mean = getMeanRr();
            double temp = 0.0;
            for (double rr : getIntervals()) {
                temp += (rr - mean) * (rr - mean);
            }
            sdnn = Math.sqrt(temp / (getIntervals().size() - 1));
        }
        return sdnn;
    }

    /**
     * Returns the RMSSD (Root Mean Square of the Successive Differences)
     */
    public double getRmssd() {
        if (rmssd == 0) {
            for (Double diff : getDifferences()) {
                rmssd += Math.pow(diff, 2);
            }
            rmssd = Math.sqrt(rmssd / (differences.size() - 1));
        }
        return rmssd;
    }

    /**
     * Returns the pNN50 (percentage of differences over 50ms)
     */
    public double getPnn50() {
        if (pnn50 == 0) {
            double i = 0.0;
            for (Double diff : getDifferences()) {
                if (diff > 50) {
                    i++;
                }
            }
            pnn50 = i / getDifferences().size();
        }
        return pnn50;
    }

    /**
     * Returns a List of RR-Intervals
     */
    private List<Double> getIntervals() {
        if (intervals == null) {
            intervals = new ArrayList<Double>(heartRate.size());
            for (Double hr : heartRate) {
                intervals.add(60000.0 / hr);
            }
        }
        return intervals;
    }
}