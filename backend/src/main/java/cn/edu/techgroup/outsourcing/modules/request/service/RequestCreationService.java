package cn.edu.techgroup.outsourcing.modules.request.service;

import cn.edu.techgroup.outsourcing.common.error.BusinessException;
import cn.edu.techgroup.outsourcing.common.error.ErrorCode;
import cn.edu.techgroup.outsourcing.modules.request.dto.CreateRequestCommand;
import cn.edu.techgroup.outsourcing.modules.request.dto.SaveDraftCommand;
import cn.edu.techgroup.outsourcing.modules.request.mapper.RequestCreationReceiptMapper;
import cn.edu.techgroup.outsourcing.modules.request.mapper.RequestCreationReceiptMapper.Receipt;
import cn.edu.techgroup.outsourcing.modules.request.vo.CreatedRequestVO;
import cn.edu.techgroup.outsourcing.modules.user.enums.UserRole;
import cn.edu.techgroup.outsourcing.security.LoginUser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RequestCreationService {
    private static final Pattern KEY_PATTERN = Pattern.compile(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
    private final RequestCreationReceiptMapper receipts;
    private final RequestService requests;
    private final RequesterRequestLifecycleService lifecycle;
    private final ObjectMapper json;

    public RequestCreationService(RequestCreationReceiptMapper receipts, RequestService requests,
            RequesterRequestLifecycleService lifecycle, ObjectMapper json) {
        this.receipts = receipts;
        this.requests = requests;
        this.lifecycle = lifecycle;
        this.json = json;
    }

    @Transactional(rollbackFor = Exception.class)
    public CreatedRequestVO create(CreateRequestCommand command, String key, LoginUser operator) {
        return execute(command, key, operator, "SUBMIT",
                () -> requests.createAndSubmit(command, operator));
    }

    @Transactional(rollbackFor = Exception.class)
    public CreatedRequestVO createDraft(SaveDraftCommand command, String key, LoginUser operator) {
        return execute(command, key, operator, "DRAFT",
                () -> lifecycle.createDraft(command, operator));
    }

    private CreatedRequestVO execute(Object command, String key, LoginUser operator,
            String operation, Supplier<CreatedRequestVO> create) {
        if (operator.role() != UserRole.REQUESTER && operator.role() != UserRole.ADMIN) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        // Existing API clients remain compatible; only keyed calls promise safe retries.
        if (key == null) return create.get();
        if (!KEY_PATTERN.matcher(key).matches()) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "Idempotency-Key 必须为标准 UUID");
        }
        String normalizedKey = key.toLowerCase(Locale.ROOT);
        try {
            String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(json.writeValueAsBytes(command)));
            receipts.reserve(new Receipt(operator.id(), normalizedKey, operation, hash, null));
            Receipt receipt = receipts.lock(operator.id(), normalizedKey);
            if (receipt == null) throw new BusinessException(ErrorCode.INTERNAL_ERROR);
            if (!operation.equals(receipt.operation()) || !hash.equals(receipt.payloadHash())) {
                throw new BusinessException(ErrorCode.IDEMPOTENCY_KEY_CONFLICT);
            }
            if (receipt.resultJson() != null) {
                return json.readValue(receipt.resultJson(), CreatedRequestVO.class);
            }
            // Both delegated services join this transaction: failed business writes leave no receipt.
            CreatedRequestVO result = create.get();
            if (receipts.complete(new Receipt(operator.id(), normalizedKey, operation, hash,
                    json.writeValueAsString(result))) != 1) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR);
            }
            return result;
        } catch (JsonProcessingException | NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Unable to encode request creation receipt", exception);
        }
    }
}
