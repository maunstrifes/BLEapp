package ac.at.tuwien.inso.ble.utils;

/**
 * Constants for Intents
 */
public enum IntentConstants {

    // Actions
    ACTION_GATT_CONNECTED,
    ACTION_GATT_DISCONNECTED,
    ACTION_GATT_SERVICES_DISCOVERED,
    ACTION_DATA_AVAILABLE,
    ACTION_HRV_DATA_AVAILABLE,

    // Flags
    IS_BASELINE,

    // Extra Data
    HR_DATA,
    HRV_DATA,
    SESSION_ID,
    DEVICE_NAME,
    DEVICE_ADDRESS
}
