package com.copick.preferences.service;

import com.copick.preferences.dto.PreferenceUpdateDto;
import com.copick.preferences.entity.Genre;
import com.copick.preferences.entity.PreferenceHistory;
import com.copick.preferences.entity.ProfilePreference;
import com.copick.preferences.entity.ProfilePreferenceId;
import com.copick.preferences.exception.InvalidWeightException;
import com.copick.preferences.repository.GenreRepository;
import com.copick.preferences.repository.PreferenceHistoryRepository;
import com.copick.preferences.repository.ProfilePreferenceRepository;
import com.copick.profiles.entity.Profile;
import com.copick.profiles.repository.ProfileRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PreferenceService {

    private static final Double WEIGHT_MIN = -1.0;
    private static final Double WEIGHT_MAX = 1.0;

    private final ProfilePreferenceRepository preferenceRepository;
    private final PreferenceHistoryRepository historyRepository;
    private final ProfileRepository profileRepository;
    private final GenreRepository genreRepository;

    @Transactional
    public void updatePreferences(Integer profileId, List<PreferenceUpdateDto> updates) {
        for (PreferenceUpdateDto dto : updates) {
            validateWeight(dto.getWeight());
        }

        Profile profileRef = profileRepository.getReferenceById(profileId);

        for (PreferenceUpdateDto dto : updates) {
            ProfilePreferenceId id = new ProfilePreferenceId(profileId, dto.getGenreId());

            ProfilePreference pref = preferenceRepository
                .findById(id)
                .orElseGet(() -> {
                    ProfilePreference newPref = new ProfilePreference();
                    newPref.setProfile(profileRef);
                    newPref.setGenre(genreRepository.getReferenceById(dto.getGenreId()));
                    return newPref;
                });

            Double oldWeight = pref.getWeight();

            pref.setWeight(dto.getWeight());
            preferenceRepository.save(pref);

            PreferenceHistory history = new PreferenceHistory();
            history.setProfile(pref.getProfile());
            history.setGenre(pref.getGenre());
            history.setOldWeight(oldWeight);
            history.setNewWeight(dto.getWeight());
            historyRepository.save(history);

            log.info("Обновлено предпочтение: profile={}, genre={}, old={}, new={}",
                    profileId, dto.getGenreId(), oldWeight, dto.getWeight());
        }
    }

    /** Получает все предпочтения пользователя */
    @Transactional(readOnly = true)
    public List<ProfilePreference> getPreferences(Integer profileId) {
        return preferenceRepository.findByProfileId(profileId);
    }

    /** Возвращает историю изменений предпочтений пользователя, отсортированную от новых к старым. */
    @Transactional(readOnly = true)
    public List<PreferenceHistory> getPreferenceHistory(Integer profileId) {
        return historyRepository.findByProfileIdOrderByChangedAtDesc(profileId);
    }

    private void validateWeight(Double weight) {
        if (weight == null) {
            throw new InvalidWeightException();
        }
        if (weight.compareTo(WEIGHT_MIN) < 0 || weight.compareTo(WEIGHT_MAX) > 0) {
            throw new InvalidWeightException(weight);
        }
    }
}