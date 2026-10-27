package com.copick.saved.service;

import com.copick.groups.entity.Group;
import com.copick.groups.repository.GroupRepository;
import com.copick.preferences.entity.Genre;
import com.copick.preferences.repository.GenreRepository;
import com.copick.profiles.entity.Profile;
import com.copick.profiles.exception.ProfileNotFoundException;
import com.copick.profiles.repository.ProfileRepository;
import com.copick.saved.entity.SavedMovie;
import com.copick.saved.entity.SavedMovieGenre;
import com.copick.saved.exception.GroupNotFoundException;
import com.copick.saved.exception.MovieAlreadySavedException;
import com.copick.saved.exception.SavedMovieNotFoundException;
import com.copick.saved.repository.SavedMovieRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SavedMovieService {

    private final SavedMovieRepository savedMovieRepository;
    private final GroupRepository groupRepository;
    private final ProfileRepository profileRepository;
    private final GenreRepository genreRepository;

    /**
     * DTO для запроса на добавление фильма в сохранённые.
     */
    @Data
    public static class SaveMovieRequest {
        private String externalId;
        private String contentType;
        private String title;
        private String originalTitle;
        private String posterUrl;
        private String backdropUrl;
        private Double rating;
        private Integer releaseYear;
        private List<String> genres;
        private String overview;
        private String notes;
    }

    /**
     * Добавляет фильм на доску группы.
     */
    @Transactional
    public SavedMovie addToSaved(Integer groupId, Integer profileId, SaveMovieRequest request) {
        // 1. Проверяем существование группы и пользователя
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new GroupNotFoundException(groupId));

        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new ProfileNotFoundException(profileId));

        // 2. Проверяем, нет ли уже такого фильма в этой группе
        savedMovieRepository.findByGroupIdAndExternalIdAndContentType(
                groupId, request.getExternalId(), request.getContentType()
        ).ifPresent(existing -> {
            throw new MovieAlreadySavedException(request.getExternalId(), groupId);
        });

        // 3. Создаём и сохраняем запись
        SavedMovie savedMovie = new SavedMovie();
        savedMovie.setGroup(group);
        savedMovie.setAddedBy(profile);

        savedMovie.setExternalId(request.getExternalId());
        savedMovie.setContentType(request.getContentType());
        savedMovie.setTitle(request.getTitle());
        savedMovie.setOriginalTitle(request.getOriginalTitle());
        savedMovie.setPosterUrl(request.getPosterUrl());
        savedMovie.setBackdropUrl(request.getBackdropUrl());

        if (request.getRating() != null) {
            savedMovie.setRating(request.getRating());
        }

        savedMovie.setReleaseYear(request.getReleaseYear());
        savedMovie.setOverview(request.getOverview());
        savedMovie.setNotes(request.getNotes());
        savedMovie.setAddedAt(OffsetDateTime.now());

        if (request.getGenres() != null) {
            // Убираем возможные дубликаты названий
            List<String> uniqueGenreNames = request.getGenres().stream().distinct().toList();

            for (String genreName : uniqueGenreNames) {
                // Ищем жанр в БД по названию
                Genre genre = genreRepository.findByGenreName(genreName)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Жанр '" + genreName + "' не найден в справочнике genres. " +
                                        "Возможно, нужно добавить его через миграцию."));

                SavedMovieGenre savedMovieGenre = new SavedMovieGenre();
                savedMovieGenre.setGenre(genre);

                // Добавляем в коллекцию. Благодаря CascadeType.ALL Hibernate сам проставит savedId
                savedMovie.getGenres().add(savedMovieGenre);
            }
        }

        SavedMovie result = savedMovieRepository.save(savedMovie);
        log.info("Контент '{}' (ID: {}) с {} жанрами добавлен в сохранённые группы {}",
                request.getTitle(), result.getSavedId(), result.getGenres().size(), groupId);

        return result;
    }

    /**
     * Получает список всех сохранённых фильмов для группы.
     */
    @Transactional(readOnly = true)
    public List<SavedMovie> getSavedMovies(Integer groupId) {
        if (!groupRepository.existsById(groupId)) {
            throw new GroupNotFoundException(groupId);
        }
        return savedMovieRepository.findByGroupIdWithGenres(groupId);
    }

    /**
     * Удаляет фильм из сохранённых (только если он принадлежит этой группе).
     */
    @Transactional
    public void removeFromSaved(Integer groupId, Integer savedId) {
        SavedMovie savedMovie = savedMovieRepository.findByIdAndGroupId(savedId, groupId)
                .orElseThrow(() -> new SavedMovieNotFoundException(savedId, groupId));

        savedMovieRepository.delete(savedMovie);
        log.info("Элемент с savedId={} удалён из группы {}", savedId, groupId);
    }
}