package com.yapp.d14.common.util;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum S3Directory {

    PORTFOLIOS("portfolios");

    private final String path;
}
