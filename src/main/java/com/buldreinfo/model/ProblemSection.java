package com.buldreinfo.model;

import java.util.List;

public record ProblemSection(int id, int nr, String description, String grade, List<Media> media) {
    public ProblemSection withMedia(List<Media> media) {
        return new ProblemSection(this.id, this.nr, this.description, this.grade, media);
    }
}