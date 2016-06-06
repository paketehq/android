package ph.pakete.view;


import android.content.Context;
import android.content.SharedPreferences;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.NativeExpressAdView;

import ph.pakete.BackHandledFragment;
import ph.pakete.Constants;
import ph.pakete.PackageTrackHistoryAdapter;
import ph.pakete.R;
import ph.pakete.databinding.FragmentPackageBinding;
import ph.pakete.helpers.MixpanelHelper;
import ph.pakete.viewmodel.ItemPackageViewModel;
import ph.pakete.viewmodel.PackagesViewModel;
import rx.android.schedulers.AndroidSchedulers;

public class PackageFragment extends BackHandledFragment {

    private FragmentPackageBinding binding;
    private PackagesViewModel packagesViewModel;
    private ItemPackageViewModel packageViewModel;

    public static PackageFragment newInstance(ItemPackageViewModel packageViewModel, PackagesViewModel packagesViewModel) {
        final PackageFragment fragment = new PackageFragment();
        fragment.packageViewModel = packageViewModel;
        fragment.packagesViewModel = packagesViewModel;
        return fragment;
    }

    public PackageFragment() {
        // Required empty public constructor
    }

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
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_package, container, false);
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        activity.setSupportActionBar(binding.toolbar);
        ActionBar actionBar = activity.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(packageViewModel.getName());
            // add back button
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }

        binding.toolbar.setNavigationOnClickListener(v -> onBackPressed());
        binding.toolbarEditButton.setOnClickListener(v -> editPackage());

        binding.textPackageTrackingNumber.setText(packageViewModel.getTrackingNumber());
        binding.textPackageCourierName.setText(packageViewModel.getCourierName());
        setupRecyclerView(binding.trackHistoryRecyclerView);

        // track mixpanel
        MixpanelHelper.getMixpanel(getActivity()).track("Package View");
        // setup ads
        setupNativeAds();

        return binding.getRoot();
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
        packageViewModel.getPackage().subscribe(aPackage1 -> {
            if (getActivity() == null) { return; }
            TextView noAvailableInformationYetText = (TextView) getActivity().findViewById(R.id.no_information_available_yet_text);
            if (noAvailableInformationYetText != null) {
                if (packageViewModel.getPackage().getValue().getTrackHistory().size() > 0) {
                    noAvailableInformationYetText.setVisibility(View.GONE);
                } else {
                    noAvailableInformationYetText.setVisibility(View.VISIBLE);
                }
            }
            binding.trackHistoryRecyclerView.getAdapter().notifyDataSetChanged();
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        packageViewModel.getPackage().unsubscribeOn(AndroidSchedulers.mainThread());
    }

    private void setupNativeAds() {
        // first we check if the user already purchased remove ads
        SharedPreferences preferences = getActivity().getSharedPreferences(Constants.SharedPreferences.NAME, Context.MODE_PRIVATE);
        if (preferences.getBoolean(Constants.SharedPreferences.REMOVE_ADS_KEY, false)) {
            binding.adView.setVisibility(View.GONE);
            return;
        }

        String nativeAdUnitID = getResources().getString(R.string.native_ad_unit_id);
        if (nativeAdUnitID.isEmpty()) { return; }

        NativeExpressAdView nativeExpressAdView = new NativeExpressAdView(getActivity());
        nativeExpressAdView.setAdSize(new AdSize(AdSize.FULL_WIDTH, 80));
        nativeExpressAdView.setAdUnitId(nativeAdUnitID);
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .build();
        LinearLayout adViewContainer = binding.adView;
        if (adViewContainer != null) {
            adViewContainer.addView(nativeExpressAdView);
            nativeExpressAdView.loadAd(adRequest);
        }
    }

    private void editPackage() {
        AddPackageFragment editPackageFragment = AddPackageFragment.newEditInstance(packagesViewModel, packageViewModel.getPackage());
        getFragmentManager()
                .beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.root_layout, editPackageFragment)
                .addToBackStack(null)
                .commit();
    }

    private void setupRecyclerView(RecyclerView recyclerView) {
        PackageTrackHistoryAdapter adapter = new PackageTrackHistoryAdapter();
        adapter.setPackageTrackHistory(packageViewModel.getTrackHistory(), getActivity());
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
    }

}
