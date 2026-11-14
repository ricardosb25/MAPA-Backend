DO $presentation$
DECLARE
    teacher_id_value bigint;
    motor_id_value bigint;
    demo_class record;
    demo_challenge record;
    class_id_value bigint;
    challenge_id_value bigint;
    student_id_value bigint;
    test_id_value bigint;
    student_ids bigint[];
    student_number integer;
    attempt_number integer;
    attempt_count integer;
    power_value numeric;
    goal_reached boolean;
    tested_time timestamptz;
    audit_action_check text;
BEGIN
    SELECT pg_get_expr(conbin, conrelid) INTO audit_action_check
    FROM pg_constraint
    WHERE conrelid = 'user_audit_logs'::regclass AND conname = 'user_audit_logs_action_check';

    IF audit_action_check IS NOT NULL AND strpos(audit_action_check, 'PRESENTATION_MODE_ENABLED') = 0 THEN
        ALTER TABLE user_audit_logs DROP CONSTRAINT user_audit_logs_action_check;
        EXECUTE format(
            'ALTER TABLE user_audit_logs ADD CONSTRAINT user_audit_logs_action_check CHECK ((%s) OR action IN (%L, %L))',
            audit_action_check, 'PRESENTATION_MODE_ENABLED', 'PRESENTATION_MODE_DISABLED'
        );
    END IF;

    SELECT record_id INTO STRICT teacher_id_value FROM presentation_data WHERE record_type = 'TEACHER';
    SELECT id INTO STRICT motor_id_value FROM motors
        ORDER BY (manufacturer = 'HONDA' AND name = 'K20A2 2.0 VTEC') DESC, id
        LIMIT 1 FOR SHARE;

    FOR demo_class IN
        SELECT * FROM (VALUES
            ('MORNING', 'Manhã', 8, 6, 1),
            ('AFTERNOON', 'Tarde', 6, 3, 2),
            ('EVENING', 'Noite', 4, 1, 1)
        ) AS classes(shift, label, students, completed, in_progress)
    LOOP
        INSERT INTO school_classes (name, code, shift, teacher_id, created_at)
        VALUES ('[Demo] Calibração — ' || demo_class.label, upper(substr(gen_random_uuid()::text, 1, 8)),
                demo_class.shift, teacher_id_value, CURRENT_TIMESTAMP - interval '14 days')
        RETURNING id INTO class_id_value;
        INSERT INTO presentation_data (record_type, record_id) VALUES ('CLASS', class_id_value);

        student_ids := ARRAY[]::bigint[];
        FOR student_number IN 1..demo_class.students LOOP
            INSERT INTO users (full_name, email, password_hash, role, active, credentials_version,
                               row_version, created_at, updated_at)
            VALUES (format('Aluno Demo %s %s', demo_class.label, student_number),
                    format('mapa-demo-%s@example.invalid', gen_random_uuid()), 'DEMO_LOGIN_DISABLED',
                    'STUDENT', false, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            RETURNING id INTO student_id_value;
            INSERT INTO presentation_data (record_type, record_id) VALUES ('STUDENT', student_id_value);
            INSERT INTO class_enrollments (class_id, student_id, joined_at)
            VALUES (class_id_value, student_id_value, CURRENT_TIMESTAMP - interval '12 days');
            student_ids := array_append(student_ids, student_id_value);
        END LOOP;

        FOR demo_challenge IN
            SELECT * FROM (VALUES
                (1, '[Demo] Primeira calibração', 'EASY', 100),
                (2, '[Demo] Ganho de potência', 'MEDIUM', 125),
                (3, '[Demo] Ajuste avançado', 'HARD', 145)
            ) AS challenges(sequence, title, difficulty, goal)
        LOOP
            INSERT INTO challenges (title, description, difficulty, motor_id, class_id, teacher_id,
                                    goal_power_cv, required_engine_status, created_at)
            VALUES (demo_challenge.title, format('Atingir %s cv com motor íntegro.', demo_challenge.goal),
                    demo_challenge.difficulty, motor_id_value, class_id_value, teacher_id_value,
                    demo_challenge.goal, 'INTEGRO', CURRENT_TIMESTAMP - interval '10 days')
            RETURNING id INTO challenge_id_value;
            INSERT INTO presentation_data (record_type, record_id) VALUES ('CHALLENGE', challenge_id_value);

            FOR student_number IN 1..(demo_class.completed + demo_class.in_progress) LOOP
                student_id_value := student_ids[student_number];
                goal_reached := student_number <= demo_class.completed;
                attempt_count := 1 + mod(student_number + demo_challenge.sequence, 4);

                FOR attempt_number IN 1..attempt_count LOOP
                    power_value := CASE WHEN goal_reached AND attempt_number = attempt_count
                        THEN demo_challenge.goal + 3 ELSE demo_challenge.goal - 16 + attempt_number END;
                    tested_time := CURRENT_TIMESTAMP - (2 + attempt_count - attempt_number) * interval '1 day';
                    INSERT INTO dynamometer_tests (user_id, motor_id, max_power, max_torque, average_afr,
                                                   engine_status, tested_at)
                    VALUES (student_id_value, motor_id_value, power_value, power_value * 1.25, 13.2,
                            'INTEGRO', tested_time)
                    RETURNING id INTO test_id_value;
                    INSERT INTO challenge_attempts (challenge_id, student_id, dynamometer_test_id, motor_id,
                                                    achieved_value, engine_status, goal_met, created_at)
                    VALUES (challenge_id_value, student_id_value, test_id_value, motor_id_value, power_value,
                            'INTEGRO', goal_reached AND attempt_number = attempt_count, tested_time);
                END LOOP;

                INSERT INTO challenge_participations (challenge_id, student_id, active, best_value,
                                                      completed, completed_at, created_at)
                VALUES (challenge_id_value, student_id_value, NOT goal_reached, power_value, goal_reached,
                        CASE WHEN goal_reached THEN tested_time END, CURRENT_TIMESTAMP - interval '7 days');
                INSERT INTO challenge_progress (challenge_id, student_id, selected, attempts, completed,
                                                last_peak_power, last_engine_status, completed_at)
                VALUES (challenge_id_value, student_id_value, false, attempt_count, goal_reached,
                        power_value, 'INTEGRO', CASE WHEN goal_reached THEN tested_time END);
            END LOOP;
        END LOOP;
    END LOOP;
END;
$presentation$;
