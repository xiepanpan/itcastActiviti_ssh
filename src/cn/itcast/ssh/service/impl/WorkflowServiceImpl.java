package cn.itcast.ssh.service.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipInputStream;

import org.activiti.engine.FormService;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.form.TaskFormData;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.history.HistoricVariableInstance;
import org.activiti.engine.impl.identity.Authentication;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.activiti.engine.impl.pvm.PvmTransition;
import org.activiti.engine.impl.pvm.process.ActivityImpl;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.DeploymentQuery;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Comment;
import org.activiti.engine.task.Task;
import org.activiti.engine.task.TaskQuery;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Cascade;


import cn.itcast.ssh.dao.ILeaveBillDao;
import cn.itcast.ssh.domain.LeaveBill;
import cn.itcast.ssh.service.IWorkflowService;
import cn.itcast.ssh.utils.SessionContext;
import cn.itcast.ssh.web.form.WorkflowBean;

public class WorkflowServiceImpl implements IWorkflowService {
	/**请假申请Dao*/
	private ILeaveBillDao leaveBillDao;
	
	private RepositoryService repositoryService;
	
	private RuntimeService runtimeService;
	
	private TaskService taskService;
	
	private FormService formService;
	
	private HistoryService historyService;
	
	public void setLeaveBillDao(ILeaveBillDao leaveBillDao) {
		this.leaveBillDao = leaveBillDao;
	}

	public void setHistoryService(HistoryService historyService) {
		this.historyService = historyService;
	}
	
	public void setFormService(FormService formService) {
		this.formService = formService;
	}
	
	public void setRuntimeService(RuntimeService runtimeService) {
		this.runtimeService = runtimeService;
	}
	public void setTaskService(TaskService taskService) {
		this.taskService = taskService;
	}

	public void setRepositoryService(RepositoryService repositoryService) {
		this.repositoryService = repositoryService;
	}

	/**
	 * 部署流程定义
	 */
	@Override
	public void saveNewDeploye(File file, String filename) {
		try {
			ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(file));
			repositoryService.createDeployment()//创建部署对象
								.name(filename)
								.addZipInputStream(zipInputStream)
								.deploy();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	@Override
	public List<Deployment> findDeploymentList() {
		List<Deployment> list = repositoryService.createDeploymentQuery()
							.orderByDeploymenTime()
							.asc()
							.list();
		return list;
	}

	@Override
	public List<ProcessDefinition> findProcessDefinitionList() {
		List<ProcessDefinition> list = repositoryService.createProcessDefinitionQuery()
							.orderByProcessDefinitionVersion().asc()
							.list();
		return list;
	}

	/**
	 * 使用部署id和资源图片名获取图片输入流
	 */
	@Override
	public InputStream findImageInputStream(String deploymentId, String imageName) {
		return repositoryService.getResourceAsStream(deploymentId, imageName);
	}

	/**
	 * 使用部署对象ID 删除流程定义
	 */
	@Override
	public void deleteProcessDefinitionByDeployementId(String deploymentId) {
		repositoryService.deleteDeployment(deploymentId,true);
	}

	/**
	 * 更新请假状态 启动流程定义 流程实例关联业务
	 */
	@Override
	public void saveStartProcess(WorkflowBean workflowBean) {
		Long id = workflowBean.getId();
		LeaveBill leaveBill = leaveBillDao.findLeaveBillById(id);
		//初始录入改为审核中
		leaveBill.setState(1);
		//获取流程定义key 当前对象的名称即为流程定义的key
		String key = leaveBill.getClass().getSimpleName();
		//流程变量#{inputUser} 设置流程的下一个任务的办理人
		Map<String, Object> variables = new HashMap<String,Object>();
		variables.put("inputUser", SessionContext.get().getName());//当前用户
		
		//流程变量设置字符串(格式LeaveBill.id) 通过设置 让流程关联业务
		//或者使用正在执行对象表的business_key,流程关联业务
		String objId=key+"."+id;
		variables.put("objId", objId);
		runtimeService.startProcessInstanceByKey(key, objId, variables);
		
	}
	
	/**
	 * 使用当前用户名查询正在执行的任务表
	 */
	@Override
	public List<Task> findTaskListByName(String name) {
		//当前用户
		String assignee = SessionContext.get().getName();
		List<Task> list = taskService.createTaskQuery()
					.taskAssignee(assignee)
					.orderByTaskCreateTime().asc()
					.list();
					
		return list;
	}

	/**
	 * 使用任务Id 获取当前任务节点中对应的Form key中连接的值
	 */
	@Override
	public String findTaskFormKeyByTaskId(String taskId) {
		TaskFormData taskFormData = formService.getTaskFormData(taskId);
		String url = taskFormData.getFormKey();
		return url;
	}
	
	/**
	 * 使用任务Id，查找请假单Id 从而获得请假信息
	 * taskId-》proc_ins_id->business_key
	 */
	@Override
	public LeaveBill findLeaveBillByTaskId(String taskId) {
		Task task = taskService.createTaskQuery()
					.taskId(taskId)
					.singleResult();
		//使用taskId取得processInstanceId
		String processInstanceId = task.getProcessInstanceId();
		//
		ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
						.processInstanceId(processInstanceId)
						.singleResult();
		String businessKey = processInstance.getBusinessKey();
		String id = "";
		if (StringUtils.isNotBlank(businessKey)) {
			//截取字符串 取businessKey对应的主键Id 使用Id 查询请假单对象
			id = businessKey.split("\\.")[1];
		}
		LeaveBill leaveBill = leaveBillDao.findLeaveBillById(Long.parseLong(id));
		return leaveBill;
	}
	
	/**
	 * 查新ProcessDefinitionEntity对象 从而获取连线名称
	 */
	@Override
	public List<String> findOutComeListByTaskId(String taskId) {
		Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
		//获取流程定义Id
		String processDefinitionId = task.getProcessDefinitionId();
		//查询ProcessDefinitionEntity对象
		ProcessDefinitionEntity processDefinitionEntity = (ProcessDefinitionEntity)repositoryService.getProcessDefinition(processDefinitionId);
		ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processDefinitionId(processDefinitionId).singleResult();
		
		//获取当前活动
		String activityId = processInstance.getActivityId();
		ActivityImpl activityImpl = processDefinitionEntity.findActivity(activityId);
		//获取当前活动之后的连接名称
		List<PvmTransition> outgoingTransitions = activityImpl.getOutgoingTransitions();
		List<String> list = new ArrayList<>();
		if (outgoingTransitions!=null&&outgoingTransitions.size()>0) {
			for (PvmTransition pvmTransition : outgoingTransitions) {
				String name = (String)pvmTransition.getProperty("name");
				if(StringUtils.isNotBlank(name)){
					list.add(name);
				}
				else{
					list.add("默认提交");
				}
			}
		}
		return list;
	}

	/**
	 * 指定连线名称完成任务
	 */
	@Override
	public void saveSubmitTask(WorkflowBean workflowBean) {
		// 添加评论信息
		String outcome = workflowBean.getOutcome();
		String message=workflowBean.getComment();
		Map<String, Object> variables = new HashMap<>();
		String taskId = workflowBean.getTaskId();
		Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
		String processInstanceId = task.getProcessInstanceId();
		Authentication.setAuthenticatedUserId(SessionContext.get().getName());
		taskService.addComment(taskId, processInstanceId, message);		
		//1.设置流程变量 按照连线名称 完成任务
		if (outcome!=null&&!outcome.equals("默认提交")) {
			variables.put("outcome", outcome);
		}
		taskService.complete(taskId, variables);
		//指定下一个办理人（流程图中已用类实现）
		
		//判断流程是否结束 如果结束 更新请假状态
		ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
		Long id = workflowBean.getId();
		if (processInstance==null) {
			LeaveBill leaveBill = leaveBillDao.findLeaveBillById(id);
			//审批中改为审批完成
			leaveBill.setState(2);
		}
	
	}
	
	/**
	 * 获取任务Id对应的批注
	 */
	@Override
	public List<Comment> findCommentByTaskId(String taskId) {
		Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
		String processInstanceId = task.getProcessInstanceId();
		List<Comment> list = new ArrayList<>();
//		List<HistoricTaskInstance> historicTaskInstances = historyService.createHistoricTaskInstanceQuery().processInstanceId(processInstanceId).list();
//		if (historicTaskInstances!=null && historicTaskInstances.size()>0) {
//			for (HistoricTaskInstance historicTaskInstance : historicTaskInstances) {
//				List<Comment> taskList = taskService.getTaskComments(historicTaskInstance.getId());
//				list.addAll(taskList);
//			}
//		}
		list = taskService.getProcessInstanceComments(processInstanceId);
		return list;
	}
	
	/**
	 * 通过请假单id查询历史批注信息
	 * @param id
	 * @return
	 */
	@Override
	public List<Comment> findCommentByLeaveBillId(Long id) {
		//两种方式 使用历史流程实例查询和历史流程变量查询
		LeaveBill leaveBill = leaveBillDao.findLeaveBillById(id);
		//请假单类名
		String simpleName = leaveBill.getClass().getSimpleName();
		String objId = simpleName+"."+id;
		//流程实例查询
//		HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery().processInstanceBusinessKey(objId).singleResult();
//		String processInstanceId = historicProcessInstance.getId();
		//流程变量查询
		HistoricVariableInstance historicVariableInstance = historyService.createHistoricVariableInstanceQuery().variableValueEquals("objId", objId).singleResult();
		String processInstanceId = historicVariableInstance.getProcessInstanceId();
		
		List<Comment> list = taskService.getProcessInstanceComments(processInstanceId);
		return list;
	}
	
	/**
	 * 根据任务Id查询流程定义
	 * @param taskId
	 * @return
	 */
	@Override
	public ProcessDefinition findProcessDefinitionByTaskId(String taskId) {
		Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
		String processDefinitionId = task.getProcessDefinitionId();
		ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionId(processDefinitionId).singleResult();
		return processDefinition;
	}

	/**
	 * 把x y width height值放入集合
	 */
	@Override
	public Map<String, Object> findCoordingByTask(String taskId) {
		Map<String, Object> map = new HashMap<>();
		Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
		String processDefinitionId = task.getProcessDefinitionId();
		String processInstanceId = task.getProcessInstanceId();
		ProcessDefinitionEntity processDefinitionEntity = (ProcessDefinitionEntity)repositoryService.getProcessDefinition(processDefinitionId);
		ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
		
		String activityId = processInstance.getActivityId();
		ActivityImpl activity = processDefinitionEntity.findActivity(activityId);
		map.put("x", activity.getX());
		map.put("y", activity.getY());
		map.put("width", activity.getWidth());
		map.put("height", activity.getHeight());
		return map;
	}
}
