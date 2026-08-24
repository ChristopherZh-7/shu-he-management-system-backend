package cn.shuhe.system.module.project.dal.dataobject;

import cn.shuhe.system.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@TableName("project_round_report_artifact")
@KeySequence("project_round_report_artifact_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectRoundReportArtifactDO extends BaseDO {
    @TableId
    private Long id;
    private Long roundId;
    private String reportType;
    private Integer versionNo;
    private String templateCode;
    private String fileName;
    private String fileUrl;
    private String fileHash;
    private String status;
    private Long createdBy;
    private Long reviewedBy;
    private LocalDateTime reviewedAt;
    private String reviewComment;
    private Long deliveredBy;
    private LocalDateTime deliveredAt;
    private String receiver;
}
