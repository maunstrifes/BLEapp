package ac.at.tuwien.inso.ble.utils;

import android.os.Environment;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import ac.at.tuwien.inso.ble.HrvParameters;
import ac.at.tuwien.inso.ble.database.Session;

/**
 * Created by manu on 04.06.2015.
 */
public class FileHelper {

    public static String getFilePath(Session session) {
        return Environment.getExternalStorageDirectory()
                .getPath() + "/" + session.getId() + ".csv";
    }

    private static String getBaselinePath() {
        return Environment.getExternalStorageDirectory()
                .getPath() + "/baseline.csv";
    }

    public static void writeBaseline(HrvParameters params) {
        File file = new File(getBaselinePath());
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            BufferedWriter writer = null;
            try {
                writer = new BufferedWriter(new FileWriter(file,
                        false /*append*/));
                writer.write(params.toString());
            } finally {
                if (writer != null) {
                    writer.close();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static HrvParameters readBaseline() {
        File file = new File(getBaselinePath());
        try {
            if (!file.exists()) {
                return null;
            }
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader(file));
                return new HrvParameters(reader.readLine());
            } finally {
                if (reader != null) {
                    reader.close();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static BufferedWriter createFile(Session session) {
        File file = new File(FileHelper.getFilePath(session));
        try {
            if (!file.exists()) {

                file.createNewFile();

            }
            return new BufferedWriter(new FileWriter(file,
                    true /*append*/));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
