
drop table if exists `method_lock`;
CREATE TABLE `method_lock` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `method_name` varchar(64) NOT NULL DEFAULT '' COMMENT '锁定的方法名',
  `desc` varchar(1024) NOT NULL DEFAULT '备注信息',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '保存数据时间，自动生成',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uidx_method_name` (`method_name`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='锁定中的方法';

--用于实现数据库排他锁
drop table if exists `user_info`;
CREATE TABLE `user_info` (
  `user_id` bigint(19) NOT NULL  AUTO_INCREMENT COMMENT '主键',
  `user_name` varchar(45) DEFAULT NULL,
  `account` varchar(45) NOT NULL,
  `password` varchar(45) DEFAULT NULL,
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO `user_info`(`user_id`, `user_name`, `account`, `password`) VALUES (150, 'name1', 'Account1', 'pass1');
INSERT INTO `user_info`(`user_id`, `user_name`, `account`, `password`) VALUES (152, 'name3', 'Account3', 'pass3');
INSERT INTO `user_info`(`user_id`, `user_name`, `account`, `password`) VALUES (154, 'name5', 'Account5', 'pass5');
INSERT INTO `user_info`(`user_id`, `user_name`, `account`, `password`) VALUES (156, 'name7', 'Account7', 'pass7');
INSERT INTO `user_info`(`user_id`, `user_name`, `account`, `password`) VALUES (158, 'name9', 'Account9', 'pass9');

--用于实现数据库乐观锁
drop table if exists `user_lock`;
CREATE TABLE `user_lock` (
  `user_id` bigint(19) NOT NULL  AUTO_INCREMENT COMMENT '主键',
  `user_name` varchar(45) DEFAULT NULL,
  `version` int(11) NOT NULL  COMMENT '版本号',
  `timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '时间搓',
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO `lock`.`user_lock`(`user_id`, `user_name`, `version`, `timestamp`) VALUES (150, 'zhangsan', 1, '2020-09-25 07:05:11');