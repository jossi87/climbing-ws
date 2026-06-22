package com.buldreinfo.model;

import java.util.List;

public record ProblemComment(int id, String date, int idUser, MediaIdentity mediaIdentity, String name, String message, boolean danger, boolean resolved, List<Media> media, boolean editable) {
	public ProblemComment withEditable(boolean editable) {
        return new ProblemComment(id, date, idUser, mediaIdentity, name, message, danger, resolved, media, editable);
    }
}