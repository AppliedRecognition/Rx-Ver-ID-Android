package com.appliedrec.rxverid;

import android.app.Activity;
import android.os.Bundle;

import androidx.annotation.Nullable;

/**
 * Interface that provides a method to set an instance of {@link RxVerID}
 * <p>If implemented in an activity and the activity's application has an instance of RxVerID registered using {@link RxVerIDHelper}, the activity will automatically have an instance of RxVerID set in {@link android.app.Activity#onCreate(Bundle)} and removed in {@link Activity#onDestroy()}.</p>
 * @since 2.0.0
 */
public interface IRxVerIDSettable {
    /**
     * Set RxVerID
     * @param rxVerID RxVerID
     * @since 2.0.0
     */
    void setRxVerID(@Nullable RxVerID rxVerID);
}
