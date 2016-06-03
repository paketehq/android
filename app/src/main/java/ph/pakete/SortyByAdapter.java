package ph.pakete;

import android.databinding.DataBindingUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import ph.pakete.databinding.ItemSortByBinding;
import ph.pakete.model.Courier;
import ph.pakete.viewmodel.ItemSortByViewModel;
import rx.subjects.PublishSubject;
import rx.subjects.ReplaySubject;

public class SortyByAdapter extends RecyclerView.Adapter<SortyByAdapter.SortByViewHolder> {

    public PublishSubject<PackagesSortBy> onSelectSortBy = PublishSubject.create();
    private List<PackagesSortBy> packagesSortByList;
    private PackagesSortBy selectedSortBy;

    public SortyByAdapter() {
        this.packagesSortByList = new ArrayList<>(EnumSet.allOf(PackagesSortBy.class));
    }

    public void setSelectedSortBy(PackagesSortBy selectedSortBy) {
        this.selectedSortBy = selectedSortBy;
        notifyDataSetChanged();
    }

    @Override
    public SortByViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ItemSortByBinding binding = DataBindingUtil.inflate(
                LayoutInflater.from(parent.getContext()),
                R.layout.item_sort_by,
                parent,
                false);
        return new SortByViewHolder(binding, onSelectSortBy, selectedSortBy);
    }

    @Override
    public void onBindViewHolder(SortByViewHolder holder, int position) {
        PackagesSortBy sortBy = packagesSortByList.get(position);
        holder.bindSortBy(sortBy, sortBy.equals(selectedSortBy));
    }

    @Override
    public int getItemCount() {
        return packagesSortByList.size();
    }

    public static class SortByViewHolder extends RecyclerView.ViewHolder {
        final ItemSortByBinding binding;
        public PublishSubject<PackagesSortBy> onSelectSortBy;
        private PackagesSortBy sortBy;

        public SortByViewHolder(ItemSortByBinding binding, PublishSubject<PackagesSortBy> onSelectSortBy, PackagesSortBy selectedSortBy) {
            super(binding.layoutContent);
            this.binding = binding;
            this.onSelectSortBy = onSelectSortBy;
            View contentLayout = itemView.findViewById(R.id.layout_content);
            contentLayout.setOnClickListener(v -> {
                binding.radioButton.setChecked(!binding.radioButton.isChecked());
                onSelectSortBy.onNext(sortBy);
            });
        }

        void bindSortBy(PackagesSortBy sortBy, boolean selected) {
            this.sortBy = sortBy;
            binding.radioButton.setChecked(selected);
            if (binding.getViewModel() == null) {
                binding.setViewModel(new ItemSortByViewModel(sortBy));
            } else {
                binding.getViewModel().setPackagesSortBy(sortBy);
            }
        }
    }
}
