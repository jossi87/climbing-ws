package com.buldreinfo.model;

import java.util.List;

public record ProblemImportResult(List<ProblemImportRow> rows, int numSaved, int numSkipped, int numFailed,
		int numSectorsCreated, int numUsersCreated, boolean committed) {
}
