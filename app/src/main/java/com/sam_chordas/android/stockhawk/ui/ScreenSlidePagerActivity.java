package com.sam_chordas.android.stockhawk.ui;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;

/**
 * Created by Arnold on 1/6/2017.
 */

public class ScreenSlidePagerActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>{

    private ViewPager mViewPager;
    private CursorPagerAdapter mCursorPagerAdapter;
    private int mPosition;
    private Context mContext;
    private boolean firstLoad = true;

    public static final String EXTRA_POSITION = "extra_position";
    public static final String LOG_TAG = ScreenSlidePagerActivity.class.getSimpleName();

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState)
    {
        savedInstanceState.putInt(EXTRA_POSITION, mPosition);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_slide_stock);
        mContext = getApplicationContext();

        if (savedInstanceState != null)
        {
            mPosition = savedInstanceState.getInt(EXTRA_POSITION);
        }
        else{
            mPosition = getIntent().getExtras().getInt(EXTRA_POSITION);
        }

        mViewPager = (ViewPager) findViewById(R.id.viewpager);
        mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            private int ready = 0;

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int pos) {

                if (ready > 0) {
                    mPosition = pos;
                }
                ready = 0;
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                switch (state) {
                    case ViewPager.SCROLL_STATE_DRAGGING:
                        ready = 1;
                        break;
                    case ViewPager.SCROLL_STATE_IDLE:
                        break;
                    case ViewPager.SCROLL_STATE_SETTLING:
                        break;
                }
            }
        });

        mCursorPagerAdapter = new CursorPagerAdapter(getSupportFragmentManager(), DetailStockFragment.class, MyStocksActivity.QUOTE_LOADER_COLUMNS, null);
        mViewPager.setAdapter(mCursorPagerAdapter);

        getLoaderManager().initLoader(0, null, ScreenSlidePagerActivity.this);
    }

    @Override
    public void finish()
    {
        Intent result = new Intent();
        result.putExtra(EXTRA_POSITION, mPosition);
        setResult(Activity.RESULT_OK, result);

        super.finish();
    }


    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(mContext, QuoteProvider.Quotes.CONTENT_URI, MyStocksActivity.QUOTE_LOADER_COLUMNS, null, null, MyStocksActivity.QUOTE_SORTORDER);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mCursorPagerAdapter.swapCursor(data);

        if(data != null && data.moveToPosition(mPosition) && firstLoad == true)
        {
            mViewPager.setCurrentItem(mPosition);
            Log.d(LOG_TAG, "onLoadFinished: position = " + mPosition + ", current item = " + mViewPager.getCurrentItem() +
            ", symbol = "+data.getString(data.getColumnIndex(QuoteColumns.SYMBOL)));
            firstLoad = false;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mCursorPagerAdapter.swapCursor(null);

    }
}
