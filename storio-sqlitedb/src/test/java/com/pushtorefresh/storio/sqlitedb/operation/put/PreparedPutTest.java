package com.pushtorefresh.storio.sqlitedb.operation.put;

import android.content.ContentValues;
import android.support.annotation.NonNull;

import com.pushtorefresh.storio.sqlitedb.Changes;
import com.pushtorefresh.storio.sqlitedb.StorIOSQLiteDb;
import com.pushtorefresh.storio.sqlitedb.design.User;
import com.pushtorefresh.storio.operation.MapFunc;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// TODO don't use User class
public class PreparedPutTest {

    // stub class to avoid violation of DRY in "putOne" tests
    private static class PutOneStub {
        final User user;
        final StorIOSQLiteDb storIOSQLiteDb;
        final StorIOSQLiteDb.Internal internal;
        final MapFunc<User, ContentValues> mapFunc;
        final PutResolver<User> putResolver;

        PutOneStub() {
            user = new User(null, "test@example.com");
            storIOSQLiteDb = mock(StorIOSQLiteDb.class);
            internal = mock(StorIOSQLiteDb.Internal.class);

            when(storIOSQLiteDb.internal())
                    .thenReturn(internal);

            when(storIOSQLiteDb.put())
                    .thenReturn(new PreparedPut.Builder(storIOSQLiteDb));

            //noinspection unchecked
            mapFunc = (MapFunc<User, ContentValues>) mock(MapFunc.class);

            //noinspection unchecked
            putResolver = (PutResolver<User>) mock(PutResolver.class);

            when(putResolver.performPut(eq(storIOSQLiteDb), any(ContentValues.class)))
                    .thenReturn(PutResult.newInsertResult(1, User.TABLE));

            when(mapFunc.map(user))
                    .thenReturn(mock(ContentValues.class));

        }

        void verifyBehavior(@NonNull PutResult putResult) {
            // put should be called only once
            verify(storIOSQLiteDb, times(1)).put();

            // object should be mapped to ContentValues only once
            verify(mapFunc, times(1)).map(user);

            // putResolver's performPut() should be called only once
            verify(putResolver, times(1)).performPut(eq(storIOSQLiteDb), any(ContentValues.class));

            // putResolver's afterPut() callback should be called only once
            verify(putResolver, times(1)).afterPut(user, putResult);

            // only one notification should be thrown
            verify(internal, times(1)).notifyAboutChanges(eq(new Changes(User.TABLE)));
        }
    }

    @Test public void putOneBlocking() {
        final PutOneStub putOneStub = new PutOneStub();

        final PutResult putResult = putOneStub.storIOSQLiteDb
                .put()
                .object(putOneStub.user)
                .withMapFunc(putOneStub.mapFunc)
                .withPutResolver(putOneStub.putResolver)
                .prepare()
                .executeAsBlocking();

        putOneStub.verifyBehavior(putResult);
    }

    @Test public void putOneObservable() {
        final PutOneStub putOneStub = new PutOneStub();

        final PutResult putResult = putOneStub.storIOSQLiteDb
                .put()
                .object(putOneStub.user)
                .withMapFunc(putOneStub.mapFunc)
                .withPutResolver(putOneStub.putResolver)
                .prepare()
                .createObservable()
                .toBlocking()
                .last();

        putOneStub.verifyBehavior(putResult);
    }

    // stub class to avoid violation of DRY in "putMultiple" tests
    private static class PutMultipleStub {
        final List<User> users;
        final StorIOSQLiteDb storIOSQLiteDb;
        final StorIOSQLiteDb.Internal internal;
        final MapFunc<User, ContentValues> mapFunc;
        final PutResolver<User> putResolver;
        final boolean useTransaction;

        PutMultipleStub(boolean useTransaction) {
            this.useTransaction = useTransaction;

            users = new ArrayList<>();
            users.add(new User(null, "1"));
            users.add(new User(null, "2"));
            users.add(new User(null, "3"));

            storIOSQLiteDb = mock(StorIOSQLiteDb.class);
            internal = mock(StorIOSQLiteDb.Internal.class);

            when(internal.transactionsSupported())
                    .thenReturn(useTransaction);

            when(storIOSQLiteDb.internal())
                    .thenReturn(internal);

            when(storIOSQLiteDb.put())
                    .thenReturn(new PreparedPut.Builder(storIOSQLiteDb));

            //noinspection unchecked
            putResolver = (PutResolver<User>) mock(PutResolver.class);

            when(putResolver.performPut(eq(storIOSQLiteDb), any(ContentValues.class)))
                    .thenReturn(PutResult.newInsertResult(1, User.TABLE));

            //noinspection unchecked
            mapFunc = (MapFunc<User, ContentValues>) mock(MapFunc.class);

            when(mapFunc.map(users.get(0)))
                    .thenReturn(mock(ContentValues.class));

            when(mapFunc.map(users.get(1)))
                    .thenReturn(mock(ContentValues.class));

            when(mapFunc.map(users.get(2)))
                    .thenReturn(mock(ContentValues.class));
        }

        void verifyBehavior(@NonNull PutCollectionResult<User> putCollectionResult) {
            // only one call to storIOSQLiteDb.put() should occur
            verify(storIOSQLiteDb, times(1)).put();

            // number of calls to putResolver's performPut() should be equal to number of objects
            verify(putResolver, times(users.size())).performPut(eq(storIOSQLiteDb), any(ContentValues.class));

            for (final User user : users) {
                // map operation for each object should be called only once
                verify(mapFunc, times(1)).map(user);

                // putResolver's afterPut() callback should be called only once for each object
                verify(putResolver, times(1))
                        .afterPut(user, putCollectionResult.results().get(user));
            }

            if (useTransaction) {
                // if put() operation used transaction, only one notification should be thrown
                verify(internal, times(1))
                        .notifyAboutChanges(eq(new Changes(User.TABLE)));
            } else {
                // if put() operation didn't use transaction,
                // number of notifications should be equal to number of objects
                verify(internal, times(users.size()))
                        .notifyAboutChanges(eq(new Changes(User.TABLE)));
            }
        }
    }

    @Test public void putMultipleBlocking() {
        final PutMultipleStub putMultipleStub = new PutMultipleStub(true);

        final PutCollectionResult<User> putCollectionResult = putMultipleStub.storIOSQLiteDb
                .put()
                .objects(putMultipleStub.users)
                .withMapFunc(putMultipleStub.mapFunc)
                .withPutResolver(putMultipleStub.putResolver)
                .prepare()
                .executeAsBlocking();

        putMultipleStub.verifyBehavior(putCollectionResult);
    }

    @Test public void putMultipleObservable() {
        final PutMultipleStub putMultipleStub = new PutMultipleStub(true);

        final PutCollectionResult<User> putCollectionResult = putMultipleStub.storIOSQLiteDb
                .put()
                .objects(putMultipleStub.users)
                .withMapFunc(putMultipleStub.mapFunc)
                .withPutResolver(putMultipleStub.putResolver)
                .prepare()
                .createObservable()
                .toBlocking()
                .last();

        putMultipleStub.verifyBehavior(putCollectionResult);
    }

    @Test public void putMultipleBlockingWithoutTransaction() {
        final PutMultipleStub putMultipleStub = new PutMultipleStub(false);

        final PutCollectionResult<User> putCollectionResult = putMultipleStub.storIOSQLiteDb
                .put()
                .objects(putMultipleStub.users)
                .withMapFunc(putMultipleStub.mapFunc)
                .withPutResolver(putMultipleStub.putResolver)
                .dontUseTransaction()
                .prepare()
                .executeAsBlocking();

        putMultipleStub.verifyBehavior(putCollectionResult);
    }

    @Test public void putMultipleObservableWithoutTransaction() {
        final PutMultipleStub putMultipleStub = new PutMultipleStub(false);

        final PutCollectionResult<User> putCollectionResult = putMultipleStub.storIOSQLiteDb
                .put()
                .objects(putMultipleStub.users)
                .withMapFunc(putMultipleStub.mapFunc)
                .withPutResolver(putMultipleStub.putResolver)
                .dontUseTransaction()
                .prepare()
                .createObservable()
                .toBlocking()
                .last();

        putMultipleStub.verifyBehavior(putCollectionResult);
    }

    @Test public void putMultipleBlockingWithTransaction() {
        final PutMultipleStub putMultipleStub = new PutMultipleStub(true);

        final PutCollectionResult<User> putCollectionResult = putMultipleStub.storIOSQLiteDb
                .put()
                .objects(putMultipleStub.users)
                .withMapFunc(putMultipleStub.mapFunc)
                .withPutResolver(putMultipleStub.putResolver)
                .useTransactionIfPossible()
                .prepare()
                .executeAsBlocking();

        putMultipleStub.verifyBehavior(putCollectionResult);
    }

    @Test public void putMultipleObservableWithTransaction() {
        final PutMultipleStub putMultipleStub = new PutMultipleStub(true);

        final PutCollectionResult<User> putCollectionResult = putMultipleStub.storIOSQLiteDb
                .put()
                .objects(putMultipleStub.users)
                .withMapFunc(putMultipleStub.mapFunc)
                .withPutResolver(putMultipleStub.putResolver)
                .useTransactionIfPossible()
                .prepare()
                .createObservable()
                .toBlocking()
                .last();

        putMultipleStub.verifyBehavior(putCollectionResult);
    }
}
