package com.sam_chordas.android.stockhawk.widget;

import android.content.Intent;
import android.database.Cursor;
import android.os.Binder;
import android.os.Build;
import android.util.Log;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.sam_chordas.android.stockhawk.ui.MyStocksActivity;
import com.sam_chordas.android.stockhawk.ui.ScreenSlidePagerActivity;


/**
 * Created by Arnold on 11/28/2016.
 */

public class StockRemoteViewService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new RemoteViewsFactory() {
            private Cursor data = null;
            public static final String LOG_TAG = "StockRemoteViewService";

            @Override
            public void onCreate() {

            }

            @Override
            public void onDataSetChanged() {
                if ( data != null )
                {
                    data.close();
                }

                final long identityToken = Binder.clearCallingIdentity();

                data = getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI, MyStocksActivity.QUOTE_LOADER_COLUMNS, null, null, MyStocksActivity.QUOTE_SORTORDER);
                Binder.restoreCallingIdentity(identityToken);

                Log.d(LOG_TAG, "onDataSetChanged");
            }

            @Override
            public void onDestroy() {
                if (data != null)
                {
                    data.close();
                    data = null;
                }
            }

            @Override
            public int getCount() {
                return data == null ? 0 : data.getCount();
            }

            @Override
            public RemoteViews getViewAt(int position) {

                if (position == AdapterView.INVALID_POSITION ||
                        data == null || !data.moveToPosition(position)) {
                    return null;
                }

                Log.d(LOG_TAG, "getViewAt, position : "+ position);

                RemoteViews views = new RemoteViews(getPackageName(), R.layout.widget_list_item_quote);

                String symbol = data.getString(data.getColumnIndex(QuoteColumns.SYMBOL));
                String bid = data.getString(data.getColumnIndex(QuoteColumns.BIDPRICE));
                int isUp = data.getInt(data.getColumnIndex(QuoteColumns.ISUP));
                String pctChange = data.getString(data.getColumnIndex(QuoteColumns.PERCENT_CHANGE));
                String change = data.getString(data.getColumnIndex(QuoteColumns.CHANGE));
                String name = data.getString(data.getColumnIndex(QuoteColumns.NAME));

                int sdk = Build.VERSION.SDK_INT;

                views.setTextViewText(R.id.stock_symbol, symbol);
                views.setTextViewText(R.id.bid_price, bid);
                views.setContentDescription(R.id.list_item_quote, name + getApplicationContext().getResources().getString(R.string.stock_item_content)  + bid);


                if (isUp == 1)
                {
                    views.setInt(R.id.change, "setBackgroundResource", R.drawable.percent_change_pill_green);
                }
                else{
                    views.setInt(R.id.change, "setBackgroundResource", R.drawable.percent_change_pill_red);
                }

                if (Utils.showPercent)
                {
                    views.setTextViewText(R.id.change, pctChange);
                }
                else{
                    views.setTextViewText(R.id.change, change);
                }

                final Intent fillInIntent = new Intent();
                fillInIntent.putExtra(ScreenSlidePagerActivity.EXTRA_POSITION, position);

                views.setOnClickFillInIntent(R.id.list_item_quote, fillInIntent);


                return views;
            }

            @Override
            public RemoteViews getLoadingView() {
                return new RemoteViews(getPackageName(), R.layout.widget_list_item_quote);
            }

            @Override
            public int getViewTypeCount() {
                return 1;
            }

            @Override
            public long getItemId(int position) {

                    if (data.moveToPosition(position))
                    {

                        return Long.valueOf(data.getInt(data.getColumnIndex(QuoteColumns._ID)));
                    }

                    return position;
            }

            @Override
            public boolean hasStableIds() {
                return true;
            }
        };
    }
}
