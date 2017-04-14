package com.example.zachg.inventoree.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.example.zachg.inventoree.data.ProductContract.ProductEntry;

/**
 * Helper class to initialize the inventory DB.
 */
public class ProductDBHelper extends SQLiteOpenHelper {

    // Database version
    private static final int DB_VERS = 1;

    // Database name
    private static final String DB_NAME = "inventory.db";

    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + ProductEntry.TABLE_NAME + " (" +
                    ProductEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    ProductEntry.COLUMN_PRODUCT_NAME + " TEXT NOT NULL," +
                    ProductEntry.COLUMN_PRODUCT_PRICE + " INTEGER NOT NULL," +
                    ProductEntry.COLUMN_PRODUCT_STOCK + " INTEGER NOT NULL," +
                    ProductEntry.COLUMN_PRODUCT_PHOTO + " BLOB);";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + ProductEntry.TABLE_NAME;

    public ProductDBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERS);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }
}
