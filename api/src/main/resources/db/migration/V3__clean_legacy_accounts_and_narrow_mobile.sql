-- The pre-Slice-02 usernames are no longer valid login accounts. This is a
-- one-time cleanup before enforcing the current mobile account width.
DELETE FROM `user`;

ALTER TABLE `user`
    MODIFY COLUMN `mobile` VARCHAR(20) NOT NULL;
