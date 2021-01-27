package net.frontuari.lvecustomprocess.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Properties;
import java.util.logging.Level;

import org.compiere.model.MConversionRate;
import org.compiere.model.MConversionType;
import org.compiere.model.MCurrency;
import org.compiere.model.PO;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.TimeUtil;

public class FTUMConversionRate extends MConversionRate {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private static CLogger		s_log = CLogger.getCLogger (FTUMConversionRate.class);

	public FTUMConversionRate(Properties ctx, int C_Conversion_Rate_ID, String trxName) {
		super(ctx, C_Conversion_Rate_ID, trxName);
		// TODO Auto-generated constructor stub
	}

	public FTUMConversionRate(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
		// TODO Auto-generated constructor stub
	}

	public FTUMConversionRate(PO po, int C_ConversionType_ID, int C_Currency_ID, int C_Currency_ID_To,
			BigDecimal MultiplyRate, Timestamp ValidFrom) {
		super(po, C_ConversionType_ID, C_Currency_ID, C_Currency_ID_To, MultiplyRate, ValidFrom);
		// TODO Auto-generated constructor stub
	}
	
	public static BigDecimal convert(Properties ctx,
			BigDecimal Amt, int CurFrom_ID, int CurTo_ID,
			Timestamp ConvDate, int C_ConversionType_ID, 
			int AD_Client_ID, int AD_Org_ID)
	{
		return convert(ctx,Amt, CurFrom_ID,CurTo_ID, ConvDate, C_ConversionType_ID, AD_Client_ID, AD_Org_ID, false);
	}	//	convert
	
	public static BigDecimal convert(Properties ctx,
			BigDecimal Amt, int CurFrom_ID, int CurTo_ID,
			Timestamp ConvDate, int C_ConversionType_ID, 
			int AD_Client_ID, int AD_Org_ID, boolean isCosting)
	{
		if (Amt == null)
			throw new IllegalArgumentException("Required parameter missing - Amt");
		if (CurFrom_ID == CurTo_ID || Amt.compareTo(Env.ZERO)==0)
			return Amt;
		//	Get Rate
		BigDecimal retValue = getRate (CurFrom_ID, CurTo_ID, 
			ConvDate, C_ConversionType_ID,
			AD_Client_ID, AD_Org_ID);
		if (retValue == null)
			return null;
			
		//	Get Amount in Currency Precision
		retValue = retValue.multiply(Amt);
		int stdPrecision = isCosting ? MCurrency.getCostingPrecision(ctx, CurTo_ID): MCurrency.getStdPrecision(ctx, CurTo_ID);		

		if (retValue.scale() > stdPrecision)
			retValue = retValue.setScale(stdPrecision, RoundingMode.HALF_UP);
			
		return retValue;
	}
	
	public static BigDecimal getRate(int CurFrom_ID, int CurTo_ID,
			Timestamp ConvDate, int ConversionType_ID, int AD_Client_ID, int AD_Org_ID)
	{
		if (CurFrom_ID == CurTo_ID)
			return Env.ONE;
		//	Conversion Type
		int C_ConversionType_ID = ConversionType_ID;
		if (C_ConversionType_ID == 0)
			C_ConversionType_ID = MConversionType.getDefault(AD_Client_ID);
		//	Conversion Date
		if (ConvDate == null)
			ConvDate = TimeUtil.getDay(null);

		//	Get Rate
		String sql = "SELECT MultiplyRate "
			+ "FROM C_Conversion_Rate "
			+ "WHERE C_Currency_ID=?"					//	#1
			+ " AND C_Currency_ID_To=?"					//	#2
			+ " AND	C_ConversionType_ID=?"				//	#3
			+ " AND	? BETWEEN ValidFrom AND ValidTo"	//	#4	TRUNC (?) ORA-00932: inconsistent datatypes: expected NUMBER got TIMESTAMP
			+ " AND AD_Client_ID IN (0,?)"				//	#5
			+ " AND AD_Org_ID IN (0,?) "				//	#6
			+ " AND IsActive = 'Y' "
			+ "ORDER BY AD_Client_ID DESC, AD_Org_ID DESC, ValidFrom DESC";
		BigDecimal retValue = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement(sql, null);
			pstmt.setInt(1, CurFrom_ID);
			pstmt.setInt(2, CurTo_ID);
			pstmt.setInt(3, C_ConversionType_ID);
			pstmt.setTimestamp(4, ConvDate);
			pstmt.setInt(5, AD_Client_ID);
			pstmt.setInt(6, AD_Org_ID);
			rs = pstmt.executeQuery();
			if (rs.next())
				retValue = rs.getBigDecimal(1);
		}
		catch (Exception e)
		{
			s_log.log(Level.SEVERE, "getRate", e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}		
		if (retValue == null)
			if (s_log.isLoggable(Level.INFO)) s_log.info ("getRate - not found - CurFrom=" + CurFrom_ID 
			  + ", CurTo=" + CurTo_ID
			  + ", " + ConvDate 
			  + ", Type=" + ConversionType_ID + (ConversionType_ID==C_ConversionType_ID ? "" : "->" + C_ConversionType_ID) 
			  + ", Client=" + AD_Client_ID 
			  + ", Org=" + AD_Org_ID);
		return retValue;
	}
}
