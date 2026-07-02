package com.buldreinfo.batch;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;

import com.buldreinfo.Application;
import com.buldreinfo.dao.HierarchyRepository;
import com.buldreinfo.dao.RegionRepository;
import com.buldreinfo.io.StorageManager;
import com.buldreinfo.model.Sector;
import com.buldreinfo.pdf.PdfGenerator;
import com.buldreinfo.service.AreaService;
import com.buldreinfo.service.LeafletPrintService;
import com.buldreinfo.service.ProblemService;
import com.buldreinfo.service.SectorService;

public class WritePdf {
	public static void main(String[] args) throws FileNotFoundException, IOException {
		var context = new SpringApplicationBuilder(Application.class)
				.web(WebApplicationType.NONE)
				.run(args);
		var storage = context.getBean(StorageManager.class);
		var leafletPrintService = context.getBean(LeafletPrintService.class);
		var areaService = context.getBean(AreaService.class);
		var problemService = context.getBean(ProblemService.class);
		var sectorService = context.getBean(SectorService.class);
		var hierarchyRepo = context.getBean(HierarchyRepository.class);
		var regionRepo = context.getBean(RegionRepository.class);

		var setup = regionRepo.getSetups().stream()
				.filter(x -> x.idRegion() == 4)
				.findAny()
				.orElseThrow();
		var authUserId = Optional.of(1);
		var problem = problemService.getProblem(authUserId, setup, 7745, false);
		var area = areaService.getArea(setup, authUserId, problem.areaId());
		var sector = sectorService.getSector(authUserId, false, setup, problem.sectorId());
		try (var fos = new FileOutputStream("C:/Users/JosteinØygarden/Desktop/problem.pdf");
				PdfGenerator generator = new PdfGenerator(storage, leafletPrintService, fos)) {
			generator.writeProblem(setup, area, sector, problem);
		}
		var area2 = areaService.getArea(setup, authUserId, 2754);
		var gradeDist = hierarchyRepo.getGradeDistribution(authUserId, area2.id(), 0);
		var sectors = new ArrayList<Sector>();
		for (var areaSector : area2.sectors()) {
			sectors.add(sectorService.getSector(authUserId, false, setup, areaSector.id()));
		}
		try (var fos = new FileOutputStream("C:/Users/JosteinØygarden/Desktop/area.pdf");
				PdfGenerator generator = new PdfGenerator(storage, leafletPrintService, fos)) {
			generator.writeArea(setup, area2, gradeDist, sectors);
		}
		System.out.println("PDFs generated successfully.");
		System.exit(0);
	}
}