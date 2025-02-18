package com.pushtorefresh.storio.sqlitedb.operation.delete;

import android.support.annotation.NonNull;

import com.pushtorefresh.storio.sqlitedb.StorIOSQLiteDb;
import com.pushtorefresh.storio.sqlitedb.Changes;
import com.pushtorefresh.storio.operation.MapFunc;
import com.pushtorefresh.storio.sqlitedb.query.DeleteQuery;
import com.pushtorefresh.storio.util.EnvironmentUtil;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import rx.Observable;
import rx.Subscriber;

public class PreparedDeleteCollectionOfObjects<T> extends PreparedDelete<DeleteCollectionOfObjectsResult<T>> {

    @NonNull private final Collection<T> objects;
    @NonNull private final MapFunc<T, DeleteQuery> mapFunc;
    private final boolean useTransactionIfPossible;

    PreparedDeleteCollectionOfObjects(@NonNull StorIOSQLiteDb storIOSQLiteDb, @NonNull Collection<T> objects, @NonNull MapFunc<T, DeleteQuery> mapFunc, boolean useTransactionIfPossible, @NonNull DeleteResolver deleteResolver) {
        super(storIOSQLiteDb, deleteResolver);
        this.objects = objects;
        this.mapFunc = mapFunc;
        this.useTransactionIfPossible = useTransactionIfPossible;
    }

    @NonNull @Override public DeleteCollectionOfObjectsResult<T> executeAsBlocking() {
        final StorIOSQLiteDb.Internal internal = storIOSQLiteDb.internal();

        final Map<T, DeleteResult> results = new HashMap<>();

        final boolean withTransaction = useTransactionIfPossible && internal.transactionsSupported();

        if (withTransaction) {
            internal.beginTransaction();
        }

        boolean transactionSuccessful = false;

        try {
            for (final T object : objects) {
                final DeleteQuery deleteQuery = mapFunc.map(object);
                final int numberOfDeletedRows = deleteResolver.performDelete(storIOSQLiteDb, deleteQuery);

                results.put(
                        object,
                        DeleteResult.newDeleteResult(
                                numberOfDeletedRows,
                                Collections.singleton(deleteQuery.table))
                );

                if (!withTransaction) {
                    internal.notifyAboutChanges(new Changes(deleteQuery.table));
                }
            }

            if (withTransaction) {
                internal.setTransactionSuccessful();
                transactionSuccessful = true;
            }
        } finally {
            if (withTransaction) {
                internal.endTransaction();

                if (transactionSuccessful) {
                    // if delete was in transaction and it was successful -> notify about changes

                    final Set<String> affectedTables = new HashSet<>(1); // in most cases it will be one table

                    for (final T object : results.keySet()) {
                        affectedTables.addAll(results.get(object).affectedTables());
                    }

                    internal.notifyAboutChanges(new Changes(affectedTables));
                }
            }
        }

        return new DeleteCollectionOfObjectsResult<>(results);
    }

    @NonNull @Override public Observable<DeleteCollectionOfObjectsResult<T>> createObservable() {
        EnvironmentUtil.throwExceptionIfRxJavaIsNotAvailable("createObservable()");

        return Observable.create(new Observable.OnSubscribe<DeleteCollectionOfObjectsResult<T>>() {
            @Override
            public void call(Subscriber<? super DeleteCollectionOfObjectsResult<T>> subscriber) {
                DeleteCollectionOfObjectsResult<T> deleteCollectionOfObjectsResult
                        = executeAsBlocking();

                if (!subscriber.isUnsubscribed()) {
                    subscriber.onNext(deleteCollectionOfObjectsResult);
                    subscriber.onCompleted();
                }
            }
        });
    }

    public static class Builder<T> {

        @NonNull private final StorIOSQLiteDb storIOSQLiteDb;
        @NonNull private final Collection<T> objects;

        private MapFunc<T, DeleteQuery> mapFunc;
        private boolean useTransactionIfPossible = true;
        private DeleteResolver deleteResolver;

        Builder(@NonNull StorIOSQLiteDb storIOSQLiteDb, @NonNull Collection<T> objects) {
            this.storIOSQLiteDb = storIOSQLiteDb;
            this.objects = objects;
        }

        /**
         * Specifies map function to map each object to {@link DeleteQuery}
         *
         * @param mapFunc map function to map each object to {@link DeleteQuery}
         * @return builder
         */
        @NonNull public Builder<T> withMapFunc(@NonNull MapFunc<T, DeleteQuery> mapFunc) {
            this.mapFunc = mapFunc;
            return this;
        }

        /**
         * Defines that Delete Operation will use transaction if it is supported by implementation of {@link StorIOSQLiteDb}
         * By default, transaction will be used
         *
         * @return builder
         */
        @NonNull public Builder<T> useTransactionIfPossible() {
            useTransactionIfPossible = true;
            return this;
        }

        /**
         * Defines that Delete Operation won't use transaction
         * By default, transaction will be used
         *
         * @return builder
         */
        @NonNull public Builder<T> dontUseTransaction() {
            useTransactionIfPossible = false;
            return this;
        }

        /**
         * Optional: Specifies {@link DeleteResolver} for Delete Operation
         *
         * @param deleteResolver {@link DeleteResolver} for Delete Operation
         * @return builder
         */
        @NonNull public Builder<T> withDeleteResolver(@NonNull DeleteResolver deleteResolver) {
            this.deleteResolver = deleteResolver;
            return this;
        }

        /**
         * Prepares Delete Operation
         *
         * @return {@link PreparedDeleteCollectionOfObjects}
         */
        @NonNull public PreparedDeleteCollectionOfObjects<T> prepare() {
            if (deleteResolver == null) {
                deleteResolver = DefaultDeleteResolver.INSTANCE;
            }

            if (mapFunc == null) {
                throw new IllegalStateException("Please specify map function");
            }

            return new PreparedDeleteCollectionOfObjects<>(
                    storIOSQLiteDb,
                    objects,
                    mapFunc,
                    useTransactionIfPossible,
                    deleteResolver
            );
        }
    }
}
