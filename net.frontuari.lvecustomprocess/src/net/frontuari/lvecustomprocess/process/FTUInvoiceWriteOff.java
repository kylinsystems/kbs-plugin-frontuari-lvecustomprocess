package net.frontuari.lvecustomprocess.process;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.logging.Level;

import org.compiere.model.MAllocationHdr;
import org.compiere.model.MAllocationLine;
import org.compiere.model.MDocType;
import org.compiere.model.MInvoice;
import org.compiere.model.MPayment;
import org.compiere.model.MSequence;
import org.compiere.process.DocAction;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.util.AdempiereUserError;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Util;

import net.frontuari.lvecustomprocess.base.FTUProcess;
import net.frontuari.lvecustomprocess.exceptions.NoCurrencyConversionException;
import net.frontuari.lvecustomprocess.model.FTUMConversionRate;

public class FTUInvoiceWriteOff extends FTUProcess {
	/**	BPartner				*/
	private int			p_C_BPartner_ID = 0;
	/** BPartner Group			*/
	private int			p_C_BP_Group_ID = 0;
	/**	Invoice					*/
	private int			p_C_Invoice_ID = 0;
	
	/** Max Amt					*/
	private BigDecimal	p_MaxInvWriteOffAmt = Env.ZERO;
	/** AP or AR				*/
	//private String		p_APAR = "R";
	/*private static String	ONLY_AP = "P";
	private static String	ONLY_AR = "R";*/
	
	/** Invoice Date From		*/
	private Timestamp	p_DateInvoiced_From = null;
	/** Invoice Date To			*/
	private Timestamp	p_DateInvoiced_To = null;
	/** Accounting Date			*/
	private Timestamp	p_DateAcct = null;
	/** Create Payment			*/
	private boolean		p_CreatePayment = false;
	/** Bank Account			*/
	private int			p_C_BankAccount_ID = 0;
	/** Simulation				*/
	private boolean		p_IsSimulation = true;
	/** Currency */
	private int			p_C_Currency_ID = -1;
	/** DocTypes */
	private String		p_C_DocType_ID = null;
	/** Charge */
	private int			p_C_Charge_ID = 0;
	/** Validate Amount */
	private boolean		p_IsValidateAmount = false;
	/** Transferred */
	private boolean		p_IsTransferred = false;

	/**	Allocation Hdr			*/
	private MAllocationHdr	m_alloc = null;
	/**	Payment					*/
	private MPayment		m_payment = null;
	
	/**
	 *  Prepare - e.g., get Parameters.
	 */
	protected void prepare()
	{
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++)
		{
			String name = para[i].getParameterName();
			if (para[i].getParameter() == null && para[i].getParameter_To() == null)
				;
			else if (name.equals("C_BPartner_ID"))
				p_C_BPartner_ID = para[i].getParameterAsInt();
			else if (name.equals("C_BP_Group_ID"))
				p_C_BP_Group_ID = para[i].getParameterAsInt();
			else if (name.equals("C_Invoice_ID"))
				p_C_Invoice_ID = para[i].getParameterAsInt();
			//
			else if (name.equals("MaxInvWriteOffAmt"))
				p_MaxInvWriteOffAmt = (BigDecimal)para[i].getParameter();
			/*else if (name.equals("APAR"))
				p_APAR = (String)para[i].getParameter();*/
			//
			else if (name.equals("DateInvoiced"))
			{
				p_DateInvoiced_From = para[i].getParameterAsTimestamp();
				p_DateInvoiced_To = para[i].getParameter_ToAsTimestamp();
			}
			else if (name.equals("DateAcct"))
				p_DateAcct = (Timestamp)para[i].getParameter();
			//
			else if (name.equals("CreatePayment"))
				p_CreatePayment = "Y".equals(para[i].getParameter());
			else if (name.equals("C_BankAccount_ID"))
				p_C_BankAccount_ID = para[i].getParameterAsInt();
			//
			else if (name.equals("IsSimulation"))
				p_IsSimulation = "Y".equals(para[i].getParameter());
			else if ("C_Currency_ID".equals(name))
				p_C_Currency_ID = para[i].getParameterAsInt();
			else if ("C_DocType_ID".equals(name))
				p_C_DocType_ID = para[i].getParameterAsString();
			else if ("C_Charge_ID".equals(name))
				p_C_Charge_ID = para[i].getParameterAsInt();
			else if ("IsValidateAmount".equals(name))
				p_IsValidateAmount = para[i].getParameterAsBoolean();
			else if ("IsTransferred".equals(name))
				p_IsTransferred = para[i].getParameterAsBoolean();
			else
				log.log(Level.SEVERE, "Unknown Parameter: " + name);
		}
	}	//	prepare

	/**
	 * 	Execute
	 *	@return message
	 *	@throws Exception
	 */
	protected String doIt () throws Exception
	{
		if (log.isLoggable(Level.INFO)) log.info("C_BPartner_ID=" + p_C_BPartner_ID 
			+ ", C_BP_Group_ID=" + p_C_BP_Group_ID
			+ ", C_Invoice_ID=" + p_C_Invoice_ID
			+ ", " + p_DateInvoiced_From + " - " + p_DateInvoiced_To
			+ "; CreatePayment=" + p_CreatePayment
			+ ", C_BankAccount_ID=" + p_C_BankAccount_ID
			+ ", C_Currency_ID=" + p_C_Currency_ID
			+ ", C_DocType_ID=" + p_C_DocType_ID);
		//
		/*if (p_C_BPartner_ID == 0 && p_C_Invoice_ID == 0 && p_C_BP_Group_ID == 0)
			throw new AdempiereUserError ("@FillMandatory@ @C_Invoice_ID@ / @C_BPartner_ID@ / ");*/
		//
		if (p_CreatePayment && p_C_BankAccount_ID == 0)
			throw new AdempiereUserError ("@FillMandatory@  @C_BankAccount_ID@");
		//
		StringBuilder sql = new StringBuilder(
			"SELECT C_Invoice_ID,DocumentNo,DateInvoiced,")
			.append(" C_Currency_ID,GrandTotal, invoiceOpen(C_Invoice_ID, 0) AS OpenAmt, ")
			.append(" C_ConversionType_ID, AD_Client_ID, AD_Org_ID ")
			.append("FROM C_Invoice WHERE ");
		if (p_C_Invoice_ID != 0)
			sql.append("C_Invoice_ID=").append(p_C_Invoice_ID);
		else
		{
			if (p_C_BPartner_ID != 0)
				sql.append("C_BPartner_ID=").append(p_C_BPartner_ID).append(" AND ");
			else if (p_C_BP_Group_ID != 0)
				sql.append("EXISTS (SELECT * FROM C_BPartner bp WHERE C_Invoice.C_BPartner_ID=bp.C_BPartner_ID AND bp.C_BP_Group_ID=")
				.append(p_C_BP_Group_ID).append(")").append(" AND ");
			/*if (ONLY_AR.equals(p_APAR))
				sql.append(" AND IsSOTrx='Y'");
			else if (ONLY_AP.equals(p_APAR))
				sql.append(" AND IsSOTrx='N'");*/
			sql.append("IsSOTrx='Y'");
			//
			if (p_DateInvoiced_From != null && p_DateInvoiced_To != null)
				sql.append(" AND TRUNC(DateInvoiced) BETWEEN ")
					.append(DB.TO_DATE(p_DateInvoiced_From, true))
					.append(" AND ")
					.append(DB.TO_DATE(p_DateInvoiced_To, true));
			else if (p_DateInvoiced_From != null)
				sql.append(" AND TRUNC(DateInvoiced) >= ")
					.append(DB.TO_DATE(p_DateInvoiced_From, true));
			else if (p_DateInvoiced_To != null)
				sql.append(" AND TRUNC(DateInvoiced) <= ")
					.append(DB.TO_DATE(p_DateInvoiced_To, true));
			if (!Util.isEmpty(p_C_DocType_ID))
				sql.append(" AND C_DocType_ID IN (")
					.append(p_C_DocType_ID)
				.append(")");
		}

		sql.append(" AND IsTransferred = ?");
		sql.append(" AND IsPaid='N' ORDER BY C_Currency_ID, C_BPartner_ID, DateInvoiced");
		if (log.isLoggable(Level.FINER)) log.finer(sql.toString());
		//
		int counter = 0;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement (sql.toString(), get_TrxName());
			DB.setParameter(pstmt, 1, p_IsTransferred);
			rs = pstmt.executeQuery ();
			while (rs.next ())
			{
				if (writeOff(rs.getInt(1), rs.getString(2), rs.getTimestamp(3)
					, rs.getInt(4), rs.getBigDecimal(6), rs.getInt(7)
					, rs.getInt(8) , rs.getInt(9)))
					counter++;
			}
		} 
		catch (Exception e)
		{
			log.log(Level.SEVERE, sql.toString(), e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
		//	final
		processPayment();
		processAllocation();
		StringBuilder msgreturn = new StringBuilder("#").append(counter);
		return msgreturn.toString();
	}	//	doIt

	/**
	 * 	Write Off
	 *	@param C_Invoice_ID invoice
	 *	@param DocumentNo doc no
	 *	@param DateInvoiced date
	 *	@param C_Currency_ID currency
	 *	@param OpenAmt open amt
	 *	@return true if written off
	 */
	private boolean writeOff (int C_Invoice_ID, String DocumentNo, Timestamp DateInvoiced 
		, int C_Currency_ID, BigDecimal OpenAmt, int C_ConversionType_ID
		, int AD_Client_ID, int AD_Org_ID)
	{
		//	Nothing to do
		
		
		if (OpenAmt == null || OpenAmt.signum() == 0)
			return false;
		
		//Added By Argenis Rodríguez 27-01-2021
		if (p_IsValidateAmount)
		{
			BigDecimal maxInvWriteOffAmt = FTUMConversionRate.convert(getCtx(), p_MaxInvWriteOffAmt
					, p_C_Currency_ID, C_Currency_ID
					, DateInvoiced, C_ConversionType_ID
					, AD_Client_ID, AD_Org_ID);
			
			if (maxInvWriteOffAmt == null)
				throw new NoCurrencyConversionException(p_C_Currency_ID, C_Currency_ID
						, DateInvoiced, C_ConversionType_ID
						, AD_Client_ID, AD_Org_ID);
			
			if (OpenAmt.abs().compareTo(maxInvWriteOffAmt) >= 0)
				return false;
		}
		//End By Argenis Rodríguez
		//
		if (p_IsSimulation)
		{
			addLog(C_Invoice_ID, DateInvoiced, OpenAmt, DocumentNo);
			return true;
		}
		
		//	Invoice
		MInvoice invoice = new MInvoice(getCtx(), C_Invoice_ID, get_TrxName());
		if (!invoice.isSOTrx())
			OpenAmt = OpenAmt.negate();
		
		//	Allocation
		if (m_alloc == null || C_Currency_ID != m_alloc.getC_Currency_ID())
		{
			processAllocation();
			m_alloc = new MAllocationHdr (getCtx(), true, 
				p_DateAcct, C_Currency_ID,
				getProcessInfo().getTitle() + " #" + getAD_PInstance_ID(), get_TrxName());
			m_alloc.setAD_Org_ID(invoice.getAD_Org_ID());
			
			MDocType dt = new MDocType(getCtx(), m_alloc.getC_DocType_ID(), get_TrxName());
			
			int AD_Sequence_ID = dt.getDocNoSequence_ID();
			
			AD_Sequence_ID = AD_Sequence_ID <= 0 ? MSequence.get(getCtx(), MAllocationHdr.Table_Name, get_TrxName(), false).get_ID()
					: AD_Sequence_ID;
			
			String documentNo = DB.getSQLValueString(get_TrxName()
					, "SELECT NextDocNo(?)"
					, AD_Sequence_ID);
			
			m_alloc.setDocumentNo(documentNo);
			
			if (!m_alloc.save())
			{
				log.log(Level.SEVERE, "Cannot create allocation header");
				return false;
			}
		}
		//	Payment
		if (p_CreatePayment 
			&& (m_payment == null 
				|| invoice.getC_BPartner_ID() != m_payment.getC_BPartner_ID()
				|| C_Currency_ID != m_payment.getC_Currency_ID()))
		{
			processPayment();
			m_payment = new MPayment(getCtx(), 0, get_TrxName());
			m_payment.setAD_Org_ID(invoice.getAD_Org_ID());
			m_payment.setC_BankAccount_ID(p_C_BankAccount_ID);
			m_payment.setTenderType(MPayment.TENDERTYPE_Check);
			m_payment.setDateTrx(p_DateAcct);
			m_payment.setDateAcct(p_DateAcct);
			m_payment.setDescription(getProcessInfo().getTitle() + " #" + getAD_PInstance_ID());
			m_payment.setC_BPartner_ID(invoice.getC_BPartner_ID());
			m_payment.setIsReceipt(true);	//	payments are negative
			m_payment.setC_Currency_ID(C_Currency_ID);
			if (p_C_Charge_ID > 0)
				m_payment.setC_Charge_ID(p_C_Charge_ID);
			if (!m_payment.save())
			{
				log.log(Level.SEVERE, "Cannot create payment");
				return false;
			}
		}

		//	Line
		MAllocationLine aLine = null;
		if (p_CreatePayment)
		{
			aLine = new MAllocationLine (m_alloc, OpenAmt,
				Env.ZERO, Env.ZERO, Env.ZERO);
			m_payment.setPayAmt(m_payment.getPayAmt().add(OpenAmt));
			aLine.setC_Payment_ID(m_payment.getC_Payment_ID());
		}
		//Added By Argenis Rodríguez 27-01-2021
		else if (p_C_Charge_ID > 0)
		{
			aLine = new MAllocationLine(m_alloc, OpenAmt
					, Env.ZERO, Env.ZERO, Env.ZERO);
			aLine.setC_Charge_ID(p_C_Charge_ID);
		}
		else
			aLine = new MAllocationLine (m_alloc, Env.ZERO, 
				Env.ZERO, OpenAmt, Env.ZERO);
		aLine.setC_Invoice_ID(C_Invoice_ID);
		if (aLine.save())
		{
			addLog(C_Invoice_ID, DateInvoiced, OpenAmt, DocumentNo);
			return true;
		}
		//	Error
		log.log(Level.SEVERE, "Cannot create allocation line for C_Invoice_ID=" + C_Invoice_ID);
		return false;
	}	//	writeOff
	
	/**
	 * 	Process Allocation
	 *	@return true if processed
	 */
	private boolean processAllocation()
	{
		if (m_alloc == null)
			return true;
		processPayment();
		//	Process It
		if (!m_alloc.processIt(DocAction.ACTION_Complete)) {
			log.warning("Allocation Process Failed: " + m_alloc + " - " + m_alloc.getProcessMsg());
			throw new IllegalStateException("Allocation Process Failed: " + m_alloc + " - " + m_alloc.getProcessMsg());
				
		}
		if (m_alloc.save()) {
			m_alloc = null;
			return true;
		}
		//
		m_alloc = null;
		return false;
	}	//	processAllocation

	/**
	 * 	Process Payment
	 *	@return true if processed
	 */
	private boolean processPayment()
	{
		if (m_payment == null)
			return true;
		//	Process It
		if (!m_payment.processIt(DocAction.ACTION_Complete)) {
			log.warning("Payment Process Failed: " + m_payment + " - " + m_payment.getProcessMsg());
			throw new IllegalStateException("Payment Process Failed: " + m_payment + " - " + m_payment.getProcessMsg());
		}		
			
		if (m_payment.save()) {
			m_payment = null;
			return true;
		}
		//
		m_payment = null;
		return false;
	}	//	processPayment

}
