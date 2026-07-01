package com.yapp.d14.common.security;

import java.util.UUID;

public interface TokenParser {

    UUID parse(String token);
}
