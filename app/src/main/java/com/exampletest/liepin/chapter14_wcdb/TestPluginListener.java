package com.exampletest.liepin.chapter14_wcdb;

import android.content.Context;

import com.tencent.matrix.plugin.DefaultPluginListener;
import com.tencent.matrix.report.Issue;

public class TestPluginListener extends DefaultPluginListener {
    public TestPluginListener(Context context) {
        super(context);
    }

    @Override
    public void onReportIssue(Issue issue) {
        super.onReportIssue(issue);


        //add your code to process data
    }
}
