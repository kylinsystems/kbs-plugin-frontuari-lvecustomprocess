package net.frontuari.model;

import java.sql.ResultSet;
import java.util.Properties;

import org.compiere.model.MColumn;
import org.compiere.model.MTable;
import org.compiere.model.M_Element;
import org.compiere.util.Env;

public class MFTUColumn extends MColumn {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public MFTUColumn(Properties ctx, int AD_Column_ID, String trxName) {
		super(ctx, AD_Column_ID, trxName);
	}

	public MFTUColumn(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	public MFTUColumn(MTable parent) {
		super(parent);
	}
	
	public MFTUColumn(MTable parent, String columnName, int length , int AD_Reference , String defaultValue, boolean updateable) {
		
		super(parent);
		setColumnName(columnName);
		setClientOrg(parent);
		
		M_Element element = M_Element.get(parent.getCtx(), columnName);
		
		if (element != null)
			setAD_Element_ID(element.get_ID());
		
		setName(columnName);
		setIsActive(true);
		setVersion(Env.ONE);
		setIsMandatory(true);
		setIsAllowLogging(true);
		setFieldLength(length);
		setAD_Reference_ID(AD_Reference);
		setDefaultValue(defaultValue);
		setUpdateable(updateable);
	}
}
