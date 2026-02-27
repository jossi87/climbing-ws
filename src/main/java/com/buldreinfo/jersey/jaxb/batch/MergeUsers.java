package com.buldreinfo.jersey.jaxb.batch;

import java.sql.PreparedStatement;

import com.buldreinfo.jersey.jaxb.Server;

public class MergeUsers {
	// WITH u AS (SELECT TRIM(CONCAT(firstname,' ',COALESCE(lastname,''))) nm FROM user u GROUP BY TRIM(CONCAT(firstname,' ',COALESCE(lastname,''))) HAVING COUNT(TRIM(CONCAT(firstname,' ',COALESCE(lastname,''))))>1) SELECT u2.* FROM u, user u2 WHERE u.nm=TRIM(CONCAT(u2.firstname,' ',COALESCE(u2.lastname,''))) ORDER BY u2.firstname, u2.lastname
	/***
	 * Delete empty users
	 SELECT *
	 FROM user
	 WHERE picture IS NULL
	   AND id NOT IN (SELECT user_id FROM fa WHERE user_id IS NOT NULL)
	   AND id NOT IN (SELECT user_id FROM fa_aid_user WHERE user_id IS NOT NULL)
	   AND id NOT IN (SELECT user_id FROM tick WHERE user_id IS NOT NULL)
	   AND id NOT IN (SELECT photographer_user_id FROM media WHERE photographer_user_id IS NOT NULL)
	   AND id NOT IN (SELECT uploader_user_id FROM media WHERE uploader_user_id IS NOT NULL)
	   AND id NOT IN (SELECT deleted_user_id FROM media WHERE deleted_user_id IS NOT NULL)
	   AND id NOT IN (SELECT user_id FROM media_user WHERE user_id IS NOT NULL)
	   AND id NOT IN (SELECT user_id FROM user_login WHERE user_id IS NOT NULL)
	   AND id NOT IN (SELECT user_id FROM user_email WHERE user_id IS NOT NULL)
	   AND id NOT IN (SELECT user_id FROM guestbook WHERE user_id IS NOT NULL);
	 */
	private final static int USER_ID_KEEP = -1;
	private final static int USER_ID_DELETE = -2;

	public static void main(String[] args) {
		Server.runSql((_, c) -> {
			// guestbook
			try (PreparedStatement ps = c.prepareStatement("UPDATE guestbook SET user_id=? WHERE user_id=?")) {
				ps.setInt(1, USER_ID_KEEP);
				ps.setInt(2, USER_ID_DELETE);
				ps.execute();
			}
			// media
			try (PreparedStatement ps = c.prepareStatement("UPDATE media SET photographer_user_id=? WHERE photographer_user_id=?")) {
				ps.setInt(1, USER_ID_KEEP);
				ps.setInt(2, USER_ID_DELETE);
				ps.execute();
			}
			try (PreparedStatement ps = c.prepareStatement("UPDATE media SET uploader_user_id=? WHERE uploader_user_id=?")) {
				ps.setInt(1, USER_ID_KEEP);
				ps.setInt(2, USER_ID_DELETE);
				ps.execute();
			}
			try (PreparedStatement ps = c.prepareStatement("UPDATE media SET deleted_user_id=? WHERE deleted_user_id=?")) {
				ps.setInt(1, USER_ID_KEEP);
				ps.setInt(2, USER_ID_DELETE);
				ps.execute();
			}
			// media_user
			try (PreparedStatement ps = c.prepareStatement("UPDATE media_user SET user_id=? WHERE user_id=?")) {
				ps.setInt(1, USER_ID_KEEP);
				ps.setInt(2, USER_ID_DELETE);
				ps.execute();
			}
			// fa
			try (PreparedStatement ps = c.prepareStatement("UPDATE fa SET user_id=? WHERE user_id=?")) {
				ps.setInt(1, USER_ID_KEEP);
				ps.setInt(2, USER_ID_DELETE);
				ps.execute();
			}
			// fa_aid_user
			try (PreparedStatement ps = c.prepareStatement("UPDATE fa_aid_user SET user_id=? WHERE user_id=?")) {
				ps.setInt(1, USER_ID_KEEP);
				ps.setInt(2, USER_ID_DELETE);
				ps.execute();
			}
			// tick
			try (PreparedStatement ps = c.prepareStatement("UPDATE tick SET user_id=? WHERE user_id=?")) {
				ps.setInt(1, USER_ID_KEEP);
				ps.setInt(2, USER_ID_DELETE);
				ps.execute();
			}
			// user_email
			try (PreparedStatement ps = c.prepareStatement("UPDATE user_email SET user_id=? WHERE user_id=?")) {
				ps.setInt(1, USER_ID_KEEP);
				ps.setInt(2, USER_ID_DELETE);
				ps.execute();
			}
			// user_login
			try (PreparedStatement ps = c.prepareStatement("UPDATE user_login SET user_id=? WHERE user_id=?")) {
				ps.setInt(1, USER_ID_KEEP);
				ps.setInt(2, USER_ID_DELETE);
				ps.execute();
			}
			// user
			try (PreparedStatement ps = c.prepareStatement("DELETE FROM user WHERE id=?")) {
				ps.setInt(1, USER_ID_DELETE);
				ps.execute();
			}
		});
	}
}