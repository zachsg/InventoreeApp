package com.example.zachg.inventoree;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.example.zachg.inventoree.data.ProductContract.ProductEntry;

public class MainActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int LOADER = 0;
    ProductCursorAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ListView listView = (ListView) findViewById(R.id.list);
        View emptyView = findViewById(R.id.empty_text_view);
        listView.setEmptyView(emptyView);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent updateProductIntent = new Intent(view.getContext(), EditorActivity.class);
                Uri uri = ContentUris.withAppendedId(ProductEntry.CONTENT_URI, id);
                updateProductIntent.setData(uri);
                startActivity(updateProductIntent);
            }
        });

        mAdapter = new ProductCursorAdapter(this, null);
        listView.setAdapter(mAdapter);
        getSupportLoaderManager().initLoader(LOADER, null, this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.main_action_delete) {
            DialogInterface.OnClickListener deleteButtonClickListener =
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            // User clicked "Delete" button, navigate to parent activity.
                            int rowsDeleted = wipeData();
                            if (rowsDeleted > 0) {
                                getContentResolver().notifyChange(ProductEntry.CONTENT_URI, null);
                                Toast.makeText(getApplicationContext(),
                                        R.string.main_delete_success, Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(getApplicationContext(),
                                        R.string.main_delete_failure, Toast.LENGTH_SHORT).show();
                            }
                        }
                    };
            // Show a dialog that notifies the user they have unsaved changes
            confirmDeleteDialog(deleteButtonClickListener);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void confirmDeleteDialog(
            DialogInterface.OnClickListener discardButtonClickListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_all_dialog_msg);
        builder.setPositiveButton(R.string.dialog_delete, discardButtonClickListener);
        builder.setNegativeButton(R.string.dialog_abort, null);

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Add a new product when the fab button is clicked.
     * @param view the fab button.
     */
    public void addProduct(View view) {
        Intent intent = new Intent(MainActivity.this, EditorActivity.class);
        startActivity(intent);
    }

    /**
     * Wipes the database of all products.
     * @return the number of rows deleted.
     */
    int wipeData() {
        return getContentResolver().delete(ProductEntry.CONTENT_URI, null, null);
    }

    /**
     * For when BUY button is pressed on an individual list_item (product).
     * If stock for given item is greater than 0, then this method decrements the stock by 1.
     * @param view The BUY button for the given product clicked.
     */
    public void buyProduct(View view) {
        String[] projection = {
                ProductEntry.COLUMN_PRODUCT_STOCK
        };

        View parent = (View) view.getParent();
        ListView listView = (ListView) parent.getParent();
        final int id = listView.getPositionForView(parent);

        Uri uri = ContentUris.withAppendedId(ProductEntry.CONTENT_URI, mAdapter.getItemId(id));
        final int position = mAdapter.getCursor().getPosition();

        Cursor cursor =
                getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            Log.v("Button Pressed", "cursor not null...id is: " + id + ". pos is: " + position);
            if (cursor.getCount() > 0) {
                Log.v("Button Pressed", "cursor not empty...");
                cursor.moveToFirst();
                int stockCol = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_STOCK);
                int stock = cursor.getInt(stockCol);
                cursor.close();
                if (stock > 0) { // Decrement stock and update product in DB if greater than 0 stock
                    ContentValues values = new ContentValues();
                    values.put(ProductEntry.COLUMN_PRODUCT_STOCK, stock - 1);
                    getContentResolver().update(uri, values, null, null);
                    getContentResolver().notifyChange(uri, null);
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
                ProductEntry.COLUMN_PRODUCT_STOCK
        };

        return new CursorLoader(this, ProductEntry.CONTENT_URI, projection, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }
}
