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
    private final GenreRepository genreRepository; // Добавили для проверки жанров

    /**
     * DTO для запроса на добавление фильма в сохранённые.
     * genres заменен на genreIds, так как мы используем реляционную таблицу.
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
        private List<Integer> genreIds; // <-- ИЗМЕНЕНО: теперь это ID жанров
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
            savedMovie.setRating(java.math.BigDecimal.valueOf(request.getRating()));
        }

        savedMovie.setReleaseYear(request.getReleaseYear());
        savedMovie.setOverview(request.getOverview());
        savedMovie.setNotes(request.getNotes());
        savedMovie.setAddedAt(OffsetDateTime.now());

        // 4. МАППИНГ ЖАНРОВ: преобразуем List<Integer> в List<SavedMovieGenre>
        if (request.getGenreIds() != null) {
            // Убираем дубликаты ID на всякий случай
            List<Integer> uniqueGenreIds = request.getGenreIds().stream().distinct().toList();

            for (Integer genreId : uniqueGenreIds) {
                // Проверяем, что жанр реально существует в БД
                Genre genre = genreRepository.findById(genreId)
                        .orElseThrow(() -> new IllegalArgumentException("Жанр с ID " + genreId + " не найден в справочнике"));

                SavedMovieGenre savedMovieGenre = new SavedMovieGenre();
                savedMovieGenre.setGenre(genre);

                // Добавляем в коллекцию фильма.
                // Благодаря CascadeType.ALL Hibernate сам проставит savedId после сохранения родителя.
                savedMovie.getGenres().add(savedMovieGenre);
            }
        }

        SavedMovie result = savedMovieRepository.save(savedMovie);
        log.info("Контент '{}' (ID: {}) с описанием и {} жанрами добавлен в сохранённые группы {}",
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
        // Убедись, что в репозитории метод называется именно так
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