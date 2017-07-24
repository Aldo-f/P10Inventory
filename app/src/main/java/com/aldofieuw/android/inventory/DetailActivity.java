package com.aldofieuw.android.inventory;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NavUtils;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.aldofieuw.android.inventory.data.ItemContract;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.aldofieuw.android.inventory.data.ItemProvider.LOG_TAG;

public class DetailActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int EXISTING_ITEM_LOADER = 0;

    private static final int PICK_IMAGE_REQUEST = 0;
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int MY_PERMISSIONS_REQUEST = 2;
    private Uri mUri;
    private Bitmap mBitmap;
    String mCurrentPhotoPath;
    private ImageView mImageView;
    private Button mButtonTakePicture;
    private boolean isgalleryPicture = false;

    private Uri mCurrentItemUri;

    private EditText mNameET;
    private EditText mQuantityET;
    private EditText mPriceET;
    private Button decreaseButton;
    private Button increaseButton;

    private boolean mItemHasChanged = false;

    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            mItemHasChanged = true;
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        //which intent was used : new or existing one
        Intent intent = getIntent();
        mCurrentItemUri = intent.getData();

        //intent doesn't contains item -> create new item
        if (mCurrentItemUri == null) {
            setTitle(getString(R.string.detail_activity_title_new_item));
            invalidateOptionsMenu();
        } else {
            setTitle(getString(R.string.detail_activity_title_edit_item));
            getLoaderManager().initLoader(EXISTING_ITEM_LOADER, null, this);
        }

        mNameET = (EditText) findViewById(R.id.edit_item_name);
        mQuantityET = (EditText) findViewById(R.id.edit_item_quantity);
        mPriceET = (EditText) findViewById(R.id.edit_item_price);
        mImageView = (ImageView) findViewById(R.id.photo);

        ViewTreeObserver viewTreeObserver = mImageView.getViewTreeObserver();
        viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mImageView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                mImageView.setImageBitmap(getBitmapFromUri(mUri));
            }
        });

        mButtonTakePicture = (Button) findViewById(R.id.take_photo_button);
        mButtonTakePicture.setEnabled(false);

        requestPermissions();

        mNameET.setOnTouchListener(mTouchListener);
        mQuantityET.setOnTouchListener(mTouchListener);
        mPriceET.setOnTouchListener(mTouchListener);

        increaseButton = (Button) findViewById(R.id.increase);
        decreaseButton = (Button) findViewById(R.id.decrease);

        decreaseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                decreaseQuantity();
            }
        });

        increaseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                increaseQuantity();
            }
        });
    }

    private void requestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE) ||
                    ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST);
            }
        } else {
            mButtonTakePicture.setEnabled(true);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    mButtonTakePicture.setEnabled(true);
                } else {
                    Toast.makeText(getApplicationContext(), "Permission Denied", Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }
    }

    public void openImageSelector(View view) {
        Intent intent;

        if (Build.VERSION.SDK_INT < 19) {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
        } else {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
        }
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    public void takePicture(View view) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        try {
            File file = createImageFile();
            mUri = FileProvider.getUriForFile(getApplication().getApplicationContext(),
                    "com.aldofieuw.android.inventory.fileprovider", file);
            intent.putExtra(MediaStore.EXTRA_OUTPUT,
                    mUri);
            startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM), "Camera");
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = "file:" + image.getAbsolutePath();
        return image;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            if (resultData != null) {
                mUri = resultData.getData();
                mBitmap = getBitmapFromUri(mUri);
                mImageView.setImageBitmap(mBitmap);
                mImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                isgalleryPicture = true;
            }
        } else if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            mBitmap = getBitmapFromUri(mUri);
            mImageView.setImageBitmap(mBitmap);
            mImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            isgalleryPicture = false;
        }
    }

    private Bitmap getBitmapFromUri(Uri uri) {
        if (uri == null) {
            return null;
        }
        int targetW = mImageView.getWidth();
        int targetH = mImageView.getHeight();
        ParcelFileDescriptor parcelFileDescriptor = null;
        try {
            parcelFileDescriptor =
                    getContentResolver().openFileDescriptor(uri, "r");
            FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();

            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeFileDescriptor(fileDescriptor, null, opts);
            int photoW = opts.outWidth;
            int photoH = opts.outHeight;

            int scaleFactor = Math.min(photoW / targetW, photoH / targetH);

            opts.inJustDecodeBounds = false;
            opts.inSampleSize = scaleFactor;
            opts.inPurgeable = true;
            Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor, null, opts);

            if (image.getWidth() > image.getHeight()) {
                Matrix mat = new Matrix();
                int degree = 90;
                mat.postRotate(degree);
                Bitmap imageRotate = Bitmap.createBitmap(image, 0, 0, image.getWidth(), image.getHeight(), mat, true);
                return imageRotate;
            } else {
                return image;
            }
        } catch (Exception e) {
            return null;
        } finally {
            try {
                if (parcelFileDescriptor != null) {
                    parcelFileDescriptor.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private void increaseQuantity() {
        String quantityString;

        if (TextUtils.isEmpty(mQuantityET.getText().toString().trim())) {
            quantityString = "0";
        } else {
            quantityString = mQuantityET.getText().toString().trim();
        }

        int quantity = Integer.parseInt(quantityString);
        quantity++;
        mQuantityET.setText(Integer.toString(quantity));
    }

    private void decreaseQuantity() {
        String quantityString;
        if (TextUtils.isEmpty(mQuantityET.getText().toString().trim())) {
            quantityString = "0";
        } else {
            quantityString = mQuantityET.getText().toString().trim();
        }

        int quantity = Integer.parseInt(quantityString);
        if (quantity == 0)
            mQuantityET.setText("0");
        else {
            quantity--;
            mQuantityET.setText(Integer.toString(quantity));
        }
    }

    private void checkFields() {
        //check
        String nameString = mNameET.getText().toString().trim();
        String priceString = mPriceET.getText().toString().trim();
        String quantityString = mQuantityET.getText().toString().trim();

        if (mCurrentItemUri == null && TextUtils.isEmpty(nameString) && TextUtils.isEmpty(priceString) && TextUtils.isEmpty(quantityString)) {
            finish();
            return;
        }

        if (TextUtils.isEmpty(nameString) || TextUtils.isEmpty(quantityString) ||
                TextUtils.isEmpty(priceString) || mUri == null || mUri.equals(Uri.EMPTY)) {
            Toast.makeText(this, R.string.fields_required,
                    Toast.LENGTH_SHORT).show();
        } else {
            saveItem();
            finish();
        }
    }

    private void saveItem() {
        String nameString = mNameET.getText().toString().trim();
        String priceString = mPriceET.getText().toString().trim();
        String quantityString = mQuantityET.getText().toString().trim();
        String photoString;

        if (mUri != null) {
            photoString = mUri.toString();
        } else {
            photoString = "";
        }

        ContentValues values = new ContentValues();
        values.put(ItemContract.ItemEntry.COLUMN_ITEM_NAME, nameString);
        int quantity = Integer.parseInt(quantityString);
        values.put(ItemContract.ItemEntry.COLUMN_ITEM_QUANTITY, quantity);
        int price = Integer.parseInt(priceString);
        values.put(ItemContract.ItemEntry.COLUMN_ITEM_PRICE, price);
        values.put(ItemContract.ItemEntry.COLUMN_ITEM_PHOTO, photoString);

        if (mCurrentItemUri == null) {
            Uri newUri = getContentResolver().insert(ItemContract.ItemEntry.CONTENT_URI, values);

            if (newUri == null)
                Toast.makeText(this, getString(R.string.editor_insert_item_failed), Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(this, getString(R.string.editor_insert_item_succes), Toast.LENGTH_SHORT).show();
        } else {
            int rowsAffected = getContentResolver().update(mCurrentItemUri, values, null, null);

            if (rowsAffected == 0)
                Toast.makeText(this, getString(R.string.editor_update_item_failed), Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(this, getString(R.string.editor_update_item_succes), Toast.LENGTH_SHORT).show();
        }
    }

    // e-mail intent to order more
    private void orderMore() {
        String nameString = mNameET.getText().toString().trim();
        String subject = "Order for " + nameString;
        String bodyText = "Greetings, " + "\n" + "We want to order more of: " + nameString +
                "\n" + "Please contact.";

        String mailto = "mailto:supplier@example.org" +
                "?cc=" + "me@example.com" +
                "&subject=" + Uri.encode(subject) +
                "&body=" + Uri.encode(bodyText);
      
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
        emailIntent.setData(Uri.parse(mailto));

        try {
            startActivity(emailIntent);
        } catch (ActivityNotFoundException e) {
            // Case where no email app is available
            Toast.makeText(this, R.string.suitable_app_failed, Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (mCurrentItemUri == null) {
            MenuItem menuItem = menu.findItem(R.id.action_delete);
            menuItem.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_save:
                checkFields();
                return true;
            case R.id.action_supplier:
                orderMore();
                return true;
            case R.id.action_delete:
                showDeleteConformationDialog();
                return true;
            case R.id.home:
                if (!mItemHasChanged) {
                    NavUtils.navigateUpFromSameTask(DetailActivity.this);
                    return true;
                }
                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                NavUtils.navigateUpFromSameTask(DetailActivity.this);
                            }
                        };
                showUnsavedChangesDialog(discardButtonClickListener);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        // If the pet hasn't changed, continue with handling back button press
        if (!mItemHasChanged) {
            super.onBackPressed();
            return;
        }

        // Otherwise if there are unsaved changes, setup a dialog to warn the user.
        // Create a click listener to handle the user confirming that changes should be discarded.
        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // User clicked "Discard" button, close the current activity.
                        finish();
                    }
                };

        // Show dialog that there are unsaved changes
        showUnsavedChangesDialog(discardButtonClickListener);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
        String[] projection = {
                ItemContract.ItemEntry._ID,
                ItemContract.ItemEntry.COLUMN_ITEM_NAME,
                ItemContract.ItemEntry.COLUMN_ITEM_QUANTITY,
                ItemContract.ItemEntry.COLUMN_ITEM_PRICE,
                ItemContract.ItemEntry.COLUMN_ITEM_PHOTO};

        return new CursorLoader(this,
                mCurrentItemUri,
                projection,
                null,
                null,
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (cursor == null || cursor.getCount() < 1) {
            return;
        }

        if (cursor.moveToFirst()) {
            int nameColumnIndex = cursor.getColumnIndex(ItemContract.ItemEntry.COLUMN_ITEM_NAME);
            int quantityColumnIndex = cursor.getColumnIndex(ItemContract.ItemEntry.COLUMN_ITEM_QUANTITY);
            int priceColumnIndex = cursor.getColumnIndex(ItemContract.ItemEntry.COLUMN_ITEM_PRICE);
            int photoColumnIndex = cursor.getColumnIndex(ItemContract.ItemEntry.COLUMN_ITEM_PHOTO);

            String itemName = cursor.getString(nameColumnIndex);
            int itemQuantity = cursor.getInt(quantityColumnIndex);
            int itemPrice = cursor.getInt(priceColumnIndex);
            String photo = cursor.getString(photoColumnIndex);

            mNameET.setText(itemName);
            mQuantityET.setText(Integer.toString(itemQuantity));
            mPriceET.setText(Integer.toString(itemPrice));

            if (!photo.isEmpty()) {
                mUri = Uri.parse(photo);
                mBitmap = getBitmapFromUri(mUri);
                mImageView.setImageBitmap(mBitmap);
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mNameET.setText("");
        mQuantityET.setText("");
        mPriceET.setText("");
    }

    private void showUnsavedChangesDialog(DialogInterface.OnClickListener discardButtonClickListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes_dialog_msg);
        builder.setPositiveButton(R.string.discard, discardButtonClickListener);
        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void showDeleteConformationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                deleteItem();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void deleteItem() {
        if (mCurrentItemUri != null) {
            int rowsDeleted = getContentResolver().delete(mCurrentItemUri, null, null);
            if (rowsDeleted == 0)
                Toast.makeText(this, R.string.editor_delete_item_failed, Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(this, R.string.editor_delete_item_success, Toast.LENGTH_SHORT).show();
        }
        finish();
    }
}
