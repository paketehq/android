package ph.pakete.view;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.eggheadgames.siren.Siren;
import com.eggheadgames.siren.SirenAlertType;
import com.eggheadgames.siren.SirenVersionCheckType;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;

import hotchemi.android.rate.AppRate;
import hotchemi.android.rate.OnClickButtonListener;
import ly.count.android.sdk.Countly;
import ph.pakete.BackHandledFragment;
import ph.pakete.PackagesAdapter;
import ph.pakete.PaketeApplication;
import ph.pakete.R;
import ph.pakete.helpers.MixpanelHelper;
import ph.pakete.model.Package;
import ph.pakete.viewmodel.ItemPackageViewModel;
import ph.pakete.viewmodel.PackagesViewModel;
import rx.subjects.ReplaySubject;

public class MainActivity extends AppCompatActivity implements BackHandledFragment.BackHandlerInterface {

    private PackagesViewModel viewModel;
    private BackHandledFragment selectedFragment;
    private Boolean exit = false;
    private SwipeRefreshLayout swipeContainer;
    private static final String SIREN_JSON_URL = "http://pakete.ph/siren/version.json";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new PackagesViewModel(this);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        if (fab != null) {
            fab.setOnClickListener(v -> showCouriersFragment());
        }
        swipeContainer = (SwipeRefreshLayout) findViewById(R.id.swipeContainer);
        if (swipeContainer != null) {
            swipeContainer.setOnRefreshListener(() -> {
                viewModel.refreshPackages();
                swipeContainer.setRefreshing(false);
            });
        }

        setupRecyclerView((RecyclerView) findViewById(R.id.packages_recycler_view));
        setupAdBannerView();
        viewModel.packages.subscribe(replaySubjects -> {
            TextView emptyText = (TextView) findViewById(R.id.empty_text);
            if (emptyText != null) {
                if (replaySubjects.size() == 0) {
                    emptyText.setVisibility(View.VISIBLE);
                } else {
                    emptyText.setVisibility(View.INVISIBLE);
                }
            }
        });

        // track mixpanel
        MixpanelHelper.getMixpanel(this).track("Packages View");
        // listen for broadcast
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver,
                new IntentFilter("removeAds"));
        // app rater
        setupAppRater();
        // check for update. we force users to update right now
        Siren siren = Siren.getInstance(getApplicationContext());
        siren.setMajorUpdateAlertType(SirenAlertType.FORCE);
        siren.setMinorUpdateAlertType(SirenAlertType.FORCE);
        siren.setPatchUpdateAlertType(SirenAlertType.FORCE);
        siren.checkVersion(this, SirenVersionCheckType.DAILY, SIREN_JSON_URL);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Countly.sharedInstance().onStart(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Countly.sharedInstance().onStop();
    }

    @Override
    protected void onDestroy() {
        // send all mixpanel events
        MixpanelHelper.getMixpanel(this).flush();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_packages, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // force refresh packages
        viewModel.refreshPackages();
    }

    @Override
    public void onBackPressed() {
        if(selectedFragment == null || !selectedFragment.onBackPressed()) {
            // Selected fragment did not consume the back press event.
            if (exit) {
                finish(); // finish activity
            } else {
                Toast.makeText(this, "Press Back again to Exit.",
                        Toast.LENGTH_SHORT).show();
                exit = true;
                new Handler().postDelayed(() -> exit = false, 3 * 1000);
            }
        }
    }

    @Override
    public void setSelectedFragment(BackHandledFragment selectedFragment) {
        this.selectedFragment = selectedFragment;
        // hide fab
        hideFAB();
    }

    public void hideFAB() {
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        if (fab != null) {
            fab.setVisibility(View.INVISIBLE);
        }
    }

    public void showFAB() {
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        if (fab != null) {
            fab.setVisibility(View.VISIBLE);
        }
    }

    private void setupAdBannerView() {
        // first we check if the user already purchased remove ads
        SharedPreferences preferences = getSharedPreferences("ph.pakete.preferences", Context.MODE_PRIVATE);
        if (preferences.getBoolean("removedAds", false)) { return; }

        String bannerAdUnitID = getResources().getString(R.string.banner_ad_unit_id);
        if (bannerAdUnitID.isEmpty()) { return; }

        AdView bottomBannerAdView = new AdView(this);
        bottomBannerAdView.setAdSize(AdSize.BANNER);
        bottomBannerAdView.setAdUnitId(bannerAdUnitID);
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .build();
        LinearLayout adViewContainer = (LinearLayout) findViewById(R.id.adView);
        if (adViewContainer != null) {
            adViewContainer.addView(bottomBannerAdView);
            bottomBannerAdView.loadAd(adRequest);
        }
    }

    private void setupRecyclerView(RecyclerView recyclerView) {
        PackagesAdapter adapter = new PackagesAdapter();
        adapter.setPackages(viewModel.packages, this);
        adapter.onSelectPackage.subscribe(this::showPackageFragment);
        adapter.onLongClickPackage.subscribe(this::showPackageOptions);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void showCouriersFragment() {
        final CouriersFragment couriersFragment = CouriersFragment.newInstance(viewModel);
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                .add(R.id.root_layout, couriersFragment)
                .addToBackStack(null)
                .commit();
    }

    private void showPackageFragment(ReplaySubject<Package> aPackage) {
        ItemPackageViewModel packageViewModel = new ItemPackageViewModel(aPackage, this);
        final PackageFragment packageFragment = PackageFragment.newInstance(packageViewModel, viewModel);
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.root_layout, packageFragment)
                .addToBackStack(null)
                .commit();
    }

    private void removePackageFromRecyclerView(ReplaySubject<Package> aPackage) {
        // get index
        int index = viewModel.packages.getValue().indexOf(aPackage);
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.packages_recycler_view);
        if (recyclerView != null) {
            recyclerView.getAdapter().notifyItemRemoved(index);
        }
    }

    private void showPackageOptions(ReplaySubject<Package> aPackage) {
        new AlertDialog.Builder(this)
                .setTitle("Archive Package")
                .setMessage("Are you sure you want to archive this package?")
                .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                    // archive
                    viewModel.archivePackage(aPackage);
                    // track mixpanel
                    MixpanelHelper.getMixpanel(this).track("Archived Package");
                    // delete from recycler view
                    removePackageFromRecyclerView(aPackage);
                })
                .setNegativeButton(android.R.string.no, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    // Our handler for received Intents. This will be called whenever an Intent
    private BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // hide ad view
            LinearLayout adViewLayout = (LinearLayout) findViewById(R.id.adView);
            if (adViewLayout != null) {
                adViewLayout.setVisibility(View.GONE);
            }
        }
    };

    private void setupAppRater() {
        AppRate.with(this).monitor();
        // Show a dialog if meets conditions
        AppRate.showRateDialogIfMeetsConditions(this);
    }
}
