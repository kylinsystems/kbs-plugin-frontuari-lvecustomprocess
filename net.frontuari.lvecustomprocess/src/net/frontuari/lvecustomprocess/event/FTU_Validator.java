package net.frontuari.lvecustomprocess.event;

import org.adempiere.base.event.IEventTopics;
import org.compiere.model.MTable;
import org.compiere.model.M_Element;
import org.compiere.util.DisplayType;

import net.frontuari.lvecustomprocess.base.FTUEvent;
import net.frontuari.model.MFTUColumn;

public class FTU_Validator extends FTUEvent {

	@Override
	protected void doHandleEvent() {
		
		if (MTable.Table_Name.equals(getPO().get_TableName())
				&& (IEventTopics.PO_AFTER_NEW.equals(getEventType())
					|| IEventTopics.PO_AFTER_CHANGE.equals(getEventType())))
		{
			MTable table = (MTable) getPO();
			boolean isNew = IEventTopics.PO_AFTER_NEW.equals(getEventType()); 
			
			if (isNew)
				createMandatoryColumns(table);
			
			if (isNew || table.is_ValueChanged("IsDocument"))
				createDocumentColumns(table);
		}
		
	}
	
	private void createDocumentColumns(MTable table) {
		
		if (!table.get_ValueAsBoolean("IsDocument"))
			return ;
		
		String tableName = table.getTableName();
		
		MFTUColumn column = null;
		
		String columnName = "C_DocType_ID";
		if (MFTUColumn.getColumn_ID(tableName, columnName) <= 0)
		{
			column = new MFTUColumn(table, columnName, 22, DisplayType.TableDir, "", false);
			column.setIsMandatory(true);
			column.setIsSelectionColumn(true);
			column.saveEx();
		}
		
		columnName = "DocumentNo";
		if (MFTUColumn.getColumn_ID(tableName, columnName) <= 0)
		{
			column = new MFTUColumn(table, columnName, 60, DisplayType.String, "", false);
			column.setIsMandatory(true);
			column.setIsSelectionColumn(true);
			column.setIsIdentifier(true);
			column.setSeqNo(1);
			column.saveEx();
		}
		columnName = "DateDoc";
		if (MFTUColumn.getColumn_ID(tableName, columnName) <= 0)
		{
			column = new MFTUColumn(table, columnName, 7, DisplayType.Date, "@#Date@", false);
			column.setIsMandatory(true);
			column.setIsSelectionColumn(true);
			column.saveEx();
		}

		//	Processed
		columnName = "Processed";
		if(MFTUColumn.getColumn_ID(tableName, columnName) <= 0) {
			column = new MFTUColumn(table, columnName, 1, DisplayType.YesNo, "N", false);
			column.setIsMandatory(true);
			column.saveEx();
		}
		//	Processing
		columnName = "Processing";
		if(MFTUColumn.getColumn_ID(tableName, columnName) <= 0) {
			column = new MFTUColumn(table, columnName, 1, DisplayType.YesNo, "N", false);
			column.setIsMandatory(true);
			column.setIsAlwaysUpdateable(true);
			column.saveEx();
		}
		//	Approved
		columnName = "IsApproved";
		if(MFTUColumn.getColumn_ID(tableName, columnName) <= 0) {
			column = new MFTUColumn(table, columnName, 1, DisplayType.YesNo, "N", false);
			column.setIsMandatory(true);
			column.saveEx();
		}
		//	Document Description
		columnName = "Description";
		if(MFTUColumn.getColumn_ID(tableName, columnName) <= 0) {
			column = new MFTUColumn(table, columnName, 255, DisplayType.Text, "", true);
			column.setIsMandatory(false);
			column.setIsAlwaysUpdateable(true);
			column.setIsSelectionColumn(true);
			column.saveEx();
		}
		//	Document Status
		columnName = "DocStatus";
		if(MFTUColumn.getColumn_ID(tableName, columnName) <= 0) {
			column = new MFTUColumn(table, columnName, 2, DisplayType.List, "DR", false);
			column.setIsMandatory(true);
			column.setAD_Reference_Value_ID(131);
			column.setIsSelectionColumn(true);
			column.setIsIdentifier(true);
			column.setSeqNo(2);
			column.saveEx();
		}
		//	Document Action
		columnName = "DocAction";
		if(MFTUColumn.getColumn_ID(tableName, columnName) <= 0) {
			column = new MFTUColumn(table, columnName, 2, DisplayType.Button, "CO", false);
			column.setIsMandatory(true);
			column.setAD_Reference_Value_ID(135);
			column.saveEx();
		}
	}
	
	private void createMandatoryColumns(MTable table) {
		
		//AD_Client_ID
		MFTUColumn column = new MFTUColumn(table, MTable.COLUMNNAME_AD_Client_ID
				, 22, DisplayType.TableDir, "@#AD_Client_ID@", false);
		column.setAD_Val_Rule_ID(129);
		column.saveEx();
		
		//AD_Org_ID
		column = new MFTUColumn(table, MTable.COLUMNNAME_AD_Org_ID
				, 22, DisplayType.TableDir, "@#AD_Org_ID@", true);
		column.setAD_Val_Rule_ID(104);
		column.saveEx();
		
		//IsActive
		column = new MFTUColumn(table, MTable.COLUMNNAME_IsActive, 1, DisplayType.YesNo, "Y", true);
		column.saveEx();
		
		//Created
		column = new MFTUColumn(table, MTable.COLUMNNAME_Created, 7, DisplayType.DateTime, "", false);
		column.saveEx();
		
		//Updated
		column = new MFTUColumn(table, MTable.COLUMNNAME_Updated, 7, DisplayType.DateTime, "", false);
		column.saveEx();
		
		//CreatedBy
		column = new MFTUColumn(table, MTable.COLUMNNAME_CreatedBy, 22, DisplayType.Table, "", false);
		column.setAD_Reference_Value_ID(110);
		column.saveEx();
		
		//UpdatedBy
		column = new MFTUColumn(table, MTable.COLUMNNAME_UpdatedBy, 22, DisplayType.Table, "", false);
		column.setAD_Reference_Value_ID(110);
		column.saveEx();
		
		if (!table.isView())
		{
			String tableName = table.getTableName();
			
			if (tableName.endsWith("_Trl") || tableName.endsWith("_Access"))
				return ;
			
			M_Element element = M_Element.get(table.getCtx(), tableName + "_ID");
			
			if (element == null)
			{
				element = new M_Element(table.getCtx(), 0, table.get_TrxName());
				element.setColumnName(tableName + "_ID");
				element.setName(table.getName() + "_ID");
				element.setPrintName(table.getName() + "_ID");
				element.setEntityType(table.getEntityType());
				element.saveEx();
				
				column = new MFTUColumn(table, element.getColumnName(), 22, DisplayType.ID, "", false);
				column.setAD_Element_ID(element.get_ID());
				column.setIsKey(true);
				column.setIsMandatory(true);
				column.saveEx();
			}
			
			element = M_Element.get(table.getCtx(), tableName + "_UU");
			
			if (element != null)
				return ;
			
			element = new M_Element(table.getCtx(), 0, table.get_TrxName());
			element.setColumnName(tableName + "_UU");
			element.setName(table.getName() + "_UU");
			element.setPrintName(table.getName() + "_UU");
			element.setEntityType(table.getEntityType());
			element.saveEx();
			
			column = new MFTUColumn(table, element.getColumnName(), 22, DisplayType.String, "", false);
			column.setAD_Element_ID(element.get_ID());
			column.setIsMandatory(true);
			column.saveEx();
		}
	}
}
