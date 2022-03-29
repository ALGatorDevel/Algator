CREATE TABLE `u_users` (
        `id` INT(11) NOT NULL AUTO_INCREMENT,
        `name` VARCHAR(255) NOT NULL COLLATE 'utf8_slovenian_ci',
        `password` VARCHAR(255) NOT NULL COLLATE 'utf8_slovenian_ci',
        `status` INT(1) NOT NULL DEFAULT '0' COMMENT '0 - active, 1 - inactive',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `name` (`name`)
)
COLLATE='utf8_slovenian_ci'
ENGINE=InnoDB
AUTO_INCREMENT=1;