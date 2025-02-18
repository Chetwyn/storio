package com.pushtorefresh.storio.sqlitedb.operation.get;

import android.database.Cursor;
import android.support.annotation.NonNull;

import com.pushtorefresh.storio.sqlitedb.StorIOSQLiteDb;
import com.pushtorefresh.storio.sqlitedb.query.Query;
import com.pushtorefresh.storio.sqlitedb.query.RawQuery;

/**
 * Default implementation of {@link GetResolver}, thread-safe
 */
public class DefaultGetResolver implements GetResolver {

    // it's thread safe and we can share it instead of creating new one for each Get operation
    static final DefaultGetResolver INSTANCE = new DefaultGetResolver();

    @NonNull @Override
    public Cursor performGet(@NonNull StorIOSQLiteDb storIOSQLiteDb, @NonNull RawQuery rawQuery) {
        return storIOSQLiteDb.internal().rawQuery(rawQuery);
    }

    @NonNull @Override public Cursor performGet(@NonNull StorIOSQLiteDb storIOSQLiteDb, @NonNull Query query) {
        return storIOSQLiteDb.internal().query(query);
    }
}
