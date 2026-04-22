-- =============================================
-- 替换全部服务类型字典数据为统一的29项服务清单
-- 三个部门字典共用同一套服务类型
-- =============================================

-- 1. 软删除所有现有服务类型字典数据
UPDATE `system_dict_data`
   SET deleted = 1, update_time = NOW()
 WHERE dict_type IN ('project_service_type_security', 'project_service_type_operation', 'project_service_type_data')
   AND deleted = 0;

-- 2. 安全服务 (project_service_type_security)
INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(1,  '信息资产梳理',             'info_asset_inventory',             'project_service_type_security', 0, 'primary', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(2,  '漏洞扫描及加固服务',       'vuln_scan_and_hardening',          'project_service_type_security', 0, 'info',    '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(3,  '安全制度建设与修订',       'security_policy_building',         'project_service_type_security', 0, 'default', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(4,  '网络安全应急响应',         'cybersecurity_emergency_response', 'project_service_type_security', 0, 'danger',  '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(5,  '安全意识培训',             'security_awareness_training',      'project_service_type_security', 0, 'default', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(6,  '应急演练',                 'emergency_drill',                  'project_service_type_security', 0, 'warning', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(7,  '安全检查迎检',             'security_inspection_preparation',  'project_service_type_security', 0, 'success', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(8,  '基线核查',                 'baseline_check',                   'project_service_type_security', 0, 'info',    '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(9,  '新系统上线安全检测',       'pre_launch_security_check',        'project_service_type_security', 0, 'primary', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(10, '安全设备运维',             'security_equipment_ops',           'project_service_type_security', 0, 'default', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(11, '重要时期安全保障值守',     'major_period_security_duty',       'project_service_type_security', 0, 'danger',  '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(12, '等级保护测评咨询',         'level_protection_consulting',      'project_service_type_security', 0, 'success', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(13, '商用密码测评咨询',         'commercial_crypto_consulting',     'project_service_type_security', 0, 'warning', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(14, '渗透测试',                 'penetration_test',                 'project_service_type_security', 0, 'primary', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(15, '数据安全风险评估',         'data_security_risk_assessment',    'project_service_type_security', 0, 'warning', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(16, '个人信息安全影响评估',     'personal_info_impact_assessment',  'project_service_type_security', 0, 'danger',  '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(17, 'ICT供应链安全风险评估',    'ict_supply_chain_risk_assessment', 'project_service_type_security', 0, 'info',    '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(18, '信息安全风险评估',         'info_security_risk_assessment',    'project_service_type_security', 0, 'warning', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(19, '网络安全宣传',             'cybersecurity_publicity',          'project_service_type_security', 0, 'default', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(20, '代码审计服务',             'code_audit',                       'project_service_type_security', 0, 'info',    '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(21, '蓝队评估/攻防演练',       'blue_team_assessment',             'project_service_type_security', 0, 'primary', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(22, '安全咨询指导',             'security_consulting_guidance',     'project_service_type_security', 0, 'default', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(23, '威胁情报同步分享',         'threat_intelligence_sharing',      'project_service_type_security', 0, 'danger',  '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(24, '"两高一弱"专项检查',       'two_high_one_weak_check',          'project_service_type_security', 0, 'warning', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(25, '终端安全管理',             'terminal_security_management',     'project_service_type_security', 0, 'info',    '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(26, '互联网暴露面检测服务',     'internet_exposure_detection',      'project_service_type_security', 0, 'danger',  '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(27, '保密检查',                 'confidentiality_check',            'project_service_type_security', 0, 'info',    '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(28, '机房巡检',                 'server_room_inspection',           'project_service_type_security', 0, 'default', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(29, 'MSS托管运营服务',          'mss_managed_service',              'project_service_type_security', 0, 'primary', '', '', 'admin', NOW(), 'admin', NOW(), b'0');

-- 3. 安全运营 (project_service_type_operation)
INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(1,  '信息资产梳理',             'info_asset_inventory',             'project_service_type_operation', 0, 'primary', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(2,  '漏洞扫描及加固服务',       'vuln_scan_and_hardening',          'project_service_type_operation', 0, 'info',    '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(3,  '安全制度建设与修订',       'security_policy_building',         'project_service_type_operation', 0, 'default', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(4,  '网络安全应急响应',         'cybersecurity_emergency_response', 'project_service_type_operation', 0, 'danger',  '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(5,  '安全意识培训',             'security_awareness_training',      'project_service_type_operation', 0, 'default', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(6,  '应急演练',                 'emergency_drill',                  'project_service_type_operation', 0, 'warning', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(7,  '安全检查迎检',             'security_inspection_preparation',  'project_service_type_operation', 0, 'success', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(8,  '基线核查',                 'baseline_check',                   'project_service_type_operation', 0, 'info',    '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(9,  '新系统上线安全检测',       'pre_launch_security_check',        'project_service_type_operation', 0, 'primary', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(10, '安全设备运维',             'security_equipment_ops',           'project_service_type_operation', 0, 'default', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(11, '重要时期安全保障值守',     'major_period_security_duty',       'project_service_type_operation', 0, 'danger',  '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(12, '等级保护测评咨询',         'level_protection_consulting',      'project_service_type_operation', 0, 'success', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(13, '商用密码测评咨询',         'commercial_crypto_consulting',     'project_service_type_operation', 0, 'warning', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(14, '渗透测试',                 'penetration_test',                 'project_service_type_operation', 0, 'primary', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(15, '数据安全风险评估',         'data_security_risk_assessment',    'project_service_type_operation', 0, 'warning', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(16, '个人信息安全影响评估',     'personal_info_impact_assessment',  'project_service_type_operation', 0, 'danger',  '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(17, 'ICT供应链安全风险评估',    'ict_supply_chain_risk_assessment', 'project_service_type_operation', 0, 'info',    '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(18, '信息安全风险评估',         'info_security_risk_assessment',    'project_service_type_operation', 0, 'warning', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(19, '网络安全宣传',             'cybersecurity_publicity',          'project_service_type_operation', 0, 'default', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(20, '代码审计服务',             'code_audit',                       'project_service_type_operation', 0, 'info',    '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(21, '蓝队评估/攻防演练',       'blue_team_assessment',             'project_service_type_operation', 0, 'primary', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(22, '安全咨询指导',             'security_consulting_guidance',     'project_service_type_operation', 0, 'default', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(23, '威胁情报同步分享',         'threat_intelligence_sharing',      'project_service_type_operation', 0, 'danger',  '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(24, '"两高一弱"专项检查',       'two_high_one_weak_check',          'project_service_type_operation', 0, 'warning', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(25, '终端安全管理',             'terminal_security_management',     'project_service_type_operation', 0, 'info',    '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(26, '互联网暴露面检测服务',     'internet_exposure_detection',      'project_service_type_operation', 0, 'danger',  '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(27, '保密检查',                 'confidentiality_check',            'project_service_type_operation', 0, 'info',    '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(28, '机房巡检',                 'server_room_inspection',           'project_service_type_operation', 0, 'default', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(29, 'MSS托管运营服务',          'mss_managed_service',              'project_service_type_operation', 0, 'primary', '', '', 'admin', NOW(), 'admin', NOW(), b'0');

-- 4. 数据安全 (project_service_type_data)
INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(1,  '信息资产梳理',             'info_asset_inventory',             'project_service_type_data', 0, 'primary', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(2,  '漏洞扫描及加固服务',       'vuln_scan_and_hardening',          'project_service_type_data', 0, 'info',    '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(3,  '安全制度建设与修订',       'security_policy_building',         'project_service_type_data', 0, 'default', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(4,  '网络安全应急响应',         'cybersecurity_emergency_response', 'project_service_type_data', 0, 'danger',  '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(5,  '安全意识培训',             'security_awareness_training',      'project_service_type_data', 0, 'default', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(6,  '应急演练',                 'emergency_drill',                  'project_service_type_data', 0, 'warning', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(7,  '安全检查迎检',             'security_inspection_preparation',  'project_service_type_data', 0, 'success', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(8,  '基线核查',                 'baseline_check',                   'project_service_type_data', 0, 'info',    '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(9,  '新系统上线安全检测',       'pre_launch_security_check',        'project_service_type_data', 0, 'primary', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(10, '安全设备运维',             'security_equipment_ops',           'project_service_type_data', 0, 'default', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(11, '重要时期安全保障值守',     'major_period_security_duty',       'project_service_type_data', 0, 'danger',  '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(12, '等级保护测评咨询',         'level_protection_consulting',      'project_service_type_data', 0, 'success', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(13, '商用密码测评咨询',         'commercial_crypto_consulting',     'project_service_type_data', 0, 'warning', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(14, '渗透测试',                 'penetration_test',                 'project_service_type_data', 0, 'primary', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(15, '数据安全风险评估',         'data_security_risk_assessment',    'project_service_type_data', 0, 'warning', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(16, '个人信息安全影响评估',     'personal_info_impact_assessment',  'project_service_type_data', 0, 'danger',  '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(17, 'ICT供应链安全风险评估',    'ict_supply_chain_risk_assessment', 'project_service_type_data', 0, 'info',    '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(18, '信息安全风险评估',         'info_security_risk_assessment',    'project_service_type_data', 0, 'warning', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(19, '网络安全宣传',             'cybersecurity_publicity',          'project_service_type_data', 0, 'default', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(20, '代码审计服务',             'code_audit',                       'project_service_type_data', 0, 'info',    '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(21, '蓝队评估/攻防演练',       'blue_team_assessment',             'project_service_type_data', 0, 'primary', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(22, '安全咨询指导',             'security_consulting_guidance',     'project_service_type_data', 0, 'default', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(23, '威胁情报同步分享',         'threat_intelligence_sharing',      'project_service_type_data', 0, 'danger',  '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(24, '"两高一弱"专项检查',       'two_high_one_weak_check',          'project_service_type_data', 0, 'warning', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(25, '终端安全管理',             'terminal_security_management',     'project_service_type_data', 0, 'info',    '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(26, '互联网暴露面检测服务',     'internet_exposure_detection',      'project_service_type_data', 0, 'danger',  '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(27, '保密检查',                 'confidentiality_check',            'project_service_type_data', 0, 'info',    '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(28, '机房巡检',                 'server_room_inspection',           'project_service_type_data', 0, 'default', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(29, 'MSS托管运营服务',          'mss_managed_service',              'project_service_type_data', 0, 'primary', '', '', 'admin', NOW(), 'admin', NOW(), b'0');
