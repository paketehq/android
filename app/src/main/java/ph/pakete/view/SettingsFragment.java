package ph.pakete.view;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.facebook.share.model.ShareLinkContent;
import com.facebook.share.widget.ShareDialog;
import com.twitter.sdk.android.tweetcomposer.TweetComposer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import hotchemi.android.rate.AppRate;
import io.smooch.ui.ConversationActivity;
import ph.pakete.BackHandledFragment;
import ph.pakete.BuildConfig;
import ph.pakete.R;
import ph.pakete.databinding.FragmentSettingsBinding;
import ph.pakete.helpers.MixpanelHelper;
import ph.pakete.model.Token;
import ph.pakete.util.IabHelper;
import ph.pakete.util.SkuDetails;
import ph.pakete.viewmodel.PackagesViewModel;

public class SettingsFragment extends BackHandledFragment {
    private PackagesViewModel viewModel;
    private FragmentSettingsBinding binding;

    private IabHelper inAppBillingHelper;
    private static final String SKU_REMOVE_ADS = "ph.pakete.iap.removeads";
    private static final int REMOVE_ADS_PURCHASE_CODE = 10001;

    public static SettingsFragment newInstance(PackagesViewModel viewModel) {
        final SettingsFragment fragment = new SettingsFragment();
        fragment.viewModel = viewModel;
        return fragment;
    }

    public SettingsFragment() { }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        MenuItem menuItem = menu.findItem(R.id.action_settings);
        menuItem.setVisible(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_settings, container, false);
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        activity.setSupportActionBar(binding.toolbar);
        ActionBar actionBar = activity.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle("Settings");
            // add back button
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }

        binding.toolbar.setNavigationOnClickListener(v -> onBackPressed());
        binding.buttonRemoveAds.setOnClickListener(v -> onClickRemoveAdsButton());
        binding.buttonRatePakete.setOnClickListener(v -> onClickWriteAReviewButton());
        binding.buttonContactThePaketeTeam.setOnClickListener(v -> onClickContactPaketeTeamButton());
        binding.buttonTweet.setOnClickListener(v -> onClickTweetAboutPakete());
        binding.buttonFb.setOnClickListener(v -> onClickTellYourFriendsAboutPakete());
        binding.textSortBy.setText(viewModel.packagesSortBy().toString());
        binding.buttonSortBy.setOnClickListener(v -> onClickSortByButton());
        binding.buttonGroupByDelivered.setOnClickListener(v -> binding.switchGroupByDelivered.setChecked(!binding.switchGroupByDelivered.isChecked()));
        binding.switchGroupByDelivered.setChecked(viewModel.packagesGroupByDelivered());
        binding.switchGroupByDelivered.setOnCheckedChangeListener((buttonView, isChecked) -> viewModel.groupByDelivered(isChecked));
        // listen to sort by change
        viewModel.packagesSortByReplySubject.subscribe(sortBy -> binding.textSortBy.setText(viewModel.packagesSortBy().toString()));

        // set version
        String version = String.format(Locale.getDefault(), "%s (%d)", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE);
        TextView versionText = binding.textVersion;
        if (versionText != null) {
            versionText.setText(version);
        }

        // track mixpanel
        MixpanelHelper.getMixpanel(getActivity()).track("Settings View");

        // fetch sku details if needed
        if (alreadyPurchasedRemoveAds() == false) {
            fetchRemoveAdsSkuDetails();
        }

        return binding.getRoot();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            if (inAppBillingHelper != null) inAppBillingHelper.dispose();
        } catch (Exception e) {
            e.printStackTrace();
        }
        inAppBillingHelper = null;
    }

    @Override
    public boolean onBackPressed() {
        if (getFragmentManager() != null) {
            getFragmentManager().popBackStack();
            // show fab
            ((MainActivity) getActivity()).showFAB();
            return true;
        }
        return false;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (inAppBillingHelper == null) return;

        // Pass on the activity result to the helper for handling
        if (!inAppBillingHelper.handleActivityResult(requestCode, resultCode, data)) {
            // not handled, so handle it ourselves (here's where you'd
            // perform any handling of activity results not related to in-app
            // billing...
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void onClickRemoveAdsButton() {
        AlertDialog.Builder removeAdsDialogBuilder = new AlertDialog.Builder(getActivity());
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

    private void onClickSortByButton() {
        final SortByFragment sortByFragment = SortByFragment.newInstance(viewModel);
        getActivity().getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                .add(R.id.root_layout, sortByFragment)
                .addToBackStack(null)
                .commit();
    }

    private void onClickWriteAReviewButton() {
        AppRate.with(getActivity()).showRateDialog(getActivity());
    }

    private void onClickContactPaketeTeamButton() {
        ConversationActivity.show(getActivity());
    }

    private void onClickTweetAboutPakete() {
        String tweet = getResources().getString(R.string.share_message) + " " + getResources().getString(R.string.app_url);
        TweetComposer.Builder builder = new TweetComposer.Builder(getActivity())
                .text(tweet);
        builder.show();
    }

    private void onClickTellYourFriendsAboutPakete() {
        ShareLinkContent content = new ShareLinkContent.Builder()
                .setContentUrl(Uri.parse(getResources().getString(R.string.app_url)))
                .build();
        ShareDialog.show(this, content);
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
            inAppBillingHelper.launchPurchaseFlow(getActivity(),
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
        ProgressDialog dialog = ProgressDialog.show(getActivity(), null, null);
        String base64EncodedPublicKey = getResources().getString(R.string.license_key);

        // compute your public key and store it in base64EncodedPublicKey
        inAppBillingHelper = new IabHelper(getActivity(), base64EncodedPublicKey);
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
                SkuDetails removeAdsSkuDetails = inventory.getSkuDetails(SKU_REMOVE_ADS);
                if (removeAdsSkuDetails != null) {
                    Resources res = getResources();
                    String text = String.format(res.getString(R.string.header_text_remove_ads), removeAdsSkuDetails.getPrice());
                    TextView headerRemoveAdsText = binding.headerText;
                    if (headerRemoveAdsText != null) {
                        headerRemoveAdsText.setText(text);
                    }

                    // show remove ads layout
                    RelativeLayout removeAdsLayout = binding.removeAdsLayout;
                    removeAdsLayout.setVisibility(View.VISIBLE);
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
        SharedPreferences preferences = getActivity().getSharedPreferences("ph.pakete.preferences", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("removedAds", true);
        editor.apply();

        // broadcast remove ads
        Intent intent = new Intent("removeAds");
        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);

        // remove ads layout
        RelativeLayout removeAdsLayout = binding.removeAdsLayout;
        removeAdsLayout.setVisibility(View.GONE);
    }

    private Boolean alreadyPurchasedRemoveAds() {
        SharedPreferences preferences = getActivity().getSharedPreferences("ph.pakete.preferences", Context.MODE_PRIVATE);
        return preferences.getBoolean("removedAds", false);
    }
}
