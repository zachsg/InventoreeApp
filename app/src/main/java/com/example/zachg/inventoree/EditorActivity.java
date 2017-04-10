package com.example.zachg.inventoree;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.example.zachg.inventoree.data.ProductContract.ProductEntry;

public class EditorActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int EXISTING_PRODUCT_LOADER = 0;
    private Uri mCurrentProductUri;

    private boolean mProductChanged = false;

    /* EditText fields for gathering user input */
    private EditText mNameEditText;
    private EditText mPriceEditText;
    private EditText mStockEditText;

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
            getSupportActionBar().setTitle(R.string.editor_title_new);
        }
    }

    public void cancel(View view) {
        // Do nothing, just return to MainActivity parent
        finish();
    }

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
        } else {
            ContentValues values = new ContentValues();
            values.put(ProductEntry.COLUMN_PRODUCT_NAME, name);
            values.put(ProductEntry.COLUMN_PRODUCT_PRICE, price);
            values.put(ProductEntry.COLUMN_PRODUCT_STOCK, stock);

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

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String[] projection = {
                ProductEntry._ID,
                ProductEntry.COLUMN_PRODUCT_NAME,
                ProductEntry.COLUMN_PRODUCT_PRICE,
                ProductEntry.COLUMN_PRODUCT_STOCK
        };

        return new CursorLoader(this, mCurrentProductUri, projection, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data.moveToFirst()) {
            int nameCol = data.getColumnIndex(ProductEntry.COLUMN_PRODUCT_NAME);
            int priceCol = data.getColumnIndex(ProductEntry.COLUMN_PRODUCT_PRICE);
            int stockCol = data.getColumnIndex(ProductEntry.COLUMN_PRODUCT_STOCK);

            String name = data.getString(nameCol);
            Double price = data.getDouble(priceCol);
            Integer stock = data.getInt(stockCol);

            mNameEditText.setText(name);
            mPriceEditText.setText(price.toString());
            mStockEditText.setText(stock.toString());
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }
}
