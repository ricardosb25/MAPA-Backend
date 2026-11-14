DO $presentation$
DECLARE
    class_ids bigint[];
    challenge_ids bigint[];
    student_ids bigint[];
BEGIN
    SELECT ARRAY(SELECT record_id FROM presentation_data WHERE record_type = 'CLASS') INTO class_ids;
    SELECT ARRAY(SELECT record_id FROM presentation_data WHERE record_type = 'STUDENT') INTO student_ids;
    SELECT ARRAY(
        SELECT id FROM challenges
        WHERE class_id = ANY(class_ids)
           OR id IN (SELECT record_id FROM presentation_data WHERE record_type = 'CHALLENGE')
    ) INTO challenge_ids;

    DELETE FROM challenge_attempts WHERE challenge_id = ANY(challenge_ids) OR student_id = ANY(student_ids);
    DELETE FROM challenge_progress WHERE challenge_id = ANY(challenge_ids) OR student_id = ANY(student_ids);
    DELETE FROM challenge_participations WHERE challenge_id = ANY(challenge_ids) OR student_id = ANY(student_ids);
    DELETE FROM challenges WHERE id = ANY(challenge_ids);
    DELETE FROM class_enrollments WHERE class_id = ANY(class_ids) OR student_id = ANY(student_ids);
    DELETE FROM school_classes WHERE id = ANY(class_ids);
    DELETE FROM dynamometer_tests WHERE user_id = ANY(student_ids);
    DELETE FROM ecu_map_tunings WHERE user_id = ANY(student_ids);
    DELETE FROM users WHERE id = ANY(student_ids);
    DELETE FROM presentation_data;
END;
$presentation$;
