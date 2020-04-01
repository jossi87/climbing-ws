package com.buldreinfo.jersey.jaxb.batch;

import com.buldreinfo.jersey.jaxb.db.ConnectionPoolProvider;
import com.buldreinfo.jersey.jaxb.db.DbConnection;
import com.buldreinfo.jersey.jaxb.helpers.GlobalFunctions;

public class FillActivity {
	public static void main(String[] args) {
		new FillActivity();
	}

	public FillActivity() {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			c.getBuldreinfoRepo().fillActivity(4935);
			c.setSuccess();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}
}