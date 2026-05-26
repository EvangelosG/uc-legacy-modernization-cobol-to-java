-- V013: Add version column to cards table for optimistic locking (Phase 3)
ALTER TABLE cards ADD COLUMN version BIGINT DEFAULT 0;
