package com.appliedrec.rxverid;

import androidx.annotation.NonNull;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.concurrent.TimeUnit;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.android.plugins.RxAndroidPlugins;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.schedulers.ExecutorScheduler;
import io.reactivex.observers.TestObserver;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.TestSubscriber;

@RunWith(MockitoJUnitRunner.class)
public class SchedulersTransformerTest {

    @BeforeClass
    public static void setupRxSchedulers() {
        Scheduler immediate = new Scheduler() {
            @Override
            public Disposable scheduleDirect(@NonNull Runnable run, long delay, @NonNull TimeUnit unit) {
                // this prevents StackOverflowErrors when scheduling with a delay
                return super.scheduleDirect(run, 0, unit);
            }

            @Override
            public Scheduler.Worker createWorker() {
                return new ExecutorScheduler.ExecutorWorker(Runnable::run, true);
            }
        };

        RxJavaPlugins.setInitIoSchedulerHandler(scheduler -> immediate);
        RxJavaPlugins.setInitComputationSchedulerHandler(scheduler -> immediate);
        RxJavaPlugins.setInitNewThreadSchedulerHandler(scheduler -> immediate);
        RxJavaPlugins.setInitSingleSchedulerHandler(scheduler -> immediate);
        RxAndroidPlugins.setInitMainThreadSchedulerHandler(scheduler -> immediate);
    }

    @Test
    public void test_composeOnSingle_succeeds() {
        String string = "Hello";
        Single<String> stringSingle = Single.just(string);

        TestObserver testObserver = stringSingle.compose(SchedulersTransformer.defaultInstance()).test();

        testObserver
                .assertSubscribed()
                .assertComplete()
                .assertNoErrors()
                .assertValue(string);

        testObserver = stringSingle.compose(new SchedulersTransformer<>(Schedulers.io(), null)).test();

        testObserver
                .assertSubscribed()
                .assertComplete()
                .assertNoErrors()
                .assertValue(string);
    }

    @Test
    public void test_composeOnObservable_succeeds() {
        String string = "Hello";
        Observable<String> stringObservable = Observable.just(string);

        TestObserver testObserver = stringObservable.compose(SchedulersTransformer.defaultInstance()).test();

        testObserver
                .assertSubscribed()
                .assertComplete()
                .assertNoErrors()
                .assertValue(string);

        testObserver = stringObservable.compose(new SchedulersTransformer<>(Schedulers.io(), null)).test();

        testObserver
                .assertSubscribed()
                .assertComplete()
                .assertNoErrors()
                .assertValue(string);
    }

    @Test
    public void test_composeOnMaybe_succeeds() {
        String string = "Hello";
        Maybe<String> stringMaybe = Maybe.just(string);

        TestObserver testObserver = stringMaybe.compose(SchedulersTransformer.defaultInstance()).test();

        testObserver
                .assertSubscribed()
                .assertComplete()
                .assertNoErrors()
                .assertValue(string);

        testObserver = stringMaybe.compose(new SchedulersTransformer<>(Schedulers.io(), null)).test();

        testObserver
                .assertSubscribed()
                .assertComplete()
                .assertNoErrors()
                .assertValue(string);
    }

    @Test
    public void test_composeOnFlowable_succeeds() {
        String string = "Hello";
        Flowable<String> stringFlowable = Flowable.just(string);

        TestSubscriber testObserver = stringFlowable.compose(SchedulersTransformer.defaultInstance()).test();

        testObserver
                .assertSubscribed()
                .assertComplete()
                .assertNoErrors()
                .assertValue(string);

        testObserver = stringFlowable.compose(new SchedulersTransformer<>(Schedulers.io(), null)).test();

        testObserver
                .assertSubscribed()
                .assertComplete()
                .assertNoErrors()
                .assertValue(string);
    }

    @Test
    public void test_composeOnCompletable_succeeds() {
        Completable completable = Completable.complete();

        TestObserver testObserver = completable.compose(SchedulersTransformer.defaultInstance()).test();

        testObserver
                .assertSubscribed()
                .assertComplete()
                .assertNoErrors();

        testObserver = completable.compose(new SchedulersTransformer<>(Schedulers.io(), null)).test();

        testObserver
                .assertSubscribed()
                .assertComplete()
                .assertNoErrors();
    }
}
