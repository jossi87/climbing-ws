package com.buldreinfo.jersey.jaxb.batch;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.buldreinfo.jersey.jaxb.db.ConnectionPoolProvider;
import com.buldreinfo.jersey.jaxb.db.DbConnection;
import com.buldreinfo.jersey.jaxb.helpers.GlobalFunctions;

public class FillActivity {
	public static void main(String[] args) {
		new FillActivity();
	}

	public FillActivity() {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			PreparedStatement ps = c.getConnection().prepareStatement("SELECT id FROM problem ORDER BY id");
			ResultSet rst = ps.executeQuery();
			while (rst.next()) {
				c.getBuldreinfoRepo().fillActivity(rst.getInt("id"));
			}
			rst.close();
			ps.close();
			c.setSuccess();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}
}