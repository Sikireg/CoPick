package com.copick.saved.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SavedMovieGenreId implements Serializable {
    private Integer savedId;
    private Integer genreId;
}