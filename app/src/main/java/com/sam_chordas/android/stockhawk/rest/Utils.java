package com.sam_chordas.android.stockhawk.rest;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.GraphColumns;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Random;

/**
 * Created by sam_chordas on 10/8/15.
 */
public class Utils {

  private static String LOG_TAG = Utils.class.getSimpleName();

  public static boolean showPercent = true;
    public static final String LASTSYNCUPDATE = "LASTSYNCUPDATE";

  public static void parseAndInsertHistoricalJson(String JSON, Context context)
  {
      ArrayList<ContentValues> cv_arraylist = new ArrayList<>();

      try {
          JSONObject jsonObject = new JSONObject(JSON);


          jsonObject = jsonObject.getJSONObject("query");
          int count = Integer.valueOf(jsonObject.getString("count"));

          JSONArray resultsArray = jsonObject.getJSONObject("results").getJSONArray("quote");

          String symbol = null;
          for (int i = 0; i < count; i++)
          {
              jsonObject = resultsArray.getJSONObject(i);

              String date = jsonObject.getString("Date");
              String close = jsonObject.getString("Close");
              symbol = jsonObject.getString("Symbol");

              if (date == null || close == null)
              {
                  Log.d(LOG_TAG, "parseAndInsertHistoricalJson, date or close values are null");
                  continue;
              }

              Calendar cal = Calendar.getInstance();
              String [] datefields = date.split("-");

              cal.clear();
              cal.set(Integer.valueOf(datefields[0]),Integer.valueOf(datefields[1])-1,Integer.valueOf(datefields[2]));
              String timestamp = String.valueOf(cal.getTimeInMillis()/1000);


              ContentValues cv = new ContentValues();
              cv.put(GraphColumns._ID, symbol.concat(timestamp));
              cv.put(GraphColumns.SYMBOL, symbol);
              cv.put(GraphColumns.STOCK_TIMESTAMP, timestamp);
              cv.put(GraphColumns.CLOSE, close);
              cv.put(GraphColumns.UPDATE_TIMESTAMP, System.currentTimeMillis());
              cv.put(GraphColumns.GRAPH_TYPE, QuoteProvider.GraphType.MONTHORYEAR);

              cv_arraylist.add(cv);
          }

          ContentValues [] cv_array = new ContentValues[cv_arraylist.size()];
          cv_arraylist.toArray(cv_array);
          int inserted = context.getContentResolver().bulkInsert(QuoteProvider.Graph_Stock.CONTENT_URI, cv_array);

          Log.d(LOG_TAG, "parseAndInsertHistoricalJson, inserted = " + inserted);


      }
      catch(JSONException | NumberFormatException e)
      {
          Log.e(LOG_TAG, "String to JSON failed: " + e);
      }

  }

  public static ArrayList quoteJsonToContentVals(String JSON, Context c){
    ArrayList<ContentProviderOperation> batchOperations = new ArrayList<>();
    JSONObject jsonObject = null;
    JSONArray resultsArray = null;
    try{
      jsonObject = new JSONObject(JSON);
      if (jsonObject != null && jsonObject.length() != 0){
        jsonObject = jsonObject.getJSONObject("query");
        int count = Integer.parseInt(jsonObject.getString("count"));
        if (count == 1){
            jsonObject = jsonObject.getJSONObject("results").getJSONObject("quote");

            ContentProviderOperation operation = buildBatchOperation(jsonObject, c);

            if (operation != null)
            {
                batchOperations.add(operation);
            }
            else{
                //Invalid stock
                throw new JSONException("Invalid Stock, there is no stock that's called " + jsonObject.getString("symbol"));
            }

        } else{
          resultsArray = jsonObject.getJSONObject("results").getJSONArray("quote");

          if (resultsArray != null && resultsArray.length() != 0){
            for (int i = 0; i < resultsArray.length(); i++){
                jsonObject = resultsArray.getJSONObject(i);

                ContentProviderOperation operation = buildBatchOperation(jsonObject, c);
                if (operation != null)
                {
                    batchOperations.add(operation);
                }
                else{
                    //Invalid stock
                    throw new JSONException("Unable to parse stock: " + jsonObject.getString("symbol"));
                }
            }
          }
        }
      }else{
          return null;
      }
    } catch (JSONException e){
      Log.e(LOG_TAG, "String to JSON failed: " + e);
        return null;
    }
    return batchOperations;
  }

  public static String truncateBidPrice(String bidPrice, Context c){
      if ( bidPrice.matches("null"))
      {
          return c.getResources().getString(R.string.graph_info_not_available);

      }
    bidPrice = String.format("%.2f", Float.parseFloat(bidPrice));
    return bidPrice;
  }

  public static String truncateChange(String change, boolean isPercentChange, Context c){

      if (change.matches("null"))
      {
          if (isPercentChange)
          {
              return c.getResources().getString(R.string.percent_change_error);
          }
          else{
              return c.getResources().getString(R.string.not_percent_change_error);
          }
      }
    String weight = change.substring(0,1);
    String ampersand = "";
    if (isPercentChange){
      ampersand = change.substring(change.length() - 1, change.length());
      change = change.substring(0, change.length() - 1);
    }
    change = change.substring(1, change.length());
    double round = (double) Math.round(Double.parseDouble(change) * 100) / 100;
    change = String.format("%.2f", round);
    StringBuffer changeBuffer = new StringBuffer(change);
    changeBuffer.insert(0, weight);
    changeBuffer.append(ampersand);
    change = changeBuffer.toString();
    return change;
  }

  public static ContentProviderOperation buildBatchOperation(JSONObject jsonObject, Context context) {
    ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(
        QuoteProvider.Quotes.CONTENT_URI);

      if ( ! isValidStockJson(jsonObject) )
      {
          return null;
      }

    try {

      String change = jsonObject.getString("Change");

      builder.withValue(QuoteColumns.SYMBOL, jsonObject.getString("symbol"));
      builder.withValue(QuoteColumns.BIDPRICE, truncateBidPrice(jsonObject.getString("Bid"),context));
      builder.withValue(QuoteColumns.PERCENT_CHANGE, truncateChange(
          jsonObject.getString("ChangeinPercent"), true, context));
      builder.withValue(QuoteColumns.CHANGE, truncateChange(change, false, context));
      if (change.charAt(0) == '-'){
        builder.withValue(QuoteColumns.ISUP, 0);
      }else{
        builder.withValue(QuoteColumns.ISUP, 1);
      }
      builder.withValue(QuoteColumns.NAME, jsonObject.getString("Name"));
        builder.withValue(QuoteColumns.OPEN, truncateBidPrice(jsonObject.getString("Open"),context));
        builder.withValue(QuoteColumns.HIGH, truncateBidPrice(jsonObject.getString("DaysHigh"),context));
        builder.withValue(QuoteColumns.LOW, truncateBidPrice(jsonObject.getString("DaysLow"),context));
        String mkt_cap = jsonObject.getString("MarketCapitalization");
        if (mkt_cap.matches("null"))
        {
            mkt_cap = context.getResources().getString(R.string.graph_info_not_available);
        }
        builder.withValue(QuoteColumns.MKT_CAP, mkt_cap);
        builder.withValue(QuoteColumns.PE_RATIO, truncateBidPrice(jsonObject.getString("PERatio"),context));
        builder.withValue(QuoteColumns.DIV_YIELD, truncateBidPrice(jsonObject.getString("DividendYield"),context));

    } catch (JSONException | NumberFormatException e ){
      e.printStackTrace();


    }
    return builder.build();
  }

    public static boolean isValidStockJson(JSONObject jsonObject)
    {
        //Check if JSONObject is a valid stock
        try{
            boolean hasChange = jsonObject.has("Name");

            if ( hasChange  ) {
              String changeStr = jsonObject.getString("Change");
              if ( !changeStr.equals("null"))
              {
                  return true;
              }
            }
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }


        return false;
    }



    public static void setLastSyncUpdate (Context c, String DateStr)
    {
        Random rand = new Random();
        DateStr = rand.nextLong() + ":" + DateStr; //Adding a random number ensures each update will trigger listeners
        Log.d(LOG_TAG, "setLastSyncUpdate, " + DateStr);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        prefs.edit().putString(LASTSYNCUPDATE, DateStr).apply();
    }

    public static String getLastSyncUpdate (Context c)
    {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        String dateStr = prefs.getString(LASTSYNCUPDATE, "...");

        dateStr = dateStr.replaceFirst("^-?\\d+:", "");

        Log.d(LOG_TAG, "getLastSyncUpdate, " + dateStr);
        return dateStr;
    }



}
