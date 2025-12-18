##
## turbine-security-data.sql
##
## Torque will not autogenerate these files anymore - but sql-maven-plugin!
## If not, please run
## this SQL code manually to get your application up and running
##
SET FOREIGN_KEY_CHECKS=0;


INSERT INTO TURBINE_USER (USER_ID,LOGIN_NAME,PASSWORD_VALUE,FIRST_NAME,LAST_NAME)
    VALUES (1,'anon','nopw','','Anon');

INSERT INTO TURBINE_USER (USER_ID,LOGIN_NAME,PASSWORD_VALUE,FIRST_NAME,LAST_NAME)
    VALUES (2,'user','nopw','Websim','Websim');
    
INSERT INTO TURBINE_ROLE (`ROLE_ID`, `ROLE_NAME`) VALUES
(1, 'administrator'),
(2, 'editor');

INSERT INTO TURBINE_GROUP (`GROUP_ID`, `GROUP_NAME`) VALUES
(1, 'global');


INSERT INTO TURBINE_ROLE_PERMISSION (`ROLE_ID`, `PERMISSION_ID`) VALUES
(1, 1),
(2, 2);

INSERT INTO TURBINE_USER_GROUP_ROLE (`USER_ID`, `GROUP_ID`, `ROLE_ID`) VALUES
(2, 1, 1),
(2, 1, 2);

-- 
ALTER TABLE TURBINE_USER MODIFY COLUMN USER_ID INT auto_increment;
ALTER TABLE TURBINE_PERMISSION MODIFY COLUMN PERMISSION_ID INT auto_increment;
ALTER TABLE TURBINE_ROLE MODIFY COLUMN ROLE_ID INT auto_increment;
ALTER TABLE TURBINE_GROUP MODIFY COLUMN GROUP_ID INT auto_increment;

