package com.yapp.d14.interview.application.port.out;

import java.util.List;

public interface JdKeywordExtractor {

    List<String> extractKeywords(String jdText);
}
