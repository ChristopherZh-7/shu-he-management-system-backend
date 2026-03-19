-- =====================================================
-- 激活借调申请 BPM 流程 (unified_service_launch)
-- 若流程被挂起，前端会提示「借调申请流程未配置」
-- 兼容 act_re_procdef / ACT_RE_PROCDEF 两种表名（Linux 大小写敏感）
-- 日期: 2026-03-16
-- =====================================================

DROP PROCEDURE IF EXISTS _activate_unified_service_launch;
DELIMITER //
CREATE PROCEDURE _activate_unified_service_launch()
BEGIN
  DECLARE v_tbl VARCHAR(64);
  SELECT table_name INTO v_tbl FROM information_schema.tables
    WHERE table_schema = DATABASE() AND LOWER(table_name) = 'act_re_procdef' LIMIT 1;
  IF v_tbl IS NOT NULL THEN
    SET @sql = CONCAT(
      'UPDATE ', v_tbl, ' p INNER JOIN (SELECT MAX(VERSION_) AS max_ver FROM ', v_tbl,
      ' WHERE KEY_ = ''unified_service_launch'') t ON p.VERSION_ = t.max_ver SET p.SUSPENSION_STATE_ = 1 WHERE p.KEY_ = ''unified_service_launch'''
    );
    PREPARE stmt FROM @sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END //
DELIMITER ;
CALL _activate_unified_service_launch();
DROP PROCEDURE IF EXISTS _activate_unified_service_launch;
