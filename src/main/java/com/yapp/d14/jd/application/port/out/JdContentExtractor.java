package com.yapp.d14.jd.application.port.out;

import com.yapp.d14.jd.exception.JdExtractionFailedException;

public interface JdContentExtractor {

    String extract(String rawText) throws JdExtractionFailedException;
}
