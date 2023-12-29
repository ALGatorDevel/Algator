CREATE TABLE `u_owners` (
  `id` INT(1) NOT NULL AUTO_INCREMENT,
  `id_owner` INT(1) NOT NULL,
  `id_entity` INT(1) NOT NULL,
  PRIMARY KEY (`id`),
  INDEX `FK_owners_users` (`id_owner`),
  INDEX `FK_owners_entities` (`id_entity`),
  CONSTRAINT `FK_owners_entities` FOREIGN KEY (`id_entity`) REFERENCES `u_entities` (`id`),
  CONSTRAINT `FK_owners_users` FOREIGN KEY (`id_owner`) REFERENCES `auth_user` (`id`)
)
COLLATE='utf8_general_ci'
ENGINE=InnoDB
AUTO_INCREMENT=1;