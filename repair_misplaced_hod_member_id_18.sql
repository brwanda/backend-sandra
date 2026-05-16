-- Repair misplaced HOD row in csub_committee_members (example: ID 18)
-- Moves the member into country_committee_member, updates users role/link,
-- then removes the wrong subcommittee-member row.

BEGIN;

-- 1) Validate and load source row (must be in Head Of Delegation/HOD subcommittee)
WITH src AS (
    SELECT
        csm.id,
        csm.name,
        csm.phone,
        csm.email,
        csm.country_id,
        csm.chair,
        csm.vice_chair,
        csm.committee_secretary,
        csm.committee_member,
        csm.secretary_of_delegation,
        sc.subcommittee_name,
        sc.parent_committee_id
    FROM csub_committee_members csm
    JOIN sub_committee sc ON sc.id = csm.position_in_ear
    WHERE csm.id = 18
      AND (
          lower(sc.subcommittee_name) LIKE '%head of delegation%'
          OR lower(sc.subcommittee_name) = 'hod'
      )
),
hod_committee AS (
    SELECT c.id AS committee_id
    FROM committee c
    WHERE lower(c.committee_name) LIKE '%head of delegation%'
    ORDER BY c.id
    LIMIT 1
),
resolved AS (
    SELECT
        s.*,
        COALESCE(s.parent_committee_id, h.committee_id) AS target_committee_id
    FROM src s
    CROSS JOIN hod_committee h
),
updated_existing AS (
    UPDATE country_committee_member ccm
    SET
        name = r.name,
        phone = r.phone,
        country_id = r.country_id,
        committee_id = r.target_committee_id,
        chair = r.chair,
        vice_chair = r.vice_chair,
        committee_secretary = r.committee_secretary,
        committee_member = r.committee_member,
        delegation_secretary = r.secretary_of_delegation
    FROM resolved r
    WHERE lower(ccm.email) = lower(r.email)
      AND ccm.committee_id = r.target_committee_id
    RETURNING ccm.id, ccm.email
)
INSERT INTO country_committee_member (
    name, phone, email, country_id, committee_id,
    chair, vice_chair, committee_secretary, committee_member, delegation_secretary
)
SELECT
    r.name, r.phone, r.email, r.country_id, r.target_committee_id,
    r.chair, r.vice_chair, r.committee_secretary, r.committee_member, r.secretary_of_delegation
FROM resolved r
WHERE NOT EXISTS (SELECT 1 FROM updated_existing);

-- 2) Keep users table aligned: HOD role + no subcommittee link
UPDATE users u
SET
    role = 'HOD',
    subcommittee_id = NULL,
    country_id = csm.country_id
FROM csub_committee_members csm
WHERE csm.id = 18
  AND lower(u.email) = lower(csm.email);

-- 3) Delete wrong source row from subcommittee members table
DELETE FROM csub_committee_members WHERE id = 18;

COMMIT;

-- Verification
SELECT 'recent HOD committee-member rows' AS check_name;
SELECT ccm.id, ccm.name, ccm.email, c.committee_name, ccm.chair, ccm.vice_chair,
       ccm.committee_secretary, ccm.delegation_secretary, ccm.committee_member
FROM country_committee_member ccm
JOIN committee c ON c.id = ccm.committee_id
WHERE lower(c.committee_name) LIKE '%head of delegation%'
ORDER BY ccm.id DESC
LIMIT 10;

SELECT 'subcommittee row should be gone' AS check_name;
SELECT id, name, email, position_in_ear FROM csub_committee_members WHERE id = 18;
