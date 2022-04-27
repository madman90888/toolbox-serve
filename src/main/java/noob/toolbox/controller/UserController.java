package noob.toolbox.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import noob.toolbox.domain.pojo.BusinessCode;
import noob.toolbox.domain.pojo.ResultData;
import noob.toolbox.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotBlank;
import java.security.Principal;

@Validated
@Tag(name = "用户模块")
@RestController
public class UserController {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Operation(summary = "获取当前用户", description = "返回当前的用户名")
    @GetMapping("/currUser")
    public ResultData<String> currentUserName(Principal principal) {
        if (principal == null) {
            return ResultData.error(BusinessCode.FAILED.getCode(), "用户未登录");
        }
        return ResultData.message(principal.getName());
    }

    @Operation(summary = "修改密码")
    @Parameter(name = "oldPass", description = "旧密码", required = true, in = ParameterIn.QUERY)
    @Parameter(name = "newPass", description = "新密码", required = true, in = ParameterIn.QUERY)
    @PostMapping("user/pass")
    public ResultData<String> updatePassword(
            @NotBlank(message = "旧密码不能为空")
            String oldPass,
            @NotBlank(message = "新密码不能为空")
            String newPass, Principal principal) {
        if (principal == null) {
            return ResultData.error(BusinessCode.NOT_USER.getCode(), "用户未登录");
        }
        String password = (String) redisTemplate.opsForValue().get(UserService.USER_KEY + principal.getName());
        if (oldPass.equals(password)) {
            redisTemplate.opsForValue().set(UserService.USER_KEY + principal.getName(), newPass);
            return ResultData.success("密码修改完成");
        }
        return ResultData.error("旧密码错误");
    }
}
