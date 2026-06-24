package com.buldreinfo.batch;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Optional;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;

import com.buldreinfo.Application; // Ensure this imports your main @SpringBootApplication class
import com.buldreinfo.dao.AreaRepository;
import com.buldreinfo.dao.HierarchyRepository;
import com.buldreinfo.dao.ProblemRepository;
import com.buldreinfo.dao.RegionRepository;
import com.buldreinfo.dao.SectorRepository;
import com.buldreinfo.infrastructure.ClimbingTransactionManager;
import com.buldreinfo.io.StorageManager;
import com.buldreinfo.model.Sector;
import com.buldreinfo.pdf.PdfGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

public class WritePdf {
    public static void main(String[] args) throws Exception {
        var context = new SpringApplicationBuilder(Application.class)
                .web(WebApplicationType.NONE)
                .run(args);
        var objectMapper = context.getBean(ObjectMapper.class);
        var storage = context.getBean(StorageManager.class);
        var txManager = context.getBean(ClimbingTransactionManager.class);
        var areaRepo = context.getBean(AreaRepository.class);
        var problemRepo = context.getBean(ProblemRepository.class);
        var sectorRepo = context.getBean(SectorRepository.class);
        var hierarchyRepo = context.getBean(HierarchyRepository.class);
        var regionRepo = context.getBean(RegionRepository.class);

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
                    PdfGenerator generator = new PdfGenerator(objectMapper, storage, fos)) {
                generator.writeProblem(setup, area, sector, problem);
            }
            var area2 = areaRepo.getArea(setup, authUserId, 2754, shouldUpdateHits);
            var gradeDist = hierarchyRepo.getGradeDistribution(authUserId, area2.id(), 0);
            var sectors = new ArrayList<Sector>();
            for (var areaSector : area2.sectors()) {
                sectors.add(sectorRepo.getSector(authUserId, false, setup, areaSector.id(), shouldUpdateHits));
            }
            try (var fos = new FileOutputStream("C:/Users/JosteinØygarden/Desktop/area.pdf");
                    PdfGenerator generator = new PdfGenerator(objectMapper, storage, fos)) {
                generator.writeArea(setup, area2, gradeDist, sectors);
            }
            return null;
        });
        System.out.println("PDFs generated successfully.");
        System.exit(0);
    }
}