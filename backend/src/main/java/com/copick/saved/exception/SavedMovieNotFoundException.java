package com.copick.saved.exception;

public class SavedMovieNotFoundException extends RuntimeException {
    public SavedMovieNotFoundException(Integer savedId, Integer groupId) {
        super("Сохранённый элемент с ID " + savedId + " не найден или не принадлежит группе " + groupId);
    }
}