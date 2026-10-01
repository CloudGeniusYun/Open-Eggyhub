package com.eggyhub.android;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.content.pm.PackageManager;
import android.widget.TextView;

public class AboutActivity extends BaseActivity {
    private View UserAgreement;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about);

        TextView versionTextView = findViewById(R.id.version_text_view);
        try {
            String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            versionTextView.setText("主应用版本号：" + versionName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            versionTextView.setText("主应用版本号：未知");
        }

        UserAgreement = findViewById(R.id.UserAgreement);
        UserAgreement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AboutActivity.this, UserAgreementActivity.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        });

    }
}
