package net.frontuari.lvecustomprocess.model;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Properties;

import org.compiere.model.MAging;
import org.compiere.util.Env;

public class MFTUAging extends MAging {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4859250189274298683L;

	public MFTUAging(Properties ctx, int T_Aging_ID, String trxName) {
		super(ctx, T_Aging_ID, trxName);
	}

	public MFTUAging(Properties ctx, int AD_PInstance_ID, Timestamp StatementDate, int C_BPartner_ID, int C_Currency_ID,
			int C_Invoice_ID, int C_InvoicePaySchedule_ID, int C_BP_Group_ID, int AD_Org_ID, Timestamp DueDate,
			boolean IsSOTrx, String trxName) {
		super(ctx, AD_PInstance_ID, StatementDate, C_BPartner_ID, C_Currency_ID, C_Invoice_ID, C_InvoicePaySchedule_ID,
				C_BP_Group_ID, AD_Org_ID, DueDate, IsSOTrx, trxName);
	}

	public MFTUAging(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	/** Number of items 		*/
	private int		m_noItems = 0;
	/** Sum of Due Days			*/
	private int		m_daysDueSum = 0;
	
	/**
	 * 	Add Amount to Buckets
	 *	@param DueDate due date 
	 *	@param daysDue positive due - negative not due
	 *	@param invoicedAmt invoiced amount
	 *	@param openAmt open amount
	 */
	@Override
	public void add (Timestamp DueDate, int daysDue, BigDecimal invoicedAmt, BigDecimal openAmt)
	{
		if (invoicedAmt == null)
			invoicedAmt = Env.ZERO;
		setInvoicedAmt(getInvoicedAmt().add(invoicedAmt));
		if (openAmt == null)
			openAmt = Env.ZERO;
		setOpenAmt(getOpenAmt().add(openAmt));
		//	Days Due
		m_noItems++;
		m_daysDueSum += daysDue;
		setDaysDue(m_daysDueSum/m_noItems);
		//	Due Date
		if (getDueDate().after(DueDate))
			setDueDate(DueDate);		//	earliest
		//
		BigDecimal amt = openAmt;
		//	Not due - negative
		if (daysDue <= 0)
		{
			setDueAmt (getDueAmt().add(amt));
			if (daysDue == 0)
				setDue0 (getDue0().add(amt));
				
			if (daysDue >= -7)
				setDue0_7 (getDue0_7().add(amt));
			//	Set DueAmt 0 To 21 DueDays
			if (daysDue >= -21)
			{
				BigDecimal Due0_21 = (BigDecimal)get_Value("Due0_21");
				if(Due0_21 == null)
					Due0_21 = BigDecimal.ZERO;
				set_Value("Due0_21", (Due0_21.add(amt)));
			}
			
			if (daysDue >= -30)
				setDue0_30 (getDue0_30().add(amt));
				
			if (daysDue <= -1 && daysDue >= -7)
				setDue1_7 (getDue1_7().add(amt));
				
			if (daysDue <= -8 && daysDue >= -30)
				setDue8_30 (getDue8_30().add(amt));
				
			if (daysDue <= -31 && daysDue >= -60)
				setDue31_60 (getDue31_60().add(amt));
				
			if (daysDue <= -31)
				setDue31_Plus (getDue31_Plus().add(amt));
				
			if (daysDue <= -61 && daysDue >= -90)
				setDue61_90 (getDue61_90().add(amt));
				
			if (daysDue <= -61)
				setDue61_Plus (getDue61_Plus().add(amt));
				
			if (daysDue <= -91)
				setDue91_Plus (getDue91_Plus().add(amt));
		}
		else	//	Due = positive (> 1)
		{
			setPastDueAmt (getPastDueAmt().add(amt));
			if (daysDue <= 7)
				setPastDue1_7 (getPastDue1_7().add(amt));
			//	Set PastDueAmt 0 To 21 DueDays
			if (daysDue <= 21)
			{
				BigDecimal PastDue1_21 = (BigDecimal)get_Value("PastDue1_21");
				if(PastDue1_21 == null)
					PastDue1_21 = BigDecimal.ZERO;
				set_Value("PastDue1_21", (PastDue1_21.add(amt)));
			}
				
			if (daysDue <= 30)
				setPastDue1_30 (getPastDue1_30().add(amt));
				
			if (daysDue >= 8 && daysDue <= 30)
				setPastDue8_30 (getPastDue8_30().add(amt));
			
			if (daysDue >= 31 && daysDue <= 60)
				setPastDue31_60 (getPastDue31_60().add(amt));
				
			if (daysDue >= 31)
				setPastDue31_Plus (getPastDue31_Plus().add(amt));
			
			if (daysDue >= 61 && daysDue <= 90)
				setPastDue61_90 (getPastDue61_90().add(amt));
				
			if (daysDue >= 61)
				setPastDue61_Plus (getPastDue61_Plus().add(amt));
				
			if (daysDue >= 91)
				setPastDue91_Plus (getPastDue91_Plus().add(amt));
		}
	}	//	add

}
