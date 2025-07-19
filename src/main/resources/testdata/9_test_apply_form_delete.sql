DELETE FROM applyform_grades WHERE applyform_id IN (SELECT id FROM applyforms WHERE club_id = 'club-001');
DELETE FROM applyforms WHERE club_id = 'club-001';