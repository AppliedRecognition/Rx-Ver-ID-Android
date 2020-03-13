package com.appliedrec.rxverid;

import org.reactivestreams.Publisher;

import io.reactivex.Completable;
import io.reactivex.CompletableSource;
import io.reactivex.CompletableTransformer;
import io.reactivex.Flowable;
import io.reactivex.FlowableTransformer;
import io.reactivex.Maybe;
import io.reactivex.MaybeSource;
import io.reactivex.MaybeTransformer;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.SingleTransformer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

/**
 * Transformer that can be used in a {@link Observable#compose(ObservableTransformer)}, {@link Single#compose(SingleTransformer)}, {@link Maybe#compose(MaybeTransformer)}, {@link Completable#compose(CompletableTransformer)} and {@link Flowable#compose(FlowableTransformer)} to subscribe and observe on specified schedulers
 * @param <T> Type
 * @since 1.10.0
 */
public class SchedulersTransformer<T> implements ObservableTransformer<T,T>, SingleTransformer<T,T>, FlowableTransformer<T,T>, MaybeTransformer<T,T>, CompletableTransformer {

    private Scheduler subscribeScheduler;
    private Scheduler observeScheduler;

    /**
     * Instance that schedules subscriptions on {@link Schedulers#io()} and observations on {@link AndroidSchedulers#mainThread()}.
     * @param <T> Type
     * @return Transformer
     * @since 1.10.0
     */
    public static <T> SchedulersTransformer<T> defaultInstance() {
        return new SchedulersTransformer<>(Schedulers.io(), AndroidSchedulers.mainThread());
    }

    /**
     * Constructor with schedulers
     * @param subscribeScheduler Subscription scheduler
     * @param observeScheduler Observation scheduler
     * @since 1.10.0
     */
    public SchedulersTransformer(Scheduler subscribeScheduler, Scheduler observeScheduler) {
        this.subscribeScheduler = subscribeScheduler;
        this.observeScheduler = observeScheduler;
    }

    @Override
    public CompletableSource apply(Completable upstream) {
        if (observeScheduler != null) {
            return upstream.subscribeOn(subscribeScheduler).observeOn(observeScheduler);
        } else {
            return upstream.subscribeOn(subscribeScheduler);
        }
    }

    @Override
    public Publisher<T> apply(Flowable<T> upstream) {
        if (observeScheduler != null) {
            return upstream.subscribeOn(subscribeScheduler).observeOn(observeScheduler);
        } else {
            return upstream.subscribeOn(subscribeScheduler);
        }
    }

    @Override
    public MaybeSource<T> apply(Maybe<T> upstream) {
        if (observeScheduler != null) {
            return upstream.subscribeOn(subscribeScheduler).observeOn(observeScheduler);
        } else {
            return upstream.subscribeOn(subscribeScheduler);
        }
    }

    @Override
    public ObservableSource<T> apply(Observable<T> upstream) {
        if (observeScheduler != null) {
            return upstream.subscribeOn(subscribeScheduler).observeOn(observeScheduler);
        } else {
            return upstream.subscribeOn(subscribeScheduler);
        }
    }

    @Override
    public SingleSource<T> apply(Single<T> upstream) {
        if (observeScheduler != null) {
            return upstream.subscribeOn(subscribeScheduler).observeOn(observeScheduler);
        } else {
            return upstream.subscribeOn(subscribeScheduler);
        }
    }
}
