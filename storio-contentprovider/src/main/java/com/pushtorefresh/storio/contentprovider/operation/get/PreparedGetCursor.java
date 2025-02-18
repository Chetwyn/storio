package com.pushtorefresh.storio.contentprovider.operation.get;

import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.pushtorefresh.storio.contentprovider.StorIOContentProvider;
import com.pushtorefresh.storio.contentprovider.query.Query;
import com.pushtorefresh.storio.operation.PreparedOperationWithReactiveStream;
import com.pushtorefresh.storio.util.EnvironmentUtil;

import rx.Observable;
import rx.Subscriber;

/**
 * Represents an Operation for {@link StorIOContentProvider} which performs query that retrieves data as {@link Cursor}
 * from {@link android.content.ContentProvider}
 */
public class PreparedGetCursor extends PreparedGet<Cursor> {

    @NonNull
    protected final Query query;

    PreparedGetCursor(@NonNull StorIOContentProvider storIOContentProvider, @NonNull GetResolver getResolver, @NonNull Query query) {
        super(storIOContentProvider, getResolver);
        this.query = query;
    }

    @Nullable
    @Override
    public Cursor executeAsBlocking() {
        return getResolver.performGet(storIOContentProvider, query);
    }

    @NonNull
    @Override
    public Observable<Cursor> createObservable() {
        EnvironmentUtil.throwExceptionIfRxJavaIsNotAvailable("createObservable()");

        return Observable.create(new Observable.OnSubscribe<Cursor>() {
            @Override
            public void call(Subscriber<? super Cursor> subscriber) {
                if (!subscriber.isUnsubscribed()) {
                    subscriber.onNext(executeAsBlocking());
                    subscriber.onCompleted();
                }
            }
        });
    }

    @NonNull
    @Override
    public Observable<Cursor> createObservableStream() {
        EnvironmentUtil.throwExceptionIfRxJavaIsNotAvailable("createObservableStream()");
        throw new IllegalStateException("Not implemented");
    }

    /**
     * Builder for {@link PreparedGetCursor}
     */
    public static class Builder {

        @NonNull
        private final StorIOContentProvider storIOContentProvider;

        private Query query;

        private GetResolver getResolver;

        public Builder(@NonNull StorIOContentProvider storIOContentProvider) {
            this.storIOContentProvider = storIOContentProvider;
        }

        /**
         * Specifies {@link Query} for Get Operation
         *
         * @param query query
         * @return builder
         */
        @NonNull
        public Builder withQuery(@NonNull Query query) {
            this.query = query;
            return this;
        }

        /**
         * Optional: Specifies {@link GetResolver} for Get Operation which allows you to customize behavior of Get Operation
         *
         * @param getResolver get resolver
         * @return builder
         */
        @NonNull
        public Builder withGetResolver(@Nullable GetResolver getResolver) {
            this.getResolver = getResolver;
            return this;
        }

        /**
         * Prepares Get Operation
         *
         * @return {@link PreparedGetCursor} instance
         */
        @NonNull
        public PreparedOperationWithReactiveStream<Cursor> prepare() {
            if (query == null) {
                throw new IllegalStateException("Please specify query");
            }

            if (getResolver == null) {
                getResolver = DefaultGetResolver.INSTANCE;
            }

            return new PreparedGetCursor(
                    storIOContentProvider,
                    getResolver,
                    query
            );
        }
    }
}
