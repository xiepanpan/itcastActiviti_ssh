package cn.itcast.ssh.dao.impl;

import java.util.List;

import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

import cn.itcast.ssh.dao.ILeaveBillDao;
import cn.itcast.ssh.domain.Employee;
import cn.itcast.ssh.domain.LeaveBill;
import cn.itcast.ssh.utils.SessionContext;

public class LeaveBillDaoImpl extends HibernateDaoSupport implements ILeaveBillDao {

	/**
	 * 查询自己的请假单信息
	 */
	@Override
	public List<LeaveBill> findLeaveBillList() {
		//从session中获取当前用户
		Employee employee = SessionContext.get();
		String hql = "from LeaveBill l where l.user=?";
		List list = this.getHibernateTemplate().find(hql, employee);
		return list;
	}

	/**
	 * 保存请假单
	 */
	@Override
	public void saveLeaveBill(LeaveBill leaveBill) {
		this.getHibernateTemplate().save(leaveBill);
	}
	
	/**
	 * 根据id查询请假单
	 */
	@Override
	public LeaveBill findLeaveBillById(Long id) {
		return this.getHibernateTemplate().get(LeaveBill.class, id);
	}
	
	/**
	 * 更新请假单
	 */
	@Override
	public void updateLeaveBill(LeaveBill leaveBill) {
		this.getHibernateTemplate().update(leaveBill);
	}
	
	/**
	 * 删除请假单
	 */
	@Override
	public void deleteLeaveBillById(Long id) {
		//根据请假单Id 查询请假单信息 获取对象LeaveBill
		LeaveBill leaveBill = this.findLeaveBillById(id);
		this.getHibernateTemplate().delete(leaveBill);
	}
}
