package com.exampletest.liepin.chapter14_wcdb;

import android.content.Context;
import android.util.Log;

import com.tencent.wcdb.DatabaseUtils;
import com.tencent.wcdb.database.SQLiteChangeListener;
import com.tencent.wcdb.database.SQLiteDatabase;
import com.tencent.wcdb.database.SQLiteOpenHelper;
import com.tencent.wcdb.repair.RepairKit;

import java.io.File;


public class EncryptedDBHelper extends SQLiteOpenHelper {

    private static final String TAG = "EncryptedDBHelper";

    private static final String DATABASE_NAME = "encrypted.db";
    private static final String OLD_DATABASE_NAME = "plain-text.db";
    private static final int DATABASE_VERSION = 2;

    private Context mContext;
    private String mPassphrase;

    public EncryptedDBHelper(Context context, String passphrase) {

        // Call "encrypted" version of the superclass constructor.
        super(context, DATABASE_NAME, passphrase.getBytes(), null, null, DATABASE_VERSION,
                null);

        // Save context object for later use.
        mContext = context;
        mPassphrase = passphrase;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Check whether old plain-text database exists, if so, export it
        // to the new, encrypted one.
        File oldDbFile = mContext.getDatabasePath(OLD_DATABASE_NAME);
        if (oldDbFile.exists()) {

            Log.i(TAG, "Migrating plain-text database to encrypted one.");

            // SQLiteOpenHelper begins a transaction before calling onCreate().
            // We have to end the transaction before we can attach a new database.
            db.endTransaction();

            // Attach old database to the newly created, encrypted database.
            String sql = String.format("ATTACH DATABASE %s AS old KEY '';",
                    DatabaseUtils.sqlEscapeString(oldDbFile.getPath()));
            db.execSQL(sql);

            // Export old database.
            db.beginTransaction();
            DatabaseUtils.stringForQuery(db, "SELECT sqlcipher_export('main', 'old');", null);
            db.setTransactionSuccessful();
            db.endTransaction();

            // Get old database version for later upgrading.
            int oldVersion = (int) DatabaseUtils.longForQuery(db, "PRAGMA old.user_version;", null);

            // Detach old database and enter a new transaction.
            db.execSQL("DETACH DATABASE old;");

            // Old database can be deleted now.
            oldDbFile.delete();

            // Before further actions, restore the transaction.
            db.beginTransaction();

            // Check if we need to upgrade the schema.
            if (oldVersion > DATABASE_VERSION) {
                onDowngrade(db, oldVersion, DATABASE_VERSION);
            } else if (oldVersion < DATABASE_VERSION) {
                onUpgrade(db, oldVersion, DATABASE_VERSION);
            }
        } else {
            Log.i(TAG, "Creating new encrypted database.");

            // Do the real initialization if the old database is absent.
            db.execSQL("CREATE TABLE message (content TEXT, "
                    + "sender TEXT);");
        }

        // OPTIONAL: backup master info for corruption recovery.
        RepairKit.MasterInfo.save(db, db.getPath() + "-mbak", mPassphrase.getBytes());
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        Log.i(TAG, String.format("Upgrading database from version %d to version %d.",
                oldVersion, newVersion));

        // Add new column to message table on database upgrade.
        db.execSQL("ALTER TABLE message ADD COLUMN sender TEXT;");

        // OPTIONAL: backup master info for corruption recovery.
        RepairKit.MasterInfo.save(db, db.getPath() + "-mbak", mPassphrase.getBytes());
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        db.setAsyncCheckpointEnabled(true);
        db.setChangeListener(new SQLiteChangeListener() {

            private StringBuilder mSB = new StringBuilder();
            private void printIds(String prefix, long[] ids) {
                mSB.append(prefix).append(": ");
                for (long id : ids) {
                    mSB.append(id).append(", ");
                }
                Log.i(TAG, mSB.toString());
                mSB.setLength(0);
            }

            @Override
            public void onChange(SQLiteDatabase db, String dbName, String table,
                                 long[] insertIds, long[] updateIds, long[] deleteIds) {
                Log.i(TAG, "onChange called: dbName = " + dbName + ", table = " + table);
                printIds("INSERT", insertIds);
                printIds("UPDATE", updateIds);
                printIds("DELETE", deleteIds);
            }
        }, true);
    }
}
