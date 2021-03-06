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
 * Copyright (C) 2012-2015 E.R.P. Consultores y Asociados, S.A. All Rights Reserved. *
 * Contributor(s): Carlos Parada www.erpcya.com             				 		 *
 *************************************************************************************/
package org.spinsuite.sync;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import org.ksoap2.serialization.SoapObject;
import org.spinsuite.base.DB;
import org.spinsuite.base.R;
import org.spinsuite.conn.CommunicationSoap;
import org.spinsuite.interfaces.BackGroundProcess;
import org.spinsuite.model.MSPSSyncMenu;
import org.spinsuite.model.MSPSSyncTable;
import org.spinsuite.model.MSPSTable;
import org.spinsuite.model.MWSWebServiceType;
import org.spinsuite.model.PO;
import org.spinsuite.model.POInfo;
import org.spinsuite.model.Query;
import org.spinsuite.model.X_AD_Rule;
import org.spinsuite.model.X_SPS_SyncTable;
import org.spinsuite.model.X_WS_WebService;
import org.spinsuite.model.X_WS_WebServiceMethod;
import org.spinsuite.util.BackGroundTask;
import org.spinsuite.util.DisplayType;
import org.spinsuite.util.Env;
import org.spinsuite.util.LogM;
import org.spinsuite.util.SyncValues;
import org.xmlpull.v1.XmlPullParserException;

import com._3e.ADInterface.WSModelCRUDRequest;

import android.app.NotificationManager;
import android.content.Context;
import android.database.sqlite.SQLiteException;
import android.os.StrictMode;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;

/**
 * 
 * @author Carlos Parada, cparada@erpcya.com, ERPCyA http://www.erpcya.com
 * @contributor Yamel Senih, ysenih@erpcya.com, ERPCyA http://www.erpcya.com
 * <li> Add Delete Before Pull option
 * @see <li>https://adempiere.atlassian.net/browse/SPIN-20
 *
 */
public class SyncDataTask implements BackGroundProcess  {
	/** Sync Menu ID					*/ 
	private int 					m_SPS_SyncMenu_ID = 0;
	/** Connection Database 			*/
	private DB 						conn = null;
	/** Context 						*/
	private Context 				m_ctx;
	/**	Main URL						*/
	private String 					m_URL;
	/** Current URL Web Service			*/
	private String 					m_Current_URL;
	/**	Web Service Value				*/
	private String 					m_Value;
	/** Web Service NameSpace 			*/
	private String 					m_NameSpace;
	/** Web Service Method				*/
	private String 					m_MethodValue;
	/** Soap Object Call				*/
	private CommunicationSoap 		soapObject;
	/** Timeout for Response Web Service*/
	private int 					m_TimeOut = 0;
	/** Soap Object Response			*/
	private SoapObject 				soapResponse = null;
	/** Notification Manager			*/
	private NotificationManager 	m_NFManager = null;
	/** Max Value Progress Bar			*/
	private int 					m_MaxPB = 0;
	/** Builder							*/
	private Builder 				m_Builder = null;
	/** Progress Indicator				*/
	private int 					m_Progress = -1 ;
	/** Public Message					*/
	private String 					m_PublicMsg = "";
	/** Public Title					*/
	private String 					m_PublicTittle = "";
	/** Background Task					*/
	private BackGroundTask 			bgTask = null;
	/**Define if is Root Node 			*/
	private boolean					m_IsRootNode = true;
	/**	Is Forced						*/
	private boolean 				m_IsForced = false;
	/**	Delete Before					*/
	private boolean					m_IsDeleteBefore = false;
	
	
	/**
	 * 
	 * *** Constructor ***
	 * @author Carlos Parada, cparada@erpcya.com, ERPCyA http://www.erpcya.com
	 * @contributor Yamel Senih, ysenih@erpcya.com, ERPCyA http://www.erpcya.com
	 * <li>Get URL SOAP from Context
	 * @param p_ctx
	 * @param p_SPS_SyncMenu_ID
	 * @param p_IsForced
	 * @param p_IsDeleteBefore
	 */
	public SyncDataTask(Context p_ctx, int p_SPS_SyncMenu_ID, boolean p_IsForced, boolean p_IsDeleteBefore) {
		m_ctx = p_ctx;
		m_SPS_SyncMenu_ID = p_SPS_SyncMenu_ID;
		m_IsForced = p_IsForced;
		m_IsDeleteBefore = p_IsDeleteBefore;
		if(m_IsDeleteBefore) {
			m_IsForced = m_IsDeleteBefore;
		}
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build(); 
        StrictMode.setThreadPolicy(policy);
        
        m_NFManager = (NotificationManager) m_ctx.getSystemService(Context.NOTIFICATION_SERVICE);
		m_Builder = new NotificationCompat.Builder(m_ctx);
		//	Set URL
		m_URL = Env.getContext("#SUrlSoap");
		//	
		bgTask = new BackGroundTask(this, m_ctx);
		bgTask.runTask();
	}
	
	@Override
	public void publishBeforeInit() {
		m_Builder.setContentTitle(m_PublicTittle)
		.setContentText(m_PublicMsg)
		.setProgress(m_MaxPB, m_Progress, m_Progress == -1)
		.setSmallIcon(android.R.drawable.stat_sys_download);
		m_NFManager.notify(0, m_Builder.build());
	}

	@Override
	public void publishOnRunning() {
		m_Builder.setContentTitle(m_PublicTittle)
			.setContentText(m_PublicMsg)
			.setProgress(m_MaxPB, m_Progress, m_Progress == -1)
			.setSmallIcon(android.R.drawable.stat_sys_download);
			
		m_NFManager.notify(0, m_Builder.build());
	}

	@Override
	public void publishAfterEnd() {
		m_Builder.setContentTitle(m_PublicTittle)
		.setContentText(m_PublicMsg)
		.setProgress(0, 0, false)
		.setSmallIcon(android.R.drawable.stat_sys_download_done);
		m_NFManager.notify(0, m_Builder.build());
	}

	@Override
	public Object run() {
		//	Get Previous Milliseconds
		long previousMillis = System.currentTimeMillis();
		boolean m_Error = false;
		try{
			conn = new DB(m_ctx);
			conn.openDB(DB.READ_WRITE);
			syncData(m_SPS_SyncMenu_ID, 0);
		} catch(Exception e) {
			m_Error = true;
			LogM.log(m_ctx, getClass(), Level.SEVERE, e.getLocalizedMessage());
			e.printStackTrace();
		}
		//	
		finally{
			conn.close();
			conn = null;
			//	Last Message
			if(!m_Error) {
				long afterMillis = System.currentTimeMillis();
				long duration = afterMillis - previousMillis;
				m_PublicMsg = (m_PublicMsg == null 
									? ""
											: m_PublicMsg + " ") + m_ctx.getString(R.string.SynchronizingEnding) + " " 
						+ m_ctx.getString(R.string.Sync_Duration) 
						+ ": " + SyncValues.getDifferenceValue(duration);
			}
		}
		//	
		return null;
	}
	
	/**
	 * Synchronize data method
	 * @author Carlos Parada, cparada@erpcya.com, ERPCyA http://www.erpcya.com
	 * @param p_SPS_SyncMenu_ID
	 * @return void
	 * @throws XmlPullParserException 
	 * @throws IOException 
	 */
	private void syncData(int p_SPS_SyncMenu_ID, int PageNo) {
		MSPSSyncMenu syncm = new MSPSSyncMenu(m_ctx, p_SPS_SyncMenu_ID, conn);
		SoapObject param  = null;
		int qtyPages = 1;
		int currentPage = PageNo + 1;
		//Run Script Before Call Web Service 
		if (syncm.getAD_RuleBefore_ID()!=0){
			X_AD_Rule rule  = new X_AD_Rule(m_ctx, syncm.getAD_RuleBefore_ID(), conn);
			runQuery(rule.getScript(),null);
		}
		
		//Call Web Services
		if (syncm.getWS_WebServiceType_ID()!=0){
			setSyncValues(syncm);
			
			//Run Query Data Web Service
			if (m_MethodValue.equals(SyncValues.WSMQueryData)) {
				param = getSoapParam(syncm,PageNo);
				callWebService(param,syncm);
				
				if (soapResponse != null && soapResponse.hasAttribute(SyncValues.WSQtyPages))
					qtyPages = Integer.parseInt(soapResponse.getAttributeAsString(SyncValues.WSQtyPages));
				
				//	Delete All Records
				if(soapResponse != null
						&& !soapResponse.hasProperty(SyncValues.WSRespError)
						&& m_IsDeleteBefore) {
					//	Delete Old Data
					if(syncm.getSPS_Table_ID() != 0) {
						MSPSTable m_Table = new MSPSTable(m_ctx, syncm.getSPS_Table_ID(), conn);
						//	Execute
						DB.executeUpdate(m_ctx, 
								"DELETE FROM " + m_Table.getTableName() + " WHERE AD_Client_ID = ?", 
								Env.getAD_Client_ID(m_ctx), conn);
					}
				}
				//	Write Data
				while (currentPage <= qtyPages) {
					writeDB(syncm,0);
					if (currentPage != qtyPages){
						param = getSoapParam(syncm, currentPage);
						callWebService(param, syncm);
					}
					currentPage++;
				}
			}//End Query Data Web Service
			
			//Run Create Data Web Service
			else if (m_MethodValue.equals(SyncValues.WSMCreateData)) {
				if (syncm.getSPS_Table_ID()!=0){
					MSPSTable table= new MSPSTable(m_ctx, syncm.getSPS_Table_ID(), conn);
					String whereClause = "";
					Object[] parameters = null;
					if (syncm.getWhereClause()!=null)
						whereClause += syncm.getWhereClause();
					if (!syncm.isForced() && !m_IsForced){
						whereClause = " AND EXISTS (SELECT 1 "
													+ "FROM "
													+ "SPS_SyncTable "
													+ "WHERE SPS_SyncTable.SPS_Table_ID = ? AND "
													+ "SPS_SyncTable.Record_ID = "+table.getTableName()+"."+table.getTableName()+"_ID AND "
//													+ "SPS_SyncTable.EventChangeLog IN (?,?) AND "
													+ "SPS_SyncTable.EventChangeLog = ? AND "
													+ "SPS_SyncTable.IsSynchronized='N' )";
						parameters = new Object[]{table.getSPS_Table_ID(),X_SPS_SyncTable.EVENTCHANGELOG_Insert};
					} else {
						parameters = new Object[]{};
					}
					
					List<PO> rows = new Query(m_ctx, table.getTableName(), whereClause, conn)
									.setParameters(parameters)
									.list();
					for (PO row : rows) {
						param= getSoapParam(syncm,PageNo,row);
						callWebService(param,syncm);
						writeDB(syncm,row.get_ID());
					}
					if (rows.size()==0){
						m_PublicTittle = syncm.getName();
						m_PublicMsg = m_ctx.getString(R.string.msg_NoRecordsPendingtoSync);
					}
				}
			}//End Create Data Web Service
			
			else if (m_MethodValue.equals(SyncValues.WSMUpdateData)) {
			if (syncm.getSPS_Table_ID()!=0){
				MSPSTable table= new MSPSTable(m_ctx, syncm.getSPS_Table_ID(), conn);
				String whereClause = "";
				Object[] parameters = null;
				if (syncm.getWhereClause()!=null)
					whereClause += syncm.getWhereClause();
				if (!syncm.isForced() && !m_IsForced){
					whereClause = " AND EXISTS (SELECT 1 "
												+ "FROM "
												+ "SPS_SyncTable "
												+ "WHERE SPS_SyncTable.SPS_Table_ID = ? AND "
												+ "SPS_SyncTable.Record_ID = "+table.getTableName()+"."+table.getTableName()+"_ID AND "
												+ "SPS_SyncTable.EventChangeLog IN (?,?) AND "
//												+ "SPS_SyncTable.EventChangeLog = ? AND "
												+ "SPS_SyncTable.IsSynchronized='N' )";
					parameters = new Object[]{table.getSPS_Table_ID(),X_SPS_SyncTable.EVENTCHANGELOG_Insert, X_SPS_SyncTable.EVENTCHANGELOG_Update};
//					parameters = new Object[]{table.getSPS_Table_ID(),X_SPS_SyncTable.EVENTCHANGELOG_Update};
				}else{
					parameters = new Object[]{};
				}
				
				whereClause = Env.parseContext(whereClause, true);
				
				List<PO> rows = new Query(m_ctx, table.getTableName(), whereClause, conn)
								.setParameters(parameters)
								.list();
				for (PO row : rows) {
					param= getSoapParam(syncm,PageNo,row);
					callWebService(param,syncm);
					writeDB(syncm,row.get_ID());
				}
				if (rows.size()==0){
					m_PublicTittle = syncm.getName();
					m_PublicMsg = m_ctx.getString(R.string.msg_NoRecordsPendingtoSync);
				}
			}
		}//End Create Data Web Service
			
		}
		
		//Run Script After Call Web Service 
		if (syncm.getAD_RuleAfter_ID()!=0){
			X_AD_Rule rule  = new X_AD_Rule(m_ctx, syncm.getAD_RuleAfter_ID(), conn);
			runQuery(rule.getScript(),null);
		}
		
		if (m_IsRootNode){
			m_IsRootNode = false;
			//Get Child's Web Services
			List<MSPSSyncMenu> syncms = MSPSSyncMenu.getNodesFromParent(m_ctx, Integer.valueOf(m_SPS_SyncMenu_ID).toString(), conn);
			
			for (MSPSSyncMenu mspsSyncMenu : syncms) {
				syncData(mspsSyncMenu.getSPS_SyncMenu_ID(),0);
			}
			
		}
	}
	
	/**
	 * 
	 * @author Carlos Parada, cparada@erpcya.com, ERPCyA http://www.erpcya.com
	 * @param sm
	 * @return
	 * @return SoapObject
	 */
	private SoapObject getSoapParam(MSPSSyncMenu sm, int PageNo,PO data) {
		SoapObject param = null;
		MWSWebServiceType wst = new MWSWebServiceType(m_ctx, sm.getWS_WebServiceType_ID(), conn);
		
		if(m_MethodValue.equals(SyncValues.WSMQueryData)){
			String whereClause="";
			
			if (!sm.isForced() && !m_IsForced){
				SimpleDateFormat sdf = DisplayType.getDateFormat(m_ctx, DisplayType.DATE_TIME, "yyyy-MM-dd hh:mm:ss");
				whereClause +=(sm.getLastSynchronized()!=null ? "(UPDATED >= '" + sdf.format(sm.getLastSynchronized()) + "')" : "");
			}
			if (sm.getWhereClause()!=null)
				whereClause += (whereClause.equals("")?"":" AND ") + "(" + Env.parseContext(sm.getWhereClause(), true) + ")";
			param = new WSModelCRUDRequest(m_ctx, m_NameSpace, wst.getWS_WebServiceType_ID(), conn, 0, null, whereClause, PageNo);
		}
		else if (m_MethodValue.equals(SyncValues.WSMCreateData))
			param = new WSModelCRUDRequest(m_ctx, m_NameSpace, wst.getWS_WebServiceType_ID(), conn, data.get_ID(), data);
		else if (m_MethodValue.equals(SyncValues.WSMUpdateData))
			param = new WSModelCRUDRequest(m_ctx, m_NameSpace, wst.getWS_WebServiceType_ID(), conn, data.get_ID() , data);
	//	
	
		
		//	
		return param;
	}
	/**
	 * 
	 * @author Carlos Parada, cparada@erpcya.com, ERPCyA http://www.erpcya.com
	 * @param sm
	 * @param PageNo
	 * @return
	 * @return SoapObject
	 */
	private SoapObject getSoapParam(MSPSSyncMenu sm, int PageNo) {
		return getSoapParam(sm, PageNo,null);
	}
	
	/**
	 * Set Synchronize Values
	 * @author Carlos Parada, cparada@erpcya.com, ERPCyA http://www.erpcya.com
	 * @param sm
	 * @return void
	 */
	private void setSyncValues(MSPSSyncMenu sm){
		MWSWebServiceType wst = new MWSWebServiceType(m_ctx, sm.getWS_WebServiceType_ID(), conn);
		
		if (wst.getWS_WebService_ID() != 0){
			X_WS_WebService ws = new X_WS_WebService(m_ctx, wst.getWS_WebService_ID(), conn);
			m_Value = ws.getValue();
			m_NameSpace =  (ws.getNameSpace() == null ? "" : ws.getNameSpace());
			m_Current_URL = SyncValues.getValidURL(m_URL, m_Value);
		}
		
		if (wst.getWS_WebServiceMethod_ID() != 0){
			X_WS_WebServiceMethod wsm =	new X_WS_WebServiceMethod(m_ctx, wst.getWS_WebServiceMethod_ID(), conn);
			if (wsm.getValue() != null)
				m_MethodValue = wsm.getValue();
		}
	}
	
	/**
	 * Call Web Service And get Response
	 * @author Carlos Parada, cparada@erpcya.com, ERPCyA http://www.erpcya.com
	 * @param p_SO_Param
	 * @throws IOException
	 * @throws XmlPullParserException
	 * @return void
	 */
	private void callWebService(SoapObject p_SO_Param, MSPSSyncMenu sm) {
		
		m_PublicTittle = sm.getName();
		m_PublicMsg = sm.getDescription();
		m_Progress = -1;
		publishOnRunning();
		
		soapObject = new CommunicationSoap(m_Current_URL, m_NameSpace, m_MethodValue, SyncValues.IsNetService);
		soapObject.setM_SoapAction(m_NameSpace + "/" + m_MethodValue);

		if (p_SO_Param != null)
			soapObject.addSoapObject(p_SO_Param);
		
		soapObject.init_envelope();

		if (m_TimeOut == 0)
			soapObject.initTransport();
		else
			soapObject.initTransport(m_TimeOut);

		try {
			soapObject.call();
			soapResponse = (SoapObject) soapObject.getM_Envelope().getResponse();
		} catch (SocketTimeoutException e) {
			e.printStackTrace();
			m_PublicMsg = e.getLocalizedMessage();
		} catch (XmlPullParserException e) {
			e.printStackTrace();
			m_PublicMsg = e.getLocalizedMessage();	
		} catch ( IOException e) {
			e.printStackTrace();
			m_PublicMsg = e.getLocalizedMessage();	
		}catch (Exception e) {
			e.printStackTrace();
			m_PublicMsg = e.getLocalizedMessage();
	 	}
		finally{
			publishOnRunning();
		}
				
	}
	
	/**
	 * Write into DB
	 * @author Carlos Parada, cparada@erpcya.com, ERPCyA http://www.erpcya.com
	 * @param sm
	 * @return void
	 */
	private void writeDB(MSPSSyncMenu sm, int p_ID)
	{		
		 
		//Validate Response
		if (soapResponse == null)
			return;
		
		if (soapResponse.hasProperty(SyncValues.WSRespError)) {
			//	Mark like Not Synchronized
			if (m_MethodValue.equals(SyncValues.WSMQueryData)) {
				sm.setLastSynchronized(null);
				sm.save();
			}
			m_PublicMsg = soapResponse.getPropertyAsString("Error");
			LogM.log(m_ctx, SyncDataTask.class, Level.SEVERE, m_PublicMsg);
			publishOnRunning();
			return;
		}
		//	
		if (m_MethodValue.equals(SyncValues.WSMQueryData)) {
			//Validate Data Set
			if (!soapResponse.hasProperty(SyncValues.WSRespDataSet))
				return;
			//Soap Data Set
			SoapObject soapDataSet= (SoapObject) soapResponse.getProperty(SyncValues.WSRespDataSet);
			
			//Validate Data Row
			if (soapDataSet == null || !soapDataSet.hasProperty(SyncValues.WSRespDataRow))
				return;
			
			//Soap Data Row
			SoapObject soapDataRow=(SoapObject)  soapDataSet.getProperty(SyncValues.WSRespDataRow);
			
			int countDataSet = soapDataSet.getPropertyCount();
			int countDataRow = soapDataRow.getPropertyCount();
			SoapObject field = null;
			Object value =null;
			PO data = null;
			String whereClause = "";
			String [] keyColumns = null;
			POInfo info = null;
			if (sm.getSPS_Table_ID()!= 0)
				info = POInfo.getPOInfo (m_ctx, sm.getSPS_Table_ID(), conn);
			
			if (info == null )
				return;
			
			m_MaxPB = countDataSet;
			
			
			if (info.getKeyColumns()!=null )
				keyColumns = info.getKeyColumns();
			else
				keyColumns = new String[]{};
			
			Arrays.sort(keyColumns);
			
			for (int i = 0; i < keyColumns.length; i++) {
				whereClause += (whereClause.equals("") ? " ": " AND ") + keyColumns[i] + "=?";
			}
				
			for (int i = 0; i < countDataSet; i++) {
				m_Progress = i+1;
				//Soap Data Row
				soapDataRow = (SoapObject)  soapDataSet.getProperty(i);
				Object [] keyValues = getKeyValues(soapDataRow, countDataRow, keyColumns);

				try {
					data = new Query(m_ctx, info.getTableName(), whereClause, conn)
						.setParameters(keyValues)
						.first();
					
					if(data == null) {
						whereClause = "EXISTS (SELECT 1 FROM SPS_SyncTable st WHERE st.SPS_Table_ID = " + info.getSPS_Table_ID();
						for (int j = 0 ;j<keyColumns.length;j++)
							whereClause += (j==0 ? " AND (" : " OR ") + " st.SyncRecord_ID = ? ";
						
						if (keyColumns.length>0)
							whereClause += ")";
						
						for (int j = 0 ;j<keyColumns.length;j++)
							whereClause += (j==0 ? " AND (" : " OR ") + " st.Record_ID = " + info.getTableName() + "." + keyColumns[j];
						
						if (keyColumns.length>0)
							whereClause += ")";
						
						whereClause += " AND IsSynchronized='Y')";
						
						data = new Query(m_ctx, info.getTableName(), whereClause, conn)
							.setParameters(keyValues)
							.first();

					}

					if (data==null){
						//Cursor rs = conn.querySQL(info.buildSelect().append(" WHERE 1=0 ").toString(),new String[]{});
						MSPSTable table = new MSPSTable(m_ctx, info.getSPS_Table_ID(), conn);
						data = table.getPO(null,conn);	
					}
					
					if (data==null)
						return;
					for (int j=0; j < countDataRow; j++){
						field = (SoapObject) soapDataRow.getProperty(j);
						String columnName = field.getAttributeAsString(SyncValues.WSColumn);
						if (keyColumns.length <= 1){
							int index = Arrays.binarySearch(keyColumns, columnName);
							if (index >= 0 && data.get_ID()!=0)
								continue;
						}
						if (field.getProperty(SyncValues.WSValue) == null)
							value = null;
						else
							value = field.getPropertyAsString(SyncValues.WSValue);
						
						value = DisplayType.parseValue (value, info.getDisplayType(columnName), columnName);
						
						data.set_Value(columnName, value);
					}
					data.setSynchronization(true);
					data.saveEx();
					publishOnRunning();
					
				} catch (Exception e) {
					m_PublicMsg = e.getLocalizedMessage();
					LogM.log(m_ctx, SyncDataTask.class, Level.SEVERE, m_PublicMsg,e.getCause());
					publishOnRunning();
				} 
					
			}
			//	Mark like Synchronized
			sm.setLastSynchronized(new Timestamp(System.currentTimeMillis()));
			sm.save();
		}
		else if (m_MethodValue.equals(SyncValues.WSMCreateData)){

			if (soapResponse.hasAttribute("RecordID")){
				String whereClause = "SPS_Table_ID = " + sm.getSPS_Table_ID() + " AND "
						+ "Record_ID = " + p_ID + " AND "
						+ "EventChangeLog ='" + X_SPS_SyncTable.EVENTCHANGELOG_Insert + "' AND IsSynchronized='N'";
	
				try {
					MSPSSyncTable synctable = MSPSSyncTable.getSyncTable(sm.getCtx(), conn, whereClause);
					if (synctable.getSPS_SyncTable_ID()>0){
						synctable.setSyncRecord_ID(soapResponse.getAttributeAsString("RecordID"));
						synctable.setIsSynchronized(true);
						synctable.save();
						sm.setLastSynchronized(new Timestamp(System.currentTimeMillis()));
						sm.save();
					}
				} catch (Exception e) {
					m_PublicMsg = e.getLocalizedMessage();
					LogM.log(m_ctx, SyncDataTask.class, Level.SEVERE, m_PublicMsg,e.getCause());
					publishOnRunning();
				}
			}
			
		}else if (m_MethodValue.equals(SyncValues.WSMUpdateData)){

		if (soapResponse.hasAttribute("RecordID")){
			String whereClause = "SPS_Table_ID = " + sm.getSPS_Table_ID() + " AND "
					+ "Record_ID = " + p_ID + " AND "
					+ "EventChangeLog ='" + X_SPS_SyncTable.EVENTCHANGELOG_Update+ "' AND IsSynchronized='N'";

			try {
				MSPSSyncTable synctable = MSPSSyncTable.getSyncTable(sm.getCtx(), conn, whereClause);
				if (synctable.getSPS_SyncTable_ID()>0){
					synctable.setSyncRecord_ID(soapResponse.getAttributeAsString("RecordID"));
					synctable.setIsSynchronized(true);
					synctable.save();
					sm.setLastSynchronized(new Timestamp(System.currentTimeMillis()));
					sm.save();
				}
			} catch (Exception e) {
				m_PublicMsg = e.getLocalizedMessage();
				LogM.log(m_ctx, SyncDataTask.class, Level.SEVERE, m_PublicMsg,e.getCause());
				publishOnRunning();
			}
		}
		
	}
		
		soapResponse = null;
	}
	/**
	 * Run a Query
	 * @author Carlos Parada, cparada@erpcya.com, ERPCyA http://www.erpcya.com
	 * @param sql
	 * @param data
	 * @return void
	 */
	private void runQuery(String sql, Object[] data){
		try {
			//	Parse SQL
			String parsedSQL = Env.parseContext(sql, true);
			if (data != null)
				conn.executeSQL(parsedSQL, data);
			else
				conn.executeSQL(parsedSQL);
		} catch (SQLiteException e){
			e.printStackTrace();
			m_PublicMsg = e.getLocalizedMessage();
		} catch (Exception e) {
			e.printStackTrace();
			m_PublicMsg = e.getLocalizedMessage();
	 	} finally{
			publishOnRunning();
		}
	}
	
	/**
	 * Get Key Values
	 * @author Carlos Parada, cparada@erpcya.com, ERPCyA http://www.erpcya.com
	 * @param soapDataRow
	 * @param countDataRow
	 * @param keyColumns
	 * @return
	 * @return Object[]
	 */
	private Object[] getKeyValues(SoapObject soapDataRow,int countDataRow,String[] keyColumns){
		Object [] keyValues = new String[keyColumns.length];
		SoapObject field = null;
		for (int i=0; i < countDataRow; i++){
			field = (SoapObject) soapDataRow.getProperty(i);
			String columnName = field.getAttributeAsString(SyncValues.WSColumn);
			int index = Arrays.binarySearch(keyColumns, columnName);
			if (index < 0)
				continue;
			if (field.getProperty(SyncValues.WSValue) == null)
				keyValues[index] = null;
			else
				keyValues[index] = field.getPropertyAsString(SyncValues.WSValue);
			
		}
		//	Return
		return keyValues;
	}
}
