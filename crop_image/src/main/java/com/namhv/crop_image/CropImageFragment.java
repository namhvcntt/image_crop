package com.namhv.crop_image;

import com.namhv.crop_image.image_crop.CropImageView;
import com.namhv.crop_image.image_crop.callback.CropCallback;
import com.namhv.crop_image.image_crop.callback.LoadCallback;
import com.namhv.crop_image.image_crop.callback.SaveCallback;
import com.namhv.crop_image.image_crop.util.Utils;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.io.File;

import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;

@RuntimePermissions
public class CropImageFragment extends Fragment {
    private static final int REQUEST_PICK_IMAGE = 10011;
    private static final int REQUEST_SAF_PICK_IMAGE = 10012;
    private static final String PROGRESS_DIALOG = "ProgressDialog";

    // Views ///////////////////////////////////////////////////////////////////////////////////////
    private CropImageView mCropView;

    // Note: only the system can call this constructor by reflection. 
    public CropImageFragment() {
    }

    public static CropImageFragment getInstance() {
        CropImageFragment fragment = new CropImageFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // bind Views
        bindViews(view);
//        mCropView.setDebug(true);
        // set bitmap to CropImageView
        if (mCropView.getImageBitmap() == null) {
            mCropView.setImageResource(R.drawable.sample5);
            mCropView.setCropMode(CropImageView.CropMode.SQUARE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent result) {
        super.onActivityResult(requestCode, resultCode, result);
        if (requestCode == REQUEST_PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            showProgress();
            mCropView.startLoad(result.getData(), mLoadCallback);
        } else if (requestCode == REQUEST_SAF_PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            showProgress();
            mCropView.startLoad(Utils.ensureUriPermission(getContext(), result), mLoadCallback);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        CropImageFragmentPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    // Bind views //////////////////////////////////////////////////////////////////////////////////

    private void bindViews(View view) {
        mCropView = (CropImageView) view.findViewById(R.id.cropImageView);
        view.findViewById(R.id.buttonDone).setOnClickListener(btnListener);
        view.findViewById(R.id.buttonPickImage).setOnClickListener(btnListener);
        view.findViewById(R.id.buttonRotateLeft).setOnClickListener(btnListener);
        view.findViewById(R.id.buttonRotateRight).setOnClickListener(btnListener);
    }

    @NeedsPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
    public void pickImage() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            startActivityForResult(new Intent(Intent.ACTION_GET_CONTENT).setType("image/*"), REQUEST_PICK_IMAGE);
        } else {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            startActivityForResult(intent, REQUEST_SAF_PICK_IMAGE);
        }
    }

    @NeedsPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    public void cropImage() {
        showProgress();
        mCropView.startCrop(createSaveUri(), mCropCallback, mSaveCallback);
    }

    @OnShowRationale(Manifest.permission.READ_EXTERNAL_STORAGE)
    public void showRationaleForPick(PermissionRequest request) {
        showRationaleDialog(R.string.permission_pick_rationale, request);
    }

    @OnShowRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    public void showRationaleForCrop(PermissionRequest request) {
        showRationaleDialog(R.string.permission_crop_rationale, request);
    }

    public void showProgress() {
        ProgressDialogFragment f = ProgressDialogFragment.getInstance();
        getFragmentManager()
                .beginTransaction()
                .add(f, PROGRESS_DIALOG)
                .commitAllowingStateLoss();
    }

    public void dismissProgress() {
        if (!isAdded()) return;
        android.support.v4.app.FragmentManager manager = getFragmentManager();
        if (manager == null) return;
        ProgressDialogFragment f = (ProgressDialogFragment) manager.findFragmentByTag(PROGRESS_DIALOG);
        if (f != null) {
            getFragmentManager().beginTransaction().remove(f).commitAllowingStateLoss();
        }
    }

    public Uri createSaveUri() {
        return Uri.fromFile(new File(getActivity().getCacheDir(), "cropped"));
    }

    private void showRationaleDialog(@StringRes int messageResId, final PermissionRequest request) {
        new AlertDialog.Builder(getActivity())
                .setPositiveButton(R.string.button_allow, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(@NonNull DialogInterface dialog, int which) {
                        request.proceed();
                    }
                })
                .setNegativeButton(R.string.button_deny, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(@NonNull DialogInterface dialog, int which) {
                        request.cancel();
                    }
                })
                .setCancelable(false)
                .setMessage(messageResId)
                .show();
    }

    // Handle button event /////////////////////////////////////////////////////////////////////////

    private final View.OnClickListener btnListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.buttonDone:
                    CropImageFragmentPermissionsDispatcher.cropImageWithCheck(CropImageFragment.this);
                    break;
                case R.id.buttonRotateLeft:
                    mCropView.rotateImage(CropImageView.RotateDegrees.ROTATE_M90D);
                    break;
                case R.id.buttonRotateRight:
                    mCropView.rotateImage(CropImageView.RotateDegrees.ROTATE_90D);
                    break;
                case R.id.buttonPickImage:
                    CropImageFragmentPermissionsDispatcher.pickImageWithCheck(CropImageFragment.this);
                    break;
            }
        }
    };

    // Callbacks ///////////////////////////////////////////////////////////////////////////////////

    private final LoadCallback mLoadCallback = new LoadCallback() {
        @Override
        public void onSuccess() {
            dismissProgress();
        }

        @Override
        public void onError() {
            dismissProgress();
        }
    };

    private final CropCallback mCropCallback = new CropCallback() {
        @Override
        public void onSuccess(Bitmap cropped) {
        }

        @Override
        public void onError() {
        }
    };

    private final SaveCallback mSaveCallback = new SaveCallback() {
        @Override
        public void onSuccess(Uri outputUri) {
            dismissProgress();
            ((CropImageActivity) getActivity()).startResultActivity(outputUri);
        }

        @Override
        public void onError() {
            dismissProgress();
        }
    };
}