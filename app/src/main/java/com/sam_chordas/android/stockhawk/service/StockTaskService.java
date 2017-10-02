package com.sam_chordas.android.stockhawk.service;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.TaskParams;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.GraphColumns;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteDatabase;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.sam_chordas.android.stockhawk.ui.DetailStockFragment;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.StringTokenizer;

import static com.sam_chordas.android.stockhawk.rest.Utils.parseAndInsertHistoricalJson;

/**
 * Created by sam_chordas on 9/30/15.
 * The GCMTask service is primarily for periodic tasks. However, OnRunTask can be called directly
 * and is used for the initialization and adding task as well.
 */
public class StockTaskService extends GcmTaskService{
  private String LOG_TAG = StockTaskService.class.getSimpleName();

  private OkHttpClient client = new OkHttpClient();
  private Context mContext;
  private StringBuilder mStoredSymbols = new StringBuilder();
  private Handler mUIHandler;

  public static final String GRAPH_INTERVAL_1D = "1d";
  public static final String GRAPH_INTERVAL_5D = "5d";
  public static final String GRAPH_INTERVAL_1M = "1m";
  public static final String GRAPH_INTERVAL_1Y = "1y";
  public static final String ACTION_DATA_UPDATED = "com.sam_chordas.android.stockhawk.ACTION_DATA_UPDATED";

  public StockTaskService(){}

  public StockTaskService(Context context){
    mContext = context;
    mUIHandler = new Handler(Looper.getMainLooper());
  }

  String fetchData(String url) throws IOException{
    Request request = new Request.Builder()
        .url(url)
        .build();

    Response response = client.newCall(request).execute();
    return response.body().string();
  }

  @Override
  public int onRunTask(TaskParams params){

    if (mContext == null){
      mContext = this;
    }
    int result = GcmNetworkManager.RESULT_FAILURE;

      //Handle creating URL and fetching data from https://query.yahooapis.com, and inserting into QUOTES table
    if ( params.getTag().equals("init") || params.getTag().equals("periodic") || params.getTag().equals("add"))
    {
      String stockInputAdd = null;
      Cursor initQueryCursor = null;
      StringBuilder urlStringBuilder = new StringBuilder();
      try{
        // Base URL for the Yahoo query
        urlStringBuilder.append("https://query.yahooapis.com/v1/public/yql?q=");
        urlStringBuilder.append(URLEncoder.encode("select * from yahoo.finance.quotes where symbol "
                + "in (", "UTF-8"));
      } catch (UnsupportedEncodingException e) {
        e.printStackTrace();
      }
      if (params.getTag().equals("init") || params.getTag().equals("periodic")){
        initQueryCursor = mContext.getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                null, null,
                null, null);
        if (initQueryCursor.getCount() == 0 || initQueryCursor == null){
          // Init task. Populates DB with quotes for the symbols seen below
          try {
            urlStringBuilder.append(
                    URLEncoder.encode("\"AAPL\",\"AMZN\",\"FB\",\"GOOG\",\"JNJ\",\"KO\",\"MOO\",\"MSFT\",\"TSLA\",\"YHOO\")", "UTF-8"));
          } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
          }
        } else if (initQueryCursor != null){
          DatabaseUtils.dumpCursor(initQueryCursor);
          initQueryCursor.moveToFirst();
          for (int i = 0; i < initQueryCursor.getCount(); i++){
            mStoredSymbols.append("\""+
                    initQueryCursor.getString(initQueryCursor.getColumnIndex("symbol"))+"\",");

            initQueryCursor.moveToNext();
          }
          mStoredSymbols.replace(mStoredSymbols.length() - 1, mStoredSymbols.length(), ")");
          try {
            urlStringBuilder.append(URLEncoder.encode(mStoredSymbols.toString(), "UTF-8"));
          } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
          }

        }

      String selection = GraphColumns.UPDATE_TIMESTAMP + " NOT IN ( " +
              " Select " + GraphColumns.UPDATE_TIMESTAMP + " FROM " + QuoteDatabase.GRAPH_STOCK +
              " ORDER BY " + GraphColumns.UPDATE_TIMESTAMP + " DESC LIMIT 6000 ) ";

       int del =  mContext.getContentResolver().delete(QuoteProvider.Graph_Stock.CONTENT_URI, selection, null);
        if (del > 0)
        {
          Log.d(LOG_TAG, "Deleted the oldest stocks... " + del);
        }

      } else if (params.getTag().equals("add")){

        // get symbol from params.getExtra and build query
        stockInputAdd = params.getExtras().getString("symbol");
        try {
          urlStringBuilder.append(URLEncoder.encode("\""+stockInputAdd+"\")", "UTF-8"));
        } catch (UnsupportedEncodingException e){
          e.printStackTrace();
        }
      }
      // finalize the URL for the API query.
      urlStringBuilder.append("&format=json&diagnostics=true&env=store%3A%2F%2Fdatatables."
              + "org%2Falltableswithkeys&callback=");

      String urlString;
      String getResponse;


      if (urlStringBuilder != null){
        urlString = urlStringBuilder.toString();
        Log.d(LOG_TAG, "url: \"" + URLDecoder.decode(urlString) + "\"");
        try {
          getResponse = fetchData(urlString);
          result = GcmNetworkManager.RESULT_SUCCESS;


          ArrayList<ContentProviderOperation> operationArrayList = Utils.quoteJsonToContentVals(getResponse, mContext);

          if (operationArrayList != null) {
            mContext.getContentResolver().applyBatch(QuoteProvider.AUTHORITY, operationArrayList);

            if (params.getTag().equals("init"))
            {
              initQueryCursor = mContext.getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                      null, null,
                      null, null);

            }

            else if (params.getTag().equals("add"))
            {
              initQueryCursor = mContext.getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                      null, QuoteColumns.SYMBOL + " = ?", new String [] {stockInputAdd}, null);

            }

            update1dGraphData(initQueryCursor);

            Calendar cal = Calendar.getInstance();
            SimpleDateFormat df = new SimpleDateFormat(mContext.getResources().getString(R.string.update_time_format_default_locale));
            String updateStr = df.format(cal.getTime());
            Utils.setLastSyncUpdate(mContext, updateStr);

            Intent dateUpdated = new Intent(ACTION_DATA_UPDATED).setPackage(mContext.getPackageName());
            mContext.sendBroadcast(dateUpdated);
          }
          else if (operationArrayList == null && params.getTag().equals("add")){
            mUIHandler.post(new Runnable() {
              @Override
              public void run() {
                Toast.makeText(mContext, R.string.invalid_stock, Toast.LENGTH_LONG).show();
              }
            });
          }


        }
        catch (RemoteException | OperationApplicationException | IOException e){
          Log.e(LOG_TAG, "Error applying batch insert", e);
        }

      }
    }

    // Handle on demand graph data.
    // Create URL and fetch data for 1d, 5d, 1m, or 1yr graph types and insert into STOCK_GRAPH table
    else if (params.getTag().equals("graph") ){

        String stockInput = params.getExtras().getString("symbol");
        String rangeInput = params.getExtras().getString("range");//1d, 5d, 1m, 1yr

        //Build http://chartapi.finance.yahoo.com uri for 1d and 5d type graph
        Uri.Builder chartApiUriBuilder = new Uri.Builder();
        String parameters = "chartdata;type=quote;range=" + rangeInput + ";";
        chartApiUriBuilder.scheme("http")
                .authority("chartapi.finance.yahoo.com")
                .appendPath("instrument")
                .appendPath("1.0")
                .appendPath(stockInput)
                .appendEncodedPath(parameters)
                .appendPath("csv");
        String chartApiUriStr = chartApiUriBuilder.build().toString();

        //Build https://query.yahooapis.com uri for 1m or 1yr graph types.
        StringBuilder queryYahooApisStrBldr = new StringBuilder();
        queryYahooApisStrBldr.append("https://query.yahooapis.com/v1/public/yql?q=");



        if (rangeInput.equals(GRAPH_INTERVAL_1D) || rangeInput.equals(GRAPH_INTERVAL_5D))
        {
          Log.d(LOG_TAG, "graph: chartApiUriStr = " + chartApiUriStr);

          try{
            String getResponse = fetchData(chartApiUriStr);

            StringTokenizer st = new StringTokenizer(getResponse, "\n");

            ArrayList<ContentValues> cv_arraylist = new ArrayList<ContentValues>();


            int count = 0;
            while(st.hasMoreTokens())
            {
              String line = st.nextToken();

              if (!line.contains(":") && line.matches(".+,.+,.+,.+,.+,.+"))
              {
                String [] rowValues = line.split(",");
                ContentValues cv = new ContentValues();

                cv.put(GraphColumns._ID, stockInput.concat(rowValues[0]));
                cv.put(GraphColumns.STOCK_TIMESTAMP, rowValues[0]);
                cv.put(GraphColumns.CLOSE, rowValues[1]);
                cv.put(GraphColumns.SYMBOL, stockInput);
                cv.put(GraphColumns.UPDATE_TIMESTAMP, System.currentTimeMillis());
                cv.put(GraphColumns.GRAPH_TYPE, QuoteProvider.GraphType.DAYS);

                cv_arraylist.add(cv);
                count++;
              }
            }

            ContentValues[] cv_array = new ContentValues[cv_arraylist.size()];
            cv_arraylist.toArray(cv_array);
            int inserted = mContext.getContentResolver().bulkInsert(QuoteProvider.Graph_Stock.CONTENT_URI, cv_array);
            result = GcmNetworkManager.RESULT_SUCCESS;
            Log.d(LOG_TAG, "count = " + count + ", inserted = " + inserted);

          }catch(IOException e)
          {
            Log.e(LOG_TAG, "Error fetching data for graph!");
          }

        }

        else if (rangeInput.equals(GRAPH_INTERVAL_1M) || rangeInput.equals(GRAPH_INTERVAL_1Y))
        {
          Calendar cal = Calendar.getInstance();

          SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
          String endDate = df.format(cal.getTime());

          if (rangeInput.equals(GRAPH_INTERVAL_1M))
          {
            cal.add(Calendar.MONTH, -1);
          }
          else{
            cal.add(Calendar.YEAR, -1);
          }

          String startDate = df.format(cal.getTime());

          try {
            queryYahooApisStrBldr.append(URLEncoder.encode("select * from yahoo.finance.historicaldata where symbol = \""+ stockInput+"\"" +
                    " AND startDate = \"" + startDate + "\" AND endDate = \"" + endDate + "\"", "UTF-8"));
          } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
          }
          queryYahooApisStrBldr.append("&format=json&diagnostics=true&env=store%3A%2F%2Fdatatables."
                  + "org%2Falltableswithkeys&callback=");

          Log.d(LOG_TAG, "1M or 1YR: " + URLDecoder.decode(queryYahooApisStrBldr.toString()));

          try{
            String getResponse = fetchData(queryYahooApisStrBldr.toString());

            parseAndInsertHistoricalJson(getResponse, mContext);
            result = GcmNetworkManager.RESULT_SUCCESS;
          }catch(IOException e)
          {
            Log.e(LOG_TAG, "Error fetching data for graph!");
          }
        }

      LocalBroadcastManager.getInstance(mContext).sendBroadcast(new Intent(DetailStockFragment.GRAPH_UPDATED_ACTION));

      Cursor num = mContext.getContentResolver().query(QuoteProvider.Graph_Stock.CONTENT_URI, null, null, null, null);
      if (num != null)
      {
        Log.d(LOG_TAG, "Total number of stocks in Graph_Stock: " + num.getCount());
      }


    }

    return result;
  }


  private void update1dGraphData(Cursor c)
  {
    if (c == null)
    {
      Log.d(LOG_TAG, "update1dGraphData, cursor is null!");
      return;
    }

    c.moveToPosition(-1);
    String stockInput;

    while(c.moveToNext())
    {
      stockInput = c.getString(c.getColumnIndex(GraphColumns.SYMBOL));

      //Build http://chartapi.finance.yahoo.com uri for 1d and 5d type graph
      Uri.Builder chartApiUriBuilder = new Uri.Builder();
      String parameters = "chartdata;type=quote;range=" + GRAPH_INTERVAL_1D + ";";
      chartApiUriBuilder.scheme("http")
              .authority("chartapi.finance.yahoo.com")
              .appendPath("instrument")
              .appendPath("1.0")
              .appendPath(stockInput)
              .appendEncodedPath(parameters)
              .appendPath("csv");
      String chartApiUriStr = chartApiUriBuilder.build().toString();

      try{
        String getResponse = fetchData(chartApiUriStr);

        StringTokenizer st = new StringTokenizer(getResponse, "\n");

        ArrayList<ContentValues> cv_arraylist = new ArrayList<ContentValues>();


        int count = 0;
        while(st.hasMoreTokens())
        {
          String line = st.nextToken();

          if (!line.contains(":") && line.matches(".+,.+,.+,.+,.+,.+"))
          {

            String [] rowValues = line.split(",");
            ContentValues cv = new ContentValues();

            cv.put(GraphColumns._ID, stockInput.concat(rowValues[0]));
            cv.put(GraphColumns.STOCK_TIMESTAMP, rowValues[0]);
            cv.put(GraphColumns.CLOSE, rowValues[1]);
            cv.put(GraphColumns.SYMBOL, stockInput);
            cv.put(GraphColumns.UPDATE_TIMESTAMP, System.currentTimeMillis());
            cv.put(GraphColumns.GRAPH_TYPE, QuoteProvider.GraphType.DAYS);

            cv_arraylist.add(cv);
            count++;
          }
        }

        ContentValues[] cv_array = new ContentValues[cv_arraylist.size()];
        cv_arraylist.toArray(cv_array);
        int inserted = mContext.getContentResolver().bulkInsert(QuoteProvider.Graph_Stock.CONTENT_URI, cv_array);

        Log.d(LOG_TAG, stockInput +  ": count = " + count + ", inserted = " + inserted);

      }catch(IOException e)
      {
        Log.e(LOG_TAG, stockInput +  ": Error fetching data for graph! ");
      }
    }


  }

}
