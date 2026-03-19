UPDATE ACT_RE_PROCDEF p
INNER JOIN (SELECT MAX(VERSION_) AS max_ver FROM ACT_RE_PROCDEF WHERE KEY_ = 'unified_service_launch') t ON p.VERSION_ = t.max_ver
SET p.SUSPENSION_STATE_ = 1
WHERE p.KEY_ = 'unified_service_launch';
