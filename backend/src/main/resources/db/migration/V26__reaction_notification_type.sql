-- Reaction push notifications: allow the new notification type. Additive; existing rows are unchanged.
ALTER TABLE notifications DROP CONSTRAINT chk_notifications_type;
ALTER TABLE notifications ADD CONSTRAINT chk_notifications_type
    CHECK (type IN ('DEVOTION_OPEN', 'YOUR_TURN', 'SUMMARY_POSTED', 'REFLECTION_POSTED', 'ANSWER_REACTED'));
