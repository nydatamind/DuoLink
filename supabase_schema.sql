-- ========================================================
-- DuoLink – Supabase Database Schema (Profiles & Pairing)
-- Run this in your Supabase SQL Editor:
-- https://supabase.com/dashboard/project/kypyymlubmylmomoqyoq/sql/new
-- ========================================================

-- 1. Create duolink_profiles table (Stores unique usernames & public keys)
CREATE TABLE IF NOT EXISTS public.duolink_profiles (
    user_id TEXT PRIMARY KEY,
    username TEXT UNIQUE NOT NULL,
    device_id TEXT NOT NULL,
    public_key TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- Index for instant username lookup and case-insensitive search
CREATE INDEX IF NOT EXISTS idx_duolink_profiles_username ON public.duolink_profiles (lower(username));

-- 2. Create duolink_pairing_requests table (Mutual pairing system)
CREATE TABLE IF NOT EXISTS public.duolink_pairing_requests (
    id TEXT PRIMARY KEY,
    sender_user_id TEXT NOT NULL,
    sender_username TEXT NOT NULL,
    sender_device_id TEXT NOT NULL,
    sender_public_key TEXT NOT NULL,
    receiver_username TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- Index for incoming requests to a specific username
CREATE INDEX IF NOT EXISTS idx_duolink_pairing_receiver ON public.duolink_pairing_requests (lower(receiver_username), status);
CREATE INDEX IF NOT EXISTS idx_duolink_pairing_sender ON public.duolink_pairing_requests (sender_device_id, status);

-- 3. Enable Row Level Security (RLS)
ALTER TABLE public.duolink_profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.duolink_pairing_requests ENABLE ROW LEVEL SECURITY;

-- 4. Set permissive access policies for the two authorized DuoLink users
-- Allows read of public usernames and public keys (Zero email/personal info is ever stored here)
DROP POLICY IF EXISTS "Allow profile reads" ON public.duolink_profiles;
CREATE POLICY "Allow profile reads" ON public.duolink_profiles FOR SELECT USING (true);

DROP POLICY IF EXISTS "Allow profile upserts" ON public.duolink_profiles;
CREATE POLICY "Allow profile upserts" ON public.duolink_profiles FOR ALL USING (true) WITH CHECK (true);

DROP POLICY IF EXISTS "Allow pairing requests" ON public.duolink_pairing_requests;
CREATE POLICY "Allow pairing requests" ON public.duolink_pairing_requests FOR ALL USING (true) WITH CHECK (true);

-- 5. Add tables to Supabase Realtime broadcast publication
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_publication_tables 
        WHERE pubname = 'supabase_realtime' AND tablename = 'duolink_profiles'
    ) THEN
        ALTER PUBLICATION supabase_realtime ADD TABLE public.duolink_profiles;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_publication_tables 
        WHERE pubname = 'supabase_realtime' AND tablename = 'duolink_pairing_requests'
    ) THEN
        ALTER PUBLICATION supabase_realtime ADD TABLE public.duolink_pairing_requests;
    END IF;
END $$;

