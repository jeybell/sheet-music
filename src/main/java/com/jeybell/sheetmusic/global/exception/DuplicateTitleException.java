package com.jeybell.sheetmusic.global.exception;

public class DuplicateTitleException extends RuntimeException {

    public DuplicateTitleException(String title) {
        super("이미 등록된 제목입니다: " + title);
    }
}
