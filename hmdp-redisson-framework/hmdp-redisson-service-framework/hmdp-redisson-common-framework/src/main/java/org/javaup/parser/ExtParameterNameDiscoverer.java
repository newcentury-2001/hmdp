package org.javaup.parser;

import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.NativeDetector;

/**
 * @program: 黑马点评-plus升级版实战项目
 * @description: 扩展参数名发现器
 * @author: 阿星不是程序员
 **/
public class ExtParameterNameDiscoverer extends DefaultParameterNameDiscoverer {
    
    public ExtParameterNameDiscoverer() {
        super();
        if (!NativeDetector.inNativeImage()) {
            addDiscoverer(new LocalVariableTableParameterNameDiscoverer());
        }
    }
}
