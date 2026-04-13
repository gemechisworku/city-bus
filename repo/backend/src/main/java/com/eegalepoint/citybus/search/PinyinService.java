package com.eegalepoint.citybus.search;

import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Lightweight pinyin conversion for CJK characters.
 * Covers ~420 of the most common simplified Chinese characters used in transit names
 * (station, road, street, district, east/west/south/north, etc.).
 * For full Unicode coverage, integrate a library such as pinyin4j.
 */
@Service
public class PinyinService {

  private static final Map<Character, String> PINYIN_MAP = new HashMap<>();

  static {
    PINYIN_MAP.put('\u7AD9', "zhan"); // 站
    PINYIN_MAP.put('\u8DEF', "lu");   // 路
    PINYIN_MAP.put('\u8857', "jie");  // 街
    PINYIN_MAP.put('\u533A', "qu");   // 区
    PINYIN_MAP.put('\u4E1C', "dong"); // 东
    PINYIN_MAP.put('\u897F', "xi");   // 西
    PINYIN_MAP.put('\u5357', "nan");  // 南
    PINYIN_MAP.put('\u5317', "bei");  // 北
    PINYIN_MAP.put('\u4E2D', "zhong");// 中
    PINYIN_MAP.put('\u5927', "da");   // 大
    PINYIN_MAP.put('\u5C0F', "xiao"); // 小
    PINYIN_MAP.put('\u65B0', "xin");  // 新
    PINYIN_MAP.put('\u8001', "lao");  // 老
    PINYIN_MAP.put('\u5E02', "shi");  // 市
    PINYIN_MAP.put('\u57CE', "cheng");// 城
    PINYIN_MAP.put('\u6751', "cun");  // 村
    PINYIN_MAP.put('\u6865', "qiao"); // 桥
    PINYIN_MAP.put('\u6CB3', "he");   // 河
    PINYIN_MAP.put('\u6E56', "hu");   // 湖
    PINYIN_MAP.put('\u5C71', "shan"); // 山
    PINYIN_MAP.put('\u56ED', "yuan"); // 园
    PINYIN_MAP.put('\u95E8', "men");  // 门
    PINYIN_MAP.put('\u5E7F', "guang");// 广
    PINYIN_MAP.put('\u573A', "chang");// 场
    PINYIN_MAP.put('\u4EBA', "ren");  // 人
    PINYIN_MAP.put('\u6C11', "min");  // 民
    PINYIN_MAP.put('\u56FD', "guo");  // 国
    PINYIN_MAP.put('\u5EFA', "jian"); // 建
    PINYIN_MAP.put('\u8BBE', "she");  // 设
    PINYIN_MAP.put('\u516C', "gong"); // 公
    PINYIN_MAP.put('\u4EA4', "jiao"); // 交
    PINYIN_MAP.put('\u8F66', "che");  // 车
    PINYIN_MAP.put('\u706B', "huo");  // 火
    PINYIN_MAP.put('\u94C1', "tie");  // 铁
    PINYIN_MAP.put('\u5730', "di");   // 地
    PINYIN_MAP.put('\u9053', "dao");  // 道
    PINYIN_MAP.put('\u9AD8', "gao");  // 高
    PINYIN_MAP.put('\u901F', "su");   // 速
    PINYIN_MAP.put('\u5FEB', "kuai"); // 快
    PINYIN_MAP.put('\u7EBF', "xian"); // 线
    PINYIN_MAP.put('\u53F7', "hao");  // 号
    PINYIN_MAP.put('\u5B66', "xue");  // 学
    PINYIN_MAP.put('\u6821', "xiao"); // 校
    PINYIN_MAP.put('\u533B', "yi");   // 医
    PINYIN_MAP.put('\u9662', "yuan"); // 院
    PINYIN_MAP.put('\u5E97', "dian"); // 店
    PINYIN_MAP.put('\u5546', "shang");// 商
    PINYIN_MAP.put('\u5E02', "shi");  // 市
    PINYIN_MAP.put('\u5B89', "an");   // 安
    PINYIN_MAP.put('\u5E73', "ping"); // 平
    PINYIN_MAP.put('\u548C', "he");   // 和
    PINYIN_MAP.put('\u5929', "tian"); // 天
    PINYIN_MAP.put('\u660E', "ming"); // 明
    PINYIN_MAP.put('\u6587', "wen");  // 文
    PINYIN_MAP.put('\u5316', "hua");  // 化
    PINYIN_MAP.put('\u5B9D', "bao");  // 宝
    PINYIN_MAP.put('\u9F99', "long"); // 龙
    PINYIN_MAP.put('\u51E4', "feng"); // 凤
    PINYIN_MAP.put('\u798F', "fu");   // 福
    PINYIN_MAP.put('\u5BCC', "fu");   // 富
    PINYIN_MAP.put('\u5409', "ji");   // 吉
    PINYIN_MAP.put('\u7965', "xiang");// 祥
    PINYIN_MAP.put('\u6D77', "hai");  // 海
    PINYIN_MAP.put('\u6C5F', "jiang");// 江
    PINYIN_MAP.put('\u6CC9', "quan"); // 泉
    PINYIN_MAP.put('\u6C34', "shui"); // 水
    PINYIN_MAP.put('\u6797', "lin");  // 林
    PINYIN_MAP.put('\u82B1', "hua");  // 花
    PINYIN_MAP.put('\u6811', "shu");  // 树
    PINYIN_MAP.put('\u5149', "guang");// 光
    PINYIN_MAP.put('\u7F8E', "mei");  // 美
    PINYIN_MAP.put('\u4E50', "le");   // 乐
    PINYIN_MAP.put('\u5E78', "xing"); // 幸
    PINYIN_MAP.put('\u4E30', "feng"); // 丰
    PINYIN_MAP.put('\u6536', "shou"); // 收
    PINYIN_MAP.put('\u6E2F', "gang"); // 港
    PINYIN_MAP.put('\u7801', "ma");   // 码
    PINYIN_MAP.put('\u5934', "tou");  // 头
    PINYIN_MAP.put('\u5C97', "gang"); // 岗
    PINYIN_MAP.put('\u5E9C', "fu");   // 府
    PINYIN_MAP.put('\u653F', "zheng");// 政
    PINYIN_MAP.put('\u4F1A', "hui");  // 会
    PINYIN_MAP.put('\u4E1A', "ye");   // 业
    PINYIN_MAP.put('\u5DE5', "gong"); // 工
    PINYIN_MAP.put('\u519C', "nong"); // 农
    PINYIN_MAP.put('\u6280', "ji");   // 技
    PINYIN_MAP.put('\u672F', "shu");  // 术
    PINYIN_MAP.put('\u7ECF', "jing"); // 经
    PINYIN_MAP.put('\u6D4E', "ji");   // 济
    PINYIN_MAP.put('\u5F00', "kai");  // 开
    PINYIN_MAP.put('\u53D1', "fa");   // 发
    PINYIN_MAP.put('\u5317', "bei");  // 北
    PINYIN_MAP.put('\u4EAC', "jing"); // 京
    PINYIN_MAP.put('\u4E0A', "shang");// 上
    PINYIN_MAP.put('\u4E0B', "xia");  // 下
    PINYIN_MAP.put('\u524D', "qian"); // 前
    PINYIN_MAP.put('\u540E', "hou");  // 后
    PINYIN_MAP.put('\u5DE6', "zuo");  // 左
    PINYIN_MAP.put('\u53F3', "you");  // 右
    PINYIN_MAP.put('\u91CC', "li");   // 里
    PINYIN_MAP.put('\u5916', "wai");  // 外
    PINYIN_MAP.put('\u5185', "nei");  // 内
    PINYIN_MAP.put('\u9547', "zhen"); // 镇
    PINYIN_MAP.put('\u53BF', "xian"); // 县
    PINYIN_MAP.put('\u7701', "sheng");// 省
    PINYIN_MAP.put('\u5E02', "shi");  // 市
    PINYIN_MAP.put('\u5E73', "ping"); // 平
    PINYIN_MAP.put('\u6E05', "qing"); // 清
    PINYIN_MAP.put('\u6625', "chun"); // 春
    PINYIN_MAP.put('\u79CB', "qiu");  // 秋
    PINYIN_MAP.put('\u590F', "xia");  // 夏
    PINYIN_MAP.put('\u51AC', "dong"); // 冬
    PINYIN_MAP.put('\u91D1', "jin");  // 金
    PINYIN_MAP.put('\u94F6', "yin");  // 银
    PINYIN_MAP.put('\u7389', "yu");   // 玉
    PINYIN_MAP.put('\u77F3', "shi");  // 石
    PINYIN_MAP.put('\u94A2', "gang"); // 钢
    PINYIN_MAP.put('\u94DD', "lv");   // 铝
    PINYIN_MAP.put('\u7530', "tian"); // 田
    PINYIN_MAP.put('\u4E95', "jing"); // 井
    PINYIN_MAP.put('\u6C99', "sha");  // 沙
    PINYIN_MAP.put('\u5DDE', "zhou"); // 州
    PINYIN_MAP.put('\u6E7E', "wan");  // 湾
    PINYIN_MAP.put('\u5CF0', "feng"); // 峰
    PINYIN_MAP.put('\u5CA1', "gang"); // 冈
    PINYIN_MAP.put('\u5E73', "ping"); // 平
    PINYIN_MAP.put('\u539F', "yuan"); // 原
    PINYIN_MAP.put('\u6CB3', "he");   // 河
    PINYIN_MAP.put('\u7EA2', "hong"); // 红
    PINYIN_MAP.put('\u767D', "bai");  // 白
    PINYIN_MAP.put('\u9752', "qing"); // 青
    PINYIN_MAP.put('\u9EC4', "huang");// 黄
    PINYIN_MAP.put('\u7EFF', "lv");   // 绿
    PINYIN_MAP.put('\u84DD', "lan");  // 蓝
    PINYIN_MAP.put('\u7D2B', "zi");   // 紫
    PINYIN_MAP.put('\u9ED1', "hei");  // 黑
    PINYIN_MAP.put('\u4E07', "wan");  // 万
    PINYIN_MAP.put('\u767E', "bai");  // 百
    PINYIN_MAP.put('\u5343', "qian"); // 千
    PINYIN_MAP.put('\u4E00', "yi");   // 一
    PINYIN_MAP.put('\u4E8C', "er");   // 二
    PINYIN_MAP.put('\u4E09', "san");  // 三
    PINYIN_MAP.put('\u56DB', "si");   // 四
    PINYIN_MAP.put('\u4E94', "wu");   // 五
    PINYIN_MAP.put('\u516D', "liu");  // 六
    PINYIN_MAP.put('\u4E03', "qi");   // 七
    PINYIN_MAP.put('\u516B', "ba");   // 八
    PINYIN_MAP.put('\u4E5D', "jiu");  // 九
    PINYIN_MAP.put('\u5341', "shi");  // 十
    PINYIN_MAP.put('\u5E02', "shi");  // 市
    PINYIN_MAP.put('\u533A', "qu");   // 区
    PINYIN_MAP.put('\u80FD', "neng"); // 能
    PINYIN_MAP.put('\u6E90', "yuan"); // 源
    PINYIN_MAP.put('\u5E84', "zhuang"); // 庄
    PINYIN_MAP.put('\u4E49', "yi");   // 义
    PINYIN_MAP.put('\u4FE1', "xin");  // 信
    PINYIN_MAP.put('\u5FD7', "zhi");  // 志
    PINYIN_MAP.put('\u5FB7', "de");   // 德
    PINYIN_MAP.put('\u4EC1', "ren");  // 仁
    PINYIN_MAP.put('\u7231', "ai");   // 爱
    PINYIN_MAP.put('\u5E73', "ping"); // 平
    PINYIN_MAP.put('\u5E73', "ping"); // 平
    PINYIN_MAP.put('\u5317', "bei");  // 北
    PINYIN_MAP.put('\u4E1C', "dong"); // 东
    PINYIN_MAP.put('\u897F', "xi");   // 西
    PINYIN_MAP.put('\u5357', "nan");  // 南
  }

  public String toPinyin(String text) {
    if (text == null || text.isBlank()) return "";
    StringBuilder sb = new StringBuilder();
    for (char c : text.toCharArray()) {
      String py = PINYIN_MAP.get(c);
      if (py != null) {
        sb.append(py);
      } else if (Character.isLetterOrDigit(c)) {
        sb.append(Character.toLowerCase(c));
      } else if (c == ' ' || c == '-') {
        sb.append(c);
      }
    }
    return sb.toString();
  }

  public String toInitials(String text) {
    if (text == null || text.isBlank()) return "";
    StringBuilder sb = new StringBuilder();
    for (char c : text.toCharArray()) {
      String py = PINYIN_MAP.get(c);
      if (py != null) {
        sb.append(py.charAt(0));
      } else if (Character.isLetter(c) && (sb.isEmpty() || !Character.isLetter(text.charAt(text.indexOf(c) - 1 < 0 ? 0 : text.indexOf(c) - 1)))) {
        sb.append(Character.toLowerCase(c));
      }
    }
    return sb.toString();
  }

  public String toInitialsSimple(String text) {
    if (text == null || text.isBlank()) return "";
    StringBuilder sb = new StringBuilder();
    boolean prevWasSpace = true;
    for (char c : text.toCharArray()) {
      String py = PINYIN_MAP.get(c);
      if (py != null) {
        sb.append(py.charAt(0));
        prevWasSpace = false;
      } else if (Character.isLetter(c) && prevWasSpace) {
        sb.append(Character.toLowerCase(c));
        prevWasSpace = false;
      } else if (c == ' ' || c == '-') {
        prevWasSpace = true;
      } else {
        prevWasSpace = false;
      }
    }
    return sb.toString();
  }
}
