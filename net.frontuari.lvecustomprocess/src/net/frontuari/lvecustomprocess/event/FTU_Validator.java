package net.frontuari.lvecustomprocess.event;

import java.math.BigDecimal;
import java.util.Optional;

import org.adempiere.base.event.IEventTopics;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MRfQLineQty;
import org.compiere.model.MRfQResponseLineQty;

import net.frontuari.lvecustomprocess.base.FTUEvent;

public class FTU_Validator extends FTUEvent {

	@Override
	protected void doHandleEvent() {
		
		if (MRfQResponseLineQty.Table_Name.equals(getPO().get_TableName())
			&& (IEventTopics.PO_BEFORE_NEW.equals(getEventType()) || IEventTopics.PO_BEFORE_CHANGE.equals(getEventType())))
		validateRfqLineQty();
		
	}
	
	private void validateRfqLineQty() {
		
		MRfQResponseLineQty responseLineQty = (MRfQResponseLineQty) getPO();
		MRfQLineQty lineQty = responseLineQty.getRfQLineQty();
		
		BigDecimal qty = lineQty.getQty();
		
		BigDecimal proposedQuantity = Optional.ofNullable((BigDecimal) responseLineQty.get_Value("ProposedQuantity"))
				.orElse(BigDecimal.ZERO);
		
		if (BigDecimal.ZERO.equals(proposedQuantity))
		{
			responseLineQty.set_ValueOfColumn("ProposedQuantity", qty);
			return ;
		}
		
		if (proposedQuantity.compareTo(qty) > 0)
			throw new AdempiereException("@ProposedQuantity@ > @Qty@");
	}
}
