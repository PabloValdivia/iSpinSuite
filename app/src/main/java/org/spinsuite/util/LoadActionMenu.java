/*************************************************************************************
 * Product: Spin-Suite (Making your Business Spin)                                   *
 * This program is free software; you can redistribute it and/or modify it           *
 * under the terms version 2 of the GNU General Public License as published          *
 * by the Free Software Foundation. This program is distributed in the hope          *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied        *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.                  *
 * See the GNU General Public License for more details.                              *
 * You should have received a copy of the GNU General Public License along           *
 * with this program; if not, write to the Free Software Foundation, Inc.,           *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                            *
 * For the text or an alternative of this public license, you may reach us           *
 * Copyright (C) 2012-2014 E.R.P. Consultores y Asociados, S.A. All Rights Reserved. *
 * Contributor(s): Yamel Senih www.erpcya.com                                        *
 *************************************************************************************/
package org.spinsuite.util;

import java.util.logging.Level;

import org.spinsuite.base.DB;
import org.spinsuite.base.R;
import org.spinsuite.view.V_Synchronization;
import org.spinsuite.view.V_StandardSearch;
import org.spinsuite.view.TV_DynamicActivity;
import org.spinsuite.view.V_Process;
import org.spinsuite.view.lookup.InfoField;
import org.spinsuite.view.lookup.LookupMenu;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * @author Yamel Senih, ysenih@erpcya.com, ERPCyA http://www.erpcya.com
 *
 */
public class LoadActionMenu {

	
	/**
	 * 
	 * *** Constructor ***
	 * @author Yamel Senih, ysenih@erpcya.com, ERPCyA http://www.erpcya.com 19/03/2014, 15:39:22
	 * @param activity
	 * @param isFromActivity
	 * @param conn
	 */
	public LoadActionMenu(Activity activity, boolean isFromActivity, DB conn) {
		this.activity = activity;
		this.isFromActivity = isFromActivity;
		this.conn = conn;
	}
	
	/**
	 * 
	 * *** Constructor ***
	 * @author Yamel Senih, ysenih@erpcya.com, ERPCyA http://www.erpcya.com 19/03/2014, 15:39:27
	 * @param activity
	 * @param isFromActivity
	 */
	public LoadActionMenu(Activity activity, boolean isFromActivity) {
		this(activity, isFromActivity, null);
	}
	
	/**	Activity				*/
	private Activity 	activity;
	/**	Connection				*/
	private DB			conn;
	/**	Is From Activity		*/
	private boolean		isFromActivity = false;
	
	/**
	 * 
	 * @author Yamel Senih, ysenih@erpcya.com, ERPCyA http://www.erpcya.com 15/03/2014, 11:16:13
	 * @param item
	 * @param param
	 * @return
	 * @return Bundle
	 */
	public Bundle loadAction(DisplayMenuItem item, ActivityParameter param) {
		//	Valid Action
		if(!item.isSummary()
				&& item.getAction() == null)
			return null;
		//	
		Bundle bundle = new Bundle();
		//	Intent Activity
		Intent intent = null;
		if (item.getM_MenuType().equals(LookupMenu.SYNCHRONIZATION_MENU)){
//			if(item.isSummary()) {
//				bundle.putParcelable("ParamSync", param);
//				intent = new Intent(activity, V_Synchronization.class);
//				intent.putExtras(bundle);
//				//	Show Activity
//				activity.startActivityForResult(intent, 0);
//			}
		}
		else {
			if(!item.isSummary()) {
				//	Load Parameter
				ActivityParameter paramAct = new ActivityParameter(item);
				//	Add from Parameter
				if(param != null) {
					paramAct.setActivityNo(param.getActivityNo());
					paramAct.setFrom_SPS_Table_ID(param.getSPS_Table_ID());
					paramAct.setFrom_Record_ID(param.getFrom_Record_ID());
		        	//	Is From Activity
					paramAct.setIsFromActivity(paramAct.isFromActivity());
				}
				//	
				bundle.putParcelable("Param", paramAct);
				//	
				if(item.getDeploymentType() == null 
						|| item.getDeploymentType().equals(DisplayMenuItem.DEPLOYMENTTYPE_DirectForm)) {
					//	Load Activity
					loadActivityWithAction(item, bundle);
				} else if(item.getDeploymentType().equals(DisplayMenuItem.DEPLOYMENTTYPE_List)
						|| item.getDeploymentType().equals(DisplayMenuItem.DEPLOYMENTTYPE_ListWithQuickAction)) {
					//	Start Search
					int m_SPS_Table_ID = item.getSPS_Table_ID();
					//	Valid Table
					if(m_SPS_Table_ID == 0) {
						Msg.toastMsg(activity, activity.getString(R.string.msg_LoadError) 
								+ ": " + activity.getString(R.string.msg_TableNotFound));
						loadActivityWithAction(item, bundle);
					} else {
						//	
						bundle.putInt("SPS_Table_ID", m_SPS_Table_ID);
						//	Set Read Write
						boolean m_IsReadWrite = Env.getWindowsAccess(paramAct.getSPS_Window_ID());
						bundle.putString("IsInsertRecord", (m_IsReadWrite? "Y": "N"));
						bundle.putString("Name", item.getName());
						//	
		            	intent = new Intent(activity, V_StandardSearch.class);
		    			intent.putExtras(bundle);
		    			//	Start with result
		    			activity.startActivityForResult(intent, 0);
					}
				} else if(item.getDeploymentType().equals(DisplayMenuItem.DEPLOYMENTTYPE_MenuWithQuickAction)) {
					
				}
			}
		}
		//	Return
		return bundle;
	}
	
	/**
	 * Load Menu from Activity
	 * @author Yamel Senih, ysenih@erpcya.com, ERPCyA http://www.erpcya.com 07/04/2014, 20:47:52
	 * @param m_field
	 * @param tabParam
	 * @return
	 * @return void
	 */
	public void loadActionFromActivity(InfoField m_field, TabParameter tabParam) {
		//	
		if(tabParam == null
				|| m_field == null)
			return ;
		//	
		Bundle bundle = new Bundle();
		//	Load Parameter
		DisplayMenuItem item = new DisplayMenuItem(m_field);
		//	Valid Action
		if(item.getAction() == null)
			return;
		ActivityParameter actParam = new ActivityParameter(item);
		//	Set Activity No
		actParam.setActivityNo(tabParam.getActivityNo());
		//	
		actParam.setFrom_SPS_Table_ID(tabParam.getSPS_Table_ID());
		int[] record_ID = Env.getTabRecord_ID(tabParam.getActivityNo(), tabParam.getTabNo());
    	actParam.setFrom_Record_ID(record_ID[0]);
    	//	Is From Activity
    	actParam.setIsFromActivity(true);
    	//	
    	bundle.putParcelable("Param", actParam);
		//	Add Tab Parameter
		bundle.putParcelable("TabParam", tabParam);
		//	Load Activity
		loadActivityWithAction(item, bundle);
	}
	
	/**
	 * Load Activity with action
	 * @author Yamel Senih, ysenih@erpcya.com, ERPCyA http://www.erpcya.com 04/02/2014, 22:52:55
	 * @param item
	 * @param bundle
	 * @return
	 * @return boolean
	 */
	public boolean loadActivityWithAction(DisplayMenuItem item, Bundle bundle) {
		boolean ok = false;
		boolean handleConnection = false;
		//	
		if(conn == null) {
			conn = new DB(activity);
			handleConnection = true;
		}
		//	Load Action
		DB.loadConnection(conn, DB.READ_ONLY);
		if(item.isSummary()) {
			
		} else if(item.getAction().equals(DisplayMenuItem.ACTION_Form)
				|| (item.getAction().equals(DisplayMenuItem.ACTION_Process)
						&& item.getAD_Form_ID() != 0)) {
			//	Get Class Name
			if(item.getAD_Form_ID() != 0) {
				String className = DB.getSQLValueString(
						activity, "SELECT f.ClassName " +
									"FROM AD_Form f " +
									"WHERE f.AD_Form_ID = ?", conn, 
									new String[]{String.valueOf(item.getAD_Form_ID())});
				//	Valid Class Name
				if(className != null
						&& className.length() > 0) {
					ok = loadDynamicClass(className, bundle);
				}
			}
		} else if(item.getAction().equals(DisplayMenuItem.ACTION_Window)) {
			ok = loadDynamicClass(TV_DynamicActivity.class.getName(), bundle);
		} else if(item.getAction().equals(DisplayMenuItem.ACTION_Process)
				|| item.getAction().equals(DisplayMenuItem.ACTION_Report)) {
			ok = loadDynamicClass(V_Process.class.getName(), bundle);
		}
		//	Show Message
		if(!ok)
			Msg.alertMsg(activity, 
					activity.getResources().getString(R.string.msg_ClassNotFound));
		//	Close Connection
		if(handleConnection)
			DB.closeConnection(conn);
		return ok;
	}
	
	/**
	 * Load Dynamic Class Activity
	 * @author Yamel Senih, ysenih@erpcya.com, ERPCyA http://www.erpcya.com 12/02/2014, 23:28:53
	 * @param className
	 * @param bundle
	 * @return
	 * @return boolean
	 */
	private boolean loadDynamicClass(String className, Bundle bundle) {
		boolean ok = false;
		Class<?> clazz;
		try {
			clazz = Class.forName(className);
			Intent intent = new Intent(activity, clazz);
			intent.putExtras(bundle);
			//	Start
			if(!isFromActivity)
				activity.startActivity(intent);
			else
				activity.startActivityForResult(intent, 0);
			ok = true;
		} catch (ClassNotFoundException e) {
			LogM.log(activity, getClass(), Level.SEVERE, "Error:", e);
		}
		return ok;
	}
}
