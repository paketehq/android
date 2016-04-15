package ph.pakete;

import android.databinding.DataBindingUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import ph.pakete.databinding.ItemCourierBinding;
import ph.pakete.model.Courier;
import ph.pakete.viewmodel.ItemCourierViewModel;
import rx.subjects.PublishSubject;
import rx.subjects.ReplaySubject;

public class CouriersAdapter extends RecyclerView.Adapter<CouriersAdapter.CourierViewHolder> {

    public PublishSubject<Courier> onSelectCourier = PublishSubject.create();
    private ReplaySubject<List<Courier>> couriers;

    public CouriersAdapter() {
        this.couriers = ReplaySubject.create();
    }

    public void setCouriers(ReplaySubject<List<Courier>> couriers) {
        this.couriers = couriers;
        this.couriers.subscribe(couriers1 -> notifyDataSetChanged());
    }

    @Override
    public CourierViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ItemCourierBinding binding = DataBindingUtil.inflate(
                LayoutInflater.from(parent.getContext()),
                R.layout.item_courier,
                parent,
                false);
        return new CourierViewHolder(binding, onSelectCourier);
    }

    @Override
    public void onBindViewHolder(CourierViewHolder holder, int position) {
        holder.bindCourier(couriers.getValue().get(position));
    }

    @Override
    public int getItemCount() {
        return couriers.getValue().size();
    }

    public static class CourierViewHolder extends RecyclerView.ViewHolder {
        final ItemCourierBinding binding;
        public PublishSubject<Courier> onSelectCourier;
        private Courier courier;

        public CourierViewHolder(ItemCourierBinding binding, PublishSubject<Courier> onSelectCourier) {
            super(binding.layoutContent);
            this.binding = binding;
            this.onSelectCourier = onSelectCourier;
            View contentLayout = itemView.findViewById(R.id.layout_content);
            contentLayout.setOnClickListener(v -> CourierViewHolder.this.onSelectCourier.onNext(courier));
        }

        void bindCourier(Courier courier) {
            this.courier = courier;
            if (binding.getViewModel() == null) {
                binding.setViewModel(new ItemCourierViewModel(courier));
            } else {
                binding.getViewModel().setCourier(courier);
            }
        }
    }
}
