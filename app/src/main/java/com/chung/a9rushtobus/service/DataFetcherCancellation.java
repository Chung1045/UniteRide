package com.chung.a9rushtobus.service;

import android.content.Context;
import android.util.Log;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Helper class to add cancellation functionality to DataFetcher
 * This class provides methods to cancel ongoing operations in DataFetcher
 */
public class DataFetcherCancellation {
    
    // The DataFetcher instance to control
    private final DataFetcher dataFetcher;
    
    // Flag to track if the current operation has been cancelled
    private final AtomicBoolean isCancelled = new AtomicBoolean(false);
    
    /**
     * Creates a new DataFetcherCancellation instance
     * 
     * @param dataFetcher The DataFetcher instance to control
     */
    public DataFetcherCancellation(DataFetcher dataFetcher) {
        this.dataFetcher = dataFetcher;
    }
    
    /**
     * Cancels all ongoing operations in the DataFetcher
     * 
     * @return true if operations were cancelled, false if already cancelled
     */
    public boolean cancelOperations() {
        if (isCancelled.getAndSet(true)) {
            // Already cancelled
            return false;
        }
        
        Log.d("DataFetcherCancellation", "Cancelling all operations");
        
        // Cancel all active network requests in the DataFetcher
        dataFetcher.cancelAllRequests();
        
        return true;
    }
    
    /**
     * Resets the cancellation state so the DataFetcher can be reused
     */
    public void resetCancellationState() {
        isCancelled.set(false);
        Log.d("DataFetcherCancellation", "Cancellation state reset");
    }
    
    /**
     * Checks if the current operation has been cancelled
     * 
     * @return true if cancelled, false otherwise
     */
    public boolean isCancelled() {
        return isCancelled.get();
    }
}
