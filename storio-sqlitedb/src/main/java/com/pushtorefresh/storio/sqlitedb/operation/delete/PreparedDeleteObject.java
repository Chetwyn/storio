package com.pushtorefresh.storio.sqlitedb.operation.delete;

import android.support.annotation.NonNull;

import com.pushtorefresh.storio.sqlitedb.StorIOSQLiteDb;
import com.pushtorefresh.storio.sqlitedb.Changes;
import com.pushtorefresh.storio.operation.MapFunc;
import com.pushtorefresh.storio.sqlitedb.query.DeleteQuery;
import com.pushtorefresh.storio.util.EnvironmentUtil;

import java.util.Collections;

import rx.Observable;
import rx.Subscriber;

public class PreparedDeleteObject<T> extends PreparedDelete<DeleteResult> {

    @NonNull private final T object;
    @NonNull private final MapFunc<T, DeleteQuery> mapFunc;

    PreparedDeleteObject(@NonNull StorIOSQLiteDb storIOSQLiteDb, @NonNull T object, @NonNull MapFunc<T, DeleteQuery> mapFunc, @NonNull DeleteResolver deleteResolver) {
        super(storIOSQLiteDb, deleteResolver);
        this.object = object;
        this.mapFunc = mapFunc;
    }

    @NonNull @Override public DeleteResult executeAsBlocking() {
        final StorIOSQLiteDb.Internal internal = storIOSQLiteDb.internal();
        final DeleteQuery deleteQuery = mapFunc.map(object);

        final int numberOfDeletedRows = deleteResolver.performDelete(storIOSQLiteDb, deleteQuery);

        internal.getLoggi().v(numberOfDeletedRows + " object(s) deleted");

        internal.notifyAboutChanges(new Changes(deleteQuery.table));

        return DeleteResult.newDeleteResult(numberOfDeletedRows, Collections.singleton(deleteQuery.table));
    }

    @NonNull @Override public Observable<DeleteResult> createObservable() {
        EnvironmentUtil.throwExceptionIfRxJavaIsNotAvailable("createObservable()");

        return Observable.create(new Observable.OnSubscribe<DeleteResult>() {
            @Override public void call(Subscriber<? super DeleteResult> subscriber) {
                final DeleteResult deleteResult = executeAsBlocking();

                if (!subscriber.isUnsubscribed()) {
                    subscriber.onNext(deleteResult);
                    subscriber.onCompleted();
                }
            }
        });
    }

    public static class Builder<T> {

        @NonNull private final StorIOSQLiteDb storIOSQLiteDb;
        @NonNull private final T object;

        private MapFunc<T, DeleteQuery> mapFunc;
        private DeleteResolver deleteResolver;

        Builder(@NonNull StorIOSQLiteDb storIOSQLiteDb, @NonNull T object) {
            this.storIOSQLiteDb = storIOSQLiteDb;
            this.object = object;
        }

        /**
         * Specifies map function to map object to {@link DeleteQuery}
         *
         * @param mapFunc map function to map object to {@link DeleteQuery}
         * @return builder
         */
        @NonNull public Builder<T> withMapFunc(@NonNull MapFunc<T, DeleteQuery> mapFunc) {
            this.mapFunc = mapFunc;
            return this;
        }


        /**
         * Optional: Specifies {@link DeleteResolver} for Delete Operation
         *
         * @param deleteResolver delete resolver
         * @return builder
         */
        @NonNull public Builder<T> withDeleteResolver(@NonNull DeleteResolver deleteResolver) {
            this.deleteResolver = deleteResolver;
            return this;
        }

        /**
         * Prepares Delete Operation
         * @return {@link PreparedDeleteObject} instance
         */
        @NonNull public PreparedDeleteObject<T> prepare() {
            if (deleteResolver == null) {
                deleteResolver = DefaultDeleteResolver.INSTANCE;
            }

            if (mapFunc == null) {
                throw new IllegalStateException("Please specify map function");
            }

            return new PreparedDeleteObject<>(
                    storIOSQLiteDb,
                    object,
                    mapFunc,
                    deleteResolver
            );
        }
    }
}
