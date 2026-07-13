package com.yapp.d14.interview.application.port.out;

import java.util.List;

public interface AxisReportScorer {

    List<AxisScoreDraft> score(AxisReportScoreContext context);
}
