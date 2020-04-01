package com.buldreinfo.jersey.jaxb.batch;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.buldreinfo.jersey.jaxb.db.ConnectionPoolProvider;
import com.buldreinfo.jersey.jaxb.db.DbConnection;
import com.buldreinfo.jersey.jaxb.helpers.GlobalFunctions;

public class FillActivity {
	private static Logger logger = LogManager.getLogger();
	
	public static void main(String[] args) {
		new FillActivity();
	}

	public FillActivity() {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			PreparedStatement ps = c.getConnection().prepareStatement("SELECT id FROM problem ORDER BY id");
			ResultSet rst = ps.executeQuery();
			int done = 0;
			while (rst.next()) {
				c.getBuldreinfoRepo().fillActivity(rst.getInt("id"));
				if ((++done) % 500 == 0) {
					logger.debug("Done with " + done + " problems");
				}
			}
			rst.close();
			ps.close();
			c.setSuccess();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}
}