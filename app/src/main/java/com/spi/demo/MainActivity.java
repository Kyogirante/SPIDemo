package com.spi.demo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.spi.demo.jspi.IService;
import com.spi.demo.spi.ISPIServie;

import java.util.Iterator;
import java.util.ServiceLoader;

public class MainActivity extends AppCompatActivity {

    private View mBtn;
    private View mSPIBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBtn = findViewById(R.id.jspi_demo_btn);
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

        mSPIBtn = findViewById(R.id.spi_demo_btn);
        mSPIBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
              com.spi.loader.ServiceLoader loader = com.spi.loader.ServiceLoader.load(ISPIServie.class);
              Iterator<ISPIServie> iterator = loader.iterator();

              while (iterator.hasNext()) {
                iterator.next().printInfo();
              }
            }
        });
    }
}
