package com.yapp.d14.auth.application.service;

import com.yapp.d14.user.domain.User;

record UserProvisionResult(User user, boolean newlyCreated) {
}
