package cn.shuhe.system.module.infra.service.file;

import cn.shuhe.system.module.infra.controller.admin.file.vo.file.FileCreateReqVO;
import cn.shuhe.system.module.infra.controller.admin.file.vo.file.FileUploadReqVO;
import cn.shuhe.system.module.infra.framework.file.core.utils.FileTypeUtils;
import org.junit.jupiter.api.Test;

import static cn.shuhe.system.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.shuhe.system.module.infra.enums.ErrorCodeConstants.FILE_DIRECT_UPLOAD_DISABLED;
import static cn.shuhe.system.module.infra.enums.ErrorCodeConstants.FILE_UPLOAD_UNSUPPORTED_TYPE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileUploadSecurityTest {

    @Test
    void acceptsSupportedContentAndOptionalDirectory() {
        byte[] content = "Quarterly report contents".getBytes(UTF_8);
        assertEquals("text/plain", FileTypeUtils.validateUpload(content, "report.txt"));
        assertTrue(FileUploadReqVO.isDirectoryValid(null));
        assertTrue(FileUploadReqVO.isDirectoryValid("project/reports"));
    }

    @Test
    void rejectsContentThatDoesNotMatchTheFileName() {
        byte[] content = "Quarterly report contents".getBytes(UTF_8);
        assertServiceException(() -> FileTypeUtils.validateUpload(content, "report.png"),
                FILE_UPLOAD_UNSUPPORTED_TYPE);
    }

    @Test
    void rejectsDirectUploadAndItsMetadataByDefault() {
        FileServiceImpl service = new FileServiceImpl();
        assertServiceException(() -> service.presignPutUrl("report.txt", null), FILE_DIRECT_UPLOAD_DISABLED);
        assertServiceException(() -> service.createFile(new FileCreateReqVO()), FILE_DIRECT_UPLOAD_DISABLED);
    }
}
