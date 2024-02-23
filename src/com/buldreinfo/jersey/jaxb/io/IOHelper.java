package com.buldreinfo.jersey.jaxb.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.base.Preconditions;

public class IOHelper {
	private static final Logger logger = LogManager.getLogger();
	
	public static void createDirectories(Path dir) throws IOException {
		if (!Files.exists(dir)) {
			Files.createDirectories(dir);
			setFilePermission(dir);
		}
	}
	
	public static Path getPathMediaOriginalJpg(int id) throws IOException {
		return getPathRoot().resolve("original/jpg").resolve(String.valueOf(id / 100 * 100)).resolve(id + ".jpg");
	}

	public static Path getPathMediaOriginalMp4(int id) throws IOException {
		return getPathRoot().resolve("original/mp4").resolve(String.valueOf(id / 100 * 100)).resolve(id + ".mp4");
	}
	
	public static Path getPathMediaWebJpg(int id) throws IOException {
		return getPathRoot().resolve("web/jpg").resolve(String.valueOf(id / 100 * 100)).resolve(id + ".jpg");
	}
	
	public static Path getPathMediaWebMp4(int id) throws IOException {
		return getPathRoot().resolve("web/mp4").resolve(String.valueOf(id / 100 * 100)).resolve(id + ".webm");
	}
	
	public static Path getPathMediaWebWebm(int id) throws IOException {
		return getPathRoot().resolve("web/webm").resolve(String.valueOf(id / 100 * 100)).resolve(id + ".webm");
	}

	public static Path getPathMediaWebWebp(int id) throws IOException {
		return getPathRoot().resolve("web/webp").resolve(String.valueOf(id / 100 * 100)).resolve(id + ".webp");
	}

	public static Path getPathTemp() throws IOException {
		return getPathRoot().resolve("temp");
	}

	private static Path getPathRoot() throws IOException {
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
	
	protected static void deleteIfExistsCreateParent(Path p) throws IOException {
		if (Files.exists(p)) {
			Files.delete(p);
		}
		else {
			createDirectories(p.getParent());
		}
	}

	protected static Path getPathOriginalUsers(int id) throws IOException {
		return getPathRoot().resolve("original/users").resolve(id + ".jpg");
	}

	protected static Path getPathWebUsers(int id) throws IOException {
		return getPathRoot().resolve("web/users").resolve(id + ".jpg");
	}

	protected static void setFilePermission(Path p) {
		if (!SystemUtils.IS_OS_WINDOWS) {
			try {
				Set<PosixFilePermission> perms = new HashSet<>();
				perms.add(PosixFilePermission.OWNER_READ);
				perms.add(PosixFilePermission.OWNER_WRITE);
				perms.add(PosixFilePermission.OTHERS_READ);
				perms.add(PosixFilePermission.OTHERS_WRITE);
				perms.add(PosixFilePermission.GROUP_READ);
				perms.add(PosixFilePermission.GROUP_WRITE);
				Files.setPosixFilePermissions(p, perms);
			} catch (IOException e) {
				logger.warn(e.getMessage(), e);
			}
		}
	}
}