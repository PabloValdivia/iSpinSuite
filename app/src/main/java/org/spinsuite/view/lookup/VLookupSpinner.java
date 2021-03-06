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
package org.spinsuite.view.lookup;

import java.util.logging.Level;

import org.spinsuite.adapters.SpinnerLookupAdapter;
import org.spinsuite.base.DB;
import org.spinsuite.interfaces.I_Lookup;
import org.spinsuite.util.DisplayLookupSpinner;
import org.spinsuite.util.DisplayType;
import org.spinsuite.util.Env;
import org.spinsuite.util.LogM;
import org.spinsuite.util.TabParameter;

import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;

/**
 * 
 * @author Yamel Senih, ysenih@erpcya.com, ERPCyA http://www.erpcya.com Mar 2, 2015, 2:46:58 AM
 *
 */
public class VLookupSpinner extends GridField 
								implements I_Lookup {

	/**
	 * 
	 * *** Constructor ***
	 * @author Yamel Senih, ysenih@erpcya.com, ERPCyA http://www.erpcya.com
	 * @param ctx
	 * @param m_field
	 */
	public VLookupSpinner(Context ctx, InfoField m_field) {
		this(ctx, m_field, null);
		this.m_Lookup = new Lookup(ctx, null, m_field);
	}
	
	/**
	 * 
	 * *** Constructor ***
	 * @author Yamel Senih, ysenih@erpcya.com, ERPCyA http://www.erpcya.com
	 * @param ctx
	 * @param m_field
	 * @param conn
	 */
	public VLookupSpinner(Context ctx, InfoField m_field, DB conn) {
		this(ctx, m_field, null, conn);
		this.m_Lookup = new Lookup(ctx, null, m_field);
	}
	
	/**
	 * 
	 * *** Constructor ***
	 * @author Yamel Senih, ysenih@erpcya.com, ERPCyA http://www.erpcya.com
	 * @param ctx
	 * @param m_field
	 * @param tabParam
	 * @param m_Lookup
	 */
	public VLookupSpinner(Context ctx, InfoField m_field, TabParameter tabParam, Lookup m_Lookup) {
		super(ctx, m_field, tabParam);
		this.m_Lookup = m_Lookup;
		if(m_Lookup == null)
			this.m_Lookup = new Lookup(ctx, tabParam, m_field);
		//	Init
		init();
	}
	
	/**
	 * With Tab Parameter
	 * *** Constructor ***
	 * @author Yamel Senih, ysenih@erpcya.com, ERPCyA http://www.erpcya.com
	 * @param ctx
	 * @param m_field
	 * @param tabParam
	 * @param conn
	 */
	public VLookupSpinner(Context ctx, InfoField m_field, TabParameter tabParam, DB conn) {
		super(ctx, m_field, tabParam);
		m_Lookup = new Lookup(ctx, tabParam, m_field);
		init();
	}
	
	/**	String 				*/
	private Spinner 			v_Spinner = null;
	/**	Lookup				*/
	private Lookup				m_Lookup = null;
	/**	Old Value			*/
	private Object 				m_OldValue = null;
	//	
	@Override
	protected void init() {
		v_Spinner = new Spinner(getContext());
		//	
		setEnabled(!m_field.IsReadOnly);
		//	Add to View
		addView(v_Spinner);
		//	Load Data
		if(m_Lookup != null) {
			m_Lookup.load(false);
			populate();
		}
		//	Set Default Value
		if(m_field.DefaultValue != null
					&& m_field.DefaultValue.length() > 0)
			setValue(Env.parseContext(m_field.DefaultValue, true));
		//	Listener
		v_Spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){

			@Override
			public void onItemSelected(AdapterView<?> a, View v,
					int position, long i) {
				Object value = getValueAtPosition(position);
				//	Set Context
				DisplayType.setContextValue(getContext(), getActivityNo(), getTabNo(), m_field, value);
				//	Listener
				event();
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				//	
			}
    	});
		//	
		v_Spinner.setClickable(true);
		v_Spinner.setLongClickable(true);
		v_Spinner.setOnLongClickListener(new OnLongClickListener() {
			
			@Override
			public boolean onLongClick(View v) {
				//	Re-Query
				m_Lookup.load(true);
				populate();
				return false;
			}
		});
	}
	
	/**
	 * Listener for event
	 * @author Yamel Senih, ysenih@erpcya.com, ERPCyA http://www.erpcya.com
	 * @return void
	 */
	private void event(){
        //	Listener
        if(m_Listener != null)
        	m_Listener.onFieldEvent(this);
	}
	
	/**
	 * Get position from value
	 * @author Yamel Senih, ysenih@erpcya.com, ERPCyA http://www.erpcya.com
	 * @param value
	 * @return
	 * @return int
	 */
	private int getPosition(Object value){
		int pos = -1;
		for(int i = 0; i < v_Spinner.getCount(); i++){
			DisplayLookupSpinner ds = (DisplayLookupSpinner) v_Spinner.getItemAtPosition(i);
			if(m_field.DisplayType == DisplayType.LIST){
				if(ds.getIDToString() != null
						&& ds.getIDToString().equals((String) value)){
					pos = i;
					break;	
				}
			} else {
				//	Only Integer
				if(!(value instanceof Integer))
					continue;
				//	
				if(ds.getIDAsInteger() == (Integer)value){
					pos = i;
					break;
				}	
			}
		}
		//	
		return pos;
	}

	/**
	 * Set Value and not reload
	 * @author Yamel Senih, ysenih@erpcya.com, ERPCyA http://www.erpcya.com
	 * @param value
	 * @return void
	 */
	public void setValueNoReload(Object value) {
		//	Valid 
		Object m_CurrentValue = getValue();
		if(value != null 
					&& m_CurrentValue != null
					&& value.equals(m_CurrentValue)) {
			return;
		}
		//	Set Old Value
		m_OldValue = getValue();
		//	
		if(value == null) {
			//	
			if(v_Spinner.getAdapter() != null
					&& !v_Spinner.getAdapter().isEmpty())
				v_Spinner.setSelection(0);
			return;
		}
		int pos = getPosition(value);
		//	
		if(pos > -1)
			v_Spinner.setSelection(pos);
		else
			v_Spinner.setSelection(0);
	}
	
	@Override
	public void setValue(Object value) {
		//	Set Old Value
		Object m_CurrentValue = getValue();
		if(value != null 
					&& m_CurrentValue != null
					&& value.equals(m_CurrentValue)) {
			return;
		}
		//	Save Old Value
		m_OldValue = getValue();
		//	
		if(value == null) {
			//	
			if(v_Spinner.getAdapter() != null
					&& !v_Spinner.getAdapter().isEmpty())
				v_Spinner.setSelection(0);
			return;
		}
		int pos = getPosition(value);
		//	Reload
		if(pos == -1
				&& m_Lookup != null) {
			//	Load
			m_Lookup.load(true);
			populate();
			//	Set Value
			pos = getPosition(value);
		}
		//	
		if(pos > 0)
			v_Spinner.setSelection(pos);
	}

	@Override
	public Object getValue() {
		DisplayLookupSpinner item = (DisplayLookupSpinner) v_Spinner.getSelectedItem();
		if(item != null) {
			if(m_field.DisplayType != DisplayType.LIST)
				return item.getIDAsInteger();
			else
				return item.getIDToString();
		}
		return null;
	}

	@Override
	public Object getOldValue() {
		if(m_OldValue != null) {
			if(m_field.DisplayType != DisplayType.LIST) { 
				if(String.valueOf(m_OldValue).length() > 0
						&& m_OldValue instanceof Integer)
					return (Integer) m_OldValue;
			} else if(String.valueOf(m_OldValue).length() > 0) {
				return String.valueOf(m_OldValue);
			}
		}
		return null;
	}
	
	
	/**
	 * Get value at position
	 * @author Yamel Senih, ysenih@erpcya.com, ERPCyA http://www.erpcya.com
	 * @param position
	 * @return
	 * @return Object
	 */
	private Object getValueAtPosition(int position) {
		//	Valid no Adapter
		if(v_Spinner.getAdapter() == null
				|| v_Spinner.getAdapter().isEmpty())
			return null;
		//	Get Value
		DisplayLookupSpinner item = (DisplayLookupSpinner) v_Spinner.getItemAtPosition(position);
		if(item != null) {
			if(m_field.DisplayType != DisplayType.LIST)
				return item.getIDAsInteger();
			else
				return item.getIDToString();
		}
		//	Return
		return null;
	}
	
	@Override
	public boolean isEmpty() {
		Object value = getValue();
		if(value != null){
			if(value instanceof Integer){
				if(((Integer)value) > -1)
					return false;
			} else if(value instanceof String){
				if(String.valueOf(value).length() != 0)
					return false;
			}
		}
		return true;
	}

	@Override
	public View getChildView() {
		return v_Spinner;
	}
	
	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		v_Spinner.setEnabled(enabled);
	}

	@Override
	public String getDisplayValue() {
		//	Get Value
		DisplayLookupSpinner item = (DisplayLookupSpinner) v_Spinner.getSelectedItem();
		//	
		if(item != null)
			return item.getValueAsString();
		return null;
	}
	
	/**
	 * Set Lookup
	 * @author Yamel Senih, ysenih@erpcya.com, ERPCyA http://www.erpcya.com
	 * @param m_lookup
	 * @return void
	 */
	public void setLookup(Lookup m_lookup){
		this.m_Lookup = m_lookup;
	}
	
	/**
	 * Load Data
	 * @author Yamel Senih, ysenih@erpcya.com, ERPCyA http://www.erpcya.com
	 * @param reQuery
	 * @return void
	 */
	public void load(boolean reQuery) {
		m_Lookup.load(reQuery);
		populate();
	}
	
	@Override
	public String getValidation() {
		String whereClause = m_Lookup.getValidation();
		LogM.log(getContext(), getClass(), Level.FINE, "Where Clause = " + m_Lookup.getValidation());
		return whereClause;
	}
	
	/**
	 * Set adapter
	 * @author <a href="mailto:yamelsenih@gmail.com">Yamel Senih</a> 19/05/2014, 09:37:16
	 * @param v_Spinner
	 * @return void
	 */
	private void populate(){
		//	Set Adapter
		SpinnerLookupAdapter sp_adapter = 
    			new SpinnerLookupAdapter(getContext(), m_Lookup.getData());
		//	
		v_Spinner.setAdapter(sp_adapter);
	}

	@Override
	public void setValueAndOldValue(Object value) {
		setValue(value);
		m_OldValue = getValue();
	}
}
