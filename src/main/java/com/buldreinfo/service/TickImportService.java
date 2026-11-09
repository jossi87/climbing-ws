package com.buldreinfo.service;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.buldreinfo.dao.ProblemRepository;
import com.buldreinfo.dao.TickRepository;
import com.buldreinfo.excel.ExcelReader;
import com.buldreinfo.excel.TickTemplate;
import com.buldreinfo.exception.ValidationFailedException;
import com.buldreinfo.model.Tick;
import com.buldreinfo.model.TickImportCandidate;
import com.buldreinfo.model.TickImportResult;
import com.buldreinfo.model.TickImportRow;
import com.buldreinfo.model.TickImportRow.Status;

/**
 * Import of ticks from the spreadsheet written by {@link TickTemplate}: each row is matched against the problems the
 * user can tick, and the matched ones are saved as ticks.
 *
 * One {@link #plan} behind two entry points: {@link #preview} reports what would happen and writes nothing, while
 * {@link #commit} applies the very same plan. Every write in this class lives in {@link #apply}, so a code path that
 * stops at the plan cannot change the database.
 */
@Service
public class TickImportService {
	public static final int MAX_ROWS = 500;
	private static final int MAX_COMMENT_LENGTH = 4000;
	private static final String COL_DESCRIPTION = "DESCRIPTION";
	private static final Set<String> XLSX_EXTENSIONS = Set.of("xlsx", "xlsm");
	private static final Pattern WHITESPACE = Pattern.compile("\\s+");

	private final ProblemRepository problemRepo;
	private final TickRepository tickRepo;
	private final TickService tickService;

	public TickImportService(ProblemRepository problemRepo, TickRepository tickRepo, TickService tickService) {
		this.problemRepo = problemRepo;
		this.tickRepo = tickRepo;
		this.tickService = tickService;
	}

	@Transactional(readOnly = true)
	public TickImportResult preview(int userId, MultipartFile file) {
		return plan(userId, file).toPreviewResult();
	}

	@Transactional
	public TickImportResult commit(int userId, MultipartFile file) {
		return apply(userId, plan(userId, file));
	}

	/**
	 * Everything an import decides, and nothing it changes: the upload is parsed, every row is matched against the
	 * problems the user can tick, and the outcome is the same list of rows {@link #apply} then saves.
	 *
	 * Preview stops here - it never reaches {@link #apply} - and the read-only transaction it runs in would reject a
	 * write anyway, so a preview can be repeated as often as wanted without leaving a trace. Keep the "reads here,
	 * writes in apply" split: a write on this path is a write the preview performs too.
	 */
	private Plan plan(int userId, MultipartFile file) {
		validateUpload(file);
		List<ParsedRow> parsed;
		try (InputStream in = file.getInputStream()) {
			parsed = read(in);
		} catch (IOException e) {
			throw new ValidationFailedException("Unable to read the uploaded file: " + e.getMessage());
		}
		var resolution = resolve(userId, parsed);
		var results = resolution.results();
		int numSaved = 0;
		int numSkipped = 0;
		int numFailed = 0;
		var toSave = new ArrayList<ParsedRow>();
		for (var p : parsed) {
			var result = results.get(p.row());
			switch (result.status()) {
				case OK -> {
					numSaved++;
					toSave.add(p);
				}
				case DUPLICATE, DUPLICATE_IN_FILE -> numSkipped++;
				default -> numFailed++;
			}
		}
		return new Plan(results, resolution.matches(), toSave, numSaved, numSkipped, numFailed);
	}

	/**
	 * Saves the rows {@link #plan} accepted. The only method in this class that writes: the ticks are handed to
	 * {@link TickService#setTicks}, which stores them and their activity rows.
	 */
	private TickImportResult apply(int userId, Plan plan) {
		var results = plan.results();
		if (!plan.toSave().isEmpty()) {
			tickService.setTicks(userId, plan.toSave().stream().map(p -> toTick(results.get(p.row()), plan.matches().get(p.row()))).toList());
			for (var p : plan.toSave()) {
				var saved = results.get(p.row());
				results.put(p.row(), new TickImportRow(saved.row(), saved.area(), saved.sector(), saved.name(), saved.date(), saved.stars(), saved.comment(), saved.idProblem(), Status.SAVED, "Saved"));
			}
		}
		return new TickImportResult(List.copyOf(results.values()), plan.numSaved(), plan.numSkipped(), plan.numFailed(), true);
	}

	private List<ParsedRow> read(InputStream in) throws IOException {
		try (var reader = ExcelReader.open(in, TickTemplate.SHEET_TICKS)) {
			for (var required : List.of(TickTemplate.COL_NAME, TickTemplate.COL_DATE)) {
				if (!reader.hasColumn(required)) {
					throw new ValidationFailedException("The sheet is missing the " + required + " column. Did you fill in the template?");
				}
			}
			String commentColumn = reader.hasColumn(TickTemplate.COL_COMMENT) ? TickTemplate.COL_COMMENT : reader.hasColumn(COL_DESCRIPTION) ? COL_DESCRIPTION : null;
			var rows = reader.rows();
			if (rows.isEmpty()) {
				throw new ValidationFailedException("The sheet has no rows below the header row");
			}
			if (rows.size() > MAX_ROWS) {
				throw new ValidationFailedException("The sheet has %d rows, but at most %d rows can be imported at once".formatted(rows.size(), MAX_ROWS));
			}
			var parsed = new ArrayList<ParsedRow>(rows.size());
			for (var row : rows) {
				parsed.add(parseRow(row, commentColumn));
			}
			return parsed;
		} catch (ValidationFailedException e) {
			throw e;
		} catch (RuntimeException e) {
			throw new ValidationFailedException("Unable to read the spreadsheet: " + e.getMessage());
		}
	}

	private static ParsedRow parseRow(ExcelReader.RowReader row, String commentColumn) {
		var area = row.text(TickTemplate.COL_AREA).orElse(null);
		var sector = row.text(TickTemplate.COL_SECTOR).orElse(null);
		var name = row.text(TickTemplate.COL_NAME).orElse(null);
		var dateText = row.text(TickTemplate.COL_DATE).orElse(null);
		var date = row.date(TickTemplate.COL_DATE).orElse(null);
		var starsText = row.text(TickTemplate.COL_STARS).orElse(null);
		var comment = commentColumn == null ? null : row.text(commentColumn).orElse(null);
		Double stars = null;
		String invalidMessage = null;
		if (starsText != null) {
			var value = parseInt(starsText);
			if (value == null || value < 0 || value > 3) {
				invalidMessage = "STARS must be empty or a number from 0 to 3";
			} else {
				stars = value.doubleValue();
			}
		}
		if (name == null) {
			invalidMessage = "NAME is required";
		} else if (date == null) {
			invalidMessage = "DATE must be a date in YYYY-MM-DD format, for instance 2024-05-01";
		} else if (comment != null && comment.length() > MAX_COMMENT_LENGTH) {
			invalidMessage = "COMMENT must be at most %d characters".formatted(MAX_COMMENT_LENGTH);
		}
		return new ParsedRow(row.rowNumber(), area, sector, name, dateText, date, stars, comment, invalidMessage);
	}

	private Resolution resolve(int userId, List<ParsedRow> parsed) {
		var results = new TreeMap<Integer, TickImportRow>();
		var pending = new ArrayList<ParsedRow>();
		for (var p : parsed) {
			if (p.invalidMessage() == null) {
				pending.add(p);
			} else {
				results.put(p.row(), p.toResult(null, Status.INVALID, p.invalidMessage()));
			}
		}
		if (pending.isEmpty()) {
			return new Resolution(results, Map.of());
		}
		var names = pending.stream().map(p -> normalize(p.name())).distinct().toList();
		var candidatesByName = problemRepo.getTickImportCandidates(userId, names).stream()
				.collect(Collectors.groupingBy(c -> normalize(c.name()), LinkedHashMap::new, Collectors.toList()));
		var matches = new LinkedHashMap<Integer, TickImportCandidate>();
		for (var p : pending) {
			var candidates = candidatesByName.getOrDefault(normalize(p.name()), List.<TickImportCandidate>of()).stream()
					.filter(c -> p.area() == null || normalize(p.area()).equals(normalize(c.area())))
					.filter(c -> p.sector() == null || normalize(p.sector()).equals(normalize(c.sector())))
					.toList();
			if (candidates.isEmpty()) {
				results.put(p.row(), p.toResult(null, Status.NOT_FOUND, "No problem named \"%s\"%s".formatted(p.name(), where(p))));
			} else if (candidates.size() > 1) {
				results.put(p.row(), p.toResult(null, Status.AMBIGUOUS, "%d problems are named \"%s\", add AREA and SECTOR to the row".formatted(candidates.size(), p.name())));
			} else {
				matches.put(p.row(), candidates.getFirst());
			}
		}
		var alreadyTicked = tickRepo.getTickedProblemIds(userId, matches.values().stream().map(TickImportCandidate::id).distinct().toList());
		var claimed = new HashSet<Integer>();
		for (var p : pending) {
			var match = matches.get(p.row());
			if (match == null) {
				continue;
			}
			if (alreadyTicked.contains(match.id())) {
				results.put(p.row(), p.toResult(match, Status.DUPLICATE, "Already ticked, the existing tick is left untouched"));
			} else if (!claimed.add(match.id())) {
				results.put(p.row(), p.toResult(match, Status.DUPLICATE_IN_FILE, "The problem is already ticked by an earlier row in this file"));
			} else {
				results.put(p.row(), p.toResult(match, Status.OK, "Ready to be saved"));
			}
		}
		return new Resolution(results, matches);
	}

	private static void validateUpload(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new ValidationFailedException("No file was uploaded");
		}
		var filename = Optional.ofNullable(file.getOriginalFilename()).orElse("").toLowerCase(Locale.ROOT);
		var dot = filename.lastIndexOf('.');
		var extension = dot < 0 ? "" : filename.substring(dot + 1);
		if (!XLSX_EXTENSIONS.contains(extension)) {
			throw new ValidationFailedException("Only xlsx files are supported, download the template and fill it in");
		}
	}

	private static Tick toTick(TickImportRow row, TickImportCandidate problem) {
		// stars=-1 counts as "no rating" (see TickRepository.setTick)
		double stars = row.stars() == null ? -1 : row.stars();
		return new Tick(false, -1, row.idProblem(), row.comment(), row.date(), stars, problem.grade(), null);
	}

	private static String where(ParsedRow p) {
		var parts = new ArrayList<String>();
		if (p.area() != null) {
			parts.add("AREA=" + p.area());
		}
		if (p.sector() != null) {
			parts.add("SECTOR=" + p.sector());
		}
		return parts.isEmpty() ? "" : " in " + String.join(", ", parts);
	}

	private static String normalize(String value) {
		return value == null ? "" : WHITESPACE.matcher(value.replace('\u00A0', ' ').strip()).replaceAll(" ").toLowerCase(Locale.ROOT);
	}

	private static Integer parseInt(String value) {
		try {
			return Integer.valueOf(value);
		} catch (NumberFormatException _) {
			return null;
		}
	}

	private record ParsedRow(int row, String area, String sector, String name, String dateText, LocalDate date, Double stars, String comment, String invalidMessage) {
		TickImportRow toResult(TickImportCandidate match, Status status, String message) {
			var dateOut = date == null ? dateText : date.toString();
			return new TickImportRow(row, area, sector, name, dateOut, stars, comment, match == null ? null : match.id(), status, message);
		}
	}

	/**
	 * What an import would do, computed without touching the database. {@link #toPreviewResult()} renders it as the
	 * dry-run response; {@link TickImportService#apply} turns it into the saved ticks.
	 */
	private record Plan(Map<Integer, TickImportRow> results, Map<Integer, TickImportCandidate> matches,
			List<ParsedRow> toSave, int numSaved, int numSkipped, int numFailed) {
		TickImportResult toPreviewResult() {
			return new TickImportResult(List.copyOf(results.values()), numSaved, numSkipped, numFailed, false);
		}
	}

	private record Resolution(Map<Integer, TickImportRow> results, Map<Integer, TickImportCandidate> matches) {
	}
}


