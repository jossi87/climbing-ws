package com.buldreinfo.jersey.jaxb.batch;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Optional;

import com.buldreinfo.jersey.jaxb.dao.AreaRepository;
import com.buldreinfo.jersey.jaxb.dao.HierarchyRepository;
import com.buldreinfo.jersey.jaxb.dao.ProblemRepository;
import com.buldreinfo.jersey.jaxb.dao.RegionRepository;
import com.buldreinfo.jersey.jaxb.dao.SectorRepository;
import com.buldreinfo.jersey.jaxb.infrastructure.TransactionManager;
import com.buldreinfo.jersey.jaxb.model.Sector;
import com.buldreinfo.jersey.jaxb.pdf.PdfGenerator;

public class WritePdf {
	public static void main(String[] args) throws Exception {
		var locator = BatchBootstrapper.createLocator();
		var txManager = locator.getService(TransactionManager.class);
		var areaRepo = locator.getService(AreaRepository.class);
		var problemRepo = locator.getService(ProblemRepository.class);
		var sectorRepo = locator.getService(SectorRepository.class);
		var hierarchyRepo = locator.getService(HierarchyRepository.class);
		var regionRepo = locator.getService(RegionRepository.class);
		txManager.executeInTransaction(() -> {
			var setup = regionRepo.getSetups().stream()
					.filter(x -> x.idRegion() == 4)
					.findAny()
					.orElseThrow();
			var authUserId = Optional.of(1);
			final boolean shouldUpdateHits = false;
			var problem = problemRepo.getProblem(authUserId, setup, 7745, false, shouldUpdateHits);
			var area = areaRepo.getArea(setup, authUserId, problem.areaId(), shouldUpdateHits);
			var sector = sectorRepo.getSector(authUserId, false, setup, problem.sectorId(), shouldUpdateHits);
			try (var fos = new FileOutputStream("C:/Users/JosteinØygarden/Desktop/problem.pdf");
					PdfGenerator generator = new PdfGenerator(fos)) {
				generator.writeProblem(setup, area, sector, problem);
			}
			var area2 = areaRepo.getArea(setup, authUserId, 2754, shouldUpdateHits);
			var gradeDist = hierarchyRepo.getGradeDistribution(authUserId, area2.id(), 0);
			var sectors = new ArrayList<Sector>();
			for (var areaSector : area2.sectors()) {
				sectors.add(sectorRepo.getSector(authUserId, false, setup, areaSector.id(), shouldUpdateHits));
			}
			try (var fos = new FileOutputStream("C:/Users/JosteinØygarden/Desktop/area.pdf");
					PdfGenerator generator = new PdfGenerator(fos)) {
				generator.writeArea(setup, area2, gradeDist, sectors);
			}
			return null;
		});
		System.out.println("PDFs generated successfully.");
	}
}