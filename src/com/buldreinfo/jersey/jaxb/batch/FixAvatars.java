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

import javax.imageio.ImageIO;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.imgscalr.Scalr;

import com.buldreinfo.jersey.jaxb.db.ConnectionPoolProvider;
import com.buldreinfo.jersey.jaxb.db.DbConnection;
import com.buldreinfo.jersey.jaxb.helpers.GlobalFunctions;
import com.google.common.base.Preconditions;

public class FixAvatars {
	private static Logger logger = LogManager.getLogger();
	
	public static void main(String[] args) {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final Path originalFolder = Paths.get("G:/My Drive/web/buldreinfo/buldreinfo_media/original/users");
			final Path resizedFolder = Paths.get("G:/My Drive/web/buldreinfo/buldreinfo_media/web/users");
			Files.createDirectories(originalFolder.getParent());
			Files.createDirectories(resizedFolder.getParent());
			int counter = 0;
			try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT u.id, u.picture FROM user u WHERE u.picture IS NOT NULL ORDER BY u.id DESC");
					ResultSet rst = ps.executeQuery()) {
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
								logger.debug("Downloaded " + original.toString());
							}
						} catch (Exception e) {
							logger.warn("Could not download {}, Files.exists(resized)={}", original.toString(), Files.exists(resized));
						}
					}
					if (Files.exists(original) && !Files.exists(resized)) {
						BufferedImage bOriginal = ImageIO.read(original.toFile());
						BufferedImage bScaled = Scalr.resize(bOriginal, Scalr.Mode.FIT_EXACT, 35, 35, Scalr.OP_ANTIALIAS);
						ImageIO.write(bScaled, "jpg", resized.toFile());
						bOriginal.flush();
						bOriginal = null;
						bScaled.flush();
						bScaled = null;
						Preconditions.checkArgument(Files.exists(resized));
						logger.debug("Created " + resized.toString());
					}
					if (++counter % 250 == 0) {
						logger.debug("Done with {} users, id={}", counter, id);
					}
				}
			}
			c.setSuccess();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}
}
