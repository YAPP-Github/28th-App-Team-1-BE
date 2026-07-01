package com.yapp.d14.user.application.port.out;

import java.util.Optional;
import java.util.UUID;

public interface TokenRepository {

    void save(UUID userId, String refreshToken);

    Optional<String> find(UUID userId);

    void delete(UUID userId);
}
