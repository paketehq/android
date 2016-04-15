package ph.pakete.view;

import android.app.AlertDialog;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

import io.smooch.ui.ConversationActivity;
import ph.pakete.BuildConfig;
import ph.pakete.R;
import ph.pakete.model.MixpanelHelper;

public class SettingsActivity extends AppCompatActivity {

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
        versionText.setText(version);

        // track mixpanel
        MixpanelHelper.getMixpanel(this).track("Settings View");
    }

    public void onClickRemoveAdsButton(View view) {
        AlertDialog.Builder removeAdsDialogBuilder = new AlertDialog.Builder(this);
        removeAdsDialogBuilder.setTitle("Hate Ads?");
        removeAdsDialogBuilder.setItems(new CharSequence[]{"Pay to Remove Ads", "Restore Purchases", "Cancel"},
                (dialog, which) -> {
                    // The 'which' argument contains the index position
                    // of the selected item
                    switch (which) {
                        case 0: // Pay to Remove Ads
                            break;
                        case 1: // Restore Purchases
                            break;
                    }
                });
        removeAdsDialogBuilder.create().show();
    }

    public void onClickReportAProblemButton(View view) {
        ConversationActivity.show(this);
    }


}
