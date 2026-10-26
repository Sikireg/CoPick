package com.copick.saved.exception;

public class GroupNotFoundException extends RuntimeException {
    public GroupNotFoundException(Integer groupId) {
        super("Группа с ID " + groupId + " не найдена");
    }
}