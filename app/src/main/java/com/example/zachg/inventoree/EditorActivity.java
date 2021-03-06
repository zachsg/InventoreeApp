package com.example.zachg.inventoree;

import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.NavUtils;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.example.zachg.inventoree.data.ProductContract.ProductEntry;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class EditorActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int PHOTO_SELECT = 100;

    private static final int EXISTING_PRODUCT_LOADER = 0;
    private Uri mCurrentProductUri;

    // Store the image the user selects
    private Bitmap mImageMap;

    private boolean mProductChanged = false;

    /* EditText fields for gathering user input */
    private ImageButton mImageButton;
    private EditText mNameEditText;
    private EditText mPriceEditText;
    private EditText mStockEditText;

    /* Track the current quantity of a given product */
    private int mCurrentStock = 0;

    /** Tag for the log messages */
    public static final String LOG_TAG = EditorActivity.class.getSimpleName();

    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            mProductChanged = true;
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        Intent intent = getIntent();
        Uri uri = intent.getData();

        mImageButton = (ImageButton) findViewById(R.id.image_button);
        mNameEditText = (EditText) findViewById(R.id.product_name_input);
        mPriceEditText = (EditText) findViewById(R.id.product_price_input);
        mStockEditText = (EditText) findViewById(R.id.product_stock_input);

        mNameEditText.setOnTouchListener(mTouchListener);
        mPriceEditText.setOnTouchListener(mTouchListener);
        mStockEditText.setOnTouchListener(mTouchListener);

        // Change title based on whether entering new product or editing existing
        if (uri != null) {
            getSupportActionBar().setTitle(R.string.editor_title_edit);
            mCurrentProductUri = uri;
            getSupportLoaderManager().initLoader(EXISTING_PRODUCT_LOADER, null, this);
        } else {
            mCurrentProductUri = null;
            getSupportActionBar().setTitle(R.string.editor_title_new);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        if (mCurrentProductUri != null) {
            getMenuInflater().inflate(R.menu.menu_editor, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.editor_action_delete:
                if (mCurrentProductUri != null) {
                    DialogInterface.OnClickListener deleteButtonClickListener =
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    // User clicked "Delete" button, navigate to parent activity.
                                    int rowsDeleted =
                                            getContentResolver().delete(mCurrentProductUri, null, null);
                                    if (rowsDeleted > 0) {
                                        getContentResolver().notifyChange(mCurrentProductUri, null);
                                        Toast.makeText(getApplicationContext(),
                                                R.string.editor_delete_success,
                                                Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(getApplicationContext(),
                                                R.string.editor_delete_failure,
                                                Toast.LENGTH_SHORT).show();
                                    }
                                    finish();
                                }
                            };
                    // Show a dialog that notifies the user they have unsaved changes
                    confirmDeleteDialog(deleteButtonClickListener);
                    return true;
                }
                finish();
            case android.R.id.home:
                if (!mProductChanged) {
                    NavUtils.navigateUpFromSameTask(EditorActivity.this);
                    return true;
                }

                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // User clicked "Discard" button, navigate to parent activity.
                                NavUtils.navigateUpFromSameTask(EditorActivity.this);
                            }
                        };

                // Show a dialog that notifies the user they have unsaved changes
                showUnsavedChangesDialog(discardButtonClickListener);
                return true;
        }
        finish();

        return super.onOptionsItemSelected(item);
    }

    /**
     * Dialog for when user is trying to perform destructive delete action on product.
     */
    private void confirmDeleteDialog(
            DialogInterface.OnClickListener discardButtonClickListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton(R.string.dialog_delete, discardButtonClickListener);
        builder.setNegativeButton(R.string.dialog_abort, null);

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Dialog for when user is trying to exit activity with unsaved changes.
     */
    private void showUnsavedChangesDialog(
            DialogInterface.OnClickListener discardButtonClickListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes_dialog_msg);
        builder.setPositiveButton(R.string.discard, discardButtonClickListener);
        builder.setNegativeButton(R.string.keep_editing, null);

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Handle when back button is pressed.  If there are unsaved changes, confirm discard
     * of those changes via dialog.  If no unsaved changes, then just finish activity and return
     * to parent/caller.
     */
    @Override
    public void onBackPressed() {
        if (!mProductChanged) {
            super.onBackPressed();
            return;
        }

        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // User clicked "Discard" button, close the current activity.
                        finish();
                    }
                };
        showUnsavedChangesDialog(discardButtonClickListener);
    }

    /**
     * When cancel button is pressed.  If unsaved changes, confirm discard of those changes
     * via dialog.  If no unsaved changes, then just finsih activity & return to parent/caller.
     */
    public void cancel(View view) {
        if (!mProductChanged) {
            finish();
            return;
        }

        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // User clicked "Discard" button, close the current activity.
                        finish();
                    }
                };
        showUnsavedChangesDialog(discardButtonClickListener);
    }

    /**
     * When order button is pressed, start mail intent to handle action.
     * Performs necessary checks for valid product (has a name, has a > 0 quantity).
     * Triggers intent for email and includes email subject, body, and to-address for callee.
     */
    public void order(View view) {
        String productName = mNameEditText.getText().toString().trim();
        int stock = 0;
        if (mStockEditText.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, R.string.editor_oder_no_stock, Toast.LENGTH_SHORT).show();
        } else {
            stock = Integer.parseInt(mStockEditText.getText().toString().trim());

            if (productName.isEmpty()) {
                Toast.makeText(this, R.string.no_name_product, Toast.LENGTH_SHORT).show();
            } else {
                Intent sendMail = new Intent(Intent.ACTION_SENDTO);
                String uriAsString = "mailto:" + Uri.encode("supplieremail@company.com") +
                        "?subject=" + Uri.encode("Order request for " + productName) +
                        "&body=" + Uri.encode("We'd like to place an order for " + stock +
                        " of the " + productName + " product.");
                sendMail.setData(Uri.parse(uriAsString));
                startActivity(Intent.createChooser(sendMail, "Send email"));
            }
        }
    }

    /**
     * When save is pressed, saves the product to the database.
     * If the product is new, then insert is run to create a new row.
     * If the product exists, then update is called to modify the existing row.
     */
    public void save(View view) {
        String name = mNameEditText.getText().toString().trim();

        Double price = 0.0;
        if (!mPriceEditText.getText().toString().trim().isEmpty()) {
            price = Double.parseDouble(mPriceEditText.getText().toString().trim());
        }

        Integer stock = 0;
        if (!mStockEditText.getText().toString().trim().isEmpty()) {
            stock = Integer.parseInt(mStockEditText.getText().toString().trim());
        }

        if (name.isEmpty()) {
            Toast.makeText(this, R.string.no_name_product, Toast.LENGTH_SHORT).show();
        } else if (mPriceEditText.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, R.string.no_price_product, Toast.LENGTH_SHORT).show();
        } else if (mStockEditText.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, R.string.no_stock_product, Toast.LENGTH_SHORT).show();
        } else if (mImageButton.getDrawable().getConstantState().equals(
                    ResourcesCompat.getDrawable(getResources(), R.mipmap.ic_product_image, null
                    ).getConstantState())) {
                Toast.makeText(this, R.string.no_image_product, Toast.LENGTH_SHORT).show();
        } else {
            ContentValues values = new ContentValues();
            values.put(ProductEntry.COLUMN_PRODUCT_NAME, name);
            values.put(ProductEntry.COLUMN_PRODUCT_PRICE, price);
            values.put(ProductEntry.COLUMN_PRODUCT_STOCK, stock);
            if (mImageMap != null) {
                ByteArrayOutputStream bOS = new ByteArrayOutputStream();

                // Compress the image so it's faster & easier to store
                mImageMap.compress(Bitmap.CompressFormat.JPEG, 100, bOS);

                // Convert to byte array & store the blob in the database
                byte[] image = bOS.toByteArray();
                values.put(ProductEntry.COLUMN_PRODUCT_PHOTO, image);
            }

            if (mCurrentProductUri != null) { // Updating and existing product in inventory DB
                int rowsUpdated =
                        getContentResolver().update(mCurrentProductUri, values, null, null);
                if (rowsUpdated > 0) {
                    getContentResolver().notifyChange(mCurrentProductUri, null);
                    Toast.makeText(this, R.string.product_update_success, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, R.string.product_update_failure, Toast.LENGTH_SHORT).show();
                }
            } else { // Saving a brand new product to inventory DB
                Uri uri = getContentResolver().insert(ProductEntry.CONTENT_URI, values);
                if (uri != null) {
                    Toast.makeText(this, R.string.product_save_success, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, R.string.product_save_failure, Toast.LENGTH_SHORT).show();
                }
            }
            finish();
        }
    }

    /**
     * Used for decrementing the stock of a given product by one each time pressed.  Will not go
     * below 0, but has no upper limit.
     */
    public void decrementStock(View view) {
        if (!mStockEditText.getText().toString().trim().isEmpty()) {
            mCurrentStock = Integer.parseInt(mStockEditText.getText().toString().trim());
        }
        if (mCurrentStock > 0) {
            mCurrentStock--;
            mStockEditText.setText(Integer.toString(mCurrentStock));
        }
    }

    /**
     * Used for incrementing the stock of a given product by one each time pressed.
     */
    public void incrementStock(View view) {
        if (!mStockEditText.getText().toString().trim().isEmpty()) {
            mCurrentStock = Integer.parseInt(mStockEditText.getText().toString().trim());
        }
        mCurrentStock++;
        mStockEditText.setText(Integer.toString(mCurrentStock));
    }

    public void openImageChooser(View view) {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Picture Selector"), PHOTO_SELECT);
    }

    public void onActivityResult(int request, int result, Intent data) {
        if (result == RESULT_OK) {
            if (request == PHOTO_SELECT) {
                // Get the url & set the image to imageButton
                Uri imageUri = data.getData();
                if (imageUri != null) {
                    try {
                        InputStream input = getContentResolver().openInputStream(imageUri);
                        mImageMap = BitmapFactory.decodeStream(input);

                        /* Instead of storing full sized image, scale it down to the size we'll
                         * display it at anyway.
                         */
                        mImageMap = Bitmap.createScaledBitmap(mImageMap, 150, 150, false);

                        // Set imageview to the image selected
                        mImageButton.setImageBitmap(mImageMap);
                    } catch (FileNotFoundException fne) {
                        Log.e(LOG_TAG, "Unable to locate file with URI " + imageUri);
                    }
                }
            }
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String[] projection = {
                ProductEntry._ID,
                ProductEntry.COLUMN_PRODUCT_NAME,
                ProductEntry.COLUMN_PRODUCT_PRICE,
                ProductEntry.COLUMN_PRODUCT_STOCK,
                ProductEntry.COLUMN_PRODUCT_PHOTO
        };

        return new CursorLoader(this, mCurrentProductUri, projection, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data.moveToFirst()) {
            int nameCol  = data.getColumnIndex(ProductEntry.COLUMN_PRODUCT_NAME);
            int priceCol = data.getColumnIndex(ProductEntry.COLUMN_PRODUCT_PRICE);
            int stockCol = data.getColumnIndex(ProductEntry.COLUMN_PRODUCT_STOCK);
            int imageCol = data.getColumnIndex(ProductEntry.COLUMN_PRODUCT_PHOTO);

            String name = data.getString(nameCol);
            Double price = data.getDouble(priceCol);
            Integer stock = data.getInt(stockCol);

            if (data.getBlob(imageCol) != null) {
                // Convert the image's byte array from the database into a usable bitmap
                byte[] b = data.getBlob(imageCol);
                Bitmap imageMap = BitmapFactory.decodeByteArray(b, 0, b.length);
                mImageButton.setImageBitmap(imageMap);
            }

            mNameEditText.setText(name);
            mPriceEditText.setText(price.toString());
            mStockEditText.setText(stock.toString());
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) { }
}
