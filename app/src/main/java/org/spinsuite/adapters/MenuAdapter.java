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
package org.spinsuite.adapters;

import java.util.ArrayList;

import org.spinsuite.base.R;
import org.spinsuite.util.DisplayMenuItem;
import org.spinsuite.util.Env;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * @author Yamel Senih, ysenih@erpcya.com, ERPCyA http://www.erpcya.com
 *
 */
public class MenuAdapter extends ArrayAdapter<DisplayMenuItem> {

	/**
	 * 
	 * *** Constructor ***
	 * @author Yamel Senih, ysenih@erpcya.com, ERPCyA http://www.erpcya.com 04/02/2014, 17:06:03
	 * @param ctx
	 * @param id_View
	 * @param data
	 */
	public MenuAdapter(Context ctx, int id_View, ArrayList<DisplayMenuItem> data) {
		super(ctx, id_View, data);
		this.ctx = ctx;
		this.data = data;
		this.id_View = id_View;
		//	Get Preferred Height
		TypedValue value = Env.getResource(ctx, android.R.attr.listPreferredItemHeight);
		DisplayMetrics displayMetrics = new DisplayMetrics();
		((WindowManager)(ctx.getSystemService(Context.WINDOW_SERVICE)))
				.getDefaultDisplay().getMetrics(displayMetrics);
		height = value.getDimension(displayMetrics);
	}
	
	/**
	 * 
	 * *** Constructor ***
	 * @author Yamel Senih, ysenih@erpcya.com, ERPCyA http://www.erpcya.com 15/03/2014, 09:16:14
	 * @param ctx
	 * @param id_View
	 * @param isMenu
	 * @param data
	 */
	public MenuAdapter(Context ctx, int id_View, boolean isMenu, ArrayList<DisplayMenuItem> data) {
		super(ctx, id_View, data);
		this.ctx = ctx;
		this.data = data;
		this.id_View = id_View;
		this.isMenu = isMenu;
		//	Get Preferred Height
		TypedValue value = Env.getResource(ctx, (isMenu
													? android.R.attr.listPreferredItemHeight
													: android.R.attr.listPreferredItemHeightSmall));
		DisplayMetrics displayMetrics = new DisplayMetrics();
		((WindowManager)(ctx.getSystemService(Context.WINDOW_SERVICE)))
				.getDefaultDisplay().getMetrics(displayMetrics);
		height = value.getDimension(displayMetrics);
	}

	/**	Context						*/
	private Context 					ctx;
	/**	Data						*/
	private ArrayList<DisplayMenuItem> 	data;
	/**	Original Data				*/
	private ArrayList<DisplayMenuItem> 	originalData;
	/**	Identifier of View			*/
	private int 						id_View;
	/**	Is Activity Menu			*/
	private boolean 					isMenu = false;
	/**	Preferred Item Height		*/
	private float						height = 0;
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View item = convertView;
		if(item == null) {
			LayoutInflater inflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			item = inflater.inflate(id_View, null);
			item.setMinimumHeight((int)height);
		}
		
		DisplayMenuItem mi = data.get(position);
		
		//	Set Name
		TextView tv_Name = (TextView)item.findViewById(R.id.tv_Name);
		tv_Name.setText(mi.getName());		
		//	
		if(isMenu) {	//	Main Menu
			//	Set Text Style
			tv_Name.setTextAppearance(ctx, R.style.TextItemMenu);
			//	Set Description
			TextView tv_Description = (TextView)item.findViewById(R.id.tv_Description);
			tv_Description.setText(mi.getDescription());
			tv_Description.setTextAppearance(ctx, R.style.TextItemSmallMenu);
			//	Set Image
			ImageView img_Menu = (ImageView)item.findViewById(R.id.img_Item);
			if(mi.getImageURL() != null 
					&& mi.getImageURL().length() > 0) {
				Resources res = ctx.getResources();
				int resID = res.getIdentifier(mi.getImageURL() , "drawable", ctx.getPackageName());
				if(resID != 0) {
					Drawable drawable = res.getDrawable(resID);
					img_Menu.setImageDrawable(drawable);
				}
			} else if(mi.isSummary()) {
				img_Menu.setImageResource(
						Env.getResourceID(ctx, R.attr.ic_ml_folder));
			} else {
				if(mi.getAction().equals(DisplayMenuItem.ACTION_Form)
						|| mi.getAction().equals(DisplayMenuItem.ACTION_Window)) {
					img_Menu.setImageResource(
							Env.getResourceID(ctx, R.attr.ic_ml_window));
				} else if(mi.getAction().equals(DisplayMenuItem.ACTION_Process)) {
					img_Menu.setImageResource(
							Env.getResourceID(ctx, R.attr.ic_ml_process));
				} else if(mi.getAction().equals(DisplayMenuItem.ACTION_Report)) {
					img_Menu.setImageResource(
							Env.getResourceID(ctx, R.attr.ic_ml_report));
				}
				else if(mi.getAction().equals(DisplayMenuItem.ACTION_WSDownload)) {
					img_Menu.setImageResource(
							Env.getResourceID(ctx, R.attr.ic_ml_download));
				}
				else if(mi.getAction().equals(DisplayMenuItem.ACTION_WSUpload)) {
					img_Menu.setImageResource(
							Env.getResourceID(ctx, R.attr.ic_ml_upload));
				}
			}
		} else {		//	Drawer Menu
			//	Set Text Style
			tv_Name.setTextAppearance(ctx, R.style.TextItemDrawerMenu);
			//	
			if(mi.getImageURL() != null 
					&& mi.getImageURL().length() > 0) {
				Resources res = ctx.getResources();
				int resID = res.getIdentifier(mi.getImageURL() , "drawable", ctx.getPackageName());
				if(resID != 0) {
					tv_Name.setCompoundDrawablesWithIntrinsicBounds(resID, 0, 0, 0);
				}
			} if(mi.isSummary()) {
				tv_Name.setCompoundDrawablesWithIntrinsicBounds(
						Env.getResourceID(ctx, R.attr.ic_dr_folder), 0, 0, 0);
			} else {
				if(mi.getAction().equals(DisplayMenuItem.ACTION_Form)
						|| mi.getAction().equals(DisplayMenuItem.ACTION_Window)) {
					tv_Name.setCompoundDrawablesWithIntrinsicBounds(
							Env.getResourceID(ctx, R.attr.ic_dr_window), 0, 0, 0);
				} else if(mi.getAction().equals(DisplayMenuItem.ACTION_Process)) {
					tv_Name.setCompoundDrawablesWithIntrinsicBounds(
							Env.getResourceID(ctx, R.attr.ic_dr_process), 0, 0, 0);
				} else if(mi.getAction().equals(DisplayMenuItem.ACTION_Report)) {
					tv_Name.setCompoundDrawablesWithIntrinsicBounds(
							Env.getResourceID(ctx, R.attr.ic_dr_report), 0, 0, 0);
				}
			}
		}
		//	Return
		return item;
	}
	
	@Override
	public Filter getFilter() {
	    return new Filter() {
	        @SuppressWarnings("unchecked")
	        @Override
	        protected void publishResults(CharSequence constraint, FilterResults results) {
	            data = (ArrayList<DisplayMenuItem>) results.values;
	            if (results.count > 0) {
	            	notifyDataSetChanged();
	            } else {
	            	notifyDataSetInvalidated();
	            }  
	        }

	        @Override
	        protected FilterResults performFiltering(CharSequence constraint) {
	            //	Populate Original Data
	        	if(originalData == null)
	            	originalData = data;
	        	//	Get filter result
	        	ArrayList<DisplayMenuItem> filteredResults = getResults(constraint);
	            //	Result
	            FilterResults results = new FilterResults();
	            //	
	            results.values = filteredResults;
	            results.count = filteredResults.size();
	            //	
	            return results;
	        }

	        /**
	         * Search
	         * @author Yamel Senih, ysenih@erpcya.com, ERPCyA http://www.erpcya.com 02/03/2014, 03:19:33
	         * @param constraint
	         * @return
	         * @return ArrayList<DisplayMenuItem>
	         */
	        private ArrayList<DisplayMenuItem> getResults(CharSequence constraint) {
	        	//	Verify
	            if(constraint != null
	            		&& constraint.length() > 0) {
	            	//	new Filter
	            	ArrayList<DisplayMenuItem> filteredResult = new ArrayList<DisplayMenuItem>();
	                for(DisplayMenuItem item : originalData) {
	                    if((item.getName() != null 
	                    		&& item.getName().toLowerCase(Env.getLocate())
	                    					.contains(constraint.toString().toLowerCase(Env.getLocate())))
	                    	|| (item.getDescription() != null 
		                    		&& item.getDescription().toLowerCase(Env.getLocate())
                					.contains(constraint.toString().toLowerCase(Env.getLocate()))))
	                        filteredResult.add(item);
	                }
	                return filteredResult;
	            }
	            //	Only Data
	            return originalData;
	        }
	    };
	}
	
	@Override
	public int getCount() {
		return data.size();
	}
	
	@Override
	public DisplayMenuItem getItem(int position) {
		return data.get(position);
	}
}
