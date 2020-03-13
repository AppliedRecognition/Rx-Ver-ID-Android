package com.appliedrec.rxverid;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import java.util.HashMap;
import java.util.HashSet;

/**
 * Helper class that registers an instance of {@link RxVerID} for an application.
 * @since 2.0.0
 */
public class RxVerIDHelper implements Application.ActivityLifecycleCallbacks {

    private static RxVerIDHelper instance = new RxVerIDHelper();
    private static final Object INSTANCE_LOCK = new Object();

    private HashSet<RxVerID> rxVerIDHashSet = new HashSet<>();
    private RxVerIDHelper() {
    }

    /**
     * Register default instance of {@link RxVerID} for application
     * @param application Application for which to register the RxVerID instance
     * @since 2.0.0
     */
    public static void registerDefaultRxVerIDForApplication(Application application) {
        RxVerIDHelper.registerRxVerIDForApplication(new RxVerID.Builder(application), application);
    }

    /**
     * Register an instance of {@link RxVerID} for application
     * @param builder Instance of RxVerID.Builder to be used to create the RxVerID instance. The context of the builder will be replaced with the application context.
     * @param application Application for which to register the RxVerID instance
     * @since 2.0.0
     */
    public static void registerRxVerIDForApplication(RxVerID.Builder builder, Application application) {
        new Handler(Looper.getMainLooper()).post(() -> {
            synchronized (INSTANCE_LOCK) {
                builder.setContext(application.getApplicationContext());
                RxVerID rxVerID = builder.build();
                for (RxVerID verID : RxVerIDHelper.instance.rxVerIDHashSet) {
                    if (verID.getContext().getApplicationContext() == application.getApplicationContext()) {
                        RxVerIDHelper.instance.rxVerIDHashSet.remove(verID);
                        break;
                    }
                }
                RxVerIDHelper.instance.rxVerIDHashSet.add(rxVerID);
            }
            application.registerActivityLifecycleCallbacks(RxVerIDHelper.instance);
        });
    }

    /**
     * Get an instance of {@link RxVerID} registered for application
     * @param application Application for which the instance is registered
     * @return RxVerID or {@literal null} if no instance is registered for the application
     * @since 2.0.0
     */
    public static RxVerID getRxVerIDRegisteredForApplication(Application application) {
        synchronized (INSTANCE_LOCK) {
            for (RxVerID verID : RxVerIDHelper.instance.rxVerIDHashSet) {
                if (verID.getContext().getApplicationContext() == application.getApplicationContext()) {
                    return verID;
                }
            }
            return null;
        }
    }

    /**
     * Unregister RxVerID from application
     * @param application Application from which to unregister RxVerID
     * @since 2.0.0
     */
    public static void unregisterRxVerIDFromApplication(Application application) {
        new Handler(Looper.getMainLooper()).post(() -> {
            synchronized (INSTANCE_LOCK) {
                RxVerID rxVerID = getRxVerIDRegisteredForApplication(application);
                if (rxVerID != null) {
                    RxVerIDHelper.instance.rxVerIDHashSet.remove(rxVerID);
                }
            }
            application.unregisterActivityLifecycleCallbacks(RxVerIDHelper.instance);
        });
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        if (activity instanceof IRxVerIDSettable) {
            RxVerID rxVerID = RxVerIDHelper.getRxVerIDRegisteredForApplication(activity.getApplication());
            if (rxVerID != null) {
                ((IRxVerIDSettable) activity).setRxVerID(rxVerID);
            }
        }
    }

    @Override
    public void onActivityStarted(Activity activity) {

    }

    @Override
    public void onActivityResumed(Activity activity) {

    }

    @Override
    public void onActivityPaused(Activity activity) {

    }

    @Override
    public void onActivityStopped(Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        if (activity instanceof IRxVerIDSettable) {
            ((IRxVerIDSettable) activity).setRxVerID(null);
        }
    }
}
