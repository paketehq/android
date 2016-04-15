package ph.pakete;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.media.Image;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import ph.pakete.databinding.ItemPackageBinding;
import ph.pakete.model.Package;
import ph.pakete.viewmodel.ItemPackageViewModel;
import rx.subjects.PublishSubject;
import rx.subjects.ReplaySubject;

public class PackagesAdapter extends RecyclerView.Adapter<PackagesAdapter.PackageViewHolder> {

    public PublishSubject<ReplaySubject<Package>> onSelectPackage = PublishSubject.create();
    public PublishSubject<ReplaySubject<Package>> onLongClickPackage = PublishSubject.create();
    private ReplaySubject<List<ReplaySubject<Package>>> packages = ReplaySubject.create();
    private Context context;

    public PackagesAdapter() { }

    public void setPackages(ReplaySubject<List<ReplaySubject<Package>>> packages, Context context) {
        this.packages = packages;
        this.packages.subscribe(replaySubjects -> notifyDataSetChanged());
        this.context = context;
    }

    @Override
    public PackageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ItemPackageBinding binding = DataBindingUtil.inflate(
                LayoutInflater.from(parent.getContext()),
                R.layout.item_package,
                parent,
                false);
        return new PackageViewHolder(binding, onSelectPackage, onLongClickPackage, context);
    }

    @Override
    public void onBindViewHolder(PackageViewHolder holder, int position) {
        holder.bindPackage(packages.getValue().get(position));
    }

    @Override
    public int getItemCount() {
        return packages.getValue().size();
    }

    public static class PackageViewHolder extends RecyclerView.ViewHolder {
        final ItemPackageBinding binding;
        public PublishSubject<ReplaySubject<Package>> onSelectPackage;
        public PublishSubject<ReplaySubject<Package>> onLongClickPackage;
        private ReplaySubject<Package> aPackage;
        private Context context;

        public PackageViewHolder(ItemPackageBinding binding, PublishSubject<ReplaySubject<Package>> onSelectPackage, PublishSubject<ReplaySubject<Package>> onLongClickPackage, Context context) {
            super(binding.layoutContent);
            this.binding = binding;
            this.onSelectPackage = onSelectPackage;
            this.onLongClickPackage = onLongClickPackage;
            this.context = context;

            // set max lines
            TextView nameText = (TextView) itemView.findViewById(R.id.text_package_name);
            nameText.setMaxLines(Integer.MAX_VALUE);
            TextView statusText = (TextView) itemView.findViewById(R.id.text_package_status);
            statusText.setMaxLines(Integer.MAX_VALUE);

            View contentLayout = itemView.findViewById(R.id.layout_content);
            contentLayout.setOnClickListener(v -> PackageViewHolder.this.onSelectPackage.onNext(aPackage));
            contentLayout.setOnLongClickListener(v -> {
                PackageViewHolder.this.onLongClickPackage.onNext(aPackage);
                return true;
            });
        }

        void bindPackage(ReplaySubject<Package> aPackage) {
            this.aPackage = aPackage;
            if (binding.getViewModel() == null) {
                binding.setViewModel(new ItemPackageViewModel(aPackage, context));
            } else {
                binding.getViewModel().setPackage(aPackage);
            }
        }
    }
}
