package ph.pakete.helpers;

import rx.Observer;

public abstract class OnErrorObserver<T> implements Observer<T> {
    @Override
    public void onCompleted() { }

    @Override
    public void onNext(T o) { }
}
