ALTER TABLE `user`
    DROP INDEX `uk_user_username`,
    -- Keep the legacy account width during the one-time rename so existing
    -- usernames longer than the new mobile format are not truncated.
    CHANGE COLUMN `username` `mobile` VARCHAR(50) NOT NULL,
    CHANGE COLUMN `nickname` `username` VARCHAR(50) NULL;

UPDATE `user`
SET `username` = `mobile`
WHERE `username` IS NULL OR TRIM(`username`) = '';

ALTER TABLE `user`
    MODIFY COLUMN `username` VARCHAR(50) NOT NULL,
    ADD UNIQUE KEY `uk_user_mobile` (`mobile`);
