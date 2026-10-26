package com.copick.saved.entity;

import com.copick.preferences.entity.Genre;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "saved_movie_genres")
@Data
@NoArgsConstructor
@AllArgsConstructor
@IdClass(SavedMovieGenreId.class)
public class SavedMovieGenre {

    @Id
    @Column(name = "saved_id")
    private Integer savedId;

    @Id
    @Column(name = "genre_id")
    private Integer genreId;

    @MapsId("savedId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "saved_id", insertable = false, updatable = false)
    private SavedMovie savedMovie;

    @MapsId("genreId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "genre_id", insertable = false, updatable = false)
    private Genre genre;
}