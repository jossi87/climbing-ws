package com.buldreinfo.jersey.jaxb.batch;

import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.buldreinfo.jersey.jaxb.Server;
import com.buldreinfo.jersey.jaxb.model.Area;
import com.buldreinfo.jersey.jaxb.model.Meta;
import com.buldreinfo.jersey.jaxb.model.Problem;
import com.buldreinfo.jersey.jaxb.model.Sector;
import com.buldreinfo.jersey.jaxb.pdf.PdfGenerator;

public class WritePdf {
	public static void main(String[] args) {
		Server.runSql((dao, c) -> {
			var setup = Server.getSetups().stream()
					.filter(x -> x.idRegion() == 4)
					.findAny()
					.orElseThrow();
			var authUserId = Optional.of(1);
			final boolean shouldUpdateHits = false;
			var meta = Meta.from(dao, c, setup, authUserId);
			Problem problem = dao.getProblem(c, authUserId, setup, 7745, false, shouldUpdateHits);
			Area area = dao.getArea(c, setup, authUserId, problem.getAreaId(), shouldUpdateHits);
			Sector sector = dao.getSector(c, authUserId, false, setup, problem.getSectorId(), shouldUpdateHits);
			// Problem
			Path pProblem = Path.of("C:/Users/JosteinØygarden/Desktop/problem.pdf");
			try (var fos = new FileOutputStream(pProblem.toFile());
					PdfGenerator generator = new PdfGenerator(fos)) {
				generator.writeProblem(meta, area, sector, problem);
			}
			// Area
			area = dao.getArea(c, setup, authUserId, 2754, shouldUpdateHits);
			var gradeDistribution = dao.getGradeDistribution(c, authUserId, area.getId(), 0);
			Path pArea = Path.of("C:/Users/JosteinØygarden/Desktop/area.pdf");
			final List<Sector> sectors = new ArrayList<>();
			for (var areaSector : area.getSectors()) {
				sectors.add(dao.getSector(c, authUserId, false, setup, areaSector.getId(), shouldUpdateHits));
			}
			try (var fos = new FileOutputStream(pArea.toFile());
					PdfGenerator generator = new PdfGenerator(fos)) {
				generator.writeArea(meta, area, gradeDistribution, sectors);
			}
		});
	}
}