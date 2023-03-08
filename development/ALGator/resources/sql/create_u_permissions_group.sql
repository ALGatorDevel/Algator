CREATE TABLE `u_permissions_group` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `id_group` INT(11) NULL DEFAULT NULL,
  `id_entity` INT(11) NULL DEFAULT NULL,
  `id_permission` INT(11) NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  INDEX `FK_permissions_group_entities` (`id_entity`),
  INDEX `FK_permissions_group_groups` (`id_group`),
  INDEX `FK_permissions_group_permissions` (`id_permission`),
  CONSTRAINT `FK_permissions_group_entities` FOREIGN KEY (`id_entity`) REFERENCES `u_entities` (`id`) ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT `FK_permissions_group_groups` FOREIGN KEY (`id_group`) REFERENCES `u_groups` (`id`) ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT `FK_permissions_group_permissions` FOREIGN KEY (`id_permission`) REFERENCES `u_permissions` (`id`) ON UPDATE NO ACTION ON DELETE NO ACTION
)
COLLATE='utf8_general_ci'
ENGINE=InnoDB
AUTO_INCREMENT=1; 