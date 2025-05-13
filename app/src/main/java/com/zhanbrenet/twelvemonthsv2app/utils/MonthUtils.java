package com.zhanbrenet.twelvemonthsv2app.utils;

import com.zhanbrenet.twelvemonthsv2app.R;

public class MonthUtils {

    // 🔁 Retourne l'ID de string correspondant au tag (ex: "january" → R.string.january)
    public static int getMonthStringId(String tag) {
        if (tag == null) return R.string.month_unknown;

        switch (tag.toLowerCase()) {
            case "january": return R.string.january;
            case "february": return R.string.february;
            case "march": return R.string.march;
            case "april": return R.string.april;
            case "may": return R.string.may;
            case "june": return R.string.june;
            case "july": return R.string.july;
            case "august": return R.string.august;
            case "september": return R.string.september;
            case "october": return R.string.october;
            case "november": return R.string.november;
            case "december": return R.string.december;
            default: return R.string.month_unknown;
        }
    }
}
