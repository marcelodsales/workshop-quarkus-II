-- Banking Database Schema
-- Grant Permissions to SCHEMA if needed: GRANT ALL ON SCHEMA public TO banking;
-- Grant Permissions to DATABASE if needed: GRANT ALL PRIVILEGES ON DATABASE banking_db TO banking;

-- Create sequence for transactions
CREATE SEQUENCE IF NOT EXISTS transaction_id_seq
    START WITH 1
    INCREMENT BY 1;

-- Account table
CREATE TABLE IF NOT EXISTS Account (
    accountNumber VARCHAR(255) PRIMARY KEY,
    balance NUMERIC(19,2) NOT NULL DEFAULT 0,
    ownerId VARCHAR(255) NOT NULL
);

-- Transaction table
CREATE TABLE IF NOT EXISTS Transaction (
    transactionId BIGINT PRIMARY KEY DEFAULT nextval('transaction_id_seq'),
    accountNumber VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL CHECK (type IN ('DEPOSIT', 'WITHDRAW', 'TRANSFER_IN', 'TRANSFER_OUT')),
    amount NUMERIC(19,2) NOT NULL,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    description VARCHAR(255)
);

-- Indexes for better performance
CREATE INDEX IF NOT EXISTS idx_transaction_account ON Transaction(accountNumber);
CREATE INDEX IF NOT EXISTS idx_transaction_timestamp ON Transaction(timestamp);

