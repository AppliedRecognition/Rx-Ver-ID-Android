package com.appliedrec.rxverid;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

import io.reactivex.disposables.Disposable;

/**
 * Base activity class with access to a RxVerID instance
 * @since 1.10.0
 */
public abstract class RxVerIDActivity extends AppCompatActivity {

    private ArrayList<Disposable> disposables = new ArrayList<>();
    private RxVerID rxVerID;

    /**
     * Get an instance of RxVerID
     * @return RxVerID built using {@link #getRxVerIDBuilder()}
     * @since 1.10.0
     */
    @NonNull
    public RxVerID getRxVerID() {
        if (rxVerID == null) {
            rxVerID = getRxVerIDBuilder().build();
        }
        return rxVerID;
    }

    /**
     * Get an instance of {@link RxVerID.Builder} used to build RxVerID instance
     * @return Builder
     * @since 1.10.0
     */
    protected RxVerID.Builder getRxVerIDBuilder() {
        return new RxVerID.Builder(getApplicationContext());
    }

    /**
     * Add a disposable that will be automatically disposed when the activity is destroyed
     * @param disposable Disposable
     * @since 1.10.0
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
}
