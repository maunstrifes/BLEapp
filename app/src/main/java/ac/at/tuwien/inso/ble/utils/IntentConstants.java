package ac.at.tuwien.inso.ble.utils;

/**
 * Created by manu on 03.04.2015.
 */
public enum IntentConstants {

    // Actions
    ACTION_GATT_CONNECTED,
    ACTION_GATT_DISCONNECTED,
    ACTION_GATT_SERVICES_DISCOVERED,
    ACTION_DATA_AVAILABLE,
    ACTION_HRV_DATA_AVAILABLE,

    // Extra Data
    HR_DATA,
    HRV_DATA,
    SESSION_ID,
    DEVICE_NAME,
    DEVICE_ADDRESS
}
