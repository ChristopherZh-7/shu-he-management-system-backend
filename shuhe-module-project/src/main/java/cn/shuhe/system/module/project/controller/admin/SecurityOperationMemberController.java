package cn.shuhe.system.module.project.controller.admin;

import cn.shuhe.system.framework.common.pojo.CommonResult;
import cn.shuhe.system.framework.common.util.object.BeanUtils;
import cn.shuhe.system.module.project.controller.admin.vo.SecurityOperationMemberRespVO;
import cn.shuhe.system.module.project.controller.admin.vo.SecurityOperationMemberSaveReqVO;
import cn.shuhe.system.module.project.dal.dataobject.SecurityOperationMemberDO;
import cn.shuhe.system.module.project.dal.mysql.SecurityOperationMemberMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static cn.shuhe.system.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.shuhe.system.framework.common.pojo.CommonResult.success;
import static cn.shuhe.system.module.project.enums.ErrorCodeConstants.SECURITY_OPERATION_MEMBER_NOT_EXISTS;

/**
 * 安全运营驻场人员 Controller
 */
@Tag(name = "管理后台 - 安全运营驻场人员")
@RestController
@RequestMapping("/project/security-operation-member")
@Validated
@Slf4j
public class SecurityOperationMemberController {

    @Resource
    private SecurityOperationMemberMapper memberMapper;

    @PostMapping("/create")
    @Operation(summary = "创建驻场人员")
    @PreAuthorize("@ss.hasPermission('project:security-operation:update')")
    public CommonResult<Long> createMember(@Valid @RequestBody SecurityOperationMemberSaveReqVO reqVO) {
        // 创建时必须有用户ID
        if (reqVO.getUserId() == null) {
            return CommonResult.error(400, "用户不能为空");
        }
        SecurityOperationMemberDO member = BeanUtils.toBean(reqVO, SecurityOperationMemberDO.class);
        // 默认状态：在岗
        if (member.getStatus() == null) {
            member.setStatus(SecurityOperationMemberDO.STATUS_ACTIVE);
        }
        // 如果没传人员类型，默认为驻场人员
        if (member.getMemberType() == null) {
            member.setMemberType(SecurityOperationMemberDO.MEMBER_TYPE_ONSITE);
        }
        memberMapper.insert(member);
        return success(member.getId());
    }

    @PutMapping("/update")
    @Operation(summary = "更新驻场人员")
    @PreAuthorize("@ss.hasPermission('project:security-operation:update')")
    public CommonResult<Boolean> updateMember(@Valid @RequestBody SecurityOperationMemberSaveReqVO reqVO) {
        // 校验记录是否存在
        if (reqVO.getId() == null) {
            return CommonResult.error(400, "ID不能为空");
        }
        SecurityOperationMemberDO existMember = memberMapper.selectById(reqVO.getId());
        if (existMember == null) {
            throw exception(SECURITY_OPERATION_MEMBER_NOT_EXISTS);
        }
        
        log.info("[updateMember][更新驻场人员，id={}，reqVO={}]", reqVO.getId(), reqVO);
        
        // 转换并更新
        SecurityOperationMemberDO member = BeanUtils.toBean(reqVO, SecurityOperationMemberDO.class);
        log.info("[updateMember][转换后 DO，id={}，startDate={}，endDate={}，status={}]", 
                member.getId(), member.getStartDate(), member.getEndDate(), member.getStatus());
        
        int updated = memberMapper.updateById(member);
        log.info("[updateMember][更新结果，affected rows={}]", updated);
        
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除驻场人员")
    @Parameter(name = "id", description = "人员ID", required = true)
    @PreAuthorize("@ss.hasPermission('project:security-operation:update')")
    public CommonResult<Boolean> deleteMember(@RequestParam("id") Long id) {
        memberMapper.deleteById(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取驻场人员详情")
    @Parameter(name = "id", description = "人员ID", required = true)
    @PreAuthorize("@ss.hasPermission('project:security-operation:query')")
    public CommonResult<SecurityOperationMemberRespVO> getMember(@RequestParam("id") Long id) {
        SecurityOperationMemberDO member = memberMapper.selectById(id);
        return success(BeanUtils.toBean(member, SecurityOperationMemberRespVO.class));
    }

    @GetMapping("/list-by-site")
    @Operation(summary = "获取驻场点的人员列表")
    @Parameter(name = "siteId", description = "驻场点ID", required = true)
    @PreAuthorize("@ss.hasPermission('project:security-operation:query')")
    public CommonResult<List<SecurityOperationMemberRespVO>> getListBySite(@RequestParam("siteId") Long siteId) {
        List<SecurityOperationMemberDO> members = memberMapper.selectListBySiteId(siteId);
        return success(BeanUtils.toBean(members, SecurityOperationMemberRespVO.class));
    }

    @GetMapping("/list-by-contract")
    @Operation(summary = "获取安全运营合同的人员列表")
    @Parameter(name = "soContractId", description = "安全运营合同ID", required = true)
    @PreAuthorize("@ss.hasPermission('project:security-operation:query')")
    public CommonResult<List<SecurityOperationMemberRespVO>> getListByContract(@RequestParam("soContractId") Long soContractId) {
        List<SecurityOperationMemberDO> members = memberMapper.selectListBySoContractId(soContractId);
        return success(BeanUtils.toBean(members, SecurityOperationMemberRespVO.class));
    }

}
