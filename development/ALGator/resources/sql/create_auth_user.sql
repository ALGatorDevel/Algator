CREATE TABLE `auth_user` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `password` varchar(128) NOT NULL,
  `last_login` datetime(6) DEFAULT NULL,
  `is_superuser` tinyint(1) NOT NULL DEFAULT 0,
  `username` varchar(150) NOT NULL,
  `first_name` varchar(150) NOT NULL DEFAULT '',
  `last_name` varchar(150) NOT NULL DEFAULT '',
  `email` varchar(254) NOT NULL DEFAULT '',
  `is_staff` tinyint(1) NOT NULL DEFAULT 0,
  `is_active` tinyint(1) NOT NULL DEFAULT 1,
  `date_joined` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`)
) 
COLLATE='utf8_general_ci'
ENGINE=InnoDB 
AUTO_INCREMENT=4 
DEFAULT CHARSET=utf8;
