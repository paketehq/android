package ph.pakete.view;


import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.jakewharton.rxbinding.widget.RxTextView;

import org.json.JSONException;
import org.json.JSONObject;

import ph.pakete.BackHandledFragment;
import ph.pakete.Constants;
import ph.pakete.R;
import ph.pakete.databinding.FragmentAddPackageBinding;
import ph.pakete.model.Courier;
import ph.pakete.helpers.MixpanelHelper;
import ph.pakete.model.Package;
import ph.pakete.viewmodel.ItemPackageViewModel;
import ph.pakete.viewmodel.PackagesViewModel;
import rx.Observable;
import rx.Subscriber;
import rx.subjects.ReplaySubject;

public class AddPackageFragment extends BackHandledFragment {

    private PackagesViewModel viewModel;
    private Courier courier;
    private FragmentAddPackageBinding binding;
    private Boolean editMode = false;
    private ReplaySubject<Package> aPackage;
    private InterstitialAd interstitialAd;

    public static AddPackageFragment newInstance(PackagesViewModel viewModel, Courier courier) {
        final AddPackageFragment fragment = new AddPackageFragment();
        fragment.viewModel = viewModel;
        fragment.courier = courier;
        return fragment;
    }

    public static AddPackageFragment newEditInstance(PackagesViewModel viewModel, ReplaySubject<Package> aPackage) {
        final AddPackageFragment fragment = new AddPackageFragment();
        fragment.viewModel = viewModel;
        fragment.editMode = true;
        fragment.aPackage = aPackage;
        fragment.courier = aPackage.getValue().getCourier();
        return fragment;
    }

    public AddPackageFragment() {
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
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_add_package, container, false);
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        activity.setSupportActionBar(binding.toolbar);
        ActionBar actionBar = activity.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(courier.getName());
            // add back button
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }

        binding.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // if edit mode then set the values
        if (editMode) {
            binding.editTextName.setText(aPackage.getValue().getName());
            binding.editTextTrackingNumber.setEnabled(false); // disable tracking number edit text
            // if jrs
            if (courier.getCode().equals("jrs")) {
                // need to split the tracking number
                String[] separatedTrackingNumber = aPackage.getValue().getTrackingNumber().split("-");
                binding.editTextTrackingNumber.setText(separatedTrackingNumber[0]);
                binding.editTextExtraCode.setText(separatedTrackingNumber[1]);
                binding.editTextExtraCode.setEnabled(false);
            } else {
                binding.editTextTrackingNumber.setText(aPackage.getValue().getTrackingNumber());
            }

            // change add package button title to update package
            binding.buttonAddPackage.setText("Update Package");
            binding.buttonAddPackage.setOnClickListener(v -> updatePackage());

            // show archive package button
            binding.buttonArchivePackage.setVisibility(View.VISIBLE);
            binding.buttonArchivePackage.getBackground().setColorFilter(Color.RED, PorterDuff.Mode.MULTIPLY);
            binding.buttonArchivePackage.setOnClickListener(v -> onClickArchivePackageButton());
            // track mixpanel
            MixpanelHelper.getMixpanel(getActivity()).track("Edit Package View");
        } else {
            binding.buttonAddPackage.setOnClickListener(v -> addPackage());
            // track mixpanel
            MixpanelHelper.getMixpanel(getActivity()).track("Add Package View");
        }

        Observable<Boolean> trackingNumberIsValid = RxTextView.textChanges(binding.editTextTrackingNumber)
                .map(t -> t.length() > 0);

        Observable<Boolean> nameIsValid = RxTextView.textChanges(binding.editTextName)
                .map(t -> t.length() > 0);

        Observable<Boolean> formIsValid;
        // check if courier is jrs because it needs an additional field
        if (courier.getCode().equals("jrs")) {
            binding.editTextExtraCode.setVisibility(View.VISIBLE);
            binding.editTextExtraCode.setHint("BC");
            // change name edit text layout below
            RelativeLayout.LayoutParams editTextNameLayoutParams = (RelativeLayout.LayoutParams) binding.editTextName.getLayoutParams();
            editTextNameLayoutParams.addRule(RelativeLayout.BELOW, R.id.edit_text_extra_code);
            binding.editTextName.setLayoutParams(editTextNameLayoutParams);

            Observable<Boolean> extraCodeIsValid = RxTextView.textChanges(binding.editTextExtraCode)
                    .map(t -> t.length() > 0);
            formIsValid = Observable.combineLatest(trackingNumberIsValid, nameIsValid, extraCodeIsValid, (a, b, c) -> a && b && c);
        } else {
            formIsValid = Observable.combineLatest(trackingNumberIsValid, nameIsValid, (a, b) -> a && b);
        }

        formIsValid.distinctUntilChanged()
                .subscribe(binding.buttonAddPackage::setEnabled);
        formIsValid.distinctUntilChanged()
                .subscribe(binding.toolbar::setEnabled);

        setupInterstitialAd();

        return binding.getRoot();
    }

    @Override
    public boolean onBackPressed() {
        if (getFragmentManager() != null) {
            getFragmentManager().popBackStack();
            return true;
        }
        return false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        hideSoftKeyboard();
    }

    private void setupInterstitialAd() {
        String interstitialAdUnitId = getResources().getString(R.string.interstitial_ad_unit_id);
        if (interstitialAdUnitId.isEmpty()) { return; }

        interstitialAd = new InterstitialAd(getActivity());
        interstitialAd.setAdUnitId(getResources().getString(R.string.interstitial_ad_unit_id));
        AdRequest adRequest = new AdRequest.Builder()
                .build();

        interstitialAd.loadAd(adRequest);
    }

    private void onClickArchivePackageButton() {
        new AlertDialog.Builder(getActivity())
                .setTitle("Archive Package")
                .setMessage("Are you sure you want to archive this package?")
                .setPositiveButton(android.R.string.yes, (dialog, id) -> {
                    archivePackage();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void archivePackage() {
        viewModel.archivePackage(aPackage);
        // track mixpanel
        MixpanelHelper.getMixpanel(getActivity()).track("Archived Package");
        // pop to root
        getFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        // show fab
        ((MainActivity) getActivity()).showFAB();
    }

    private void addPackage() {
        // hide keyboard
        hideSoftKeyboard();
        // check if package already exists
        Package aPackage = viewModel.packageForTrackingNumber(binding.editTextTrackingNumber.getText().toString(), courier);
        if (aPackage != null) {
            if (aPackage.getArchived()) {
                // if archived ask the user if he/she wants it to unarchive
                new AlertDialog.Builder(getActivity())
                        .setTitle("Hey!")
                        .setMessage("This package has already been archived! Would you like to unarchive this package?")
                        .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                            // unarchive
                            viewModel.unarchivePackage(aPackage);
                            // pop to root
                            getFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
            } else {
                // show alert that this package already exists!
                new AlertDialog.Builder(getActivity())
                        .setTitle("Hey!")
                        .setMessage("This package already exists!")
                        .setPositiveButton("OK", null)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
            }
            return;
        }
        // else we add this new package!
        // if jrs we need to append the extra code
        String trackingNumber = binding.editTextTrackingNumber.getText().toString();
        if (courier.getCode().equals("jrs")) {
            trackingNumber += "-" + binding.editTextExtraCode.getText().toString();
        }
        // show progress dialog
        ProgressDialog dialog = ProgressDialog.show(getActivity(), null, "Tracking package...");

        Package newPackage = new Package();
        newPackage.setTrackingNumber(trackingNumber);
        newPackage.setCourier(courier);
        newPackage.setName(binding.editTextName.getText().toString());
        ReplaySubject<Package> packageReplaySubject = ReplaySubject.create();
        packageReplaySubject.onNext(newPackage);
        viewModel.trackPackage(packageReplaySubject)
                .subscribe(new Subscriber<ReplaySubject<Package>>() {
                    @Override
                    public void onCompleted() { }

                    @Override
                    public void onError(Throwable e) {
                        dialog.dismiss();
                        // show error
                        new AlertDialog.Builder(getActivity())
                                .setTitle("Sorry!")
                                .setMessage("Tracking number is invalid! Please check your tracking number.")
                                .setPositiveButton("OK", null)
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .show();
                    }

                    @Override
                    public void onNext(ReplaySubject<Package> packageReplaySubject) {
                        // track mixpanel
                        try {
                            JSONObject props = new JSONObject();
                            props.put("Courier", courier.getName());
                            MixpanelHelper.getMixpanel(getActivity()).track("Added Package", props);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        dialog.dismiss();
                        showInterstitialAd();
                        // add to package list
                        viewModel.addPackage(packageReplaySubject);
                        // show package
                        showPackageDetails(packageReplaySubject);
                    }
                });
    }

    private void showInterstitialAd() {
        SharedPreferences preferences = getActivity().getSharedPreferences(Constants.SharedPreferences.NAME, Context.MODE_PRIVATE);
        if (preferences.getBoolean(Constants.SharedPreferences.REMOVE_ADS_KEY, false)) { return; }
        if (interstitialAd == null) { return; }
        if (interstitialAd.isLoaded()) interstitialAd.show();
    }

    private void updatePackage() {
        viewModel.updatePackageName(binding.editTextName.getText().toString(), aPackage);
        Toast.makeText(getActivity(), "Package successfully updated!", Toast.LENGTH_SHORT).show();
        getFragmentManager().popBackStack();
    }

    private void hideSoftKeyboard() {
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(binding.editTextTrackingNumber.getWindowToken(), 0);
        imm.hideSoftInputFromWindow(binding.editTextName.getWindowToken(), 0);
    }

    private void showPackageDetails(ReplaySubject<Package> aPackage) {
        // pop to root
        getFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

        ItemPackageViewModel packageViewModel = new ItemPackageViewModel(aPackage, getActivity());
        PackageFragment packageFragment = PackageFragment.newInstance(packageViewModel, viewModel);
        getFragmentManager()
                .beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.root_layout, packageFragment)
                .addToBackStack(null)
                .commit();
    }

}
