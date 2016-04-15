package ph.pakete.viewmodel;

import android.databinding.BaseObservable;

import ph.pakete.model.Courier;

public class ItemCourierViewModel extends BaseObservable implements ViewModel {
    private Courier courier;

    public ItemCourierViewModel(Courier courier) {
        this.courier = courier;
    }

    public String getName() {
        return courier.getName();
    }

    // Allows recycling ItemCourierViewModel within the recyclerview adapter
    public void setCourier(Courier courier) {
        this.courier = courier;
        notifyChange();
    }

    @Override
    public void destroy() {
        //In this case destroy doesn't need to do anything because there is not async calls
    }
}
