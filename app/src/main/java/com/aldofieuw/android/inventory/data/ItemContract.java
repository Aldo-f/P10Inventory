package com.aldofieuw.android.inventory.data;

import android.content.ContentResolver;
import android.graphics.Path;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.provider.BaseColumns;

public class ItemContract {

    private ItemContract() {
    }

    //content provider
    public static final String CONTENT_AUTHORITY = "com.aldofieuw.android.inventory";

    //base of URI's which apps will use to contact the content provider
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    //possible path
    public static final String PATH_ITEMS = "items";

    public static final class ItemEntry implements BaseColumns {
        //content uri to access item data in provider
        public static final Uri CONTENT_URI = Uri.withAppendedPath(BASE_CONTENT_URI, PATH_ITEMS);

        //MIME type of content uri for list of items
        public static final String CONTENT_LIST_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_ITEMS;

        //MIME type of content uri for single item
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_ITEMS;

        //DBName
        public static final String TABLE_NAME = "items";

        //unique if number for item
        //TYPE: Integer
        public static final String _ID = BaseColumns._ID;

        //name
        //TYPE: String
        public static final String COLUMN_ITEM_NAME = "name";

        //price
        //TYPE: Integer
        public static final String COLUMN_ITEM_PRICE = "price";

        //quantity
        //TYPE: Integer
        public static final String COLUMN_ITEM_QUANTITY = "quantity";

        //photo
        //TYPE: String
        public static final String COLUMN_ITEM_PHOTO = "photo";
    }
}
