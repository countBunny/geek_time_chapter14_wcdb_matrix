package com.exampletest.liepin.chapter14_wcdb;

import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDex;

import com.tencent.matrix.Matrix;
import com.tencent.matrix.util.MatrixLog;
import com.tencent.sqlitelint.SQLiteLintPlugin;
import com.tencent.sqlitelint.config.SQLiteLintConfig;

public class MyApplication extends Application {
    private static Context sCtx;

    public static Context getCtx() {
        return sCtx;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
        sCtx = base;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Matrix.Builder builder = new Matrix.Builder(this); // build matrix
        builder.patchListener(new TestPluginListener(this)); // add general pluginListener
//        DynamicConfigImplDemo dynamicConfig = new DynamicConfigImplDemo(); // dynamic config

        // init plugin
//        IOCanaryPlugin ioCanaryPlugin = new IOCanaryPlugin(new IOConfig.Builder()
//                .dynamicConfig(dynamicConfig)
//                .build());


        //init matrix
        Matrix.init(builder.build());
        SQLiteLintPlugin plugin = prepareSQLiteLint();
        // start plugin
//        ioCanaryPlugin.start();
        //add to matrix

        if (null != plugin) {
            MatrixLog.i("MyApplication", "SQLiteLint load succeed! ");
            builder.plugin(plugin);
            plugin.start();
        }
    }

    private SQLiteLintPlugin prepareSQLiteLint() {
        SQLiteLintPlugin plugin = (SQLiteLintPlugin) Matrix.with().getPluginByClass(SQLiteLintPlugin.class);
        if (plugin == null) {
            return null;
        }
        plugin.addConcernedDB(new SQLiteLintConfig.ConcernDb(MatrixDbHelper.get().getWritableDatabase())
                .setWhiteListXml(R.xml.sqlite_lint_whitelist)
                .enableAllCheckers());
        return plugin;
    }
}
