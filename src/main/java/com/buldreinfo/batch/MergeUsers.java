package com.buldreinfo.batch;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionTemplate;
import com.buldreinfo.Application;

public class MergeUsers {
	// WITH u AS (SELECT TRIM(CONCAT(firstname,' ',COALESCE(lastname,''))) nm FROM user u GROUP BY TRIM(CONCAT(firstname,' ',COALESCE(lastname,''))) HAVING COUNT(TRIM(CONCAT(firstname,' ',COALESCE(lastname,''))))>1) SELECT u2.* FROM u, user u2 WHERE u.nm=TRIM(CONCAT(u2.firstname,' ',COALESCE(u2.lastname,''))) ORDER BY u2.firstname, u2.lastname
	/***
	 * Delete empty users
	 SELECT *
	 FROM user
	 WHERE id NOT IN (SELECT user_id FROM fa WHERE user_id IS NOT NULL)
	   AND id NOT IN (SELECT user_id FROM fa_aid_user WHERE user_id IS NOT NULL)
	   AND id NOT IN (SELECT user_id FROM tick WHERE user_id IS NOT NULL)
	   AND id NOT IN (SELECT user_id FROM todo WHERE user_id IS NOT NULL)
	   AND id NOT IN (SELECT photographer_user_id FROM media WHERE photographer_user_id IS NOT NULL)
	   AND id NOT IN (SELECT uploader_user_id FROM media WHERE uploader_user_id IS NOT NULL)
	   AND id NOT IN (SELECT deleted_user_id FROM media WHERE deleted_user_id IS NOT NULL)
	   AND id NOT IN (SELECT user_id FROM media_user WHERE user_id IS NOT NULL)
	   AND id NOT IN (SELECT user_id FROM user_login WHERE user_id IS NOT NULL)
	   AND id NOT IN (SELECT user_id FROM user_email WHERE user_id IS NOT NULL)
	   AND id NOT IN (SELECT user_id FROM guestbook WHERE user_id IS NOT NULL);
	 */
	private final static int USER_ID_DELETE = -1;
	private final static int USER_ID_KEEP = -2;

	public static void main(String[] args) {
		var context = new SpringApplicationBuilder(Application.class)
				.web(WebApplicationType.NONE)
				.run(args);

		var jdbcClient = context.getBean(JdbcClient.class);
		var transactionTemplate = context.getBean(TransactionTemplate.class);

		transactionTemplate.executeWithoutResult(_ -> {
			// guestbook
			jdbcClient.sql("UPDATE guestbook SET user_id=? WHERE user_id=?").params(USER_ID_KEEP, USER_ID_DELETE).update();
			// media
			jdbcClient.sql("UPDATE media SET photographer_user_id=? WHERE photographer_user_id=?").params(USER_ID_KEEP, USER_ID_DELETE).update();
			jdbcClient.sql("UPDATE media SET uploader_user_id=? WHERE uploader_user_id=?").params(USER_ID_KEEP, USER_ID_DELETE).update();
			jdbcClient.sql("UPDATE media SET deleted_user_id=? WHERE deleted_user_id=?").params(USER_ID_KEEP, USER_ID_DELETE).update();
			// media_user
			jdbcClient.sql("UPDATE media_user SET user_id=? WHERE user_id=?").params(USER_ID_KEEP, USER_ID_DELETE).update();
			// fa
			jdbcClient.sql("UPDATE fa SET user_id=? WHERE user_id=?").params(USER_ID_KEEP, USER_ID_DELETE).update();
			// fa_aid_user
			jdbcClient.sql("UPDATE fa_aid_user SET user_id=? WHERE user_id=?").params(USER_ID_KEEP, USER_ID_DELETE).update();
			// tick
			jdbcClient.sql("UPDATE tick SET user_id=? WHERE user_id=?").params(USER_ID_KEEP, USER_ID_DELETE).update();
			// user_email
			jdbcClient.sql("UPDATE user_email SET user_id=? WHERE user_id=?").params(USER_ID_KEEP, USER_ID_DELETE).update();
			// user_login
			jdbcClient.sql("UPDATE user_login SET user_id=? WHERE user_id=?").params(USER_ID_KEEP, USER_ID_DELETE).update();
			// user
			jdbcClient.sql("DELETE FROM user WHERE id=?").param(USER_ID_DELETE).update();
		});
	}
}