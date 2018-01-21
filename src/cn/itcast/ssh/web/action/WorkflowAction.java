package cn.itcast.ssh.web.action;

import cn.itcast.ssh.domain.Employee;
import cn.itcast.ssh.domain.LeaveBill;
import cn.itcast.ssh.service.ILeaveBillService;
import cn.itcast.ssh.service.IWorkflowService;
import cn.itcast.ssh.utils.SessionContext;
import cn.itcast.ssh.utils.ValueContext;
import cn.itcast.ssh.web.form.WorkflowBean;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletOutputStream;

import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.task.Comment;
import org.activiti.engine.task.Task;
import org.activiti.explorer.ui.content.file.PdfAttachmentRenderer;
import org.apache.struts2.ServletActionContext;

import com.opensymphony.xwork2.ActionSupport;
import com.opensymphony.xwork2.ModelDriven;

@SuppressWarnings("serial")
public class WorkflowAction extends ActionSupport implements ModelDriven<WorkflowBean> {

	private WorkflowBean workflowBean = new WorkflowBean();
	
	@Override
	public WorkflowBean getModel() {
		return workflowBean;
	}
	
	private IWorkflowService workflowService;
	
	private ILeaveBillService leaveBillService;

	public void setLeaveBillService(ILeaveBillService leaveBillService) {
		this.leaveBillService = leaveBillService;
	}

	public void setWorkflowService(IWorkflowService workflowService) {
		this.workflowService = workflowService;
	}

	/**
	 * 部署管理首页显示
	 * @return
	 */
	public String deployHome(){
		//部署对象信息 对应信息(act_re_deployment)
		List<Deployment> deploymentList = workflowService.findDeploymentList();
		//查询流程定义信息 对应表(act_re_procdef)
		List<ProcessDefinition> processDefinitionList = workflowService.findProcessDefinitionList();
		//放置到上下文对象中
		ValueContext.putValueContext("deploymentList", deploymentList);
		ValueContext.putValueContext("processDefinitionList", processDefinitionList);
		return "deployHome";
	}
	
	/**
	 * 发布流程
	 * @return
	 */
	public String newdeploy(){
		//获取页面传递的值
		File file = workflowBean.getFile();
		String filename = workflowBean.getFilename();
		workflowService.saveNewDeploye(file,filename);
		return "list";
	}
	
	/**
	 * 删除部署信息
	 */
	public String delDeployment(){
		String deploymentId = workflowBean.getDeploymentId();
		workflowService.deleteProcessDefinitionByDeployementId(deploymentId);
		return "list";
	}
	
	/**
	 * 查看流程图
	 * @throws Exception 
	 */
	public String viewImage() throws Exception{
		//1.获取页面传递的部署对象ID和资源图片名称
		String deploymentId = workflowBean.getDeploymentId();
		String imageName = workflowBean.getImageName();
		//2.获取资源文件表中资源图片的输入流
		InputStream inputStream = workflowService.findImageInputStream(deploymentId,imageName);
		//3.从response对象中获取输出流
		OutputStream outputStream = ServletActionContext.getResponse().getOutputStream();
		for(int b=-1;(b=inputStream.read())!=-1;){
			outputStream.write(b);
		} 
		outputStream.close();
		inputStream.close();
		return null;
	}
	
	// 启动流程
	public String startProcess(){
		//更新请假状态 启动流程定义 流程实例关联业务
		workflowService.saveStartProcess(workflowBean);
		return "listTask";
	}
	
	
	
	/**
	 * 任务管理首页显示
	 * @return
	 */
	public String listTask(){
		//当前用户的名字
		String name = SessionContext.get().getName();
		List<Task> list = workflowService.findTaskListByName(name);
		ValueContext.putValueContext("list", list);
		return "task";
	}
	
	/**
	 * 打开任务表单
	 */
	public String viewTaskForm(){
		//任务Id
		String taskId = workflowBean.getTaskId();
		String url = workflowService.findTaskFormKeyByTaskId(taskId);
		url += "?taskId="+taskId;
		ValueContext.putValueContext("url", url);
		return "viewTaskForm";
	}
	
	// 准备表单数据
	public String audit(){
		String taskId = workflowBean.getTaskId();
		//一.使用taskId 查找请假单Id 获取请假信息
		LeaveBill leaveBill = workflowService.findLeaveBillByTaskId(taskId);
		ValueContext.putValueStack(leaveBill);
		//二.已知任务Id
		List<String> outcomeList = workflowService.findOutComeListByTaskId(taskId);
		ValueContext.putValueContext("outcomeList", outcomeList);
		//三. 查询所有历史审核人的审核信息 对应表act_hi_comment
		List<Comment> commentList = workflowService.findCommentByTaskId(taskId);
		ValueContext.putValueContext("commentList", commentList);
		return "taskForm";
	}
	
	/**
	 * 提交任务
	 */
	public String submitTask(){
		workflowService.saveSubmitTask(workflowBean);
		return "listTask";
	}
	
	/**
	 * 查看当前流程图（查看当前活动节点，并使用红色的框标注）
	 */
	public String viewCurrentImage(){
		//查看流程图
		String taskId = workflowBean.getTaskId();
		ProcessDefinition processDefinition = workflowService.findProcessDefinitionByTaskId(taskId);
		ValueContext.putValueContext("deploymentId", processDefinition.getDeploymentId());
		ValueContext.putValueContext("imageName", processDefinition.getDiagramResourceName());
		Map<String, Object> map = workflowService.findCoordingByTask(taskId);
		ValueContext.putValueContext("acs", map);
		return "image";
	}
	
	// 查看历史的批注信息
	public String viewHisComment(){
		//查询请假单信息
		Long id = workflowBean.getId();
		LeaveBill leaveBill = leaveBillService.findLeaveBillById(id);
		ValueContext.putValueStack(leaveBill);
		//查询历史批注信息
		List<Comment> commentList = workflowService.findCommentByLeaveBillId(id);
		ValueContext.putValueContext("commentList", commentList);
		return "viewHisComment";
	}
}
