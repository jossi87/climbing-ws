package com.buldreinfo.jersey.jaxb.batch;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Set;
import java.util.TreeSet;

import javax.imageio.ImageIO;

import org.imgscalr.Scalr;

import com.buldreinfo.jersey.jaxb.db.ConnectionPoolProvider;
import com.buldreinfo.jersey.jaxb.db.DbConnection;
import com.buldreinfo.jersey.jaxb.helpers.GlobalFunctions;
import com.google.common.base.Preconditions;

public class FixAvatars {
	public static void main(String[] args) {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final Path originalFolder = Paths.get("C:/users/joste_000/desktop/test/original");
			final Path resizedFolder = Paths.get("C:/users/joste_000/desktop/test/resized");
			Files.createDirectories(originalFolder.getParent());
			Files.createDirectories(resizedFolder.getParent());
			Set<String> missingFiles = new TreeSet<>();
			try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT u.id, u.picture FROM user u WHERE u.picture IS NOT NULL");
					ResultSet rst = ps.executeQuery();) {
				while (rst.next()) {
					int id = rst.getInt("id");
					String picture = rst.getString("picture");
					Path original = originalFolder.resolve(id + ".jpg");
					Path resized = resizedFolder.resolve(id + ".jpg");
					if (!Files.exists(original)) {
						try {
							try (InputStream in = new URL(picture).openStream()) {
								Files.copy(in, original, StandardCopyOption.REPLACE_EXISTING);
								in.close();
							}
						} catch (Exception e) {
							missingFiles.add(original.toString());
						}
					}
					if (Files.exists(original)) {
						// Create new resized
						Files.deleteIfExists(resized);
						BufferedImage bOriginal = ImageIO.read(original.toFile());
						BufferedImage bScaled = Scalr.resize(bOriginal, Scalr.Mode.FIT_EXACT, 35, 35, Scalr.OP_ANTIALIAS);
						ImageIO.write(bScaled, "jpg", resized.toFile());
						bOriginal.flush();
						bOriginal = null;
						bScaled.flush();
						bScaled = null;
						Preconditions.checkArgument(Files.exists(resized));
					}
				}
			}
			for (String missingFile : missingFiles) {
				System.err.println(missingFile);
			}
			c.setSuccess();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}
}
