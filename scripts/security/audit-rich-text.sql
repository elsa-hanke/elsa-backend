-- Read-only triage of ORIGINAL HTML; this script is never executed by the application.
-- Results are candidates for manual review, not proof of exploitation.
-- Formatting such as style= is often legitimate. Encoded/obfuscated payloads may be missed.
-- Preserve original records and relevant logs before considering any separate cleanup.
WITH rich_text AS (
    SELECT 'ilmoitus' AS source, id, 'teksti' AS field, teksti AS html FROM ilmoitus
    UNION ALL
    SELECT 'arvioitava_kokonaisuus', id, 'kuvaus', kuvaus FROM arvioitava_kokonaisuus
    UNION ALL
    SELECT 'arvioitava_kokonaisuus', id, 'kuvaus_sv', kuvaus_sv FROM arvioitava_kokonaisuus
    UNION ALL
    SELECT 'arviointityokalu', id, 'ohjeteksti', ohjeteksti FROM arviointityokalu
)
SELECT source, id, field, html
FROM rich_text
WHERE html ~* '(<[[:space:]]*(script|iframe|svg|math|object|embed)|on[a-z]+[[:space:]]*=|javascript[[:space:]]*:|style[[:space:]]*=)'
ORDER BY source, id, field;
