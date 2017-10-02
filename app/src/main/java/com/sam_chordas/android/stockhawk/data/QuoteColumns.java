package com.sam_chordas.android.stockhawk.data;

import net.simonvt.schematic.annotation.AutoIncrement;
import net.simonvt.schematic.annotation.ConflictResolutionType;
import net.simonvt.schematic.annotation.DataType;
import net.simonvt.schematic.annotation.NotNull;
import net.simonvt.schematic.annotation.PrimaryKey;
import net.simonvt.schematic.annotation.Unique;

/**
 * Created by sam_chordas on 10/5/15.
 */
public class QuoteColumns {
  @DataType(DataType.Type.INTEGER) @PrimaryKey @AutoIncrement
  public static final String _ID = "_id";
  @DataType(DataType.Type.TEXT) @NotNull @Unique (onConflict = ConflictResolutionType.REPLACE)
  public static final String SYMBOL = "symbol";
  @DataType(DataType.Type.TEXT) @NotNull
  public static final String PERCENT_CHANGE = "percent_change";
  @DataType(DataType.Type.TEXT) @NotNull
  public static final String CHANGE = "change";
  @DataType(DataType.Type.TEXT) @NotNull
  public static final String BIDPRICE = "bid_price";
  @DataType(DataType.Type.INTEGER) @NotNull
  public static final String ISUP = "is_up";
  @DataType(DataType.Type.TEXT)
  public static final String NAME = "name";
  @DataType(DataType.Type.TEXT)
  public static final String OPEN = "open";
  @DataType(DataType.Type.TEXT)
  public static final String HIGH = "dayshigh";
  @DataType(DataType.Type.TEXT)
  public static final String LOW = "dayslow";
  @DataType(DataType.Type.TEXT)
  public static final String MKT_CAP = "marketcapitalization";
  @DataType(DataType.Type.TEXT)
  public static final String PE_RATIO = "peratio";
  @DataType(DataType.Type.TEXT)
  public static final String DIV_YIELD = "dividendyield";


}
