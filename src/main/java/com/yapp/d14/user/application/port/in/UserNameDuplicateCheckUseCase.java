package com.yapp.d14.user.application.port.in;

public interface UserNameDuplicateCheckUseCase {

    boolean isAvailable(String name);
}
