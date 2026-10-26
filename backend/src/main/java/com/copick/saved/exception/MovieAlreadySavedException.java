package com.copick.saved.exception;

public class MovieAlreadySavedException extends RuntimeException {
    public MovieAlreadySavedException(String externalId, Integer groupId) {
        super("Контент с externalId '" + externalId + "' уже сохранён в группе с ID " + groupId);
    }
}