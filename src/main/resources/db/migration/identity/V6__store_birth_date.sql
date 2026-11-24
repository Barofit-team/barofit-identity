-- 기존 출생연도로 생월일을 추정하지 않는다. 다음 입력·수정부터 정확한 날짜를 저장한다.
ALTER TABLE identity.user_profiles ADD COLUMN birth_date date;
ALTER TABLE identity.user_profiles ADD CONSTRAINT ck_user_profiles_birth_date_year
    CHECK (birth_date IS NULL OR EXTRACT(YEAR FROM birth_date) = birth_year);
