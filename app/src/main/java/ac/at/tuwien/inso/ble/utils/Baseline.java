package ac.at.tuwien.inso.ble.utils;

import android.os.Environment;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import ac.at.tuwien.inso.ble.HrvParameters;

/**
 * Singleton for the Baseline.
 * Reads baseline from file at creating instance.
 */
public class Baseline {
    private static final String FILE_NAME = "baseline";
    private static Baseline instance = new Baseline();

    private HrvParameters params;

    private Baseline() {
        readBaseline();
    }

    public static Baseline getInstance() {
        return instance;
    }

    /**
     * Reads the Baseline-File. If it doesn't exist the Activity to create the Baseline is loaded.
     */
    private void readBaseline() {
        File file = new File(Environment.getExternalStorageDirectory()
                .getPath() + "/" + FILE_NAME);
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            double meanHr = Double.parseDouble(reader.readLine());
            double sdnn = Double.parseDouble(reader.readLine());
            double rmssd = Double.parseDouble(reader.readLine());
            double pnn50 = Double.parseDouble(reader.readLine());
            params = new HrvParameters(meanHr, sdnn, rmssd, pnn50);
        } catch (FileNotFoundException e) {
            //TODO: baseline-activity laden? Vorsicht: wird auch aufgerufen, wenn die Baseline gespeichert wird :/
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    //ignore
                }
            }
        }
    }

    /**
     * Sets the HRV Parameters of the baseline. To persist saveBaseline() must be called afterwards.
     *
     * @param params
     */
    public void setBaseline(HrvParameters params) {
        this.params = params;
    }

    /**
     * Writes the Baseline file
     */
    public void saveBaseline() {
        File file = new File(Environment.getExternalStorageDirectory()
                .getPath() + "/" + FILE_NAME);

        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(file, false /*append*/));
            writer.write(String.valueOf(params.getMeanHr()));
            writer.newLine();
            writer.write(String.valueOf(params.getSdnn()));
            writer.newLine();
            writer.write(String.valueOf(params.getRmssd()));
            writer.newLine();
            writer.write(String.valueOf(params.getPnn50()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    //ignore
                }
            }
        }
    }
}
