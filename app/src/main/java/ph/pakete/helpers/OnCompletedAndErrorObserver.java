package ph.pakete.helpers;

import rx.Observer;

public abstract class OnCompletedAndErrorObserver<T> implements Observer<T> {
    @Override
    public void onNext(T o) { }
}
