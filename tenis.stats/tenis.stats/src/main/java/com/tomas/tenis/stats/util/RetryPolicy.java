package com.tomas.tenis.stats.util;

import feign.FeignException;
import org.springframework.stereotype.Component;

import java.net.ConnectException;
import java.net.SocketTimeoutException;

@Component
public class RetryPolicy {

    public boolean esErrorReintentable(Exception e) {

        if (e instanceof FeignException feignEx) {
            int status = feignEx.status();
            return status >= 500;
        }

        return e instanceof SocketTimeoutException
                || e instanceof ConnectException;
    }
}