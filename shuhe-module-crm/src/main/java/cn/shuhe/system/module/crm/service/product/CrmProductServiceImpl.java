package cn.shuhe.system.module.crm.service.product;

import cn.hutool.core.collection.CollUtil;
import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.framework.common.util.object.BeanUtils;
import cn.shuhe.system.module.crm.controller.admin.product.vo.product.CrmProductPageReqVO;
import cn.shuhe.system.module.crm.controller.admin.product.vo.product.CrmProductSaveReqVO;
import cn.shuhe.system.module.crm.dal.dataobject.product.CrmProductDO;
import cn.shuhe.system.module.crm.dal.mysql.product.CrmProductMapper;
import cn.shuhe.system.module.crm.enums.common.CrmBizTypeEnum;
import cn.shuhe.system.module.crm.enums.permission.CrmPermissionLevelEnum;
import cn.shuhe.system.module.crm.enums.product.CrmProductStatusEnum;
import cn.shuhe.system.module.crm.framework.permission.core.annotations.CrmPermission;
import cn.shuhe.system.module.crm.service.permission.CrmPermissionService;
import cn.shuhe.system.module.crm.service.permission.bo.CrmPermissionCreateReqBO;
import cn.shuhe.system.module.system.api.user.AdminUserApi;
import com.mzt.logapi.context.LogRecordContext;
import com.mzt.logapi.service.impl.DiffParseFunction;
import com.mzt.logapi.starter.annotation.LogRecord;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static cn.shuhe.system.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.shuhe.system.framework.common.util.collection.CollectionUtils.convertMap;
import static cn.shuhe.system.module.crm.enums.ErrorCodeConstants.*;
import static cn.shuhe.system.module.crm.enums.LogRecordConstants.*;


/**
 * CRM 产品 Service 实现类
 *
 * @author ZanGe丶
 */
@Service
@Validated
public class CrmProductServiceImpl implements CrmProductService {

    @Resource(name = "crmProductMapper")
    private CrmProductMapper productMapper;

    @Resource
    private CrmPermissionService permissionService;

    @Resource
    private AdminUserApi adminUserApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = CRM_PRODUCT_TYPE, subType = CRM_PRODUCT_CREATE_SUB_TYPE, bizNo = "{{#productId}}",
            success = CRM_PRODUCT_CREATE_SUCCESS)
    public Long createProduct(CrmProductSaveReqVO createReqVO) {
        // 1. 校验（负责人可选，有则校验）
        if (createReqVO.getOwnerUserId() != null) {
            adminUserApi.validateUserList(Collections.singleton(createReqVO.getOwnerUserId()));
        }

        // 2. 插入产品（编码、单位、价格、分类已移除，仅保留名称、状态、描述、负责人）
        CrmProductDO product = BeanUtils.toBean(createReqVO, CrmProductDO.class);
        productMapper.insert(product);

        // 3. 插入数据权限（有负责人时）
        if (product.getOwnerUserId() != null) {
            permissionService.createPermission(new CrmPermissionCreateReqBO().setUserId(product.getOwnerUserId())
                    .setBizType(CrmBizTypeEnum.CRM_PRODUCT.getType()).setBizId(product.getId())
                    .setLevel(CrmPermissionLevelEnum.OWNER.getLevel()));
        }

        // 4. 记录操作日志上下文
        LogRecordContext.putVariable("productId", product.getId());
        return product.getId();
    }

    @Override
    @LogRecord(type = CRM_PRODUCT_TYPE, subType = CRM_PRODUCT_UPDATE_SUB_TYPE, bizNo = "{{#updateReqVO.id}}",
            success = CRM_PRODUCT_UPDATE_SUCCESS)
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_PRODUCT, bizId = "#updateReqVO.id", level = CrmPermissionLevelEnum.WRITE)
    public void updateProduct(CrmProductSaveReqVO updateReqVO) {
        // 1. 校验存在
        updateReqVO.setOwnerUserId(null); // 不修改负责人
        CrmProductDO crmProductDO = validateProductExists(updateReqVO.getId());

        // 2. 更新产品（仅更新名称、状态、描述，保留编码/单位/价格/分类等已废弃字段的旧值）
        CrmProductDO updateObj = BeanUtils.toBean(updateReqVO, CrmProductDO.class);
        updateObj.setNo(crmProductDO.getNo());
        updateObj.setUnit(crmProductDO.getUnit());
        updateObj.setPrice(crmProductDO.getPrice());
        updateObj.setCategoryId(crmProductDO.getCategoryId());
        productMapper.updateById(updateObj);

        // 3. 记录操作日志上下文
        LogRecordContext.putVariable(DiffParseFunction.OLD_OBJECT, BeanUtils.toBean(crmProductDO, CrmProductSaveReqVO.class));
    }

    private CrmProductDO validateProductExists(Long id) {
        CrmProductDO product = productMapper.selectById(id);
        if (product == null) {
            throw exception(PRODUCT_NOT_EXISTS);
        }
        return product;
    }

    @Override
    @LogRecord(type = CRM_PRODUCT_TYPE, subType = CRM_PRODUCT_DELETE_SUB_TYPE, bizNo = "{{#id}}",
            success = CRM_PRODUCT_DELETE_SUCCESS)
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_PRODUCT, bizId = "#id", level = CrmPermissionLevelEnum.OWNER)
    public void deleteProduct(Long id) {
        // 校验存在
        validateProductExists(id);
        // 删除
        productMapper.deleteById(id);
    }

    @Override
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_PRODUCT, bizId = "#id", level = CrmPermissionLevelEnum.READ)
    public CrmProductDO getProduct(Long id) {
        return productMapper.selectById(id);
    }

    @Override
    public PageResult<CrmProductDO> getProductPage(CrmProductPageReqVO pageReqVO) {
        return productMapper.selectPage(pageReqVO);
    }

    @Override
    public Long getProductByCategoryId(Long categoryId) {
        return productMapper.selectCountByCategoryId(categoryId);
    }

    @Override
    public List<CrmProductDO> getProductListByStatus(Integer status) {
        return productMapper.selectListByStatus(status);
    }

    @Override
    public List<CrmProductDO> validProductList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        List<CrmProductDO> list = productMapper.selectByIds(ids);
        Map<Long, CrmProductDO> productMap = convertMap(list, CrmProductDO::getId);
        for (Long id : ids) {
            CrmProductDO product = productMap.get(id);
            if (productMap.get(id) == null) {
                throw exception(PRODUCT_NOT_EXISTS);
            }
            if (CrmProductStatusEnum.isDisable(product.getStatus())) {
                throw exception(PRODUCT_NOT_ENABLE, product.getName());
            }
        }
        return list;
    }

    @Override
    public List<CrmProductDO> getProductList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return productMapper.selectByIds(ids);
    }

}
