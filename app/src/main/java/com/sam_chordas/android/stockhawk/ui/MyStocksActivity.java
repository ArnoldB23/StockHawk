package com.sam_chordas.android.stockhawk.ui;

import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.PeriodicTask;
import com.google.android.gms.gcm.Task;
import com.melnykov.fab.FloatingActionButton;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.QuoteCursorAdapter;
import com.sam_chordas.android.stockhawk.rest.RecyclerViewItemClickListener;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.sam_chordas.android.stockhawk.service.StockIntentService;
import com.sam_chordas.android.stockhawk.service.StockTaskService;
import com.sam_chordas.android.stockhawk.touch_helper.SimpleItemTouchHelperCallback;

import static android.support.v7.widget.RecyclerView.SCROLL_STATE_DRAGGING;
import static android.support.v7.widget.RecyclerView.SCROLL_STATE_IDLE;
import static android.support.v7.widget.RecyclerView.SCROLL_STATE_SETTLING;

public class MyStocksActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>, SharedPreferences.OnSharedPreferenceChangeListener{

  /**
   * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
   */
  private String LOG_TAG = MyStocksActivity.class.getSimpleName();
  /**
   * Used to store the last screen title. For use in {@link #restoreActionBar()}.
   */
  private CharSequence mTitle;
  private Intent mServiceIntent;
  private ItemTouchHelper mItemTouchHelper;
  private static final int CURSOR_LOADER_ID = 0;
  private QuoteCursorAdapter mCursorAdapter;
  public FloatingActionButton mFab;
  public SwipeRefreshLayout mSwipeRefreshLayout;
  public RecyclerView mRecyclerView;
  private Context mContext;
  public Handler mHandler;
  private Cursor mCursor;
  boolean isConnected;

  public static final int POSITION_REQUEST_CODE = 286;
  public static final String [] QUOTE_LOADER_COLUMNS  = new String[]{ QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.BIDPRICE,
          QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.ISUP, QuoteColumns.NAME };
  public static final String QUOTE_SORTORDER = QuoteColumns.SYMBOL + " ASC ";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mContext = this;

    isConnected = isConnected(mContext);
    final int sdk = Build.VERSION.SDK_INT;
    setContentView(R.layout.activity_main_stocks);
    // The intent service is for executing immediate pulls from the Yahoo API
    // GCMTaskService can only schedule tasks, they cannot execute immediately
    mServiceIntent = new Intent(this, StockIntentService.class);
    if (savedInstanceState == null){
      // Run the initialize task service so that some stocks appear upon an empty database
      mServiceIntent.putExtra("tag", "init");
      if (isConnected){
        startService(mServiceIntent);
      } else{
        networkToast();
      }
    }
    mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
    mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    getLoaderManager().initLoader(CURSOR_LOADER_ID, null, this);


    mHandler = new Handler();

    mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);

    mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
      @Override
      public void onRefresh() {
        Intent i = new Intent (mContext, StockIntentService.class);
        i.putExtra("tag", "periodic");
        isConnected = isConnected(mContext);
        if (isConnected){
          startService(i);
        } else{
          networkToast();
        }

        getLoaderManager().restartLoader(CURSOR_LOADER_ID, null, MyStocksActivity.this);
      }
    });


    mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
      @Override
      public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
        super.onScrollStateChanged(recyclerView, newState);

        switch (newState){
          case SCROLL_STATE_DRAGGING:
            break;

          case SCROLL_STATE_SETTLING:
            break;

          case SCROLL_STATE_IDLE:
            if (((LinearLayoutManager)(recyclerView.getLayoutManager())).findFirstCompletelyVisibleItemPosition() == 0 ||
                    mCursorAdapter.getItemCount() == 0)
            {
              mSwipeRefreshLayout.setEnabled(true);
            }
            else{
              mSwipeRefreshLayout.setEnabled(false);
            }
            break;
        }
      }

      @Override
      public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
        super.onScrolled(recyclerView, dx, dy);
      }
    });


    mCursorAdapter = new QuoteCursorAdapter(this, null);
    mRecyclerView.addOnItemTouchListener(new RecyclerViewItemClickListener(this,
            new RecyclerViewItemClickListener.OnItemClickListener() {
              @Override public void onItemClick(View v, int position) {
                //Start DetailStockActivity

                Cursor cursor = mCursorAdapter.getCursor();

                if (cursor.moveToPosition(position))
                {
                  int symbolIndex = cursor.getColumnIndex(QuoteColumns.SYMBOL);
                  String symbol = cursor.getString(symbolIndex);
                  Log.d(LOG_TAG, "RecyclerView onItemClick: position = " + position + ", symbol = " + symbol);

                  Intent i = new Intent(mContext, ScreenSlidePagerActivity.class);
                  i.putExtra(ScreenSlidePagerActivity.EXTRA_POSITION, position);

                  startActivityForResult(i, POSITION_REQUEST_CODE);
                }

              }
            }));


    mFab = (FloatingActionButton) findViewById(R.id.fab);
    mFab.attachToRecyclerView(mRecyclerView);
    mFab.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        isConnected = isConnected(mContext);
        if (isConnected){
          new MaterialDialog.Builder(mContext).title(R.string.symbol_search)
              .content(R.string.content_test)
              .inputType(InputType.TYPE_CLASS_TEXT)
              .input(R.string.input_hint, R.string.input_prefill, new MaterialDialog.InputCallback() {
                @Override public void onInput(MaterialDialog dialog, CharSequence input) {
                  // On FAB click, receive user input. Make sure the stock doesn't already exist
                  // in the DB and proceed accordingly
                  String inputStr = input.toString();
                  inputStr = inputStr.toUpperCase();
                  inputStr = inputStr.replaceAll("\\s+",""); //Remove any whitespace

                  Cursor c = getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                      new String[] { QuoteColumns.SYMBOL }, QuoteColumns.SYMBOL + "= ?",
                      new String[] { inputStr }, null);
                  if (c.getCount() != 0) {
                    Toast toast =
                        Toast.makeText(MyStocksActivity.this, R.string.invalid_stock_preexisting,
                            Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.CENTER, Gravity.CENTER, 0);
                    toast.show();
                    return;
                  } else {
                    // Add the stock to DB
                    mServiceIntent.putExtra("tag", "add");
                    mServiceIntent.putExtra("symbol", inputStr);
                    startService(mServiceIntent);

                    getLoaderManager().restartLoader(CURSOR_LOADER_ID, null, MyStocksActivity.this);
                  }
                }
              })
                  .backgroundColorRes(R.color.primary_text)
              .show();
        } else {
          networkToast();
        }

      }
    });

    mRecyclerView.setAdapter(mCursorAdapter);

    mCursorAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
      @Override
      public void onChanged() {
        super.onChanged();
        mFab.show();
      }
    });



    ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(mCursorAdapter);
    mItemTouchHelper = new ItemTouchHelper(callback);
    mItemTouchHelper.attachToRecyclerView(mRecyclerView);

    mTitle = getTitle();
    if (isConnected){
      long period = 3600L; //1 hr = 3600 sec
      long flex = 10L;
      String periodicTag = "periodic";

      // create a periodic task to pull stocks once every hour after the app has been opened. This
      // is so Widget data stays up to date.
      PeriodicTask periodicTask = new PeriodicTask.Builder()
          .setService(StockTaskService.class)
          .setPeriod(period)
          .setFlex(flex)
          .setTag(periodicTag)
          .setRequiredNetwork(Task.NETWORK_STATE_CONNECTED)
          .setRequiresCharging(false)
          .build();
      // Schedule task with tag "periodic." This ensure that only the stocks present in the DB
      // are updated.
      GcmNetworkManager.getInstance(this).schedule(periodicTask);
    }
  }

  @Override
  public void onPause()
  {
    mSwipeRefreshLayout.setRefreshing(false);
    SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
    sp.unregisterOnSharedPreferenceChangeListener(this);
    super.onPause();

  }


  @Override
  public void onResume() {
    SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
    sp.registerOnSharedPreferenceChangeListener(this);

    super.onResume();
    getLoaderManager().restartLoader(CURSOR_LOADER_ID, null, this);

    ActionBar actionBar = getSupportActionBar();
    actionBar.setSubtitle(getResources().getString(R.string.stocks_updated_text) + Utils.getLastSyncUpdate(mContext));
  }

  public void networkToast(){
    Toast.makeText(mContext, getString(R.string.network_toast), Toast.LENGTH_SHORT).show();
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    // Check which request we're responding to
    if (requestCode == POSITION_REQUEST_CODE) {
      // Make sure the request was successful
      if (resultCode == RESULT_OK) {
          int position = data.getExtras().getInt(ScreenSlidePagerActivity.EXTRA_POSITION);
          mRecyclerView.smoothScrollToPosition(position);
      }
    }
  }

  public void restoreActionBar() {
    ActionBar actionBar = getSupportActionBar();
    actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
    actionBar.setDisplayShowTitleEnabled(true);
    actionBar.setTitle(mTitle);
    actionBar.setSubtitle(getResources().getString(R.string.stocks_updated_text) + Utils.getLastSyncUpdate(mContext));
  }

  public static boolean isConnected(Context c)
  {
    ConnectivityManager cm = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

    return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
      getMenuInflater().inflate(R.menu.my_stocks, menu);
      restoreActionBar();
      return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();

    //noinspection SimplifiableIfStatement
    if (id == R.id.action_settings) {
      return true;
    }

    if (id == R.id.action_change_units){
      // this is for changing stock changes from percent value to dollar value
      Utils.showPercent = !Utils.showPercent;
      this.getContentResolver().notifyChange(QuoteProvider.Quotes.CONTENT_URI, null);
    }

    return super.onOptionsItemSelected(item);
  }

  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle args){

    return new CursorLoader(this, QuoteProvider.Quotes.CONTENT_URI,
        QUOTE_LOADER_COLUMNS,
        null,
        null,
        QUOTE_SORTORDER);
  }

  @Override
  public void onLoadFinished(Loader<Cursor> loader, Cursor data){
    mCursorAdapter.swapCursor(data);

    mHandler.postDelayed(new Runnable() {
      @Override
      public void run() {
        mSwipeRefreshLayout.setRefreshing(false);
      }
    }, 15000); //Turn of swipe-refresh after 15 seconds if no data updates from StockTaskService

  }

  @Override
  public void onLoaderReset(Loader<Cursor> loader){
    mCursorAdapter.swapCursor(null);
  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    if (key.equals(Utils.LASTSYNCUPDATE))
    {
      String updateStr = Utils.getLastSyncUpdate(mContext);
      ActionBar actionBar = getSupportActionBar();
      actionBar.setSubtitle(getResources().getString(R.string.stocks_updated_text) + updateStr);

      mSwipeRefreshLayout.setRefreshing(false);
    }
  }
}
