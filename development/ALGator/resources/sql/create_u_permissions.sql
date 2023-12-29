CREATE TABLE `u_permissions` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `permission` VARCHAR(255) NOT NULL, 
  `permission_code` VARCHAR(50) NOT NULL, 
  PRIMARY KEY (`id`),
  UNIQUE INDEX `permission_code` (`permission_code`)
)
COLLATE='utf8_general_ci'
ENGINE=InnoDB
AUTO_INCREMENT=1;