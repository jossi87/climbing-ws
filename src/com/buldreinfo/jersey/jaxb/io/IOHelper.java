package com.buldreinfo.jersey.jaxb.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

import org.apache.commons.lang3.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.buldreinfo.jersey.jaxb.beans.Setup;
import com.google.common.base.Preconditions;

public class IOHelper {
	private static final Logger logger = LogManager.getLogger();

	public static void createDirectories(Path dir) throws IOException {
		if (!Files.exists(dir)) {
			Files.createDirectories(dir);
			setFilePermission(dir);
		}
	}

	public static void deleteIfExistsCreateParent(Path p) throws IOException {
		if (Files.exists(p)) {
			Files.delete(p);
		}
		else {
			createDirectories(p.getParent());
		}
	}

	public static void deleteResizedCache(int id) throws IOException {
	    Path parentDir = getPathMediaWebJpgResizedParent(id);
	    if (!Files.exists(parentDir)) {
	        return;
	    }
	    // Pattern: id_w... (e.g., 123_w300_m0.jpg)
	    String prefix = id + "_w";
	    try (var stream = Files.list(parentDir)) {
	        stream.filter(path -> path.getFileName().toString().startsWith(prefix))
	              .forEach(path -> {
	                  try {
	                      Files.deleteIfExists(path);
	                      logger.debug("Deleted cached resize: {}", path);
	                  } catch (IOException e) {
	                      logger.error("Failed to delete cached resize: " + path, e);
	                  }
	              });
	    }
	}

	public static String getFullUrlAvatar(Setup setup, int userId, long avatarCrc32) {
		return String.format("%s/com.buldreinfo.jersey.jaxb/v2/avatar?id=%d&avatarCrc32=%d", setup.url(), userId, avatarCrc32);
	}
	
	public static Path getPathImage(int id, boolean webP) {
		Path p = null;
		if (webP) {
			p = getPathMediaWebWebp(id);
		} else {
			p = getPathMediaWebJpg(id);
		}
		Preconditions.checkArgument(Files.exists(p), p.toString() + " does not exist");
		return p;
	}

	public static Path getPathMediaOriginalJpg(int id) {
		return getPathRoot().resolve("original/jpg").resolve(getFolderName(id)).resolve(id + ".jpg");
	}

	public static Path getPathMediaOriginalMp4(int id) {
		return getPathRoot().resolve("original/mp4").resolve(getFolderName(id)).resolve(id + ".mp4");
	}
	
	public static Path getPathMediaWebJpg(int id) {
		return getPathRoot().resolve("web/jpg").resolve(getFolderName(id)).resolve(id + ".jpg");
	}

	public static Path getPathMediaWebJpgRegion(int id, int x, int y, int width, int height) {
		return getPathRoot().resolve("web/jpg_region").resolve(getFolderName(id)).resolve(String.valueOf(id)).resolve(x + "_" + y + "_" + width + "_" + height + ".jpg");
	}

	public static Path getPathMediaWebJpgResized(int id, int targetWidth, int minDimension) {
	    String filename = String.format("%d_w%d_m%d.jpg", id, targetWidth, minDimension);
	    return getPathMediaWebJpgResizedParent(id).resolve(filename);
	}

	public static Path getPathMediaWebMp4(int id) {
		return getPathRoot().resolve("web/mp4").resolve(getFolderName(id)).resolve(id + ".mp4");
	}

	public static Path getPathMediaWebWebm(int id) {
		return getPathRoot().resolve("web/webm").resolve(getFolderName(id)).resolve(id + ".webm");
	}

	public static Path getPathMediaWebWebp(int id) {
		return getPathRoot().resolve("web/webp").resolve(getFolderName(id)).resolve(id + ".webp");
	}
	
	public static Path getPathOriginalUsers(int id) {
		return getPathRoot().resolve("original/users").resolve(id + ".jpg");
	}

	public static Path getPathWebUsers(int id) {
		return getPathRoot().resolve("web/users").resolve(id + ".jpg");
	}

	private static String getFolderName(int id) {
		return String.valueOf(id / 100 * 100);
	}

	private static Path getPathMediaWebJpgResizedParent(int id) {
	    return getPathRoot().resolve("web/jpg_resized").resolve(getFolderName(id));
	}

	private static Path getPathRoot() {
		Path root = null;
		if (!SystemUtils.IS_OS_WINDOWS) {
			root = Paths.get("/mnt/buldreinfo/media");
		}
		else {
			root = Paths.get("G:/My Drive/web/buldreinfo/buldreinfo_media");
		}
		Preconditions.checkArgument(root != null && Files.exists(root), "Invalid root: " + root);
		return root;
	}

	protected static void setFilePermission(Path p) {
		if (!SystemUtils.IS_OS_WINDOWS) {
			try {
				Set<PosixFilePermission> perms = Set.of(
						PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE,
						PosixFilePermission.OTHERS_READ, PosixFilePermission.OTHERS_WRITE,
						PosixFilePermission.GROUP_READ, PosixFilePermission.GROUP_WRITE);
				Files.setPosixFilePermissions(p, perms);
			} catch (IOException e) {
				logger.warn(e.getMessage(), e);
			}
		}
	}
}