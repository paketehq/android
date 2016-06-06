package ph.pakete;

public interface Constants {
    public interface SharedPreferences {
        public static final String NAME = "ph.pakete.preferences";
        public static final String SORT_BY_KEY = "SORT_BY_KEY";
        public static final String GROUP_BY_DELIVERED_KEY = "GROUP_BY_DELIVERED_KEY";
        public static final String REMOVE_ADS_KEY = "REMOVE_ADS_KEY";
    }

    public interface IAP {
        public static final String SKU_REMOVE_ADS = "ph.pakete.iap.removeads";
        public static final int REMOVE_ADS_PURCHASE_CODE = 10001;
    }
}
