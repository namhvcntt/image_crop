package com.namhv.crop_image.image_crop.callback;


import android.net.Uri;

public interface SaveCallback extends Callback{
    void onSuccess(Uri outputUri);
    void onError();
}
