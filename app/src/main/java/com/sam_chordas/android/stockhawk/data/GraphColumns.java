package com.sam_chordas.android.stockhawk.data;

import net.simonvt.schematic.annotation.Check;
import net.simonvt.schematic.annotation.ConflictResolutionType;
import net.simonvt.schematic.annotation.DataType;
import net.simonvt.schematic.annotation.NotNull;
import net.simonvt.schematic.annotation.PrimaryKey;
import net.simonvt.schematic.annotation.References;

/**
 * Created by Arnold on 7/2/2016.
 */
public class GraphColumns {

    @DataType(DataType.Type.TEXT) @PrimaryKey (onConflict = ConflictResolutionType.IGNORE)
    public static final String _ID = "_id";

    @DataType(DataType.Type.INTEGER) @References(table = QuoteDatabase.QUOTES, column = QuoteColumns.SYMBOL)
    public static final String SYMBOL = "symbol";

    @DataType(DataType.Type.TEXT)  @NotNull
    public static final String STOCK_TIMESTAMP = "stock_timestamp";

    @DataType(DataType.Type.REAL) @NotNull
    public static final String CLOSE = "CLOSE";

    @DataType(DataType.Type.TEXT) @NotNull
    public static final String UPDATE_TIMESTAMP = "update_timestamp";

    @DataType(DataType.Type.TEXT) @NotNull @Check(GraphColumns.GRAPH_TYPE + " in ('" + QuoteProvider.GraphType.MONTHORYEAR + "','" + QuoteProvider.GraphType.DAYS + "')")
    public static final String GRAPH_TYPE = "graph_type";
}
