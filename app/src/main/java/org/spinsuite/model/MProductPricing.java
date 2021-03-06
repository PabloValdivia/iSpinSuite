/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 1999-2006 ComPiere, Inc. All Rights Reserved.                *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA 95054, USA        *
 * or via info@compiere.org or http://www.compiere.org/license.html           *
 *****************************************************************************/
package org.spinsuite.model;

import java.math.BigDecimal;
import java.util.Date;
import java.util.logging.Level;

import org.spinsuite.base.DB;
import org.spinsuite.util.Env;
import org.spinsuite.util.LogM;

import android.content.Context;
import android.database.Cursor;

/**
 *  Product Price Calculations
 *
 *  @author Jorg Janke
 *  @version $Id: MProductPricing.java,v 1.2 2006/07/30 00:51:02 jjanke Exp $
 */
public class MProductPricing
{

	/**
	 * 	Constructor
	 * 	@param M_Product_ID product
	 * 	@param C_BPartner_ID partner
	 * 	@param Qty quantity
	 * 	@param isSOTrx SO or PO
	 */
	public MProductPricing (Context ctx, int M_Product_ID, int C_BPartner_ID, 
		BigDecimal Qty, boolean isSOTrx)
	{
		m_ctx = ctx;
		m_M_Product_ID = M_Product_ID;
		m_C_BPartner_ID = C_BPartner_ID;
		if (Qty != null && Env.ZERO.compareTo(Qty) != 0)
			m_Qty = Qty;
		m_isSOTrx = isSOTrx;
		int thereAreVendorBreakRecords = DB.getSQLValue(ctx, 
				"SELECT count(M_Product_ID) FROM M_ProductPriceVendorBreak "
				+ "WHERE M_Product_ID=? AND C_BPartner_ID=?", 
						new String[] {String.valueOf(m_M_Product_ID), String.valueOf(m_C_BPartner_ID)});
		m_useVendorBreak = thereAreVendorBreakRecords > 0;
	}	//	MProductPricing

	private Context				m_ctx;
	private int 				m_M_Product_ID;
	private int 				m_C_BPartner_ID;
	private BigDecimal 			m_Qty = Env.ONE;
	private boolean				m_isSOTrx = true;
	//
	private int					m_M_PriceList_ID = 0;
	private int 				m_M_PriceList_Version_ID = 0;
	private Date 				m_PriceDate;	
	/** Precision -1 = no rounding		*/
	private int		 			m_precision = -1;
	
	
	private boolean 			m_calculated = false;
	private boolean 			m_vendorbreak = false;
	private boolean 			m_useVendorBreak;
	private Boolean				m_found = null;
	
	private BigDecimal 			m_PriceList = Env.ZERO;
	private BigDecimal 			m_PriceStd = Env.ZERO;
	private BigDecimal 			m_PriceLimit = Env.ZERO;
	private int 				m_C_Currency_ID = 0;
	private boolean				m_enforcePriceLimit = false;
	private int 				m_C_UOM_ID = 0;
	private int 				m_M_Product_Category_ID;
	private boolean				m_discountSchema = false;
	private boolean				m_isTaxIncluded = false;
	
	/**
	 * 	Calculate Price
	 * 	@return true if calculated
	 */
	public boolean calculatePrice ()
	{
		if (m_M_Product_ID == 0 
			|| (m_found != null && !m_found.booleanValue()))	//	previously not found
			return false;
		
		if (m_useVendorBreak) {
			//	Price List Version known - vendor break
			if (!m_calculated) {
				m_calculated = calculatePLV_VB ();
				if (m_calculated)
					m_vendorbreak = true;
			}
			//	Price List known - vendor break
			if (!m_calculated) {
				m_calculated = calculatePL_VB();
				if (m_calculated)
					m_vendorbreak = true;
			}
			//	Base Price List used - vendor break
			if (!m_calculated) {
				m_calculated = calculateBPL_VB();
				if (m_calculated)
					m_vendorbreak = true;
			}
		}
		
		//	Price List Version known
		if (!m_calculated)
			m_calculated = calculatePLV ();
		//	Price List known
		if (!m_calculated)
			m_calculated = calculatePL();
		//	Base Price List used
		if (!m_calculated)
			m_calculated = calculateBPL();
		//	Set UOM, Prod.Category
		if (!m_calculated)
			setBaseInfo();
		//	User based Discount
		if (m_calculated && !m_vendorbreak)
			calculateDiscount();
		//setPrecision();		//	from Price List
		//
		m_found = m_calculated;
		return m_calculated;
	}	//	calculatePrice

	/**
	 * 	Calculate Price based on Price List Version
	 * 	@return true if calculated
	 */
	private boolean calculatePLV()
	{
		if (m_M_Product_ID == 0 || m_M_PriceList_Version_ID == 0)
			return false;
		//
		String sql = "SELECT pp.PriceStd,"	//	1
			+ " pp.PriceList,"		//	2
			+ " pp.PriceLimit,"	//	3
			+ " p.C_UOM_ID,"
			+ "(strftime('%s',  pv.ValidFrom)*1000) ValidFrom, "
			+ "pl.C_Currency_ID,p.M_Product_Category_ID,"	//	4..7
			+ " pl.EnforcePriceLimit, pl.IsTaxIncluded "	// 8..9
			+ "FROM M_Product p"
			+ " INNER JOIN M_ProductPrice pp ON (p.M_Product_ID=pp.M_Product_ID)"
			+ " INNER JOIN  M_PriceList_Version pv ON (pp.M_PriceList_Version_ID=pv.M_PriceList_Version_ID)"
			+ " INNER JOIN M_Pricelist pl ON (pv.M_PriceList_ID=pl.M_PriceList_ID) "
			+ "WHERE pv.IsActive='Y'"
			+ " AND pp.IsActive='Y'"
			+ " AND p.M_Product_ID=?"				//	#1
			+ " AND pv.M_PriceList_Version_ID=?";	//	#2
		m_calculated = false;
		DB conn = new DB(m_ctx);
		Cursor rs = null;
		try
		{
			DB.loadConnection(conn, DB.READ_ONLY);
			conn.compileQuery(sql.toString());
			conn.addInt(m_M_Product_ID);
			conn.addInt(m_M_PriceList_Version_ID);
			rs = conn.querySQL();
			if (rs.moveToFirst())
			{
				//	Prices
				double m_DoublePriceStd = rs.getDouble(rs.getColumnIndex("PriceStd"));
				m_PriceStd = new BigDecimal(m_DoublePriceStd);
				double m_DoublePriceList = rs.getDouble(rs.getColumnIndex("PriceList"));
				m_PriceList = new BigDecimal(m_DoublePriceList);
				double m_DoublePriceLimit = rs.getDouble(rs.getColumnIndex("PriceLimit"));
				m_PriceLimit = new BigDecimal(m_DoublePriceLimit);
				//
				m_C_UOM_ID = rs.getInt(rs.getColumnIndex("C_UOM_ID"));
				m_C_Currency_ID = rs.getInt(rs.getColumnIndex("C_Currency_ID"));
				m_M_Product_Category_ID = rs.getInt(rs.getColumnIndex("M_Product_Category_ID"));
				m_enforcePriceLimit = "Y".equals(rs.getString(rs.getColumnIndex("EnforcePriceLimit")));
				m_isTaxIncluded = "Y".equals(rs.getString(rs.getColumnIndex("IsTaxIncluded")));
				//
				LogM.log(m_ctx, getClass(), Level.FINE, 
						"M_PriceList_Version_ID=" + m_M_PriceList_Version_ID + " - " + m_PriceStd);
				m_calculated = true;
			}
		} catch (Exception e) {
			LogM.log(m_ctx, getClass(), Level.SEVERE, sql, e); 
			m_calculated = false;
		} finally {
			DB.closeConnection(conn);
		}
		//	
		return m_calculated;
	}	//	calculatePLV

	/**
	 * 	Calculate Price based on Price List
	 * 	@return true if calculated
	 */
	private boolean calculatePL()
	{
		if (m_M_Product_ID == 0)
			return false;

		if (m_M_PriceList_ID == 0) {
			LogM.log(m_ctx, getClass(), Level.SEVERE, "No PriceList");
			return false;
		}

		//	Get Prices for Price List
		String sql = "SELECT pp.PriceStd,"	//	1
			+ " pp.PriceList,"		//	2
			+ " pp.PriceLimit,"	//	3
			+ " p.C_UOM_ID,"
			+ "(strftime('%s',  pv.ValidFrom)*1000) ValidFrom, "
			+ "pl.C_Currency_ID,p.M_Product_Category_ID,pl.EnforcePriceLimit "	// 4..8
			+ "FROM M_Product p"
			+ " INNER JOIN M_ProductPrice pp ON (p.M_Product_ID=pp.M_Product_ID)"
			+ " INNER JOIN  M_PriceList_Version pv ON (pp.M_PriceList_Version_ID=pv.M_PriceList_Version_ID)"
			+ " INNER JOIN M_Pricelist pl ON (pv.M_PriceList_ID=pl.M_PriceList_ID) "
			+ "WHERE pv.IsActive='Y'"
			+ " AND pp.IsActive='Y'"
			+ " AND p.M_Product_ID=?"				//	#1
			+ " AND pv.M_PriceList_ID=?"			//	#2
			+ " ORDER BY pv.ValidFrom DESC";
		m_calculated = false;
		if (m_PriceDate == null)
			m_PriceDate = new Date(System.currentTimeMillis());
		DB conn = new DB(m_ctx);
		Cursor rs = null;
		try
		{
			DB.loadConnection(conn, DB.READ_ONLY);
			conn.compileQuery(sql.toString());
			conn.addInt(m_M_Product_ID);
			conn.addInt(m_M_PriceList_ID);
			rs = conn.querySQL();
			while (!m_calculated && rs.moveToFirst())
			{
				long longDate = rs.getLong(rs.getColumnIndex("ValidFrom"));
				Date plDate = new Date(longDate);
				//	we have the price list
				//	if order date is after or equal PriceList validFrom
				if (plDate == null || !m_PriceDate.before(plDate))
				{
					//	Prices
					double m_DoublePriceStd = rs.getDouble(rs.getColumnIndex("PriceStd"));
					m_PriceStd = new BigDecimal(m_DoublePriceStd);
					double m_DoublePriceList = rs.getDouble(rs.getColumnIndex("PriceList"));
					m_PriceList = new BigDecimal(m_DoublePriceList);
					double m_DoublePriceLimit = rs.getDouble(rs.getColumnIndex("PriceLimit"));
					m_PriceLimit = new BigDecimal(m_DoublePriceLimit);
					//
					m_C_UOM_ID = rs.getInt(rs.getColumnIndex("C_UOM_ID"));
					m_C_Currency_ID = rs.getInt(rs.getColumnIndex("C_Currency_ID"));
					m_M_Product_Category_ID = rs.getInt(rs.getColumnIndex("M_Product_Category_ID"));
					m_enforcePriceLimit = "Y".equals(rs.getString(rs.getColumnIndex("EnforcePriceLimit")));
					//
					LogM.log(m_ctx, getClass(), Level.FINE, 
							"M_PriceList_ID=" + m_M_PriceList_ID 
							+ "(" + plDate + ")" + " - " + m_PriceStd);
					m_calculated = true;
					break;
				}
			}
		} catch (Exception e) {
			LogM.log(m_ctx, getClass(), Level.SEVERE, sql, e); 
			m_calculated = false;
		} finally {
			DB.closeConnection(conn);
		}
		if (!m_calculated)
			LogM.log(m_ctx, getClass(), Level.FINE, "Not found (PL)");
		return m_calculated;
	}	//	calculatePL

	/**
	 * 	Calculate Price based on Base Price List
	 * 	@return true if calculated
	 */
	private boolean calculateBPL()
	{
		if (m_M_Product_ID == 0 || m_M_PriceList_ID == 0)
			return false;
		//
		String sql = "SELECT pp.PriceStd,"	//	1
			+ " pp.PriceList,"		//	2
			+ " pp.PriceLimit,"	//	3
			+ " p.C_UOM_ID,"
			+ "(strftime('%s',  pv.ValidFrom)*1000) ValidFrom, "
			+ "pl.C_Currency_ID,p.M_Product_Category_ID,"	//	4..7
			+ " pl.EnforcePriceLimit, pl.IsTaxIncluded "	// 8..9
			+ "FROM M_Product p"
			+ " INNER JOIN M_ProductPrice pp ON (p.M_Product_ID=pp.M_Product_ID)"
			+ " INNER JOIN  M_PriceList_Version pv ON (pp.M_PriceList_Version_ID=pv.M_PriceList_Version_ID)"
			+ " INNER JOIN M_Pricelist bpl ON (pv.M_PriceList_ID=bpl.M_PriceList_ID)"
			+ " INNER JOIN M_Pricelist pl ON (bpl.M_PriceList_ID=pl.BasePriceList_ID) "
			+ "WHERE pv.IsActive='Y'"
			+ " AND pp.IsActive='Y'"
			+ " AND p.M_Product_ID=?"				//	#1
			+ " AND pl.M_PriceList_ID=?"			//	#2
			+ " ORDER BY pv.ValidFrom DESC";
		m_calculated = false;
		if (m_PriceDate == null)
			m_PriceDate = new Date (System.currentTimeMillis());
		DB conn = new DB(m_ctx);
		Cursor rs = null;
		try
		{
			DB.loadConnection(conn, DB.READ_ONLY);
			conn.compileQuery(sql.toString());
			conn.addInt(m_M_Product_ID);
			conn.addInt(m_M_PriceList_ID);
			rs = conn.querySQL();
			while (!m_calculated && rs.moveToFirst())
			{
				long longDate = rs.getLong(rs.getColumnIndex("ValidFrom"));
				Date plDate = new Date(longDate);
				//	we have the price list
				//	if order date is after or equal PriceList validFrom
				if (plDate == null || !m_PriceDate.before(plDate))
				{
					//	Prices
					double m_DoublePriceStd = rs.getDouble(rs.getColumnIndex("PriceStd"));
					m_PriceStd = new BigDecimal(m_DoublePriceStd);
					double m_DoublePriceList = rs.getDouble(rs.getColumnIndex("PriceList"));
					m_PriceList = new BigDecimal(m_DoublePriceList);
					double m_DoublePriceLimit = rs.getDouble(rs.getColumnIndex("PriceLimit"));
					m_PriceLimit = new BigDecimal(m_DoublePriceLimit);
					//
					m_C_UOM_ID = rs.getInt(rs.getColumnIndex("C_UOM_ID"));
					m_C_Currency_ID = rs.getInt(rs.getColumnIndex("C_Currency_ID"));
					m_M_Product_Category_ID = rs.getInt(rs.getColumnIndex("M_Product_Category_ID"));
					m_enforcePriceLimit = "Y".equals(rs.getString(rs.getColumnIndex("EnforcePriceLimit")));
					m_isTaxIncluded = "Y".equals(rs.getString(rs.getColumnIndex("IsTaxIncluded")));
					//
					LogM.log(m_ctx, getClass(), Level.FINE, 
							"M_PriceList_ID=" + m_M_PriceList_ID 
							+ "(" + plDate + ")" + " - " + m_PriceStd);
					m_calculated = true;
					break;
				}
			}
		} catch (Exception e) {
			LogM.log(m_ctx, getClass(), Level.SEVERE, sql, e); 
			m_calculated = false;
		} finally {
			DB.closeConnection(conn);
		}
		if (!m_calculated)
			LogM.log(m_ctx, getClass(), Level.FINE, "Not found (BPL)");
		return m_calculated;
	}	//	calculateBPL

	/**
	 * 	Calculate Price based on Price List Version and Vendor Break
	 * 	@return true if calculated
	 */
	private boolean calculatePLV_VB()
	{
		if (m_M_Product_ID == 0 || m_M_PriceList_Version_ID == 0)
			return false;
		//
		String sql = "SELECT pp.PriceStd,"	//	1
			+ " pp.PriceList,"		//	2
			+ " pp.PriceLimit,"	//	3
			+ " p.C_UOM_ID,"
			+ "(strftime('%s',  pv.ValidFrom)*1000) ValidFrom, "
			+ "pl.C_Currency_ID,p.M_Product_Category_ID,"	//	4..7
			+ " pl.EnforcePriceLimit, pl.IsTaxIncluded "	// 8..9
			+ "FROM M_Product p"
			+ " INNER JOIN M_ProductPriceVendorBreak pp ON (p.M_Product_ID=pp.M_Product_ID)"
			+ " INNER JOIN  M_PriceList_Version pv ON (pp.M_PriceList_Version_ID=pv.M_PriceList_Version_ID)"
			+ " INNER JOIN M_Pricelist pl ON (pv.M_PriceList_ID=pl.M_PriceList_ID) "
			+ "WHERE pv.IsActive='Y'"
			+ " AND pp.IsActive='Y'"
			+ " AND p.M_Product_ID=?"				//	#1
			+ " AND pv.M_PriceList_Version_ID=?"	//	#2
			+ " AND pp.C_BPartner_ID=?"				//	#3
			+ " AND ?>=pp.BreakValue"				//  #4
			+ " ORDER BY BreakValue DESC";
		m_calculated = false;
		if (m_PriceDate == null)
			m_PriceDate = new Date (System.currentTimeMillis());
		DB conn = new DB(m_ctx);
		Cursor rs = null;
		try
		{
			DB.loadConnection(conn, DB.READ_ONLY);
			conn.compileQuery(sql.toString());
			conn.addInt(m_M_Product_ID);
			conn.addInt(m_M_PriceList_Version_ID);
			conn.addInt(m_C_BPartner_ID);
			rs = conn.querySQL();
			while (!m_calculated && rs.moveToFirst())
			{
				long longDate = rs.getLong(rs.getColumnIndex("ValidFrom"));
				Date plDate = new Date(longDate);
				//	we have the price list
				//	if order date is after or equal PriceList validFrom
				if (plDate == null || !m_PriceDate.before(plDate))
				{
					//	Prices
					double m_DoublePriceStd = rs.getDouble(rs.getColumnIndex("PriceStd"));
					m_PriceStd = new BigDecimal(m_DoublePriceStd);
					double m_DoublePriceList = rs.getDouble(rs.getColumnIndex("PriceList"));
					m_PriceList = new BigDecimal(m_DoublePriceList);
					double m_DoublePriceLimit = rs.getDouble(rs.getColumnIndex("PriceLimit"));
					m_PriceLimit = new BigDecimal(m_DoublePriceLimit);
					//
					m_C_UOM_ID = rs.getInt(rs.getColumnIndex("C_UOM_ID"));
					m_C_Currency_ID = rs.getInt(rs.getColumnIndex("C_Currency_ID"));
					m_M_Product_Category_ID = rs.getInt(rs.getColumnIndex("M_Product_Category_ID"));
					m_enforcePriceLimit = "Y".equals(rs.getString(rs.getColumnIndex("EnforcePriceLimit")));
					m_isTaxIncluded = "Y".equals(rs.getString(rs.getColumnIndex("IsTaxIncluded")));
					//
					LogM.log(m_ctx, getClass(), Level.FINE, 
							"M_PriceList_ID=" + m_M_PriceList_ID 
							+ "(" + plDate + ")" + " - " + m_PriceStd);
					m_calculated = true;
					break;
				}
			}
		} catch (Exception e) {
			LogM.log(m_ctx, getClass(), Level.SEVERE, sql, e); 
			m_calculated = false;
		} finally {
			DB.closeConnection(conn);
		}
		if (!m_calculated)
			LogM.log(m_ctx, getClass(), Level.FINE, "Not found (BPL)");
		return m_calculated;
	}	//	calculatePLV_VB

	/**
	 * 	Calculate Price based on P rice List and Vendor break
	 * 	@return true if calculated
	 */
	private boolean calculatePL_VB()
	{
		if (m_M_Product_ID == 0)
			return false;

		//	Get Price List
		if (m_M_PriceList_ID == 0) {
			LogM.log(m_ctx, getClass(), Level.SEVERE, "No PriceList");
			return false;
		}

		//	Get Prices for Price List
		String sql = "SELECT pp.PriceStd,"	//	1
			+ " pp.PriceList,"		//	2
			+ " pp.PriceLimit,"	//	3
			+ " p.C_UOM_ID,"
			+ "(strftime('%s',  pv.ValidFrom)*1000) ValidFrom, "
			+ "pl.C_Currency_ID,p.M_Product_Category_ID,pl.EnforcePriceLimit "	// 4..8
			+ "FROM M_Product p"
			+ " INNER JOIN M_ProductPriceVendorBreak pp ON (p.M_Product_ID=pp.M_Product_ID)"
			+ " INNER JOIN  M_PriceList_Version pv ON (pp.M_PriceList_Version_ID=pv.M_PriceList_Version_ID)"
			+ " INNER JOIN M_Pricelist pl ON (pv.M_PriceList_ID=pl.M_PriceList_ID) "
			+ "WHERE pv.IsActive='Y'"
			+ " AND pp.IsActive='Y'"
			+ " AND p.M_Product_ID=?"				//	#1
			+ " AND pv.M_PriceList_ID=?"			//	#2
			+ " AND pp.C_BPartner_ID=?"				//	#3
			+ " AND ?>=pp.BreakValue"				//  #4
			+ " ORDER BY pv.ValidFrom DESC, BreakValue DESC";
		m_calculated = false;
		if (m_PriceDate == null)
			m_PriceDate = new Date(System.currentTimeMillis());
		DB conn = new DB(m_ctx);
		Cursor rs = null;
		try
		{
			DB.loadConnection(conn, DB.READ_ONLY);
			conn.compileQuery(sql.toString());
			conn.addInt(m_M_Product_ID);
			conn.addInt(m_M_PriceList_ID);
			conn.addInt(m_C_BPartner_ID);
			conn.addDouble(m_Qty.doubleValue());
			rs = conn.querySQL();
			while (!m_calculated && rs.moveToFirst())
			{
				long longDate = rs.getLong(rs.getColumnIndex("ValidFrom"));
				Date plDate = new Date(longDate);
				//	we have the price list
				//	if order date is after or equal PriceList validFrom
				if (plDate == null || !m_PriceDate.before(plDate))
				{
					//	Prices
					double m_DoublePriceStd = rs.getDouble(rs.getColumnIndex("PriceStd"));
					m_PriceStd = new BigDecimal(m_DoublePriceStd);
					double m_DoublePriceList = rs.getDouble(rs.getColumnIndex("PriceList"));
					m_PriceList = new BigDecimal(m_DoublePriceList);
					double m_DoublePriceLimit = rs.getDouble(rs.getColumnIndex("PriceLimit"));
					m_PriceLimit = new BigDecimal(m_DoublePriceLimit);
					//
					m_C_UOM_ID = rs.getInt(rs.getColumnIndex("C_UOM_ID"));
					m_C_Currency_ID = rs.getInt(rs.getColumnIndex("C_Currency_ID"));
					m_M_Product_Category_ID = rs.getInt(rs.getColumnIndex("M_Product_Category_ID"));
					m_enforcePriceLimit = "Y".equals(rs.getString(rs.getColumnIndex("EnforcePriceLimit")));
					//
					LogM.log(m_ctx, getClass(), Level.FINE, 
							"M_PriceList_ID=" + m_M_PriceList_ID 
							+ "(" + plDate + ")" + " - " + m_PriceStd);
					m_calculated = true;
					break;
				}
			}
		} catch (Exception e) {
			LogM.log(m_ctx, getClass(), Level.SEVERE, sql, e); 
			m_calculated = false;
		} finally {
			DB.closeConnection(conn);
		}
		if (!m_calculated)
			LogM.log(m_ctx, getClass(), Level.FINE, "Not found (PL)");
		return m_calculated;
	}	//	calculatePL_VB

	/**
	 * 	Calculate Price based on Base Price List and Vendor Break
	 * 	@return true if calculated
	 */
	private boolean calculateBPL_VB()
	{
		if (m_M_Product_ID == 0 || m_M_PriceList_ID == 0)
			return false;
		//
		String sql = "SELECT pp.PriceStd,"	//	1
			+ " pp.PriceList,"		//	2
			+ " pp.PriceLimit,"	//	3
			+ " p.C_UOM_ID,"
			+ "(strftime('%s',  pv.ValidFrom)*1000) ValidFrom, "
			+ "pl.C_Currency_ID,p.M_Product_Category_ID,"	//	4..7
			+ " pl.EnforcePriceLimit, pl.IsTaxIncluded "	// 8..9
			+ "FROM M_Product p"
			+ " INNER JOIN M_ProductPriceVendorBreak pp ON (p.M_Product_ID=pp.M_Product_ID)"
			+ " INNER JOIN  M_PriceList_Version pv ON (pp.M_PriceList_Version_ID=pv.M_PriceList_Version_ID)"
			+ " INNER JOIN M_Pricelist bpl ON (pv.M_PriceList_ID=bpl.M_PriceList_ID)"
			+ " INNER JOIN M_Pricelist pl ON (bpl.M_PriceList_ID=pl.BasePriceList_ID) "
			+ "WHERE pv.IsActive='Y'"
			+ " AND pp.IsActive='Y'"
			+ " AND p.M_Product_ID=?"				//	#1
			+ " AND pl.M_PriceList_ID=?"			//	#2
			+ " AND pp.C_BPartner_ID=?"				//	#3
			+ " AND ?>=pp.BreakValue"				//  #4
			+ " ORDER BY pv.ValidFrom DESC, BreakValue DESC";
		m_calculated = false;
		if (m_PriceDate == null)
			m_PriceDate = new Date(System.currentTimeMillis());
		DB conn = new DB(m_ctx);
		Cursor rs = null;
		try
		{
			DB.loadConnection(conn, DB.READ_ONLY);
			rs = conn.querySQL(sql.toString(), 
					new String[]{String.valueOf(m_M_Product_ID), String.valueOf(m_M_PriceList_ID), 
										String.valueOf(m_C_BPartner_ID), String.valueOf(m_Qty.doubleValue())});
			while (!m_calculated && rs.moveToFirst())
			{
				long longDate = rs.getLong(rs.getColumnIndex("ValidFrom"));
				Date plDate = new Date(longDate);
				//	we have the price list
				//	if order date is after or equal PriceList validFrom
				if (plDate == null || !m_PriceDate.before(plDate))
				{
					//	Prices
					double m_DoublePriceStd = rs.getDouble(rs.getColumnIndex("PriceStd"));
					m_PriceStd = new BigDecimal(m_DoublePriceStd);
					double m_DoublePriceList = rs.getDouble(rs.getColumnIndex("PriceList"));
					m_PriceList = new BigDecimal(m_DoublePriceList);
					double m_DoublePriceLimit = rs.getDouble(rs.getColumnIndex("PriceLimit"));
					m_PriceLimit = new BigDecimal(m_DoublePriceLimit);
					//
					m_C_UOM_ID = rs.getInt(rs.getColumnIndex("C_UOM_ID"));
					m_C_Currency_ID = rs.getInt(rs.getColumnIndex("C_Currency_ID"));
					m_M_Product_Category_ID = rs.getInt(rs.getColumnIndex("M_Product_Category_ID"));
					m_enforcePriceLimit = "Y".equals(rs.getString(rs.getColumnIndex("EnforcePriceLimit")));
					m_isTaxIncluded = "Y".equals(rs.getString(rs.getColumnIndex("IsTaxIncluded")));
					//
					LogM.log(m_ctx, getClass(), Level.FINE, 
							"M_PriceList_ID=" + m_M_PriceList_ID 
							+ "(" + plDate + ")" + " - " + m_PriceStd);
					m_calculated = true;
					break;
				}
			}
		} catch (Exception e) {
			LogM.log(m_ctx, getClass(), Level.SEVERE, sql, e); 
			m_calculated = false;
		} finally {
			DB.closeConnection(conn);
		}
		if (!m_calculated)
			LogM.log(m_ctx, getClass(), Level.FINE, "Not found (BPL)");
		return m_calculated;
	}	//	calculateBPL_VB

	/**
	 * 	Is Tax Included
	 *	@return tax included
	 */
	public boolean isTaxIncluded()
	{
		return m_isTaxIncluded;
	}	//	isTaxIncluded
	
	
	/**************************************************************************
	 * 	Calculate (Business Partner) Discount
	 */
	private void calculateDiscount()
	{
		m_discountSchema = false;
		if (m_C_BPartner_ID == 0 || m_M_Product_ID == 0)
			return;
		
		int M_DiscountSchema_ID = 0;
		BigDecimal FlatDiscount = null;
		String sql = "SELECT COALESCE(p.M_DiscountSchema_ID,g.M_DiscountSchema_ID) M_DiscountSchema_ID,"
			+ " COALESCE(p.PO_DiscountSchema_ID,g.PO_DiscountSchema_ID) PO_DiscountSchema_ID, p.FlatDiscount "
			+ "FROM C_BPartner p"
			+ " INNER JOIN C_BP_Group g ON (p.C_BP_Group_ID=g.C_BP_Group_ID) "
			+ "WHERE p.C_BPartner_ID=?";
		DB conn = new DB(m_ctx);
		Cursor rs = null;
		try
		{
			DB.loadConnection(conn, DB.READ_ONLY);
			rs = conn.querySQL(sql.toString(), 
					new String[]{String.valueOf(m_C_BPartner_ID)});
			if (rs.moveToFirst())
			{
				M_DiscountSchema_ID = rs.getInt(rs.getColumnIndex(m_isSOTrx ? "M_DiscountSchema_ID" : "PO_DiscountSchema_ID"));
				double m_DoubleFlatDiscount = rs.getDouble(rs.getColumnIndex("FlatDiscount"));
				FlatDiscount = new BigDecimal(m_DoubleFlatDiscount);
			}
		} catch (Exception e) {
			LogM.log(m_ctx, getClass(), Level.SEVERE, sql, e); 
			m_calculated = false;
		} finally {
			DB.closeConnection(conn);
		}
		//	No Discount Schema
		if (M_DiscountSchema_ID == 0)
			return;
		
		MDiscountSchema sd = new MDiscountSchema(m_ctx, M_DiscountSchema_ID, null);	//	not correct
		if (sd.get_ID() == 0)
			return;
		//
		m_discountSchema = true;		
		m_PriceStd = sd.calculatePrice(m_Qty, m_PriceStd, m_M_Product_ID, 
			m_M_Product_Category_ID, FlatDiscount);
		
	}	//	calculateDiscount

	/**
	 * 	Set Base Info (UOM)
	 */
	private void setBaseInfo()
	{
		if (m_M_Product_ID == 0)
			return;
		
		MProduct product = new MProduct(m_ctx, m_M_Product_ID, null);
		if (product != null) {
			 m_C_UOM_ID = product.getC_UOM_ID();
			 m_M_Product_Category_ID = product.getM_Product_Category_ID();
		}
		
	}	//	setBaseInfo
	
	/**************************************************************************
	 * 	Calculate Discount Percentage based on Standard/List Price
	 * 	@return Discount
	 */
	public BigDecimal getDiscount()
	{
		BigDecimal Discount = Env.ZERO;
		if (m_PriceList.intValue() != 0)
			Discount = new BigDecimal ((m_PriceList.doubleValue() - m_PriceStd.doubleValue())
				/ m_PriceList.doubleValue() * 100.0);
		if (Discount.scale() > 2)
			Discount = Discount.setScale(2, BigDecimal.ROUND_HALF_UP);
		return Discount;
	}	//	getDiscount


	

	/**************************************************************************
	 * 	Get Product ID
	 *	@return id
	 */
	public int getM_Product_ID()
	{
		return m_M_Product_ID;
	}
	
	/**
	 * 	Get PriceList ID
	 *	@return pl
	 */
	public int getM_PriceList_ID()
	{
		return m_M_PriceList_ID;
	}	//	getM_PriceList_ID
	
	/**
	 * 	Set PriceList
	 *	@param M_PriceList_ID pl
	 */
	public void setM_PriceList_ID( int M_PriceList_ID)
	{
		m_M_PriceList_ID = M_PriceList_ID;
		m_calculated = false;
	}	//	setM_PriceList_ID
	
	/**
	 * 	Get PriceList Version
	 *	@return plv
	 */
	public int getM_PriceList_Version_ID()
	{
		return m_M_PriceList_Version_ID;
	}	//	getM_PriceList_Version_ID
	
	/**
	 * 	Set PriceList Version
	 *	@param M_PriceList_Version_ID plv
	 */
	public void setM_PriceList_Version_ID (int M_PriceList_Version_ID)
	{
		m_M_PriceList_Version_ID = M_PriceList_Version_ID;
		m_calculated = false;
	}	//	setM_PriceList_Version_ID
	
	/**
	 * 	Get Price Date
	 *	@return date
	 */
	public Date getPriceDate()
	{
		return m_PriceDate;
	}	//	getPriceDate
	
	/**
	 * 	Set Price Date
	 *	@param priceDate date
	 */
	public void setPriceDate(Date priceDate)
	{
		m_PriceDate = priceDate;
		m_calculated = false;
	}	//	setPriceDate
	
	/**
	 * 	Set Precision.
	 */
	/*private void setPrecision ()
	{
		if (m_M_PriceList_ID != 0)
			m_precision = MPriceList.getPricePrecision(m_ctx, getM_PriceList_ID());
	}	//	setPrecision*/
	
	/**
	 * 	Get Precision
	 *	@return precision - -1 = no rounding
	 */
	public int getPrecision()
	{
		return m_precision;
	}	//	getPrecision
	
	/**
	 * 	Round
	 *	@param bd number
	 *	@return rounded number
	 */
	private BigDecimal round (BigDecimal bd)
	{
		if (m_precision >= 0	//	-1 = no rounding
			&& bd.scale() > m_precision)
			return bd.setScale(m_precision, BigDecimal.ROUND_HALF_UP);
		return bd;
	}	//	round
	
	/**************************************************************************
	 * 	Get C_UOM_ID
	 *	@return uom
	 */
	public int getC_UOM_ID()
	{
		if (!m_calculated)
			calculatePrice();
		return m_C_UOM_ID;
	}
	
	/**
	 * 	Get Price List
	 *	@return list
	 */
	public BigDecimal getPriceList()
	{
		if (!m_calculated)
			calculatePrice();
		return round(m_PriceList);
	}
	/**
	 * 	Get Price Std
	 *	@return std
	 */
	public BigDecimal getPriceStd()
	{
		if (!m_calculated)
			calculatePrice();
		return round(m_PriceStd);
	}
	/**
	 * 	Get Price Limit
	 *	@return limit
	 */
	public BigDecimal getPriceLimit()
	{
		if (!m_calculated)
			calculatePrice();
		return round(m_PriceLimit);
	}
	/**
	 * 	Get Price List Currency
	 *	@return currency
	 */
	public int getC_Currency_ID()
	{
		if (!m_calculated)
			calculatePrice();
		return m_C_Currency_ID;
	}
	/**
	 * 	Is Price List enforded?
	 *	@return enforce limit
	 */
	public boolean isEnforcePriceLimit()
	{
		if (!m_calculated)
			calculatePrice();
		return m_enforcePriceLimit;
	}	//	isEnforcePriceLimit

	/**
	 * 	Is a DiscountSchema active?
	 *	@return active Discount Schema
	 */
	public boolean isDiscountSchema()
	{
		return m_discountSchema || m_useVendorBreak;	
	}	//	isDiscountSchema
	
	/**
	 * 	Is the Price Calculated (i.e. found)?
	 *	@return calculated
	 */
	public boolean isCalculated()
	{
		return m_calculated;
	}	//	isCalculated
	
}	//	MProductPrice
