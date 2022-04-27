package noob.toolbox.domain.pojo;

public enum BusinessCode {
    SUCCESS(200, "操作成功"),
    FAILED(400, "操作失败"),
    NOT_USER(401, "用户未登录"),
    NOT_AUTH_USER(403, "无权限"),
    ERROR(500, "服务器错误");

    /**自定义状态码**/
    private final int code;
    /**自定义描述**/
    private final String message;

    BusinessCode(int code, String message){
        this.code = code;
        this.message = message;
    }
    public int getCode() {
        return code;
    }
    public String getMessage() {
        return message;
    }
}