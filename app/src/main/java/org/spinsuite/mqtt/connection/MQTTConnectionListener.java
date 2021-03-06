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
 * Contributor(s): Yamel Senih www.erpcya.com                                        *
 *************************************************************************************/
package org.spinsuite.mqtt.connection;

import java.util.logging.Level;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.spinsuite.bchat.util.BCMessageHandle;
import org.spinsuite.util.Env;
import org.spinsuite.util.LogM;

import android.content.Context;

/**
 * @author Yamel Senih, ysenih@erpcya.com, ERPCyA http://www.erpcya.com Apr 1, 2015, 3:36:57 AM
 *
 */
public class MQTTConnectionListener implements IMqttActionListener {

	/**
	 * *** Constructor ***
	 * @author Yamel Senih, ysenih@erpcya.com, ERPCyA http://www.erpcya.com
	 * @param p_Ctx
	 */
	public MQTTConnectionListener(Context p_Ctx) {
		m_Ctx = p_Ctx;
	}

	/**	Context					*/
	private Context 			m_Ctx = null;
	/**	Default Topics			*/
	private String[] 			m_DefaultTopics = null;
	
	@Override
	public void onFailure(IMqttToken token, Throwable e) {
		MQTTConnection.getInstance(m_Ctx).setStatus(MQTTConnection.DISCONNECTED);
		LogM.log(m_Ctx, getClass(), Level.SEVERE, "Connection Error", e);
		//	Try Connect
		MQTTConnection.getInstance(m_Ctx).tryConnect();
	}

	@Override
	public void onSuccess(IMqttToken token) {
		LogM.log(m_Ctx, getClass(), Level.FINE, "Connection MQTT is Ok");
		MQTTConnection.getInstance(m_Ctx).setStatus(MQTTConnection.CONNECTED);
		subscribeToDefaultsTopics();
		//	Verify Messages
		BCMessageHandle.getInstance(m_Ctx).processMessageThread();
	}

	/**
	 * Subscribe to topics
	 * @author Yamel Senih, ysenih@erpcya.com, ERPCyA http://www.erpcya.com
	 * @return void
	 */
	private void subscribeToDefaultsTopics() {
		try {
			MQTTConnection m_Connection = MQTTConnection.getInstance(m_Ctx);
			//	Verify Topics for conversations
			if(m_DefaultTopics == null) {
				m_DefaultTopics = BCMessageHandle.getInstance(m_Ctx).getTopics();
				m_Connection.addTopic(m_DefaultTopics);
			}
			//	Add Standard Topics
			m_Connection.addTopic(MQTTDefaultValues.getInitialLoadTopic());
			m_Connection.addTopic(MQTTDefaultValues.getUserStatusTopic());
			m_Connection.addTopic(MQTTDefaultValues.getSyncTopic(String.valueOf(Env.getAD_User_ID())));
			m_Connection.addTopic(MQTTDefaultValues.getRequestTopic(String.valueOf(Env.getAD_User_ID())));
			//	Subscribe to the topics
			m_Connection.subscribeEx(MQTTConnection.EXACTLY_ONCE_2);
		} catch (MqttSecurityException e) {
			LogM.log(m_Ctx, getClass(), Level.SEVERE, "Security Exception", e);
		} catch (MqttException e) {
			LogM.log(m_Ctx, getClass(), Level.SEVERE, "Exception", e);
		}
	}
}
