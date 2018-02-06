package org.p3p.wf.sawf;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class InputCodeActivity extends WearableActivity {

    private TextView mTextView;
    private EditText editText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_input_code);

        mTextView = findViewById(R.id.text);
        editText = findViewById(R.id.editText);
        // Enables Always-on
        setAmbientEnabled();
    }

    /**
     * @param v
     */
    public void onGoNextStep(View v) {
        SharedPreferences sp = getSharedPreferences("sp_sawf", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("usercode", editText.getText().toString());
        editor.commit();
        //跳转
        Intent intent = new Intent();
        intent.setClass(this,SettingActivity.class);
        startActivity(intent);
        this.finish();

    }
}
