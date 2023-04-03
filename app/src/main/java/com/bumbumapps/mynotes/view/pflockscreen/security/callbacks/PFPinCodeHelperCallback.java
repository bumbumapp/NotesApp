package com.bumbumapps.mynotes.view.pflockscreen.security.callbacks;

import com.bumbumapps.mynotes.view.pflockscreen.security.PFResult;

public interface PFPinCodeHelperCallback<T> {
    void onResult(PFResult<T> result);
}
