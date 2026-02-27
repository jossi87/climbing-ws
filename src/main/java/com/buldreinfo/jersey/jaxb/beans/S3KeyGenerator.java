package com.buldreinfo.jersey.jaxb.beans;

public final class S3KeyGenerator {
    public static String getOriginalJpg(int id) {
        return "original/jpg/%s/%d.jpg".formatted(getFolderName(id), id);
    }

    public static String getOriginalMp4(int id) {
        return "original/mp4/%s/%d.mp4".formatted(getFolderName(id), id);
    }

    public static String getWebJpg(int id) {
        return "web/jpg/%s/%d.jpg".formatted(getFolderName(id), id);
    }

    public static String getWebJpgRegion(int id, int x, int y, int width, int height) {
        return "web/jpg_region/%s/%d/%d_%d_%d_%d.jpg".formatted(
                getFolderName(id), id, x, y, width, height);
    }

    public static String getWebJpgResized(int id, int targetWidth, int minDimension) {
        return getWebJpgResizedPrefix(id) + "w%d_m%d.jpg".formatted(targetWidth, minDimension);
    }
    
    public static String getWebJpgResizedPrefix(int id) {
        return "web/jpg_resized/%s/%d/".formatted(getFolderName(id), id);
    }
    
    public static String getWebMp4(int id) {
        return "web/mp4/%s/%d.mp4".formatted(getFolderName(id), id);
    }

    public static String getWebWebm(int id) {
        return "web/webm/%s/%d.webm".formatted(getFolderName(id), id);
    }

    public static String getWebWebp(int id) {
        return "web/webp/%s/%d.webp".formatted(getFolderName(id), id);
    }

    private static String getFolderName(int id) {
        return String.valueOf((id / 100) * 100);
    }

	private S3KeyGenerator() {}
}