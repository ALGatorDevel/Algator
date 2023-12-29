CREATE TABLE `u_group_users` (
	`id` INT(11) NOT NULL AUTO_INCREMENT, 
	`id_user` INT(11) NOT NULL,
	`id_group` INT(11) NOT NULL,
	PRIMARY KEY (`id`),
	INDEX `FK__users` (`id_user`),
	INDEX `FK__groups` (`id_group`),
	CONSTRAINT `FK__groups` FOREIGN KEY (`id_group`) REFERENCES `u_groups` (`id`),
	CONSTRAINT `FK__users` FOREIGN KEY (`id_user`) REFERENCES `auth_user` (`id`)
)
COLLATE='utf8_general_ci'
ENGINE=InnoDB
AUTO_INCREMENT=1;