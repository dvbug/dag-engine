package com.dvbug.dagengine.executor;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.List;

@Getter
@ToString
@RequiredArgsConstructor(access = AccessLevel.MODULE)
public class ExecuteResult<T> {
    private final T data;
    private final List<History<T>> history;

    @Getter
    @ToString
    @RequiredArgsConstructor(access = AccessLevel.MODULE)
    public static class History<T> {
        private final T data;
        private final String executeNode;
    }
}
