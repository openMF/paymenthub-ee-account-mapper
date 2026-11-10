-- PHEE-474: the payment modality is a two-character code ("00" ACCOUNT_ID ... "04" VOUCHER, see
-- PaymentModalityEnum), but V1 created the columns as BIGINT, so "00" was stored as 0 and read back as "0".
-- Change both columns to text and restore the leading zero on the rows already saved. Nulls stay null.

-- Only the five validated codes are saved here.
ALTER TABLE payment_modality_details MODIFY COLUMN modality VARCHAR(2) NULL DEFAULT NULL;
UPDATE payment_modality_details SET modality = LPAD(modality, 2, '0') WHERE CHAR_LENGTH(modality) = 1;

-- Holds whatever modality a rejected request carried, valid or not, so it is as wide as the other columns.
ALTER TABLE error_tracking MODIFY COLUMN modality VARCHAR(200) NULL DEFAULT NULL;
UPDATE error_tracking SET modality = LPAD(modality, 2, '0') WHERE CHAR_LENGTH(modality) = 1;
