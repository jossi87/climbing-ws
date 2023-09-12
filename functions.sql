DELIMITER $$
CREATE FUNCTION `is_readable`(ur_admin_read INT, ur_superadmin_read INT, locked_admin INT, locked_superadmin INT, trash TIMESTAMP) RETURNS int
    DETERMINISTIC
BEGIN
  IF (trash IS NOT NULL) THEN
    return 0;
  ELSEIF (locked_admin=0 AND locked_superadmin=0) THEN
    RETURN 1;
  ELSEIF (ur_superadmin_read=1) THEN
    RETURN 1;
  ELSEIF (ur_admin_read=1 AND locked_superadmin=0) THEN
    RETURN 1;
  ELSE
    RETURN 0;
  END IF;
END$$
DELIMITER ;
