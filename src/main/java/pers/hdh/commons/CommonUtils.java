package pers.hdh.commons;

import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;

/**
 * 通用的工具类
 */
public class CommonUtils {

    /**
     * 解析dh-springmvc的配置文件，获取扫描的基本包名
     * @param contextConfigLocation
     * @return
     * @throws Exception
     */
    public static String getBasePackName(String contextConfigLocation) throws Exception {
        // 读到xml配置文件
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        InputStream in = CommonUtils.class.getClassLoader().getResourceAsStream(contextConfigLocation);
        Document doc = builder.parse(in);

        // 开始解析
        Element root = doc.getDocumentElement();
//        System.out.println(root.getTagName());// beans
        NodeList childNodes = root.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node child = childNodes.item(i);
            if (child instanceof Element) {
                Element element = (Element) child;
//                System.out.println(element.getTagName());// context:component-scan
                String attribute = element.getAttribute("base-package");
//                System.out.println(attribute);// pers.hdh
                if (attribute != null || "".equals(attribute.trim())) {
                    return attribute.trim();
                }
            }
        }

        return null;
    }

    /**
     * 将限定名转换为路径，如pers.hdh -> pers/hdh
     * @param qualifiedName
     * @return
     */
    public static String transferQualifiedToPath(String qualifiedName) throws Exception {
        if (qualifiedName == null) {
            throw new Exception("空串不可转换");
        }
        return qualifiedName.replaceAll("\\.", "/");
    }

    /**
     * 转换第一个字母为小写
     * @param simpleName
     * @return
     */
    public static String toLowerFirstWord(String simpleName) {
        char[] charArray = simpleName.toCharArray();
        charArray[0] += 32;
        return String.valueOf(charArray);
    }

    public static void main(String[] args) throws Exception{
        System.out.println(getBasePackName("dh-springmvc.xml"));
    }
}
