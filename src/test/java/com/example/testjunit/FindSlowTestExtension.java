package com.example.testjunit;

import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.reflect.Method;

public class FindSlowTestExtension implements BeforeTestExecutionCallback, AfterTestExecutionCallback {
    
    //1초 이상 걸리면 길다고 가정하기 위해
    private long THRESHOLD;
    
    // @RegisterExtension을 이용할 경우, THRESHOLD 값을 변경하기 위한 생성자
    public FindSlowTestExtension(long THRESHOLD) {
        this.THRESHOLD = THRESHOLD;
    }

    @Override
    public void beforeTestExecution(ExtensionContext extensionContext) throws Exception {
        ExtensionContext.Store store = getStore(extensionContext);
        /** store에 저장할 값 넣기**/
        store.put("START_TIME", System.currentTimeMillis());
    }

    @Override
    public void afterTestExecution(ExtensionContext extensionContext) throws Exception {

        /** java reflection: testMethod에서 사용되는 Annotation정보가져오기 **/
        Method requiredTestMethod = extensionContext.getRequiredTestMethod();
        SlowTest annotation = requiredTestMethod.getAnnotation(SlowTest.class);
        
        /** ExtensionContext에서 store가져오기 **/
        ExtensionContext.Store store = getStore(extensionContext);
        String testMethodName = extensionContext.getRequiredTestMethod().getName();

        /** store에 저장한 값 꺼내면서 지우기 - remove사용 **/
        //1. store에서 저장한 값을 빼내면서 지우고 그 값을 start_time 객체에 넣어두기
        long start_time = store.remove("START_TIME",long.class);
        //2. 현재 시간과 비교하기 (얼마나 오래걸렸는지)
        long duration = System.currentTimeMillis() - start_time;
        //3. 설정 duration과 SlowTest어노테이션이 붙지않은 경우에만 메세지 출력되도록
        if(duration > THRESHOLD && annotation == null) {
            System.out.printf("Please consider mark method [%s] with @SlowTest.\n", testMethodName);
        }

    }

    private ExtensionContext.Store getStore(ExtensionContext extensionContext) {
        /** store만들기 **/
        //1. testClassName과 testMethodName을 조합하여 NameSpace를 만들 것이기에 ExtensionContext 에서 정보가져오기
        String testClassName = extensionContext.getRequiredTestClass().getName();
        String testMethodName = extensionContext.getRequiredTestMethod().getName();
        //2. ExtensionContext 에서 가져온 값들로 store의 NameSpace로 넣어 store 받아오기
        ExtensionContext.Store store
                = extensionContext.getStore(ExtensionContext.Namespace.create(testClassName, testMethodName));
        return store;
    }

}
