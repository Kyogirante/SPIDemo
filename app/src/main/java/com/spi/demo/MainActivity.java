package com.spi.demo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import java.util.Iterator;
import java.util.ServiceLoader;

public class MainActivity extends AppCompatActivity {

    private View mBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBtn = findViewById(R.id.spi_deom_btn);
        mBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ServiceLoader<IService> loader = ServiceLoader.load(IService.class);
                Iterator<IService> iterator = loader.iterator();
                boolean isKeepLoc = false;

                while (iterator.hasNext()) {
                    if(iterator.next().keep()){
                        isKeepLoc = true;
                        break;
                    }
                }

                Toast.makeText(MainActivity.this, "Keep location : " + isKeepLoc, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
