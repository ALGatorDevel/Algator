CREATE TABLE `u_permissions_users` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `id_user` INT(11) NULL DEFAULT NULL,
  `id_entity` INT(11) NULL DEFAULT NULL,
  `id_permission` INT(11) NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  INDEX `FK_permissions_users_entities` (`id_entity`),
  INDEX `FK_permissions_users_groups` (`id_user`),
  INDEX `FK_permissions_users_permissions` (`id_permission`),
  CONSTRAINT `FK_permissions_users_entities` FOREIGN KEY (`id_entity`) REFERENCES `u_entities` (`id`) ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT `FK_permissions_users_groups` FOREIGN KEY (`id_user`) REFERENCES `u_groups` (`id`) ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT `FK_permissions_users_permissions` FOREIGN KEY (`id_permission`) REFERENCES `u_permissions` (`id`) ON UPDATE NO ACTION ON DELETE NO ACTION
)
COLLATE='utf8_general_ci'
ENGINE=InnoDB
AUTO_INCREMENT=1;