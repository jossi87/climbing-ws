package com.buldreinfo.jersey.jaxb.batch;

import java.sql.PreparedStatement;

import com.buldreinfo.jersey.jaxb.db.ConnectionPoolProvider;
import com.buldreinfo.jersey.jaxb.db.DbConnection;
import com.buldreinfo.jersey.jaxb.helpers.GlobalFunctions;

public class MergeUsers {
	// SELECT * FROM user u, (SELECT TRIM(concat(TRIM(firstname), ' ', COALESCE(TRIM(lastname),''))) y FROM user GROUP BY TRIM(concat(TRIM(firstname), ' ', COALESCE(TRIM(lastname),''))) HAVING COUNT(TRIM(concat(TRIM(firstname), ' ', COALESCE(TRIM(lastname),''))))>1) x WHERE TRIM(concat(TRIM(u.firstname), ' ', TRIM(u.lastname)))=x.y ORDER BY firstname, lastname
	private final static int USER_ID_KEEP = -1;
	private final static int USER_ID_DELETE = -2;

	public static void main(String[] args) {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			// guestbook
			PreparedStatement ps = c.getConnection().prepareStatement("UPDATE guestbook SET user_id=? WHERE user_id=?");
			ps.setInt(1, USER_ID_KEEP);
			ps.setInt(2, USER_ID_DELETE);
			ps.execute();
			ps.close();
			// media
			ps = c.getConnection().prepareStatement("UPDATE media SET photographer_user_id=? WHERE photographer_user_id=?");
			ps.setInt(1, USER_ID_KEEP);
			ps.setInt(2, USER_ID_DELETE);
			ps.execute();
			ps.close();
			ps = c.getConnection().prepareStatement("UPDATE media SET uploader_user_id=? WHERE uploader_user_id=?");
			ps.setInt(1, USER_ID_KEEP);
			ps.setInt(2, USER_ID_DELETE);
			ps.execute();
			ps.close();
			ps = c.getConnection().prepareStatement("UPDATE media SET deleted_user_id=? WHERE deleted_user_id=?");
			ps.setInt(1, USER_ID_KEEP);
			ps.setInt(2, USER_ID_DELETE);
			ps.execute();
			ps.close();
			// media_user
			ps = c.getConnection().prepareStatement("UPDATE media_user SET user_id=? WHERE user_id=?");
			ps.setInt(1, USER_ID_KEEP);
			ps.setInt(2, USER_ID_DELETE);
			ps.execute();
			ps.close();
			// fa
			ps = c.getConnection().prepareStatement("UPDATE fa SET user_id=? WHERE user_id=?");
			ps.setInt(1, USER_ID_KEEP);
			ps.setInt(2, USER_ID_DELETE);
			ps.execute();
			ps.close();
			// tick
			ps = c.getConnection().prepareStatement("UPDATE tick SET user_id=? WHERE user_id=?");
			ps.setInt(1, USER_ID_KEEP);
			ps.setInt(2, USER_ID_DELETE);
			ps.execute();
			ps.close();
			// user_email
			ps = c.getConnection().prepareStatement("UPDATE user_email SET user_id=? WHERE user_id=?");
			ps.setInt(1, USER_ID_KEEP);
			ps.setInt(2, USER_ID_DELETE);
			ps.execute();
			ps.close();
			// android_user
			ps = c.getConnection().prepareStatement("UPDATE android_user SET user_id=? WHERE user_id=?");
			ps.setInt(1, USER_ID_KEEP);
			ps.setInt(2, USER_ID_DELETE);
			ps.execute();
			ps.close();
			// android_user
			ps = c.getConnection().prepareStatement("UPDATE user_login SET user_id=? WHERE user_id=?");
			ps.setInt(1, USER_ID_KEEP);
			ps.setInt(2, USER_ID_DELETE);
			ps.execute();
			ps.close();
			// user
			ps = c.getConnection().prepareStatement("DELETE FROM user WHERE id=?");
			ps.setInt(1, USER_ID_DELETE);
			ps.execute();
			ps.close();
			c.setSuccess();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}
}
