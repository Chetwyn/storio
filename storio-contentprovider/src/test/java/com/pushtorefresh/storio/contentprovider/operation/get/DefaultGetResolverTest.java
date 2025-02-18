package com.pushtorefresh.storio.contentprovider.operation.get;

import android.database.Cursor;

import com.pushtorefresh.storio.contentprovider.StorIOContentProvider;
import com.pushtorefresh.storio.contentprovider.query.Query;

import org.junit.Test;

import static junit.framework.Assert.assertSame;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DefaultGetResolverTest {

    @Test
    public void query() {
        final StorIOContentProvider storIOContentProvider = mock(StorIOContentProvider.class);
        final StorIOContentProvider.Internal internal = mock(StorIOContentProvider.Internal.class);
        final Query query = mock(Query.class);
        final Cursor expectedCursor = mock(Cursor.class);

        when(storIOContentProvider.internal())
                .thenReturn(internal);

        when(internal.query(query))
                .thenReturn(expectedCursor);

        final DefaultGetResolver defaultGetResolver = new DefaultGetResolver();

        final Cursor actualCursor = defaultGetResolver.performGet(storIOContentProvider, query);

        // only one request should occur
        verify(internal, times(1)).query(any(Query.class));

        // and this request should be equals to original
        verify(internal, times(1)).query(query);

        assertSame(expectedCursor, actualCursor);
    }
}
