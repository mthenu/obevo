//// CHANGE name=base
CREATE TABLE PRODUCT (
	PRODUCT_ID    INT,
	NAME            varchar(30) NULL,
    PRIMARY KEY (PRODUCT_ID)
)
GO

//// CHANGE name=chng3
ALTER TABLE PRODUCT ADD COLUMN DESC varchar(50) NULL
GO
