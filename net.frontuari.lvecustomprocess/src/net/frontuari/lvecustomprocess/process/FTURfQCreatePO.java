package net.frontuari.lvecustomprocess.process;

import java.math.BigDecimal;
import java.util.logging.Level;

import org.compiere.model.MBPartner;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.MRequisitionLine;
import org.compiere.model.MRfQ;
import org.compiere.model.MRfQLine;
import org.compiere.model.MRfQResponse;
import org.compiere.model.MRfQResponseLine;
import org.compiere.model.MRfQResponseLineQty;
import org.compiere.process.ProcessInfoParameter;

import net.frontuari.lvecustomprocess.base.FTUProcess;

public class FTURfQCreatePO extends FTUProcess{
	/**	RfQ 			*/
	private int		p_C_RfQ_ID = 0;
	private int		p_C_DocType_ID = 0;
	/**
	 * 	Prepare
	 */
	protected void prepare ()
	{
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++)
		{
			String name = para[i].getParameterName();
			if (para[i].getParameter() == null)
				;
			else if (name.equals("C_DocType_ID"))
				p_C_DocType_ID = para[i].getParameterAsInt();
			else
				log.log(Level.SEVERE, "Unknown Parameter: " + name);
		}
		p_C_RfQ_ID = getRecord_ID();
	}	//	prepare

	/**
	 * 	Process.
	 * 	Create purchase order(s) for the resonse(s) and lines marked as 
	 * 	Selected Winner using the selected Purchase Quantity (in RfQ Line Quantity) . 
	 * 	If a Response is marked as Selected Winner, all lines are created 
	 * 	(and Selected Winner of other responses ignored).  
	 * 	If there is no response marked as Selected Winner, the lines are used.
	 *	@return message
	 */
	protected String doIt () throws Exception
	{
		MRfQ rfq = new MRfQ (getCtx(), p_C_RfQ_ID, get_TrxName());
		if (rfq.get_ID() == 0)
			throw new IllegalArgumentException("No RfQ found");
		if (log.isLoggable(Level.INFO)) log.info(rfq.toString());
		
		//	Complete 
		MRfQResponse[] responses = rfq.getResponses(true, true);
		if (log.isLoggable(Level.CONFIG)) log.config("#Responses=" + responses.length);
		if (responses.length == 0)
			throw new IllegalArgumentException("No completed RfQ Responses found");
		
		//	Winner for entire RfQ
		for (int i = 0; i < responses.length; i++)
		{
			MRfQResponse response = responses[i];
			if (!response.isSelectedWinner())
				continue;
			//
			MBPartner bp = new MBPartner(getCtx(), response.getC_BPartner_ID(), get_TrxName());
			if (log.isLoggable(Level.CONFIG)) log.config("Winner=" + bp);
			MOrder order = new MOrder (getCtx(), 0, get_TrxName());
			order.setIsSOTrx(false);
			if (p_C_DocType_ID != 0)
				order.setC_DocTypeTarget_ID(p_C_DocType_ID);
			else
				order.setC_DocTypeTarget_ID();
			order.setBPartner(bp);
			order.setC_BPartner_Location_ID(response.getC_BPartner_Location_ID());
			order.setSalesRep_ID(rfq.getSalesRep_ID());
			if (response.getDateWorkComplete() != null)
				order.setDatePromised(response.getDateWorkComplete());
			else if (rfq.getDateWorkComplete() != null)
				order.setDatePromised(rfq.getDateWorkComplete());
			order.saveEx();
			//
			MRfQResponseLine[] lines = response.getLines(false);
			for (int j = 0; j < lines.length; j++)
			{
				//	Respones Line
				MRfQResponseLine line = lines[j];
				if (!line.isActive())
					continue;
				
				MRfQLine rfqline = new MRfQLine(getCtx(), line.getRfQLine().get_ID(), null);
				
				
				MRfQResponseLineQty[] qtys = line.getQtys(false);
				//	Response Line Qty
				for (int k = 0; k < qtys.length; k++)
				{
					MRfQResponseLineQty qty = qtys[k];
					//	Create PO Lline for all Purchase Line Qtys
					if (qty.getRfQLineQty().isActive() && qty.getRfQLineQty().isPurchaseQty())
					{
						MOrderLine ol = new MOrderLine (order);
						ol.setM_Product_ID(line.getRfQLine().getM_Product_ID(), 
							qty.getRfQLineQty().getC_UOM_ID());
						ol.setDescription(line.getDescription());
						ol.setQty(qty.getRfQLineQty().getQty());
						BigDecimal price = qty.getNetAmt();
						ol.setPrice();
						ol.setPrice(price);
						
						if(rfqline.get_ValueAsInt("M_RequisitionLine_ID") != 0) {
							
							MRequisitionLine mrl = new MRequisitionLine(getCtx(), rfqline.get_ValueAsInt("M_RequisitionLine_ID"), null);
							
							if(mrl.get_ValueAsInt("C_Activity_ID") != 0) {
								ol.setC_Activity_ID(mrl.get_ValueAsInt("C_Activity_ID"));
							}
							
							if(mrl.get_ValueAsInt("User1_ID") != 0) {
								ol.setUser1_ID(mrl.get_ValueAsInt("User1_ID"));
							}
							
						}
						
						ol.saveEx();
					}
				}
			}
			response.setC_Order_ID(order.getC_Order_ID());
			response.saveEx();
			return order.getDocumentNo();
		}

		
		//	Selected Winner on Line Level
		int noOrders = 0;
		for (int i = 0; i < responses.length; i++)
		{
			MRfQResponse response = responses[i];
			MBPartner bp = null;
			MOrder order = null;
			//	For all Response Lines
			MRfQResponseLine[] lines = response.getLines(false);
			for (int j = 0; j < lines.length; j++)
			{
				MRfQResponseLine line = lines[j];
				if (!line.isActive() || !line.isSelectedWinner())
					continue;
				//	New/different BP
				if (bp == null || bp.getC_BPartner_ID() != response.getC_BPartner_ID())
				{
					bp = new MBPartner(getCtx(), response.getC_BPartner_ID(), get_TrxName());
					order = null;
				}
				if (log.isLoggable(Level.CONFIG)) log.config("Line=" + line + ", Winner=" + bp);
				//	New Order
				if (order == null)
				{
					order = new MOrder (getCtx(), 0, get_TrxName());
					order.setIsSOTrx(false);
					order.setC_DocTypeTarget_ID();
					order.setBPartner(bp);
					order.setC_BPartner_Location_ID(response.getC_BPartner_Location_ID());
					order.setSalesRep_ID(rfq.getSalesRep_ID());
					order.saveEx();
					noOrders++;
					addBufferLog(0, null, null, order.getDocumentNo(), order.get_Table_ID(), order.getC_Order_ID());
				}
				//	For all Qtys
				MRfQResponseLineQty[] qtys = line.getQtys(false);
				for (int k = 0; k < qtys.length; k++)
				{
					MRfQResponseLineQty qty = qtys[k];
					if (qty.getRfQLineQty().isActive() && qty.getRfQLineQty().isPurchaseQty())
					{
						MOrderLine ol = new MOrderLine (order);
						ol.setM_Product_ID(line.getRfQLine().getM_Product_ID(), 
							qty.getRfQLineQty().getC_UOM_ID());
						ol.setDescription(line.getDescription());
						ol.setQty(qty.getRfQLineQty().getQty());
						BigDecimal price = qty.getNetAmt();
						ol.setPrice();
						ol.setPrice(price);
						ol.saveEx();
					}
				}	//	for all Qtys
			}	//	for all Response Lines
			if (order != null)
			{
				response.setC_Order_ID(order.getC_Order_ID());
				response.saveEx();
			}
		}
		StringBuilder msgreturn = new StringBuilder("#").append(noOrders);
		return msgreturn.toString();
	}	//	doIt
}
