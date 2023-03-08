CREATE TABLE `u_groups` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(255) NOT NULL,
  `status` INT(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  UNIQUE INDEX `name` (`name`)
)
COLLATE='utf8_general_ci'
ENGINE=InnoDB
AUTO_INCREMENT=1;