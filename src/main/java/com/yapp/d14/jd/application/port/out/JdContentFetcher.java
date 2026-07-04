package com.yapp.d14.jd.application.port.out;

import com.yapp.d14.jd.exception.JdCrawlingFailedException;

public interface JdContentFetcher {

    String fetch(String jdUrl) throws JdCrawlingFailedException;
}
