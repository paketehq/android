package ph.pakete;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Collections;
import java.util.List;

import ph.pakete.databinding.ItemTrackHistoryBinding;
import ph.pakete.model.PackageTrackHistory;
import ph.pakete.viewmodel.ItemPackageTrackHistoryViewModel;

public class PackageTrackHistoryAdapter extends RecyclerView.Adapter<PackageTrackHistoryAdapter.PackageTrackHistoryViewHolder> {

    private List<PackageTrackHistory> trackHistory;
    private Context context;

    public PackageTrackHistoryAdapter() {
        this.trackHistory = Collections.emptyList();
    }

    public void setPackageTrackHistory(List<PackageTrackHistory> trackHistory, Context context) {
        this.trackHistory = trackHistory;
        this.context = context;
    }

    @Override
    public PackageTrackHistoryViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ItemTrackHistoryBinding binding = DataBindingUtil.inflate(
                LayoutInflater.from(parent.getContext()),
                R.layout.item_track_history,
                parent,
                false);
        return new PackageTrackHistoryViewHolder(binding, context);
    }

    @Override
    public void onBindViewHolder(PackageTrackHistoryViewHolder holder, int position) {
        holder.bindPackageTrackHistory(trackHistory.get(position));
    }

    @Override
    public int getItemCount() {
        return trackHistory.size();
    }

    public static class PackageTrackHistoryViewHolder extends RecyclerView.ViewHolder {
        final ItemTrackHistoryBinding binding;
        private Context context;

        public PackageTrackHistoryViewHolder(ItemTrackHistoryBinding binding, Context context) {
            super(binding.layoutContent);
            this.binding = binding;
            this.context = context;

            // set max lines
            TextView statusText = (TextView) itemView.findViewById(R.id.text_package_track_history_status);
            statusText.setMaxLines(Integer.MAX_VALUE);
        }

        void bindPackageTrackHistory(PackageTrackHistory trackHistory) {
            if (binding.getViewModel() == null) {
                binding.setViewModel(new ItemPackageTrackHistoryViewModel(trackHistory, context));
            } else {
                binding.getViewModel().setPackageTrackHistory(trackHistory);
            }
        }
    }
}
