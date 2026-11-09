package com.milk.web;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Number formatting for the pages, using Indian digit grouping (e.g. Rs 1,25,000.00). Used as ${@fmt.money(x)}. */
@Component("fmt")
public class Fmt {

    public String money(BigDecimal v) {
        return "₹" + group(v);
    }

    public String qty(BigDecimal v) {
        return group(v);
    }

    private String group(BigDecimal value) {
        BigDecimal v = (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
        boolean negative = v.signum() < 0;
        String plain = v.abs().toPlainString();
        int dot = plain.indexOf('.');
        String intPart = dot >= 0 ? plain.substring(0, dot) : plain;
        String frac = dot >= 0 ? plain.substring(dot) : "";

        String grouped;
        if (intPart.length() <= 3) {
            grouped = intPart;
        } else {
            String last3 = intPart.substring(intPart.length() - 3);
            String rest = intPart.substring(0, intPart.length() - 3);
            StringBuilder sb = new StringBuilder();
            while (rest.length() > 2) {
                sb.insert(0, "," + rest.substring(rest.length() - 2));
                rest = rest.substring(0, rest.length() - 2);
            }
            sb.insert(0, rest);
            grouped = sb + "," + last3;
        }
        return (negative ? "-" : "") + grouped + frac;
    }
}
