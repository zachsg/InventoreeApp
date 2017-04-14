package com.example.zachg.inventoree;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.example.zachg.inventoree.data.ProductContract.ProductEntry;

/**
 * Adapter for a list that uses a product's data from inventory DB as its data source.
 * The adapter knows how to create a list_item rom a given product (row in inventory DB).
 */

public class ProductCursorAdapter extends CursorAdapter {

    public ProductCursorAdapter(Context context, Cursor cursor) {
        super(context, cursor, 0);
    }

    /**
     * Makes a new blank list item view. No data is set (or bound) to the views yet.
     * @param context The app's context.
     * @param cursor Cursor (at the location) from which to get the data.
     * @param parent Parent new View is attached to.
     * @return The created list_item.
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return super.getView(position, convertView, parent);
    }

    /**
     * Binds product's data at current row (already pointed to by cursor) to given list_item.
     * @param view View returned from newView method.
     * @param context The app's context.
     * @param cursor Cursor (at the location) from which to get the data.
     */
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        TextView nameView = (TextView) view.findViewById(R.id.product_label);
        TextView priceView = (TextView) view.findViewById(R.id.price_val);
        TextView stockView = (TextView) view.findViewById(R.id.stock_val);

        int nameCol = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_NAME);
        int priceCol = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_PRICE);
        int stockCol = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_STOCK);

        String name = cursor.getString(nameCol);
        Double price = cursor.getDouble(priceCol);
        Integer stock = cursor.getInt(stockCol);

        nameView.setText(name);
        priceView.setText(price.toString());
        stockView.setText(stock.toString());

        /* If a product is out of stock, hide the buy button */
        Button buyButton = (Button) view.findViewById(R.id.buy_button);
        if (stock == 0) {
            buyButton.setVisibility(View.INVISIBLE);
        } else {
            buyButton.setVisibility(View.VISIBLE);
        }
    }
}
