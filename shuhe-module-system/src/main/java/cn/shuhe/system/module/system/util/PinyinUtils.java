package cn.shuhe.system.module.system.util;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;

/**
 * 拼音工具类
 */
public class PinyinUtils {

    private static final HanyuPinyinOutputFormat FORMAT = new HanyuPinyinOutputFormat();

    static {
        FORMAT.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        FORMAT.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
    }

    /**
     * 汉字转拼音（不带声调）
     * 
     * @param chinese 中文字符串
     * @return 拼音字符串
     */
    public static String toPinyin(String chinese) {
        if (chinese == null || chinese.isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        for (char c : chinese.toCharArray()) {
            if (Character.toString(c).matches("[\\u4E00-\\u9FA5]")) {
                // 是汉字
                try {
                    String[] pinyinArray = PinyinHelper.toHanyuPinyinStringArray(c, FORMAT);
                    if (pinyinArray != null && pinyinArray.length > 0) {
                        result.append(pinyinArray[0]);
                    }
                } catch (Exception e) {
                    result.append(c);
                }
            } else if (Character.isLetterOrDigit(c)) {
                // 字母或数字保留
                result.append(Character.toLowerCase(c));
            }
            // 其他字符跳过
        }
        return result.toString();
    }
}
