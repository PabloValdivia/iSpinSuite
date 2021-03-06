/******************************************************************************
 * Product: Spin-Suite (Making your Business Spin)                            *
 * Copyright (C) 1999-2007 ComPiere, Inc. All Rights Reserved.                *
 * This program is free software, you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY, without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program, if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA 95054, USA        *
 * or via info@compiere.org or http://www.compiere.org/license.html           *
 *****************************************************************************/
package org.spinsuite.model;

import java.util.Date;
import org.spinsuite.util.KeyNamePair;

/** Generated Interface for SPS_SyncMenu
 *  @author Adempiere (generated) 
 *  @version Release 3.7.0LTS
 */
public interface I_SPS_SyncMenu 
{

    /** TableName=SPS_SyncMenu */
    public static final String Table_Name = "SPS_SyncMenu";

    /** SPS_Table_ID=50103 */
    public static final int SPS_Table_ID = 50103;
    KeyNamePair Model = new KeyNamePair(SPS_Table_ID, Table_Name);

    /** Load Meta Data */

    /** Column name AD_Client_ID */
    public static final String COLUMNNAME_AD_Client_ID = "AD_Client_ID";

	/** Get Client.
	  * Client/Tenant for this installation.
	  */
	public int getAD_Client_ID();

    /** Column name AD_Org_ID */
    public static final String COLUMNNAME_AD_Org_ID = "AD_Org_ID";

	/** Set Organization.
	  * Organizational entity within client
	  */
	public void setAD_Org_ID (int AD_Org_ID);

	/** Get Organization.
	  * Organizational entity within client
	  */
	public int getAD_Org_ID();

    /** Column name AD_RuleAfter_ID */
    public static final String COLUMNNAME_AD_RuleAfter_ID = "AD_RuleAfter_ID";

	/** Set Rule After Run	  */
	public void setAD_RuleAfter_ID (int AD_RuleAfter_ID);

	/** Get Rule After Run	  */
	public int getAD_RuleAfter_ID();

    /** Column name AD_RuleBefore_ID */
    public static final String COLUMNNAME_AD_RuleBefore_ID = "AD_RuleBefore_ID";

	/** Set Rule Before Run	  */
	public void setAD_RuleBefore_ID (int AD_RuleBefore_ID);

	/** Get Rule Before Run	  */
	public int getAD_RuleBefore_ID();

    /** Column name Created */
    public static final String COLUMNNAME_Created = "Created";

	/** Get Created.
	  * Date this record was created
	  */
	public Date getCreated();

    /** Column name CreatedBy */
    public static final String COLUMNNAME_CreatedBy = "CreatedBy";

	/** Get Created By.
	  * User who created this records
	  */
	public int getCreatedBy();

    /** Column name Description */
    public static final String COLUMNNAME_Description = "Description";

	/** Set Description.
	  * Optional short description of the record
	  */
	public void setDescription (String Description);

	/** Get Description.
	  * Optional short description of the record
	  */
	public String getDescription();

    /** Column name EntityType */
    public static final String COLUMNNAME_EntityType = "EntityType";

	/** Set Entity Type.
	  * Dictionary Entity Type;
 Determines ownership and synchronization
	  */
	public void setEntityType (String EntityType);

	/** Get Entity Type.
	  * Dictionary Entity Type;
 Determines ownership and synchronization
	  */
	public String getEntityType();

    /** Column name ErrImgUrl */
    public static final String COLUMNNAME_ErrImgUrl = "ErrImgUrl";

	/** Set Error Image URL	  */
	public void setErrImgUrl (String ErrImgUrl);

	/** Get Error Image URL	  */
	public String getErrImgUrl();

    /** Column name ImageURL */
    public static final String COLUMNNAME_ImageURL = "ImageURL";

	/** Set Image URL.
	  * URL of  image
	  */
	public void setImageURL (String ImageURL);

	/** Get Image URL.
	  * URL of  image
	  */
	public String getImageURL();

    /** Column name IsActive */
    public static final String COLUMNNAME_IsActive = "IsActive";

	/** Set Active.
	  * The record is active in the system
	  */
	public void setIsActive (boolean IsActive);

	/** Get Active.
	  * The record is active in the system
	  */
	public boolean isActive();

    /** Column name IsForced */
    public static final String COLUMNNAME_IsForced = "IsForced";

	/** Set Forced	  */
	public void setIsForced (boolean IsForced);

	/** Get Forced	  */
	public boolean isForced();

    /** Column name IsSummary */
    public static final String COLUMNNAME_IsSummary = "IsSummary";

	/** Set Summary Level.
	  * This is a summary entity
	  */
	public void setIsSummary (boolean IsSummary);

	/** Get Summary Level.
	  * This is a summary entity
	  */
	public boolean isSummary();

    /** Column name IsSynchronized */
    public static final String COLUMNNAME_IsSynchronized = "IsSynchronized";

	/** Set Synchronized	  */
	public void setIsSynchronized (boolean IsSynchronized);

	/** Get Synchronized	  */
	public boolean isSynchronized();

    /** Column name LastSynchronized */
    public static final String COLUMNNAME_LastSynchronized = "LastSynchronized";

	/** Set Last Synchronized.
	  * Date when last synchronized
	  */
	public void setLastSynchronized (Date LastSynchronized);

	/** Get Last Synchronized.
	  * Date when last synchronized
	  */
	public Date getLastSynchronized();

    /** Column name Name */
    public static final String COLUMNNAME_Name = "Name";

	/** Set Name.
	  * Alphanumeric identifier of the entity
	  */
	public void setName (String Name);

	/** Get Name.
	  * Alphanumeric identifier of the entity
	  */
	public String getName();

    /** Column name Processing */
    public static final String COLUMNNAME_Processing = "Processing";

	/** Set Process Now	  */
	public void setProcessing (boolean Processing);

	/** Get Process Now	  */
	public boolean isProcessing();

    /** Column name SPS_SyncMenu_ID */
    public static final String COLUMNNAME_SPS_SyncMenu_ID = "SPS_SyncMenu_ID";

	/** Set Sync/Option Menu	  */
	public void setSPS_SyncMenu_ID (int SPS_SyncMenu_ID);

	/** Get Sync/Option Menu	  */
	public int getSPS_SyncMenu_ID();

    /** Column name SPS_Table_ID */
    public static final String COLUMNNAME_SPS_Table_ID = "SPS_Table_ID";

	/** Set Mobile Table	  */
	public void setSPS_Table_ID (int SPS_Table_ID);

	/** Get Mobile Table	  */
	public int getSPS_Table_ID();

    /** Column name Updated */
    public static final String COLUMNNAME_Updated = "Updated";

	/** Get Updated.
	  * Date this record was updated
	  */
	public Date getUpdated();

    /** Column name UpdatedBy */
    public static final String COLUMNNAME_UpdatedBy = "UpdatedBy";

	/** Get Updated By.
	  * User who updated this records
	  */
	public int getUpdatedBy();

    /** Column name WhereClause */
    public static final String COLUMNNAME_WhereClause = "WhereClause";

	/** Set Sql WHERE.
	  * Fully qualified SQL WHERE clause
	  */
	public void setWhereClause (String WhereClause);

	/** Get Sql WHERE.
	  * Fully qualified SQL WHERE clause
	  */
	public String getWhereClause();

    /** Column name WS_WebService_ID */
    public static final String COLUMNNAME_WS_WebService_ID = "WS_WebService_ID";

	/** Set Web Service	  */
	public void setWS_WebService_ID (int WS_WebService_ID);

	/** Get Web Service	  */
	public int getWS_WebService_ID();

    /** Column name WS_WebServiceMethod_ID */
    public static final String COLUMNNAME_WS_WebServiceMethod_ID = "WS_WebServiceMethod_ID";

	/** Set Web Service Method	  */
	public void setWS_WebServiceMethod_ID (int WS_WebServiceMethod_ID);

	/** Get Web Service Method	  */
	public int getWS_WebServiceMethod_ID();

    /** Column name WS_WebServiceType_ID */
    public static final String COLUMNNAME_WS_WebServiceType_ID = "WS_WebServiceType_ID";

	/** Set Web Service Type	  */
	public void setWS_WebServiceType_ID (int WS_WebServiceType_ID);

	/** Get Web Service Type	  */
	public int getWS_WebServiceType_ID();
}
