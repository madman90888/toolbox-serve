package noob.toolbox.cloudflare;

import org.springframework.util.ObjectUtils;

import java.util.HashMap;
import java.util.Map;

public class FlareErrorCode {
    public static final Map<Integer, String> map = new HashMap<>();

    static {
        map.put(304, "未修改");
        map.put(400, "请求无效");
        map.put(401, "用户没有权限");
        map.put(403, "请求未通过身份验证");
        map.put(429, "客户限速");
        map.put(405, "提供的 HTTP 方法不正确");
        map.put(415, "响应不是有效的 JSON");

        map.put(1000, "用户无效或令牌无效");
        map.put(1001, "区域ID无效，请确认该域名是否存在");
        map.put(1002, "无效域");
        map.put(1003, "'jump_start' 必须是布尔值");
        map.put(1006, "区域无效或缺失");
        map.put(1008, "区域 ID 无效或缺失");
        map.put(1010, "已达到批量交易限制");
        map.put(1018, "无效的区域状态");
        map.put(1019, "区域已暂停");
        map.put(1020, "区域无效或缺失");
        map.put(1052, "发生错误并已记录。我们会及时解决这个问题。我们对不便表示抱歉");
        map.put(1061, "该域已存在");
        map.put(1068, "没有权限");
        map.put(1074, "找不到有效区域");
        map.put(1076, "服务器IP无效");
        map.put(1077, "找不到有效区域");
        map.put(1080, "与其中一个名称服务器发生冲突");
        map.put(1086, "无效属性");
        map.put(1088, "无效/缺少区域计划 ID");
        map.put(1089, "无效/缺少区域计划 ID");
        map.put(1096, "此操作不可用，因为您的区域已因可能违反服务条款而被停用");
        map.put(1098, "目前暂时限制将此 Web 资源添加到 Cloudflare。请稍后再试，或如有任何问题，请联系 Cloudflare 支持");
        map.put(1099, "我们无法将 <domain> 识别为注册域。请确保您提供的是根域而不是任何子域（例如，example.com，而不是 subdomain.example.com)");
        map.put(1100, "标记超过 1024 个字符的最大长度");
        map.put(1104, "不允许部分区域注册");
        map.put(1105, "目前暂时限制将此 Web 资源添加到 Cloudflare。请稍后再试，或如有任何问题，请联系 Cloudflare 支持");
        map.put(1111, "超过单个请求可清除的最大主机数量 30");
        map.put(1113, "无法按主机清除，已达到速率限制。如果您需要执行更多操作，请稍候");
        map.put(1115, "无效主机");
        map.put(6008, "请求参数无效");
        map.put(6007, "请求正文中的 JSON 格式错误");
        map.put(9109, "无效的访问令牌");
        map.put(1224, "您每小时只能执行一次此操作");
        map.put(10000, "授权错误");
        map.put(1007, "区域设置 always_use_https 的值无效");
        map.put(7003, "区域ID无效");
        map.put(7000, "找不到对应的资源");
        map.put(1004, "DNS 验证错误");
        map.put(9002, "DNS 记录类型无效");
        map.put(6003, "令牌无效");
        map.put(81057, "记录已经存在");
    }

    /**
     * 根据错误代码 获取错误提示
     * @param code   错误代码
     * @param defaultMsg
     * @return
     */
    public static String getMsg(Integer code, String defaultMsg) {
        if (ObjectUtils.isEmpty(code) || ObjectUtils.isEmpty(map.get(code))) {
            return defaultMsg;
        }
        return map.get(code);
    }
}
