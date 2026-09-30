package cn.shuhe.system.module.project.dal.dataobject;

import cn.shuhe.system.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("project_golish_job")
public class GolishJobDO extends BaseDO {
    @TableId
    private Long id;
    private Long ticketId;
    private Long roundId;
    private Long sourceJobId;
    private String kind;
    private String requestKey;
    private String requestJson;
    private String remoteId;
    private String state;
    private String error;
    private String resultJson;
    private String reportFile;
    private Boolean reportReady;
    private Boolean imported;
    private LocalDateTime nextPoll;
    private String leaseToken;
    private LocalDateTime leaseUntil;
}
