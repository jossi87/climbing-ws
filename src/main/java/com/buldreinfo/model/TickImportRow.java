package com.buldreinfo.model;

public record TickImportRow(int row, String area, String sector, String name, String date, Double stars, String comment, Integer idProblem, Status status, String message) {
	public enum Status {
		/** Valid row that is ready to be saved (dry run only, nothing written). */
		OK,
		/** Valid row that was saved as a tick. */
		SAVED,
		/** The problem is already ticked by this user, so the existing tick is kept untouched. */
		DUPLICATE,
		/** Another row in the same file already ticked this problem. */
		DUPLICATE_IN_FILE,
		/** No problem matched AREA/SECTOR/NAME. */
		NOT_FOUND,
		/** Several problems matched; add AREA or SECTOR to the row. */
		AMBIGUOUS,
		/** The row is missing required values or has an unparsable date. */
		INVALID
	}
}
