package com.example.zachg.inventoree;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
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
        View emptyView = (TextView) findViewById(R.id.empty_view);
        listView.setEmptyView(emptyView);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, EditorActivity.class);
                startActivity(intent);
            }
        });

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
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.main_action_delete) {
            int rowsDeleted = wipeData();
            if (rowsDeleted > 0) {
                getContentResolver().notifyChange(ProductEntry.CONTENT_URI, null);
                Toast.makeText(this, R.string.main_delete_success, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, R.string.main_delete_failure, Toast.LENGTH_SHORT).show();
            }
        }

        return super.onOptionsItemSelected(item);
    }

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

        Uri uri = ContentUris.withAppendedId(ProductEntry.CONTENT_URI, id);
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
