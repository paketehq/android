package ph.pakete.view;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import io.smooch.ui.ConversationActivity;
import ph.pakete.BuildConfig;
import ph.pakete.R;
import ph.pakete.model.MixpanelHelper;
import ph.pakete.model.Token;
import ph.pakete.util.IabHelper;
import ph.pakete.util.Purchase;
import ph.pakete.util.SkuDetails;

public class SettingsActivity extends AppCompatActivity {

    IabHelper inAppBillingHelper;
    static final String SKU_REMOVE_ADS = "ph.pakete.iap.removeads";
    static final int REMOVE_ADS_PURCHASE_CODE = 10001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // add back button
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }

        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> onBackPressed());
        }

        // set version
        String version = String.format("%s (%d)", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE);
        TextView versionText = (TextView) findViewById(R.id.text_version);
        if (versionText != null) {
            versionText.setText(version);
        }

        // track mixpanel
        MixpanelHelper.getMixpanel(this).track("Settings View");
        // fetch sku details
        fetchRemoveAdsSkuDetails();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (inAppBillingHelper == null) return;

        // Pass on the activity result to the helper for handling
        if (!inAppBillingHelper.handleActivityResult(requestCode, resultCode, data)) {
            // not handled, so handle it ourselves (here's where you'd
            // perform any handling of activity results not related to in-app
            // billing...
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    public void onClickRemoveAdsButton(View view) {
        AlertDialog.Builder removeAdsDialogBuilder = new AlertDialog.Builder(this);
        removeAdsDialogBuilder.setTitle("Hate Ads?");
        removeAdsDialogBuilder.setItems(new CharSequence[]{"Pay to Remove Ads", "Cancel"},
                (dialog, which) -> {
                    // The 'which' argument contains the index position
                    // of the selected item
                    switch (which) {
                        case 0: // Pay to Remove Ads
                            purchaseRemoveAds();
                            break;
                    }
                });
        removeAdsDialogBuilder.create().show();
    }

    public void onClickReportAProblemButton(View view) {
        ConversationActivity.show(this);
    }

    private void purchaseRemoveAds() {
        // purchase listener
        IabHelper.OnIabPurchaseFinishedListener purchaseFinishedListener = (result, purchase) -> {
            if (purchase != null) {
                if (purchase.getSku().equals(SKU_REMOVE_ADS)) {
                    broadcastRemoveAds();
                }
            }
        };

        try {
            inAppBillingHelper.launchPurchaseFlow(this,
                    SKU_REMOVE_ADS,
                    REMOVE_ADS_PURCHASE_CODE,
                    purchaseFinishedListener,
                    Token.getUniqueID());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void fetchRemoveAdsSkuDetails() {
        // show progress dialog
        ProgressDialog dialog = ProgressDialog.show(this, null, null);
        String base64EncodedPublicKey = getResources().getString(R.string.license_key);

        // compute your public key and store it in base64EncodedPublicKey
        inAppBillingHelper = new IabHelper(this, base64EncodedPublicKey);
        inAppBillingHelper.enableDebugLogging(true);

        // Listener that's called when we finish querying the items and
        // subscriptions we own
        IabHelper.QueryInventoryFinishedListener queryInventoryFinishedListener = (result, inventory) -> {
            dialog.dismiss();
            // Have we been disposed of in the meantime? If so, quit.
            if (inAppBillingHelper == null) {
                return;
            }

            if (result.isSuccess() && inventory != null) {
                // check purchase
                Purchase removeAdsPurchase = inventory.getPurchase(SKU_REMOVE_ADS);
                if (removeAdsPurchase != null) {
                    // go send remove ads
                    broadcastRemoveAds();
                    return;
                }

                SkuDetails removeAdsSkuDetails = inventory.getSkuDetails(SKU_REMOVE_ADS);
                if (removeAdsSkuDetails != null) {
                    Resources res = getResources();
                    String text = String.format(res.getString(R.string.header_text_remove_ads), removeAdsSkuDetails.getPrice());
                    TextView headerRemoveAdsText = (TextView) findViewById(R.id.header_text);
                    if (headerRemoveAdsText != null) {
                        headerRemoveAdsText.setText(text);
                    }
                }
            }
        };

        inAppBillingHelper.startSetup(result -> {
            if (result.isSuccess()) {
                // fetch remove ads sku details
                try {
                    List additionalSkuList = new ArrayList();
                    additionalSkuList.add(SKU_REMOVE_ADS);
                    inAppBillingHelper.queryInventoryAsync(true, additionalSkuList,
                            queryInventoryFinishedListener);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                dialog.dismiss();
            }
        });
    }

    private void broadcastRemoveAds() {
        // save to preferences
        SharedPreferences preferences = getSharedPreferences("ph.pakete.preferences", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("removedAds", true);
        editor.apply();

        // broadcast remove ads
        Intent intent = new Intent("removeAds");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (inAppBillingHelper != null) inAppBillingHelper.dispose();
        } catch (Exception e) {
            e.printStackTrace();
        }
        inAppBillingHelper = null;
    }
}
