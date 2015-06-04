package ac.at.tuwien.inso.ble.utils;

import android.os.Environment;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import ac.at.tuwien.inso.ble.database.Session;

/**
 * Created by manu on 04.06.2015.
 */
public class FileHelper {

    public static String getFilePath(Session session) {
        return Environment.getExternalStorageDirectory()
                .getPath() + "/" + session.getId() + ".csv";
    }

    public static BufferedWriter createFile(Session session) {
        File file = new File(FileHelper.getFilePath(session));
        try {
            if (!file.exists()) {

                file.createNewFile();

            }
            return new BufferedWriter(new FileWriter(file,
                    true));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
