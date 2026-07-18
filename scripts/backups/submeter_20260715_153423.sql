--
-- PostgreSQL database dump
--

\restrict qVdpIZr2sVYdzMO00nrpqu4v2zFfQUpmqpSU0zqoKrUt2IS7RvL5MAIOOMwjw0k

-- Dumped from database version 16.14
-- Dumped by pg_dump version 16.14

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: pgcrypto; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public;


--
-- Name: EXTENSION pgcrypto; Type: COMMENT; Schema: -; Owner: -
--

COMMENT ON EXTENSION pgcrypto IS 'cryptographic functions';


--
-- Name: fn_set_updated_at(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_set_updated_at() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$;


--
-- Name: next_invoice_number(uuid, integer); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.next_invoice_number(p_org_id uuid, p_year integer) RETURNS text
    LANGUAGE plpgsql
    AS $$
DECLARE
    v_seq INT;
BEGIN
    INSERT INTO invoice_sequences (org_id, year, last_seq)
    VALUES (p_org_id, p_year, 1)
    ON CONFLICT (org_id, year) DO UPDATE
        SET last_seq = invoice_sequences.last_seq + 1
    RETURNING last_seq INTO v_seq;

    RETURN 'INV-' || p_year || '-' || LPAD(v_seq::TEXT, 4, '0');
END;
$$;


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: api_keys; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.api_keys (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    org_id uuid NOT NULL,
    key_id text NOT NULL,
    prefix text NOT NULL,
    key_hash text NOT NULL,
    last_4 text NOT NULL,
    name text NOT NULL,
    scopes text[] DEFAULT '{}'::text[] NOT NULL,
    environment text DEFAULT 'PRODUCTION'::text NOT NULL,
    created_by uuid,
    last_used_at timestamp with time zone,
    revoked_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    deleted_at timestamp with time zone,
    CONSTRAINT chk_api_keys_env CHECK ((environment = ANY (ARRAY['PRODUCTION'::text, 'SANDBOX'::text])))
);


--
-- Name: audit_log; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.audit_log (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    org_id uuid NOT NULL,
    actor_id uuid,
    actor_type text NOT NULL,
    entity_type text NOT NULL,
    entity_id uuid NOT NULL,
    action text NOT NULL,
    ip_address text,
    user_agent text,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    request_id text,
    correlation_id text,
    resource_type text,
    resource_name text,
    success boolean,
    duration_ms bigint,
    old_value jsonb,
    new_value jsonb,
    CONSTRAINT chk_audit_log_action CHECK ((action = ANY (ARRAY['CREATE'::text, 'UPDATE'::text, 'DELETE'::text, 'STATUS_CHANGE'::text]))),
    CONSTRAINT chk_audit_log_actor_type CHECK ((actor_type = ANY (ARRAY['USER'::text, 'SYSTEM'::text])))
);


--
-- Name: customers; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.customers (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    org_id uuid NOT NULL,
    name text NOT NULL,
    email text NOT NULL,
    phone text,
    metadata jsonb DEFAULT '{}'::jsonb NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    deleted_at timestamp with time zone,
    external_id character varying(255),
    gender character varying(20),
    is_senior boolean DEFAULT false NOT NULL,
    has_partner boolean DEFAULT false NOT NULL
);


--
-- Name: flyway_schema_history; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.flyway_schema_history (
    installed_rank integer NOT NULL,
    version character varying(50),
    description character varying(200) NOT NULL,
    type character varying(20) NOT NULL,
    script character varying(1000) NOT NULL,
    checksum integer,
    installed_by character varying(100) NOT NULL,
    installed_on timestamp without time zone DEFAULT now() NOT NULL,
    execution_time integer NOT NULL,
    success boolean NOT NULL
);


--
-- Name: invitations; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.invitations (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    org_id uuid NOT NULL,
    email text NOT NULL,
    role text NOT NULL,
    token_hash text NOT NULL,
    status text DEFAULT 'PENDING'::text NOT NULL,
    invited_by uuid NOT NULL,
    expires_at timestamp with time zone NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    deleted_at timestamp with time zone,
    CONSTRAINT chk_invitations_role CHECK ((role = ANY (ARRAY['OWNER'::text, 'ADMIN'::text, 'MEMBER'::text]))),
    CONSTRAINT chk_invitations_status CHECK ((status = ANY (ARRAY['PENDING'::text, 'ACCEPTED'::text, 'EXPIRED'::text, 'REVOKED'::text])))
);


--
-- Name: invoice_line_items; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.invoice_line_items (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    invoice_id uuid NOT NULL,
    description text NOT NULL,
    quantity bigint DEFAULT 1 NOT NULL,
    unit_amount bigint NOT NULL,
    amount bigint NOT NULL,
    pricing_model text NOT NULL,
    tier_detail jsonb,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT chk_line_items_amount CHECK ((amount >= 0)),
    CONSTRAINT chk_line_items_pricing_model CHECK ((pricing_model = ANY (ARRAY['FLAT'::text, 'TIERED'::text, 'METERED'::text]))),
    CONSTRAINT chk_line_items_quantity CHECK ((quantity > 0))
);


--
-- Name: invoice_sequences; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.invoice_sequences (
    org_id uuid NOT NULL,
    year integer NOT NULL,
    last_seq integer DEFAULT 0 NOT NULL
);


--
-- Name: invoices; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.invoices (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    org_id uuid NOT NULL,
    subscription_id uuid NOT NULL,
    invoice_number text NOT NULL,
    status text DEFAULT 'DRAFT'::text NOT NULL,
    period_start timestamp with time zone NOT NULL,
    period_end timestamp with time zone NOT NULL,
    subtotal_cents bigint DEFAULT 0 NOT NULL,
    tax_cents bigint DEFAULT 0 NOT NULL,
    total_cents bigint DEFAULT 0 NOT NULL,
    due_at timestamp with time zone NOT NULL,
    paid_at timestamp with time zone,
    razorpay_order_id text,
    pdf_path text,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT chk_invoices_amounts CHECK (((subtotal_cents >= 0) AND (tax_cents >= 0) AND (total_cents >= 0))),
    CONSTRAINT chk_invoices_status CHECK ((status = ANY (ARRAY['DRAFT'::text, 'OPEN'::text, 'PAID'::text, 'VOID'::text, 'UNCOLLECTIBLE'::text])))
);


--
-- Name: organization_members; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.organization_members (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    org_id uuid NOT NULL,
    user_id uuid NOT NULL,
    role text NOT NULL,
    status text DEFAULT 'ACTIVE'::text NOT NULL,
    joined_at timestamp with time zone DEFAULT now() NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    deleted_at timestamp with time zone,
    CONSTRAINT chk_org_members_role CHECK ((role = ANY (ARRAY['OWNER'::text, 'ADMIN'::text, 'MEMBER'::text])))
);


--
-- Name: organizations; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.organizations (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    name text NOT NULL,
    slug text NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    deleted_at timestamp with time zone,
    logo_url text,
    timezone text DEFAULT 'UTC'::text NOT NULL,
    currency text DEFAULT 'INR'::text NOT NULL,
    support_email text,
    default_tax_rate bigint,
    invoice_prefix text,
    invoice_footer text,
    company_website text,
    company_address text,
    version bigint DEFAULT 0 NOT NULL
);


--
-- Name: payments; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.payments (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    org_id uuid NOT NULL,
    invoice_id uuid NOT NULL,
    razorpay_order_id text NOT NULL,
    razorpay_payment_id text,
    amount_cents bigint NOT NULL,
    currency text DEFAULT 'INR'::text NOT NULL,
    status text DEFAULT 'PENDING'::text NOT NULL,
    failure_reason text,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    payment_method character varying(20),
    CONSTRAINT chk_payments_amount CHECK ((amount_cents > 0)),
    CONSTRAINT chk_payments_status CHECK ((status = ANY (ARRAY['PENDING'::text, 'SUCCESS'::text, 'FAILED'::text, 'REFUNDED'::text])))
);


--
-- Name: plan_tiers; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.plan_tiers (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    plan_id uuid NOT NULL,
    tier_order integer NOT NULL,
    up_to bigint,
    unit_amount bigint NOT NULL,
    flat_fee bigint DEFAULT 0 NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT chk_plan_tiers_fee CHECK ((flat_fee >= 0)),
    CONSTRAINT chk_plan_tiers_unit CHECK ((unit_amount >= 0))
);


--
-- Name: plans; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.plans (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    org_id uuid NOT NULL,
    name text NOT NULL,
    description text,
    pricing_model text NOT NULL,
    flat_amount bigint,
    billing_interval text NOT NULL,
    trial_days integer DEFAULT 0 NOT NULL,
    is_archived boolean DEFAULT false NOT NULL,
    version integer DEFAULT 1 NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT chk_plans_billing_interval CHECK ((billing_interval = ANY (ARRAY['MONTHLY'::text, 'ANNUAL'::text]))),
    CONSTRAINT chk_plans_flat_amount CHECK ((((pricing_model = 'FLAT'::text) AND (flat_amount IS NOT NULL) AND (flat_amount >= 0)) OR (pricing_model = ANY (ARRAY['TIERED'::text, 'METERED'::text])))),
    CONSTRAINT chk_plans_pricing_model CHECK ((pricing_model = ANY (ARRAY['FLAT'::text, 'TIERED'::text, 'METERED'::text]))),
    CONSTRAINT chk_plans_trial_days CHECK ((trial_days >= 0))
);


--
-- Name: refresh_tokens; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.refresh_tokens (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    token text NOT NULL,
    expires_at timestamp with time zone NOT NULL,
    revoked_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: subscriptions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.subscriptions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    org_id uuid NOT NULL,
    customer_id uuid NOT NULL,
    plan_id uuid NOT NULL,
    plan_version integer NOT NULL,
    status text NOT NULL,
    trial_end_at timestamp with time zone,
    current_period_start timestamp with time zone,
    current_period_end timestamp with time zone,
    canceled_at timestamp with time zone,
    cancellation_reason text,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    months_active integer,
    CONSTRAINT chk_subscriptions_status CHECK ((status = ANY (ARRAY['TRIAL'::text, 'ACTIVE'::text, 'PAST_DUE'::text, 'CANCELED'::text])))
);


--
-- Name: usage_events; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.usage_events (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    org_id uuid NOT NULL,
    subscription_id uuid NOT NULL,
    event_type text NOT NULL,
    quantity bigint NOT NULL,
    idempotency_key text NOT NULL,
    occurred_at timestamp with time zone NOT NULL,
    invoice_id uuid,
    rejected_reason text,
    metadata jsonb DEFAULT '{}'::jsonb NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT chk_usage_events_quantity CHECK ((quantity > 0))
);


--
-- Name: users; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.users (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    org_id uuid NOT NULL,
    email text NOT NULL,
    password_hash text NOT NULL,
    role text NOT NULL,
    email_verified boolean DEFAULT false NOT NULL,
    last_login_at timestamp with time zone,
    failed_login_count integer DEFAULT 0 NOT NULL,
    locked_until timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    deleted_at timestamp with time zone,
    CONSTRAINT chk_users_role CHECK ((role = ANY (ARRAY['OWNER'::text, 'ADMIN'::text, 'MEMBER'::text])))
);


--
-- Data for Name: api_keys; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.api_keys (id, org_id, key_id, prefix, key_hash, last_4, name, scopes, environment, created_by, last_used_at, revoked_at, created_at, updated_at, deleted_at) FROM stdin;
\.


--
-- Data for Name: audit_log; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.audit_log (id, org_id, actor_id, actor_type, entity_type, entity_id, action, ip_address, user_agent, created_at, updated_at, request_id, correlation_id, resource_type, resource_name, success, duration_ms, old_value, new_value) FROM stdin;
05c06764-6d93-4897-b89b-2bede8d68f49	0f0f214e-bcb7-4d84-96cc-537986200898	f2435a80-01ac-45c5-b510-ea3f7c05396f	USER	plan	10ac3db2-eac0-45f1-94ce-e3063051aa28	CREATE	\N	\N	2026-07-14 13:25:06.242469+00	2026-07-15 09:50:17.391877+00	\N	\N	\N	\N	\N	\N	\N	{"name": "Pro (Flat)"}
035c4f38-c9d8-4290-8efe-90238611b3d1	0f0f214e-bcb7-4d84-96cc-537986200898	f2435a80-01ac-45c5-b510-ea3f7c05396f	USER	plan	8484f42b-a419-460c-a92b-90ac6ccc77e6	CREATE	\N	\N	2026-07-14 13:25:06.245181+00	2026-07-15 09:50:17.391877+00	\N	\N	\N	\N	\N	\N	\N	{"name": "Scale (Metered)"}
d0f97e62-ce6f-4e73-bca9-c33fbeff29ae	0f0f214e-bcb7-4d84-96cc-537986200898	f2435a80-01ac-45c5-b510-ea3f7c05396f	USER	customer	fd3b0f83-d380-4ea6-b6a1-3f0368e4af09	CREATE	\N	\N	2026-07-14 13:25:06.250848+00	2026-07-15 09:50:17.391877+00	\N	\N	\N	\N	\N	\N	\N	{"name": "Customer 1"}
f824d0bc-ac93-47f2-b6c3-4346252a8d3d	0f0f214e-bcb7-4d84-96cc-537986200898	f2435a80-01ac-45c5-b510-ea3f7c05396f	USER	subscription	9d8c6f7d-d40f-4fa0-b5b6-ab0f0c5f069b	CREATE	\N	\N	2026-07-14 13:25:06.253979+00	2026-07-15 09:50:17.391877+00	\N	\N	\N	\N	\N	\N	\N	{"plan_id": "10ac3db2-eac0-45f1-94ce-e3063051aa28"}
baeb03d9-9325-4375-8470-d86baa05e099	0f0f214e-bcb7-4d84-96cc-537986200898	f2435a80-01ac-45c5-b510-ea3f7c05396f	USER	invoice	b2201c48-a5ff-4cf3-a5d8-ff0337f64161	CREATE	\N	\N	2026-07-14 13:25:06.258824+00	2026-07-15 09:50:17.391877+00	\N	\N	\N	\N	\N	\N	\N	{"invoice_number": "INV-00001"}
548a1b9e-5c3f-4e0b-84f7-d7fdc3e7e725	0f0f214e-bcb7-4d84-96cc-537986200898	f2435a80-01ac-45c5-b510-ea3f7c05396f	USER	customer	e286fe63-64c5-4812-934b-c1f65a49fac6	CREATE	\N	\N	2026-07-14 13:25:06.260866+00	2026-07-15 09:50:17.391877+00	\N	\N	\N	\N	\N	\N	\N	{"name": "Customer 2"}
7eb8efab-1616-484c-865c-2453856f6dab	0f0f214e-bcb7-4d84-96cc-537986200898	f2435a80-01ac-45c5-b510-ea3f7c05396f	USER	subscription	91796f5f-5773-4688-a37d-f43b9001f2c6	CREATE	\N	\N	2026-07-14 13:25:06.262985+00	2026-07-15 09:50:17.391877+00	\N	\N	\N	\N	\N	\N	\N	{"plan_id": "10ac3db2-eac0-45f1-94ce-e3063051aa28"}
1f42d070-b339-4940-a1ba-08fa7f8f6bbe	0f0f214e-bcb7-4d84-96cc-537986200898	f2435a80-01ac-45c5-b510-ea3f7c05396f	USER	invoice	5dd14a90-819c-453b-9c19-9184382c5872	CREATE	\N	\N	2026-07-14 13:25:06.264557+00	2026-07-15 09:50:17.391877+00	\N	\N	\N	\N	\N	\N	\N	{"invoice_number": "INV-00002"}
2f615720-4f99-476b-853d-97bcc0888a6d	0f0f214e-bcb7-4d84-96cc-537986200898	f2435a80-01ac-45c5-b510-ea3f7c05396f	USER	customer	2f8eec44-9b9f-4a1e-b61d-1f610b06cae8	CREATE	\N	\N	2026-07-14 13:25:06.266116+00	2026-07-15 09:50:17.391877+00	\N	\N	\N	\N	\N	\N	\N	{"name": "Customer 3"}
ad206d5a-1a3f-44d6-944c-ee23682a15ff	0f0f214e-bcb7-4d84-96cc-537986200898	f2435a80-01ac-45c5-b510-ea3f7c05396f	USER	subscription	ce7c9b7f-9ca2-4fce-886f-1954e5ed726a	CREATE	\N	\N	2026-07-14 13:25:06.268215+00	2026-07-15 09:50:17.391877+00	\N	\N	\N	\N	\N	\N	\N	{"plan_id": "8484f42b-a419-460c-a92b-90ac6ccc77e6"}
1aad3b52-c5a6-4380-9699-b8108bf345b3	0f0f214e-bcb7-4d84-96cc-537986200898	f2435a80-01ac-45c5-b510-ea3f7c05396f	USER	invoice	447c8d15-db6c-4305-8b08-1ab2889382bf	CREATE	\N	\N	2026-07-14 13:25:06.270302+00	2026-07-15 09:50:17.391877+00	\N	\N	\N	\N	\N	\N	\N	{"invoice_number": "INV-00003"}
d3eb7e2f-7ded-47db-9712-752023f99ba3	0f0f214e-bcb7-4d84-96cc-537986200898	f2435a80-01ac-45c5-b510-ea3f7c05396f	USER	customer	27e47b59-bfb2-41f9-8abb-57643b22ca39	CREATE	\N	\N	2026-07-14 13:25:06.295647+00	2026-07-15 09:50:17.391877+00	\N	\N	\N	\N	\N	\N	\N	{"name": "Customer 4"}
3437a1f2-0fd9-4148-ab52-9a6311205950	0f0f214e-bcb7-4d84-96cc-537986200898	f2435a80-01ac-45c5-b510-ea3f7c05396f	USER	subscription	4be19af5-c9e1-41da-925c-4812cb3c369a	CREATE	\N	\N	2026-07-14 13:25:06.297741+00	2026-07-15 09:50:17.391877+00	\N	\N	\N	\N	\N	\N	\N	{"plan_id": "10ac3db2-eac0-45f1-94ce-e3063051aa28"}
3e0d57cc-b3be-4099-b740-fde7b4bbbe10	0f0f214e-bcb7-4d84-96cc-537986200898	f2435a80-01ac-45c5-b510-ea3f7c05396f	USER	invoice	421c5445-57bd-492d-825b-18e666819ca8	CREATE	\N	\N	2026-07-14 13:25:06.299295+00	2026-07-15 09:50:17.391877+00	\N	\N	\N	\N	\N	\N	\N	{"invoice_number": "INV-00004"}
8e1a8dd3-538e-4146-9bb8-07c8dd4a0ee3	0f0f214e-bcb7-4d84-96cc-537986200898	f2435a80-01ac-45c5-b510-ea3f7c05396f	USER	customer	5829e36f-c22c-4ca8-9750-984be4234db6	CREATE	\N	\N	2026-07-14 13:25:06.300855+00	2026-07-15 09:50:17.391877+00	\N	\N	\N	\N	\N	\N	\N	{"name": "Customer 5"}
45c7306f-d5ad-4e77-b2f8-7a9776953e7a	0f0f214e-bcb7-4d84-96cc-537986200898	f2435a80-01ac-45c5-b510-ea3f7c05396f	USER	subscription	c6cafeb3-5538-42e1-97a4-19ceb6581308	CREATE	\N	\N	2026-07-14 13:25:06.302407+00	2026-07-15 09:50:17.391877+00	\N	\N	\N	\N	\N	\N	\N	{"plan_id": "10ac3db2-eac0-45f1-94ce-e3063051aa28"}
6f98fd32-274f-481a-91c9-f7e2664de05b	0f0f214e-bcb7-4d84-96cc-537986200898	f2435a80-01ac-45c5-b510-ea3f7c05396f	USER	invoice	0424c2da-e9bc-48b6-b4e5-b7eebacd465a	CREATE	\N	\N	2026-07-14 13:25:06.30402+00	2026-07-15 09:50:17.391877+00	\N	\N	\N	\N	\N	\N	\N	{"invoice_number": "INV-00005"}
152f5cde-cae2-4a3c-9723-964ecce891f6	0f0f214e-bcb7-4d84-96cc-537986200898	f2435a80-01ac-45c5-b510-ea3f7c05396f	USER	customer	b8e83644-aaa2-4c58-bd3e-27a80d679be7	CREATE	\N	\N	2026-07-14 13:25:06.305579+00	2026-07-15 09:50:17.391877+00	\N	\N	\N	\N	\N	\N	\N	{"name": "Customer 6"}
4bcf61e3-e6c7-4af7-aab4-736c301010d1	0f0f214e-bcb7-4d84-96cc-537986200898	f2435a80-01ac-45c5-b510-ea3f7c05396f	USER	subscription	49e97a8c-47a1-4a63-a9cb-c4fa56c55870	CREATE	\N	\N	2026-07-14 13:25:06.307153+00	2026-07-15 09:50:17.391877+00	\N	\N	\N	\N	\N	\N	\N	{"plan_id": "8484f42b-a419-460c-a92b-90ac6ccc77e6"}
8650b0e7-88dd-495e-94f8-f80d3a5b6825	0f0f214e-bcb7-4d84-96cc-537986200898	f2435a80-01ac-45c5-b510-ea3f7c05396f	USER	invoice	b7f89ccb-2cc0-47c2-9524-29c93b8a26de	CREATE	\N	\N	2026-07-14 13:25:06.308159+00	2026-07-15 09:50:17.391877+00	\N	\N	\N	\N	\N	\N	\N	{"invoice_number": "INV-00006"}
eaa8d13e-8878-4919-9c7b-6d07dd228b83	0f0f214e-bcb7-4d84-96cc-537986200898	f2435a80-01ac-45c5-b510-ea3f7c05396f	USER	customer	e9dae2e7-53a9-4952-ac05-20bdf3ab23f4	CREATE	\N	\N	2026-07-14 13:25:06.332664+00	2026-07-15 09:50:17.391877+00	\N	\N	\N	\N	\N	\N	\N	{"name": "Customer 7"}
efb21633-b2f2-4004-be4a-6c5ccac42e07	0f0f214e-bcb7-4d84-96cc-537986200898	f2435a80-01ac-45c5-b510-ea3f7c05396f	USER	subscription	23ab7d92-110f-4cf1-90a6-932a692d3694	CREATE	\N	\N	2026-07-14 13:25:06.333754+00	2026-07-15 09:50:17.391877+00	\N	\N	\N	\N	\N	\N	\N	{"plan_id": "10ac3db2-eac0-45f1-94ce-e3063051aa28"}
9819b123-75f9-4633-b607-04e1c73cb28b	0f0f214e-bcb7-4d84-96cc-537986200898	f2435a80-01ac-45c5-b510-ea3f7c05396f	USER	invoice	d35e26d1-2e8e-451f-bb5f-396f5e77593d	CREATE	\N	\N	2026-07-14 13:25:06.335339+00	2026-07-15 09:50:17.391877+00	\N	\N	\N	\N	\N	\N	\N	{"invoice_number": "INV-00007"}
6446f373-e74e-4379-978c-534d872fdb72	0f0f214e-bcb7-4d84-96cc-537986200898	f2435a80-01ac-45c5-b510-ea3f7c05396f	USER	customer	da189bfc-c66e-40ea-8a54-46938efad956	CREATE	\N	\N	2026-07-14 13:25:06.336896+00	2026-07-15 09:50:17.391877+00	\N	\N	\N	\N	\N	\N	\N	{"name": "Customer 8"}
c48bb328-402d-43cf-a8e5-9f45b6765f01	0f0f214e-bcb7-4d84-96cc-537986200898	f2435a80-01ac-45c5-b510-ea3f7c05396f	USER	subscription	183aa1c0-89c2-434b-a2d5-670351dafde7	CREATE	\N	\N	2026-07-14 13:25:06.338456+00	2026-07-15 09:50:17.391877+00	\N	\N	\N	\N	\N	\N	\N	{"plan_id": "10ac3db2-eac0-45f1-94ce-e3063051aa28"}
5fda5fa0-8b5a-404f-a95b-302fa1730bbe	0f0f214e-bcb7-4d84-96cc-537986200898	f2435a80-01ac-45c5-b510-ea3f7c05396f	USER	invoice	d23659e1-0483-411c-9a55-bd3c5edfdae3	CREATE	\N	\N	2026-07-14 13:25:06.340027+00	2026-07-15 09:50:17.391877+00	\N	\N	\N	\N	\N	\N	\N	{"invoice_number": "INV-00008"}
dbf87827-55ba-46c6-97a4-ad8858bb5961	0f0f214e-bcb7-4d84-96cc-537986200898	f2435a80-01ac-45c5-b510-ea3f7c05396f	USER	customer	82ca83e5-99b7-4c84-acec-61cbf2f102b1	CREATE	\N	\N	2026-07-14 13:25:06.341067+00	2026-07-15 09:50:17.391877+00	\N	\N	\N	\N	\N	\N	\N	{"name": "Customer 9"}
76366a6b-d992-4cd6-a255-77512759c3ff	0f0f214e-bcb7-4d84-96cc-537986200898	f2435a80-01ac-45c5-b510-ea3f7c05396f	USER	subscription	991c9b03-482b-423e-b09b-459f0a65afa7	CREATE	\N	\N	2026-07-14 13:25:06.34268+00	2026-07-15 09:50:17.391877+00	\N	\N	\N	\N	\N	\N	\N	{"plan_id": "8484f42b-a419-460c-a92b-90ac6ccc77e6"}
d3e9069a-26e4-4ce6-95f5-a5fddc5afb31	0f0f214e-bcb7-4d84-96cc-537986200898	f2435a80-01ac-45c5-b510-ea3f7c05396f	USER	invoice	19203e3f-2239-4e1d-9ba7-e308e26ac688	CREATE	\N	\N	2026-07-14 13:25:06.344237+00	2026-07-15 09:50:17.391877+00	\N	\N	\N	\N	\N	\N	\N	{"invoice_number": "INV-00009"}
aa6157b1-2906-40e0-b06a-c8b747687360	0f0f214e-bcb7-4d84-96cc-537986200898	f2435a80-01ac-45c5-b510-ea3f7c05396f	USER	customer	6a3cc1cc-b9cb-4080-a4ff-9d215da0e451	CREATE	\N	\N	2026-07-14 13:25:06.367947+00	2026-07-15 09:50:17.391877+00	\N	\N	\N	\N	\N	\N	\N	{"name": "Customer 10"}
b9a61ce4-5b1f-44ea-a226-2d553d4eec55	0f0f214e-bcb7-4d84-96cc-537986200898	f2435a80-01ac-45c5-b510-ea3f7c05396f	USER	subscription	6bf9ae39-24bd-4877-ad00-95d48eea1c3b	CREATE	\N	\N	2026-07-14 13:25:06.369524+00	2026-07-15 09:50:17.391877+00	\N	\N	\N	\N	\N	\N	\N	{"plan_id": "10ac3db2-eac0-45f1-94ce-e3063051aa28"}
2b4cd947-0840-4fdb-896a-f4dca7b21794	0f0f214e-bcb7-4d84-96cc-537986200898	f2435a80-01ac-45c5-b510-ea3f7c05396f	USER	invoice	cadfd255-824c-4328-8a49-3df8c2846401	CREATE	\N	\N	2026-07-14 13:25:06.370564+00	2026-07-15 09:50:17.391877+00	\N	\N	\N	\N	\N	\N	\N	{"invoice_number": "INV-00010"}
70668ce4-fa9c-490b-875d-7b02a0f03f9f	0f0f214e-bcb7-4d84-96cc-537986200898	f2435a80-01ac-45c5-b510-ea3f7c05396f	USER	customer	c250be62-7f7d-47b9-a4d7-2cdcfccb7e46	CREATE	\N	\N	2026-07-14 13:25:06.372125+00	2026-07-15 09:50:17.391877+00	\N	\N	\N	\N	\N	\N	\N	{"name": "Customer 11"}
01724759-7c8a-4a69-9854-0d5a2cb14cd0	0f0f214e-bcb7-4d84-96cc-537986200898	f2435a80-01ac-45c5-b510-ea3f7c05396f	USER	subscription	45690492-7936-4bdb-a2a3-e3d40954f083	CREATE	\N	\N	2026-07-14 13:25:06.373358+00	2026-07-15 09:50:17.391877+00	\N	\N	\N	\N	\N	\N	\N	{"plan_id": "10ac3db2-eac0-45f1-94ce-e3063051aa28"}
35578dfb-45d4-455f-829e-40990d80f863	0f0f214e-bcb7-4d84-96cc-537986200898	f2435a80-01ac-45c5-b510-ea3f7c05396f	USER	invoice	230ddfa2-b6ad-4445-9761-3d41a6c6da7e	CREATE	\N	\N	2026-07-14 13:25:06.374938+00	2026-07-15 09:50:17.391877+00	\N	\N	\N	\N	\N	\N	\N	{"invoice_number": "INV-00011"}
acbc9025-6dae-4542-9e60-598e1c7d46b2	0f0f214e-bcb7-4d84-96cc-537986200898	f2435a80-01ac-45c5-b510-ea3f7c05396f	USER	customer	4b3993b9-e4ad-474c-9177-3c9c6705333a	CREATE	\N	\N	2026-07-14 13:25:06.376508+00	2026-07-15 09:50:17.391877+00	\N	\N	\N	\N	\N	\N	\N	{"name": "Customer 12"}
bbc89d9e-d7f3-4f4b-83ee-d9222eb0530d	0f0f214e-bcb7-4d84-96cc-537986200898	f2435a80-01ac-45c5-b510-ea3f7c05396f	USER	subscription	405605d8-265e-447d-8e28-af8ca028a3bf	CREATE	\N	\N	2026-07-14 13:25:06.378078+00	2026-07-15 09:50:17.391877+00	\N	\N	\N	\N	\N	\N	\N	{"plan_id": "8484f42b-a419-460c-a92b-90ac6ccc77e6"}
9d0b85ed-56f7-4aaa-88b5-05f3b6daa279	0f0f214e-bcb7-4d84-96cc-537986200898	f2435a80-01ac-45c5-b510-ea3f7c05396f	USER	invoice	15ceaef2-9352-4428-b2ea-15447b7e388e	CREATE	\N	\N	2026-07-14 13:25:06.379121+00	2026-07-15 09:50:17.391877+00	\N	\N	\N	\N	\N	\N	\N	{"invoice_number": "INV-00012"}
78df63dc-0489-4985-8cd4-92786c2e96b1	0f0f214e-bcb7-4d84-96cc-537986200898	f2435a80-01ac-45c5-b510-ea3f7c05396f	USER	customer	e84b0970-bbe5-4b8e-aa60-228c54f55f4b	CREATE	\N	\N	2026-07-14 13:25:06.401636+00	2026-07-15 09:50:17.391877+00	\N	\N	\N	\N	\N	\N	\N	{"name": "Customer 13"}
f8db1155-405b-40bf-879c-3a964384f71b	0f0f214e-bcb7-4d84-96cc-537986200898	f2435a80-01ac-45c5-b510-ea3f7c05396f	USER	subscription	f8975cb9-56ae-4f97-8a1e-62a8351ad565	CREATE	\N	\N	2026-07-14 13:25:06.403246+00	2026-07-15 09:50:17.391877+00	\N	\N	\N	\N	\N	\N	\N	{"plan_id": "10ac3db2-eac0-45f1-94ce-e3063051aa28"}
0a2f89e5-a8a3-4a4d-b327-01f5cd4d98c1	0f0f214e-bcb7-4d84-96cc-537986200898	f2435a80-01ac-45c5-b510-ea3f7c05396f	USER	invoice	0350f0ce-a2dd-4624-a976-3ecf4686c141	CREATE	\N	\N	2026-07-14 13:25:06.405893+00	2026-07-15 09:50:17.391877+00	\N	\N	\N	\N	\N	\N	\N	{"invoice_number": "INV-00013"}
01490e69-1d2d-440e-afdd-a9c51ef347e4	0f0f214e-bcb7-4d84-96cc-537986200898	f2435a80-01ac-45c5-b510-ea3f7c05396f	USER	customer	77b8b758-f230-4ba2-ae11-00edd0bba534	CREATE	\N	\N	2026-07-14 13:25:06.406896+00	2026-07-15 09:50:17.391877+00	\N	\N	\N	\N	\N	\N	\N	{"name": "Customer 14"}
2875a3b6-a5ad-4a55-a559-ccc0bdb1acb6	0f0f214e-bcb7-4d84-96cc-537986200898	f2435a80-01ac-45c5-b510-ea3f7c05396f	USER	subscription	ebbaa684-f125-45d8-8aaf-b65331a48138	CREATE	\N	\N	2026-07-14 13:25:06.408899+00	2026-07-15 09:50:17.391877+00	\N	\N	\N	\N	\N	\N	\N	{"plan_id": "10ac3db2-eac0-45f1-94ce-e3063051aa28"}
b77a85f9-5193-4dde-a318-18bd969a3d22	0f0f214e-bcb7-4d84-96cc-537986200898	f2435a80-01ac-45c5-b510-ea3f7c05396f	USER	invoice	c5e9924e-7f15-41b9-8b4e-5db7cb557ecd	CREATE	\N	\N	2026-07-14 13:25:06.409898+00	2026-07-15 09:50:17.391877+00	\N	\N	\N	\N	\N	\N	\N	{"invoice_number": "INV-00014"}
237c027e-1b7f-4e50-9ddd-4ba82a04027c	0f0f214e-bcb7-4d84-96cc-537986200898	f2435a80-01ac-45c5-b510-ea3f7c05396f	USER	customer	118882ea-be36-4d5b-aea9-22a1c0413353	CREATE	\N	\N	2026-07-14 13:25:06.411966+00	2026-07-15 09:50:17.391877+00	\N	\N	\N	\N	\N	\N	\N	{"name": "Customer 15"}
a01b067c-2cad-407a-8cf3-fb87cad6b3b3	0f0f214e-bcb7-4d84-96cc-537986200898	f2435a80-01ac-45c5-b510-ea3f7c05396f	USER	subscription	187dc506-5dbe-4dc1-b212-1d42972099dc	CREATE	\N	\N	2026-07-14 13:25:06.41297+00	2026-07-15 09:50:17.391877+00	\N	\N	\N	\N	\N	\N	\N	{"plan_id": "8484f42b-a419-460c-a92b-90ac6ccc77e6"}
0fda9d83-3667-42cf-b590-8b9dd25baf76	0f0f214e-bcb7-4d84-96cc-537986200898	f2435a80-01ac-45c5-b510-ea3f7c05396f	USER	invoice	644d2ea2-94c6-4f18-8474-0af6c8dad794	CREATE	\N	\N	2026-07-14 13:25:06.414971+00	2026-07-15 09:50:17.391877+00	\N	\N	\N	\N	\N	\N	\N	{"invoice_number": "INV-00015"}
\.


--
-- Data for Name: customers; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.customers (id, org_id, name, email, phone, metadata, created_at, updated_at, deleted_at, external_id, gender, is_senior, has_partner) FROM stdin;
fd3b0f83-d380-4ea6-b6a1-3f0368e4af09	813ba451-15b6-4e97-9890-626c99eda811	Customer 1	customer1@example.com	\N	{}	2026-07-14 13:25:06.246223+00	2026-07-15 09:59:41.136453+00	\N	\N	\N	f	f
e286fe63-64c5-4812-934b-c1f65a49fac6	813ba451-15b6-4e97-9890-626c99eda811	Customer 2	customer2@example.com	\N	{}	2026-07-14 13:25:06.259838+00	2026-07-15 09:59:41.136453+00	\N	\N	\N	f	f
2f8eec44-9b9f-4a1e-b61d-1f610b06cae8	813ba451-15b6-4e97-9890-626c99eda811	Customer 3	customer3@example.com	\N	{}	2026-07-14 13:25:06.2656+00	2026-07-15 09:59:41.136453+00	\N	\N	\N	f	f
27e47b59-bfb2-41f9-8abb-57643b22ca39	813ba451-15b6-4e97-9890-626c99eda811	Customer 4	customer4@example.com	\N	{}	2026-07-14 13:25:06.295125+00	2026-07-15 09:59:41.136453+00	\N	\N	\N	f	f
5829e36f-c22c-4ca8-9750-984be4234db6	813ba451-15b6-4e97-9890-626c99eda811	Customer 5	customer5@example.com	\N	{}	2026-07-14 13:25:06.299814+00	2026-07-15 09:59:41.136453+00	\N	\N	\N	f	f
b8e83644-aaa2-4c58-bd3e-27a80d679be7	813ba451-15b6-4e97-9890-626c99eda811	Customer 6	customer6@example.com	\N	{}	2026-07-14 13:25:06.305057+00	2026-07-15 09:59:41.136453+00	\N	\N	\N	f	f
e9dae2e7-53a9-4952-ac05-20bdf3ab23f4	813ba451-15b6-4e97-9890-626c99eda811	Customer 7	customer7@example.com	\N	{}	2026-07-14 13:25:06.331621+00	2026-07-15 09:59:41.136453+00	\N	\N	\N	f	f
da189bfc-c66e-40ea-8a54-46938efad956	813ba451-15b6-4e97-9890-626c99eda811	Customer 8	customer8@example.com	\N	{}	2026-07-14 13:25:06.336379+00	2026-07-15 09:59:41.136453+00	\N	\N	\N	f	f
82ca83e5-99b7-4c84-acec-61cbf2f102b1	813ba451-15b6-4e97-9890-626c99eda811	Customer 9	customer9@example.com	\N	{}	2026-07-14 13:25:06.340545+00	2026-07-15 09:59:41.136453+00	\N	\N	\N	f	f
6a3cc1cc-b9cb-4080-a4ff-9d215da0e451	813ba451-15b6-4e97-9890-626c99eda811	Customer 10	customer10@example.com	\N	{}	2026-07-14 13:25:06.366915+00	2026-07-15 09:59:41.136453+00	\N	\N	\N	f	f
c250be62-7f7d-47b9-a4d7-2cdcfccb7e46	813ba451-15b6-4e97-9890-626c99eda811	Customer 11	customer11@example.com	\N	{}	2026-07-14 13:25:06.371605+00	2026-07-15 09:59:41.136453+00	\N	\N	\N	f	f
4b3993b9-e4ad-474c-9177-3c9c6705333a	813ba451-15b6-4e97-9890-626c99eda811	Customer 12	customer12@example.com	\N	{}	2026-07-14 13:25:06.375979+00	2026-07-15 09:59:41.136453+00	\N	\N	\N	f	f
e84b0970-bbe5-4b8e-aa60-228c54f55f4b	813ba451-15b6-4e97-9890-626c99eda811	Customer 13	customer13@example.com	\N	{}	2026-07-14 13:25:06.401636+00	2026-07-15 09:59:41.136453+00	\N	\N	\N	f	f
77b8b758-f230-4ba2-ae11-00edd0bba534	813ba451-15b6-4e97-9890-626c99eda811	Customer 14	customer14@example.com	\N	{}	2026-07-14 13:25:06.405893+00	2026-07-15 09:59:41.136453+00	\N	\N	\N	f	f
118882ea-be36-4d5b-aea9-22a1c0413353	813ba451-15b6-4e97-9890-626c99eda811	Customer 15	customer15@example.com	\N	{}	2026-07-14 13:25:06.411401+00	2026-07-15 09:59:41.136453+00	\N	\N	\N	f	f
\.


--
-- Data for Name: flyway_schema_history; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) FROM stdin;
1	1	create schema	SQL	V1__create_schema.sql	-1477582333	submeter	2026-07-14 18:52:58.04453	137	t
2	2	create indexes	SQL	V2__create_indexes.sql	-1805391964	submeter	2026-07-14 18:52:58.210147	80	t
3	3	create functions	SQL	V3__create_functions.sql	694725130	submeter	2026-07-14 18:52:58.303534	13	t
4	4	create refresh tokens	SQL	V4__create_refresh_tokens.sql	-840833439	submeter	2026-07-14 18:52:58.330113	25	t
5	5	add telco fields	SQL	V5__add_telco_fields.sql	-2087443792	submeter	2026-07-15 09:50:17.33508	26	t
6	6	add settings tables	SQL	V6__add_settings_tables.sql	2075140162	submeter	2026-07-15 09:50:17.383899	69	t
\.


--
-- Data for Name: invitations; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.invitations (id, org_id, email, role, token_hash, status, invited_by, expires_at, created_at, updated_at, deleted_at) FROM stdin;
\.


--
-- Data for Name: invoice_line_items; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.invoice_line_items (id, invoice_id, description, quantity, unit_amount, amount, pricing_model, tier_detail, created_at, updated_at) FROM stdin;
\.


--
-- Data for Name: invoice_sequences; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.invoice_sequences (org_id, year, last_seq) FROM stdin;
\.


--
-- Data for Name: invoices; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.invoices (id, org_id, subscription_id, invoice_number, status, period_start, period_end, subtotal_cents, tax_cents, total_cents, due_at, paid_at, razorpay_order_id, pdf_path, created_at, updated_at) FROM stdin;
644d2ea2-94c6-4f18-8474-0af6c8dad794	813ba451-15b6-4e97-9890-626c99eda811	187dc506-5dbe-4dc1-b212-1d42972099dc	INV-00015	PAID	2026-05-31 13:25:05.691542+00	2026-06-30 13:25:05.691542+00	0	0	150000	2026-06-30 13:25:05.691542+00	\N	\N	\N	2026-07-14 13:25:06.413971+00	2026-07-15 09:59:41.136453+00
c5e9924e-7f15-41b9-8b4e-5db7cb557ecd	813ba451-15b6-4e97-9890-626c99eda811	ebbaa684-f125-45d8-8aaf-b65331a48138	INV-00014	PAID	2026-04-24 13:25:05.691542+00	2026-05-24 13:25:05.691542+00	0	0	500000	2026-05-24 13:25:05.691542+00	\N	\N	\N	2026-07-14 13:25:06.409898+00	2026-07-15 09:59:41.136453+00
0350f0ce-a2dd-4624-a976-3ecf4686c141	813ba451-15b6-4e97-9890-626c99eda811	f8975cb9-56ae-4f97-8a1e-62a8351ad565	INV-00013	PAID	2026-05-10 13:25:05.691542+00	2026-06-09 13:25:05.691542+00	0	0	500000	2026-06-09 13:25:05.691542+00	\N	\N	\N	2026-07-14 13:25:06.404389+00	2026-07-15 09:59:41.136453+00
15ceaef2-9352-4428-b2ea-15447b7e388e	813ba451-15b6-4e97-9890-626c99eda811	405605d8-265e-447d-8e28-af8ca028a3bf	INV-00012	OPEN	2026-07-08 13:25:05.691542+00	2026-08-07 13:25:05.691542+00	0	0	150000	2026-08-07 13:25:05.691542+00	\N	\N	\N	2026-07-14 13:25:06.378603+00	2026-07-15 09:59:41.136453+00
230ddfa2-b6ad-4445-9761-3d41a6c6da7e	813ba451-15b6-4e97-9890-626c99eda811	45690492-7936-4bdb-a2a3-e3d40954f083	INV-00011	PAID	2026-05-08 13:25:05.691542+00	2026-06-07 13:25:05.691542+00	0	0	500000	2026-06-07 13:25:05.691542+00	\N	\N	\N	2026-07-14 13:25:06.374418+00	2026-07-15 09:59:41.136453+00
cadfd255-824c-4328-8a49-3df8c2846401	813ba451-15b6-4e97-9890-626c99eda811	6bf9ae39-24bd-4877-ad00-95d48eea1c3b	INV-00010	PAID	2026-06-21 13:25:05.691542+00	2026-07-21 13:25:05.691542+00	0	0	500000	2026-07-21 13:25:05.691542+00	\N	\N	\N	2026-07-14 13:25:06.370041+00	2026-07-15 09:59:41.136453+00
19203e3f-2239-4e1d-9ba7-e308e26ac688	813ba451-15b6-4e97-9890-626c99eda811	991c9b03-482b-423e-b09b-459f0a65afa7	INV-00009	PAID	2026-06-08 13:25:05.691542+00	2026-07-08 13:25:05.691542+00	0	0	150000	2026-07-08 13:25:05.691542+00	\N	\N	\N	2026-07-14 13:25:06.343202+00	2026-07-15 09:59:41.136453+00
d23659e1-0483-411c-9a55-bd3c5edfdae3	813ba451-15b6-4e97-9890-626c99eda811	183aa1c0-89c2-434b-a2d5-670351dafde7	INV-00008	OPEN	2026-05-31 13:25:05.691542+00	2026-06-30 13:25:05.691542+00	0	0	500000	2026-06-30 13:25:05.691542+00	\N	\N	\N	2026-07-14 13:25:06.338979+00	2026-07-15 09:59:41.136453+00
d35e26d1-2e8e-451f-bb5f-396f5e77593d	813ba451-15b6-4e97-9890-626c99eda811	23ab7d92-110f-4cf1-90a6-932a692d3694	INV-00007	PAID	2026-05-10 13:25:05.691542+00	2026-06-09 13:25:05.691542+00	0	0	500000	2026-06-09 13:25:05.691542+00	\N	\N	\N	2026-07-14 13:25:06.334819+00	2026-07-15 09:59:41.136453+00
b7f89ccb-2cc0-47c2-9524-29c93b8a26de	813ba451-15b6-4e97-9890-626c99eda811	49e97a8c-47a1-4a63-a9cb-c4fa56c55870	INV-00006	PAID	2026-06-10 13:25:05.691542+00	2026-07-10 13:25:05.691542+00	0	0	150000	2026-07-10 13:25:05.691542+00	\N	\N	\N	2026-07-14 13:25:06.308159+00	2026-07-15 09:59:41.136453+00
0424c2da-e9bc-48b6-b4e5-b7eebacd465a	813ba451-15b6-4e97-9890-626c99eda811	c6cafeb3-5538-42e1-97a4-19ceb6581308	INV-00005	PAID	2026-05-26 13:25:05.691542+00	2026-06-25 13:25:05.691542+00	0	0	500000	2026-06-25 13:25:05.691542+00	\N	\N	\N	2026-07-14 13:25:06.303509+00	2026-07-15 09:59:41.136453+00
421c5445-57bd-492d-825b-18e666819ca8	813ba451-15b6-4e97-9890-626c99eda811	4be19af5-c9e1-41da-925c-4812cb3c369a	INV-00004	OPEN	2026-06-13 13:25:05.691542+00	2026-07-13 13:25:05.691542+00	0	0	500000	2026-07-13 13:25:05.691542+00	\N	\N	\N	2026-07-14 13:25:06.298262+00	2026-07-15 09:59:41.136453+00
447c8d15-db6c-4305-8b08-1ab2889382bf	813ba451-15b6-4e97-9890-626c99eda811	ce7c9b7f-9ca2-4fce-886f-1954e5ed726a	INV-00003	PAID	2026-05-08 13:25:05.691542+00	2026-06-07 13:25:05.691542+00	0	0	150000	2026-06-07 13:25:05.691542+00	\N	\N	\N	2026-07-14 13:25:06.269257+00	2026-07-15 09:59:41.136453+00
5dd14a90-819c-453b-9c19-9184382c5872	813ba451-15b6-4e97-9890-626c99eda811	91796f5f-5773-4688-a37d-f43b9001f2c6	INV-00002	PAID	2026-05-04 13:25:05.691542+00	2026-06-03 13:25:05.691542+00	0	0	500000	2026-06-03 13:25:05.691542+00	\N	\N	\N	2026-07-14 13:25:06.264035+00	2026-07-15 09:59:41.136453+00
b2201c48-a5ff-4cf3-a5d8-ff0337f64161	813ba451-15b6-4e97-9890-626c99eda811	9d8c6f7d-d40f-4fa0-b5b6-ab0f0c5f069b	INV-00001	PAID	2026-06-11 13:25:05.691542+00	2026-07-11 13:25:05.691542+00	0	0	500000	2026-07-11 13:25:05.691542+00	\N	\N	\N	2026-07-14 13:25:06.256041+00	2026-07-15 09:59:41.136453+00
\.


--
-- Data for Name: organization_members; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.organization_members (id, org_id, user_id, role, status, joined_at, created_at, updated_at, deleted_at) FROM stdin;
99de17e3-3ee3-48cf-b5c3-0ce4cdd16f30	0f0f214e-bcb7-4d84-96cc-537986200898	f2435a80-01ac-45c5-b510-ea3f7c05396f	OWNER	ACTIVE	2026-07-15 09:59:41.136453+00	2026-07-15 09:59:41.136453+00	2026-07-15 09:59:41.136453+00	\N
6f35eae8-0415-4103-8942-bbce92e8b05f	813ba451-15b6-4e97-9890-626c99eda811	f5deadab-294b-47d1-b141-dd97934f1e9b	OWNER	ACTIVE	2026-07-15 09:59:41.136453+00	2026-07-15 09:59:41.136453+00	2026-07-15 09:59:41.136453+00	\N
946136b3-5eab-40dd-b08b-9c51ec55c5af	bd4f4b21-5e95-41d0-b7a0-d94ee58a79b7	31a77a8b-0d54-4019-885c-1a80a945eb08	OWNER	ACTIVE	2026-07-15 09:59:41.136453+00	2026-07-15 09:59:41.136453+00	2026-07-15 09:59:41.136453+00	\N
\.


--
-- Data for Name: organizations; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.organizations (id, name, slug, created_at, updated_at, deleted_at, logo_url, timezone, currency, support_email, default_tax_rate, invoice_prefix, invoice_footer, company_website, company_address, version) FROM stdin;
0f0f214e-bcb7-4d84-96cc-537986200898	Demo Inc	demo-inc	2026-07-14 13:25:06.230989+00	2026-07-14 13:25:06.230989+00	\N	\N	UTC	INR	\N	\N	\N	\N	\N	\N	0
813ba451-15b6-4e97-9890-626c99eda811	Jagga	jagga	2026-07-14 14:20:44.698976+00	2026-07-14 14:20:44.698976+00	\N	\N	UTC	INR	\N	\N	\N	\N	\N	\N	0
bd4f4b21-5e95-41d0-b7a0-d94ee58a79b7	jagga corp	jagga-corp	2026-07-15 09:54:54.122172+00	2026-07-15 09:54:54.122172+00	\N	\N	UTC	INR	\N	\N	\N	\N	\N	\N	0
\.


--
-- Data for Name: payments; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.payments (id, org_id, invoice_id, razorpay_order_id, razorpay_payment_id, amount_cents, currency, status, failure_reason, created_at, updated_at, payment_method) FROM stdin;
\.


--
-- Data for Name: plan_tiers; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.plan_tiers (id, plan_id, tier_order, up_to, unit_amount, flat_fee, created_at, updated_at) FROM stdin;
\.


--
-- Data for Name: plans; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.plans (id, org_id, name, description, pricing_model, flat_amount, billing_interval, trial_days, is_archived, version, created_at, updated_at) FROM stdin;
10ac3db2-eac0-45f1-94ce-e3063051aa28	0f0f214e-bcb7-4d84-96cc-537986200898	Pro (Flat)	Simple flat rate	FLAT	500000	MONTHLY	0	f	1	2026-07-14 13:25:06.240355+00	2026-07-14 13:25:06.240355+00
8484f42b-a419-460c-a92b-90ac6ccc77e6	0f0f214e-bcb7-4d84-96cc-537986200898	Scale (Metered)	Usage based API billing	METERED	100000	MONTHLY	0	f	1	2026-07-14 13:25:06.241948+00	2026-07-14 13:25:06.241948+00
\.


--
-- Data for Name: refresh_tokens; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.refresh_tokens (id, user_id, token, expires_at, revoked_at, created_at, updated_at) FROM stdin;
cc274033-785c-42ad-bb67-886201f1880a	f2435a80-01ac-45c5-b510-ea3f7c05396f	8fe98cd7-cbf8-4aec-9d41-675dc084586c	2026-07-21 13:26:57.065858+00	2026-07-14 13:42:06.139366+00	2026-07-14 13:26:57.071968+00	2026-07-14 13:42:05.663456+00
1b72291e-564e-4a2f-bcce-69389055df76	f2435a80-01ac-45c5-b510-ea3f7c05396f	d3b59dab-c6e7-4a7f-be03-a57ce9c55262	2026-07-21 13:42:06.152243+00	\N	2026-07-14 13:42:06.168059+00	2026-07-14 13:42:06.168059+00
5f0683ae-be7f-43f5-a1cc-dd9b8bd7a1d5	f5deadab-294b-47d1-b141-dd97934f1e9b	90609db9-e90c-488c-b124-09c5b10d4a90	2026-07-21 14:20:44.695469+00	2026-07-14 14:20:45.281235+00	2026-07-14 14:20:44.708495+00	2026-07-14 14:20:44.843929+00
742c4766-41f5-4dc6-aa2a-f22618703ebf	f5deadab-294b-47d1-b141-dd97934f1e9b	57f38714-1378-4d30-b7fa-7f03b5d26d7f	2026-07-21 14:20:45.283304+00	2026-07-14 14:36:29.994559+00	2026-07-14 14:20:45.288509+00	2026-07-14 14:36:29.565285+00
0b6400ad-bb18-40d3-94ac-d9a34f599dd6	f5deadab-294b-47d1-b141-dd97934f1e9b	2d4d2817-0e16-4b1f-8f39-eff9b64b7f02	2026-07-21 14:36:29.997125+00	2026-07-14 14:57:21.257059+00	2026-07-14 14:36:29.99917+00	2026-07-14 14:57:20.777145+00
10ac8a97-65d5-461d-a94e-dbe6541757ea	f5deadab-294b-47d1-b141-dd97934f1e9b	1aab80fd-923c-489d-8557-0151a4029907	2026-07-21 14:57:21.269902+00	2026-07-14 15:32:09.592958+00	2026-07-14 14:57:21.286732+00	2026-07-14 15:32:09.148394+00
956bff50-455d-4977-a783-367b9e782e3c	31a77a8b-0d54-4019-885c-1a80a945eb08	a546e489-46a4-484a-b927-ad01f683f2df	2026-07-22 09:54:54.119093+00	2026-07-15 09:54:54.686032+00	2026-07-15 09:54:54.130286+00	2026-07-15 09:54:54.281338+00
09a1a996-53ae-4a11-941c-7983c4e15569	f5deadab-294b-47d1-b141-dd97934f1e9b	846c8ce5-cb41-4824-b88e-901e14a43e20	2026-07-21 15:32:09.596615+00	2026-07-15 09:57:25.785106+00	2026-07-14 15:32:09.598706+00	2026-07-15 09:57:25.315238+00
1fdb0ba9-21de-461c-b1ea-7c9d12cc162d	31a77a8b-0d54-4019-885c-1a80a945eb08	11a4f3f6-c5d4-411f-a619-6e4c02f2c9b0	2026-07-22 09:54:54.688117+00	2026-07-15 09:57:33.802181+00	2026-07-15 09:54:54.693916+00	2026-07-15 09:57:33.312314+00
fc90afba-b0cc-4c53-a3c7-36bb3c1bc439	31a77a8b-0d54-4019-885c-1a80a945eb08	78f3e836-dc2a-4457-b06a-78e4bdfbc196	2026-07-22 09:57:33.80425+00	\N	2026-07-15 09:57:33.805797+00	2026-07-15 09:57:33.805797+00
a1d9711b-9588-4efa-8c21-0fd99f274303	f5deadab-294b-47d1-b141-dd97934f1e9b	9d9158b8-9563-4792-850f-4ced31474112	2026-07-22 09:57:25.792889+00	2026-07-15 10:01:00.667877+00	2026-07-15 09:57:25.795627+00	2026-07-15 10:01:00.223111+00
029f51d9-ab85-4111-9c71-61f61247e649	f5deadab-294b-47d1-b141-dd97934f1e9b	0d33e684-af25-4546-84a5-e2e423d4a368	2026-07-22 10:01:00.670586+00	\N	2026-07-15 10:01:00.672222+00	2026-07-15 10:01:00.672222+00
\.


--
-- Data for Name: subscriptions; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.subscriptions (id, org_id, customer_id, plan_id, plan_version, status, trial_end_at, current_period_start, current_period_end, canceled_at, cancellation_reason, created_at, updated_at, months_active) FROM stdin;
9d8c6f7d-d40f-4fa0-b5b6-ab0f0c5f069b	813ba451-15b6-4e97-9890-626c99eda811	fd3b0f83-d380-4ea6-b6a1-3f0368e4af09	10ac3db2-eac0-45f1-94ce-e3063051aa28	0	ACTIVE	\N	2026-06-11 13:25:05.691542+00	2026-07-11 13:25:05.691542+00	\N	\N	2026-07-14 13:25:06.25187+00	2026-07-15 09:59:41.136453+00	\N
91796f5f-5773-4688-a37d-f43b9001f2c6	813ba451-15b6-4e97-9890-626c99eda811	e286fe63-64c5-4812-934b-c1f65a49fac6	10ac3db2-eac0-45f1-94ce-e3063051aa28	0	ACTIVE	\N	2026-05-04 13:25:05.691542+00	2026-06-03 13:25:05.691542+00	\N	\N	2026-07-14 13:25:06.261901+00	2026-07-15 09:59:41.136453+00	\N
ce7c9b7f-9ca2-4fce-886f-1954e5ed726a	813ba451-15b6-4e97-9890-626c99eda811	2f8eec44-9b9f-4a1e-b61d-1f610b06cae8	8484f42b-a419-460c-a92b-90ac6ccc77e6	0	ACTIVE	\N	2026-05-08 13:25:05.691542+00	2026-06-07 13:25:05.691542+00	\N	\N	2026-07-14 13:25:06.267171+00	2026-07-15 09:59:41.136453+00	\N
4be19af5-c9e1-41da-925c-4812cb3c369a	813ba451-15b6-4e97-9890-626c99eda811	27e47b59-bfb2-41f9-8abb-57643b22ca39	10ac3db2-eac0-45f1-94ce-e3063051aa28	0	ACTIVE	\N	2026-06-13 13:25:05.691542+00	2026-07-13 13:25:05.691542+00	\N	\N	2026-07-14 13:25:06.296688+00	2026-07-15 09:59:41.136453+00	\N
c6cafeb3-5538-42e1-97a4-19ceb6581308	813ba451-15b6-4e97-9890-626c99eda811	5829e36f-c22c-4ca8-9750-984be4234db6	10ac3db2-eac0-45f1-94ce-e3063051aa28	0	ACTIVE	\N	2026-05-26 13:25:05.691542+00	2026-06-25 13:25:05.691542+00	\N	\N	2026-07-14 13:25:06.301375+00	2026-07-15 09:59:41.136453+00	\N
49e97a8c-47a1-4a63-a9cb-c4fa56c55870	813ba451-15b6-4e97-9890-626c99eda811	b8e83644-aaa2-4c58-bd3e-27a80d679be7	8484f42b-a419-460c-a92b-90ac6ccc77e6	0	ACTIVE	\N	2026-06-10 13:25:05.691542+00	2026-07-10 13:25:05.691542+00	\N	\N	2026-07-14 13:25:06.306633+00	2026-07-15 09:59:41.136453+00	\N
23ab7d92-110f-4cf1-90a6-932a692d3694	813ba451-15b6-4e97-9890-626c99eda811	e9dae2e7-53a9-4952-ac05-20bdf3ab23f4	10ac3db2-eac0-45f1-94ce-e3063051aa28	0	ACTIVE	\N	2026-05-10 13:25:05.691542+00	2026-06-09 13:25:05.691542+00	\N	\N	2026-07-14 13:25:06.333232+00	2026-07-15 09:59:41.136453+00	\N
183aa1c0-89c2-434b-a2d5-670351dafde7	813ba451-15b6-4e97-9890-626c99eda811	da189bfc-c66e-40ea-8a54-46938efad956	10ac3db2-eac0-45f1-94ce-e3063051aa28	0	ACTIVE	\N	2026-05-31 13:25:05.691542+00	2026-06-30 13:25:05.691542+00	\N	\N	2026-07-14 13:25:06.337418+00	2026-07-15 09:59:41.136453+00	\N
991c9b03-482b-423e-b09b-459f0a65afa7	813ba451-15b6-4e97-9890-626c99eda811	82ca83e5-99b7-4c84-acec-61cbf2f102b1	8484f42b-a419-460c-a92b-90ac6ccc77e6	0	ACTIVE	\N	2026-06-08 13:25:05.691542+00	2026-07-08 13:25:05.691542+00	\N	\N	2026-07-14 13:25:06.342105+00	2026-07-15 09:59:41.136453+00	\N
6bf9ae39-24bd-4877-ad00-95d48eea1c3b	813ba451-15b6-4e97-9890-626c99eda811	6a3cc1cc-b9cb-4080-a4ff-9d215da0e451	10ac3db2-eac0-45f1-94ce-e3063051aa28	0	ACTIVE	\N	2026-06-21 13:25:05.691542+00	2026-07-21 13:25:05.691542+00	\N	\N	2026-07-14 13:25:06.368479+00	2026-07-15 09:59:41.136453+00	\N
45690492-7936-4bdb-a2a3-e3d40954f083	813ba451-15b6-4e97-9890-626c99eda811	c250be62-7f7d-47b9-a4d7-2cdcfccb7e46	10ac3db2-eac0-45f1-94ce-e3063051aa28	0	ACTIVE	\N	2026-05-08 13:25:05.691542+00	2026-06-07 13:25:05.691542+00	\N	\N	2026-07-14 13:25:06.372835+00	2026-07-15 09:59:41.136453+00	\N
405605d8-265e-447d-8e28-af8ca028a3bf	813ba451-15b6-4e97-9890-626c99eda811	4b3993b9-e4ad-474c-9177-3c9c6705333a	8484f42b-a419-460c-a92b-90ac6ccc77e6	0	ACTIVE	\N	2026-07-08 13:25:05.691542+00	2026-08-07 13:25:05.691542+00	\N	\N	2026-07-14 13:25:06.377032+00	2026-07-15 09:59:41.136453+00	\N
f8975cb9-56ae-4f97-8a1e-62a8351ad565	813ba451-15b6-4e97-9890-626c99eda811	e84b0970-bbe5-4b8e-aa60-228c54f55f4b	10ac3db2-eac0-45f1-94ce-e3063051aa28	0	CANCELED	\N	2026-05-10 13:25:05.691542+00	2026-06-09 13:25:05.691542+00	2026-05-25 13:25:05.691542+00	\N	2026-07-14 13:25:06.403246+00	2026-07-15 09:59:41.136453+00	\N
ebbaa684-f125-45d8-8aaf-b65331a48138	813ba451-15b6-4e97-9890-626c99eda811	77b8b758-f230-4ba2-ae11-00edd0bba534	10ac3db2-eac0-45f1-94ce-e3063051aa28	0	CANCELED	\N	2026-04-24 13:25:05.691542+00	2026-05-24 13:25:05.691542+00	2026-05-09 13:25:05.691542+00	\N	2026-07-14 13:25:06.407896+00	2026-07-15 09:59:41.136453+00	\N
187dc506-5dbe-4dc1-b212-1d42972099dc	813ba451-15b6-4e97-9890-626c99eda811	118882ea-be36-4d5b-aea9-22a1c0413353	8484f42b-a419-460c-a92b-90ac6ccc77e6	0	CANCELED	\N	2026-05-31 13:25:05.691542+00	2026-06-30 13:25:05.691542+00	2026-06-15 13:25:05.691542+00	\N	2026-07-14 13:25:06.41297+00	2026-07-15 09:59:41.136453+00	\N
\.


--
-- Data for Name: usage_events; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.usage_events (id, org_id, subscription_id, event_type, quantity, idempotency_key, occurred_at, invoice_id, rejected_reason, metadata, created_at, updated_at) FROM stdin;
1ff74e03-edfe-408f-8780-99df4c1a525b	0f0f214e-bcb7-4d84-96cc-537986200898	ce7c9b7f-9ca2-4fce-886f-1954e5ed726a	api_calls	885	304346a1-8789-4681-b0ea-8b8e30ba8ac0	2026-05-08 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.270813+00	2026-07-14 13:25:06.270813+00
9958f8d0-529b-4bf4-919b-49b644b17b74	0f0f214e-bcb7-4d84-96cc-537986200898	ce7c9b7f-9ca2-4fce-886f-1954e5ed726a	api_calls	58	7942d091-aad1-4c57-abbf-ca0fe6a45432	2026-05-09 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.272955+00	2026-07-14 13:25:06.272955+00
9b90bf9d-8abb-4328-9436-887abc362664	0f0f214e-bcb7-4d84-96cc-537986200898	ce7c9b7f-9ca2-4fce-886f-1954e5ed726a	api_calls	426	4fde086a-5abb-4de6-8151-f31de31dba6c	2026-05-10 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.273473+00	2026-07-14 13:25:06.273473+00
ab4d40f6-7e99-412b-839b-acd938d38ea9	0f0f214e-bcb7-4d84-96cc-537986200898	ce7c9b7f-9ca2-4fce-886f-1954e5ed726a	api_calls	280	0b9a48a6-b9cb-4a48-84bd-82709075c912	2026-05-11 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.274509+00	2026-07-14 13:25:06.274509+00
a81a4c5d-75ed-4b2a-8c76-e6e0e302c0ea	0f0f214e-bcb7-4d84-96cc-537986200898	ce7c9b7f-9ca2-4fce-886f-1954e5ed726a	api_calls	452	e3c126b2-6378-4c6a-a1b6-cbfa2f39dacf	2026-05-12 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.27556+00	2026-07-14 13:25:06.27556+00
4bf47eeb-4d26-45c7-ad97-2bf872457862	0f0f214e-bcb7-4d84-96cc-537986200898	ce7c9b7f-9ca2-4fce-886f-1954e5ed726a	api_calls	291	d835f393-7e0e-464b-ae43-7b06e01e3939	2026-05-13 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.276083+00	2026-07-14 13:25:06.276083+00
ae50838e-1f3c-4b69-91ba-a8f6b2ff3382	0f0f214e-bcb7-4d84-96cc-537986200898	ce7c9b7f-9ca2-4fce-886f-1954e5ed726a	api_calls	112	088d5c8f-6704-402c-adf5-1c37cb300006	2026-05-14 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.277111+00	2026-07-14 13:25:06.277111+00
190be180-71a5-449a-972b-8a644f3c699f	0f0f214e-bcb7-4d84-96cc-537986200898	ce7c9b7f-9ca2-4fce-886f-1954e5ed726a	api_calls	667	2282a565-5fe7-49be-b9be-96012e654dba	2026-05-15 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.27764+00	2026-07-14 13:25:06.27764+00
19a15ea0-95be-41b6-9d09-e0e824b012f8	0f0f214e-bcb7-4d84-96cc-537986200898	ce7c9b7f-9ca2-4fce-886f-1954e5ed726a	api_calls	498	b968929c-e4ba-4dbf-8b34-c68fa02d9e1d	2026-05-16 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.278694+00	2026-07-14 13:25:06.278694+00
bcb9059a-a383-4f78-8033-16bfa6b9d827	0f0f214e-bcb7-4d84-96cc-537986200898	ce7c9b7f-9ca2-4fce-886f-1954e5ed726a	api_calls	833	4a77d1a7-439a-488e-98ec-f86ea00183e0	2026-05-17 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.279254+00	2026-07-14 13:25:06.279254+00
6d5bdb53-ebce-44b0-972d-2a947f34be11	0f0f214e-bcb7-4d84-96cc-537986200898	ce7c9b7f-9ca2-4fce-886f-1954e5ed726a	api_calls	459	73945ed8-0cc3-47b9-949d-b4d2ac43845b	2026-05-18 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.280299+00	2026-07-14 13:25:06.280299+00
17a54496-3b04-4ff9-bf5a-bc47f173cabf	0f0f214e-bcb7-4d84-96cc-537986200898	ce7c9b7f-9ca2-4fce-886f-1954e5ed726a	api_calls	388	4bd17802-01bc-4c90-96e3-024ce8defa27	2026-05-19 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.280823+00	2026-07-14 13:25:06.280823+00
d1094763-fdea-408e-b0be-5bc66769af30	0f0f214e-bcb7-4d84-96cc-537986200898	ce7c9b7f-9ca2-4fce-886f-1954e5ed726a	api_calls	946	513a058b-3429-43ba-9441-8b80c97e3857	2026-05-20 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.281854+00	2026-07-14 13:25:06.281854+00
4748f9b7-9e09-4e43-956f-1b656439dfc0	0f0f214e-bcb7-4d84-96cc-537986200898	ce7c9b7f-9ca2-4fce-886f-1954e5ed726a	api_calls	844	d75a53f0-3652-42b0-a659-9832452407c3	2026-05-21 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.282367+00	2026-07-14 13:25:06.282367+00
3e5d8e74-9afa-4376-9515-b7d186e0f35f	0f0f214e-bcb7-4d84-96cc-537986200898	ce7c9b7f-9ca2-4fce-886f-1954e5ed726a	api_calls	290	e30b177e-9fb4-439a-9984-731afc49be95	2026-05-22 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.28288+00	2026-07-14 13:25:06.28288+00
d8e9ef47-85e3-44a5-8e7d-5d418924a1b2	0f0f214e-bcb7-4d84-96cc-537986200898	ce7c9b7f-9ca2-4fce-886f-1954e5ed726a	api_calls	227	5a4dd622-e936-412b-af25-6a4bcb5a2473	2026-05-23 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.283969+00	2026-07-14 13:25:06.283969+00
2c7ce862-f1b1-4892-840a-4677cf748fd2	0f0f214e-bcb7-4d84-96cc-537986200898	ce7c9b7f-9ca2-4fce-886f-1954e5ed726a	api_calls	987	8989d192-296c-4ec1-9485-3082c02165bb	2026-05-24 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.284493+00	2026-07-14 13:25:06.284493+00
ee6b7fb5-dcbb-4d50-ab8c-f72013fec4d8	0f0f214e-bcb7-4d84-96cc-537986200898	ce7c9b7f-9ca2-4fce-886f-1954e5ed726a	api_calls	389	4dd7dd3e-0920-4bb1-94ef-303a91b42c4a	2026-05-25 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.28501+00	2026-07-14 13:25:06.28501+00
a0486829-663c-4ea1-be8e-6779352045d2	0f0f214e-bcb7-4d84-96cc-537986200898	ce7c9b7f-9ca2-4fce-886f-1954e5ed726a	api_calls	944	df7b3547-f1ee-4813-9c0b-b72c9f34e369	2026-05-26 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.286044+00	2026-07-14 13:25:06.286044+00
9f0d9409-9895-481d-8abb-3d7d860ea462	0f0f214e-bcb7-4d84-96cc-537986200898	ce7c9b7f-9ca2-4fce-886f-1954e5ed726a	api_calls	770	627d06b3-995f-498b-bd3b-090ef558f3fd	2026-05-27 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.286566+00	2026-07-14 13:25:06.286566+00
2b0d8be1-fbaa-40ac-86d9-fa6733c899e3	0f0f214e-bcb7-4d84-96cc-537986200898	ce7c9b7f-9ca2-4fce-886f-1954e5ed726a	api_calls	772	2806f7d4-a572-46ba-9e3e-62d643374cad	2026-05-28 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.28764+00	2026-07-14 13:25:06.28764+00
d5866953-1845-42db-9510-176d9d288bec	0f0f214e-bcb7-4d84-96cc-537986200898	ce7c9b7f-9ca2-4fce-886f-1954e5ed726a	api_calls	986	eb9d8c81-7d7e-410b-8096-61755f821ca9	2026-05-29 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.288154+00	2026-07-14 13:25:06.288154+00
cca16083-75fd-4f61-86d6-b63ad83c6f88	0f0f214e-bcb7-4d84-96cc-537986200898	ce7c9b7f-9ca2-4fce-886f-1954e5ed726a	api_calls	539	acf8aa24-dd90-42af-9aae-0e085e3cc455	2026-05-30 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.289187+00	2026-07-14 13:25:06.289187+00
d8684372-26a7-40e7-be9d-9f34524e5cb5	0f0f214e-bcb7-4d84-96cc-537986200898	ce7c9b7f-9ca2-4fce-886f-1954e5ed726a	api_calls	414	61901f89-a02e-4679-a57d-fe34cf12524d	2026-05-31 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.289728+00	2026-07-14 13:25:06.289728+00
0207b1e3-7751-4f0f-ac0d-c7c37d1d7783	0f0f214e-bcb7-4d84-96cc-537986200898	ce7c9b7f-9ca2-4fce-886f-1954e5ed726a	api_calls	784	fb57727e-423c-46be-90c5-cf5f6d10e471	2026-06-01 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.290772+00	2026-07-14 13:25:06.290772+00
4776129e-6b01-49ff-81d4-17cf5880a1db	0f0f214e-bcb7-4d84-96cc-537986200898	ce7c9b7f-9ca2-4fce-886f-1954e5ed726a	api_calls	443	3e52f127-c362-4a23-b89c-8d259c2f863b	2026-06-02 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.291298+00	2026-07-14 13:25:06.291298+00
24688e99-6665-430c-bd4d-773fa865b017	0f0f214e-bcb7-4d84-96cc-537986200898	ce7c9b7f-9ca2-4fce-886f-1954e5ed726a	api_calls	675	0388f929-7095-41ee-9527-ad1ef76e4b3d	2026-06-03 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.292319+00	2026-07-14 13:25:06.292319+00
789ec051-3658-4bd7-877f-6cb24cb7e5ab	0f0f214e-bcb7-4d84-96cc-537986200898	ce7c9b7f-9ca2-4fce-886f-1954e5ed726a	api_calls	272	dcd2eb9f-4705-40ac-8270-94cc210c663d	2026-06-04 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.292906+00	2026-07-14 13:25:06.292906+00
daff75a8-6fd7-4b96-9ddb-db4cf3cf69bc	0f0f214e-bcb7-4d84-96cc-537986200898	ce7c9b7f-9ca2-4fce-886f-1954e5ed726a	api_calls	785	e815dc35-f38d-49ee-b1e1-1f5c48546813	2026-06-05 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.293428+00	2026-07-14 13:25:06.293428+00
8b7c0b13-c495-4007-99c1-814deada0572	0f0f214e-bcb7-4d84-96cc-537986200898	ce7c9b7f-9ca2-4fce-886f-1954e5ed726a	api_calls	911	1c1a5b53-1acf-4441-9ff8-f46865d7dc32	2026-06-06 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.294473+00	2026-07-14 13:25:06.294473+00
5e6a11de-6145-4ede-ae17-9b669f70451f	0f0f214e-bcb7-4d84-96cc-537986200898	49e97a8c-47a1-4a63-a9cb-c4fa56c55870	api_calls	120	073c0b71-ac2b-41d3-9ff0-298ae1b724ed	2026-06-10 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.309686+00	2026-07-14 13:25:06.309686+00
747ba070-5ea3-44b8-a083-f631910cdd49	0f0f214e-bcb7-4d84-96cc-537986200898	49e97a8c-47a1-4a63-a9cb-c4fa56c55870	api_calls	680	454eaef8-ca66-48c8-b60b-a3a320211563	2026-06-11 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.309686+00	2026-07-14 13:25:06.309686+00
879faa84-9f5e-44f3-9c37-525f18db4e80	0f0f214e-bcb7-4d84-96cc-537986200898	49e97a8c-47a1-4a63-a9cb-c4fa56c55870	api_calls	975	c2117278-335e-4629-902e-f4342093db39	2026-06-12 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.31069+00	2026-07-14 13:25:06.31069+00
213d9e26-9d1d-489c-a9f0-fff0dd014dd7	0f0f214e-bcb7-4d84-96cc-537986200898	49e97a8c-47a1-4a63-a9cb-c4fa56c55870	api_calls	208	16fa73a2-96f3-4420-8815-fa52ad32cd30	2026-06-13 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.311691+00	2026-07-14 13:25:06.311691+00
2b87be5e-fc15-4a00-a596-6423d135b47d	0f0f214e-bcb7-4d84-96cc-537986200898	49e97a8c-47a1-4a63-a9cb-c4fa56c55870	api_calls	161	46ca24bc-e0e0-450d-ade8-115f2ef4aac3	2026-06-14 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.311691+00	2026-07-14 13:25:06.311691+00
7cee5239-354e-4d09-a194-16e76291cd94	0f0f214e-bcb7-4d84-96cc-537986200898	49e97a8c-47a1-4a63-a9cb-c4fa56c55870	api_calls	635	086068df-1e9b-4e72-8dc5-2dbbf1326301	2026-06-15 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.31269+00	2026-07-14 13:25:06.31269+00
ca664c5b-dc66-4911-8544-e18e98d725c2	0f0f214e-bcb7-4d84-96cc-537986200898	49e97a8c-47a1-4a63-a9cb-c4fa56c55870	api_calls	141	b6eff525-650c-4475-b713-57169bb43e21	2026-06-16 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.313725+00	2026-07-14 13:25:06.313725+00
38a80957-12b7-40e0-8218-c9fb9fa1c038	0f0f214e-bcb7-4d84-96cc-537986200898	49e97a8c-47a1-4a63-a9cb-c4fa56c55870	api_calls	660	79f53f0a-e6ea-4df9-b3ee-b0607b0e271d	2026-06-17 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.314723+00	2026-07-14 13:25:06.314723+00
af1b3ad5-fb52-49da-8a32-9269a8e408ee	0f0f214e-bcb7-4d84-96cc-537986200898	49e97a8c-47a1-4a63-a9cb-c4fa56c55870	api_calls	843	cbb5a0d7-3517-47cd-8385-35f77536b2b2	2026-06-18 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.314723+00	2026-07-14 13:25:06.314723+00
66a6b4bd-8c2a-43eb-ad26-ea570bfe9a7e	0f0f214e-bcb7-4d84-96cc-537986200898	49e97a8c-47a1-4a63-a9cb-c4fa56c55870	api_calls	52	468003fa-b6a7-4f12-864d-8a9894c84874	2026-06-19 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.315724+00	2026-07-14 13:25:06.315724+00
e2b4afb1-531e-4d05-a184-f4b0e4413cad	0f0f214e-bcb7-4d84-96cc-537986200898	49e97a8c-47a1-4a63-a9cb-c4fa56c55870	api_calls	904	d18fe2b0-8b2d-45df-9286-df74a7d6d100	2026-06-20 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.316727+00	2026-07-14 13:25:06.316727+00
368bd558-372b-4133-9784-56a22a339e14	0f0f214e-bcb7-4d84-96cc-537986200898	49e97a8c-47a1-4a63-a9cb-c4fa56c55870	api_calls	176	7adc4a1d-c237-4c23-b05e-99b7e3eb38ea	2026-06-21 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.316727+00	2026-07-14 13:25:06.316727+00
30724e23-6275-4c27-8d4c-8841f0736990	0f0f214e-bcb7-4d84-96cc-537986200898	49e97a8c-47a1-4a63-a9cb-c4fa56c55870	api_calls	204	1c0ccda0-6cca-4c0a-a5ba-5c14b52445ce	2026-06-22 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.318232+00	2026-07-14 13:25:06.318232+00
47e3d92f-11c4-4fee-af30-a2adb5b2a609	0f0f214e-bcb7-4d84-96cc-537986200898	49e97a8c-47a1-4a63-a9cb-c4fa56c55870	api_calls	952	01e9b49b-3eca-41d1-8b70-0e079afc6dc9	2026-06-23 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.319236+00	2026-07-14 13:25:06.319236+00
f4fb161e-0a25-45c5-9c3e-6065407fa08c	0f0f214e-bcb7-4d84-96cc-537986200898	49e97a8c-47a1-4a63-a9cb-c4fa56c55870	api_calls	667	ea92d9b1-0cc6-45ab-9e63-595e9e0c9926	2026-06-24 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.319236+00	2026-07-14 13:25:06.319236+00
76ad97df-f299-4c35-a744-9303db32d230	0f0f214e-bcb7-4d84-96cc-537986200898	49e97a8c-47a1-4a63-a9cb-c4fa56c55870	api_calls	489	df2ffb61-b15b-4204-ac5d-cb118744416d	2026-06-25 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.320235+00	2026-07-14 13:25:06.320235+00
4f9ca518-b6bd-406c-9ebe-8ad1623f4b3e	0f0f214e-bcb7-4d84-96cc-537986200898	49e97a8c-47a1-4a63-a9cb-c4fa56c55870	api_calls	201	3514fb11-afc5-45da-b324-eafe2c166c3f	2026-06-26 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.321235+00	2026-07-14 13:25:06.321235+00
d4eb9730-bcb2-4628-a861-65fdd041db12	0f0f214e-bcb7-4d84-96cc-537986200898	49e97a8c-47a1-4a63-a9cb-c4fa56c55870	api_calls	341	be5a05dd-437b-492d-96c1-c57994a57af8	2026-06-27 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.322237+00	2026-07-14 13:25:06.322237+00
1bef6742-674b-4466-bde6-b9872c2250d5	0f0f214e-bcb7-4d84-96cc-537986200898	49e97a8c-47a1-4a63-a9cb-c4fa56c55870	api_calls	301	0d3c6cfb-8b35-4146-9f42-4ea00934534d	2026-06-28 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.322237+00	2026-07-14 13:25:06.322237+00
b969c39e-960f-481a-b885-206517a493c4	0f0f214e-bcb7-4d84-96cc-537986200898	49e97a8c-47a1-4a63-a9cb-c4fa56c55870	api_calls	530	eacccd2e-7899-447e-903f-03ec67efe611	2026-06-29 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.323421+00	2026-07-14 13:25:06.323421+00
91e0b699-6676-48e3-8095-69a48f5167ba	0f0f214e-bcb7-4d84-96cc-537986200898	49e97a8c-47a1-4a63-a9cb-c4fa56c55870	api_calls	599	a503ff34-7ba1-42ad-8903-46e363bb1395	2026-06-30 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.324422+00	2026-07-14 13:25:06.324422+00
8fe60a27-8d7b-4dd3-aae0-ed8c08986b86	0f0f214e-bcb7-4d84-96cc-537986200898	49e97a8c-47a1-4a63-a9cb-c4fa56c55870	api_calls	715	41428ed3-e0d7-4534-bd90-0e6ffcd0447c	2026-07-01 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.325421+00	2026-07-14 13:25:06.325421+00
5f7203fc-1758-4a1c-b7ba-4fd60be657a4	0f0f214e-bcb7-4d84-96cc-537986200898	49e97a8c-47a1-4a63-a9cb-c4fa56c55870	api_calls	374	418c9aa5-73b2-42b9-b1de-896c4b60425b	2026-07-02 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.325421+00	2026-07-14 13:25:06.325421+00
18bb340a-c84e-4391-9c97-53534381ffc5	0f0f214e-bcb7-4d84-96cc-537986200898	49e97a8c-47a1-4a63-a9cb-c4fa56c55870	api_calls	112	fc015229-b260-437a-802c-4474a644454f	2026-07-03 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.326422+00	2026-07-14 13:25:06.326422+00
7d294541-5070-41f3-9e46-aa8fe06f4762	0f0f214e-bcb7-4d84-96cc-537986200898	49e97a8c-47a1-4a63-a9cb-c4fa56c55870	api_calls	737	264cdf43-7f35-4d08-a0aa-761fa77cb18d	2026-07-04 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.327422+00	2026-07-14 13:25:06.327422+00
9ff66394-f4b0-48d8-a45d-fb34b02abf3b	0f0f214e-bcb7-4d84-96cc-537986200898	49e97a8c-47a1-4a63-a9cb-c4fa56c55870	api_calls	741	a02de9cd-be35-4183-b686-1ba99d952cbe	2026-07-05 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.327422+00	2026-07-14 13:25:06.327422+00
549b8c90-f301-474b-be6c-725a603b3c8b	0f0f214e-bcb7-4d84-96cc-537986200898	49e97a8c-47a1-4a63-a9cb-c4fa56c55870	api_calls	992	b38afdaa-877c-4470-ba81-b26a64127196	2026-07-06 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.328422+00	2026-07-14 13:25:06.328422+00
4c33065f-cbf2-495d-a590-c4235a0c2425	0f0f214e-bcb7-4d84-96cc-537986200898	49e97a8c-47a1-4a63-a9cb-c4fa56c55870	api_calls	735	875bd38a-a0b7-4aa3-bbf1-cc7db5e7f76a	2026-07-07 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.329461+00	2026-07-14 13:25:06.329461+00
90b6d57d-ddc7-4441-8a2f-00224435ecfd	0f0f214e-bcb7-4d84-96cc-537986200898	49e97a8c-47a1-4a63-a9cb-c4fa56c55870	api_calls	50	fcfded07-efcb-419b-986b-9005ea4a041e	2026-07-08 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.330062+00	2026-07-14 13:25:06.330062+00
1d4152b8-ed15-4ac8-a645-174e10a537b7	0f0f214e-bcb7-4d84-96cc-537986200898	49e97a8c-47a1-4a63-a9cb-c4fa56c55870	api_calls	262	8cd7650f-a3a5-4511-b5ba-44cd0822167b	2026-07-09 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.331104+00	2026-07-14 13:25:06.331104+00
7aa96d52-d744-433a-85d1-27dc785e5ee7	0f0f214e-bcb7-4d84-96cc-537986200898	991c9b03-482b-423e-b09b-459f0a65afa7	api_calls	844	089354fa-8469-4b6f-8f2c-7cac3e68a9f7	2026-06-08 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.344748+00	2026-07-14 13:25:06.344748+00
dd0281a1-20b0-4c26-8b7a-0c8c318b179c	0f0f214e-bcb7-4d84-96cc-537986200898	991c9b03-482b-423e-b09b-459f0a65afa7	api_calls	623	1d3ef475-19cf-46e7-b501-94fff71b170f	2026-06-09 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.34578+00	2026-07-14 13:25:06.34578+00
f635e11d-ea96-4ccb-be4b-ae06a8d13502	0f0f214e-bcb7-4d84-96cc-537986200898	991c9b03-482b-423e-b09b-459f0a65afa7	api_calls	762	16bf2c67-681e-4be7-85c2-4c10ce8e9fd8	2026-06-10 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.346291+00	2026-07-14 13:25:06.346291+00
131b1cd7-b797-43a5-9bca-bf900bff2586	0f0f214e-bcb7-4d84-96cc-537986200898	991c9b03-482b-423e-b09b-459f0a65afa7	api_calls	631	85dde6a8-60d9-43c4-b445-37bab91ca6fa	2026-06-11 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.347334+00	2026-07-14 13:25:06.347334+00
3abd01dd-4a00-452c-8597-8e092284a92f	0f0f214e-bcb7-4d84-96cc-537986200898	991c9b03-482b-423e-b09b-459f0a65afa7	api_calls	231	25c69d1d-60c7-4efb-928e-f870744f4d9b	2026-06-12 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.34786+00	2026-07-14 13:25:06.34786+00
60c6551d-afce-43c8-b637-c7a3577cddc7	0f0f214e-bcb7-4d84-96cc-537986200898	991c9b03-482b-423e-b09b-459f0a65afa7	api_calls	76	2ed04f44-2640-4344-ba5e-87286ae10d61	2026-06-13 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.348382+00	2026-07-14 13:25:06.348382+00
a378a3c1-a17e-4230-b3ea-96171ec5c026	0f0f214e-bcb7-4d84-96cc-537986200898	991c9b03-482b-423e-b09b-459f0a65afa7	api_calls	349	6f8d31ae-05ce-4c84-98bf-64d9a0caf208	2026-06-14 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.349465+00	2026-07-14 13:25:06.349465+00
c56a8ed6-7633-458e-8afc-012f0e0f6a2e	0f0f214e-bcb7-4d84-96cc-537986200898	991c9b03-482b-423e-b09b-459f0a65afa7	api_calls	647	6d99f50a-ec70-4b5b-911a-46fcda2ef40e	2026-06-15 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.349994+00	2026-07-14 13:25:06.349994+00
a04fda7a-d46a-469c-bdd0-963021a438ef	0f0f214e-bcb7-4d84-96cc-537986200898	991c9b03-482b-423e-b09b-459f0a65afa7	api_calls	843	0bd5aa23-1ac2-4a04-bc48-a5be784f2944	2026-06-16 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.351031+00	2026-07-14 13:25:06.351031+00
64404efa-a663-48c2-b438-3de1d788cedd	0f0f214e-bcb7-4d84-96cc-537986200898	991c9b03-482b-423e-b09b-459f0a65afa7	api_calls	96	b13122a8-c280-43a8-aa81-b0b221f347cb	2026-06-17 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.351763+00	2026-07-14 13:25:06.351763+00
ec83da8d-0ee0-4875-97e1-ce5a1798ea0c	0f0f214e-bcb7-4d84-96cc-537986200898	991c9b03-482b-423e-b09b-459f0a65afa7	api_calls	805	5958905c-e3eb-4a7a-bd1d-a856d37de214	2026-06-18 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.35285+00	2026-07-14 13:25:06.35285+00
58a480c7-fff2-499a-825e-a8dab44fbd78	0f0f214e-bcb7-4d84-96cc-537986200898	991c9b03-482b-423e-b09b-459f0a65afa7	api_calls	904	7031b27f-def2-431a-ab30-21d75a5de04d	2026-06-19 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.353365+00	2026-07-14 13:25:06.353365+00
d5638633-cdbc-4dba-a077-907b24ecc09b	0f0f214e-bcb7-4d84-96cc-537986200898	991c9b03-482b-423e-b09b-459f0a65afa7	api_calls	202	773fd2b0-cedd-4352-8321-0c823688513a	2026-06-20 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.354402+00	2026-07-14 13:25:06.354402+00
61d628c8-8390-48bd-af80-4c15ee8808c0	0f0f214e-bcb7-4d84-96cc-537986200898	991c9b03-482b-423e-b09b-459f0a65afa7	api_calls	225	3ac72f5b-c6da-4a9d-90db-43774d43294e	2026-06-21 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.354919+00	2026-07-14 13:25:06.354919+00
40a144fe-e59f-4bed-b73f-c9a9d608c959	0f0f214e-bcb7-4d84-96cc-537986200898	991c9b03-482b-423e-b09b-459f0a65afa7	api_calls	158	eb84c8b8-171d-4f87-84ba-3ea926030de8	2026-06-22 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.355442+00	2026-07-14 13:25:06.355442+00
433970ed-8590-4fc8-a65c-0fedebc0d767	0f0f214e-bcb7-4d84-96cc-537986200898	991c9b03-482b-423e-b09b-459f0a65afa7	api_calls	127	2b503bc1-7982-4dc2-9f5e-a2cd06909c73	2026-06-23 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.356486+00	2026-07-14 13:25:06.356486+00
4f88d23a-dc2e-4c9e-aa62-54823c8b8add	0f0f214e-bcb7-4d84-96cc-537986200898	991c9b03-482b-423e-b09b-459f0a65afa7	api_calls	172	76115481-0fd8-482c-8b5c-57ab911c845a	2026-06-24 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.357001+00	2026-07-14 13:25:06.357001+00
08a68c6c-4079-463b-afb5-c4780dde7c57	0f0f214e-bcb7-4d84-96cc-537986200898	991c9b03-482b-423e-b09b-459f0a65afa7	api_calls	864	d7216867-20fc-4070-8f09-74b859158451	2026-06-25 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.357523+00	2026-07-14 13:25:06.357523+00
3533ccff-eb27-4c3a-b5d5-6dee31f14b2a	0f0f214e-bcb7-4d84-96cc-537986200898	991c9b03-482b-423e-b09b-459f0a65afa7	api_calls	451	d8a3087d-6230-4a21-9100-643ecf71aff2	2026-06-26 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.358564+00	2026-07-14 13:25:06.358564+00
7da16781-726c-4fb7-b875-a44d776f4108	0f0f214e-bcb7-4d84-96cc-537986200898	991c9b03-482b-423e-b09b-459f0a65afa7	api_calls	792	6ab3f355-5e88-4e08-8874-5df1c8c15796	2026-06-27 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.359089+00	2026-07-14 13:25:06.359089+00
f044ca27-b299-416e-87c6-1d3df7a0969c	0f0f214e-bcb7-4d84-96cc-537986200898	991c9b03-482b-423e-b09b-459f0a65afa7	api_calls	860	080a535e-9679-48f5-8093-d15d9e2b2e0e	2026-06-28 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.360128+00	2026-07-14 13:25:06.360128+00
cb96408e-32e8-4de7-b41d-aa02330210a6	0f0f214e-bcb7-4d84-96cc-537986200898	991c9b03-482b-423e-b09b-459f0a65afa7	api_calls	271	63e0984b-77f3-450d-a745-43a494e6fdbf	2026-06-29 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.360644+00	2026-07-14 13:25:06.360644+00
3032a3e7-1b84-4fa2-b5c1-72eeab24bd50	0f0f214e-bcb7-4d84-96cc-537986200898	991c9b03-482b-423e-b09b-459f0a65afa7	api_calls	50	ad55c07e-f99c-425b-a600-3aeea0af3620	2026-06-30 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.361164+00	2026-07-14 13:25:06.361164+00
7625a2cf-c1ea-44df-a24c-8322aaab24ea	0f0f214e-bcb7-4d84-96cc-537986200898	991c9b03-482b-423e-b09b-459f0a65afa7	api_calls	530	01a8e6e9-958c-4e4a-aac5-9e4cc40a7815	2026-07-01 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.362203+00	2026-07-14 13:25:06.362203+00
d907839a-e468-491f-981b-a8608c78c344	0f0f214e-bcb7-4d84-96cc-537986200898	991c9b03-482b-423e-b09b-459f0a65afa7	api_calls	361	f65fb335-7a97-44bb-a8ab-557e1ba6e457	2026-07-02 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.362722+00	2026-07-14 13:25:06.362722+00
c52c4c36-fe3d-448a-91fd-911edaf5cab2	0f0f214e-bcb7-4d84-96cc-537986200898	991c9b03-482b-423e-b09b-459f0a65afa7	api_calls	202	3fc2ab87-6068-47c1-9236-e4c48e9812ea	2026-07-03 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.363765+00	2026-07-14 13:25:06.363765+00
d31a6d5a-5ca4-4028-a7a6-9dcfb14f4249	0f0f214e-bcb7-4d84-96cc-537986200898	991c9b03-482b-423e-b09b-459f0a65afa7	api_calls	500	b66515db-831f-4494-bf54-64f4be437dca	2026-07-04 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.364289+00	2026-07-14 13:25:06.364289+00
014cf4cf-08bd-46ee-8a08-4ab2c55f1631	0f0f214e-bcb7-4d84-96cc-537986200898	991c9b03-482b-423e-b09b-459f0a65afa7	api_calls	309	bff1168d-0c33-42e9-85d4-a7394ecc2fd5	2026-07-05 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.364828+00	2026-07-14 13:25:06.364828+00
c9654708-96e9-4a14-bb74-3aa93c5b9732	0f0f214e-bcb7-4d84-96cc-537986200898	991c9b03-482b-423e-b09b-459f0a65afa7	api_calls	208	4f0ff743-f110-4a4c-ad9d-97a531938264	2026-07-06 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.36535+00	2026-07-14 13:25:06.36535+00
8ff7c991-c663-4cff-b955-69232367dabf	0f0f214e-bcb7-4d84-96cc-537986200898	991c9b03-482b-423e-b09b-459f0a65afa7	api_calls	368	d2ed75c3-0115-48cc-a3cf-9f8e0442f6a3	2026-07-07 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.366389+00	2026-07-14 13:25:06.366389+00
b09cab87-6cab-4381-93a6-a8a0f88761e4	0f0f214e-bcb7-4d84-96cc-537986200898	405605d8-265e-447d-8e28-af8ca028a3bf	api_calls	241	f7ca9b51-483f-4842-9f77-3de1a4463110	2026-07-08 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.380188+00	2026-07-14 13:25:06.380188+00
ccafcb87-aa8b-481c-8f87-ca30b769b74d	0f0f214e-bcb7-4d84-96cc-537986200898	405605d8-265e-447d-8e28-af8ca028a3bf	api_calls	625	fd14f60c-3e75-412d-9306-b228a918a2a1	2026-07-09 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.380713+00	2026-07-14 13:25:06.380713+00
cd551511-c819-4726-a854-0dc7e7867a73	0f0f214e-bcb7-4d84-96cc-537986200898	405605d8-265e-447d-8e28-af8ca028a3bf	api_calls	711	29c5bb2b-ba42-45fd-a868-d209e4cc7a85	2026-07-10 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.381751+00	2026-07-14 13:25:06.381751+00
bb6a5e31-7032-4bc9-908c-f034d540a097	0f0f214e-bcb7-4d84-96cc-537986200898	405605d8-265e-447d-8e28-af8ca028a3bf	api_calls	498	eaffee1d-1e6e-4b34-b335-6a3b8578e248	2026-07-11 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.382269+00	2026-07-14 13:25:06.382269+00
391ce7f4-faf1-4713-92ba-a65200f131d7	0f0f214e-bcb7-4d84-96cc-537986200898	405605d8-265e-447d-8e28-af8ca028a3bf	api_calls	703	18697730-2e57-4ee6-8aad-197dd0bef715	2026-07-12 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.382888+00	2026-07-14 13:25:06.382888+00
efa867a5-f9c4-4d6a-b664-f30803a04e58	0f0f214e-bcb7-4d84-96cc-537986200898	405605d8-265e-447d-8e28-af8ca028a3bf	api_calls	361	d670d6a8-3257-45e8-8cb5-997925e09d18	2026-07-13 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.383414+00	2026-07-14 13:25:06.383414+00
737240e1-f05b-4df3-b113-ffe351f775a9	0f0f214e-bcb7-4d84-96cc-537986200898	405605d8-265e-447d-8e28-af8ca028a3bf	api_calls	408	55c64d03-7efa-47cb-ba3c-7be2c86f143c	2026-07-14 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.384451+00	2026-07-14 13:25:06.384451+00
a5c93eb4-1103-4203-9cbb-66f339c8d8dd	0f0f214e-bcb7-4d84-96cc-537986200898	405605d8-265e-447d-8e28-af8ca028a3bf	api_calls	918	06198ce3-2397-4b49-b215-7fea6b105e0d	2026-07-15 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.384967+00	2026-07-14 13:25:06.384967+00
7d64449c-1902-489d-bbdf-ff2de325da5d	0f0f214e-bcb7-4d84-96cc-537986200898	405605d8-265e-447d-8e28-af8ca028a3bf	api_calls	928	d7a1b865-b57b-4725-afab-4c7c4d1fb1cf	2026-07-16 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.38549+00	2026-07-14 13:25:06.38549+00
1521756d-d146-40f1-86a7-722e0a8dd74a	0f0f214e-bcb7-4d84-96cc-537986200898	405605d8-265e-447d-8e28-af8ca028a3bf	api_calls	432	3e113191-ea82-450a-9da8-d2d69bb7413e	2026-07-17 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.386015+00	2026-07-14 13:25:06.386015+00
a0ce74ed-c74d-4238-8dc6-7b1770dec496	0f0f214e-bcb7-4d84-96cc-537986200898	405605d8-265e-447d-8e28-af8ca028a3bf	api_calls	811	c84a2a8d-3540-49ca-bc35-ec6c959ed2d5	2026-07-18 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.387052+00	2026-07-14 13:25:06.387052+00
78877f28-9d93-4597-b107-64615161f6e7	0f0f214e-bcb7-4d84-96cc-537986200898	405605d8-265e-447d-8e28-af8ca028a3bf	api_calls	650	19555cbf-1b7d-49cf-9bfa-6fd065a911ef	2026-07-19 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.387574+00	2026-07-14 13:25:06.387574+00
41421185-e606-4b6d-816f-1041a010cad4	0f0f214e-bcb7-4d84-96cc-537986200898	405605d8-265e-447d-8e28-af8ca028a3bf	api_calls	941	71d222b6-fd5f-4684-b2a8-ff29a08aca1e	2026-07-20 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.388097+00	2026-07-14 13:25:06.388097+00
09c99370-3c63-4bf1-9d6a-e3604b068de0	0f0f214e-bcb7-4d84-96cc-537986200898	405605d8-265e-447d-8e28-af8ca028a3bf	api_calls	827	8811a1b6-a23f-49d5-b903-87cee4903da0	2026-07-21 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.38914+00	2026-07-14 13:25:06.38914+00
c9bd500d-3b2c-490b-b605-76b38555845b	0f0f214e-bcb7-4d84-96cc-537986200898	405605d8-265e-447d-8e28-af8ca028a3bf	api_calls	307	10471277-a221-486e-b8cc-745b3409732c	2026-07-22 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.389651+00	2026-07-14 13:25:06.389651+00
eb21435a-94fb-45d3-8829-665a218cd470	0f0f214e-bcb7-4d84-96cc-537986200898	405605d8-265e-447d-8e28-af8ca028a3bf	api_calls	532	27b89482-1fe4-42ec-81e0-5497958a63ba	2026-07-23 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.390694+00	2026-07-14 13:25:06.390694+00
f421f6ca-97e9-4041-89ce-937d165b597a	0f0f214e-bcb7-4d84-96cc-537986200898	405605d8-265e-447d-8e28-af8ca028a3bf	api_calls	471	eaf78512-bc1f-4d01-8197-d53cdfc0be7e	2026-07-24 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.391213+00	2026-07-14 13:25:06.391213+00
1d334dc8-ce36-4f03-9c16-b80eb1de08f0	0f0f214e-bcb7-4d84-96cc-537986200898	405605d8-265e-447d-8e28-af8ca028a3bf	api_calls	565	03c070aa-51c8-4456-a5b0-4d59b2c04a46	2026-07-25 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.391736+00	2026-07-14 13:25:06.391736+00
530acc08-df2b-4553-b10e-9cac415760fe	0f0f214e-bcb7-4d84-96cc-537986200898	405605d8-265e-447d-8e28-af8ca028a3bf	api_calls	394	fc88c9a1-5528-46d0-9432-c18dc44baf1d	2026-07-26 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.392794+00	2026-07-14 13:25:06.392794+00
10f17699-633c-4102-b03d-561590305489	0f0f214e-bcb7-4d84-96cc-537986200898	405605d8-265e-447d-8e28-af8ca028a3bf	api_calls	347	61e3c995-805a-4ce5-b2a6-a7d39f2bca25	2026-07-27 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.393317+00	2026-07-14 13:25:06.393317+00
501cbfd8-8678-490a-8047-bdfa4d481235	0f0f214e-bcb7-4d84-96cc-537986200898	405605d8-265e-447d-8e28-af8ca028a3bf	api_calls	875	542b9fec-790b-4be3-afd1-5895f19de794	2026-07-28 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.394365+00	2026-07-14 13:25:06.394365+00
5db4505f-93e7-4a37-bc1c-dbb7637b5d22	0f0f214e-bcb7-4d84-96cc-537986200898	405605d8-265e-447d-8e28-af8ca028a3bf	api_calls	971	e8bbe7a1-5f9b-4fb8-a4bb-10443590c259	2026-07-29 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.394885+00	2026-07-14 13:25:06.394885+00
04e14954-503e-437b-b6a0-a244557cc6f7	0f0f214e-bcb7-4d84-96cc-537986200898	405605d8-265e-447d-8e28-af8ca028a3bf	api_calls	348	a10e6a60-94ec-4b38-86aa-2a1b33536843	2026-07-30 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.395947+00	2026-07-14 13:25:06.395947+00
12232fe0-9e45-4cba-9b2c-ad2c86caef39	0f0f214e-bcb7-4d84-96cc-537986200898	405605d8-265e-447d-8e28-af8ca028a3bf	api_calls	755	3bedd3f5-ebd3-4efe-8388-ea5fd8d96ff8	2026-07-31 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.39699+00	2026-07-14 13:25:06.39699+00
17d8970b-ef53-4f3b-b172-4b9a053fde9d	0f0f214e-bcb7-4d84-96cc-537986200898	405605d8-265e-447d-8e28-af8ca028a3bf	api_calls	516	bef1b2f4-1529-4124-b3a7-0c200b843a99	2026-08-01 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.397505+00	2026-07-14 13:25:06.397505+00
6db5add6-3b02-4672-a480-6b1377f7f9cb	0f0f214e-bcb7-4d84-96cc-537986200898	405605d8-265e-447d-8e28-af8ca028a3bf	api_calls	659	02ee71a3-550b-412a-ae68-ed695d72ac72	2026-08-02 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.39803+00	2026-07-14 13:25:06.39803+00
497a26fc-a92d-462d-b030-edfc9f9f7d92	0f0f214e-bcb7-4d84-96cc-537986200898	405605d8-265e-447d-8e28-af8ca028a3bf	api_calls	133	ea4abc39-32dd-4a24-ab11-87a78b8ef164	2026-08-03 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.399067+00	2026-07-14 13:25:06.399067+00
257f7895-99e9-4529-b4e7-aac630a4ac82	0f0f214e-bcb7-4d84-96cc-537986200898	405605d8-265e-447d-8e28-af8ca028a3bf	api_calls	211	1a10a2ec-bd68-47d6-a230-899959732a0b	2026-08-04 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.399585+00	2026-07-14 13:25:06.399585+00
7e337106-8926-4cbf-b14c-0a21e9d010db	0f0f214e-bcb7-4d84-96cc-537986200898	405605d8-265e-447d-8e28-af8ca028a3bf	api_calls	761	b196af3e-9254-4936-bc2f-5884ac2c26a5	2026-08-05 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.400106+00	2026-07-14 13:25:06.400106+00
b3081a79-d574-4774-839c-abc532f161ec	0f0f214e-bcb7-4d84-96cc-537986200898	405605d8-265e-447d-8e28-af8ca028a3bf	api_calls	960	44597c76-6893-4d26-9e8a-5477d317fd0e	2026-08-06 13:25:05.691542+00	\N	\N	{}	2026-07-14 13:25:06.40063+00	2026-07-14 13:25:06.40063+00
\.


--
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.users (id, org_id, email, password_hash, role, email_verified, last_login_at, failed_login_count, locked_until, created_at, updated_at, deleted_at) FROM stdin;
f2435a80-01ac-45c5-b510-ea3f7c05396f	0f0f214e-bcb7-4d84-96cc-537986200898	demo@submeter.app	$argon2id$v=19$m=65536,t=3,p=1$tHNAACBFsse2K27i065A2Q$jEgIBL+CVqQCyP2jToDRoaIOuBctWWoBcBXQe4PABR0	ADMIN	f	2026-07-14 13:42:06.134707+00	0	\N	2026-07-14 13:25:06.237728+00	2026-07-14 13:42:05.663456+00	\N
31a77a8b-0d54-4019-885c-1a80a945eb08	bd4f4b21-5e95-41d0-b7a0-d94ee58a79b7	binoremohapatra@gmail.com	$argon2id$v=19$m=65536,t=3,p=1$uzPeYTf3ArDwkaGrz0c/5g$0T4I1/StrnHU/CKyJ9jhV8yCav8QcX9C+V/LuSn1VeM	OWNER	f	2026-07-15 09:57:33.799554+00	0	\N	2026-07-15 09:54:54.128735+00	2026-07-15 09:57:33.312314+00	\N
f5deadab-294b-47d1-b141-dd97934f1e9b	813ba451-15b6-4e97-9890-626c99eda811	mohapatrabinore9@gmail.com	$argon2id$v=19$m=65536,t=3,p=1$KaNoFHMz+FlH1nhOc9dGyA$djxvdDsmn7rZ7iEluGhtCFefegdmutxAlwkk7jT/DTU	OWNER	f	2026-07-15 10:01:00.66561+00	0	\N	2026-07-14 14:20:44.706422+00	2026-07-15 10:01:00.223111+00	\N
\.


--
-- Name: customers customers_external_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customers
    ADD CONSTRAINT customers_external_id_key UNIQUE (external_id);


--
-- Name: flyway_schema_history flyway_schema_history_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.flyway_schema_history
    ADD CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank);


--
-- Name: api_keys pk_api_keys; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.api_keys
    ADD CONSTRAINT pk_api_keys PRIMARY KEY (id);


--
-- Name: audit_log pk_audit_log; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.audit_log
    ADD CONSTRAINT pk_audit_log PRIMARY KEY (id);


--
-- Name: customers pk_customers; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customers
    ADD CONSTRAINT pk_customers PRIMARY KEY (id);


--
-- Name: invitations pk_invitations; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invitations
    ADD CONSTRAINT pk_invitations PRIMARY KEY (id);


--
-- Name: invoice_line_items pk_invoice_line_items; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invoice_line_items
    ADD CONSTRAINT pk_invoice_line_items PRIMARY KEY (id);


--
-- Name: invoice_sequences pk_invoice_sequences; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invoice_sequences
    ADD CONSTRAINT pk_invoice_sequences PRIMARY KEY (org_id, year);


--
-- Name: invoices pk_invoices; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invoices
    ADD CONSTRAINT pk_invoices PRIMARY KEY (id);


--
-- Name: organization_members pk_organization_members; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.organization_members
    ADD CONSTRAINT pk_organization_members PRIMARY KEY (id);


--
-- Name: organizations pk_organizations; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.organizations
    ADD CONSTRAINT pk_organizations PRIMARY KEY (id);


--
-- Name: payments pk_payments; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT pk_payments PRIMARY KEY (id);


--
-- Name: plan_tiers pk_plan_tiers; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.plan_tiers
    ADD CONSTRAINT pk_plan_tiers PRIMARY KEY (id);


--
-- Name: plans pk_plans; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.plans
    ADD CONSTRAINT pk_plans PRIMARY KEY (id);


--
-- Name: refresh_tokens pk_refresh_tokens; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT pk_refresh_tokens PRIMARY KEY (id);


--
-- Name: subscriptions pk_subscriptions; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.subscriptions
    ADD CONSTRAINT pk_subscriptions PRIMARY KEY (id);


--
-- Name: usage_events pk_usage_events; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.usage_events
    ADD CONSTRAINT pk_usage_events PRIMARY KEY (id);


--
-- Name: users pk_users; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT pk_users PRIMARY KEY (id);


--
-- Name: api_keys uq_api_keys_hash; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.api_keys
    ADD CONSTRAINT uq_api_keys_hash UNIQUE (key_hash);


--
-- Name: api_keys uq_api_keys_key_id; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.api_keys
    ADD CONSTRAINT uq_api_keys_key_id UNIQUE (key_id);


--
-- Name: invoices uq_invoices_number; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invoices
    ADD CONSTRAINT uq_invoices_number UNIQUE (invoice_number);


--
-- Name: invoices uq_invoices_period; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invoices
    ADD CONSTRAINT uq_invoices_period UNIQUE (subscription_id, period_start);


--
-- Name: organization_members uq_org_members_user_org; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.organization_members
    ADD CONSTRAINT uq_org_members_user_org UNIQUE (org_id, user_id);


--
-- Name: organizations uq_organizations_slug; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.organizations
    ADD CONSTRAINT uq_organizations_slug UNIQUE (slug);


--
-- Name: plan_tiers uq_plan_tiers_order; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.plan_tiers
    ADD CONSTRAINT uq_plan_tiers_order UNIQUE (plan_id, tier_order);


--
-- Name: refresh_tokens uq_refresh_tokens_token; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT uq_refresh_tokens_token UNIQUE (token);


--
-- Name: usage_events uq_usage_events_idempotency; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.usage_events
    ADD CONSTRAINT uq_usage_events_idempotency UNIQUE (idempotency_key);


--
-- Name: users uq_users_email; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT uq_users_email UNIQUE (email);


--
-- Name: flyway_schema_history_s_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX flyway_schema_history_s_idx ON public.flyway_schema_history USING btree (success);


--
-- Name: idx_audit_log_entity; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audit_log_entity ON public.audit_log USING btree (entity_type, entity_id, created_at DESC);


--
-- Name: idx_audit_log_org_created; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audit_log_org_created ON public.audit_log USING btree (org_id, created_at DESC);


--
-- Name: idx_customers_name_search; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_customers_name_search ON public.customers USING gin (to_tsvector('english'::regconfig, name)) WHERE (deleted_at IS NULL);


--
-- Name: idx_customers_org_created; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_customers_org_created ON public.customers USING btree (org_id, created_at DESC);


--
-- Name: idx_customers_org_email; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_customers_org_email ON public.customers USING btree (org_id, email) WHERE (deleted_at IS NULL);


--
-- Name: idx_invoices_org_created; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_invoices_org_created ON public.invoices USING btree (org_id, created_at DESC);


--
-- Name: idx_invoices_status_due; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_invoices_status_due ON public.invoices USING btree (status, due_at) WHERE (status = ANY (ARRAY['OPEN'::text, 'DRAFT'::text]));


--
-- Name: idx_invoices_subscription_period; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_invoices_subscription_period ON public.invoices USING btree (subscription_id, period_start);


--
-- Name: idx_line_items_invoice; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_line_items_invoice ON public.invoice_line_items USING btree (invoice_id);


--
-- Name: idx_payments_invoice; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payments_invoice ON public.payments USING btree (invoice_id);


--
-- Name: idx_payments_org_created; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payments_org_created ON public.payments USING btree (org_id, created_at DESC);


--
-- Name: idx_plans_org_active; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_plans_org_active ON public.plans USING btree (org_id, is_archived) WHERE (is_archived = false);


--
-- Name: idx_plans_org_created; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_plans_org_created ON public.plans USING btree (org_id, created_at DESC);


--
-- Name: idx_refresh_tokens_active; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_refresh_tokens_active ON public.refresh_tokens USING btree (token) WHERE (revoked_at IS NULL);


--
-- Name: idx_refresh_tokens_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_refresh_tokens_user_id ON public.refresh_tokens USING btree (user_id);


--
-- Name: idx_subscriptions_customer; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_subscriptions_customer ON public.subscriptions USING btree (customer_id);


--
-- Name: idx_subscriptions_org_created; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_subscriptions_org_created ON public.subscriptions USING btree (org_id, created_at DESC);


--
-- Name: idx_subscriptions_status_period_end; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_subscriptions_status_period_end ON public.subscriptions USING btree (status, current_period_end) WHERE (status = ANY (ARRAY['ACTIVE'::text, 'PAST_DUE'::text]));


--
-- Name: idx_subscriptions_trial_end; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_subscriptions_trial_end ON public.subscriptions USING btree (trial_end_at) WHERE (status = 'TRIAL'::text);


--
-- Name: idx_usage_events_org_created; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_usage_events_org_created ON public.usage_events USING btree (org_id, created_at DESC);


--
-- Name: idx_usage_events_subscription_occurred; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_usage_events_subscription_occurred ON public.usage_events USING btree (subscription_id, occurred_at DESC);


--
-- Name: idx_usage_events_unbilled; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_usage_events_unbilled ON public.usage_events USING btree (subscription_id, occurred_at) WHERE ((invoice_id IS NULL) AND (rejected_reason IS NULL));


--
-- Name: idx_users_org_created; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_users_org_created ON public.users USING btree (org_id, created_at DESC);


--
-- Name: uq_invitations_pending_email; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_invitations_pending_email ON public.invitations USING btree (org_id, email) WHERE (status = 'PENDING'::text);


--
-- Name: uq_payments_razorpay_payment_id; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_payments_razorpay_payment_id ON public.payments USING btree (razorpay_payment_id) WHERE (razorpay_payment_id IS NOT NULL);


--
-- Name: audit_log trg_audit_log_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_audit_log_updated_at BEFORE UPDATE ON public.audit_log FOR EACH ROW EXECUTE FUNCTION public.fn_set_updated_at();


--
-- Name: customers trg_customers_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_customers_updated_at BEFORE UPDATE ON public.customers FOR EACH ROW EXECUTE FUNCTION public.fn_set_updated_at();


--
-- Name: invoice_line_items trg_invoice_line_items_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_invoice_line_items_updated_at BEFORE UPDATE ON public.invoice_line_items FOR EACH ROW EXECUTE FUNCTION public.fn_set_updated_at();


--
-- Name: invoices trg_invoices_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_invoices_updated_at BEFORE UPDATE ON public.invoices FOR EACH ROW EXECUTE FUNCTION public.fn_set_updated_at();


--
-- Name: organizations trg_organizations_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_organizations_updated_at BEFORE UPDATE ON public.organizations FOR EACH ROW EXECUTE FUNCTION public.fn_set_updated_at();


--
-- Name: payments trg_payments_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_payments_updated_at BEFORE UPDATE ON public.payments FOR EACH ROW EXECUTE FUNCTION public.fn_set_updated_at();


--
-- Name: plan_tiers trg_plan_tiers_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_plan_tiers_updated_at BEFORE UPDATE ON public.plan_tiers FOR EACH ROW EXECUTE FUNCTION public.fn_set_updated_at();


--
-- Name: plans trg_plans_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_plans_updated_at BEFORE UPDATE ON public.plans FOR EACH ROW EXECUTE FUNCTION public.fn_set_updated_at();


--
-- Name: refresh_tokens trg_refresh_tokens_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_refresh_tokens_updated_at BEFORE UPDATE ON public.refresh_tokens FOR EACH ROW EXECUTE FUNCTION public.fn_set_updated_at();


--
-- Name: subscriptions trg_subscriptions_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_subscriptions_updated_at BEFORE UPDATE ON public.subscriptions FOR EACH ROW EXECUTE FUNCTION public.fn_set_updated_at();


--
-- Name: usage_events trg_usage_events_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_usage_events_updated_at BEFORE UPDATE ON public.usage_events FOR EACH ROW EXECUTE FUNCTION public.fn_set_updated_at();


--
-- Name: users trg_users_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_users_updated_at BEFORE UPDATE ON public.users FOR EACH ROW EXECUTE FUNCTION public.fn_set_updated_at();


--
-- Name: api_keys fk_api_keys_creator; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.api_keys
    ADD CONSTRAINT fk_api_keys_creator FOREIGN KEY (created_by) REFERENCES public.users(id);


--
-- Name: api_keys fk_api_keys_org; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.api_keys
    ADD CONSTRAINT fk_api_keys_org FOREIGN KEY (org_id) REFERENCES public.organizations(id);


--
-- Name: audit_log fk_audit_log_actor; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.audit_log
    ADD CONSTRAINT fk_audit_log_actor FOREIGN KEY (actor_id) REFERENCES public.users(id);


--
-- Name: audit_log fk_audit_log_org; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.audit_log
    ADD CONSTRAINT fk_audit_log_org FOREIGN KEY (org_id) REFERENCES public.organizations(id);


--
-- Name: customers fk_customers_org; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customers
    ADD CONSTRAINT fk_customers_org FOREIGN KEY (org_id) REFERENCES public.organizations(id);


--
-- Name: invitations fk_invitations_inviter; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invitations
    ADD CONSTRAINT fk_invitations_inviter FOREIGN KEY (invited_by) REFERENCES public.users(id);


--
-- Name: invitations fk_invitations_org; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invitations
    ADD CONSTRAINT fk_invitations_org FOREIGN KEY (org_id) REFERENCES public.organizations(id);


--
-- Name: invoice_sequences fk_invoice_seq_org; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invoice_sequences
    ADD CONSTRAINT fk_invoice_seq_org FOREIGN KEY (org_id) REFERENCES public.organizations(id);


--
-- Name: invoices fk_invoices_org; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invoices
    ADD CONSTRAINT fk_invoices_org FOREIGN KEY (org_id) REFERENCES public.organizations(id);


--
-- Name: invoices fk_invoices_subscription; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invoices
    ADD CONSTRAINT fk_invoices_subscription FOREIGN KEY (subscription_id) REFERENCES public.subscriptions(id);


--
-- Name: invoice_line_items fk_line_items_invoice; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invoice_line_items
    ADD CONSTRAINT fk_line_items_invoice FOREIGN KEY (invoice_id) REFERENCES public.invoices(id);


--
-- Name: organization_members fk_org_members_org; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.organization_members
    ADD CONSTRAINT fk_org_members_org FOREIGN KEY (org_id) REFERENCES public.organizations(id);


--
-- Name: organization_members fk_org_members_user; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.organization_members
    ADD CONSTRAINT fk_org_members_user FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: payments fk_payments_invoice; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT fk_payments_invoice FOREIGN KEY (invoice_id) REFERENCES public.invoices(id);


--
-- Name: payments fk_payments_org; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT fk_payments_org FOREIGN KEY (org_id) REFERENCES public.organizations(id);


--
-- Name: plan_tiers fk_plan_tiers_plan; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.plan_tiers
    ADD CONSTRAINT fk_plan_tiers_plan FOREIGN KEY (plan_id) REFERENCES public.plans(id);


--
-- Name: plans fk_plans_org; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.plans
    ADD CONSTRAINT fk_plans_org FOREIGN KEY (org_id) REFERENCES public.organizations(id);


--
-- Name: refresh_tokens fk_refresh_tokens_user; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: subscriptions fk_subscriptions_customer; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.subscriptions
    ADD CONSTRAINT fk_subscriptions_customer FOREIGN KEY (customer_id) REFERENCES public.customers(id);


--
-- Name: subscriptions fk_subscriptions_org; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.subscriptions
    ADD CONSTRAINT fk_subscriptions_org FOREIGN KEY (org_id) REFERENCES public.organizations(id);


--
-- Name: subscriptions fk_subscriptions_plan; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.subscriptions
    ADD CONSTRAINT fk_subscriptions_plan FOREIGN KEY (plan_id) REFERENCES public.plans(id);


--
-- Name: usage_events fk_usage_events_invoice; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.usage_events
    ADD CONSTRAINT fk_usage_events_invoice FOREIGN KEY (invoice_id) REFERENCES public.invoices(id);


--
-- Name: usage_events fk_usage_events_org; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.usage_events
    ADD CONSTRAINT fk_usage_events_org FOREIGN KEY (org_id) REFERENCES public.organizations(id);


--
-- Name: usage_events fk_usage_events_subscription; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.usage_events
    ADD CONSTRAINT fk_usage_events_subscription FOREIGN KEY (subscription_id) REFERENCES public.subscriptions(id);


--
-- Name: users fk_users_org; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT fk_users_org FOREIGN KEY (org_id) REFERENCES public.organizations(id);


--
-- PostgreSQL database dump complete
--

\unrestrict qVdpIZr2sVYdzMO00nrpqu4v2zFfQUpmqpSU0zqoKrUt2IS7RvL5MAIOOMwjw0k

