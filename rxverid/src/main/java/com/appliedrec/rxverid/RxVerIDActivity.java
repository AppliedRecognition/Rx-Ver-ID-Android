package com.appliedrec.rxverid;

import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

import io.reactivex.disposables.Disposable;

/**
 * Base activity class with access to a RxVerID instance
 * @since 2.0.0
 */
public abstract class RxVerIDActivity extends AppCompatActivity implements IRxVerIDSettable {

    private ArrayList<Disposable> disposables = new ArrayList<>();
    private RxVerID rxVerID;

    /**
     * Get an instance of RxVerID
     * <p>You will need to call {@link RxVerIDHelper#registerRxVerIDForApplication(RxVerID, Application)} or {@link RxVerIDHelper#registerDefaultRxVerIDForApplication(Application)}. Otherwise the method returns {@literal null}.</p>
     * @return RxVerID
     * @since 2.0.0
     */
    @Nullable
    public RxVerID getRxVerID() {
        return rxVerID;
    }

    /**
     * Add a disposable that will be automatically disposed when the activity is destroyed
     * @param disposable Disposable
     * @since 2.0.0
     */
    protected void addDisposable(@NonNull Disposable disposable) {
        runOnUiThread(() -> {
            if (isDestroyed()) {
                disposable.dispose();
            } else {
                disposables.add(disposable);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        for (Disposable disposable : disposables) {
            disposable.dispose();
        }
        disposables.clear();
    }

    /**
     * Called automatically at {@link android.app.Activity#onCreate(Bundle)}
     * @param rxVerID {@link RxVerID} or {@literal null}
     * @since 2.0.0
     */
    @Override
    public void setRxVerID(@Nullable RxVerID rxVerID) {
        this.rxVerID = rxVerID;
    }
}
