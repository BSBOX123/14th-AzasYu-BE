-- 회의 참여 코드. 프로젝트 구성원이 이 코드로 회의에 합류한다.
ALTER TABLE meetings ADD COLUMN join_code VARCHAR(8);

-- 기존 회의에도 값을 채운다. id 기반이라 충돌하지 않는다.
-- 새로 만드는 회의는 애플리케이션이 무작위로 생성한다.
UPDATE meetings SET join_code = CONCAT('MTG', LPAD(id, 5, '0')) WHERE join_code IS NULL;

ALTER TABLE meetings MODIFY COLUMN join_code VARCHAR(8) NOT NULL;
ALTER TABLE meetings ADD CONSTRAINT uk_meetings_join_code UNIQUE (join_code);
