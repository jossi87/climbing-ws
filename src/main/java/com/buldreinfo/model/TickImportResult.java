package com.buldreinfo.model;

import java.util.List;

public record TickImportResult(List<TickImportRow> rows, int numSaved, int numSkipped, int numFailed, boolean committed) {
}
