ALTER TABLE identity.user_profiles DROP CONSTRAINT ck_user_profiles_goal;
ALTER TABLE identity.user_profiles ADD CONSTRAINT ck_user_profiles_goal
    CHECK (goal IN ('WEIGHT_LOSS', 'STRENGTH', 'ENDURANCE', 'POSTURE', 'HABIT'));
