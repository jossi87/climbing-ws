package com.buldreinfo.model;

import java.util.List;

public record ProblemImportRow(int row, String area, String sector, Integer nr, String name, String description,
		String type, String grade, String faDate, String faUsers, Integer idArea, Integer idSector, Integer idProblem,
		boolean newSector, List<String> newUsers, Status status, String message) {
	public enum Status {
		/** Valid row that is ready to be saved (dry run only, nothing written). */
		OK,
		/** Valid row that was saved as a problem. */
		SAVED,
		/** The sector already has a problem with this name. */
		DUPLICATE,
		/** Another row in the same file already added this problem to the sector. */
		DUPLICATE_IN_FILE,
		/** No area named AREA exists in the region, or several sectors share SECTOR. */
		NOT_FOUND,
		/** Several areas or sectors matched. */
		AMBIGUOUS,
		/** The row is missing required values, or NR/TYPE/GRADE/FA DATE cannot be read. */
		INVALID,
		/** The user is not an admin of the area (or of the sector it already belongs to). */
		FORBIDDEN
	}

	/** The same row with its outcome: used once the row has been written, when a new sector has an id too. */
	public ProblemImportRow withResult(Integer idSector, Integer idProblem, Status status, String message) {
		return new ProblemImportRow(row, area, sector, nr, name, description, type, grade, faDate, faUsers, idArea,
				idSector, idProblem, newSector, newUsers, status, message);
	}
}
