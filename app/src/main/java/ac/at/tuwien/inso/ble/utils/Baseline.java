package ac.at.tuwien.inso.ble.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import ac.at.tuwien.inso.ble.HrvParameters;
import ac.at.tuwien.inso.ble.R;
import ac.at.tuwien.inso.ble.activities.DeviceScanActivity;

/**
 * Singleton for the Baseline.
 * Reads baseline from file at creating instance.
 * TODO: Auf SharedPreferences umstellen?
 */
public class Baseline {

    private final static String TAG = Baseline.class.getSimpleName();

    private static final String FILE_NAME = "baseline";
    private static Baseline instance;

    private final Context context;
    private HrvParameters params;

    private Baseline(Context context) {
        this.context = context;
        readBaseline();
    }

    public static Baseline getInstance(Context context) {
        if (instance == null) {
            instance = new Baseline(context);
        }
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

            Log.i(TAG, "--- BASELINE ---");
            Log.i(TAG, "MeanHR: " + meanHr);
            Log.i(TAG, "SDNN: " + sdnn);
            Log.i(TAG, "RMSSD: " + rmssd);
            Log.i(TAG, "pNN50: " + pnn50);
        } catch (FileNotFoundException e) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage(R.string.no_baseline_existing)
                    .setTitle(R.string.baseline)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Intent intent = new Intent(context, DeviceScanActivity.class);
                            intent.putExtra(IntentConstants.IS_BASELINE.toString(), true);
                            context.startActivity(intent);
                        }
                    });
            AlertDialog dialog = builder.create();
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
