-- Runs automatically when the MySQL container is first created.
CREATE TABLE IF NOT EXISTS avb_closeout (
    avb_sku        VARCHAR(255) PRIMARY KEY,
    closeout_id    VARCHAR(255) NOT NULL,
    linq_id        INT(10)      NOT NULL,
    avb_status     VARCHAR(255) NOT NULL,
    closeout_type  VARCHAR(255) NOT NULL,
    avb_brand      VARCHAR(255) NOT NULL
);
