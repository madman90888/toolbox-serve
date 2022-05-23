package noob.toolbox.config;

import noob.toolbox.util.WebUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.session.SessionInformationExpiredEvent;
import org.springframework.util.ObjectUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // 开启跨域访问、取消跨站请求伪造防护
        http.cors().and().csrf().disable()
                .authorizeRequests()    // 设置路径访问权限
                // 公共方法，所有人都能访问
                .antMatchers(
                        "/s/**", "/code",
                        "/msg/**",  "/p/**",
                        "/", "/admin", "/currUser", "/favicon.ico",
                        "/css/**", "/js/**", "/img/**",
                        "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                // 匿名访问，未登录的时候
                .antMatchers("/login").anonymous()
                //注销接口需要认证才能访问
                //.antMatchers("/logout").authenticated()
                // 其余所有路径都需要 鉴权认证
                .anyRequest().authenticated();

        /** 返回JSON配置 **/
        http
                // 未登录时提示
                .httpBasic()
                .authenticationEntryPoint(this::notUser)
                // 指定支持基于表单的身份验证
                .and()
                .formLogin()
                // 配置登录验证路径
                .loginProcessingUrl("/login")
                // 登录成功/失败处理
                .successHandler(this::onAuthenticationSuccess)
                .failureHandler(this::onAuthenticationFailure)
                .and()
                // 登出配置
                .logout()
                .logoutUrl("/logout")
                .logoutSuccessHandler(this::onLogoutSuccess)
                // 删除Cookie
                .deleteCookies("JSESSIONID")
                // 异常处理器
                .and()
                .exceptionHandling()
                // 权限不足
                .accessDeniedHandler(this::accessDeniedHandler)
                // 用户未登录，不跳转，而返回未登录提示
                .authenticationEntryPoint(this::notUser)
                // Session处理
                .and()
                .sessionManagement()
                // session超时提示，配置了导致放行资源也被拦截
                //.invalidSessionStrategy(this::onInvalidSessionDetected)
                // 最大登录数
                .maximumSessions(1)
                // 是否保留已经登录的用户；为true，新用户无法登录；为 false，旧用户被踢出
                .maxSessionsPreventsLogin(false)
                // 当达到最大值时，旧用户被踢出后的操作
                .expiredSessionStrategy(this::onExpiredSessionDetected);
    }

    /**
     * 返回用户未登录提示的
     * 实现AuthenticationEntryPoint接口,的commence方法
     */
    void notUser(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        final String accept = request.getHeader("accept");
        if (!ObjectUtils.isEmpty(accept) && accept.indexOf("html") > -1) {
            response.sendRedirect("/");
        }else {
            WebUtils.writeString(401, "未登录", response);
        }
    }

    /**
     * 登录成功提示
     * AuthenticationSuccessHandler接口
     */
    void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        WebUtils.writeString(200, "登录成功", response);
    }

    /**
     * 登录失败提示
     * AuthenticationFailureHandler接口
     */
    void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) {
        if (exception instanceof UsernameNotFoundException || exception instanceof BadCredentialsException) {
            WebUtils.writeString(4010, "用户名或密码错误", response);
        } else if (exception instanceof DisabledException) {
            WebUtils.writeString(4010, "账户被禁用", response);
        } else {
            WebUtils.writeString(4010, "登录验证出现异常", response);
        }
    }

    /**
     * 权限不足提示
     */
    void accessDeniedHandler(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) {
        WebUtils.writeString(403, "权限不足", response);
    }

    /**
     * 退出登录提示
     */
    void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        WebUtils.writeString(200, "退出成功", response);
    }

    /**
     * session过期
     */
    void onInvalidSessionDetected(HttpServletRequest request, HttpServletResponse response) {
        WebUtils.writeString(401, "Session 已过期，请重新登录", response);
    }

    /**
     * session登录达到上限，旧用户处理方式
     */
    void onExpiredSessionDetected(SessionInformationExpiredEvent event) {
        final HttpServletResponse response = event.getResponse();
        WebUtils.writeString(401, "账号在其他地方登录，您被迫下线。" + event.getSessionInformation().getLastRequest(), response);
    }
}