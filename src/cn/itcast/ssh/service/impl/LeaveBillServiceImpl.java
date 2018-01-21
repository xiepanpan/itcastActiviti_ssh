package cn.itcast.ssh.service.impl;

import java.util.List;

import cn.itcast.ssh.dao.ILeaveBillDao;
import cn.itcast.ssh.domain.LeaveBill;
import cn.itcast.ssh.service.ILeaveBillService;
import cn.itcast.ssh.utils.SessionContext;

public class LeaveBillServiceImpl implements ILeaveBillService {

	private ILeaveBillDao leaveBillDao;

	public void setLeaveBillDao(ILeaveBillDao leaveBillDao) {
		this.leaveBillDao = leaveBillDao;
	}
	
	/**
	 * 查询自己的请假单信息
	 */
	@Override
	public List<LeaveBill> findLeaveBillList() {
		List<LeaveBill> leaveBillList = leaveBillDao.findLeaveBillList();
		return leaveBillList;
	}

	/**
	 * 保存请假单
	 */
	@Override
	public void saveLeaveBill(LeaveBill leaveBill) {
		Long id = leaveBill.getId();
		//新增保存
		if (id==null) {			
			leaveBill.setUser(SessionContext.get());
			leaveBillDao.saveLeaveBill(leaveBill);
		}
		//更新保存
		else {
			leaveBillDao.updateLeaveBill(leaveBill);
		}
	}
	
	/**
	 * 使用请假单Id 查询请假单对象
	 */
	@Override
	public LeaveBill findLeaveBillById(Long id) {
		LeaveBill leaveBill = leaveBillDao.findLeaveBillById(id);
		return leaveBill;
	}
	
	/**
	 * 删除请假单
	 */
	@Override
	public void deleteLeaveBillById(Long id) {
		leaveBillDao.deleteLeaveBillById(id);
	}
}
