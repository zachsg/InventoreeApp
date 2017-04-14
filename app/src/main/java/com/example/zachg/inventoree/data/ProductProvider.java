package com.example.zachg.inventoree.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringDef;
import android.util.Log;

import com.example.zachg.inventoree.data.ProductContract.ProductEntry;

/**
 * Provides an interface with which inventory DB can be interacted with.
 */
public class ProductProvider extends ContentProvider {

    // Tag for the log messages
    public static final String LOG_TAG = ProductProvider.class.getSimpleName();

    private ProductDBHelper mDbHelper;

    public static final int PRODUCTS = 1;
    public static final int PRODUCT_ID = 2;
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        sUriMatcher.addURI(ProductContract.CONTENT_AUTHORITY,
                ProductContract.PATH_PRODUCTS, PRODUCTS);
        sUriMatcher.addURI(ProductContract.CONTENT_AUTHORITY,
                ProductContract.PATH_PRODUCTS + "/#", PRODUCT_ID);
    }

    /**
     * Initialize ProductProvider and the database helper mDbHelper
     * @return
     */
    @Override
    public boolean onCreate() {
        mDbHelper = new ProductDBHelper(getContext());
        return true;
    }

    /**
     * Perform a query on the inventory DB with the provided parameters.
     * @param uri
     * @param projection
     * @param selection
     * @param selectionArgs
     * @param sortOrder
     * @return
     */
    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection,
                        @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        Cursor cursor = null;

        switch (sUriMatcher.match(uri)) {
            case PRODUCTS:
                cursor = db.query(ProductEntry.TABLE_NAME, projection,
                        selection, selectionArgs, null, null, sortOrder);
                break;
            case PRODUCT_ID:
                selection = ProductEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };

                cursor = db.query(ProductEntry.TABLE_NAME, projection,
                        selection, selectionArgs, null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException(LOG_TAG + ": Cannot query unknown URI " + uri);
        }

        /* Set notification URI on the cursor so we know what content URI cursor was made for.
         * If data at this URI changes, we know we need to update the cursor.
         * This will set the notification URI for any cursort returned from this query method.
         */
        if (getContext() != null) {
            cursor.setNotificationUri(getContext().getContentResolver(), uri);
        }

        return cursor;
    }

    /**
     * Returns the MIME type of data for the content URI.
     * @param uri the content URI
     * @return MIME type of passed content URI
     */
    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case PRODUCTS:
                break;
            case PRODUCT_ID:
                break;
            default:
                throw new IllegalStateException(LOG_TAG +
                        ": Unkonwn URI " + uri + " with match " + sUriMatcher.match(uri));
        }
        return null;
    }

    /**
     * Insert new data into the ProductProvider with the given values.
     * @param uri provides the necessary info to know where to route the request.
     * @param values the new values used to inserted into a new row.
     * @return the URI for the newly inserted row.
     */
    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        switch (sUriMatcher.match(uri)) {
            case PRODUCTS:
                return insertProduct(uri, values);
            default:
                throw new IllegalArgumentException(LOG_TAG +
                        ": Insertion not supported for " + uri);
        }
    }

    /**
     * Helper method to perform the actuall insertion of a new product into the inventory DB.
     * @param uri provides the necessary info to know where to route the request.
     * @param values provides the values to be inserted for the new product.
     * @return the URI of the product newly inserted into the inventory DB.
     */
    private Uri insertProduct(Uri uri, ContentValues values) {
        validate(values);

        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        long id = db.insert(ProductEntry.TABLE_NAME, null, values);
        db.close();
        if (id == -1) {
            Log.e(LOG_TAG, "Failed to insert new product into database");
            return null;
        }

        // Notify all listeners content has changed for product content URI
        if (getContext() != null) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return ContentUris.withAppendedId(uri, id);
    }

    /**
     * Delete the data at the given selection and selection arguments.
     * @param uri provides the necessary info to know where to route the request.
     * @param selection criteria to apply when filtering rows. If null all rows included.
     * @param selectionArgs applied as values for selection criteria when filtering rows.
     * @return the number of rows deleted.
     */
    @Override
    public int delete(@NonNull Uri uri,
                      @Nullable String selection, @Nullable String[] selectionArgs) {
        int rowsDeleted = 0;
        switch (sUriMatcher.match(uri)) {
            case PRODUCTS:
                rowsDeleted = deleteProduct(uri, selection, selectionArgs);
                break;
            case PRODUCT_ID:
                selection = ProductEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };
                rowsDeleted = deleteProduct(uri, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException(LOG_TAG + ": Delete not supported for " + uri);
        }

        return rowsDeleted;
    }

    /**
     * Helper method to delete values from inventory DB identified by selection and selectionArgs.
     * @param uri provides the necessary info to know where to route the request.
     * @param selection criteria to apply when filtering rows. If null all rows included.
     * @param selectionArgs applied as values for selection criteria when filtering rows.
     * @return the number of rows deleted.
     */
    private int deleteProduct(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        int rowsDeleted = db.delete(ProductEntry.TABLE_NAME, selection, selectionArgs);
        db.close();
        if (rowsDeleted <= 0) {
            Log.e(LOG_TAG, "Failed to update UI (and notify listeners) when delete called");
        }

        return rowsDeleted;
    }

    /**
     * Updates the data at the given selection and selection arguments, with the new ContentValues.
     * @param uri provides the necessary info to know where to route the request.
     * @param values the new values used to updated existing rows.
     * @param selection criteria to apply when filtering rows. If null all rows included.
     * @param selectionArgs applied as values for selection criteria when filtering rows.
     * @return the number of rows updated.
     */
    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values,
                      @Nullable String selection, @Nullable String[] selectionArgs) {
        int rowsUpdated = 0;
        switch (sUriMatcher.match(uri)) {
            case PRODUCTS:
                rowsUpdated = updateProduct(uri, values, selection, selectionArgs);
                break;
            case PRODUCT_ID:
                selection = ProductEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };
                rowsUpdated = updateProduct(uri, values, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException(LOG_TAG + ": Update not suppored for " + uri);
        }

        return rowsUpdated;
    }

    /**
     * Helpfer method to update product with specified values in inventory DB.
     * @param uri provides the necessary info to know where to route the request.
     * @param values the new values used to updated existing rows.
     * @param selection criteria to apply when filtering rows. If null all rows included.
     * @param selectionArgs applied as values for selection criteria when filtering rows.
     * @return the number of rows updated.
     */
    private int updateProduct(Uri uri, ContentValues values,
                              String selection, String[] selectionArgs) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        int rowsUpdated = db.update(ProductEntry.TABLE_NAME, values, selection, selectionArgs);
        db.close();
        if (rowsUpdated <= 0) {
            Log.e(LOG_TAG, "Failed to update UI (and notify listeners) when update called");
        }

        return rowsUpdated;
    }

    /**
     * Ensure the provided values to be inserted or updated in the inventory DB are valid.
     * @param values the ContentValues to be checked for integrity.
     */
    private void validate(ContentValues values) {
        if (values.containsKey(ProductEntry.COLUMN_PRODUCT_NAME)) {
            if (values.getAsString(ProductEntry.COLUMN_PRODUCT_NAME) == null) {
                throw new IllegalArgumentException(LOG_TAG + ": A product requires a name");
            }
        }

        if (values.containsKey(ProductEntry.COLUMN_PRODUCT_PRICE)) {
            if (values.getAsDouble(ProductEntry.COLUMN_PRODUCT_PRICE) < 0.0) {
                throw new IllegalArgumentException(LOG_TAG +
                        ": A product requires a price of at least $0");
            }
        }

        if (values.containsKey(ProductEntry.COLUMN_PRODUCT_STOCK)) {
            if (values.getAsInteger(ProductEntry.COLUMN_PRODUCT_STOCK) < 0) {
                throw new IllegalArgumentException(LOG_TAG +
                        ": A product cannot have a quantity of less than 0");
            }
        }
    }
}
