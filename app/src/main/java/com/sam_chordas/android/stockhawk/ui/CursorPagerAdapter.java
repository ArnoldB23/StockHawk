package com.sam_chordas.android.stockhawk.ui;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

//Credit to https://gist.github.com/mlc/2975296

public class CursorPagerAdapter<F extends Fragment> extends FragmentStatePagerAdapter {
    private final Class<F> fragmentClass;
    private final String[] projection;
    private Cursor cursor;


    public CursorPagerAdapter(FragmentManager fm, Class<F> fragmentClass, String[] projection, Cursor cursor) {
        super(fm);
        this.fragmentClass = fragmentClass;
        this.projection = projection;
        this.cursor = cursor;
    }

    @Override
    public F getItem(int position) {
        if (cursor == null) // shouldn't happen
            return null;

        cursor.moveToPosition(position);
        F frag;
        try {
            frag = fragmentClass.newInstance();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        Bundle args = new Bundle();
        for (int i = 0; i < projection.length; ++i) {
            args.putString(projection[i], cursor.getString(i));
        }
        frag.setArguments(args);
        return frag;
    }

    @Override
    public int getCount() {
        if (cursor == null)
            return 0;
        else
            return cursor.getCount();
    }

    public Cursor swapCursor(Cursor newCursor)
    {
        if (cursor == newCursor)
        {
            return null;
        }

        Cursor oldCursor = cursor;

        this.cursor = newCursor;
        notifyDataSetChanged();

        return oldCursor;
    }

    /**
     * Change the underlying cursor to a new cursor. If there is an existing cursor it will be
     * closed.
     *
     * @param cursor The new cursor to be used
     */
    public void changeCursor(Cursor cursor)
    {
        Cursor old = swapCursor(cursor);
        if (old != null)
        {
            old.close();
        }
    }

    public Cursor getCursor() {
        return cursor;
    }
}
