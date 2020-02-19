package com.appliedrec.rxverid;

import com.appliedrec.verid.core.VerIDSessionResult;

/**
 * Exception thrown by Ver-ID session
 * @since 1.8.0
 */
public class VerIDSessionException extends Exception {

    private final VerIDSessionResult result;

    /**
     * Constructor
     * @param result Session result
     * @since 1.8.0
     */
    public VerIDSessionException(VerIDSessionResult result) {
        super(result.getError());
        this.result = result;
    }

    /**
     * Get the result of the failed session
     * @return Session result
     * @since 1.8.0
     */
    public VerIDSessionResult getSessionResult() {
        return result;
    }


}
