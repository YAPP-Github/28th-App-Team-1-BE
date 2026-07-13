package com.yapp.d14.interview.application.port.out;

import java.util.List;

public interface RedFlagReconciler {

    List<RedFlagVerdict> reconcile(RedFlagReconcileContext context);
}
