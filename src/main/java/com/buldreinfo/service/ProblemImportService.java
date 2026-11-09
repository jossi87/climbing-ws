package com.buldreinfo.service;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.buldreinfo.dao.ActivityRepository;
import com.buldreinfo.dao.AreaRepository;
import com.buldreinfo.dao.AreaRepository.ImportArea;
import com.buldreinfo.dao.GradeSystemRepository;
import com.buldreinfo.dao.ProblemRepository;
import com.buldreinfo.dao.ProblemRepository.ImportProblem;
import com.buldreinfo.dao.RegionRepository;
import com.buldreinfo.dao.SectorRepository;
import com.buldreinfo.dao.SectorRepository.ImportSector;
import com.buldreinfo.dao.UserRepository;
import com.buldreinfo.excel.ExcelReader;
import com.buldreinfo.excel.ProblemTemplate;
import com.buldreinfo.exception.ForbiddenException;
import com.buldreinfo.exception.ValidationFailedException;
import com.buldreinfo.helpers.GradeConverter;
import com.buldreinfo.model.ProblemImportResult;
import com.buldreinfo.model.ProblemImportRow;
import com.buldreinfo.model.ProblemImportRow.Status;
import com.buldreinfo.model.Type;

@Service
public class ProblemImportService {
	public static final int MAX_ROWS = 500;
	private static final int MAX_NAME_LENGTH = 255;
	private static final int MAX_DESCRIPTION_LENGTH = 4000;
	private static final int MAX_NR = 9999;
	private static final String COL_DESCRIPTION_ALIAS = "PROBLEM_DESCRIPTION";
	private static final Set<String> XLSX_EXTENSIONS = Set.of("xlsx", "xlsm");
	private static final Pattern WHITESPACE = Pattern.compile("\\s+");
	private static final Pattern USER_SEPARATOR = Pattern.compile("[,;]");

	private final ActivityRepository activityRepo;
	private final AreaRepository areaRepo;
	private final GradeSystemRepository gradeSystemRepo;
	private final ProblemRepository problemRepo;
	private final RegionRepository regionRepo;
	private final SectorRepository sectorRepo;
	private final UserRepository userRepo;

	public ProblemImportService(ActivityRepository activityRepo, AreaRepository areaRepo,
			GradeSystemRepository gradeSystemRepo, ProblemRepository problemRepo, RegionRepository regionRepo,
			SectorRepository sectorRepo, UserRepository userRepo) {
		this.activityRepo = activityRepo;
		this.areaRepo = areaRepo;
		this.gradeSystemRepo = gradeSystemRepo;
		this.problemRepo = problemRepo;
		this.regionRepo = regionRepo;
		this.sectorRepo = sectorRepo;
		this.userRepo = userRepo;
	}

	@Transactional(readOnly = true)
	public ProblemImportResult preview(int userId, int regionId, int idLanguage, MultipartFile file) {
		return process(userId, regionId, idLanguage, file, false);
	}

	@Transactional
	public ProblemImportResult commit(int userId, int regionId, int idLanguage, MultipartFile file) {
		return process(userId, regionId, idLanguage, file, true);
	}

	private ProblemImportResult process(int userId, int regionId, int idLanguage, MultipartFile file, boolean commit) {
		validateUpload(file);
		var types = regionRepo.getTypes(regionId, idLanguage);
		List<ParsedRow> parsed;
		try (InputStream in = file.getInputStream()) {
			parsed = read(in);
		} catch (IOException e) {
			throw new ValidationFailedException("Unable to read the uploaded file: " + e.getMessage());
		}
		var resolution = resolve(userId, regionId, types, parsed);
		var results = resolution.results();
		int numSaved = 0;
		int numSkipped = 0;
		int numFailed = 0;
		var toSave = new ArrayList<ParsedRow>();
		for (var p : parsed) {
			switch (results.get(p.row()).status()) {
				case OK -> {
					numSaved++;
					toSave.add(p);
				}
				case DUPLICATE, DUPLICATE_IN_FILE -> numSkipped++;
				default -> numFailed++;
			}
		}
		if (!commit) {
			return new ProblemImportResult(List.copyOf(results.values()), numSaved, numSkipped, numFailed,
					resolution.numSectorsCreated(), resolution.numUsersCreated(), false);
		}
		var createdSectors = new HashMap<String, Integer>();
		var touchedSectors = new LinkedHashSet<Integer>();
		var createdProblems = new ArrayList<Integer>();
		for (var p : toSave) {
			var row = results.get(p.row());
			int idSector = row.idSector() != null ? row.idSector()
					: createdSectors.computeIfAbsent(sectorKey(row.idArea(), p.sector()), _ -> sectorRepo.insertImportSector(row.idArea(), p.sector()));
			var faUserIds = new ArrayList<>(resolution.faUserIds().getOrDefault(p.row(), List.of()));
			for (var name : row.newUsers()) {
				faUserIds.add(userRepo.getExistingOrInsertUser(name));
			}
			int idProblem = problemRepo.insertImportProblem(idSector, row.nr(), p.name(), p.description(),
					resolution.typeIds().get(p.row()), resolution.gradeIds().get(p.row()), p.faDate(), faUserIds);
			createdProblems.add(idProblem);
			touchedSectors.add(idSector);
			results.put(p.row(), row.withResult(idSector, idProblem, Status.SAVED, "Saved"));
		}
		for (int idSector : touchedSectors) {
			sectorRepo.normalizeProblemOrder(idSector);
		}
		// The FA rows are stamped with the FA date (see ActivityRepository#applyIdOffset), so the imported problems
		// enter the activity feed at the date they were established and not today.
		activityRepo.fillActivity(createdProblems);
		return new ProblemImportResult(List.copyOf(results.values()), numSaved, numSkipped, numFailed,
				createdSectors.size(), resolution.numUsersCreated(), true);
	}

	private List<ParsedRow> read(InputStream in) throws IOException {
		try (var reader = ExcelReader.open(in, ProblemTemplate.SHEET_PROBLEMS)) {
			for (var required : List.of(ProblemTemplate.COL_AREA, ProblemTemplate.COL_SECTOR, ProblemTemplate.COL_NR,
					ProblemTemplate.COL_PROBLEM, ProblemTemplate.COL_TYPE, ProblemTemplate.COL_GRADE)) {
				if (!reader.hasColumn(required)) {
					throw new ValidationFailedException("The sheet is missing the " + required + " column. Did you fill in the template?");
				}
			}
			String descriptionColumn = reader.hasColumn(ProblemTemplate.COL_DESCRIPTION) ? ProblemTemplate.COL_DESCRIPTION
					: reader.hasColumn(COL_DESCRIPTION_ALIAS) ? COL_DESCRIPTION_ALIAS : null;
			var rows = reader.rows();
			if (rows.isEmpty()) {
				throw new ValidationFailedException("The sheet has no rows below the header row");
			}
			if (rows.size() > MAX_ROWS) {
				throw new ValidationFailedException("The sheet has %d rows, but at most %d rows can be imported at once".formatted(rows.size(), MAX_ROWS));
			}
			var parsed = new ArrayList<ParsedRow>(rows.size());
			for (var row : rows) {
				parsed.add(parseRow(row, descriptionColumn));
			}
			return parsed;
		} catch (ValidationFailedException e) {
			throw e;
		} catch (RuntimeException e) {
			throw new ValidationFailedException("Unable to read the spreadsheet: " + e.getMessage());
		}
	}

	private static ParsedRow parseRow(ExcelReader.RowReader row, String descriptionColumn) {
		var area = row.text(ProblemTemplate.COL_AREA).orElse(null);
		var sector = row.text(ProblemTemplate.COL_SECTOR).orElse(null);
		var nrText = row.text(ProblemTemplate.COL_NR).orElse(null);
		var name = row.text(ProblemTemplate.COL_PROBLEM).orElse(null);
		var description = descriptionColumn == null ? null : row.text(descriptionColumn).orElse(null);
		var type = row.text(ProblemTemplate.COL_TYPE).orElse(null);
		var grade = row.text(ProblemTemplate.COL_GRADE).orElse(null);
		var faDateText = row.text(ProblemTemplate.COL_FA_DATE).orElse(null);
		var faDate = row.date(ProblemTemplate.COL_FA_DATE).orElse(null);
		var faUsersText = row.text(ProblemTemplate.COL_FA_USERS).orElse(null);
		var faUsers = new ArrayList<String>();
		if (faUsersText != null) {
			var seen = new HashSet<String>();
			for (var part : USER_SEPARATOR.split(faUsersText)) {
				var user = part.strip();
				if (!user.isEmpty() && seen.add(normalize(user))) {
					faUsers.add(user);
				}
			}
		}
		var nr = nrText == null ? null : parseInt(nrText);
		String invalidMessage = null;
		if (area == null) {
			invalidMessage = "AREA is required";
		} else if (sector == null) {
			invalidMessage = "SECTOR is required";
		} else if (nrText == null) {
			invalidMessage = "NR is required";
		} else if (nr == null || nr < 1 || nr > MAX_NR) {
			invalidMessage = "NR must be a whole number from 1 to " + MAX_NR;
		} else if (name == null) {
			invalidMessage = "PROBLEM is required";
		} else if (name.length() > MAX_NAME_LENGTH) {
			invalidMessage = "PROBLEM must be at most %d characters".formatted(MAX_NAME_LENGTH);
		} else if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
			invalidMessage = "DESCRIPTION must be at most %d characters".formatted(MAX_DESCRIPTION_LENGTH);
		} else if (type == null) {
			invalidMessage = "TYPE is required";
		} else if (grade == null) {
			invalidMessage = "GRADE is required";
		} else if (faDateText != null && faDate == null) {
			invalidMessage = "FA DATE must be a date in YYYY-MM-DD format, for instance 2024-05-01";
		} else if (!faUsers.isEmpty() && faDate == null) {
			invalidMessage = "FA DATE is required when FA USERS is filled";
		}
		return new ParsedRow(row.rowNumber(), area, sector, nr, name, description, type, grade, faDateText, faDate,
				faUsers, invalidMessage);
	}

	private Resolution resolve(int userId, int regionId, List<Type> types, List<ParsedRow> parsed) {
		var results = new TreeMap<Integer, ProblemImportRow>();
		var pending = new ArrayList<ParsedRow>();
		for (var p : parsed) {
			if (p.invalidMessage() == null) {
				pending.add(p);
			} else {
				results.put(p.row(), p.toResult(null, null, null, false, List.of(), Status.INVALID, p.invalidMessage()));
			}
		}
		var faUserIds = new LinkedHashMap<Integer, List<Integer>>();
		var typeIds = new LinkedHashMap<Integer, Integer>();
		var gradeIds = new LinkedHashMap<Integer, Integer>();
		if (pending.isEmpty()) {
			return new Resolution(results, faUserIds, typeIds, gradeIds, 0, 0);
		}

		var areasByName = new HashMap<String, List<ImportArea>>();
		for (var a : areaRepo.getAreasForImport(regionId)) {
			areasByName.computeIfAbsent(normalize(a.name()), _ -> new ArrayList<>()).add(a);
		}
		var sectorsByArea = new HashMap<Integer, Map<String, List<ImportSector>>>();
		for (var s : sectorRepo.getSectorsForImport(regionId)) {
			sectorsByArea.computeIfAbsent(s.areaId(), _ -> new HashMap<>()).computeIfAbsent(normalize(s.name()), _ -> new ArrayList<>()).add(s);
		}
		var problemsBySector = new HashMap<Integer, Map<String, List<ImportProblem>>>();
		var nrsBySector = new HashMap<Integer, Set<Integer>>();
		for (var p : problemRepo.getProblemsForImport(regionId)) {
			problemsBySector.computeIfAbsent(p.sectorId(), _ -> new HashMap<>()).computeIfAbsent(normalize(p.name()), _ -> new ArrayList<>()).add(p);
			nrsBySector.computeIfAbsent(p.sectorId(), _ -> new HashSet<>()).add(p.nr());
		}
		var typesByLabel = new HashMap<String, Type>();
		for (var t : types) {
			typesByLabel.putIfAbsent(normalize(ProblemTemplate.typeLabel(t)), t);
			typesByLabel.putIfAbsent(normalize(t.type()), t);
			if (t.subType() != null) {
				typesByLabel.putIfAbsent(normalize(t.type() + " " + t.subType()), t);
			}
		}
		var faUsers = new LinkedHashSet<String>();
		for (var p : pending) {
			faUsers.addAll(p.faUsers());
		}
		var existingUsers = userRepo.getUserIdsByNames(faUsers);

		var claimedNrs = new HashMap<String, Map<Integer, Integer>>();
		var claimedProblems = new HashMap<String, Integer>();
		var newSectors = new HashSet<String>();
		var createdUsers = new LinkedHashSet<String>();
		var converters = new HashMap<Integer, GradeConverter>();
		var writableAreas = new HashSet<Integer>();
		var writableSectors = new HashSet<Integer>();

		for (var p : pending) {
			var areas = areasByName.getOrDefault(normalize(p.area()), List.of());
			if (areas.isEmpty()) {
				results.put(p.row(), p.toResult(null, null, null, false, List.of(), Status.NOT_FOUND, "No area named \"%s\" in this region".formatted(p.area())));
				continue;
			}
			if (areas.size() > 1) {
				results.put(p.row(), p.toResult(null, null, null, false, List.of(), Status.AMBIGUOUS, "%d areas are named \"%s\"".formatted(areas.size(), p.area())));
				continue;
			}
			int idArea = areas.getFirst().id();
			var sectors = sectorsByArea.getOrDefault(idArea, Map.of()).getOrDefault(normalize(p.sector()), List.of());
			if (sectors.size() > 1) {
				results.put(p.row(), p.toResult(idArea, null, null, false, List.of(), Status.AMBIGUOUS, "%d sectors in this area are named \"%s\"".formatted(sectors.size(), p.sector())));
				continue;
			}
			Integer idSector = sectors.isEmpty() ? null : sectors.getFirst().id();
			boolean newSector = idSector == null;
			if (!newSector) {
				var existing = problemsBySector.getOrDefault(idSector, Map.of()).getOrDefault(normalize(p.name()), List.of());
				if (!existing.isEmpty()) {
					results.put(p.row(), p.toResult(idArea, idSector, null, false, List.of(), Status.DUPLICATE, "The sector already has a problem named \"%s\"".formatted(p.name())));
					continue;
				}
			}
			var firstRow = claimedProblems.putIfAbsent(sectorKey(idArea, p.sector()) + "|" + normalize(p.name()), p.row());
			if (firstRow != null) {
				results.put(p.row(), p.toResult(idArea, idSector, null, newSector, List.of(), Status.DUPLICATE_IN_FILE, "Row %d already adds a problem named \"%s\" to this sector".formatted(firstRow, p.name())));
				continue;
			}
			var nrOwner = claimedNrs.computeIfAbsent(sectorKey(idArea, p.sector()), _ -> new HashMap<>()).putIfAbsent(p.nr(), p.row());
			if (nrOwner != null) {
				results.put(p.row(), p.toResult(idArea, idSector, null, newSector, List.of(), Status.INVALID, "NR %d is already used by row %d of this file".formatted(p.nr(), nrOwner)));
				continue;
			}
			var type = typesByLabel.get(normalize(p.type()));
			if (type == null) {
				results.put(p.row(), p.toResult(idArea, idSector, null, newSector, List.of(), Status.INVALID, "Unknown TYPE \"%s\", pick one of the types in the template".formatted(p.type())));
				continue;
			}
			int gradeId;
			try {
				var converter = converters.computeIfAbsent(idArea, gradeSystemRepo::getConverterForArea);
				gradeId = converter.getIdGradeFromGrade(p.grade());
			} catch (ValidationFailedException e) {
				results.put(p.row(), p.toResult(idArea, idSector, null, newSector, List.of(), Status.INVALID, e.getMessage()));
				continue;
			}
			if (!canWrite(userId, idArea, idSector, writableAreas, writableSectors)) {
				results.put(p.row(), p.toResult(idArea, idSector, null, newSector, List.of(), Status.FORBIDDEN, "You do not have write access to area \"%s\"".formatted(p.area())));
				continue;
			}
			var newUsers = new ArrayList<String>();
			var userIds = new ArrayList<Integer>();
			for (var user : p.faUsers()) {
				var id = existingUsers.get(normalize(user));
				if (id == null) {
					newUsers.add(user);
					createdUsers.add(user);
				} else {
					userIds.add(id);
				}
			}
			faUserIds.put(p.row(), userIds);
			typeIds.put(p.row(), type.id());
			gradeIds.put(p.row(), gradeId);
			if (newSector) {
				newSectors.add(sectorKey(idArea, p.sector()));
			}
			results.put(p.row(), p.toResult(idArea, idSector, null, newSector, newUsers, Status.OK,
					okMessage(p, newSector, idSector, nrsBySector, newUsers)));
		}
		return new Resolution(results, faUserIds, typeIds, gradeIds, newSectors.size(), createdUsers.size());
	}

	private boolean canWrite(int userId, int idArea, Integer idSector, Set<Integer> writableAreas, Set<Integer> writableSectors) {
		try {
			if (idSector == null) {
				if (!writableAreas.contains(idArea)) {
					areaRepo.ensureAdminWriteArea(Optional.of(userId), idArea);
					writableAreas.add(idArea);
				}
			} else if (!writableSectors.contains(idSector)) {
				sectorRepo.ensureAdminWriteSector(Optional.of(userId), idSector);
				writableSectors.add(idSector);
			}
			return true;
		} catch (ForbiddenException | IllegalArgumentException _) {
			return false;
		}
	}

	private static String okMessage(ParsedRow p, boolean newSector, Integer idSector, Map<Integer, Set<Integer>> nrsBySector, List<String> newUsers) {
		var message = new StringBuilder("Ready to be imported");
		if (newSector) {
			message.append(", the sector will be created");
		} else if (nrsBySector.getOrDefault(idSector, Set.of()).contains(p.nr())) {
			message.append(", NR ").append(p.nr()).append(" is already used in this sector");
		}
		if (!newUsers.isEmpty()) {
			message.append(", new users: ").append(String.join(", ", newUsers));
		}
		return message.toString();
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

	private static String sectorKey(int idArea, String sector) {
		return idArea + "|" + normalize(sector);
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

	private record ParsedRow(int row, String area, String sector, Integer nr, String name, String description,
			String type, String grade, String faDateText, LocalDate faDate, List<String> faUsers, String invalidMessage) {
		ProblemImportRow toResult(Integer idArea, Integer idSector, Integer idProblem, boolean newSector,
				List<String> newUsers, Status status, String message) {
			var dateOut = faDate == null ? faDateText : faDate.toString();
			return new ProblemImportRow(row, area, sector, nr, name, description, type, grade, dateOut,
					faUsers.isEmpty() ? null : String.join(", ", faUsers), idArea, idSector, idProblem, newSector,
					newUsers, status, message);
		}
	}

	private record Resolution(Map<Integer, ProblemImportRow> results, Map<Integer, List<Integer>> faUserIds,
			Map<Integer, Integer> typeIds, Map<Integer, Integer> gradeIds, int numSectorsCreated, int numUsersCreated) {
	}
}
