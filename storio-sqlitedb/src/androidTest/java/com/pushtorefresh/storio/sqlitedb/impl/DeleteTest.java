package com.pushtorefresh.storio.sqlitedb.impl;

import android.database.Cursor;
import android.support.test.runner.AndroidJUnit4;

import com.pushtorefresh.storio.sqlitedb.operation.delete.DeleteCollectionOfObjectsResult;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class DeleteTest extends BaseTest {

    @Test public void deleteOne() {
        final User user = putUser();

        final Cursor cursorAfterInsert = db.query(User.TABLE, null, null, null, null, null, null);
        assertEquals(1, cursorAfterInsert.getCount());
        cursorAfterInsert.close();

        deleteUser(user);

        final Cursor cursorAfterDelete = db.query(User.TABLE, null, null, null, null, null, null);
        assertEquals(0, cursorAfterDelete.getCount());
        cursorAfterDelete.close();
    }

    @Test public void deleteCollection() {
        final List<User> allUsers = putUsers(10);

        final List<User> usersToDelete = new ArrayList<>();
        for (int i = 0; i < allUsers.size(); i += 2) {  // I will delete every second
            usersToDelete.add(allUsers.get(i));
        }

        final DeleteCollectionOfObjectsResult<User> deleteResult = storIOSQLiteDb
                .delete()
                .objects(usersToDelete)
                .withMapFunc(User.MAP_TO_DELETE_QUERY)
                .prepare()
                .executeAsBlocking();

        final List<User> existUsers = getAllUsers();

        for (User user : allUsers) {
            final boolean shouldBeDeleted = usersToDelete.contains(user);

            // Check if we deleted what we going to.
            assertEquals(shouldBeDeleted, deleteResult.wasDeleted(user));

            // Check if exist, what we want to save.
            assertEquals(!shouldBeDeleted, existUsers.contains(user));
        }
    }
}
