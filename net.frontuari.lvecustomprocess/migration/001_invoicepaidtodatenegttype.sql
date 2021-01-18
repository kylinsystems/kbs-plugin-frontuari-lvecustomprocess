CREATE OR REPLACE FUNCTION adempiere.invoicepaidtodatenegttype(p_c_invoice_id numeric, p_c_currency_id numeric, p_multiplierap numeric, p_dateacct timestamp with time zone, p_typenegotiation  character varying)
 RETURNS numeric
 LANGUAGE plpgsql
 STABLE
AS $function$
DECLARE
	v_Precision         NUMERIC := 0;
    v_Min            	NUMERIC := 0;
	v_MultiplierAP		numeric := 1;
	v_PaymentAmt		numeric := 0;
	v_DateInvoiced		TIMESTAMP;
	allocation 		record;
BEGIN
	SELECT StdPrecision
	    INTO v_Precision
	    FROM C_Currency
	    WHERE C_Currency_ID = p_C_Currency_ID;
	   
	-- Get DateInvoiced for Type Negotiation
	SELECT DateInvoiced
	    INTO v_DateInvoiced 
	    FROM C_Invoice 
	    WHERE C_Invoice_ID = p_C_Invoice_ID;

	SELECT 1/10^v_Precision INTO v_Min;
	
	--	Default
	IF (p_MultiplierAP IS NOT NULL) THEN
		v_MultiplierAP := p_MultiplierAP;
	END IF;
	--	Calculate Allocated Amount
	FOR allocation IN 
	SELECT	al.AD_Client_ID, al.AD_Org_ID,al.Amount, al.DiscountAmt, al.WriteOffAmt,a.C_Currency_ID, a.DateTrx
	FROM	C_ALLOCATIONLINE al
	INNER JOIN C_ALLOCATIONHDR a ON (al.C_AllocationHdr_ID=a.C_AllocationHdr_ID)
    WHERE	al.C_Invoice_ID = p_C_Invoice_ID AND   a.IsActive='Y' AND a.DateAcct <= p_DateAcct
	LOOP
		v_PaymentAmt := v_PaymentAmt
			+ Currencyconvert(allocation.Amount + allocation.DisCountAmt + allocation.WriteOffAmt,
				allocation.C_Currency_ID, p_C_Currency_ID,(CASE WHEN p_typenegotiation='DD' THEN v_DateInvoiced ELSE allocation.DateTrx END), NULL, allocation.AD_Client_ID, allocation.AD_Org_ID);
	END LOOP;
	
	--	Ignore Rounding
	IF (v_PaymentAmt > -v_Min AND v_PaymentAmt < v_Min) THEN
		v_PaymentAmt := 0;
	END IF;

	--	Round to currency precision
	v_PaymentAmt := ROUND(COALESCE(v_PaymentAmt,0), v_Precision);
	
	RETURN	v_PaymentAmt * v_MultiplierAP;
END;	
$function$
;
