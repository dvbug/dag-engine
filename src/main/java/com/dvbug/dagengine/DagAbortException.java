package com.dvbug.dagengine;


/**
 * DAG调用中止异常
 */
public class DagAbortException extends RuntimeException{
    public DagAbortException() {
    }

    public DagAbortException(String message) {
        super(message);
    }

    public DagAbortException(String message, Throwable cause) {
        super(message, cause);
    }

    public DagAbortException(Throwable cause) {
        super(cause);
    }

    public DagAbortException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
