package cn.itcast.ssh.utils;

import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.interceptor.Interceptor;
import com.sun.org.apache.regexp.internal.recompile;

import cn.itcast.ssh.domain.Employee;


/**
 * 登录验证拦截器
 *
 */
@SuppressWarnings("serial")
public class LoginInteceptor implements Interceptor {

	@Override
	public void destroy() {
		// TODO Auto-generated method stub

	}

	@Override
	public void init() {
		// TODO Auto-generated method stub

	}

	/**
	 * 访问action之前 执行intercept方法
	 */
	@Override
	public String intercept(ActionInvocation invocation) throws Exception {
		//获取当前访问的url
		String actionName = invocation.getProxy().getActionName();
		if (!actionName.equals("loginAction_login")) {
			Employee employee = SessionContext.get();
			if (employee==null) {
				return "login";
			}
		}
		//访问Action类中方法
		return invocation.invoke();
		
	}

}
