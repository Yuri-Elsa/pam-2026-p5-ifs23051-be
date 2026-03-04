-- Buat tabel users
CREATE TABLE IF NOT EXISTS users (
                                     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    photo VARCHAR(255) NULL,
    about TEXT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
    );

-- Buat tabel refresh_tokens
CREATE TABLE IF NOT EXISTS refresh_tokens (
                                              id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    refresh_token TEXT NOT NULL,
    auth_token TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL
    );

-- Buat tabel todos
CREATE TABLE IF NOT EXISTS todos (
                                     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    title VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    is_done BOOLEAN NOT NULL DEFAULT false,
    urgency VARCHAR(10) NOT NULL DEFAULT 'medium',
    cover TEXT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
    );

-- Migrasi untuk database yang sudah ada (jalankan jika tabel sudah ada sebelumnya)
ALTER TABLE users ADD COLUMN IF NOT EXISTS about TEXT NULL;
ALTER TABLE todos ADD COLUMN IF NOT EXISTS urgency VARCHAR(10) NOT NULL DEFAULT 'medium';
ALTER TABLE users ADD COLUMN IF NOT EXISTS photo VARCHAR(255) NULL;
ALTER TABLE users ALTER COLUMN username SET NOT NULL;