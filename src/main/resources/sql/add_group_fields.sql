-- Add new fields to groups table
ALTER TABLE groups 
ADD COLUMN course_code VARCHAR(50),
ADD COLUMN faculty VARCHAR(255),
ADD COLUMN lecturer VARCHAR(255);

-- Update existing groups with sample data (optional)
-- UPDATE groups SET 
--     course_code = 'ECO3052',
--     faculty = 'Kinh tế & Quản trị Kinh doanh',
--     lecturer = 'Dr. Nguyễn Văn A'
-- WHERE id = 1;


