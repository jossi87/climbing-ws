package com.buldreinfo.jersey.jaxb.model;

import java.util.List;

public record FrontpageRandomMedia(int idMedia, long versionStamp, int width, int height, int idArea, String area, int idSector, String sector, int idProblem, String problem, String grade, User photographer, List<User> tagged) {}