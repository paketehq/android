package ph.pakete.view;


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
import android.widget.Toast;

import ph.pakete.BackHandledFragment;
import ph.pakete.CouriersAdapter;
import ph.pakete.R;
import ph.pakete.databinding.FragmentCouriersBinding;
import ph.pakete.helpers.OnErrorObserver;
import ph.pakete.model.Courier;
import ph.pakete.model.MixpanelHelper;
import ph.pakete.viewmodel.PackagesViewModel;

public class CouriersFragment extends BackHandledFragment {

    private PackagesViewModel viewModel;
    private FragmentCouriersBinding binding;

    public static CouriersFragment newInstance(PackagesViewModel viewModel) {
        final CouriersFragment fragment = new CouriersFragment();
        fragment.viewModel = viewModel;
        return fragment;
    }

    public CouriersFragment() { }

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
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_couriers, container, false);
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        activity.setSupportActionBar(binding.toolbar);
        ActionBar actionBar = activity.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle("Couriers");
            // add back button
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }

        binding.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        viewModel.couriers
                .map(couriers -> couriers.size() > 0 ? View.GONE : View.VISIBLE)
                .subscribe(binding.progress::setVisibility);

        setupRecyclerView(binding.couriersRecyclerView);

        // track mixpanel
        MixpanelHelper.getMixpanel(getActivity()).track("Couriers View");

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
        // get couriers for update
        if (viewModel.couriers.size() == 0) {
            viewModel.getCouriers()
                    .subscribe(new OnErrorObserver<Void>() {
                        @Override
                        public void onError(Throwable e) {
                            // hide progress view
                            binding.progress.setVisibility(View.GONE);
                            // if we didn't have the cached data then we show the error
                            if (viewModel.couriers.getValue().isEmpty()) {
                                // show toast error
                                Toast.makeText(getActivity(), "There was a problem fetching Couriers. Please try again.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
    }

    private void setupRecyclerView(RecyclerView recyclerView) {
        CouriersAdapter adapter = new CouriersAdapter();
        adapter.setCouriers(viewModel.couriers);
        adapter.onSelectCourier.subscribe(this::showAddPackage);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
    }

    private void showAddPackage(Courier courier) {
        AddPackageFragment addPackageFragment = AddPackageFragment.newInstance(viewModel, courier);
        getFragmentManager()
                .beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.root_layout, addPackageFragment)
                .addToBackStack(null)
                .commit();
    }
}
