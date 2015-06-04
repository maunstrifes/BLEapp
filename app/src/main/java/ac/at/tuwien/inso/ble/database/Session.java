package ac.at.tuwien.inso.ble.database;

import java.io.Serializable;

public class Session implements Serializable {

    private long id;
    private long time;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }
}
