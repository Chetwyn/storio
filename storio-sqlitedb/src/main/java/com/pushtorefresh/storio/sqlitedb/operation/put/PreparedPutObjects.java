package com.pushtorefresh.storio.sqlitedb.operation.put;

import android.content.ContentValues;
import android.support.annotation.NonNull;

import com.pushtorefresh.storio.sqlitedb.StorIOSQLiteDb;
import com.pushtorefresh.storio.sqlitedb.Changes;
import com.pushtorefresh.storio.operation.MapFunc;
import com.pushtorefresh.storio.util.EnvironmentUtil;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import rx.Observable;
import rx.Subscriber;

public class PreparedPutObjects<T> extends PreparedPut<T, PutCollectionResult<T>> {

    @NonNull private final Iterable<T> objects;
    @NonNull private final MapFunc<T, ContentValues> mapFunc;
    private final boolean useTransactionIfPossible;

    PreparedPutObjects(@NonNull StorIOSQLiteDb storIOSQLiteDb,
                       @NonNull PutResolver<T> putResolver,
                       @NonNull Iterable<T> objects, @NonNull MapFunc<T, ContentValues> mapFunc,
                       boolean useTransactionIfPossible) {
        super(storIOSQLiteDb, putResolver);
        this.objects = objects;
        this.mapFunc = mapFunc;
        this.useTransactionIfPossible = useTransactionIfPossible;
    }

    @NonNull @Override public PutCollectionResult<T> executeAsBlocking() {
        final StorIOSQLiteDb.Internal internal = storIOSQLiteDb.internal();
        final Map<T, PutResult> putResults = new HashMap<>();

        final boolean withTransaction = useTransactionIfPossible
                && storIOSQLiteDb.internal().transactionsSupported();

        if (withTransaction) {
            internal.beginTransaction();
        }

        boolean transactionSuccessful = false;

        try {
            for (T object : objects) {
                final PutResult putResult = putResolver.performPut(
                        storIOSQLiteDb,
                        mapFunc.map(object)
                );

                putResolver.afterPut(object, putResult);
                putResults.put(object, putResult);

                if (!withTransaction) {
                    internal.notifyAboutChanges(new Changes(putResult.affectedTable()));
                }
            }

            if (withTransaction) {
                storIOSQLiteDb.internal().setTransactionSuccessful();
                transactionSuccessful = true;
            }
        } finally {
            if (withTransaction) {
                storIOSQLiteDb.internal().endTransaction();

                if (transactionSuccessful) {
                    final Set<String> affectedTables = new HashSet<>(1); // in most cases it will be 1 table

                    for (final T object : putResults.keySet()) {
                        affectedTables.add(putResults.get(object).affectedTable());
                    }

                    storIOSQLiteDb.internal().notifyAboutChanges(new Changes(affectedTables));
                }
            }
        }

        return new PutCollectionResult<>(putResults);
    }

    @NonNull @Override public Observable<PutCollectionResult<T>> createObservable() {
        EnvironmentUtil.throwExceptionIfRxJavaIsNotAvailable("createObservable()");

        return Observable.create(new Observable.OnSubscribe<PutCollectionResult<T>>() {
            @Override
            public void call(Subscriber<? super PutCollectionResult<T>> subscriber) {
                PutCollectionResult<T> putResults = executeAsBlocking();

                if (!subscriber.isUnsubscribed()) {
                    subscriber.onNext(putResults);
                    subscriber.onCompleted();
                }
            }
        });
    }

    public static class Builder<T> {

        @NonNull private final StorIOSQLiteDb storIOSQLiteDb;
        @NonNull private final Iterable<T> objects;

        private MapFunc<T, ContentValues> mapFunc;
        private PutResolver<T> putResolver;
        private boolean useTransactionIfPossible = true;

        Builder(@NonNull StorIOSQLiteDb storIOSQLiteDb, @NonNull Iterable<T> objects) {
            this.storIOSQLiteDb = storIOSQLiteDb;
            this.objects = objects;
        }

        /**
         * Specifies map function for Put Operation which will be used to map each object to {@link ContentValues}
         *
         * @param mapFunc map function for Put Operation which will be used to map each object to {@link ContentValues}
         * @return builder
         */
        @NonNull public Builder<T> withMapFunc(@NonNull MapFunc<T, ContentValues> mapFunc) {
            this.mapFunc = mapFunc;
            return this;
        }

        /**
         * Specifies {@link PutResolver} for Put Operation which allows you to customize behavior of Put Operation
         *
         * @param putResolver put resolver
         * @return builder
         * @see {@link DefaultPutResolver} — easy way to create {@link PutResolver}
         */
        @NonNull public Builder<T> withPutResolver(@NonNull PutResolver<T> putResolver) {
            this.putResolver = putResolver;
            return this;
        }

        /**
         * Defines that Put Operation will use transaction if it is supported by implementation of {@link StorIOSQLiteDb}
         * By default, transaction will be used
         *
         * @return builder
         */
        @NonNull public Builder<T> useTransactionIfPossible() {
            useTransactionIfPossible = true;
            return this;
        }

        /**
         * Defines that Put Operation won't use transaction
         * By default, transaction will be used
         *
         * @return builder
         */
        @NonNull public Builder<T> dontUseTransaction() {
            useTransactionIfPossible = false;
            return this;
        }

        /**
         * Prepares Put Operation
         * @return {@link PreparedPutObjects} instance
         */
        @NonNull public PreparedPutObjects<T> prepare() {
            if (mapFunc == null) {
                throw new IllegalStateException("Please specify map function");
            }

            if (putResolver == null) {
                throw new IllegalStateException("Please specify put resolver");
            }

            return new PreparedPutObjects<>(
                    storIOSQLiteDb,
                    putResolver,
                    objects,
                    mapFunc,
                    useTransactionIfPossible);
        }
    }
}
