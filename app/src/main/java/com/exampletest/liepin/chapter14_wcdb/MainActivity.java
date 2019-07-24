package com.exampletest.liepin.chapter14_wcdb;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Build;
import android.os.SystemClock;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

import com.tencent.sqlitelint.SQLiteLint;
import com.tencent.wcdb.Cursor;
import com.tencent.wcdb.database.SQLiteDatabase;
import com.tencent.wcdb.database.SQLiteOpenHelper;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "WCDB.EncryptDBSample";

    private SQLiteDatabase mDB;
    private SQLiteOpenHelper mDBHelper;
    private int mDBVersion;

    private ListView mListView;
    private SimpleCursorAdapter mAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mListView = (ListView) findViewById(R.id.list);
        mAdapter = new SimpleCursorAdapter(this, R.layout.main_listitem, null,
                new String[] {"content", "_id", "sender"},
                new int[] {R.id.list_tv_content, R.id.list_tv_id, R.id.list_tv_sender},
                0);

        mListView.setAdapter(mAdapter);

        findViewById(R.id.btn_init_plain).setOnClickListener(new View.OnClickListener() {
            // Init plain-text button pressed.
            // Create or open database in version 1, then refresh adapter.

            @SuppressLint("StaticFieldLeak")
            @Override
            public void onClick(View v) {
                new AsyncTask<Void, Void, Cursor>() {
                    @Override
                    protected void onPreExecute() {
                        mAdapter.changeCursor(null);
                    }

                    @SuppressLint("WrongThread")
                    @Override
                    protected Cursor doInBackground(Void... params) {
                        if (mDBHelper != null && mDB != null && mDB.isOpen()) {
                            mDBHelper.close();
                            mDBHelper = null;
                            mDB = null;
                        }

                        mDBHelper = new PlainTextDBHelper(MainActivity.this);
                        mDBHelper.setWriteAheadLoggingEnabled(true);
                        mDB = mDBHelper.getWritableDatabase();
                        mDBVersion = mDB.getVersion();
                        return mDB.rawQuery("SELECT rowid as _id, content, '???' as sender FROM message;",
                                null);
                    }

                    @Override
                    protected void onPostExecute(Cursor cursor) {
                        mAdapter.changeCursor(cursor);
                    }
                }.execute();
            }
        });

        findViewById(R.id.btn_init_encrypted).setOnClickListener(new View.OnClickListener() {
            // Init encrypted button pressed.
            // Create or open database in version 2, then refresh adapter.
            // If plain-text database exists and encrypted one does not, transfer all
            // data from the plain-text database (which in version 1), then upgrade it
            // to version 2.

            // See EncryptedDBHelper.java for details about data transfer and schema upgrade.

            @SuppressLint("StaticFieldLeak")
            @Override
            public void onClick(View v) {
                new AsyncTask<Void, Void, Cursor>() {
                    @Override
                    protected void onPreExecute() {
                        mAdapter.changeCursor(null);
                    }

                    @SuppressLint("WrongThread")
                    @Override
                    protected Cursor doInBackground(Void... params) {
                        if (mDBHelper != null && mDB != null && mDB.isOpen()) {
                            mDBHelper.close();
                            mDBHelper = null;
                            mDB = null;
                        }

                        String passphrase = "passphrase";
                        mDBHelper = new EncryptedDBHelper(MainActivity.this, passphrase);
                        mDBHelper.setWriteAheadLoggingEnabled(true);
                        mDB = mDBHelper.getWritableDatabase();
                        mDBVersion = mDB.getVersion();
                        return mDB.rawQuery("SELECT rowid as _id, content, sender FROM message;",
                                null);
                    }

                    @Override
                    protected void onPostExecute(Cursor cursor) {
                        mAdapter.changeCursor(cursor);
                    }
                }.execute();
            }
        });

        findViewById(R.id.btn_insert).setOnClickListener(new View.OnClickListener() {
            // Insert button pressed.
            // Insert a message to the database.

            // To test data transfer, init plain-text database, insert messages,
            // then init encrypted database.

            final DateFormat DATE_FORMAT = SimpleDateFormat.getDateTimeInstance();

            @SuppressLint("StaticFieldLeak")
            @Override
            public void onClick(View v) {
                new AsyncTask<Void, Void, Cursor>() {
                    @Override
                    protected void onPreExecute() {
                        mAdapter.changeCursor(null);
                    }

                    @SuppressLint("WrongThread")
                    @Override
                    protected Cursor doInBackground(Void... params) {
                        if (mDB == null || !mDB.isOpen())
                            return null;

                        String message = "Message inserted on " + DATE_FORMAT.format(new Date());

                        long begin = SystemClock.uptimeMillis();
                        if (mDBVersion == 1) {
                            mDB.execSQL("INSERT INTO message VALUES (?);",
                                    new Object[]{message});
                            long end = SystemClock.uptimeMillis();
                            if (Build.VERSION.SDK_INT>= Build.VERSION_CODES.O) {
                                SQLiteLint.notifySqlExecution("/data/user/0/com.exampletest.liepin.chapter14_wcdb/databases/plain-text.db","INSERT INTO message VALUES (?);", (int) (end - begin));
                            }
                            return mDB.rawQuery("SELECT rowid as _id, content, '???' as sender FROM message;",
                                    null);
                        } else {
                            mDB.execSQL("INSERT INTO message VALUES (?, ?);",
                                    new Object[]{message, "Me"});
                            long end = SystemClock.uptimeMillis();
                            if (Build.VERSION.SDK_INT>= Build.VERSION_CODES.O) {
                                SQLiteLint.notifySqlExecution("/data/user/0/com.exampletest.liepin.chapter14_wcdb/databases/plain-text.db","INSERT INTO message VALUES (?, ?);", (int) (end - begin));
                            }
                            return mDB.rawQuery("SELECT rowid as _id, content, sender FROM message;",
                                    null);
                        }
                    }

                    @Override
                    protected void onPostExecute(Cursor cursor) {
                        if (cursor == null)
                            return;
                        mAdapter.changeCursor(cursor);
                    }
                }.execute();
            }
        });
    }
}
