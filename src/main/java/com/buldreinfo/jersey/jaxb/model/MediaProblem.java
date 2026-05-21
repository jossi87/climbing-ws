package com.buldreinfo.jersey.jaxb.model;

public record MediaProblem(int problemId, String problemName, String problemGrade, int problemPitch, long milliseconds, String areaName, String sectorName) {}