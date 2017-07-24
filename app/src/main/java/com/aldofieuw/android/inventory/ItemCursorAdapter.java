package com.aldofieuw.android.inventory;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import static com.aldofieuw.android.inventory.data.ItemContract.ItemEntry;

public class ItemCursorAdapter extends CursorAdapter {

    public ItemCursorAdapter(Context context, Cursor c) {
        super(context, c, 0);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
    }

    @Override
    public void bindView(View view, final Context context, final Cursor cursor) {
        TextView nameTV = (TextView) view.findViewById(R.id.item_name);
        TextView quantityTV = (TextView) view.findViewById(R.id.quantity);
        TextView priceTV = (TextView) view.findViewById(R.id.price);
        ImageView saleButton = (ImageView) view.findViewById(R.id.sale_button);

        final int position = cursor.getPosition();

        saleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cursor.moveToPosition(position);

                int itemIdColumnIndex = cursor.getColumnIndex(ItemEntry._ID);
                final long itemId = cursor.getLong(itemIdColumnIndex);
                Uri mCurrentItemUri = ContentUris.withAppendedId(ItemEntry.CONTENT_URI, itemId);

                int quantityColumnIndex = cursor.getColumnIndex(ItemEntry.COLUMN_ITEM_QUANTITY);

                String itemQuantity = cursor.getString(quantityColumnIndex);
                int updateQuantity = Integer.parseInt(itemQuantity);

                if (updateQuantity > 0) {
                    Toast.makeText(context, R.string.reduce_by_one, Toast.LENGTH_SHORT).show();
                    updateQuantity--;
                    ContentValues updateValues = new ContentValues();
                    updateValues.put(ItemEntry.COLUMN_ITEM_QUANTITY, updateQuantity);
                    int rowsupdated = context.getContentResolver().update(mCurrentItemUri, updateValues, null, null);
                } else {
                    Toast.makeText(context, R.string.cant_reduce_zero, Toast.LENGTH_SHORT).show();
                }
            }
        });

        int nameColumnIndex = cursor.getColumnIndex(ItemEntry.COLUMN_ITEM_NAME);
        int quantityColumnIndex = cursor.getColumnIndex(ItemEntry.COLUMN_ITEM_QUANTITY);
        int priceColumnIndex = cursor.getColumnIndex(ItemEntry.COLUMN_ITEM_PRICE);

        String itemName = cursor.getString(nameColumnIndex);
        int itemQuantity = cursor.getInt(quantityColumnIndex);
        int itemPrice = cursor.getInt(priceColumnIndex);

        String quantityField = "Quantity: " + Integer.toString(itemQuantity);
        String priceField = "Price: € " + Integer.toString(itemPrice);

        nameTV.setText(itemName);
        quantityTV.setText(quantityField);
        priceTV.setText(priceField);
    }
}
