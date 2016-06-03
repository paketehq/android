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

import ph.pakete.BackHandledFragment;
import ph.pakete.PackagesSortBy;
import ph.pakete.R;
import ph.pakete.SortyByAdapter;
import ph.pakete.databinding.FragmentSortByBinding;
import ph.pakete.helpers.MixpanelHelper;
import ph.pakete.viewmodel.PackagesViewModel;
import rx.Subscriber;

public class SortByFragment extends BackHandledFragment {

    private PackagesViewModel viewModel;
    private FragmentSortByBinding binding;
    private int selectedPosition;

    public static SortByFragment newInstance(PackagesViewModel viewModel) {
        final SortByFragment fragment = new SortByFragment();
        fragment.viewModel = viewModel;
        fragment.selectedPosition = viewModel.packagesSortBy().ordinal();
        return fragment;
    }

    public SortByFragment() { }

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
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_sort_by, container, false);
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        activity.setSupportActionBar(binding.toolbar);
        ActionBar actionBar = activity.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle("Sort By");
            // add back button
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }

        binding.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        setupRecyclerView(binding.sortByRecyclerView);

        // track mixpanel
        MixpanelHelper.getMixpanel(getActivity()).track("Sort By View");

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

    private void setupRecyclerView(RecyclerView recyclerView) {
        SortyByAdapter adapter = new SortyByAdapter();
        adapter.setSelectedSortBy(viewModel.packagesSortBy());
        adapter.onSelectSortBy.subscribe(sortBy -> {
            viewModel.sortBy(sortBy);
            adapter.setSelectedSortBy(sortBy);
        });
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
    }
}
