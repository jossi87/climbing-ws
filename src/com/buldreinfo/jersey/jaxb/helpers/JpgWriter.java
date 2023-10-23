package com.buldreinfo.jersey.jaxb.helpers;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;

import com.google.common.base.Preconditions;

public class JpgWriter {

	public static void write(BufferedImage b, Path dst) throws IOException {
		Preconditions.checkArgument(Files.exists(dst.getParent()), dst.getParent().toString() + " does not exist");
		Preconditions.checkArgument(!Files.exists(dst), dst.toString() + " already exists");
		boolean ok = ImageIO.write(b, "jpg", dst.toFile());
		if (!ok) {
			boolean isPng = ImageIO.getImageWriters(ImageTypeSpecifier.createFromRenderedImage(b), "png").hasNext();
			Preconditions.checkArgument(isPng, "No writer available, cannot save " + dst.toString());
			
			BufferedImage newImage = new BufferedImage(b.getWidth(), b.getHeight(), BufferedImage.TYPE_INT_RGB);
			newImage.createGraphics().drawImage(b, 0, 0, Color.BLACK, null);
			ok = ImageIO.write(newImage, "jpg", dst.toFile());
			newImage.flush();
			Preconditions.checkArgument(ok, "Could not save " + dst.toString());
		}
	}
}
