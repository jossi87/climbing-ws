package com.buldreinfo.jersey.jaxb.batch;

import java.sql.PreparedStatement;

import com.buldreinfo.jersey.jaxb.db.ConnectionPoolProvider;
import com.buldreinfo.jersey.jaxb.db.DbConnection;
import com.buldreinfo.jersey.jaxb.helpers.GlobalFunctions;

public class MergeUsers {
	// WITH u AS (SELECT CONCAT(firstname,COALESCE(lastname,'')) nm FROM user u GROUP BY CONCAT(firstname,COALESCE(lastname,'')) HAVING COUNT(CONCAT(firstname,COALESCE(lastname,'')))>1) SELECT u2.* FROM u, user u2 WHERE u.nm=CONCAT(u2.firstname,COALESCE(u2.lastname,'')) ORDER BY u2.firstname, u2.lastname
	private final static int USER_ID_KEEP = -1;
	private final static int USER_ID_DELETE = -2;

	public static void main(String[] args) {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			// guestbook
			try (PreparedStatement ps = c.getConnection().prepareStatement("UPDATE guestbook SET user_id=? WHERE user_id=?")) {
				ps.setInt(1, USER_ID_KEEP);
				ps.setInt(2, USER_ID_DELETE);
				ps.execute();
			}
			// media
			try (PreparedStatement ps = c.getConnection().prepareStatement("UPDATE media SET photographer_user_id=? WHERE photographer_user_id=?")) {
				ps.setInt(1, USER_ID_KEEP);
				ps.setInt(2, USER_ID_DELETE);
				ps.execute();
			}
			try (PreparedStatement ps = c.getConnection().prepareStatement("UPDATE media SET uploader_user_id=? WHERE uploader_user_id=?")) {
				ps.setInt(1, USER_ID_KEEP);
				ps.setInt(2, USER_ID_DELETE);
				ps.execute();
			}
			try (PreparedStatement ps = c.getConnection().prepareStatement("UPDATE media SET deleted_user_id=? WHERE deleted_user_id=?")) {
				ps.setInt(1, USER_ID_KEEP);
				ps.setInt(2, USER_ID_DELETE);
				ps.execute();
			}
			// media_user
			try (PreparedStatement ps = c.getConnection().prepareStatement("UPDATE media_user SET user_id=? WHERE user_id=?")) {
				ps.setInt(1, USER_ID_KEEP);
				ps.setInt(2, USER_ID_DELETE);
				ps.execute();
			}
			// fa
			try (PreparedStatement ps = c.getConnection().prepareStatement("UPDATE fa SET user_id=? WHERE user_id=?")) {
				ps.setInt(1, USER_ID_KEEP);
				ps.setInt(2, USER_ID_DELETE);
				ps.execute();
			}
			// fa_aid_user
			try (PreparedStatement ps = c.getConnection().prepareStatement("UPDATE fa_aid_user SET user_id=? WHERE user_id=?")) {
				ps.setInt(1, USER_ID_KEEP);
				ps.setInt(2, USER_ID_DELETE);
				ps.execute();
			}
			// tick
			try (PreparedStatement ps = c.getConnection().prepareStatement("UPDATE tick SET user_id=? WHERE user_id=?")) {
				ps.setInt(1, USER_ID_KEEP);
				ps.setInt(2, USER_ID_DELETE);
				ps.execute();
			}
			// user_email
			try (PreparedStatement ps = c.getConnection().prepareStatement("UPDATE user_email SET user_id=? WHERE user_id=?")) {
				ps.setInt(1, USER_ID_KEEP);
				ps.setInt(2, USER_ID_DELETE);
				ps.execute();
			}
			// android_user
			try (PreparedStatement ps = c.getConnection().prepareStatement("UPDATE android_user SET user_id=? WHERE user_id=?")) {
				ps.setInt(1, USER_ID_KEEP);
				ps.setInt(2, USER_ID_DELETE);
				ps.execute();
			}
			// android_user
			try (PreparedStatement ps = c.getConnection().prepareStatement("UPDATE user_login SET user_id=? WHERE user_id=?")) {
				ps.setInt(1, USER_ID_KEEP);
				ps.setInt(2, USER_ID_DELETE);
				ps.execute();
			}
			// user
			try (PreparedStatement ps = c.getConnection().prepareStatement("DELETE FROM user WHERE id=?")) {
				ps.setInt(1, USER_ID_DELETE);
				ps.execute();
			}
			c.setSuccess();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}
}
