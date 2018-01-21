package cn.itcast.ssh.utils;

import javax.servlet.ServletContext;

import org.activiti.engine.delegate.DelegateTask;
import org.activiti.engine.delegate.TaskListener;
import org.apache.struts2.ServletActionContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import cn.itcast.ssh.domain.Employee;
import cn.itcast.ssh.service.IEmployeeService;

/**
 * 员工经理任务分配
 *
 */
@SuppressWarnings("serial")
public class ManagerTaskHandler implements TaskListener {

	@Override
	public void notify(DelegateTask delegateTask) {
		//懒加载异常
		Employee employee = SessionContext.get();
//		delegateTask.setAssignee(employee.getManager().getName());
		//重新查询当前用户 在获取领导信息
		String employeeName = employee.getName();
		//从容器中获取实体对象
		WebApplicationContext webApplicationContext = WebApplicationContextUtils.getWebApplicationContext(ServletActionContext.getServletContext());
		IEmployeeService employeeService = (IEmployeeService)webApplicationContext.getBean("employeeService");
		Employee currentEmployee = employeeService.findEmployeeByName(employeeName);
		delegateTask.setAssignee(currentEmployee.getManager().getName());
	}

}
