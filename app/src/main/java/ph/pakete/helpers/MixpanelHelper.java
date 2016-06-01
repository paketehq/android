package ph.pakete.helpers;

import android.content.Context;

import com.mixpanel.android.mpmetrics.MixpanelAPI;

import ph.pakete.PaketeApplication;
import ph.pakete.R;

public class MixpanelHelper {

    public static MixpanelAPI getMixpanel(Context context) {
        String mixpanelToken = PaketeApplication.getAppContext().getResources().getString(R.string.mixpanel_token);
        return MixpanelAPI.getInstance(context, mixpanelToken);
    }
}
