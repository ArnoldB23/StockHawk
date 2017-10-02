package com.sam_chordas.android.stockhawk.ui;

import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.transition.Fade;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.GraphColumns;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.sam_chordas.android.stockhawk.service.StockIntentService;
import com.sam_chordas.android.stockhawk.service.StockTaskService;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

/**
 * Created by Arnold on 1/6/2017.
 */

public class DetailStockFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>{

    public static final String LOG_TAG = DetailStockFragment.class.getSimpleName();

    public static final String EXTRA_GRAPHSELECTION = "extra_graphselection";
    public static final String EXTRA_LOADERSELSTR = "extra_loaderselstr";
    public final int GRAPH_LOADER = 0;
    public final int CURRENT_PRICE_LOADER = 1;
    public static final String GRAPH_UPDATED_ACTION = "com.sam_chordas.android.stockhawk.GRAPH_UPDATED_ACTION";
    public final int sdk = Build.VERSION.SDK_INT;

    public Context mContext;
    public String mSymbol;
    public String mLoaderSelStr;
    public Handler mHandler;
    public BroadcastReceiver mGraphUpdatedBroadcastReceiver;


    public TextView mBidPriceTextView;
    public TextView mSymbolTextView;
    public TextView mChangeTextView;
    public TextView m1dTextView;
    public TextView m5dTextView;
    public TextView m1mTextView;
    public TextView m1yTextView;
    public TextView mNameTextView;
    public LineChart mLineChart;
    public ProgressBar mProgressBar;
    public TextView mOpenTextView;
    public TextView mLowTextView;
    public TextView mHighTextView;
    public TextView mMktCapTextView;
    public TextView mPERatioTextView;
    public TextView mDivYieldTextView;
    public LinearLayout mAnimateSelectionLayout;
    public LinearLayout mAnimateItemQuoteLayout;
    public RelativeLayout mAnimateRelativeLayout;
    public LinearLayout mListItemLayout;


    public String [] GRAPH_COLUMNS = {GraphColumns.STOCK_TIMESTAMP, GraphColumns.CLOSE};
    public static String [] CURRENT_PRICE_COLUMNS = { QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.BIDPRICE,
            QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.ISUP, QuoteColumns.NAME,
            QuoteColumns.OPEN, QuoteColumns.HIGH, QuoteColumns.LOW,
            QuoteColumns.MKT_CAP, QuoteColumns.PE_RATIO, QuoteColumns.DIV_YIELD};
    public static long YEAR_AND_HALF_AGO_SEC = 60 * 60 * 24 * 547;

    public enum GRAPH_SELECTION {m1D, m5D, m1M, m1Y}
    public GRAPH_SELECTION mGraphSelection;

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState)
    {
        savedInstanceState.putInt(EXTRA_GRAPHSELECTION, mGraphSelection.ordinal());
        savedInstanceState.putString(EXTRA_LOADERSELSTR, mLoaderSelStr);

        super.onSaveInstanceState(savedInstanceState);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){

        View rootView = inflater.inflate(R.layout.activity_detail_stock, container, false);


        mLineChart = (LineChart) rootView.findViewById(R.id.graph_linechart);

        mContext = getContext();
        mSymbol = getArguments().getString(QuoteColumns.SYMBOL);


        mHandler = new Handler();

        mSymbolTextView = (TextView)rootView.findViewById(R.id.detail_stock_symbol);
        mSymbolTextView.setText(mSymbol);
        mBidPriceTextView = (TextView)rootView.findViewById(R.id.detail_bid_price);
        mChangeTextView = (TextView)rootView.findViewById(R.id.detail_change);

        m1dTextView = (TextView) rootView.findViewById(R.id.one_day_textview);
        m5dTextView = (TextView) rootView.findViewById(R.id.five_day_textview);
        m1mTextView = (TextView) rootView.findViewById(R.id.one_month_textview);
        m1yTextView = (TextView) rootView.findViewById(R.id.one_year_textView);
        mNameTextView = (TextView) rootView.findViewById(R.id.stock_name);

        mProgressBar = (ProgressBar) rootView.findViewById(R.id.graph_progressbar);
        mOpenTextView = (TextView) rootView.findViewById(R.id.open_value_textview);
        mHighTextView = (TextView) rootView.findViewById(R.id.high_value_textview);
        mLowTextView = (TextView) rootView.findViewById(R.id.low_value_textview);
        mMktCapTextView = (TextView) rootView.findViewById(R.id.mkt_cap_value_textview);
        mPERatioTextView = (TextView) rootView.findViewById(R.id.pe_ratio_value_textview);
        mDivYieldTextView = (TextView) rootView.findViewById(R.id.div_yield_value_textview);

        mAnimateSelectionLayout = (LinearLayout) rootView.findViewById(R.id.grap_selection_linearlayout);
        mAnimateRelativeLayout = (RelativeLayout) rootView.findViewById(R.id.graph_extra_details_layout);
        mAnimateItemQuoteLayout = (LinearLayout) rootView.findViewById(R.id.detail_list_item_quote);
        mListItemLayout = (LinearLayout) rootView.findViewById(R.id.detail_linearlayout);

        if (savedInstanceState != null)
        {


            mGraphSelection = GRAPH_SELECTION.values()[savedInstanceState.getInt(EXTRA_GRAPHSELECTION)];
            mLoaderSelStr = savedInstanceState.getString(EXTRA_LOADERSELSTR);

            Log.d(LOG_TAG, "savedInstanceState restored: mGraphSelection = " + mGraphSelection + ", mLoaderSelStr = " + mLoaderSelStr );

            switch (mGraphSelection)
            {
                case m1D:
                    setSelectedTextColor(m1dTextView, true);
                    break;
                case m5D:
                    setSelectedTextColor(m5dTextView, true);
                    break;
                case m1M:
                    setSelectedTextColor(m1mTextView, true);
                    break;
                case m1Y:
                    setSelectedTextColor(m1yTextView, true);
                    break;
            }

        }
        else {
            mLoaderSelStr = getTimestampSQL(StockTaskService.GRAPH_INTERVAL_1D);
            mGraphSelection = GRAPH_SELECTION.m1D;
            setSelectedTextColor(m1dTextView, true);
        }

        mSymbolTextView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {

                if (mSymbolTextView.getText().toString().isEmpty())
                {
                    return false;
                }


                Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
                intent.putExtra(SearchManager.QUERY, mSymbolTextView.getText().toString() + getResources().getString(R.string.detail_stock_news_query));

                startActivity(intent);

                return true;
            }
        });

        m1dTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent i = new Intent(mContext, StockIntentService.class);
                i.putExtra("tag", "graph");
                i.putExtra("symbol", mSymbol);
                i.putExtra("range",StockTaskService.GRAPH_INTERVAL_1D);
                mContext.startService(i);

                mLoaderSelStr = getTimestampSQL(StockTaskService.GRAPH_INTERVAL_1D);
                mLineChart.clear();

                Log.d(LOG_TAG, "1d selected");

                mLineChart.setVisibility(View.INVISIBLE);
                mProgressBar.setVisibility(View.VISIBLE);

                setSelectedTextColor(m1dTextView, true);
                setSelectedTextColor(m5dTextView, false);
                setSelectedTextColor(m1mTextView, false);
                setSelectedTextColor(m1yTextView, false);

                getLoaderManager().destroyLoader(GRAPH_LOADER);//Allow only the broadcast from StockTaskService to restart Graph Loader

                mGraphSelection = GRAPH_SELECTION.m1D;

            }
        });



        m5dTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                Intent i = new Intent(mContext, StockIntentService.class);
                i.putExtra("tag", "graph");
                i.putExtra("symbol", mSymbol);
                i.putExtra("range",StockTaskService.GRAPH_INTERVAL_5D);
                mContext.startService(i);

                mLineChart.clear();

                mLoaderSelStr = getTimestampSQL(StockTaskService.GRAPH_INTERVAL_5D);

                Log.d(LOG_TAG, "5d selected");

                mLineChart.setVisibility(View.INVISIBLE);
                mProgressBar.setVisibility(View.VISIBLE);

                setSelectedTextColor(m1dTextView, false);
                setSelectedTextColor(m5dTextView, true);
                setSelectedTextColor(m1mTextView, false);
                setSelectedTextColor(m1yTextView, false);

                getLoaderManager().destroyLoader(GRAPH_LOADER);

                mGraphSelection = GRAPH_SELECTION.m5D;
            }
        });

        m1mTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                Intent i = new Intent(mContext, StockIntentService.class);
                i.putExtra("tag", "graph");
                i.putExtra("symbol", mSymbol);
                i.putExtra("range",StockTaskService.GRAPH_INTERVAL_1M);
                mContext.startService(i);

                mLineChart.clear();


                mLoaderSelStr = getTimestampSQL(StockTaskService.GRAPH_INTERVAL_1M);
                Log.d(LOG_TAG, "1m selected");

                mLineChart.setVisibility(View.INVISIBLE);
                mProgressBar.setVisibility(View.VISIBLE);

                setSelectedTextColor(m1dTextView, false);
                setSelectedTextColor(m5dTextView, false);
                setSelectedTextColor(m1mTextView, true);
                setSelectedTextColor(m1yTextView, false);

                getLoaderManager().destroyLoader(GRAPH_LOADER);
                mGraphSelection = GRAPH_SELECTION.m1M;
            }
        });

        m1yTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                Intent i = new Intent(mContext, StockIntentService.class);
                i.putExtra("tag", "graph");
                i.putExtra("symbol", mSymbol);
                i.putExtra("range",StockTaskService.GRAPH_INTERVAL_1Y);
                mContext.startService(i);

                mLineChart.clear();

                mLoaderSelStr = getTimestampSQL(StockTaskService.GRAPH_INTERVAL_1Y);
                Log.d(LOG_TAG, "1y selected");

                mLineChart.setVisibility(View.INVISIBLE);
                mProgressBar.setVisibility(View.VISIBLE);

                setSelectedTextColor(m1dTextView, false);
                setSelectedTextColor(m5dTextView, false);
                setSelectedTextColor(m1mTextView, false);
                setSelectedTextColor(m1yTextView, true);

                getLoaderManager().destroyLoader(GRAPH_LOADER);
                mGraphSelection = GRAPH_SELECTION.m1Y;
            }
        });


        mGraphUpdatedBroadcastReceiver = new DetailStockFragment.GraphUpdatedBroadcastReceiver();


        return rootView;

    }

    @Override
    public void onActivityCreated(Bundle saveInstanceState)
    {
        super.onActivityCreated(saveInstanceState);


    }

    @Override
    public void onPause()
    {
        super.onPause();

        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(mGraphUpdatedBroadcastReceiver);
    }


    @Override
    public void onResume()
    {
        super.onResume();

        LocalBroadcastManager.getInstance(mContext).registerReceiver(mGraphUpdatedBroadcastReceiver,
                new IntentFilter(GRAPH_UPDATED_ACTION));

        getLoaderManager().initLoader(GRAPH_LOADER, null, this);
        getLoaderManager().initLoader(CURRENT_PRICE_LOADER, null, this);

        if (sdk < Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
        {

            mNameTextView.setVisibility(View.VISIBLE);
            mAnimateRelativeLayout.setVisibility(View.VISIBLE);
            mAnimateSelectionLayout.setVisibility(View.VISIBLE);
            mAnimateItemQuoteLayout.setVisibility(View.VISIBLE);
        }
    }

    public class GraphUpdatedBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (isAdded())
            {
                getLoaderManager().restartLoader(GRAPH_LOADER, null, DetailStockFragment.this);
            }

        }
    }

    public void setSelectedTextColor(TextView selection, boolean isOn)
    {
        if (isOn)
        {
            int color = Color.rgb(140, 234, 255);
            selection.setTextColor(color);
        }
        else {
            TypedArray tp = mContext.obtainStyledAttributes(new int [] {android.R.attr.textColorSecondary});

            selection.setTextColor(tp.getColor(0,0));
            tp.recycle();
        }
    }

    public static String getTimestampSQL(String graphInterval)
    {
        String timestampSql = null;
        Calendar endCal = Calendar.getInstance();
        Calendar startCal = Calendar.getInstance();



        endCal.set(Calendar.HOUR_OF_DAY, 23);
        endCal.set(Calendar.MINUTE, 59);
        int dayOfWeek = endCal.get(Calendar.DAY_OF_WEEK);

        //Get latest weekday
        while ( dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY )
        {
            endCal.add(Calendar.DAY_OF_MONTH, -1);
            dayOfWeek = endCal.get(Calendar.DAY_OF_WEEK);

        }

        startCal.setTime(endCal.getTime());
        startCal.set(Calendar.HOUR_OF_DAY, 0);
        startCal.set(Calendar.MINUTE, 0);


        Log.d(LOG_TAG, "getTimestampSQL, Ending date = " + DateFormat.getInstance().format(endCal.getTime()));

        switch(graphInterval)
        {
            case StockTaskService.GRAPH_INTERVAL_1D:
            {
                timestampSql = GraphColumns.GRAPH_TYPE + " = '" + QuoteProvider.GraphType.DAYS + "'";
                break;
            }
            case StockTaskService.GRAPH_INTERVAL_5D:
            {
                for (int i = 0; i < 5; )
                {
                    startCal.add(Calendar.DAY_OF_MONTH, -1);
                    dayOfWeek = startCal.get(Calendar.DAY_OF_WEEK);
                    if ( dayOfWeek != Calendar.SATURDAY && dayOfWeek != Calendar.SUNDAY )
                    {
                        i++;
                    }
                }

                timestampSql = GraphColumns.GRAPH_TYPE + " = '" + QuoteProvider.GraphType.DAYS + "'";

                break;
            }
            case StockTaskService.GRAPH_INTERVAL_1M:
            {
                timestampSql = GraphColumns.GRAPH_TYPE + " = '" + QuoteProvider.GraphType.MONTHORYEAR + "'";
                startCal.add(Calendar.MONTH, -1);
                break;
            }
            case StockTaskService.GRAPH_INTERVAL_1Y:
            {
                timestampSql = GraphColumns.GRAPH_TYPE + " = '" + QuoteProvider.GraphType.MONTHORYEAR + "'";
                startCal.add(Calendar.YEAR, -1);
                break;
            }

        }

        Log.d(LOG_TAG, "getTimestampSQL, Starting date = " + DateFormat.getInstance().format(startCal.getTime()));
        timestampSql += " AND " + GraphColumns.STOCK_TIMESTAMP + " <= " + endCal.getTime().getTime() + " AND " + GraphColumns.STOCK_TIMESTAMP + " >= " + startCal.getTime().getTime();
        Log.d(LOG_TAG, "getTimestampSQL, timestampSql = " + timestampSql);

        return timestampSql;
    }

    public static Fragment newInstance()
    {
        Fragment f = new DetailStockFragment();

        return f;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        CursorLoader cursorLoader = null;


        switch (id)
        {
            case GRAPH_LOADER:
                Uri uri = QuoteProvider.Graph_Stock.withStockSymbol(mSymbol);
                Log.d(LOG_TAG, "URI: " + uri );

                cursorLoader = new CursorLoader(mContext, uri, GRAPH_COLUMNS,  mLoaderSelStr, null, GraphColumns.STOCK_TIMESTAMP + " ASC");
                break;

            case CURRENT_PRICE_LOADER:

                String where = QuoteColumns.SYMBOL + "=?";

                cursorLoader = new CursorLoader(mContext, QuoteProvider.Quotes.CONTENT_URI,
                        CURRENT_PRICE_COLUMNS,
                        where,
                        new String [] {mSymbol},
                        null);
                break;
        }



        return cursorLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        switch(loader.getId())
        {
            case GRAPH_LOADER:{

                int COL_TIMESTAMP = data.getColumnIndex(GraphColumns.STOCK_TIMESTAMP);
                int COL_CLOSE = data.getColumnIndex(GraphColumns.CLOSE);
                long currentUTC = System.currentTimeMillis();
                currentUTC = currentUTC / 1000;
                long toYearAndHalfAgo = currentUTC - YEAR_AND_HALF_AGO_SEC;
                float convertTimestamp;
                long timestamp;


                Log.d(LOG_TAG, "onLoadFinished Graph_Loader, cursor count = " + data.getCount() + ", " + mSymbol);
                ArrayList<Entry> entries = new ArrayList<Entry>();

                data.moveToPosition(-1);

                while(data.moveToNext())
                {
                    timestamp = Long.valueOf(data.getString(COL_TIMESTAMP) );
                    convertTimestamp =  (float) (timestamp - toYearAndHalfAgo);
                    String closeStr = data.getString(COL_CLOSE);

                    Calendar date = Calendar.getInstance();
                    date.setTimeInMillis(timestamp*1000);

                    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm a");
                    Log.d(LOG_TAG, "timestamp = " + timestamp + ", date timestamp = " + date.getTimeInMillis()/1000 + ", Date = " +  df.format(date.getTime()) + ", close = " + closeStr + ", " + mSymbol);
                    entries.add(new Entry(convertTimestamp, Float.valueOf(closeStr)));
                }

                if (!entries.isEmpty())
                {
                    if (mGraphSelection == GRAPH_SELECTION.m5D)
                    {
                        float lastTimestamp = entries.get(0).getX();
                        float tenMinDiff = 600f;
                        for (Entry entry : entries)
                        {
                            if ( (entry.getX() - lastTimestamp) > tenMinDiff)
                            {
                                entry.setX(lastTimestamp + tenMinDiff);
                            }

                            lastTimestamp = entry.getX();
                        }
                    }

                    LineDataSet dataSet = new LineDataSet(entries, getResources().getString(R.string.close_price_label_graph)); // add entries to dataset

                    dataSet.setCircleRadius(1.2f);
                    dataSet.setLineWidth(1.2f);


                    LineData lineData = new LineData(dataSet);
                    lineData.setDrawValues(false);
                    lineData.setHighlightEnabled(false);

                    mLineChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
                    mLineChart.getXAxis().setDrawAxisLine(true);
                    mLineChart.getXAxis().setDrawGridLines(false);
                    mLineChart.getXAxis().setDrawLabels(false);
                    mLineChart.getAxisLeft().setTextColor(Color.LTGRAY);
                    mLineChart.getAxisRight().setTextColor(Color.LTGRAY);
                    mLineChart.getLegend().setTextColor(Color.LTGRAY);
                    mLineChart.setScaleYEnabled(false);
                    mLineChart.setDescription(null);
                    mLineChart.animateX(500);
                    mLineChart.setData(lineData);
                    mLineChart.invalidate(); // refresh

                    mLineChart.fitScreen();
                }

                mProgressBar.setVisibility(View.INVISIBLE);
                mLineChart.setVisibility(View.VISIBLE);
            }break;

            case CURRENT_PRICE_LOADER:{

                if (data.moveToFirst())
                {
                    mBidPriceTextView.setText(data.getString(data.getColumnIndex(QuoteColumns.BIDPRICE)));
                    mChangeTextView.setText(data.getString(data.getColumnIndex(QuoteColumns.CHANGE)));
                    mNameTextView.setText(data.getString(data.getColumnIndex(QuoteColumns.NAME)));
                    mListItemLayout.setContentDescription(data.getString(data.getColumnIndex(QuoteColumns.NAME)));
                    mOpenTextView.setText(data.getString(data.getColumnIndex(QuoteColumns.OPEN)));
                    mHighTextView.setText(data.getString(data.getColumnIndex(QuoteColumns.HIGH)));
                    mLowTextView.setText(data.getString(data.getColumnIndex(QuoteColumns.LOW)));
                    mMktCapTextView.setText(data.getString(data.getColumnIndex(QuoteColumns.MKT_CAP)));
                    mPERatioTextView.setText(data.getString(data.getColumnIndex(QuoteColumns.PE_RATIO)));
                    mDivYieldTextView.setText(data.getString(data.getColumnIndex(QuoteColumns.DIV_YIELD)));

                    if (data.getInt(data.getColumnIndex("is_up")) == 1){
                        if (sdk < Build.VERSION_CODES.JELLY_BEAN){
                            mChangeTextView.setBackgroundDrawable(
                                    mContext.getResources().getDrawable(R.drawable.percent_change_pill_green));
                        }else {
                            mChangeTextView.setBackgroundDrawable(
                                    mContext.getResources().getDrawable(R.drawable.percent_change_pill_green));
                        }
                    } else{
                        if (sdk < Build.VERSION_CODES.JELLY_BEAN) {
                            mChangeTextView.setBackgroundDrawable(
                                    mContext.getResources().getDrawable(R.drawable.percent_change_pill_red));
                        } else{
                            mChangeTextView.setBackgroundDrawable(
                                    mContext.getResources().getDrawable(R.drawable.percent_change_pill_red));
                        }
                    }

                    if (Utils.showPercent){
                        mChangeTextView.setText(data.getString(data.getColumnIndex("percent_change")));
                    } else{
                        mChangeTextView.setText(data.getString(data.getColumnIndex("change")));
                    }


                    if (sdk > Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
                    {

                        ViewGroup transitionContainer = (ViewGroup) getView().findViewById(R.id.detail_linearlayout);
                        Fade fade = new Fade();
                        fade.setDuration(500);
                        TransitionManager.beginDelayedTransition(transitionContainer, fade);

                        mNameTextView.setVisibility(View.VISIBLE);
                        mAnimateRelativeLayout.setVisibility(View.VISIBLE);
                        mAnimateSelectionLayout.setVisibility(View.VISIBLE);
                        mAnimateItemQuoteLayout.setVisibility(View.VISIBLE);
                    }



                }

            }break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }
}
