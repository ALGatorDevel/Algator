CREATE TABLE IF NOT EXISTS `u_entities` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(255) NOT NULL,
  `type` TINYINT(4) NOT NULL DEFAULT '1',
  `id_parent` INT(11) NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  INDEX `FK_entities_entities` (`id_parent`),
  CONSTRAINT `FK_entities_entities` FOREIGN KEY (`id_parent`) REFERENCES `u_entities` (`id`) ON UPDATE CASCADE ON DELETE SET NULL
)
COLLATE='utf8_general_ci'
ENGINE=InnoDB
AUTO_INCREMENT=1;