package cn.itcast.ssh.web.action;

import cn.itcast.ssh.domain.LeaveBill;
import cn.itcast.ssh.service.ILeaveBillService;
import cn.itcast.ssh.utils.ValueContext;

import java.util.List;

import com.opensymphony.xwork2.ActionSupport;
import com.opensymphony.xwork2.ModelDriven;

@SuppressWarnings("serial")
public class LeaveBillAction extends ActionSupport implements ModelDriven<LeaveBill> {

	private LeaveBill leaveBill = new LeaveBill();
	
	@Override
	public LeaveBill getModel() {
		return leaveBill;
	}
	
	private ILeaveBillService leaveBillService;

	public void setLeaveBillService(ILeaveBillService leaveBillService) {
		this.leaveBillService = leaveBillService;
	}

	/**
	 * 请假管理首页显示
	 * @return
	 */
	public String home(){
		List<LeaveBill> leaveBillList = leaveBillService.findLeaveBillList();
		ValueContext.putValueContext("leaveBillList", leaveBillList);
		return "home";
	}
	
	/**
	 * 添加请假申请
	 * @return
	 */
	public String input(){
		Long id = leaveBill.getId();
		if (id!=null) {
			LeaveBill leaveBillById = leaveBillService.findLeaveBillById(id);
			//请假单信息放到栈顶 页面使用struts的标签 支持表单回显
			ValueContext.putValueStack(leaveBillById);
		}
		return "input";
	}
	
	/**
	 * 保存/更新，请假申请
	 * 
	 * */
	public String save() {
		leaveBillService.saveLeaveBill(leaveBill);
		return "save";
	}
	
	/**
	 * 删除，请假申请
	 * 
	 * */
	public String delete(){
		Long id = leaveBill.getId();
		//执行删除
		leaveBillService.deleteLeaveBillById(id);
		return "save";
	}
	
}
