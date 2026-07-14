package com.yapp.d14.interview.application.port.out;

import com.yapp.d14.interview.domain.RedFlag;

import java.util.List;

public interface RedFlagRepository {

    RedFlag save(RedFlag redFlag);

    void saveAll(List<RedFlag> redFlags);

    List<RedFlag> findAllBySessionId(Long sessionId);

    void deleteBySessionId(Long sessionId);
}
