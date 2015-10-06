package ac.at.tuwien.inso.ble.utils;

import android.os.Environment;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import ac.at.tuwien.inso.ble.database.Session;

/**
 * Helper for Session Files
 */
public class SessionFileHelper {

    public static String getFilePath(Session session) {
        return Environment.getExternalStorageDirectory()
                .getPath() + "/" + session.getId() + ".csv";
    }

    public static BufferedWriter createFile(Session session) {
        File file = new File(SessionFileHelper.getFilePath(session));
        try {
            return new BufferedWriter(new FileWriter(file,
                    false /*append*/));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
