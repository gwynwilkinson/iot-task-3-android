package com.plump_monkey.iotble;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.webkit.WebView;

public class HelpActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Back");

        String uri = Constants.NO_HELP;
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            uri = extras.getString(Constants.URI);
        }

        WebView help_content = (WebView)findViewById(R.id.general_help);
        help_content.getSettings().setBuiltInZoomControls(true);
        help_content.loadUrl(uri);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.setResult(RESULT_OK);
                this.finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
