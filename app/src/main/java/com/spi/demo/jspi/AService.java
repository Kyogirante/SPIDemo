package com.spi.demo.jspi;

import com.spi.annotations.ServiceProvider;
import com.spi.demo.spi.ASPIService;

/**
 * @author KyoWang
 * @since 2017/05/09
 */
public class AService implements IService {
    @Override
    public boolean keep() {
        return false;
    }
}
