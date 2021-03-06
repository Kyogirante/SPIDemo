package com.spi.demo.spi;

import android.util.Log;

import com.spi.annotations.ServiceProvider;

/**
 * @author KyoWang
 * @since 2017/08/25
 */
@ServiceProvider(value = ISPIServie.class)
public class ASPIService implements ISPIServie {
    @Override
    public void printInfo() {
        Log.d("wang", ASPIService.class.getSimpleName());
    }
}
